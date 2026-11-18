# Ticket 0012 – Recordatorios por tarea: datos y formulario (PL-10, parte 1)

## Estado
Terminado

## Descripción
Primera mitad de PL-10 (recordatorios por tarea: "a la hora, X minutos antes, o varias por tarea").
Este paso **no programa ninguna alarma ni muestra notificaciones**: solo deja listo dónde se guardan
los recordatorios y cómo se cargan desde el formulario de tarea. La segunda mitad (ticket 0013:
`AlarmScheduler`, `ReminderReceiver`, notificación con Completar/Posponer, permisos en contexto,
`BootReceiver` + `RescheduleWorker`) se apoya en esto. Mismo criterio de partición que 0008/0009.

**Decisiones de alcance confirmadas con el desarrollador:**
- Dos tickets: 0012 (datos + formulario) y 0013 (programar, avisar, reprogramar).
- Opciones de aviso: **lista fija y minutos libres**. Lista fija = a la hora, 5, 10, 15, 30 min,
  1 hora y 1 día antes; además, un campo para escribir cualquier cantidad de minutos antes. Una
  tarea puede tener **varios** recordatorios.
- Los recordatorios **pertenecen a la tarea, no a cada ocurrencia** (opción A elegida):
  - Tarea simple: se editan como cualquier otro campo.
  - Tarea recurrente, "todas": se pisan los recordatorios de la tarea.
  - Tarea recurrente, "esta y siguientes": la tarea nueva se lleva los recordatorios editados; la
    original conserva los suyos.
  - Tarea recurrente, "solo esta": los recordatorios **no cambian** (la ocurrencia hereda los de la
    tarea). No hay override de recordatorios por ocurrencia; si hace falta, es un paso aparte.
- Notificación con acciones "Completar" y "Posponer": **entra en el 0013**, no acá.

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Tabla `reminder` con los campos del documento técnico: `ownerType`, `ownerId`, `minutesBefore`,
  `kind` (aviso o alarma), `soundKey`. En este paso solo se escribe `kind = "notice"` y `soundKey`
  queda nulo; `"alarm"` y el sonido llegan con PL-11. `ownerType = "task"` (la tabla también va a
  servir a otros módulos). `ownerId` apunta al `id` numérico, no al `uuid`.
- `minutesBefore = 0` significa "a la hora". Tope de lo que se puede escribir: **7 días**
  (10080 min), una constante fácil de cambiar. No se permite repetir el mismo valor en una tarea.
- Solo las tareas **con hora** tienen recordatorios (no hay "hora de aviso" sin hora de inicio). Si
  se activa "sin hora", la sección se oculta y al guardar no se guarda ningún recordatorio.
- Borrar una tarea (o "esta y siguientes" desde la primera ocurrencia, que equivale a "todas")
  borra lógicamente también sus recordatorios: las filas hijas no se eliminan solas (CLAUDE.md).
- Las tareas que genera una rutina no traen recordatorios (las rutinas no los guardan todavía).
- La función pura de "próximo disparo" (calcula cuándo suena cada recordatorio mirando recurrencia,
  ocurrencias saltadas y overrides de hora) **pasa al 0013**, donde se usa por primera vez; hacerla
  acá sería código sin consumidor.

## Checklist de cambios
- [x] `ReminderEntity` (tabla `reminder`): campos comunes + `ownerType`, `ownerId`,
      `minutesBefore`, `kind`, `soundKey`; índice único en `uuid` e índice en
      (`ownerType`, `ownerId`)
- [x] `ReminderDao`: insertar, listar activos por dueño (`status = 0`), borrado lógico por id y por
      dueño
- [x] `ReminderRepository`: `getByOwner`, `replaceForOwner` (deja el conjunto de `minutesBefore`
      pedido: borra lógicamente los que sobran, inserta los que faltan y no toca los que ya
      están) y `deleteByOwner`; módulo Hilt
- [x] `AppDatabase`: sube a versión 5, migración `MIGRATION_4_5` (crea `reminder`), exporta
      esquema versión 5
- [x] `planner/domain` (`ReminderOptions.kt`): funciones puras — opciones fijas, validación de
      `minutesBefore` (0..tope) y descomposición en unidad (minutos/horas/días) para mostrar
- [x] `CreateTaskViewModel`: estado con la lista de recordatorios elegidos; carga los de la tarea
      al editar; los guarda al crear y al editar (simple, "todas", "esta y siguientes"); no los
      toca con "solo esta"; los borra al borrar la tarea
- [x] `CreateTaskScreen`: sección "Recordatorios" (chips de la lista fija, chips de valores libres
      ya elegidos y chip "Otro…" que abre un diálogo para escribir los minutos); aviso en tareas
      recurrentes de que con "solo esta" no cambian
- [x] Textos nuevos en `strings.xml` (sección, opciones, diálogo, aviso, plurales de unidad)
- [x] Pruebas unitarias: `ReminderOptions` (validación y unidades), `ReminderRepository`
      (`replaceForOwner`: agrega, saca, no toca los repetidos), `CreateTaskViewModel` (cargar
      recordatorios existentes, guardar en tarea nueva/simple, cada alcance de recurrente, "sin
      hora" no guarda, borrar con cada alcance)
- [x] Pruebas instrumentadas: `ReminderDaoTest` y migración 4→5 en `MigrationTest`
- [x] Verificar compilación: `./gradlew assembleDebug` (y `assembleDebugAndroidTest`)
- [x] Correr pruebas: `./gradlew test` y `./gradlew lint`
- [ ] Correr `./gradlew connectedAndroidTest` (`ReminderDaoTest` y migración 4→5) — **pendiente:
      no había emulador ni teléfono conectado, así que esas pruebas están escritas y compilan
      (`assembleDebugAndroidTest`) pero no se ejecutaron**
- [ ] Probar manualmente en el emulador/teléfono: crear tarea con varios recordatorios (de la lista
      y con minutos libres); editarla y ver que se conservan; recurrente con cada alcance; "sin
      hora" oculta la sección — **pendiente, lo tenés que hacer vos**

## Notas
- Pendiente para el 0013, a confirmar con el desarrollador antes de hacerlo: agregar WorkManager
  (está en el stack de CLAUDE.md pero todavía no está en `libs.versions.toml`), registrar
  `ReminderReceiver` en el manifiesto (los permisos de notificaciones, alarmas exactas, pantalla
  completa y arranque ya están declarados) y cuánto dura "Posponer".
