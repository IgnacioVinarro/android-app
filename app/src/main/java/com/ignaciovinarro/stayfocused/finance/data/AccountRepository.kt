package com.ignaciovinarro.stayfocused.finance.data

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccountModule {

    @Provides
    @Singleton
    fun provideAccountRepository(accountDao: AccountDao): AccountRepository =
        AccountRepositoryImpl(accountDao)
}

interface AccountRepository {
    fun observeAll(): Flow<List<AccountEntity>>
    suspend fun getById(id: Long): AccountEntity?
    suspend fun create(account: AccountEntity): Long
    suspend fun update(account: AccountEntity)

    // Borra la cuenta (lógico) solo si no tiene transacciones (ni propias ni como destino de una
    // transferencia). Devuelve `false`, sin borrar nada, si las tiene (ticket 0029).
    suspend fun delete(id: Long): Boolean
}

class AccountRepositoryImpl(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun observeAll(): Flow<List<AccountEntity>> = accountDao.observeAll()

    override suspend fun getById(id: Long): AccountEntity? = accountDao.getById(id)

    override suspend fun create(account: AccountEntity): Long = accountDao.insert(account)

    override suspend fun update(account: AccountEntity) {
        accountDao.update(account.copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun delete(id: Long): Boolean {
        if (accountDao.countTransactions(id) > 0) return false
        accountDao.softDelete(id, System.currentTimeMillis())
        return true
    }
}
