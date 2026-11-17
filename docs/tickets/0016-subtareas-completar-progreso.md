# Ticket 0016 – Completar subtareas y progreso del día (PL-05, parte 2)

## Estado
En revisión

## Descripción
Segunda mitad de PL-05: "Marcar tareas y subtareas como completadas con un toque, con animación y
progreso del día". Se apoya en la tabla `subtask` y el formulario del 0015.

**Decisiones de alcance confirmadas con el desarrollador:**
- Las subtareas se tildan desde la línea de tiempo con un **chip "X/Y" en el bloque que abre una
  hoja inferior** con la lista y casillas. Tocar el resto del bloque sigue completando la tarea.
- **Tarea y subtareas son independientes**: tildar subtareas no completa la tarea ni al revés.
- En tareas recurrentes el tilde de cada subtarea se guarda **por ocurrencia** (tabla nueva); en
  tareas simples se usa `subtask.done` (0015).
- El progreso del día se muestra en el **encabezado de Hoy**.

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Tabla `subtask_completion` (`subtaskId`, `date`, `completedAt`): una fila por (subtarea, día),
  con índice único. Igual que `task_occurrence`: solo hay fila si esa ocurrencia se tildó alguna vez;
  destildar pone `completedAt = null` en vez de borrar (así el índice único no estorba al volver a
  tildar). Sin `DELETE` físico.
- `TimelineBlock` suma `partsDone`/`partsTotal` (genérico, con 0 por defecto) para que el núcleo
  pueda mostrar "2/5" sin conocer a Planificación; los bloques de otros módulos no cambian.
- Reglas puras en `planner/domain`: `subtaskItemsFor` (qué subtareas tiene una tarea ese día y
  cuáles están tildadas) y `dayProgress`.
- **Progreso del día** = tareas completadas + subtareas completadas, sobre el total de ambas,
  contando solo Planificación (bloques de la línea de tiempo + tareas de la bandeja de hoy). Los
  bloques de otros módulos (gimnasio, comida…) se completan en su pantalla y no entran en esta cuenta.
  Se muestra como texto "X de Y completadas" más una barra (el estado no depende solo del color).
- Las subtareas borradas (`status = -1`) no cuentan. Los tildes de una subtarea borrada se ignoran.
- Animación: la barra de progreso y la opacidad del bloque animan al cambiar; la casilla de Material
  anima sola. Chip "X/Y" con área táctil chica (el bloque es angosto), mismo criterio pragmático que
  el ícono de editar; la hoja inferior sí tiene filas de 48 dp.

## Checklist de cambios
- [x] `SubtaskCompletionEntity` (tabla `subtask_completion`) + `SubtaskCompletionDao`
- [x] `SubtaskDao`: `observeAll`, `updateDone`
- [x] `SubtaskRepository`: `observeAll`, `observeCompletions(date)`, `setDone` (tarea simple) y
      `setDoneOn` (recurrente, por fecha) + pruebas
- [x] `AppDatabase`: versión 7, migración `MIGRATION_6_7`, esquema 7
- [x] `planner/domain`: `subtaskItemsFor` y `dayProgress` (funciones puras) + pruebas
- [x] `TimelineBlock`: `partsDone`/`partsTotal`; `TaskTimelineSource` los calcula + pruebas
- [x] `TodayViewModel`: progreso del día, hoja de subtareas (abrir/cerrar), tildar subtarea + pruebas
- [x] `TodayScreen`: encabezado de progreso (animado), chip "X/Y" en el bloque, hoja inferior con
      casillas de 48 dp
- [x] Textos nuevos en `strings.xml`
- [x] Pruebas instrumentadas: `SubtaskCompletionDaoTest` y migración 6→7 en `MigrationTest`
- [x] `./gradlew assembleDebug` (y `assembleDebugAndroidTest`), `test` y `lint` en verde
- [ ] Correr `./gradlew connectedAndroidTest` — necesita emulador o teléfono conectado
- [ ] Probar manualmente en el emulador/teléfono (abajo)

## Lista de comprobación manual
1. Tarea con 3 subtareas: el bloque muestra "0/3"; al tocarlo se abre la hoja con las 3 y casillas.
2. Tildar una: la casilla anima, el chip pasa a "1/3" y el progreso del día sube.
3. Tocar el resto del bloque completa la tarea sin tocar las subtareas (y al revés).
4. Tarea recurrente con subtareas: tildar una hoy no la tilda mañana.
5. Tarea de la bandeja (sin hora) con subtareas: cuentan en el progreso del día.
6. Borrar una subtarea en el formulario: desaparece del chip y del progreso.
7. Sin tareas ni subtareas ese día: no se muestra el encabezado de progreso.
8. Con TalkBack: el chip lee "Subtareas: 1 de 3 completadas"; las casillas se anuncian con su estado.
