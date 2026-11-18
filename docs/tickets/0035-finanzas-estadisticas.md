# Ticket 0035 – Finanzas: estadísticas y resumen del mes (FI-08 parcial, FI-10)

## Estado
En revisión

## Descripción
Pantalla "Estadísticas" (se abre desde el menú de Finanzas) para el **mes actual**:
- **Resumen del mes (FI-10):** ingresos, gastos y ahorro (ingresos − gastos), cada uno con la
  comparación contra el mes anterior.
- **Gasto por categoría (FI-08):** ranking de categorías de mayor a menor gasto, con monto, porcentaje
  del total y barra.
- **Evolución mensual (FI-08):** gasto de los últimos 6 meses con `ColumnChart` (ticket 0027).

Las cuentas se hacen en funciones puras con pruebas (`finance/domain/Statistics.kt`); los montos siguen
en centavos y solo se convierten a `Double` al armar el gráfico. Las transferencias no cuentan como
ingreso ni gasto.

**Alcance acotado a propósito:**
- **Gasto por categoría como ranking con barras, no como torta.** El documento funcional dice "torta",
  pero el ticket 0027 dejó la torta fuera de los gráficos propios. Queda para decidir con el
  desarrollador si se agrega un gráfico de torta/dona en `core/ui/chart`.
- **Ingresos contra gastos en el tiempo** (el documento lo menciona): por ahora solo se compara el mes
  actual contra el anterior; el gráfico de varios meses es solo de gastos (`ColumnChart` es de una serie).
- Solo el mes actual (sin navegación entre meses), como en presupuestos (0034).
- Sin filtros (FI-09) ni exportación (FI-12).

## Checklist de cambios
- [x] `finance/domain/Statistics.kt`: totales del mes, gasto por categoría, variación porcentual, serie mensual (puras) + pruebas
- [x] `formatMonthShort` en `core/util` + prueba
- [x] `StatisticsViewModel` con un único `UiState` + pruebas
- [x] Pantalla de estadísticas (resumen, ranking con barras, gráfico de columnas) y estado vacío
- [x] Menú en Finanzas y ruta de navegación
- [x] Textos, accesibilidad, compilar, `test`, `lint`
