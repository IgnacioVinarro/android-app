package com.ignaciovinarro.stayfocused.core.timeline

data class PositionedTimelineBlock(
    val block: TimelineBlock,
    val column: Int,
    val columnCount: Int
)

// Función pura: agrupa los bloques que se pisan en el tiempo y les asigna columna,
// sin importar de qué módulo vengan. Se prueba sin Android.
object OverlapArranger {

    fun arrange(blocks: List<TimelineBlock>): List<PositionedTimelineBlock> {
        if (blocks.isEmpty()) return emptyList()

        val sorted = blocks.sortedBy { it.startMinutes }
        val clusters = agruparSuperpuestos(sorted)
        return clusters.flatMap { asignarColumnas(it) }
    }

    private fun agruparSuperpuestos(sorted: List<TimelineBlock>): List<List<TimelineBlock>> {
        val clusters = mutableListOf<MutableList<TimelineBlock>>()
        var clusterEnd = Int.MIN_VALUE

        for (block in sorted) {
            if (clusters.isEmpty() || block.startMinutes >= clusterEnd) {
                clusters.add(mutableListOf(block))
                clusterEnd = block.startMinutes + block.durationMinutes
            } else {
                clusters.last().add(block)
                clusterEnd = maxOf(clusterEnd, block.startMinutes + block.durationMinutes)
            }
        }
        return clusters
    }

    private fun asignarColumnas(cluster: List<TimelineBlock>): List<PositionedTimelineBlock> {
        val finDeColumna = mutableListOf<Int>() // minuto en que se libera cada columna
        val columnaDe = mutableMapOf<String, Int>() // TimelineBlock.id -> columna

        for (block in cluster) {
            val columnaLibre = finDeColumna.indexOfFirst { it <= block.startMinutes }
            if (columnaLibre >= 0) {
                finDeColumna[columnaLibre] = block.startMinutes + block.durationMinutes
                columnaDe[block.id] = columnaLibre
            } else {
                finDeColumna.add(block.startMinutes + block.durationMinutes)
                columnaDe[block.id] = finDeColumna.size - 1
            }
        }

        val columnCount = finDeColumna.size
        return cluster.map { block -> PositionedTimelineBlock(block, columnaDe.getValue(block.id), columnCount) }
    }
}
