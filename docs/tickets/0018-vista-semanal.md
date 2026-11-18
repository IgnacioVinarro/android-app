# Ticket 0018 – Vista semanal con navegación por día y resumen de carga (PL-12)

## Estado
En revisión

## Descripción
PL-12: "Vista semanal con navegación por día y resumen de carga". El documento funcional no define
los detalles; las decisiones siguientes las confirmó el desarrollador.

**Decisiones de alcance confirmadas con el desarrollador:**
- **Dónde:** dentro de Hoy. Una tira de 7 días (lunes a domingo) arriba de la pantalla; tocar un día
  cambia la línea de tiempo y la bandeja a ese día. No hay pantalla ni destino nuevo.
- **Resumen de carga:** por día, **cantidad de tareas + minutos planificados** (ej. "4 tareas · 3 h"),
  con una barrita proporcional al día más cargado de la semana.
- **Qué cuenta:** solo tareas de Planificación (simples + ocurrencias de recurrentes no saltadas). La
  bandeja cuenta en la cantidad pero no suma minutos (sin hora no hay duración planificada).
- **Navegación:** semana anterior / siguiente y botón "Hoy" para volver. La semana empieza el lunes.

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Regla pura en `planner/domain` (`WeekLoad.kt`): `weekStartOf`, `buildWeekLoad` (usa `occursOn` y los
  overrides de `task_occurrence`) y `DayLoad` (con la fracción para la barra).
- Nueva consulta `observeForRange(start, end)`: tareas simples del rango + recurrentes ya iniciadas.
- `TodayViewModel` guarda la **fecha seleccionada** (por defecto hoy) y todo el estado del día (bloques,
  bandeja, progreso, subtareas) se calcula para esa fecha. "Atrasadas" sigue midiéndose contra el
  **hoy real**, no contra el día elegido.
- La línea roja de "ahora" solo se dibuja cuando el día seleccionado es hoy.
- La bandeja de un día ahora filtra las recurrentes sin hora con `occursOn` (antes aparecían todos los
  días aunque no les tocara; con navegación por día se notaría).
- Cada celda mide al menos 48 dp, el día elegido se marca con fondo **y** texto en negrita, y la
  descripción accesible lleva fecha, cantidad y minutos (el color no es el único indicador).

**Fuera de este ticket (pasos aparte):** crear una tarea con la fecha del día seleccionado (el
formulario sigue abriendo en hoy) y aplicar una rutina a otro día (ticket 0010).

## Checklist de cambios
- [x] `planner/domain` (`WeekLoad.kt`): `weekStartOf`, `buildWeekLoad`, `DayLoad` + pruebas
- [x] `TaskDao` / `TaskRepository`: `observeForRange(start, end)` + pruebas (unitaria y DAO instrumentada)
- [x] `TodayViewModel`: fecha seleccionada, carga de la semana, `onSelectDay`, `onPreviousWeek`,
      `onNextWeek`, `onGoToToday` + pruebas
- [x] `planner/ui/WeekStrip.kt` y su uso en `TodayScreen` (marca "ahora" solo en hoy)
- [x] Textos nuevos en `strings.xml`
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Correr `./gradlew connectedAndroidTest` — necesita emulador o teléfono conectado
- [ ] Probar manualmente en el emulador/teléfono (abajo)

## Lista de comprobación manual
1. Arriba de Hoy se ve la semana actual (lunes a domingo) con hoy marcado y seleccionado.
2. Tocar otro día: la línea de tiempo, la bandeja y el progreso pasan a ese día; la línea roja de
   "ahora" desaparece si no es hoy.
3. Cada día muestra su cantidad de tareas y una barra; el resumen del día elegido dice
   "N tareas · X h".
4. Una tarea recurrente cuenta en los días que le tocan y no en los demás; una ocurrencia saltada no cuenta.
5. Una tarea sin hora cuenta en la cantidad pero no suma horas.
6. Flechas: la semana avanza/retrocede y el día elegido se mantiene en el mismo día de la semana.
7. "Hoy" aparece solo si el día elegido no es hoy y vuelve a hoy.
8. Completar una tarea o una subtarea en un día futuro/pasado se guarda en ese día (recurrentes: solo esa ocurrencia).
9. Con TalkBack: cada día lee la fecha, la cantidad de tareas, los minutos y si está seleccionado.
