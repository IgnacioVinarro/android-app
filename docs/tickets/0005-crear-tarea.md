# Ticket 0005 – Crear tarea (PL-04)

## Estado
Terminado

## Descripción
Cuarto paso de Planificación. Agrega la pantalla para crear una tarea desde la propia app
(hasta ahora las tareas de prueba se insertaban a mano en la base). Con esto la línea de tiempo
deja de depender de datos cargados manualmente.

**Alcance acotado a propósito** (no adelantar fases/funcionalidad futura):
- Sin subtareas (la tabla `subtask` todavía no existe, ver nota del ticket 0002) ni recurrencia
  ni rutinas — campos aparte, con su propio ticket (PL-08, PL-09).
- Sin opción "sin hora" (bandeja/Inbox, PL-06): esta pantalla siempre pide hora de inicio. La
  entidad ya soporta `startMinutes` nulo, pero la bandeja (pantalla para verlas y arrastrarlas)
  es un requisito aparte.
- Sin editar ni borrar tareas todavía (solo alta). Tocar un bloque en la línea de tiempo sigue
  completando/descompletando (ticket 0004); editar el resto de sus datos queda para cuando se
  decida cómo convive con esa interacción.
- El selector de ícono/color y el diálogo de hora quedan en `core/ui` por la regla de
  reutilización de CLAUDE.md: los va a necesitar cualquier otro módulo que cree sus propios
  eventos (sesión de gimnasio, comida, etc.).

## Checklist de cambios
- [x] `core/ui/TaskStyleKeys.kt`: reemplaza el mapa provisorio de `BlockStyle.kt` (ticket 0004) por
      una paleta de colores y una lista de íconos reales, como constantes (`enum class`), con los
      mismos `colorForKey`/`iconForKey` para no tocar `TodayScreen`
- [x] `core/ui/TimePickerDialog.kt`: diálogo reutilizable que envuelve `TimePicker` de Material 3
      (no trae uno armado)
- [x] `CreateTaskViewModel` (`planner/ui`): estado del formulario, duración por defecto leída de
      `SettingsRepository.defaultTaskDuration`, valida título no vacío, guarda con
      `TaskRepository.create`
- [x] `CreateTaskScreen`: título, fecha (selector), hora de inicio (selector), duración en
      minutos, ícono, color, notas opcionales; botón "Guardar" deshabilitado hasta que el título
      no esté vacío
- [x] Selección de ícono/color con un indicador que no sea solo el color (borde o tilde), por la
      regla de accesibilidad de CLAUDE.md
- [x] Botón flotante "+" en `TodayScreen` que abre `CreateTaskScreen`; al guardar, vuelve a "Hoy"
- [x] Ruta nueva en el `NavHost` (`planner/task/new`)
- [x] Textos nuevos en `strings.xml` (español Argentina)
- [x] Pruebas unitarias de `CreateTaskViewModel`: toma la duración por defecto de Ajustes, valida
      que no se pueda guardar con título vacío, arma el `TaskEntity` correctamente al guardar
- [x] Verificar compilación: `./gradlew assembleDebug`
- [x] Correr pruebas: `./gradlew test`, `./gradlew connectedAndroidTest` y `./gradlew lint`
- [x] Probar manualmente en el emulador: crear una tarea desde la pantalla, confirmar que aparece
      en la línea de tiempo con los datos cargados

## Notas
- La paleta de colores anterior (ticket 0004) reusaba los colores de módulo
  (`PlannerColor`, `FinanceColor`, etc.) como si fueran opciones para el usuario, lo cual no tenía
  sentido para elegir el color de una tarea propia. Esta paleta nueva es independiente de los
  colores de módulo.
- Probado manualmente en el emulador de punta a punta: tocar "+", completar título/ícono/color,
  "Guardar" (deshabilitado hasta cargar el título), volver a "Hoy" y ver la tarea nueva
  ("Pagar expensas", ícono casa, color rojo) en su horario correcto dentro de la línea de tiempo.
- Nota de proceso: `connectedAndroidTest` desinstala la app al terminar (comportamiento normal de
  Gradle/AGP), así que si se corrió antes de probar a mano hay que volver a `./gradlew installDebug`.
- `./gradlew assembleDebug`, `./gradlew test`, `./gradlew connectedAndroidTest` y
  `./gradlew lint` corren en verde.
