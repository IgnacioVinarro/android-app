# Ticket 0003 – Línea de tiempo: `TimelineBlock`/`TimelineSource` + acomodo de superposiciones

## Estado
Terminado

## Descripción
Segundo paso de Planificación (sigue al ticket 0002, entidad `task`). Arma la arquitectura de
línea de tiempo que describe el documento técnico (sección 6): el modelo `TimelineBlock`, la
interfaz `TimelineSource`, el repositorio que combina las fuentes registradas por Hilt, la
función pura de acomodo de bloques superpuestos, y la primera fuente real (`TaskTimelineSource`,
que convierte las tareas con hora en bloques).

**Alcance acotado a propósito** (no adelantar fases/funcionalidad futura):
- Sin pantalla todavía (PL-01/PL-02 visual es el siguiente ticket). Este paso es dominio puro +
  conexión con Room, todo con pruebas unitarias, sin Compose.
- Solo la fuente de Planificación (`TaskTimelineSource`). Gimnasio, Alimentación y Meditación
  todavía no tienen tablas propias, así que no aportan fuente — se suman cuando exista su
  entidad, sin tocar el núcleo (`TimelineRepository` ya queda listo para que se sumen solas).
- Sin recurrencia (`occurrencesOn`) ni rutinas: la fuente solo lee tareas ya guardadas con
  `startMinutes` no nulo.

## Checklist de cambios
- [x] `TimelineBlock` en `core/timeline` (campos exactos del documento técnico: `id`, `source`,
      `date`, `startMinutes`, `durationMinutes`, `title`, `iconKey`, `colorKey`, `completed`, `route`)
- [x] Interfaz `TimelineSource` (`blocksFor(date): Flow<List<TimelineBlock>>`) en `core/timeline`
- [x] `TimelineRepository` en `core/timeline`: recibe el `Set<TimelineSource>` (multibinding de
      Hilt), combina los `Flow` y ordena por `startMinutes`
- [x] Función pura de acomodo de superposiciones (`OverlapArranger`) en `core/timeline`: agrupa
      bloques que se pisan y asigna columna + cantidad de columnas; se prueba sin Android
- [x] `TaskTimelineSource` en `planner/domain`: implementa `TimelineSource`, filtra tareas sin
      hora (van a la bandeja) y mapea el resto a `TimelineBlock`
- [x] Módulo de Hilt que registra `TaskTimelineSource` en el conjunto (`@IntoSet`)
- [x] Pruebas unitarias de `OverlapArranger`: sin superposición, dos bloques que se pisan, tres
      bloques con reuso de columna, clusters separados que no se afectan entre sí
- [x] Pruebas unitarias de `TaskTimelineSource` (MockK sobre `TaskRepository`, Turbine sobre el Flow)
- [x] Pruebas unitarias de `TimelineRepository` (fuentes falsas, Turbine)
- [x] Verificar compilación: `./gradlew assembleDebug`
- [x] Correr pruebas: `./gradlew test`

## Notas
- El bloque usa el `uuid` de la tarea como `TimelineBlock.id` (único) y el `id` numérico en la
  `route` de navegación (`planner/task/{id}`), seguido de la regla "las claves apuntan al id
  numérico, el uuid es solo para sincronización".
- `completed` del bloque sale de `completedAt != null` de la tarea.
- Se agregó la dependencia `kotlinx-coroutines-test` (faltaba para `runTest`, usado junto con
  Turbine en las pruebas de `Flow`).
- `./gradlew assembleDebug`, `./gradlew test`, `./gradlew connectedAndroidTest` (regresión del
  DAO, sin pruebas nuevas en este ticket) y `./gradlew lint` corren en verde.
