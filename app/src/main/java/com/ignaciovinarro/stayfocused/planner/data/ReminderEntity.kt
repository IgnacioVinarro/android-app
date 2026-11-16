package com.ignaciovinarro.stayfocused.planner.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

// Valores de `ownerType`: a qué tipo de bloque pertenece el recordatorio. La tabla es compartida
// (la van a usar otros módulos, ver documento técnico), por eso el dueño es (tipo, id) y no una
// clave foránea a `task`. Hoy solo existe "task".
const val REMINDER_OWNER_TASK = "task"

// Valores de `kind`: aviso común (notificación) o alarma sonora. En PL-10 solo se escribe "notice";
// "alarm" (y `soundKey`) llegan con PL-11.
const val REMINDER_KIND_NOTICE = "notice"
const val REMINDER_KIND_ALARM = "alarm"

// Recordatorio de un bloque (PL-10). `minutesBefore` es cuántos minutos antes de la hora de
// inicio avisa; 0 = "a la hora". Pertenece a la tarea, no a cada ocurrencia: en una tarea
// recurrente los comparten todas las ocurrencias (ticket 0012).
@Entity(
    tableName = "reminder",
    indices = [
        Index(value = ["uuid"], unique = true),
        Index(value = ["ownerType", "ownerId"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val ownerType: String = REMINDER_OWNER_TASK,
    val ownerId: Long, // apunta al id numérico del dueño, no al uuid
    val minutesBefore: Int,
    val kind: String = REMINDER_KIND_NOTICE,
    val soundKey: String? = null,
    val status: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
