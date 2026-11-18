# Ticket 0021 – Widget de inicio con las próximas tareas y accesos rápidos (PL-16)

## Estado
En revisión

## Descripción
PL-16: "Widget de pantalla de inicio con las próximas tareas y accesos rápidos". El documento funcional
no define los detalles; las decisiones siguientes las confirmó el desarrollador.

**Decisiones de alcance confirmadas con el desarrollador:**
- **Tecnología:** API nativa (`AppWidgetProvider` + `RemoteViews` con layouts XML), **sin librerías nuevas**.
- **Contenido:** las **próximas 4 tareas de hoy** (hora + título + franja del color de la tarea) y el
  progreso del día "X de Y". Estado vacío con mensaje. Tocar una fila abre la app.
- **Accesos rápidos:** botón **+** (abre el formulario de nueva tarea con la fecha de hoy) y abrir la app.
  Sin acciones que cambien datos desde el widget (no se completa desde acá).
- **Refresco:** al cambiar los datos (mientras la app está viva) y cada 30 min con WorkManager (periódico).
- **Manifiesto (confirmado):** `<receiver>` del widget (`exported=true`, lo exige el launcher) con el filtro
  `APPWIDGET_UPDATE` y su `meta-data`; y `MainActivity` pasa a `launchMode="singleTop"` para reusar la app
  abierta con `onNewIntent`. No suma permisos.

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Regla pura en `planner/domain` (`UpcomingTasks.kt`): `buildWidgetSnapshot`. "Próximas" = tareas de
  Planificación **no completadas que todavía no terminaron** (las que están en curso también entran),
  ordenadas por hora, hasta 4. El progreso cuenta solo tareas con hora de hoy (no incluye bandeja ni
  subtareas, así que puede diferir del encabezado de Hoy).
- Código del widget en `planner/widget` (actualizador, proveedor y trabajador periódico). El actualizador
  lee la línea de tiempo de hoy por `TimelineRepository` y arma el `RemoteViews`; el proveedor solo delega.
- Las filas se agregan con `addView` a un contenedor, así el límite de filas vive en una constante y no en
  el XML.
- **Franja sin `View`:** la franja de color es un `ImageView` sin imagen (lint rechaza un `View` simple dentro de un `RemoteViews`).
- **Sin ícono por tarea:** los íconos son `ImageVector` de Compose y un `RemoteViews` no los puede usar.
  Cada fila lleva hora y título en texto + una franja de color: el color no es el único indicador.
- El widget sigue el **tema del sistema** (claro/oscuro del teléfono), no el ajuste de tema de la app:
  vive en el launcher, fuera del tema de Compose.
- El botón + lleva un `Intent` con un pedido de arranque (`AppLaunchRequest.NEW_TASK`); `MainActivity` lo
  lee al arrancar y en `onNewIntent`, y `StayFocusedApp` navega al formulario una sola vez.
- `ReminderSyncer` y el nuevo `WidgetSyncer` comparten la misma mecánica "esperar un instante y actuar al
  cambiar algo": se extrae a `core/util` (`syncOnChange`) en vez de copiarla (norma de reutilizar).

**Límites conocidos (pasos aparte):** sin pantalla de configuración ni tamaños alternativos; el refresco
entre ticks depende de que la app esté viva (si Android la mató, el widget se actualiza en el próximo tick
de 30 min); no se muestran tareas de la bandeja (sin hora).

## Checklist de cambios
- [x] `core/util` (`syncOnChange`) y `ReminderSyncer` pasa a usarlo (sus pruebas siguen en verde)
- [x] `planner/domain` (`UpcomingTasks.kt`): `buildWidgetSnapshot` + pruebas
- [x] `core/ui/navigation`: `AppLaunchRequest` + `MainActivity`/`StayFocusedApp` lo atienden + pruebas
- [x] `planner/widget`: actualizador, proveedor, trabajador periódico y `WidgetSyncer`
- [x] Layouts, configuración del widget, colores claro/oscuro y textos
- [x] Manifiesto: receiver del widget y `singleTop` en `MainActivity` (confirmado)
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Correr `./gradlew connectedAndroidTest` — necesita emulador o teléfono conectado
- [ ] Probar manualmente en el teléfono real (abajo)

## Lista de comprobación manual
Los widgets no se pueden probar bien automáticamente: probar en el teléfono real.
1. Mantener presionado el inicio → Widgets → "Stay Focused": se agrega un widget de 4 × 4 aprox.
2. Muestra "Hoy", el progreso "X de Y" y hasta 4 tareas de hoy con hora y título, con su franja de color.
3. Con tareas de hoy ya terminadas o completadas, esas no aparecen; la que está en curso sí.
4. Sin tareas pendientes: "No hay más tareas hoy"; sin ninguna tarea con hora: "No hay tareas hoy".
5. Crear, completar o borrar una tarea con la app abierta: el widget se actualiza en pocos segundos.
6. Cerrar la app y esperar el tick de 30 min (o cambiar la hora): "las próximas" avanzan con la hora.
7. Pasada la medianoche, el widget muestra el día nuevo.
8. Tocar una fila o el título abre la app en Hoy.
9. Tocar **+** con la app cerrada: abre directo el formulario "Nueva tarea" con la fecha de hoy.
10. Tocar **+** con la app abierta en otra pantalla: reusa la app (sin pantallas duplicadas) y abre el formulario;
    al volver con Atrás se ve lo que estaba antes. Girar la pantalla no vuelve a abrir el formulario.
11. Quitar el widget: deja de refrescarse (sin trabajo periódico).
12. Tema oscuro del teléfono: el fondo y los textos del widget se ven bien (contraste legible).
13. Con TalkBack: el + se anuncia "Agregar tarea"; cada fila lee hora y título.
