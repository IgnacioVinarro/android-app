# Ticket 0017 – Tareas atrasadas: ver, reprogramar o llevar a la bandeja (PL-14)

## Estado
En revisión

## Descripción
PL-14: "Tareas atrasadas: mostrar las no completadas y permitir reprogramarlas o llevarlas a la
bandeja". El documento funcional no define qué es "atrasada" ni cómo se muestra; las decisiones
siguientes las confirmó el desarrollador.

**Decisiones de alcance confirmadas con el desarrollador:**
- **Atrasada** = tarea **simple** (no recurrente), no completada, con **fecha anterior a hoy** (con o
  sin hora). Una tarea de hoy cuya hora ya pasó **no** es atrasada. Las tareas recurrentes quedan
  fuera: sus ocurrencias pasadas no tienen fila y habría que recorrer el historial (queda como paso
  aparte si hace falta).
- **Dónde:** aviso **"N atrasadas"** arriba de Hoy (junto al progreso del día) que abre una **hoja
  inferior** con la lista y las acciones, mismo patrón que las subtareas. Si no hay atrasadas, no se
  muestra nada.
- **Acciones por tarea:** **Reprogramar** (selector de fecha; conserva la hora si la tenía) y
  **A la bandeja** (pasa a la bandeja de **hoy**: fecha de hoy y sin hora). Nada más (sin completar
  ni borrar desde esta lista).

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Reglas puras en `planner/domain` (`OverdueTasks.kt`): `isOverdue`, `rescheduleOverdueTask` (devuelve
  nulo si la fecha elegida es anterior a hoy) y `moveToInbox`.
- "Reprogramar" solo deja elegir **hoy o una fecha futura** (el selector deshabilita los días
  pasados); si igual llegara una fecha pasada, la regla pura la rechaza.
- "A la bandeja" quita la hora, y sin hora no hay recordatorios (ticket 0012): se borran
  lógicamente los recordatorios de la tarea. "Reprogramar" conserva los recordatorios; las alarmas se
  reprograman solas (`ReminderSyncer`, ticket 0013).
- Las subtareas de la tarea no se tocan al reprogramar ni al llevarla a la bandeja.
- La lista se ordena de la más vieja a la más nueva; cada fila muestra el título y la fecha original.
- **Selector de fecha compartido:** `CreateTaskScreen` ya repetía dos veces el mismo `DatePickerDialog`
  con sus conversiones UTC. Se extrae a un componente de `core/ui` (`AppDatePickerDialog`, con fecha
  mínima opcional) que usan esas dos pantallas y la hoja de atrasadas (norma de reutilizar).
- El texto del aviso y la hoja llevan ícono además de color; las filas y botones de la hoja miden al
  menos 48 dp.

## Checklist de cambios
- [x] `core/ui`: `AppDatePickerDialog` (con fecha mínima opcional) y conversiones UTC compartidas;
      `CreateTaskScreen` pasa a usarlo en sus dos selectores
- [x] `TaskDao` / `TaskRepository`: `observeOverdue(today)` (simples, no completadas, fecha < hoy)
- [x] `planner/domain` (`OverdueTasks.kt`): `isOverdue`, `rescheduleOverdueTask`, `moveToInbox` + pruebas
- [x] `TodayViewModel`: lista de atrasadas en el estado; `onRescheduleOverdue` y `onMoveOverdueToInbox`
      (esta última borra también los recordatorios) + pruebas
- [x] `TodayScreen`: aviso "N atrasadas" y hoja inferior con las acciones y el selector de fecha
- [x] Textos nuevos en `strings.xml`
- [x] Pruebas instrumentadas: consulta `observeOverdue` en un DAO test
- [x] `./gradlew assembleDebug` (y `assembleDebugAndroidTest`), `test` y `lint` en verde
- [ ] Correr `./gradlew connectedAndroidTest` — necesita emulador o teléfono conectado
- [ ] Probar manualmente en el emulador/teléfono (abajo)

## Lista de comprobación manual
1. Crear una tarea con fecha de ayer: arriba de Hoy aparece "1 atrasada"; sin tareas atrasadas no aparece.
2. Una tarea de hoy con la hora ya pasada **no** figura como atrasada.
3. Una tarea atrasada completada no figura; una recurrente de días anteriores tampoco.
4. Abrir la hoja: se ven título y fecha original, de la más vieja a la más nueva.
5. **Reprogramar** a mañana: sale de la lista, aparece mañana a la misma hora; el selector no deja
   elegir días pasados.
6. **Reprogramar** a hoy una tarea con hora: aparece hoy en la línea de tiempo a su hora.
7. **A la bandeja**: sale de la lista y aparece en la bandeja de hoy, sin hora; si tenía
   recordatorios, ya no suenan.
8. Una tarea reprogramada que tenía recordatorio: el aviso suena en la fecha nueva (probar con hora cercana).
9. Con TalkBack: el aviso lee "N tareas atrasadas"; los botones de cada fila indican a qué tarea pertenecen.
