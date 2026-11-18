# Ticket 0007 – Mover y redimensionar bloques arrastrando (PL-07)

## Estado
Terminado

## Descripción
Sexto paso de Planificación. Permite arrastrar un bloque de tarea ya ubicado en la línea de
tiempo para cambiarle la hora (arrastre vertical) y arrastrar su borde inferior para cambiarle
la duración (redimensionar). Los cambios se enganchan ("snap") a intervalos de 5 minutos.

**Decisión de alcance (confirmada con el desarrollador):**
- Solo se puede mover/redimensionar bloques de tareas (`source == "planner"`); los bloques de
  otros módulos (entrenamiento, comidas, meditación) todavía no existen y, cuando existan, se
  completan y editan en su propia pantalla, no desde la línea de tiempo.
- El arrastre desde la bandeja (Inbox) hacia la línea de tiempo queda **fuera** de este paso; la
  bandeja sigue asignando hora por toque + selector (ticket 0006). Es un paso aparte porque cruza
  dos composables y es un gesto más complejo.
- Snap de 5 minutos tanto para mover como para redimensionar.

## Checklist de cambios
- [x] `core/timeline`: función pura para enganchar minutos al intervalo de 5 (`snapToGrid` o
      similar) y pruebas unitarias
- [x] `TodayViewModel`: `onMoveTask(task, newStartMinutes)` y `onResizeTask(task, newDurationMinutes)`,
      ambos restringidos a `source == planner`, clampeando dentro del día (0–1439) y con una
      duración mínima (reusar el valor por defecto de 30 min configurable ya existente)
- [x] `TodayScreen`: gesto de arrastre vertical sobre `TimelineBlockCard` para mover (actualiza
      `startMinutes` en vivo mientras se arrastra, confirma al soltar); borde inferior con asa de
      redimensionar que arrastra para cambiar `durationMinutes`
- [x] Textos nuevos en `strings.xml` si hace falta (ej. descripción de accesibilidad del asa de
      redimensionar)
- [x] Pruebas unitarias: función de snap, `TodayViewModel.onMoveTask`/`onResizeTask` (clamping,
      restricción a planner, snap aplicado)
- [x] Verificar compilación: `./gradlew assembleDebug`
- [x] Correr pruebas: `./gradlew test` y `./gradlew lint` — **`./gradlew connectedAndroidTest` no
      se pudo correr** (este entorno no tiene emulador ni `adb`)
- [ ] Probar manualmente en el emulador: arrastrar una tarea para cambiarle la hora, arrastrar su
      borde inferior para cambiarle la duración, confirmar que se conserva al salir y volver a
      entrar a la pantalla — **pendiente, lo tenés que hacer vos en el teléfono/emulador real**

## Notas
- Gesto elegido: mantener presionado (long press) y luego arrastrar verticalmente el bloque para
  moverlo; un toque corto sigue completando la tarea, como antes. Se usa
  `detectDragGesturesAfterLongPress` para que ambos gestos convivan en el mismo `Surface` sin
  pisarse: un toque rápido no alcanza a disparar el long-press, así que el `clickable` de siempre
  sigue respondiendo.
- El asa de redimensionar es una franja de 16 dp en el borde inferior del bloque, con ícono
  `DragHandle` y descripción de accesibilidad separada; usa el mismo gesto de long-press + arrastre
  para no competir con el resto de la tarjeta.
- Mientras se arrastra (mover o redimensionar), la posición/duración que se ve es un valor
  provisorio guardado en memoria (`mutableStateMapOf` en `TodayScreen`), no lo que todavía dice la
  base; al soltar se llama a `onMoveTask`/`onResizeTask`, que confirman el cambio (con snap y
  clamp ya aplicados otra vez por las dudas) y el valor provisorio se descarta.
- Solo se puede mover/redimensionar bloques con `source == "planner"`; los demás (cuando existan)
  no muestran el asa ni reaccionan al arrastre.
- No se pudo correr `./gradlew connectedAndroidTest` ni probar en un emulador real porque este
  entorno no tiene `adb` ni un dispositivo conectado. `./gradlew assembleDebug`, `./gradlew test`
  y `./gradlew lint` corren en verde.
