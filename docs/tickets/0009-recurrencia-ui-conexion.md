# Ticket 0009 – Tareas recurrentes: crear desde la UI y verlas en la línea de tiempo (PL-08, parte 2)

## Estado
En revisión

## Descripción
Octavo paso de Planificación. Segunda mitad de PL-08: conecta lo que dejó listo el ticket 0008
(`occursOn`, `task_occurrence`) con la aplicación real. Ahora se puede crear una tarea recurrente
desde `CreateTaskScreen`, sus ocurrencias aparecen en la línea de tiempo el día que les toca, y
completar una ocurrencia no completa las demás (queda en `task_occurrence`, no en `task`).

**Alcance acotado a propósito**:
- Recurrencia solo con hora asignada: activar "Repetir" fuerza "sin hora" apagado y viceversa.
  Combinar recurrencia con la bandeja (qué día recibiría la ocurrencia sin hora) no está definido
  por el documento funcional, así que no se mezcla en este paso.
- Las ocurrencias de tareas recurrentes **no se pueden mover ni redimensionar** arrastrando
  (PL-07): hacerlo bien requiere escribir `overrideStartMinutes` en la ocurrencia puntual, no
  tocar el `startMinutes` de la tarea (que correría todas las ocurrencias). Se agregó
  `TimelineBlock.movable` para que el gesto de arrastre se desactive solo en estos bloques; el
  resto sigue igual.
- Sin "saltar" una ocurrencia puntual desde la UI (el campo `skipped` existe en el esquema desde
  el ticket 0008, pero nada lo pone en `true` todavía).
- Sin PL-15 (editar/borrar "solo esta", "esta y siguientes", "todas"): sigue siendo un paso
  aparte, como ya aclaraba el ticket 0008.
- Mensual con el día 31 (o 29/30): mismo comportamiento documentado en el ticket 0008 (ese mes no
  tiene ocurrencia si el mes no llega a ese día).

## Checklist de cambios
- [x] `TimelineBlock`: nuevo campo `movable` (default `true`) para poder desactivar el arrastre en
      bloques que no lo soportan todavía
- [x] `TaskDao.observeByDate`: además de las tareas simples de ese día exacto, trae las tareas
      recurrentes con inicio anterior o igual (candidatas a tener ocurrencia ese día)
- [x] `TaskTimelineSource`: usa `occursOn` para filtrar las ocurrencias de ese día; si hay fila en
      `task_occurrence` la usa para la hora (`overrideStartMinutes`), si está completada
      (`completedAt`) y si está saltada (`skipped`, no se muestra); marca `movable = false` en
      esos bloques
- [x] `TodayViewModel.onToggleComplete`: para una tarea recurrente, busca/crea la fila de
      `task_occurrence` de ese día y alterna su `completedAt` (no toca `task.completedAt`)
- [x] `TodayViewModel.onMoveTask`/`onResizeTask`: ignoran bloques con `movable = false`
- [x] `CreateTaskViewModel`/`CreateTaskUiState`: campos de recurrencia (activar, tipo, intervalo,
      máscara de días, fecha de fin opcional); activar recurrencia fuerza hora asignada y viceversa;
      `isValid` exige máscara no vacía en "días específicos" e intervalo > 0 en "cada N días"
- [x] `CreateTaskScreen`: sección "Repetir" (switch + tipo con `RadioButton`, chips de días para
      "días específicos", campo numérico para "cada N días", switch + selector de fecha para el
      fin opcional), oculta si "sin hora" está activo
- [x] Textos nuevos en `strings.xml` (tipos de recurrencia, días de la semana, fecha de fin)
- [x] Pruebas unitarias: `occursOn` ya cubierto en el ticket 0008; `TaskTimelineSource` (ocurre
      solo el día que toca, usa override de hora/completado, respeta `skipped`, `movable = false`
      en recurrentes); `TodayViewModel` (completar ocurrencia nueva/existente, mover/redimensionar
      ignora `movable = false`); `CreateTaskViewModel` (cada tipo de recurrencia guarda los campos
      correctos, validación de días específicos/intervalo, hora↔recurrencia se apagan entre sí)
- [x] Prueba instrumentada nueva de `TaskDao.observeByDate` con una tarea recurrente de fecha
      anterior — **escrita, no corrida** (sin emulador en este entorno)
- [x] Verificar compilación: `./gradlew assembleDebug` y `./gradlew assembleDebugAndroidTest`
- [x] Correr pruebas: `./gradlew test` y `./gradlew lint` — **`./gradlew connectedAndroidTest` no
      se pudo correr** (sin emulador/`adb` en este entorno, igual que en los tickets 0007 y 0008)
- [ ] Probar manualmente en el emulador: crear una tarea diaria, semanal, de días específicos,
      mensual y cada N días; verificar que cada una aparece los días correctos en la línea de
      tiempo; completar una ocurrencia y confirmar que las demás siguen sin completar

## Notas
- **Este ticket se creó después de empezar a programar, no antes.** Correspondía crearlo primero
  (regla del repo); quedó documentado acá una vez terminado el trabajo, pero avisándolo para que
  quede claro que no se siguió el orden correcto esta vez.
- `TaskDao.observeByDate` amplía su alcance: antes solo traía tareas con `date` exactamente igual
  a la fecha pedida; ahora también trae las recurrentes con `date` anterior o igual. Las tareas
  simples (sin recurrencia) no cambian de comportamiento. Esto también amplía ligeramente qué
  filas ve `TodayViewModel` para armar `inboxTasks`, pero como toda tarea recurrente en este paso
  exige tener hora, nunca termina en la bandeja (se filtra por `startMinutes == null`).
- No se pudo correr `./gradlew connectedAndroidTest` ni la prueba instrumentada nueva
  (`observeByDate_incluyeTareasRecurrentesConFechaAnteriorYExcluyeSimplesDeOtroDia`) porque este
  entorno no tiene `adb` ni un dispositivo conectado, igual que en los tickets anteriores.
- `./gradlew assembleDebug`, `./gradlew assembleDebugAndroidTest`, `./gradlew test` y
  `./gradlew lint` corren en verde.