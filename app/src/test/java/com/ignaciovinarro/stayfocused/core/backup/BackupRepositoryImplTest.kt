package com.ignaciovinarro.stayfocused.core.backup

import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupRepositoryImplTest {

    private val dao = mockk<BackupDao>()
    private val repository = BackupRepositoryImpl(dao)
    private val task = TaskEntity(id = 1, uuid = "u1", title = "Correr", date = 20_000, durationMinutes = 30, iconKey = "i", colorKey = "c")

    @Test
    fun `export devuelve un JSON que se puede volver a leer con los datos del DAO`() = runTest {
        val data = BackupData(tasks = listOf(task))
        coEvery { dao.readAll() } returns data

        assertEquals(data, BackupJson.decode(repository.export()))
    }

    @Test
    fun `import reemplaza los datos con los del archivo`() = runTest {
        val data = BackupData(tasks = listOf(task))
        coEvery { dao.replaceAll(any()) } returns Unit

        repository.import(BackupJson.encode(data, exportedAt = 1L))

        coVerify(exactly = 1) { dao.replaceAll(data) }
    }

    @Test
    fun `import con un archivo invalido falla sin tocar los datos`() = runTest {
        try {
            repository.import("esto no es un respaldo")
            fail("Debía lanzar BackupFormatException")
        } catch (e: BackupFormatException) {
            // esperado
        }
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }
}
