package com.ignaciovinarro.stayfocused.core.backup

import com.ignaciovinarro.stayfocused.planner.data.ReminderEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineEntity
import com.ignaciovinarro.stayfocused.planner.data.RoutineItemEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskCompletionEntity
import com.ignaciovinarro.stayfocused.planner.data.SubtaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskEntity
import com.ignaciovinarro.stayfocused.planner.data.TaskOccurrenceEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// Conversión BackupData <-> texto JSON. Funciones puras (sin Room ni Context), por eso se prueban en JVM.
// Se usa org.json (ya viene en Android) en vez de sumar una librería de serialización.
//
// Forma del archivo:
//   { "formatVersion": 1, "exportedAt": <ms>, "tables": { "task": [...], "task_occurrence": [...], ... } }
// Los campos nulos se omiten; al leer, un campo ausente cuenta como nulo.
object BackupJson {

    fun encode(data: BackupData, exportedAt: Long): String {
        val tables = JSONObject()
            .put("task", array(data.tasks, ::taskToJson))
            .put("task_occurrence", array(data.occurrences, ::occurrenceToJson))
            .put("routine", array(data.routines, ::routineToJson))
            .put("routine_item", array(data.routineItems, ::routineItemToJson))
            .put("reminder", array(data.reminders, ::reminderToJson))
            .put("subtask", array(data.subtasks, ::subtaskToJson))
            .put("subtask_completion", array(data.subtaskCompletions, ::subtaskCompletionToJson))
        return JSONObject()
            .put("formatVersion", BACKUP_FORMAT_VERSION)
            .put("exportedAt", exportedAt)
            .put("tables", tables)
            .toString()
    }

    // Lanza BackupFormatException si el texto no se puede importar. No devuelve datos a medias:
    // o se lee todo el archivo o falla, así el reemplazo de datos nunca arranca con un archivo roto.
    fun decode(text: String): BackupData {
        try {
            val root = JSONObject(text)
            val version = root.optInt("formatVersion", -1)
            if (version < 1) throw BackupFormatException("El archivo no tiene versión de formato")
            if (version > BACKUP_FORMAT_VERSION) {
                throw BackupFormatException("Formato $version más nuevo que el soportado ($BACKUP_FORMAT_VERSION)")
            }
            // Acá irían las conversiones de versiones anteriores cuando exista un formato 2.
            val tables = root.getJSONObject("tables")
            return BackupData(
                tasks = list(tables, "task", ::taskFromJson),
                occurrences = list(tables, "task_occurrence", ::occurrenceFromJson),
                routines = list(tables, "routine", ::routineFromJson),
                routineItems = list(tables, "routine_item", ::routineItemFromJson),
                reminders = list(tables, "reminder", ::reminderFromJson),
                subtasks = list(tables, "subtask", ::subtaskFromJson),
                subtaskCompletions = list(tables, "subtask_completion", ::subtaskCompletionFromJson)
            )
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: JSONException) {
            throw BackupFormatException("El archivo no es un respaldo válido", e)
        }
    }

    private fun <T> array(items: List<T>, toJson: (T) -> JSONObject): JSONArray =
        JSONArray().also { array -> items.forEach { array.put(toJson(it)) } }

    private fun <T> list(tables: JSONObject, name: String, fromJson: (JSONObject) -> T): List<T> {
        val array = tables.getJSONArray(name)
        return List(array.length()) { fromJson(array.getJSONObject(it)) }
    }

    // putOpt omite la clave si el valor es nulo (put(nombre, null) también la borraría, pero putOpt lo dice claro).

    private fun JSONObject.longOrNull(name: String): Long? = if (isNull(name)) null else getLong(name)

    private fun JSONObject.intOrNull(name: String): Int? = if (isNull(name)) null else getInt(name)

    private fun JSONObject.stringOrNull(name: String): String? = if (isNull(name)) null else getString(name)

    // --- task ---

    private fun taskToJson(t: TaskEntity) = JSONObject()
        .put("id", t.id).put("uuid", t.uuid).put("title", t.title).putOpt("notes", t.notes)
        .put("date", t.date).putOpt("startMinutes", t.startMinutes).put("durationMinutes", t.durationMinutes)
        .put("iconKey", t.iconKey).put("colorKey", t.colorKey)
        .putOpt("recurrenceType", t.recurrenceType).putOpt("recurrenceInterval", t.recurrenceInterval)
        .putOpt("recurrenceDays", t.recurrenceDays).putOpt("recurrenceEnd", t.recurrenceEnd)
        .putOpt("routineId", t.routineId).put("status", t.status).putOpt("completedAt", t.completedAt)
        .put("createdAt", t.createdAt).put("updatedAt", t.updatedAt)

    private fun taskFromJson(o: JSONObject) = TaskEntity(
        id = o.getLong("id"), uuid = o.getString("uuid"), title = o.getString("title"),
        notes = o.stringOrNull("notes"), date = o.getLong("date"), startMinutes = o.intOrNull("startMinutes"),
        durationMinutes = o.getInt("durationMinutes"), iconKey = o.getString("iconKey"),
        colorKey = o.getString("colorKey"), recurrenceType = o.stringOrNull("recurrenceType"),
        recurrenceInterval = o.intOrNull("recurrenceInterval"), recurrenceDays = o.intOrNull("recurrenceDays"),
        recurrenceEnd = o.longOrNull("recurrenceEnd"), routineId = o.longOrNull("routineId"),
        status = o.getInt("status"), completedAt = o.longOrNull("completedAt"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    // --- task_occurrence ---

    private fun occurrenceToJson(t: TaskOccurrenceEntity) = JSONObject()
        .put("id", t.id).put("uuid", t.uuid).put("taskId", t.taskId).put("date", t.date).put("skipped", t.skipped)
        .putOpt("overrideStartMinutes", t.overrideStartMinutes)
        .putOpt("overrideDurationMinutes", t.overrideDurationMinutes)
        .putOpt("overrideTitle", t.overrideTitle).putOpt("overrideNotes", t.overrideNotes)
        .putOpt("overrideIconKey", t.overrideIconKey).putOpt("overrideColorKey", t.overrideColorKey)
        .putOpt("completedAt", t.completedAt).put("status", t.status)
        .put("createdAt", t.createdAt).put("updatedAt", t.updatedAt)

    private fun occurrenceFromJson(o: JSONObject) = TaskOccurrenceEntity(
        id = o.getLong("id"), uuid = o.getString("uuid"), taskId = o.getLong("taskId"), date = o.getLong("date"),
        skipped = o.getBoolean("skipped"), overrideStartMinutes = o.intOrNull("overrideStartMinutes"),
        overrideDurationMinutes = o.intOrNull("overrideDurationMinutes"),
        overrideTitle = o.stringOrNull("overrideTitle"), overrideNotes = o.stringOrNull("overrideNotes"),
        overrideIconKey = o.stringOrNull("overrideIconKey"), overrideColorKey = o.stringOrNull("overrideColorKey"),
        completedAt = o.longOrNull("completedAt"), status = o.getInt("status"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    // --- routine ---

    private fun routineToJson(r: RoutineEntity) = JSONObject()
        .put("id", r.id).put("uuid", r.uuid).put("name", r.name).put("startMinutes", r.startMinutes)
        .put("status", r.status).put("createdAt", r.createdAt).put("updatedAt", r.updatedAt)

    private fun routineFromJson(o: JSONObject) = RoutineEntity(
        id = o.getLong("id"), uuid = o.getString("uuid"), name = o.getString("name"),
        startMinutes = o.getInt("startMinutes"), status = o.getInt("status"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    // --- routine_item ---

    private fun routineItemToJson(r: RoutineItemEntity) = JSONObject()
        .put("id", r.id).put("uuid", r.uuid).put("routineId", r.routineId).put("title", r.title)
        .put("offsetMinutes", r.offsetMinutes).put("durationMinutes", r.durationMinutes)
        .put("iconKey", r.iconKey).put("colorKey", r.colorKey).put("position", r.position)
        .put("status", r.status).put("createdAt", r.createdAt).put("updatedAt", r.updatedAt)

    private fun routineItemFromJson(o: JSONObject) = RoutineItemEntity(
        id = o.getLong("id"), uuid = o.getString("uuid"), routineId = o.getLong("routineId"),
        title = o.getString("title"), offsetMinutes = o.getInt("offsetMinutes"),
        durationMinutes = o.getInt("durationMinutes"), iconKey = o.getString("iconKey"),
        colorKey = o.getString("colorKey"), position = o.getInt("position"), status = o.getInt("status"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    // --- reminder ---

    private fun reminderToJson(r: ReminderEntity) = JSONObject()
        .put("id", r.id).put("uuid", r.uuid).put("ownerType", r.ownerType).put("ownerId", r.ownerId)
        .put("minutesBefore", r.minutesBefore).put("kind", r.kind).putOpt("soundKey", r.soundKey)
        .put("status", r.status).put("createdAt", r.createdAt).put("updatedAt", r.updatedAt)

    private fun reminderFromJson(o: JSONObject) = ReminderEntity(
        id = o.getLong("id"), uuid = o.getString("uuid"), ownerType = o.getString("ownerType"),
        ownerId = o.getLong("ownerId"), minutesBefore = o.getInt("minutesBefore"), kind = o.getString("kind"),
        soundKey = o.stringOrNull("soundKey"), status = o.getInt("status"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    // --- subtask ---

    private fun subtaskToJson(s: SubtaskEntity) = JSONObject()
        .put("id", s.id).put("uuid", s.uuid).put("taskId", s.taskId).put("title", s.title).put("done", s.done)
        .put("position", s.position).put("status", s.status).put("createdAt", s.createdAt)
        .put("updatedAt", s.updatedAt)

    private fun subtaskFromJson(o: JSONObject) = SubtaskEntity(
        id = o.getLong("id"), uuid = o.getString("uuid"), taskId = o.getLong("taskId"),
        title = o.getString("title"), done = o.getBoolean("done"), position = o.getInt("position"),
        status = o.getInt("status"), createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )

    // --- subtask_completion ---

    private fun subtaskCompletionToJson(s: SubtaskCompletionEntity) = JSONObject()
        .put("id", s.id).put("uuid", s.uuid).put("subtaskId", s.subtaskId).put("date", s.date)
        .putOpt("completedAt", s.completedAt).put("status", s.status).put("createdAt", s.createdAt)
        .put("updatedAt", s.updatedAt)

    private fun subtaskCompletionFromJson(o: JSONObject) = SubtaskCompletionEntity(
        id = o.getLong("id"), uuid = o.getString("uuid"), subtaskId = o.getLong("subtaskId"),
        date = o.getLong("date"), completedAt = o.longOrNull("completedAt"), status = o.getInt("status"),
        createdAt = o.getLong("createdAt"), updatedAt = o.getLong("updatedAt")
    )
}
