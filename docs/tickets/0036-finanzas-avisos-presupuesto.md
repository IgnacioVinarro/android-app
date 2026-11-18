# Ticket 0036 – Finanzas: notificaciones de presupuesto al 80 % y al 100 % (FI-06, parte 2)

## Estado
En revisión

## Descripción
Cierra FI-06: avisa con una notificación cuando el gasto del mes **llega** al 80 % y al 100 % del
presupuesto, del total o de una categoría. Usa `budgetProgress` / `monthlySpent` / `budgetLimitFor` (0028).

**Cuándo se evalúa:** justo después de guardar o borrar un movimiento y después de fijar un límite en la
pantalla de presupuestos. Es el único momento en que el gasto o el límite cambian, así que no hace
falta un trabajo periódico ni alarmas (por eso tampoco se toca `AlarmScheduler`).

**Sin avisos repetidos:** se guarda en DataStore el último nivel avisado de cada presupuesto del mes. Solo se
avisa cuando el nivel **sube** (nada → 80 %, 80 % → 100 %, nada → 100 %). Si el gasto baja (se borra un
movimiento) el nivel guardado baja con él, y si vuelve a subir se avisa de nuevo. Al cambiar de mes el
estado del mes anterior se descarta solo.

**Decisiones:**
- **Sin cambios de manifiesto:** `POST_NOTIFICATIONS` ya estaba declarado (tareas). Si el permiso no está
  concedido no se avisa y la alerta sigue viéndose dentro de la pantalla de presupuestos
  (degradación elegante, CLAUDE.md).
- Canal propio "Presupuesto" (CLAUDE.md: un canal por módulo), así se puede silenciar por separado (TR-04).
- La notificación dice el nombre de la categoría y el porcentaje, **sin montos**; nada se escribe en logs.
- Un presupuesto que ya estaba pasado al fijarlo (límite nuevo menor a lo gastado) avisa en ese momento.

**Fuera de alcance:** pedir el permiso de notificaciones desde Finanzas (hoy se pide desde el flujo de
recordatorios de tareas), navegar a Presupuestos al tocar el aviso (abre la app), evaluar en segundo plano.

## Checklist de cambios
- [x] `finance/domain/BudgetAlerts.kt`: niveles actuales, avisos nuevos y (de)serialización del estado (puras) + pruebas
- [x] Canal "Presupuesto" en `NotificationChannels` y textos
- [x] `BudgetAlertStore` (DataStore) y `BudgetAlertNotifier` (notificación) detrás de interfaces
- [x] `BudgetAlertChecker` + pruebas
- [x] Llamarlo al guardar/borrar movimientos y al fijar un límite (+ ajustar pruebas de ViewModel)
- [x] Compilar, `test`, `lint`; lista de comprobación manual en un teléfono real

## Lista de comprobación manual (teléfono real)
1. Con las notificaciones permitidas, fijar un presupuesto de $ 1.000 para una categoría y cargar un gasto de
   $ 800: llega "Presupuesto de <categoría> – Llegaste al 80 %".
2. Cargar otro gasto hasta pasar $ 1.000: llega el aviso del 100 % y **reemplaza** al del 80 % (no se apilan).
3. Cargar un gasto más: **no** llega ningún aviso nuevo (ya estaba en 100 %).
4. Borrar los gastos hasta quedar bajo el 80 % y volver a cargar uno que llegue al 80 %: avisa otra vez.
5. Fijar un límite menor a lo ya gastado: el aviso llega al guardar el límite.
6. Presupuesto total: se avisa con el título "Presupuesto total del mes".
7. Con las notificaciones denegadas en Ajustes de Android: no llega nada, la app no falla y la alerta se ve
   en la pantalla de Presupuestos.
8. Ajustes de Android → Notificaciones de la app: aparece el canal "Presupuesto" y se puede silenciar solo.
9. La notificación no muestra montos y, al tocarla, abre la app.
