# Ticket 0014 – Alarma sonora con pantalla completa, posponer y descartar (PL-11)

## Estado
En revisión

## Descripción
PL-11: "Tipos de alarma: notificación simple o alarma sonora con pantalla completa, con posponer y
descartar". Se apoya en el 0013 (`AlarmScheduler`, `ReminderScheduler`, `ReminderReceiver`) y en la
columna `reminder.kind` (`notice` | `alarm`) que ya existe desde el 0012.

**Decisiones de alcance confirmadas con el desarrollador:**
- El tipo se elige con **un interruptor por tarea** ("Sonar como alarma") en la sección
  Recordatorios: aplica a todos los avisos de esa tarea (se guarda en `kind` de cada recordatorio).
- Sonido: **tono de alarma del sistema** (el que el usuario ya tiene configurado). No se agregan
  archivos ni librerías; `soundKey` sigue nulo. Un selector de tonos propios queda para un paso aparte.
- Cambio de manifiesto confirmado: declarar `AlarmActivity` (sin exportar, sobre la pantalla de
  bloqueo, enciende la pantalla). `USE_FULL_SCREEN_INTENT` ya estaba declarado.

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Canal de notificación nuevo "Alarmas" (separado del de tareas, como pide CLAUDE.md): importancia
  alta, sonido de alarma con `USAGE_ALARM` y vibración.
- La alarma se muestra como notificación persistente que **repite el sonido** (`FLAG_INSISTENT`)
  hasta que se actúe, con un tope de 5 minutos para que no suene indefinidamente. Lleva
  `fullScreenIntent` hacia `AlarmActivity`; si el sistema no permite pantalla completa (Android 14+
  puede revocarlo), se ve como notificación emergente y sigue sonando: degradación elegante.
- Botones de la alarma: **Posponer** (mismos 10 min del 0013) y **Descartar** (silencia y quita la
  notificación, sin completar la tarea). "Completar" queda solo en la notificación simple.
- `AlarmActivity` no tiene lógica propia: sus botones disparan los mismos `PendingIntent` que los de
  la notificación hacia `ReminderReceiver` (una sola implementación de posponer/descartar).
- `replaceForOwner` recibe el `kind` y actualiza el de los recordatorios que ya existían si cambió.
- La pantalla de alarma usa el tema claro/oscuro del sistema (no lee todavía el ajuste de tema de la app).

## Checklist de cambios
- [x] `ReminderDao`: `updateKind`; `ReminderRepository.replaceForOwner(..., kind)` (inserta con ese
      `kind` y cambia el de los existentes si hace falta) + pruebas
- [x] `CreateTaskViewModel`: estado `alarmSound`, `onAlarmSoundChange`; se carga desde los
      recordatorios existentes y se guarda en cada alcance + pruebas
- [x] `CreateTaskScreen`: interruptor "Sonar como alarma" en la sección Recordatorios
- [x] `NotificationChannels`: canal "Alarmas" (sonido de alarma del sistema, vibración)
- [x] `ReminderNotifier`: variante de alarma (canal, insistente, tope de 5 min, pantalla completa,
      acciones Posponer y Descartar)
- [x] `ReminderReceiver`: acción "Descartar" y paso del `kind` al notificar
- [x] `AlarmActivity`: pantalla completa con título, hora, Posponer y Descartar (áreas táctiles
      ≥ 48 dp, texto además de color)
- [x] Manifiesto: declarar `AlarmActivity` (confirmado)
- [x] Textos nuevos en `strings.xml`
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Lista de comprobación manual en el teléfono (abajo) — **pendiente, la tenés que hacer vos**

## Lista de comprobación manual (teléfono real)
1. Crear una tarea con hora, un recordatorio y el interruptor "Sonar como alarma" activo; guardar y
   reabrirla: el interruptor sigue activo.
2. Con el teléfono **bloqueado y en reposo**, esperar la hora: se enciende la pantalla, aparece la
   pantalla de alarma y suena el tono de alarma repetido.
3. **Posponer:** se silencia y vuelve a sonar a los 10 min.
4. **Descartar:** se silencia, desaparece la notificación y la tarea queda sin completar.
5. Sin tocar nada: a los 5 min deja de sonar sola.
6. Con el teléfono **desbloqueado y en uso:** aparece como notificación emergente con Posponer y
   Descartar (y la pantalla completa según lo permita Android).
7. Tarea sin el interruptor: sigue saliendo la notificación simple de siempre, con Completar y Posponer.
8. Apagar el interruptor en una tarea que ya lo tenía: vuelve a notificación simple.
9. Permiso de pantalla completa revocado (Android 14+): la alarma suena igual como notificación.
10. Modo No molestar: anotar si la alarma suena o no (depende de la configuración del teléfono).
