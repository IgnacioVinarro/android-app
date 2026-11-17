# Ticket 0013 – Recordatorios por tarea: programar, avisar y reprogramar (PL-10, parte 2)

## Estado
En revisión

## Descripción
Segunda mitad de PL-10. Se apoya en la tabla `reminder` y el formulario del ticket 0012. Acá los
recordatorios empiezan a sonar: se programa la alarma, se muestra la notificación con acciones y se
reprograma ante los eventos del sistema.

Reglas de CLAUDE.md que aplican:
- `AlarmScheduler` es el único punto que habla con `AlarmManager`.
- Se programa solo la próxima ocurrencia de cada recordatorio; al dispararse, se programa la siguiente.
- Reprogramar tras: reinicio, cambio de hora/zona, actualización de la app, crear/editar/completar/
  borrar una tarea, y una vez al abrir la app (`BootReceiver` + `RescheduleWorker`).
- Antes de programar una alarma exacta, comprobar el permiso y, si falta, guiar al usuario a Ajustes.
- Canal de notificación propio para tareas. Permisos pedidos en contexto, con explicación previa.
- Las alarmas no se prueban bien de forma automática: verificación manual con el teléfono en reposo.

## Decisiones confirmadas con el desarrollador
- [x] Agregar WorkManager (`androidx.work:work-runtime` 2.12.0, última estable según la documentación
      oficial; desde 2.9 `work-runtime-ktx` está vacío).
- [x] Registrar `ReminderReceiver` y `BootReceiver` en el manifiesto.
- [x] "Posponer" dura 10 minutos (constante `REMINDER_SNOOZE_MINUTES`, fija por ahora).

## Decisiones de diseño tomadas en este paso (a revisar en la entrega)
- `AlarmScheduler` (core) es genérico: recibe una clave y un `AlarmRequest` (receptor + acción +
  extras numéricos), sin Intents, para poder simularlo en pruebas. No conoce tareas.
- `ReminderScheduler`, `ReminderReceiver` y `ReminderNotifier` viven en `planner/data` (son la capa
  de "fuentes" del planificador). `BootReceiver` y `RescheduleWorker` quedan en `core/notifications`.
- `RescheduleWorker` obtiene sus dependencias con un `EntryPoint` de Hilt para no agregar la
  librería `androidx.hilt:hilt-work`.
- Clave de alarma = id del recordatorio; "Posponer" usa la clave negativa para no pisar el próximo
  aviso regular. La notificación usa el id del recordatorio (un nuevo aviso reemplaza al anterior).
- Si al sonar la alarma la tarea/recordatorio se borró o la ocurrencia ya se completó o saltó, no
  se muestra nada (pero igual se programa el siguiente).
- La reprogramación se engancha a la base de datos (un solo `ReminderSyncer`) y no a cada
  ViewModel: cualquier cambio futuro de tareas (rutinas, mover, etc.) queda cubierto solo. Usa
  `InvalidationTracker.createFlow` de Room, que es una API marcada como experimental.
- Limitación conocida: si se borra una tarea o un recordatorio, su alarma ya programada no se
  cancela; al sonar, el receptor la ignora (no muestra nada). No queda ningún aviso en pantalla.
- "Completar" desde la notificación repite la lógica de completar de `TodayViewModel` (pero siempre
  completa, no alterna). Queda como posible extracción a un componente compartido.

## Checklist de cambios
- [x] `planner/domain` (`ReminderTrigger.kt`): función pura `nextReminder` (próximo disparo y fecha de
      la ocurrencia, mirando recurrencia, ocurrencias saltadas/completadas y overrides de hora) +
      pruebas unitarias
- [x] `AlarmScheduler` en `core/notifications`: programar y cancelar, comprobando el permiso de alarma exacta
- [x] `ReminderScheduler` (`planner/data`): calcula y programa el próximo aviso de cada recordatorio
      (`rescheduleTask`, `rescheduleAll`, `snooze`) + pruebas unitarias; consultas nuevas
      (`getAllTimed`, `getByTask`, `getById`)
- [x] `ReminderReceiver`: muestra la notificación y programa el siguiente disparo
- [x] Notificación con acciones "Completar" y "Posponer"
- [x] Canal de notificación de tareas (se crea al iniciar la app)
- [x] Pedido de permisos en contexto (notificaciones y alarma exacta), con explicación y guía a
      Ajustes: `ReminderPermissions` (core/notifications, función pura `missingReminderPermission` +
      pruebas) y `ReminderPermissionPrompt` (core/ui, reutilizable); se muestra en el formulario de
      tarea solo cuando hay un recordatorio elegido
- [x] `BootReceiver` + `RescheduleWorker`: reinicio, cambio de hora/zona, actualización de la app
- [x] Reprogramar al crear/editar/completar/borrar tarea y al abrir la app: `ReminderSyncer`
      (`planner/data`) observa las tablas `task`, `task_occurrence` y `reminder` y llama a
      `rescheduleAll` (con debounce de 500 ms); se arranca en `StayFocusedApplication` + pruebas
- [x] Cambios de manifiesto (`ReminderReceiver`; `BootReceiver` ya estaba)
- [x] Compilación (`assembleDebug`), `./gradlew test` y `./gradlew lint` en verde con lo hecho hasta acá
- [ ] Lista de comprobación manual en el teléfono (abajo) — **pendiente, la tenés que hacer vos**

## Lista de comprobación manual (teléfono real)
1. **Permisos:** instalar la app de cero. Crear una tarea con hora y elegir un recordatorio: aparece
   el aviso rojo de notificaciones. "Permitir notificaciones" abre el diálogo del sistema. Aceptar:
   el aviso desaparece. Repetir rechazando dos veces: el botón pasa a "Abrir ajustes".
2. **Alarma exacta (Android 12 y 12L):** si pide el permiso de alarmas, "Abrir ajustes" lleva a la
   pantalla correcta y al volver el aviso desaparece.
3. **Aviso a la hora:** tarea simple para dentro de 3 min con recordatorio "a la hora"; bloquear el
   teléfono y esperar sin tocarlo. Debe sonar la notificación a la hora, con "Completar" y "Posponer".
4. **Antes de:** tarea con "5 min antes" y otra con minutos libres (p. ej. 7): suenan a la hora
   correcta.
5. **Varios recordatorios** en una misma tarea: suenan todos.
6. **Posponer:** vuelve a sonar a los 10 min y no pisa el siguiente aviso.
7. **Completar:** desde la notificación; la tarea queda tildada en la línea de tiempo.
8. **Recurrente:** tarea diaria con recordatorio; completar hoy y comprobar que mañana vuelve a avisar.
   "Solo esta" saltada: ese día no avisa.
9. **Editar / borrar:** cambiar la hora de la tarea (el aviso nuevo sale a la hora nueva, no a la
   vieja); borrarla (no sale ningún aviso).
10. **Reinicio:** programar un aviso, reiniciar el teléfono, comprobar que sigue sonando.
11. **Cambio de hora/zona horaria:** mover el reloj del teléfono y comprobar que el aviso se corre.
12. **Notificaciones apagadas** a mano en Ajustes: la app no falla y el aviso del formulario aparece.

## Notas
- El servidor de lenguaje de Kotlin de VS Code (extensión `fwcd.kotlin`) deja trabado
  `app/build/.../R.jar` y rompe el build en Windows. Hay que cerrarlo antes de compilar.
