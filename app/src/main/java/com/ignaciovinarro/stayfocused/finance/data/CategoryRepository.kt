package com.ignaciovinarro.stayfocused.finance.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CategoryModule {

    @Provides
    @Singleton
    fun provideCategoryRepository(categoryDao: CategoryDao): CategoryRepository =
        CategoryRepositoryImpl(categoryDao)
}

interface CategoryRepository {
    fun observeAll(): Flow<List<CategoryEntity>>
    suspend fun getById(id: Long): CategoryEntity?
    suspend fun create(category: CategoryEntity): Long
    suspend fun update(category: CategoryEntity)

    // Borra la categoría (lógico). Si tiene transacciones, `reassignToId` es obligatorio: se mueven a esa
    // categoría (que debe existir, ser otra y del mismo tipo). Sus presupuestos se borran con ella.
    // Lanza IllegalStateException si faltan las reasignaciones e IllegalArgumentException si el destino
    // no es válido.
    suspend fun delete(id: Long, reassignToId: Long? = null)

    // Inserta las predefinidas solo si la tabla nunca tuvo filas (ni siquiera borradas).
    suspend fun seedDefaults(defaults: List<CategoryEntity>)
}

class CategoryRepositoryImpl(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun observeAll(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    override suspend fun getById(id: Long): CategoryEntity? = categoryDao.getById(id)

    override suspend fun create(category: CategoryEntity): Long = categoryDao.insert(category)

    override suspend fun update(category: CategoryEntity) {
        categoryDao.update(category.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long, reassignToId: Long?) {
        val category = categoryDao.getById(id) ?: return
        val hasTransactions = categoryDao.countTransactions(id) > 0
        if (hasTransactions) {
            checkNotNull(reassignToId) { "La categoría tiene transacciones: hay que reasignarlas" }
            require(reassignToId != id) { "No se puede reasignar a la misma categoría" }
            val target = categoryDao.getById(reassignToId)
            require(target != null && target.type == category.type) {
                "La categoría destino no existe o es de otro tipo"
            }
        }
        categoryDao.deleteReassigning(id, if (hasTransactions) reassignToId else null, System.currentTimeMillis())
    }

    override suspend fun seedDefaults(defaults: List<CategoryEntity>) {
        if (categoryDao.countAllRows() == 0) categoryDao.insertAll(defaults)
    }
}
