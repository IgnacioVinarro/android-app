package com.ignaciovinarro.stayfocused.core.backup

import com.ignaciovinarro.stayfocused.planner.data.ReminderEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BackupJsonTest {

    // Todos los campos opcionales con valor, para comprobar que ninguno se pierde en la ida y vuelta.
    private fun fullData() = BackupData(
        tasks = listOf(
            TaskEntity(
                id = 1, uuid = "u-task-1", title = "Correr", notes = "Con \"comillas\" y ñ\ny salto", date = 20_000,
                startMinutes = 480, durationMinutes = 45, iconKey = "run", colorKey = "blue",
                recurrenceType = "WEEKLY", recurrenceInterval = 1, recurrenceDays = 0b0101010, recurrenceEnd = 20_100,
                routineId = 7, status = 0, completedAt = 123L, createdAt = 10, updatedAt = 20
            ),
            // Tarea borrada lógicamente y sin hora: el borrado y los nulos deben viajar.
            TaskEntity(id = 2, uuid = "u-task-2", title = "Borrada", date = 20_001, durationMinutes = 30,
                iconKey = "task_default", colorKey = "red", status = -1, createdAt = 11, updatedAt = 21)
        ),
        occurrences = listOf(
            TaskOccurrenceEntity(
                id = 1, uuid = "u-occ-1", taskId = 1, date = 20_007, skipped = true, overrideStartMinutes = 500,
                overrideDurationMinutes = 60, overrideTitle = "Otro", overrideNotes = "n", overrideIconKey = "i",
                overrideColorKey = "c", completedAt = 99, status = 0, createdAt = 1, updatedAt = 2
            ),
            TaskOccurrenceEntity(id = 2, uuid = "u-occ-2", taskId = 1, date = 20_014, createdAt = 3, updatedAt = 4)
        ),
        routines = listOf(RoutineEntity(id = 1, uuid = "u-rou", name = "Mañana", startMinutes = 420, createdAt = 5, updatedAt = 6)),
        routineItems = listOf(
            RoutineItemEntity(id = 1, uuid = "u-ri", routineId = 1, title = "Ducha", offsetMinutes = 0,
                durationMinutes = 15, iconKey = "i", colorKey = "c", position = 0, createdAt = 7, updatedAt = 8)
        ),
        reminders = listOf(
            ReminderEntity(id = 1, uuid = "u-rem", ownerId = 1, minutesBefore = 10, kind = "alarm", soundKey = "tono", createdAt = 9, updatedAt = 10),
            ReminderEntity(id = 2, uuid = "u-rem-2", ownerId = 1, minutesBefore = 0, createdAt = 9, updatedAt = 10)
        ),
        subtasks = listOf(SubtaskEntity(id = 1, uuid = "u-sub", taskId = 1, title = "Calentar", done = true, position = 0, createdAt = 11, updatedAt = 12)),
        subtaskCompletions = listOf(
            SubtaskCompletionEntity(id = 1, uuid = "u-sc", subtaskId = 1, date = 20_007, completedAt = 55, createdAt = 13, updatedAt = 14),
            SubtaskCompletionEntity(id = 2, uuid = "u-sc-2", subtaskId = 1, date = 20_014, createdAt = 13, updatedAt = 14)
        )
    )

    private fun assertInvalid(text: String) {
        try {
            BackupJson.decode(text)
            fail("Debía lanzar BackupFormatException")
        } catch (e: BackupFormatException) {
            // esperado
        }
    }

    @Test
    fun `ida y vuelta devuelve exactamente los mismos datos`() {
        val data = fullData()
        assertEquals(data, BackupJson.decode(BackupJson.encode(data, exportedAt = 1L)))
    }

    @Test
    fun `una base vacia tambien se exporta e importa`() {
        assertEquals(BackupData(), BackupJson.decode(BackupJson.encode(BackupData(), exportedAt = 1L)))
    }

    @Test
    fun `incluye las filas borradas logicamente`() {
        val round = BackupJson.decode(BackupJson.encode(fullData(), exportedAt = 1L))
        assertEquals(listOf(0, -1), round.tasks.map { it.status })
    }

    @Test
    fun `el encabezado lleva la version del formato y la fecha de exportacion`() {
        val root = JSONObject(BackupJson.encode(BackupData(), exportedAt = 1234L))
        assertEquals(BACKUP_FORMAT_VERSION, root.getInt("formatVersion"))
        assertEquals(1234L, root.getLong("exportedAt"))
    }

    @Test
    fun `los campos nulos se omiten del archivo y vuelven como nulos`() {
        val text = BackupJson.encode(fullData(), exportedAt = 1L)
        val borrada = JSONObject(text).getJSONObject("tables").getJSONArray("task").getJSONObject(1)
        assertFalse(borrada.has("notes"))
        assertFalse(borrada.has("startMinutes"))
        assertNull(BackupJson.decode(text).tasks[1].notes)
        assertNull(BackupJson.decode(text).tasks[1].startMinutes)
    }

    @Test
    fun `conserva texto con comillas, acentos y saltos de linea`() {
        val notes = BackupJson.decode(BackupJson.encode(fullData(), exportedAt = 1L)).tasks[0].notes
        assertEquals("Con \"comillas\" y ñ\ny salto", notes)
    }

    @Test
    fun `rechaza texto que no es JSON`() = assertInvalid("esto no es json")

    @Test
    fun `rechaza un archivo vacio`() = assertInvalid("")

    @Test
    fun `rechaza un JSON sin version de formato`() = assertInvalid("""{"tables":{}}""")

    @Test
    fun `rechaza una version de formato mas nueva que la soportada`() {
        val text = JSONObject(BackupJson.encode(BackupData(), 1L)).put("formatVersion", BACKUP_FORMAT_VERSION + 1).toString()
        assertInvalid(text)
    }

    @Test
    fun `rechaza una version de formato invalida`() {
        val text = JSONObject(BackupJson.encode(BackupData(), 1L)).put("formatVersion", 0).toString()
        assertInvalid(text)
    }

    @Test
    fun `rechaza un archivo al que le falta una tabla`() {
        val root = JSONObject(BackupJson.encode(fullData(), 1L))
        root.getJSONObject("tables").remove("reminder")
        assertInvalid(root.toString())
    }

    @Test
    fun `rechaza una fila a la que le falta un campo obligatorio`() {
        val root = JSONObject(BackupJson.encode(fullData(), 1L))
        root.getJSONObject("tables").getJSONArray("task").getJSONObject(0).remove("title")
        assertInvalid(root.toString())
    }

    @Test
    fun `rechaza una fila con un campo del tipo equivocado`() {
        val root = JSONObject(BackupJson.encode(fullData(), 1L))
        root.getJSONObject("tables").getJSONArray("task").getJSONObject(0).put("date", "no es un numero")
        assertInvalid(root.toString())
    }

    @Test
    fun `un archivo roto no devuelve datos a medias`() {
        val root = JSONObject(BackupJson.encode(fullData(), 1L))
        // La última tabla está rota: toda la lectura falla, no se devuelve nada de las anteriores.
        root.getJSONObject("tables").getJSONArray("subtask_completion").getJSONObject(0).remove("uuid")
        assertInvalid(root.toString())
        assertTrue(BackupJson.decode(BackupJson.encode(fullData(), 1L)).tasks.isNotEmpty())
    }
}
