# Ticket 0006 – Bandeja de entrada para tareas sin hora (PL-06)

## Estado
Terminado

## Descripción
Quinto paso de Planificación. Agrega la posibilidad de crear una tarea sin hora (va a la
bandeja) y una sección en "Hoy" que las lista, donde tocar una tarea le asigna una hora y pasa
a la línea de tiempo.

**Decisión de alcance (confirmada con el desarrollador):** el requisito original dice que las
tareas de la bandeja "se arrastran" a la línea de tiempo (gesto de arrastre). Eso se superpone
con PL-07 (mover/redimensionar bloques arrastrando), que es un paso aparte y más complejo. Por
ahora: tocar una tarea de la bandeja abre el selector de hora y la pasa a la línea de tiempo,
sin gesto de arrastre. El arrastre real (desde la bandeja y entre bloques) se hace junto con
PL-07.

## Checklist de cambios
- [x] `CreateTaskScreen`/`CreateTaskViewModel`: agrega la opción "Sin hora (bandeja)"; al activarla
      se oculta el selector de hora y la tarea se guarda con `startMinutes = null`
- [x] `TodayViewModel`: suma al estado la lista de tareas de la bandeja (tareas de hoy con
      `startMinutes == null`) combinando con lo que ya arma la línea de tiempo; agrega
      `onAssignTime(task, hora)` que actualiza la tarea con la hora elegida
- [x] `TodayScreen`: sección "Bandeja" arriba de la línea de tiempo (solo se muestra si hay
      tareas), cada ítem toca para abrir el selector de hora (reusa `core/ui/TimePickerDialog`)
      y así pasar a la línea de tiempo; si no hay tareas en la bandeja, la sección no aparece
- [x] Textos nuevos en `strings.xml`
- [x] Pruebas unitarias: `TodayViewModel` arma `inboxTasks` filtrando `startMinutes == null` y
      `onAssignTime` actualiza la tarea correctamente; `CreateTaskViewModel` guarda
      `startMinutes = null` cuando está activado "sin hora"
- [x] Verificar compilación: `./gradlew assembleDebug`
- [x] Correr pruebas: `./gradlew test`, `./gradlew connectedAndroidTest` y `./gradlew lint`
- [x] Probar manualmente en el emulador: crear una tarea sin hora, verla en la bandeja, tocarla,
      asignarle una hora y confirmar que aparece en la línea de tiempo

## Notas
- La bandeja se acota a las tareas de **hoy** sin hora (mismo alcance de fecha que ya usa toda la
  pantalla "Hoy"); una bandeja que junte tareas de cualquier día es un paso aparte, para cuando
  exista la vista semanal (PL-12).
- Bug de compilación encontrado y corregido: había importado expl­ícitamente
  `androidx.compose.foundation.layout.weight`, que choca con una propiedad interna del mismo
  nombre en ese paquete (`RowColumnParentData.weight`) y rompe la compilación ("it is internal in
  file"). `ColumnScope.weight` es miembro de la interfaz `ColumnScope`, se resuelve solo por el
  receptor implícito del lambda de `Column { ... }`, sin necesidad de importarlo.
- Probado manualmente en el emulador: se creó "Llamar al plomero" con el switch "Sin hora
  (bandeja)" activado, apareció en la sección "Bandeja" sobre la línea de tiempo, tocarla abrió el
  selector de hora reutilizable (`core/ui/TimePickerDialog`), y al confirmar la tarea pasó
  correctamente a la línea de tiempo con la hora elegida (y la bandeja quedó vacía, por lo que la
  sección desapareció).
- `./gradlew assembleDebug`, `./gradlew test`, `./gradlew connectedAndroidTest` y
  `./gradlew lint` corren en verde.
