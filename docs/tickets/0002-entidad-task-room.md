# Ticket 0002 – Entidad `task` + Room (capa de datos)

## Estado
Terminado

## Descripción
Primer paso del módulo Planificación (Fase 1): crear la entidad `task` en Room con su DAO
y repositorio, sin UI todavía. Esta es la mini-especificación 01 que menciona el documento
técnico (sección de fases) y resuelve lo que había quedado pendiente del ticket 0001
(`AppDatabase` sin anotación `@Database`, sin `DatabaseModule`).

**Alcance acotado a propósito** (no adelantar fases/funcionalidad futura):
- Sin recurrencia (`recurrenceType`, `recurrenceInterval`, `recurrenceDays`, `recurrenceEnd`) — eso es PL-08, un paso aparte.
- Sin `routineId` — rutinas son PL-09, un paso aparte.
- Sin `subtask` ni `task_occurrence` — son tablas propias de pasos posteriores (PL-05 subtareas, PL-08/PL-15 ocurrencias).
- Sin línea de tiempo ni pantallas (eso es el siguiente ticket, PL-01/PL-02).

## Checklist de cambios
- [x] Definir entidad Room `TaskEntity` (tabla `task`) con campos en inglés:
      `id` (Long, PK autogenerada), `uuid` (String, único), `title`, `notes` (nullable),
      `date` (epochDay, Long), `startMinutes` (Int, nullable = bandeja/Inbox),
      `durationMinutes` (Int), `iconKey`, `colorKey`, `status` (Int: 0 = existe, -1 = borrado lógico),
      `completedAt` (Long, nullable), `createdAt`, `updatedAt` (Long, ms)
- [x] Convertidor de tipos `epochDay` ↔ `LocalDate` (ya existía en `core/database`, se reutiliza)
- [x] DAO `TaskDao`: insertar, actualizar, borrado lógico (update de `status`), consultas por `date` y por rango de fechas, todas filtrando `status = 0`
- [x] `TaskRepository` en `planner/data` que envuelve el DAO (es lo único que verá el ViewModel más adelante)
- [x] `DatabaseModule` de Hilt: agrega `TaskEntity` a `AppDatabase`, habilita `@Database` (ahora que hay una entidad real) y expone `TaskDao`
- [x] Exportar esquema a `app/schemas/` (versión 1 de la base)
- [x] Pruebas instrumentadas del DAO con Room en memoria (insertar, listar por fecha y orden, borrado lógico, unicidad de `uuid`) — corridas en el emulador, en verde
- [x] Pruebas unitarias de `TaskRepository` (MockK sobre el DAO)
- [x] Verificar compilación: `./gradlew assembleDebug`
- [x] Correr pruebas: `./gradlew test`, `./gradlew connectedAndroidTest` y `./gradlew lint`

## Notas
- Dinero no aplica acá. Pesos tampoco.
- `uuid`: generado con `UUID.randomUUID().toString()` por defecto en el constructor de `TaskEntity`, índice único.
- Claves foráneas (cuando existan en pasos futuros) apuntan al `id` numérico, no al `uuid`.
- No se toca el manifiesto en este ticket.
- `./gradlew assembleDebug`, `./gradlew test`, `./gradlew connectedAndroidTest` y `./gradlew lint`
  corren en verde.
- Se agregó la dependencia `androidx.test.ext:junit` (faltaba para `AndroidJUnit4` en pruebas
  instrumentadas) y se corrigió `TaskDaoTest`: un test con cuerpo de expresión terminaba en
  `assertThat(...).containsExactly(...)`, que en Truth devuelve `Ordered` y no `Unit` — JUnit4
  exige métodos `void`. Se pasaron todos los tests del archivo a cuerpo de bloque.
- Se corrigió `local.properties` (fuera de Git) para escapar el separador de unidad (`C\:`) y que
  lint no lo marque como error.
- `TaskRepository.create()` recibe un `TaskEntity` ya armado (con `uuid`/`createdAt` default);
  no hay todavía una decisión de dónde se arman esos valores desde una pantalla porque no hay
  pantalla en este ticket — se retoma en el ticket de "Crear tarea" (PL-04).
