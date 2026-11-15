# Ticket 0004 – Línea de tiempo: pantalla "Hoy" (PL-01, PL-02)

## Estado
Terminado

## Descripción
Tercer paso de Planificación. Reemplaza la pantalla provisoria de "Hoy" por la línea de tiempo
vertical real: escala horaria, marca de "ahora" que avanza sola (PL-01), y las tareas dibujadas
como bloques cuyo alto es proporcional a su duración, con ícono y color (PL-02). Usa la
arquitectura del ticket 0003 (`TimelineRepository`, `OverlapArranger`).

**Alcance acotado a propósito** (no adelantar fases/funcionalidad futura):
- Sin arrastrar, redimensionar ni crear tareas tocando un hueco libre (PL-03, PL-07): eso son
  pasos aparte.
- Sin navegar al detalle de la tarea al tocar el bloque (no existe esa pantalla todavía, es
  PL-04): por ahora, tocar un bloque de tarea solo alterna completada/no completada (la única
  acción que la línea de tiempo hace directamente, según el documento técnico).
- Sin bandeja (Inbox) visible todavía (PL-06): las tareas sin hora simplemente no aparecen en esta
  pantalla (ya las filtra `TaskTimelineSource` del ticket anterior).
- Sin vista semanal (PL-12) ni widget (PL-16).

## Checklist de cambios
- [x] `core/ui/BlockStyle.kt`: resolución mínima de `iconKey`/`colorKey` a `ImageVector`/`Color`
      (lista chica, se completa cuando exista el selector de ícono/color de PL-04)
- [x] `TodayViewModel` (`planner/ui`): combina `TimelineRepository.blocksFor(hoy)` con un tick de
      "ahora" (cada 30 s) en un único `StateFlow<TodayUiState>`; expone `onToggleComplete`
- [x] `TodayScreen`: escala horaria vertical (0 a 24, con scroll), marca de "ahora", bloques
      posicionados con `OverlapArranger` (alto proporcional a la duración, ancho repartido por
      columna), tocar el bloque completa/descompleta la tarea
- [x] Altura mínima de bloque de 48 dp (regla de accesibilidad de CLAUDE.md) aunque la tarea dure
      menos, sin mover su posición vertical real
- [x] Tarea completada: no solo cambia el color (también tacha el texto), por la regla de que el
      color nunca es el único indicador
- [x] Pruebas unitarias de `TodayViewModel` (Turbine + MockK): arma el estado con los bloques
      posicionados, `onToggleComplete` completa y descompleta correctamente, ignora bloques que no
      son de Planificación
- [x] Verificar compilación: `./gradlew assembleDebug`
- [x] Correr pruebas: `./gradlew test`, `./gradlew connectedAndroidTest` y `./gradlew lint`
- [x] Probar manualmente en el emulador: tareas de prueba insertadas a mano directo en la base
      (con `adb` + Python, porque PL-04 todavía no existe pantalla de alta), confirmando hora,
      alto, color, ícono, superposición en columnas, altura mínima táctil, marca de "ahora" y
      toggle de completado (con tachado, no solo color)

## Notas
- `onToggleComplete` solo actúa si `block.source == TIMELINE_SOURCE_PLANNER`; para los demás
  módulos no hace nada (todavía no existen esas fuentes).
- El id de tarea se obtiene parseando el último segmento de `block.route`
  (`"planner/task/{id}"`), siguiendo la convención del ticket 0003.
- Prueba manual: se insertaron 5 tareas de prueba directo en `stayfocused.db` (sacando el archivo
  del dispositivo con `adb` + `run-as`, editando con el módulo `sqlite3` de Python, y devolviéndolo)
  para verificar la pantalla sin tener todavía el alta de tareas (PL-04). Se confirmó: hora/alto/
  color/ícono correctos, dos tareas superpuestas (9:00 y 9:30) en columnas separadas, una tarea de
  15 min respeta el alto mínimo de 48 dp, la marca de "ahora" se ubica bien, y tocar un bloque lo
  completa (atenuado + tachado) y volver a tocarlo lo descompleta. Se vio un parpadeo puntual de
  los íconos de la barra de estado superpuesto dentro de un bloque en dos capturas de pantalla
  consecutivas; no se reprodujo en capturas posteriores y no tiene relación con el código de la
  pantalla (ningún composable de `TodayScreen` dibuja esos íconos) — parece un artefacto de
  composición del emulador, no un bug de la app.
- Los datos de prueba quedan en la base del emulador; se pueden borrar con "Borrar almacenamiento"
  de la app en Ajustes de Android, o reinstalando, antes de seguir con el próximo ticket.
