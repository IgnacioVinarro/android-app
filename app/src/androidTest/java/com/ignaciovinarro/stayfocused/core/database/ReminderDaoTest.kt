package com.ignaciovinarro.stayfocused.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.planner.data.REMINDER_OWNER_TASK
import com.ignaciovinarro.stayfocused.planner.data.ReminderDao
import com.ignaciovinarro.stayfocused.planner.data.ReminderEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ReminderDao

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.reminderDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    private fun recordatorio(ownerId: Long = 1L, minutesBefore: Int = 10, ownerType: String = REMINDER_OWNER_TASK) =
        ReminderEntity(ownerType = ownerType, ownerId = ownerId, minutesBefore = minutesBefore)

    @Test
    fun insertarYListarPorDueno_devuelveLosRecordatoriosOrdenadosPorMinutos() {
        runBlocking {
            dao.insert(recordatorio(ownerId = 1L, minutesBefore = 30))
            dao.insert(recordatorio(ownerId = 1L, minutesBefore = 0))
            dao.insert(recordatorio(ownerId = 1L, minutesBefore = 10))

            val obtenidos = dao.getByOwner(REMINDER_OWNER_TASK, 1L)

            assertThat(obtenidos.map { it.minutesBefore }).containsExactly(0, 10, 30).inOrder()
        }
    }

    @Test
    fun listarPorDueno_noMezclaOtrosDuenosNiOtrosTipos() {
        runBlocking {
            dao.insert(recordatorio(ownerId = 1L, minutesBefore = 5))
            dao.insert(recordatorio(ownerId = 2L, minutesBefore = 15))
            dao.insert(recordatorio(ownerId = 1L, minutesBefore = 60, ownerType = "meal"))

            val obtenidos = dao.getByOwner(REMINDER_OWNER_TASK, 1L)

            assertThat(obtenidos.map { it.minutesBefore }).containsExactly(5)
        }
    }

    @Test
    fun valoresPorDefecto_sonAvisoSimpleSinSonido() {
        runBlocking {
            dao.insert(recordatorio())

            val obtenido = dao.getByOwner(REMINDER_OWNER_TASK, 1L).single()

            assertThat(obtenido.kind).isEqualTo("notice")
            assertThat(obtenido.soundKey).isNull()
            assertThat(obtenido.status).isEqualTo(0)
        }
    }

    @Test
    fun softDelete_ocultaElRecordatorioPeroNoBorraLaFila() {
        runBlocking {
            val id = dao.insert(recordatorio(ownerId = 1L, minutesBefore = 10))
            dao.insert(recordatorio(ownerId = 1L, minutesBefore = 20))

            dao.softDelete(id, updatedAt = 999L)

            assertThat(dao.getByOwner(REMINDER_OWNER_TASK, 1L).map { it.minutesBefore }).containsExactly(20)
            val cursor = database.openHelper.readableDatabase.query(
                "SELECT status, updatedAt FROM reminder WHERE id = $id"
            )
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(0)).isEqualTo(-1)
            assertThat(cursor.getLong(1)).isEqualTo(999L)
            cursor.close()
        }
    }

    @Test
    fun softDeleteByOwner_ocultaSoloLosDeEseDueno() {
        runBlocking {
            dao.insert(recordatorio(ownerId = 1L, minutesBefore = 10))
            dao.insert(recordatorio(ownerId = 1L, minutesBefore = 20))
            dao.insert(recordatorio(ownerId = 2L, minutesBefore = 30))

            dao.softDeleteByOwner(REMINDER_OWNER_TASK, 1L, updatedAt = 5L)

            assertThat(dao.getByOwner(REMINDER_OWNER_TASK, 1L)).isEmpty()
            assertThat(dao.getByOwner(REMINDER_OWNER_TASK, 2L).map { it.minutesBefore }).containsExactly(30)
        }
    }

    @Test
    fun uuidUnico_insertarDuplicadoFalla() {
        runBlocking {
            val original = recordatorio()
            dao.insert(original)

            var fallo = false
            try {
                dao.insert(recordatorio(minutesBefore = 20).copy(uuid = original.uuid))
            } catch (e: Exception) {
                fallo = true
            }

            assertThat(fallo).isTrue()
        }
    }
}
