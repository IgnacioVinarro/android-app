# Ticket 0008 – Tareas recurrentes: datos + cálculo de ocurrencias (PL-08, parte 1)

## Estado
Terminado

## Descripción
Séptimo paso de Planificación. Primera mitad de PL-08 (tareas recurrentes): agrega a la
entidad `task` los campos de la regla de recurrencia, la tabla `task_occurrence` (solo existe
una fila cuando una ocurrencia se completa, se salta o se modifica, según CLAUDE.md) y la
función pura `occursOn(task, date)` que calcula si una tarea recurrente tiene ocurrencia en una
fecha dada.

**Alcance acotado a propósito** (no adelantar funcionalidad futura, mismo criterio que los
tickets 0002/0003):
- Sin pantalla para crear/editar una tarea recurrente todavía — `CreateTaskScreen` sigue creando
  solo tareas simples. Eso es un paso aparte (siguiente mini-especificación de PL-08).
- Sin conectar `occursOn` con `TaskTimelineSource` ni con la bandeja todavía — hoy ninguna tarea
  puede ser recurrente desde la UI, así que no hay nada que mostrar. Se conecta en el mismo paso
  en que exista la UI, para poder probarlo de punta a punta.
- Sin "completar una ocurrencia" ni la lógica de `task_occurrence` en el `TodayViewModel` — viene
  junto con lo anterior.
- Sin PL-15 (editar/borrar "solo esta", "esta y siguientes", "todas"): es un paso aparte,
  explícitamente separado en el documento funcional.
- Mensual con días que no existen en todos los meses (ej. tarea creada el 31): ese mes
  simplemente no tiene ocurrencia; no se corre al último día del mes. Decisión tomada para no
  sumar una regla no pedida; si el desarrollador prefiere otro comportamiento, se ajusta después.

## Checklist de cambios
- [x] `TaskEntity`: agrega `recurrenceType` (String?, nombre del enum o `null` = no recurrente),
      `recurrenceInterval` (Int?, usado por "cada N días"), `recurrenceDays` (Int?, máscara de
      días de la semana), `recurrenceEnd` (Long?, epochDay, fin opcional)
- [x] `TaskOccurrenceEntity` (tabla `task_occurrence`): `id`, `uuid`, `taskId`, `date` (epochDay),
      `skipped`, `overrideStartMinutes`, `completedAt`, `status`, `createdAt`, `updatedAt`; índice
      único por (`taskId`, `date`) y por `uuid`
- [x] `TaskOccurrenceDao`: insertar, actualizar, borrado lógico, obtener por tarea+fecha, observar
      por rango de fechas (todo filtrando `status = 0`)
- [x] `TaskOccurrenceRepository` (interfaz + implementación + módulo de Hilt), mismo patrón que
      `TaskRepository`
- [x] `AppDatabase`: sube a versión 2, agrega `TaskOccurrenceEntity`, expone `TaskOccurrenceDao`,
      migración `MIGRATION_1_2` (agrega columnas a `task`, crea `task_occurrence`)
- [x] Exportar esquema versión 2 a `app/schemas/`
- [x] `planner/domain`: `RecurrenceType` (enum: `DAILY`, `WEEKLY`, `SPECIFIC_DAYS`, `MONTHLY`,
      `EVERY_N_DAYS`) y función pura `occursOn(task, date): Boolean`
- [x] Pruebas unitarias de `occursOn`: cada tipo de recurrencia, fecha anterior al inicio, fin de
      recurrencia, tarea no recurrente
- [x] Pruebas unitarias de `TaskOccurrenceRepositoryImpl` (MockK sobre el DAO, mismo patrón que
      `TaskRepositoryImplTest`)
- [x] Prueba instrumentada de `TaskOccurrenceDao` (Room en memoria): insertar, único por
      tarea+fecha, borrado lógico, observar por rango — **escrita, no corrida** (ver nota)
- [x] Prueba instrumentada de la migración 1→2 (`MigrationTestHelper`, agrega dependencia
      `androidx.room:room-testing`) — **escrita, no corrida** (ver nota)
- [x] Verificar compilación: `./gradlew assembleDebug` (y `assembleDebugAndroidTest`, para que
      compilen también las pruebas instrumentadas nuevas)
- [x] Correr pruebas: `./gradlew test` y `./gradlew lint` — **`./gradlew connectedAndroidTest` no
      se pudo correr** (este entorno no tiene emulador ni `adb`, igual que en el ticket 0007)

## Notas
- Mensual con el día 31 (o 29/30 según el mes): si el mes de la fecha consultada no tiene ese día,
  simplemente no hay ocurrencia ese mes; no se corre al último día del mes. Es una simplificación
  a propósito, documentada en el alcance; se ajusta si preferís otro comportamiento.
- `TaskOccurrenceEntity.taskId` apunta al `id` numérico de `task`, sin `@ForeignKey` de Room (el
  proyecto todavía no usa esa anotación en ningún lado); el índice único (`taskId`, `date`) ya
  evita duplicar ocurrencias de una misma tarea en una misma fecha.
- Se agregó `androidx.room:room-testing` (mismo paquete `androidx.room`, variante oficial para
  probar migraciones con `MigrationTestHelper`, no es una librería de un tercero nuevo) y se
  configuró `sourceSets.androidTest.assets` para que apunte a `app/schemas/`, que es donde
  `MigrationTestHelper` busca el esquema "antes" de la migración.
- No se pudo correr `./gradlew connectedAndroidTest` ni las pruebas instrumentadas nuevas
  (`TaskOccurrenceDaoTest`, `MigrationTest`) porque este entorno no tiene `adb` ni un dispositivo
  conectado (igual que en el ticket 0007). Si compilan (`assembleDebugAndroidTest` corrió en
  verde) pero quedan sin ejecutar hasta que las corras vos en el emulador/teléfono real.
- `./gradlew assembleDebug`, `./gradlew assembleDebugAndroidTest`, `./gradlew test` y
  `./gradlew lint` corren en verde.
