# CLAUDE.md – Stay Focused (Android)

App Android nativa de uso personal (Kotlin + Jetpack Compose) que organiza el día en una línea de tiempo y reúne planificación, finanzas, gimnasio, alimentación, meditación, música/auriculares y tiempo de pantalla. Offline-first. Sin publicación en Google Play (APK instalado a mano). Gratuita.

**Documentos fuente (leer antes de trabajar en un módulo):**
- `docs/Documento funcional – Stay Focused, app integral de vida diaria (Android + Web E.md` → qué hace la app (requisitos con ID: PL-, FI-, GY-, AL-, ME-, MU-, TP-, TR-, PC-, NF-).
- `docs/Documento técnico – Stay Focused (Android).md` → cómo se construye.

Si algo de este archivo contradice a los documentos, mandan los documentos; avisá la discrepancia.

## Contexto del desarrollador

- Una sola persona, **primer proyecto en Kotlin/Android**. Responder en español (Argentina).
- Explicá brevemente el *porqué* de cada decisión de Kotlin/Android que no sea obvia. Código legible antes que ingenioso.
- Trabajá **un paso por sesión**, con mini-especificaciones chicas. No adelantes módulos ni fases.
- El código se revisa antes de aceptarlo: dejá cambios chicos y fáciles de leer.

## Normas generales (valen para todo el repo)

1. **Reutilizar siempre que se pueda.** Todo código que se pueda reutilizar se implementa de forma reutilizable, y todo lo que dos o más partes tengan en común va en un componente/función compartida (en `core/ui`, `core/util`, etc.), no copiado.
   - **Por qué:** el código queda más liviano y, si hay un cambio, se hace en un solo lugar y se aplica en todos a la vez.
   - Ejemplos: la barra inferior (tab bar) va en el layout/scaffold raíz, porque se ve en toda la app, y no se repite en cada pantalla; lo mismo con tarjetas, diálogos, estados vacíos, selectores de ícono/color, formateo de fechas y moneda, etc.
   - Antes de escribir algo nuevo, buscá si ya existe algo equivalente y usalo. Si ves duplicación, extraela.
   - Respetar la regla de capas: lo compartido entre módulos vive en `core`; los módulos no se llaman entre sí.
2. **Ante la duda, consultar; nunca suponer.** Si algo no se entiende (un requisito, una regla, una decisión técnica, el alcance, un comportamiento de Android), preguntale al desarrollador antes de avanzar. No rellenes huecos con supuestos ni inventes comportamiento.

## Stack y parámetros

- Kotlin, Jetpack Compose + Material 3, Navigation Compose, Corrutinas/Flow, Room, Hilt, DataStore, `java.time`, AlarmManager, WorkManager.
- Más adelante: librería de gráficos (a decidir en fase 2), CameraX + ML Kit (fase 3), Media3 (post-MVP), Supabase (fase 4).
- Android mínimo **8.0 (API 26)**; target y compile con la API estable más reciente.
- Gradle con Kotlin DSL. Versiones en `libs.versions.toml` (usar las estables más recientes).
- **Antes de agregar cualquier librería nueva, consultala con el desarrollador** y verificá su documentación oficial. Pocas librerías.
- **Un solo módulo de Gradle (`app`)**. No crear módulos adicionales.

## Arquitectura (4 capas, flujo en un solo sentido)

1. **Interfaz**: composables + ViewModels. Los composables solo dibujan; **nunca** acceden a Room ni a servicios del sistema.
2. **Dominio**: funciones puras con reglas de negocio. **No importan clases de Android.** Si una pantalla no tiene regla que aplicar, el ViewModel puede llamar directo al repositorio.
3. **Datos**: un repositorio por área; es lo único que ven los ViewModels.
4. **Fuentes**: Room, DataStore, servicios del sistema y red, envueltas en clases propias (ej. `AlarmScheduler`) para poder simularlas.

Patrón de pantalla: cada pantalla tiene un ViewModel que expone **un único `StateFlow<UiState>`** y recibe eventos. Pantalla → evento → ViewModel → estado → redibujado.

### Estructura de paquetes (`app/src/main/java/.../stayfocused/`)

```text
core/       database · timeline · notifications · settings · ui · util
planner/ finance/ gym/ nutrition/ meditation/ screentime/   → cada uno: data · domain · ui
media/      servicio de escucha, reproductor, auriculares
MainActivity.kt
```

- Cada área separa `data` (entidades, DAO, repositorio), `domain` (reglas puras) y `ui`.
- **Las áreas no se llaman entre sí**: se comunican solo a través de `core`.
- Una sola actividad con un `NavHost`. Barra inferior: Hoy, Finanzas, Gimnasio, Alimentación, Meditación. Música, Tiempo de pantalla y Ajustes son destinos secundarios.

## Línea de tiempo (eje de la app)

- Cada módulo implementa `TimelineSource` (`blocksFor(date): Flow<List<TimelineBlock>>`) y se registra con Hilt en un conjunto. **El núcleo solo conoce la interfaz, nunca a cada módulo.**
- Los bloques **no tienen tabla propia**: se calculan a partir de las tablas de cada módulo.
- Solo entran bloques con hora; las tareas sin hora van a la bandeja (Inbox).
- Cada bloque guarda una `route` de texto; al tocarlo se navega a esa ruta.
- Desde la línea de tiempo solo se completan tareas; los demás bloques se completan en su propia pantalla.
- El acomodo de superposiciones es una única función pura que recibe todos los bloques.
- Tarea con hora y sin duración: 30 min por defecto (configurable). Las superposiciones se permiten y se avisan.

## Datos (Room)

- Una sola `AppDatabase`. Nombres de campos y tablas **en inglés**.
- Todas las tablas: `id`, `uuid`, `createdAt`, `updatedAt` (ms) y `status` (borrado lógico). Preparan la sincronización futura.
- **`id` y `uuid` son cosas distintas:**
  - `id`: clave primaria numérica (`Long`, autogenerada), identificador interno de la fila.
  - `uuid`: texto de 36 caracteres en formato canónico `8-4-4-4-12` (32 dígitos hexadecimales + 4 guiones; ej. `550e8400-e29b-41d4-a716-446655440000`). Internamente es un número de 128 bits (16 bytes). Se genera en la app con `UUID.randomUUID().toString()`, es único (índice único) y es el identificador que se usará para sincronizar.
- **Borrado siempre lógico**, con la columna `status` (`Int`): **`0` = la fila existe**, **`-1` = la fila fue borrada lógicamente**. Nunca se hace `DELETE` físico. Todas las consultas que listan datos filtran por `status = 0`. Las filas hijas no se eliminan solas.
- **Dinero: `Long` en la unidad mínima (centavos). Nunca decimales.** El formato de moneda se resuelve solo al mostrar.
- **Pesos del gimnasio: gramos enteros**; convertir a kg/lb solo al mostrar.
- Fechas: día como `epochDay` con convertidor a `LocalDate`; hora de tarea/comida como **minutos desde las 00:00** (sin dependencia de zona horaria).
- Colores e íconos: claves de texto. Claves foráneas con índice; **apuntan al `id` numérico** (el `uuid` se usa solo para sincronizar).
- Ajustes del usuario (tema, moneda, unidades, notificaciones) en **DataStore**, no en Room.
- **Cada cambio de esquema**: subir la versión de la base, escribir **y probar** la migración, y exportar el esquema a `schemas/`. Nunca usar migración destructiva.
- Tablas de packs/sesiones guiadas de meditación: solo cuando se incorporen los audios.

## Reglas de negocio clave (en funciones puras con pruebas)

- **Recurrencia**: la regla vive en la tarea; las ocurrencias se calculan con una función pura (`occurrencesOn(date)`). Solo hay fila en `task_occurrence` si la ocurrencia se completa, se salta o se modifica. Completar una ocurrencia no completa las demás. Editar "esta y siguientes" divide la regla en dos tareas.
- **Rutinas**: aplicar una rutina crea tareas normales enlazadas por `routineId`; editar la rutina no cambia las ya creadas.
- **Finanzas**: saldo = ingresos − gastos − transferencias salientes + entrantes (se calcula, no se guarda). Las transferencias (una sola fila con `transferAccountId`) **no cuentan como ingreso ni gasto**. Borrar una categoría con transacciones exige reasignarlas. Presupuesto con alertas al 80 % y 100 %.
- **Gimnasio**: volumen de serie = peso × reps; volumen del ejercicio = suma de series normales. **1RM con Epley: `peso × (1 + reps / 30)`.** Las series de calentamiento no cuentan para récords ni volumen. Sesión abandonada = sesión incompleta que conserva las series. La sesión en curso se guarda en la base serie por serie.
- **Alimentación**: 4 kcal/g de proteína y carbohidrato, 9 kcal/g de grasa. `meal_item` **copia los valores al momento de cargar**: cambiar un alimento no altera registros pasados. Datos de Open Food Facts se muestran como orientativos y editables.
- **Meditación**: la racha cuenta sesiones completadas con ≥ 80 % de lo planeado (configurable). Temporizador con reloj monotónico, no contando segundos.
- **Sincronización (futuro)**: gana la modificación más reciente; el borrado lógico cuenta como modificación.

## Alarmas, notificaciones y segundo plano (zona sensible)

- `AlarmScheduler` es **el único punto** que habla con `AlarmManager`.
- Se programa **solo la próxima ocurrencia** de cada recordatorio; al dispararse, se programa la siguiente.
- Reprogramar tras: reinicio, cambio de hora/zona horaria, actualización de la app, crear/editar/completar/borrar una tarea, y una vez al abrir la app. (`BootReceiver` + `RescheduleWorker`.)
- Antes de programar una alarma exacta, comprobar el permiso y, si falta, guiar al usuario a Ajustes.
- Canales de notificación separados por módulo (tareas, alarmas, entrenamiento, comidas, meditación, presupuesto, tiempo de pantalla).
- Temporizador de meditación = servicio en primer plano; revisión de límites de uso = WorkManager periódico (~15 min, mínimo del sistema).
- Las alarmas **no se pueden probar bien automáticamente**: pedir verificación manual en el teléfono real, con el teléfono en reposo.

## Permisos (manifiesto)

- **Cualquier cambio en el manifiesto lo confirma el desarrollador antes de hacerlo.**
- Declarar cada permiso solo cuando su módulo entra en desarrollo; pedirlo en el momento en que hace falta, con explicación previa (nunca todos al abrir la app).
- Verificar nombres y condiciones en la **documentación oficial** del nivel de API usado; las reglas cambian entre versiones de Android.
- Si el usuario rechaza un permiso: la función se deshabilita con explicación y el resto de la app sigue funcionando.
- **Degradación elegante**: lo que depende del equipo o de permisos (batería de auriculares, música, uso del teléfono) muestra "no disponible", nunca falla ni lanza errores.
- Música: servicio de escucha de notificaciones + controles de la sesión activa; **no usar APIs ocultas del sistema** para la batería de auriculares.
- Tiempo de pantalla: `UsageStatsManager`. **No bloquear apps ni usar accesibilidad** en la v1.

## Interfaz y accesibilidad

- Material 3 con tema, tipografía y espaciados en `core/ui`; modo claro, oscuro y automático. Interfaz calma, sin ruido visual.
- **Todos los textos en `strings.xml`** (español Argentina), nunca hardcodeados.
- Áreas táctiles ≥ 48 dp, `contentDescription` en íconos, y **el color nunca es el único indicador de estado** (sumar ícono o texto).
- Registrar un gasto o una serie: pocos toques (< 5 s) y confirmación en < 300 ms.
- Estados vacíos y errores con mensaje claro y acción sugerida.
- Diseño propio: **no copiar** íconos, textos, paleta, nombres ni contenidos de Structured, Medito, Symmetry ni StayFree.

## Seguridad y privacidad

- Los datos no salen del teléfono en el MVP. Las únicas conexiones son a Open Food Facts, solo por **HTTPS**; identificar la app en cada pedido y mostrar la atribución.
- **Nunca** poner claves, tokens ni secretos en el código ni en Git (usar `local.properties` o archivos fuera del repositorio). Con Supabase solo van las claves públicas; **jamás la clave de servicio**. RLS primero.
- **No escribir en los logs** montos, alimentos, notas ni datos de salud.
- Desactivar o excluir los datos de la app de las copias automáticas de Android.
- Datos de música y de uso del teléfono: solo en el teléfono, nunca a un servidor.
- La clave de firma del APK vive fuera del repositorio y con copia de resguardo (sin ella no se puede actualizar sin perder datos).
- La app no da consejo médico, nutricional ni financiero, y lo indica.

## Pruebas

Mayor esfuerzo en la lógica de negocio:
- **Unitarias (JUnit)** para toda función pura de `domain`: acomodo de bloques, recurrencias, saldos/presupuestos, 1RM/récords, macros, rachas. Escribirlas junto con la regla.
- **Datos**: DAO, consultas y **cada migración** con Room en memoria.
- **ViewModel**: Turbine + MockK en los flujos principales.
- **Interfaz (Compose)**: pocas, solo caminos críticos.
- **Manuales**: alarmas, permisos, auriculares, música, tiempo de pantalla.
- Cuando una fase se da por terminada, entregar la **lista de comprobación manual** correspondiente.

## Comandos

```bash
./gradlew assembleDebug            # compilar para probar
./gradlew test                     # pruebas unitarias
./gradlew connectedAndroidTest     # pruebas instrumentadas
./gradlew lint                     # revisión de estilo
```

Antes de dar una tarea por terminada: compilar y correr `test` (y `lint` si tocaste varias cosas). Si no pudiste ejecutar algo, decilo.

## Tickets de cambios

- **Cada cambio que se haga requiere un ticket**, creado antes de empezar a trabajar.
- Cada ticket lleva una **checklist de cambios** (un ítem por cambio concreto, tildado al completarse) y un **estado** visible: `Pendiente` → `En progreso` → `En revisión` → `Terminado`.
- Mantener el estado y la checklist actualizados a medida que avanza el trabajo; un ticket solo pasa a `Terminado` cuando todos sus ítems están tildados y el desarrollador lo aprobó.
- Un ticket por tarea, alineado con la regla de una rama y un commit por tarea.
- **Los tickets viven en `docs/tickets/`**, un archivo Markdown por ticket (ej. `docs/tickets/0001-nombre-corto.md`), con título, estado y checklist de cambios.

## Git y entrega

- Una rama por tarea y **un commit por tarea terminada**. No hacer commit ni push sin que el desarrollador lo pida.
- `versionCode` sube en cada APK instalado; `versionName` simple (0.1, 0.2, …).
- Antes de actualizar el APK, recordar exportar el respaldo JSON (con número de versión del formato). La importación valida y convierte versiones anteriores.

## Fases (no adelantarse)

| Fase | Contenido |
| --- | --- |
| 1 | Base + Planificación (PL-01…17), tema, navegación, alarmas, widget básico |
| 2 | Finanzas + Gimnasio |
| 3 | Alimentación + Meditación (solo temporizador y campanas) |
| 4 | Cuenta/sincronización Supabase, web (PWA), música y auriculares, tiempo de pantalla |
| 5 | Pulido: widgets, búsqueda global, bloqueo biométrico, backup, resumen semanal, TalkBack, APK firmado |

Trabajar solo en la fase actual. Si una tarea pertenece a una fase posterior, avisar antes de implementarla.

## Fuera de alcance

iOS, publicación en Google Play, red social, compras dentro de la app, asesoramiento médico/nutricional/financiero, bloqueo real de apps, sesiones guiadas de meditación con audio (post-MVP), sincronización/web (hasta la fase 4), ejecutable de Windows (solo Windows, mucho después).

## Decisiones pendientes (no decidir solas; consultar)

- Librería de gráficos para Compose (fase 2).
- Cifrar la base local (SQLCipher) o alcanzar con el almacenamiento privado.
- Adelantar un respaldo JSON básico a la fase 1.
- Separar la fase 4 en dos.
- Biblioteca de ejercicios precargada: propia o con licencia abierta.
- Ícono de la app (pendiente de diseñar y aprobar).
