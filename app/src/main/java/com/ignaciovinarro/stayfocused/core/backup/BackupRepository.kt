package com.ignaciovinarro.stayfocused.core.backup

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

// Lo único que ven los ViewModels del respaldo. Trabaja con texto JSON: el archivo en sí lo maneja
// BackupFileStore, así este repositorio no sabe nada de Uri ni de Android.
interface BackupRepository {
    suspend fun export(): String

    // Lanza BackupFormatException si el texto no es un respaldo válido; en ese caso no se toca ningún dato.
    suspend fun import(json: String)
}

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val backupDao: BackupDao
) : BackupRepository {

    private val clock: Clock = Clock.systemUTC()

    override suspend fun export(): String = BackupJson.encode(backupDao.readAll(), clock.millis())

    // Primero se decodifica TODO (valida el archivo); recién después se reemplaza, en una transacción.
    override suspend fun import(json: String) {
        val data = BackupJson.decode(json)
        backupDao.replaceAll(data)
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BackupRepositoryModule {
    @Binds
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository
}
