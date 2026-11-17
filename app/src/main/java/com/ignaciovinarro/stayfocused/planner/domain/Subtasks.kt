package com.ignaciovinarro.stayfocused.planner.domain

// Una subtarea en edición en el formulario. `id` es el de la fila ya guardada, o null si es nueva
// (todavía no está en la base). `key` identifica la fila en la lista de la pantalla mientras se
// edita; es distinto de `id` porque una subtarea nueva todavía no tiene `id`.
data class SubtaskDraft(
    val key: Long,
    val title: String,
    val id: Long? = null
)

// Función pura: recorta los títulos y descarta las subtareas vacías, manteniendo el orden.
fun normalizeSubtasks(drafts: List<SubtaskDraft>): List<SubtaskDraft> =
    drafts.map { it.copy(title = it.title.trim()) }.filter { it.title.isNotEmpty() }
