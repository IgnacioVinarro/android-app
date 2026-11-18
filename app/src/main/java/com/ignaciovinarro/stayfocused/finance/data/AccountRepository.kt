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

// Sin `delete` a propósito: qué hacer con las transacciones de una cuenta borrada es una decisión
// pendiente (ticket 0028, notas).
interface AccountRepository {
    fun observeAll(): Flow<List<AccountEntity>>
    suspend fun getById(id: Long): AccountEntity?
    suspend fun create(account: AccountEntity): Long
    suspend fun update(account: AccountEntity)
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
}
