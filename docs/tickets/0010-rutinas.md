# Ticket 0010 – Rutinas: crear, editar y aplicar a un día (PL-09)

## Estado
Terminado

## Descripción
Noveno paso de Planificación. Permite armar una rutina (por ejemplo "Rutina de mañana") como un
conjunto de ítems con horarios relativos a una hora ancla propia de la rutina, y aplicarla a un
día con un toque: eso crea tareas normales, enlazadas por `routineId`, iguales a las que se crean
desde "Nueva tarea". Editar la rutina después no cambia las tareas ya creadas (CLAUDE.md).

**Decisión de alcance confirmada con el desarrollador:**
- Cada ítem de la rutina guarda un `offsetMinutes` relativo a la hora ancla de la rutina
  (`routine.startMinutes`). El editor muestra/edita ese horario como una hora absoluta (más fácil
  de usar), pero internamente se guarda como offset: si después cambiás la hora ancla de la
  rutina, todos los ítems se corren juntos la próxima vez que la apliques.
- "Aplicar a un día con un toque" hoy solo puede ser "hoy", porque la navegación por día (PL-12)
  todavía no existe (quedó pospuesta, ver conversación). Cuando se haga PL-12, aplicar a otro día
  es un ajuste chico sobre la misma función pura.

**Alcance acotado a propósito** (mismo criterio que tickets anteriores):
- Sin reordenar ítems arrastrando: el orden es el de creación/edición (`position` incremental).
- Sin subtareas en los ítems (subtareas en general todavía no existen, PL-04 queda para otro paso).
- Sin recurrencia propia de la rutina (aplicarla es siempre manual, un toque por vez); programar
  una rutina automáticamente ciertos días de la semana no es lo que pide PL-09 (eso ya lo cubre la
  recurrencia de tareas individuales, PL-08).
- Aplicar la misma rutina dos veces al mismo día crea tareas duplicadas sin aviso: no hay
  protección contra duplicados en este paso.
- Borrar una rutina (borrado lógico) no borra ni modifica las tareas que ya generó: quedan con su
  `routineId` igual que antes, consistente con "editar la rutina no cambia las ya creadas".
- Sin confirmación al borrar una rutina o un ítem del editor (acción directa, reversible solo a
  nivel de base de datos).

## Checklist de cambios
- [x] `RoutineEntity` (tabla `routine`): `id`, `uuid`, `name`, `startMinutes` (hora ancla),
      `status`, `createdAt`, `updatedAt`
- [x] `RoutineItemEntity` (tabla `routine_item`): `id`, `uuid`, `routineId`, `title`,
      `offsetMinutes`, `durationMinutes`, `iconKey`, `colorKey`, `position`, `status`,
      `createdAt`, `updatedAt`; índice único por `uuid`
- [x] `RoutineDao` / `RoutineItemDao`: insertar, actualizar, borrado lógico, obtener por id,
      observar todas (`routine`) / observar por rutina ordenadas por `position` (`routine_item`)
- [x] `RoutineRepository` / `RoutineItemRepository` (interfaz + implementación + módulo Hilt),
      mismo patrón que `TaskRepository`/`TaskOccurrenceRepository`
- [x] `TaskEntity`: agrega `routineId` (Long?, nulo = no viene de una rutina)
- [x] `AppDatabase`: sube a versión 3, agrega las dos tablas nuevas y la columna de `task`,
      migración `MIGRATION_2_3`, exporta esquema versión 3
- [x] `planner/domain`: función pura `applyRoutine(routine, items, date): List<TaskEntity>` que
      calcula `startMinutes` de cada tarea (ancla + offset, clamp dentro del día) y arma las
      `TaskEntity` con `routineId`
- [x] `core/ui`: extraer los selectores de ícono y color de `CreateTaskScreen` a un componente
      compartido (`TaskIconSelector`/`TaskColorSelector`) y reusarlo en el editor de rutinas, para
      no duplicar la paleta (regla de reuso de CLAUDE.md)
- [x] `RoutinesViewModel` + `RoutinesScreen`: lista de rutinas con su hora ancla, botón "Aplicar
      hoy" por rutina, botón "+" para crear una nueva, acceso a editar y borrar
- [x] `RoutineEditorViewModel` + `RoutineEditorScreen`: nombre, hora ancla, lista de ítems
      (agregar/editar/quitar cada uno con título, hora, duración, ícono y color), guardar
- [x] Entrada de navegación: nueva ruta a `RoutinesScreen` desde `TodayScreen` (segundo botón
      flotante) y rutas de `RoutinesScreen` → `RoutineEditorScreen`
- [x] Textos nuevos en `strings.xml`
- [x] Pruebas unitarias: `applyRoutine` (offsets, clamp, varios ítems), `RoutineRepositoryImpl` /
      `RoutineItemRepositoryImpl` (MockK), `RoutinesViewModel` (aplicar hoy, borrar),
      `RoutineEditorViewModel` (cargar rutina existente, agregar/editar/quitar ítems, guardar,
      validación)
- [x] Prueba instrumentada nueva de `RoutineDao`/`RoutineItemDao` (Room en memoria) y de la
      migración 2→3 — corrida en un emulador conectado, en verde
- [x] Verificar compilación: `./gradlew assembleDebug` (y `assembleDebugAndroidTest`)
- [x] Correr pruebas: `./gradlew test` y `./gradlew lint`
- [x] Probar manualmente en el emulador/teléfono: crear una rutina con varios ítems, aplicarla a
      hoy, confirmar que aparecen como tareas normales en la línea de tiempo; editar la rutina y
      confirmar que las tareas ya aplicadas no cambian; borrar una rutina y confirmar que sus
      tareas ya creadas siguen intactas

## Notas
- Había un emulador conectado durante este paso (a diferencia de los tickets 0007-0009), así que
  esta vez sí se pudieron correr las pruebas instrumentadas nuevas y las que habían quedado
  pendientes de esos tickets (`TaskDaoTest`, `MigrationTest` 1→2, `TaskOccurrenceDaoTest`):
  15 pruebas instrumentadas en total, todas en verde (`./gradlew connectedAndroidTest`).
- Al guardar una rutina existente, el ViewModel parte de la entidad ya cargada (`loadedRoutine`/
  `loadedItemsById`) y la copia con los campos nuevos, en vez de construir una entidad desde cero:
  así no se pierde el `uuid` ni el `createdAt` originales en cada edición (importante para la
  sincronización futura).
- La hora de cada ítem se muestra y edita como hora absoluta en el editor, pero se guarda como
  `offsetMinutes` relativo a la hora ancla de la rutina (`routine.startMinutes`): si después
  cambiás la hora ancla, todos los ítems se corren juntos la próxima vez que se aplique la rutina.
- Se extrajeron `TaskIconSelector`/`TaskColorSelector` a `core/ui/StyleSelectors.kt` y
  `CreateTaskScreen` se actualizó para usarlos, eliminando la duplicación que iba a aparecer al
  necesitar el mismo selector en el editor de rutinas.
- `./gradlew assembleDebug`, `./gradlew assembleDebugAndroidTest`, `./gradlew test`,
  `./gradlew lint` y `./gradlew connectedAndroidTest` corren en verde.
