package com.ignaciovinarro.stayfocused.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.ignaciovinarro.stayfocused.planner.data.RoutineDao
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemDao
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoutineDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var routineDao: RoutineDao
    private lateinit var routineItemDao: RoutineItemDao

    @Before
    fun crearBaseEnMemoria() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        routineDao = database.routineDao()
        routineItemDao = database.routineItemDao()
    }

    @After
    fun cerrarBase() {
        database.close()
    }

    @Test
    fun insertarYObtenerPorId_devuelveLaMismaRutina() = runBlocking {
        val id = routineDao.insert(RoutineEntity(name = "Rutina de mañana", startMinutes = 420))

        val obtenida = routineDao.getById(id)

        assertThat(obtenida?.name).isEqualTo("Rutina de mañana")
    }

    @Test
    fun softDelete_marcaStatusBorradoYNoApareceEnObserveAll() = runBlocking {
        val id = routineDao.insert(RoutineEntity(name = "Rutina", startMinutes = 420))

        routineDao.softDelete(id, System.currentTimeMillis())

        assertThat(routineDao.observeAll().first()).isEmpty()
        assertThat(routineDao.getById(id)).isNull()
    }

    @Test
    fun observeByRoutine_devuelveLosItemsOrdenadosPorPosicionYExcluyeBorrados() = runBlocking {
        val routineId = routineDao.insert(RoutineEntity(name = "Rutina", startMinutes = 420))
        routineItemDao.insert(item(routineId, title = "Segundo", position = 1))
        routineItemDao.insert(item(routineId, title = "Primero", position = 0))
        val borradoId = routineItemDao.insert(item(routineId, title = "Borrado", position = 2))
        routineItemDao.softDelete(borradoId, System.currentTimeMillis())

        val items = routineItemDao.observeByRoutine(routineId).first()

        assertThat(items.map { it.title }).containsExactly("Primero", "Segundo").inOrder()
    }

    private fun item(routineId: Long, title: String, position: Int) = RoutineItemEntity(
        routineId = routineId,
        title = title,
        offsetMinutes = 0,
        durationMinutes = 30,
        iconKey = "task_default",
        colorKey = "blue",
        position = position
    )
}
