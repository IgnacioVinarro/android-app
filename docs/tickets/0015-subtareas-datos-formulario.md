# Ticket 0015 – Subtareas: datos y formulario (PL-04 / PL-05, parte 1)

## Estado
En revisión

## Descripción
Primera mitad de PL-05 y la parte de subtareas que le faltaba a PL-04 ("Crear tarea con … subtareas").
Este paso deja listo **dónde se guardan las subtareas y cómo se cargan desde el formulario de
tarea**. **No** permite tildarlas desde la línea de tiempo ni muestra progreso: eso es el ticket 0016.
Mismo criterio de partición que 0008/0009 y 0012/0013.

**Decisiones de alcance confirmadas con el desarrollador:**
- Dos tickets: 0015 (datos + formulario) y 0016 (completar con un toque, animación y progreso del día).
- En tareas recurrentes, las subtareas son **una plantilla fija que pertenece a la tarea** (se editan
  con "todas" / "esta y siguientes"); el **tilde de cada una se guarda por ocurrencia** (tabla de
  tildes, en el 0016), igual que el completado de la tarea.
- El progreso del día se muestra en el encabezado de Hoy (0016).

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Tabla `subtask` con los campos del documento técnico (`taskId`, `title`, `done`, `position`) más
  los comunes (`id`, `uuid`, `createdAt`, `updatedAt`, `status`). `taskId` apunta al `id` numérico.
- `done` guarda el tilde de las subtareas de **tareas simples**. En una tarea recurrente se ignora:
  los tildes por ocurrencia van a otra tabla en el 0016.
- Las subtareas valen con o sin hora (a diferencia de los recordatorios): una tarea de la bandeja
  también puede tenerlas.
- Reglas del formulario: el título se recorta y las subtareas vacías se descartan al guardar; el
  orden es el de la lista (`position` 0, 1, 2…). No se puede reordenar arrastrando todavía.
- Editar: las subtareas que ya existían conservan su `uuid` y su `done` (solo cambian título y
  posición); las que se sacan se borran **lógicamente**; las nuevas se insertan.
- Alcance al editar una tarea recurrente: "todas" pisa las subtareas de la tarea; "esta y
  siguientes": la tarea nueva se lleva las editadas (como filas nuevas) y la original conserva las
  suyas; "solo esta": las subtareas **no cambian** (no hay override por ocurrencia, igual que con
  los recordatorios).
- Borrar una tarea (o "esta y siguientes" desde la primera ocurrencia, que equivale a "todas")
  borra lógicamente también sus subtareas: las filas hijas no se eliminan solas (CLAUDE.md).
- Las tareas que genera una rutina no traen subtareas (las rutinas no las guardan).

## Checklist de cambios
- [x] `SubtaskEntity` (tabla `subtask`): campos comunes + `taskId`, `title`, `done`, `position`;
      índice único en `uuid` e índice en `taskId`
- [x] `SubtaskDao`: insertar, actualizar título/posición, listar activos por tarea ordenados por
      `position` (`status = 0`), borrado lógico por id y por tarea
- [x] `SubtaskRepository`: `getByTask`, `replaceForTask` (conserva los existentes, inserta nuevos,
      borra los que sobran) y `deleteByTask`; módulo Hilt
- [x] `AppDatabase`: sube a versión 6, migración `MIGRATION_5_6` (crea `subtask`), exporta esquema
      versión 6
- [x] `planner/domain` (`Subtasks.kt`): `SubtaskDraft` y función pura que recorta títulos y
      descarta vacíos
- [x] `CreateTaskViewModel`: lista de subtareas en el estado; agregar/editar/sacar; cargar las de la
      tarea al editar; guardarlas en cada alcance; borrarlas al borrar la tarea
- [x] `CreateTaskScreen`: sección "Subtareas" (una fila con campo de texto y botón de borrar por
      subtarea, botón "Agregar subtarea"); aviso en recurrentes de que con "Solo esta" no cambian
- [x] Textos nuevos en `strings.xml`
- [x] Pruebas unitarias: `Subtasks` (recorte y descarte), `SubtaskRepository` (`replaceForTask`:
      inserta, conserva `done`, borra los que sobran, reordena), `CreateTaskViewModel` (cargar,
      guardar en tarea nueva/simple, cada alcance de recurrente, borrar)
- [x] Pruebas instrumentadas: `SubtaskDaoTest` y migración 5→6 en `MigrationTest`
- [x] Verificar compilación: `./gradlew assembleDebug` (y `assembleDebugAndroidTest`)
- [x] Correr pruebas: `./gradlew test` y `./gradlew lint`
- [ ] Correr `./gradlew connectedAndroidTest` (`SubtaskDaoTest` y migración 5→6) — necesita
      emulador o teléfono conectado
- [ ] Probar manualmente en el emulador/teléfono: crear tarea con varias subtareas; editarla y ver
      que se conservan, se reordenan y se sacan; recurrente con cada alcance; borrar la tarea

## Notas
- El tilde de las subtareas desde la línea de tiempo, la animación y el progreso del día son el 0016.
