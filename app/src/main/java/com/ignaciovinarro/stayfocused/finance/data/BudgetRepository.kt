package com.ignaciovinarro.stayfocused.finance.data

import com.ignaciovinarro.stayfocused.finance.domain.toBudgetKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import java.time.YearMonth
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BudgetModule {

    @Provides
    @Singleton
    fun provideBudgetRepository(budgetDao: BudgetDao): BudgetRepository = BudgetRepositoryImpl(budgetDao)
}

interface BudgetRepository {
    fun observeAll(): Flow<List<BudgetEntity>>

    // Fija el límite desde `month` en adelante (`categoryId` null = total). Si ya había uno exactamente
    // para ese mes lo reemplaza; si no, agrega uno nuevo. El límite de cada mes se resuelve con
    // `budgetLimitFor`. `amount` en centavos y > 0.
    suspend fun setLimit(categoryId: Long?, month: YearMonth, amount: Long)
}

class BudgetRepositoryImpl(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun observeAll(): Flow<List<BudgetEntity>> = budgetDao.observeAll()

    override suspend fun setLimit(categoryId: Long?, month: YearMonth, amount: Long) {
        require(amount > 0) { "El límite del presupuesto debe ser mayor que cero" }
        val key = month.toBudgetKey()
        val existing = budgetDao.findExact(categoryId, key)
        if (existing != null) {
            budgetDao.update(existing.copy(amount = amount, updatedAt = System.currentTimeMillis()))
        } else {
            budgetDao.insert(BudgetEntity(categoryId = categoryId, effectiveFrom = key, amount = amount))
        }
    }
}
