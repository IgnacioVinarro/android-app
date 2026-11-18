package com.ignaciovinarro.stayfocused.core.settings.ui

import android.net.Uri
import com.ignaciovinarro.stayfocused.core.backup.BackupFileStore
import com.ignaciovinarro.stayfocused.core.backup.BackupFormatException
import com.ignaciovinarro.stayfocused.core.backup.BackupRepository
import com.ignaciovinarro.stayfocused.planner.data.ReminderScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.mockk
import java.io.IOException
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val repository = mockk<BackupRepository>()
    private val fileStore = mockk<BackupFileStore>()
    private val scheduler = mockk<ReminderScheduler>()
    private val uri = mockk<Uri>()
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = BackupViewModel(repository, fileStore, scheduler)

    @Test
    fun `estado inicial sin mensaje y sin operacion en curso`() {
        assertEquals(BackupUiState(), viewModel().state.value)
    }

    @Test
    fun `exportar escribe el respaldo en el archivo elegido`() = runTest(dispatcher) {
        coEvery { repository.export() } returns "{json}"
        coEvery { fileStore.write(uri, "{json}") } returns Unit
        val vm = viewModel()

        vm.onExportTo(uri)
        assertTrue(vm.state.value.working)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify { fileStore.write(uri, "{json}") }
        assertEquals(BackupUiState(working = false, message = BackupMessage.ExportDone), vm.state.value)
    }

    @Test
    fun `exportar muestra error si no se puede escribir el archivo`() = runTest(dispatcher) {
        coEvery { repository.export() } returns "{json}"
        coEvery { fileStore.write(any(), any()) } throws IOException()
        val vm = viewModel()

        vm.onExportTo(uri)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(BackupMessage.ExportFailed, vm.state.value.message)
        assertFalse(vm.state.value.working)
    }

    @Test
    fun `importar lee el archivo, reemplaza los datos y reprograma los avisos en ese orden`() = runTest(dispatcher) {
        coEvery { fileStore.read(uri) } returns "{json}"
        coEvery { repository.import("{json}") } returns Unit
        coEvery { scheduler.rescheduleAll() } returns true
        val vm = viewModel()

        vm.onImportFrom(uri)
        dispatcher.scheduler.advanceUntilIdle()

        coVerifyOrder {
            fileStore.read(uri)
            repository.import("{json}")
            scheduler.rescheduleAll()
        }
        assertEquals(BackupMessage.ImportDone, vm.state.value.message)
    }

    @Test
    fun `importar avisa si faltan permisos para reprogramar los avisos`() = runTest(dispatcher) {
        coEvery { fileStore.read(uri) } returns "{json}"
        coEvery { repository.import(any()) } returns Unit
        coEvery { scheduler.rescheduleAll() } returns false
        val vm = viewModel()

        vm.onImportFrom(uri)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(BackupMessage.ImportDoneAlarmsPending, vm.state.value.message)
    }

    @Test
    fun `importar un archivo invalido muestra el mensaje y no reprograma nada`() = runTest(dispatcher) {
        coEvery { fileStore.read(uri) } returns "basura"
        coEvery { repository.import(any()) } throws BackupFormatException("inválido")
        val vm = viewModel()

        vm.onImportFrom(uri)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(BackupMessage.ImportInvalidFile, vm.state.value.message)
        coVerify(exactly = 0) { scheduler.rescheduleAll() }
    }

    @Test
    fun `importar muestra error generico si falla la lectura o la base`() = runTest(dispatcher) {
        coEvery { fileStore.read(uri) } throws SecurityException()
        val vm = viewModel()

        vm.onImportFrom(uri)
        dispatcher.scheduler.advanceUntilIdle()

        assertEquals(BackupMessage.ImportFailed, vm.state.value.message)
        coVerify(exactly = 0) { repository.import(any()) }
    }

    @Test
    fun `un segundo toque mientras se procesa se ignora`() = runTest(dispatcher) {
        coEvery { fileStore.read(uri) } returns "{json}"
        coEvery { repository.import(any()) } returns Unit
        coEvery { scheduler.rescheduleAll() } returns true
        val vm = viewModel()

        vm.onImportFrom(uri)
        vm.onImportFrom(uri)
        dispatcher.scheduler.advanceUntilIdle()

        coVerify(exactly = 1) { repository.import(any()) }
    }

    @Test
    fun `onMessageShown borra el mensaje`() = runTest(dispatcher) {
        coEvery { repository.export() } returns "{json}"
        coEvery { fileStore.write(any(), any()) } returns Unit
        val vm = viewModel()
        vm.onExportTo(uri)
        dispatcher.scheduler.advanceUntilIdle()

        vm.onMessageShown()

        assertNull(vm.state.value.message)
    }

    @Test
    fun `el nombre sugerido del archivo lleva la fecha`() {
        assertEquals("stayfocused-respaldo-2026-11-18.json", defaultBackupFileName(LocalDate.of(2026, 11, 18)))
    }
}
