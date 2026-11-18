# Ticket 0019 – Temporizador de enfoque con pausa y aviso de fin (PL-13)

## Estado
Terminado

## Descripción
PL-13: "Temporizador de enfoque para la tarea actual, con pausa y aviso de fin". El documento
funcional no define los detalles; las decisiones siguientes las confirmó el desarrollador.

**Decisiones de alcance confirmadas con el desarrollador:**
- **Duración:** Pomodoro configurable. Se inicia desde una tarea de Hoy y se elige la duración al
  arrancar (25, 50 o personalizada en minutos), independiente de la duración planificada.
- **Segundo plano:** servicio en primer plano con notificación persistente (pausar/reanudar/detener).
  Sigue corriendo con la pantalla apagada.
- **Aviso de fin:** notificación con sonido en un canal propio (no es la alarma de pantalla completa).
- **Al terminar:** solo avisa; no completa la tarea (el tilde sigue siendo manual).
- **Manifiesto (confirmado):** `FOREGROUND_SERVICE_SPECIAL_USE` + `<service>` con
  `foregroundServiceType="specialUse"` y su `<property>`. Nombres verificados en la documentación
  oficial de tipos de servicio en primer plano. `FOREGROUND_SERVICE` y `POST_NOTIFICATIONS` ya estaban.

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Reglas puras en `planner/domain` (`FocusTimer.kt`): sesión con estado `RUNNING`/`PAUSED`, tiempos con
  **reloj monotónico** (no se cuentan segundos), restante, pausa y reanudación; minutos válidos (1 a 240).
  El formato `mm:ss` / `h:mm:ss` va en `core/util` (`Countdown.kt`) porque lo va a reusar Meditación.
- `FocusTimerController` (singleton) guarda la sesión en un `StateFlow` y es lo único que la cambia.
  El aviso de fin se programa con `AlarmScheduler` (único punto que habla con `AlarmManager`), así suena
  aunque el teléfono esté en reposo; al pausar se cancela y al reanudar se reprograma con lo que queda.
- El servicio en primer plano solo mantiene viva la app y refresca la notificación cuando cambia la
  sesión; la cuenta regresiva de la notificación la dibuja el sistema (cronómetro nativo), sin ticks propios.
- El aviso de fin lo muestra el receptor de la alarma (no el servicio), con el título leído de la tarea,
  así funciona aunque el proceso haya muerto.
- Dos canales: **Enfoque** (silencioso, para la notificación persistente) y **Fin de enfoque**
  (con sonido y vibración).
- Sin el permiso de notificaciones o de alarmas exactas, **Iniciar** queda deshabilitado con la
  explicación (reusa `ReminderPermissionPrompt`, CLAUDE.md "degradación elegante").
- Se inicia desde el ícono de temporizador de un bloque de la línea de tiempo (tareas con hora, no
  completadas, y solo si no hay otra sesión en curso). La barra de la sesión activa vive arriba de Hoy.

**Límites conocidos (pasos aparte):** la sesión no sobrevive a que Android mate el proceso (el aviso de
fin sí suena, pero la barra de Hoy no se recupera); no se inicia desde tareas de la bandeja (sin hora).

## Checklist de cambios
- [x] `core/util` (`Countdown.kt`): `formatCountdown` + pruebas
- [x] `planner/domain` (`FocusTimer.kt`): sesión, pausa/reanudación, restante, minutos válidos + pruebas
- [x] `FocusTimerController` (+ reloj, lanzador del servicio y módulo Hilt) + pruebas
- [x] `FocusTimerService`, `FocusNotifier` y `FocusTimerReceiver`; canales nuevos
- [x] Manifiesto: permiso, servicio y receptor (confirmado)
- [x] `FocusTimerViewModel` + pruebas
- [x] UI: barra de sesión activa, diálogo de inicio y ícono en los bloques
- [x] `ReminderPermissionPrompt` avisa si hay problema (para deshabilitar Iniciar)
- [x] Textos nuevos en `strings.xml`
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Correr `./gradlew connectedAndroidTest` — necesita emulador o teléfono conectado
- [ ] Probar manualmente en el teléfono real (abajo)

## Lista de comprobación manual
Las alarmas y el segundo plano no se pueden probar bien automáticamente: probar en el teléfono real.
1. En un bloque de Hoy aparece un ícono de temporizador; tocarlo abre el diálogo con 25, 50 y personalizada.
2. Con "personalizada" vacía, en 0 o mayor a 240, **Iniciar** queda deshabilitado.
3. Iniciar: aparece la barra arriba de Hoy con el título y la cuenta regresiva, y una notificación
   persistente con el tiempo restante; el ícono de temporizador de los bloques desaparece.
4. Pausar (desde la barra y desde la notificación): la cuenta se congela y la notificación dice "En pausa".
   Reanudar: sigue desde donde quedó.
5. Detener: desaparecen la barra y la notificación y **no** suena ningún aviso después.
6. Dejar correr con la pantalla apagada y el teléfono en reposo: al llegar a cero suena la notificación
   de fin (sonido + vibración) y la notificación persistente se va.
7. Pausar a mitad, esperar más que el tiempo restante y comprobar que **no** suena; al reanudar suena
   cuando corresponde.
8. Tocar la notificación abre la app. La tarea no se marca como completada.
9. Sin permiso de notificaciones o de alarmas exactas: el diálogo lo explica y **Iniciar** está deshabilitado.
10. Con TalkBack: la barra lee título y tiempo restante; los botones indican su acción.
