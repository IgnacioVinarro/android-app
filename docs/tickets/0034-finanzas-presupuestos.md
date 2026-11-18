# Ticket 0034 – Finanzas: pantalla de presupuestos (FI-06, parte 1)

## Estado
Terminado

## Descripción
Pantalla para ver y fijar el presupuesto del mes actual: un renglón "Total" y uno por cada categoría de
gasto, con el límite, lo gastado, barra de progreso y el estado de alerta (80 % y 100 %). Usa la
lógica y los datos que ya existen del ticket 0028 (`budgetProgress`, `budgetLimitFor`, `monthlySpent`,
`BudgetRepository.setLimit`). Se abre desde el menú de la pantalla Finanzas.

**Alcance acotado a propósito (cada punto es un paso posterior):**
- **Sin notificaciones** al llegar al 80 % / 100 %: requieren canal, permiso de notificaciones y
  decidir cuándo se evalúan; eso toca el manifiesto y se confirma con el desarrollador antes.
  Por ahora la alerta se ve solo dentro de la pantalla (ícono + texto, no solo color).
- **Solo el mes actual**: sin navegación entre meses.
- **Sin quitar un presupuesto** (`BudgetRepository` no tiene borrado): se puede fijar y cambiar el límite.
- **Sin día de inicio de ciclo** (el documento funcional lo menciona como opción): mes calendario.
- Un límite fijado rige "desde este mes en adelante" (así lo define `setLimit`); la pantalla lo aclara.

## Checklist de cambios
- [x] `BudgetsViewModel`: filas (total + categorías de gasto) con límite, gastado y progreso; diálogo de edición
- [x] Pantalla de presupuestos con barra de progreso y alerta con ícono y texto, y diálogo para fijar el límite
- [x] Menú en la pantalla Finanzas y ruta de navegación
- [x] Textos, accesibilidad, pruebas de ViewModel, compilar, `test`, `lint`
