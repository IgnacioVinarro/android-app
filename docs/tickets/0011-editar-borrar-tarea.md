# Ticket 0011 – Editar y borrar una tarea, con opciones de recurrencia (PL-15)

## Estado
En revisión

## Descripción
Hasta ahora la app solo puede *crear* tareas: no hay forma de editarlas ni borrarlas. Este paso
agrega ese punto de entrada (que faltaba para cualquier tarea) y, sobre esa base, cubre PL-15:
editar o borrar una tarea recurrente ofreciendo **"solo esta"**, **"esta y siguientes"** o
**"todas"**.

**Decisiones de alcance confirmadas con el desarrollador:**
- El punto de entrada para editar/borrar se arma en este mismo paso (no hay ticket previo):
  en la línea de tiempo, cada bloque de tarea suma un ícono chico de editar (lápiz) además del
  toque que ya completa la tarea y el arrastre que ya mueve/redimensiona. Tocarlo abre la pantalla
  de tarea en modo edición (se reusa `CreateTaskScreen`/`CreateTaskViewModel`, igual que el editor
  de rutinas reusa su propia pantalla para crear y editar).
- "Editar solo esta ocurrencia" es completo: además de la hora (ya existía en `task_occurrence`
  como `overrideStartMinutes`), ahora también se puede overridear por ocurrencia el título, las
  notas, la duración, el ícono y el color. Se agregan esas columnas a `task_occurrence`
  (migración de base nueva).
- "Esta y siguientes" divide la tarea en dos filas de `task` (CLAUDE.md: "Editar 'esta y
  siguientes' divide la regla en dos tareas"): la original recorta su `recurrenceEnd` al día
  anterior a la ocurrencia editada, y se crea una tarea nueva con los campos editados que arranca
  ese día y mantiene el mismo patrón de recurrencia (tipo/intervalo/días/fin original). Si la
  ocurrencia editada es la primera (no hay ocurrencias anteriores), no tiene sentido dividir: se
  comporta como "todas".
- "Todas" edita/borra la fila de `task` directamente, afecta pasado y futuro, igual que hoy
  funciona editar una tarea no recurrente.
- Borrar con "solo esta" marca esa ocurrencia como salteada (`task_occurrence.skipped`, ya existía
  para otro propósito, se reusa). Borrar con "esta y siguientes" recorta `recurrenceEnd` igual que
  al editar. Borrar con "todas" es el borrado lógico que ya tiene `TaskRepository.delete`.
- Sin confirmación extra tipo "¿Seguro?" antes de borrar (mismo criterio que rutinas en el ticket
  0010): elegir el alcance (solo esta/siguientes/todas) ya es el paso deliberado.
- No se edita el patrón de recurrencia en sí (tipo, intervalo, días, fin) desde "esta y
  siguientes": solo título, notas, hora, duración, ícono y color. Cambiar el patrón de recurrencia
  de una tarea ya creada queda fuera de este paso.
- Sigue sin existir mover/redimensionar arrastrando una ocurrencia puntual de una tarea
  recurrente (`movable = false` se mantiene); ahora si se puede reprogramar su hora/duración
  editándola con "solo esta".

## Checklist de cambios
- [x] `TaskOccurrenceEntity`: agrega `overrideTitle`, `overrideNotes`, `overrideDurationMinutes`,
      `overrideIconKey`, `overrideColorKey` (todos nulos = sin override, como ya pasa con
      `overrideStartMinutes`)
- [x] `AppDatabase`: sube a versión 4, migración `MIGRATION_3_4` (agrega las 5 columnas), exporta
      esquema versión 4
- [x] `planner/domain`: funciones puras nuevas (`RecurrenceEdit.kt`) — `applyEditToAll` (pisa los
      campos editables sobre la tarea), `applyEditFromDate` (devuelve la tarea original recortada
      + la tarea nueva, o solo la editada si no hay ocurrencias previas) y
      `truncateRecurrenceFromDate` (recorta `recurrenceEnd`, para el borrado "esta y siguientes")
- [x] `TaskTimelineSource`: al armar el bloque de una ocurrencia, usa los overrides de
      `task_occurrence` para título/notas/duración/ícono/color además de la hora (ya lo hacía)
- [x] `CreateTaskViewModel`: recibe `SavedStateHandle` con `taskId` (0 = nueva) y `occurrenceDate`;
      si hay `taskId`, carga la tarea (y la ocurrencia de esa fecha, si la tarea es recurrente) y
      precarga el formulario; `save()` distingue tarea nueva / edición simple / edición de
      recurrente (pide alcance primero); se agrega `delete()` con la misma lógica de alcance
- [x] `CreateTaskScreen`: título cambia a "Editar tarea" en modo edición, botón de borrar en la
      barra superior, diálogo para elegir alcance (solo esta/esta y siguientes/todas) cuando la
      tarea es recurrente y se guarda o se borra
- [x] `TodayScreen`: ícono chico de editar en cada bloque de la línea de tiempo (no reemplaza el
      toque que completa ni el arrastre que mueve/redimensiona), navega a la tarea en modo edición
      con la fecha del bloque
- [x] `StayFocusedApp`: ruta de tarea pasa a aceptar `taskId` (0 = nueva) y `occurrenceDate`
      opcional
- [x] Textos nuevos en `strings.xml` (título de edición, diálogo de alcance, descripción del
      ícono de editar)
- [x] Pruebas unitarias: `RecurrenceEdit` (las tres funciones puras, casos normales y el caso
      límite de editar/borrar desde la primera ocurrencia), `CreateTaskViewModel` (cargar tarea
      existente simple y recurrente con/sin override previo, guardar con cada alcance, borrar con
      cada alcance), `TaskTimelineSource` (bloque usa los overrides nuevos)
- [x] Prueba instrumentada: migración 3→4 en `MigrationTest`
- [x] Verificar compilación: `./gradlew assembleDebug` (y `assembleDebugAndroidTest`)
- [x] Correr pruebas: `./gradlew test` y `./gradlew lint`
- [ ] Probar manualmente en el emulador/teléfono: editar una tarea simple; editar una recurrente
      con cada alcance y confirmar el efecto en la línea de tiempo de varios días; borrar con cada
      alcance — **pendiente, lo tenés que hacer vos**

## Notas
- Había un emulador conectado durante este paso, así que se corrió `./gradlew connectedAndroidTest`
  completo (incluida la migración 3→4 nueva) en verde.
- El punto de entrada de edición es un ícono de lápiz chico (16 dp) en cada bloque de tarea de la
  línea de tiempo, al lado del título. El área táctil queda por debajo de los 48 dp recomendados
  en bloques angostos (varias tareas superpuestas); se tomó el mismo criterio pragmático que ya
  existía para el asa de redimensionar (`RESIZE_HANDLE_HEIGHT` = 16 dp).
- En modo edición de una tarea recurrente se oculta la fecha editable, el switch "sin hora" y toda
  la sección de recurrencia (se reemplaza por un aviso de que el patrón no se edita acá): el
  patrón de recurrencia (tipo/intervalo/días/fin) no se puede cambiar desde este paso, solo el
  contenido de la tarea (título, notas, hora, duración, ícono, color).
- `CreateTaskScreen`/`CreateTaskViewModel` se reusan para crear y editar (mismo criterio que
  `RoutineEditorScreen`/`RoutineEditorViewModel` en el ticket 0010), en vez de duplicar una
  pantalla nueva.
- `./gradlew assembleDebug`, `./gradlew assembleDebugAndroidTest`, `./gradlew test`,
  `./gradlew lint` y `./gradlew connectedAndroidTest` corren en verde.
