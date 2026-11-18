package com.ignaciovinarro.stayfocused.finance.domain

import com.ignaciovinarro.stayfocused.finance.data.TransactionTagEntity

const val TAG_NAME_MAX_LENGTH = 30

enum class TagNameError { EMPTY, TOO_LONG, DUPLICATE }

fun normalizeTagName(name: String): String = name.trim()

// Función pura (FI-02): valida el nombre de una etiqueta. `existingNames` son los nombres de las
// DEMÁS etiquetas (al renombrar, quien llama excluye la propia). La comparación ignora mayúsculas.
fun validateTagName(name: String, existingNames: List<String>): TagNameError? {
    val normalized = normalizeTagName(name)
    return when {
        normalized.isEmpty() -> TagNameError.EMPTY
        normalized.length > TAG_NAME_MAX_LENGTH -> TagNameError.TOO_LONG
        existingNames.any { normalizeTagName(it).equals(normalized, ignoreCase = true) } -> TagNameError.DUPLICATE
        else -> null
    }
}

data class TagLinkChanges(
    val toInsert: List<TransactionTagEntity>,
    val toUpdate: List<TransactionTagEntity>
)

// Función pura: qué filas de `transaction_tag` hay que crear o modificar para que la transacción quede
// con exactamente las etiquetas `wanted`. `existing` incluye las borradas (status = -1): si una
// etiqueta vuelve a ponerse se reactiva la fila en vez de insertar otra (índice único por par).
fun planTagLinkChanges(
    existing: List<TransactionTagEntity>,
    transactionId: Long,
    wanted: Set<Long>,
    now: Long
): TagLinkChanges {
    val toUpdate = mutableListOf<TransactionTagEntity>()
    for (link in existing) {
        val shouldBeActive = link.tagId in wanted
        if (shouldBeActive && link.status != 0) toUpdate += link.copy(status = 0, updatedAt = now)
        if (!shouldBeActive && link.status == 0) toUpdate += link.copy(status = -1, updatedAt = now)
    }
    val existingTagIds = existing.map { it.tagId }.toSet()
    val toInsert = (wanted - existingTagIds).sorted().map {
        TransactionTagEntity(transactionId = transactionId, tagId = it, createdAt = now, updatedAt = now)
    }
    return TagLinkChanges(toInsert = toInsert, toUpdate = toUpdate)
}
