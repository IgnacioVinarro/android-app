# Documento técnico – Stay Focused (Android)

Nov 14, 2026 · @Ignacio

## 1. Resumen y principios técnicos

Stay Focused es una app Android nativa de uso personal, hecha con Kotlin y Jetpack Compose, que guarda todo en una base local y funciona sin conexión. La sincronización con Supabase y la versión web llegan en una fase posterior, y el diseño actual ya las deja preparadas. Este documento traduce el documento funcional a decisiones técnicas concretas.

| Principio | Qué implica |
| --- | --- |
| Offline-first | La base local (Room) es la fuente de verdad; ninguna pantalla depende de internet |
| Simplicidad | Un solo módulo de Gradle, pocas librerías y código legible: es el primer proyecto de Kotlin de su autor |
| Línea de tiempo como eje | Los módulos aportan bloques a la línea de tiempo mediante una interfaz común; el núcleo no conoce a cada módulo |
| Lógica pura y probable | Las reglas de negocio viven en funciones sin dependencias de Android, fáciles de probar |
| Degradación elegante | Lo que depende del equipo o de permisos (auriculares, uso del teléfono) muestra "no disponible" en vez de fallar |
| Preparada para sincronizar | Todas las entidades llevan `uuid` único, fechas de modificación y borrado lógico (`status`) desde el primer día |
| Privacidad por defecto | Los datos no salen del teléfono en el MVP |

**Alcance de este documento.** Cubre Android. La versión web y el ejecutable de Windows se describen solo a nivel de decisiones de diseño (sección 10), porque se detallarán cuando llegue su fase.

## 2. Stack y herramientas

Las versiones no se fijan en este documento: se usan las estables más recientes y se registran en el catálogo de versiones de Gradle (`libs.versions.toml`). Cualquier librería nueva se consulta antes de agregarla.

| Área | Elección | Para qué se usa | Cuándo entra |
| --- | --- | --- | --- |
| Lenguaje | Kotlin | Todo el código de la app | Fase 1 |
| Interfaz | Jetpack Compose con Material 3 | Pantallas, tema claro y oscuro | Fase 1 |
| Navegación | Navigation Compose | Rutas entre pantallas, barra inferior | Fase 1 |
| Asincronía | Corrutinas y Flow | Datos reactivos y tareas en segundo plano | Fase 1 |
| Base de datos | Room (SQLite) | Almacenamiento local con migraciones | Fase 1 |
| Inyección de dependencias | Hilt | Armado de repositorios y servicios | Fase 1 |
| Preferencias | DataStore | Ajustes del usuario | Fase 1 |
| Fechas | `java.time` | Fechas y horas (disponible desde Android 8.0) | Fase 1 |
| Alarmas | AlarmManager | Alarmas exactas de tareas | Fase 1 |
| Tareas periódicas | WorkManager | Reprogramar alarmas, generar recurrencias, revisar límites de uso | Fase 1 |
| Gráficos | Una librería de gráficos para Compose (a elegir) | Estadísticas de finanzas, macros, progreso | Fase 2 |
| Cámara y código de barras | CameraX y ML Kit de lectura de códigos | Escanear productos | Fase 3 |
| Red | Cliente HTTP con serialización JSON | Consulta a Open Food Facts (y luego Supabase) | Fase 3 |
| Audio | Media3 | Sesiones guiadas de meditación | Después del MVP |
| Sincronización | Supabase (Postgres, autenticación) | Cuenta y sincronización con la web | Fase 4 |
| Pruebas | JUnit, MockK, Turbine, pruebas de Compose, Room en memoria | Ver sección 12 | Fase 1 |

**Herramientas de desarrollo:** Android Studio (versión estable), Gradle con Kotlin DSL, Git con repositorio privado, un teléfono real con depuración USB y un emulador para pruebas rápidas.

**Parámetros de compilación:** Android mínimo 8.0 (API 26); objetivo y compilación con el nivel de API estable más reciente.

## 3. Arquitectura

La app tiene cuatro capas y cada una solo habla con la de abajo; los datos fluyen hacia arriba desde la base local hasta la pantalla.

&#91;embedded content: arquitectura · 4 capas y flujo de datos\]

La pantalla no consulta la base ni el sistema: pide todo al ViewModel, que a su vez usa repositorios, y Room es la fuente de verdad.

| Capa | Contiene | Reglas |
| --- | --- | --- |
| Interfaz | Pantallas de Compose y ViewModels | Los composables no acceden a la base ni a servicios del sistema |
| Dominio | Funciones puras con las reglas de negocio | No importan clases de Android, para poder probarlas sin dispositivo. Si una pantalla no tiene regla que aplicar, el ViewModel puede llamar directo al repositorio |
| Datos | Un repositorio por área | Son lo único que ven los ViewModels; ocultan si el dato viene de Room, de DataStore, del sistema o de la red |
| Fuentes | Room, DataStore, servicios del sistema Android y red | Envueltas en clases propias (por ejemplo, `AlarmScheduler`) para cambiarlas o simularlas en pruebas |

El flujo de datos es de un solo sentido: la pantalla manda eventos, el ViewModel actualiza el estado y la pantalla se redibuja con el nuevo estado.

## 4. Estructura del proyecto, navegación e interfaz

### 4.1 Paquetes

Un solo módulo de Gradle (`app`) con un paquete por área. Si el proyecto crece, cada paquete puede pasar a ser un módulo sin reescribir la lógica.

```text
app/src/main/java/.../stayfocused/
├── core/
│   ├── database/     AppDatabase, convertidores de tipos, migraciones
│   ├── timeline/     TimelineBlock, TimelineSource, repositorio de la línea de tiempo
│   ├── notifications/ canales, alarmas, receptores
│   ├── settings/     DataStore
│   ├── ui/           tema, componentes comunes, íconos y colores
│   └── util/         fechas, formatos de moneda y tiempo
├── planner/          data · domain · ui
├── finance/          data · domain · ui
├── gym/              data · domain · ui
├── nutrition/        data · domain · ui
├── meditation/       data · domain · ui
├── media/            servicio de escucha, reproductor, auriculares
├── screentime/       data · domain · ui
└── MainActivity.kt
```

Cada área sigue la misma separación: `data` (entidades, DAO, repositorio), `domain` (reglas puras) y `ui` (pantallas y ViewModels). Las áreas no se llaman entre sí: se comunican solo a través de `core`.

### 4.2 Navegación

Una sola actividad con un `NavHost` de Navigation Compose. La barra inferior tiene cinco destinos (Hoy, Finanzas, Gimnasio, Meditación y Tiempo de pantalla) y los demás se abren desde ellos. Dieta no es un destino propio: es una pestaña dentro de Gimnasio.

| Destino | Tipo | Pantalla de entrada |
| --- | --- | --- |
| Hoy | Barra inferior | Línea de tiempo del día |
| Finanzas | Barra inferior | Resumen financiero |
| Gimnasio | Barra inferior | Tab bar en el encabezado con dos pestañas: "Gimnasio" (por defecto: rutinas y última sesión) y "Dieta" |
| Dieta | Pestaña dentro de Gimnasio | Diario del día |
| Meditación | Barra inferior | Inicio de meditación |
| Música y auriculares | Secundario | Mini reproductor en "Hoy" y pantalla propia |
| Tiempo de pantalla | Barra inferior (último) | Uso del teléfono (pantalla provisoria hasta la fase 4) |
| Ajustes | Secundario | Se abre con un ícono de engranaje en "Hoy" (pantalla con flecha para volver) |

Los bloques de la línea de tiempo abren su pantalla de destino mediante una ruta de texto guardada en el propio bloque (ver sección 6).

### 4.3 Interfaz

- **Patrón de pantalla:** cada pantalla tiene un ViewModel que expone un único `StateFlow<UiState>` y recibe eventos; los composables solo dibujan.
- **Tema:** Material 3 con colores, tipografía y espaciados definidos en `core/ui`; modo claro, oscuro y automático. Paleta de colores y lista de íconos de tareas como constantes.
- **Textos:** todos en `strings.xml`, en español, preparados para traducir.
- **Accesibilidad:** áreas táctiles de al menos 48 dp, descripciones de contenido en íconos y estados que no dependan solo del color.
- **Tamaños de pantalla:** diseñada para teléfono; las tabletas usan el mismo diseño con ancho máximo. La adaptación a pantallas grandes queda para la versión web.

## 5. Modelo de datos en Room

Una sola base de datos (`AppDatabase`) con las tablas de todos los módulos. Los nombres de campos van en inglés, como el código.

### 5.1 Reglas generales

- **Campos de control** en todas las tablas: `id`, `uuid`, `createdAt` y `updatedAt` (milisegundos) y `status` (borrado lógico). Preparan la sincronización futura.
- **`id` y `uuid` se distinguen:** `id` es la clave primaria numérica autogenerada (`Long`), de uso interno; `uuid` es un texto de 36 caracteres en formato canónico `8-4-4-4-12` (32 dígitos hexadecimales y 4 guiones; ej. `550e8400-e29b-41d4-a716-446655440000`), equivalente a un número de 128 bits (16 bytes). Se genera en la app con `UUID.randomUUID().toString()`, tiene índice único y es el identificador para sincronizar.
- **Borrado lógico con `status`:** `0` = la fila existe; `-1` = la fila tiene borrado lógico aplicado. No se hacen `DELETE` físicos y las consultas de lectura filtran por `status = 0`.
- **Fechas:** un día se guarda como número de días desde 1970 (`epochDay`) con un convertidor de tipos a `LocalDate`, lo que permite buscar por rangos. La hora de una tarea o comida se guarda como minutos desde las 00:00.
- **Dinero:** enteros (`Long`) en la unidad mínima. Nunca decimales.
- **Relaciones:** claves foráneas con índice, que apuntan al `id` numérico (el `uuid` se usa solo para sincronizar). Al borrar un registro padre se hace borrado lógico, así que las filas hijas no se eliminan solas.
- **Colores e íconos:** claves de texto.
- **Migraciones:** cada cambio de esquema sube la versión de la base e incluye su migración escrita y probada; el esquema se exporta a la carpeta `schemas/` para versionarlo.
- **Ajustes** (tema, moneda, unidades, notificaciones): en DataStore, no en Room.
- **La línea de tiempo no tiene tabla propia:** sus bloques se calculan a partir de las tablas de cada módulo (ver sección 6).

### 5.2 Planificación

| Tabla | Campos principales | Notas |
| --- | --- | --- |
| `task` | `title`, `notes`, `date`, `startMinutes` (nulo = bandeja), `durationMinutes`, `iconKey`, `colorKey`, `status`, `completedAt`, `recurrenceType`, `recurrenceInterval`, `recurrenceDays` (máscara de días), `recurrenceEnd`, `routineId` | Una fila por tarea; las recurrentes guardan su regla acá |
| `subtask` | `taskId`, `title`, `done`, `position` | Hijas de una tarea |
| `task_occurrence` | `taskId`, `date`, `status`, `completedAt`, `skipped`, `overrideStartMinutes` | Solo existe cuando una ocurrencia de una tarea recurrente se completa, salta o se modifica; las demás se calculan |
| `routine` y `routine_item` | nombre; `title`, `offsetMinutes`, `durationMinutes`, íconos | Plantillas que generan tareas |
| `reminder` | `ownerType`, `ownerId`, `minutesBefore`, `kind` (aviso o alarma), `soundKey` | Sirve a tareas y a otros módulos |
| `tag` y tablas de cruce | `name`, `colorKey` | Compartidas con finanzas |

### 5.3 Finanzas

| Tabla | Campos principales | Notas |
| --- | --- | --- |
| `account` | `name`, `type`, `currency`, `initialBalanceCents` | El saldo se calcula, no se guarda |
| `category` | `name`, `iconKey`, `colorKey`, `kind` (ingreso o gasto) | Con categorías por defecto |
| `transaction` | `type` (ingreso, gasto, transferencia), `amountCents`, `date`, `accountId`, `categoryId`, `transferAccountId`, `note` | Índices por fecha, cuenta y categoría |
| `budget` | `categoryId` (nulo = total), `periodStartDay`, `amountCents` | Se compara con la suma de gastos del período |
| `recurring_transaction` | plantilla de transacción, `recurrence*`, `autoConfirm`, `nextDate` | La genera un trabajo diario |

### 5.4 Gimnasio

| Tabla | Campos principales | Notas |
| --- | --- | --- |
| `exercise` | `name`, `muscleGroup`, `equipment`, `trackingType`, `isCustom`, `notes` | Biblioteca precargada más propios |
| `workout_routine` | `name`, `scheduleDays` (máscara de días), `startMinutes` | Aparece como bloque los días programados |
| `routine_exercise` | `routineId`, `exerciseId`, `position`, `targetSets`, `targetReps`, `targetWeight`, `supersetGroup` (nulo si va solo) | Los ejercicios con el mismo `supersetGroup` forman una superserie o circuito |
| `workout_session` | `routineId`, `date`, `startedAt`, `endedAt`, `status`, `notes` | Nota de sesión |
| `session_exercise_note` | `sessionId`, `exerciseId`, `note` | Nota por ejercicio |
| `session_set` | `sessionId`, `exerciseId`, `position`, `weight`, `reps`, `type`, `completed` | Pesos en gramos enteros para evitar decimales |
| `personal_record` | `exerciseId`, `kind`, `value`, `sessionSetId`, `achievedAt` | Se deriva de las series; se guarda para consultarlo rápido |
| `body_measurement` | `date`, `kind`, `value` | Peso corporal y medidas |

### 5.5 Dieta, meditación y tiempo de pantalla

| Tabla | Campos principales | Notas |
| --- | --- | --- |
| `food` | `name`, `servingGrams`, `kcalPer100g`, `proteinPer100g`, `carbsPer100g`, `fatPer100g`, `barcode`, `source` | Propios o importados de Open Food Facts |
| `meal` | `date`, `startMinutes`, `slot` (desayuno, almuerzo, etc.) |  |
| `meal_item` | `mealId`, `foodId`, `name`, `grams`, `kcal`, `proteinG`, `carbsG`, `fatG` | Guarda los valores del momento: cambiar el alimento no altera los registros pasados |
| `nutrition_goal` | `dayOfWeek` (nulo = todos), `kcal`, `proteinG`, `carbsG`, `fatG` |  |
| `meditation_log` | `date`, `startedAt`, `durationSeconds`, `plannedSeconds`, `completed`, `type` | Alimenta historial y racha |
| `usage_limit` | `packageName` o `category`, `dailyMinutes`, `warnMinutesBefore`, `enabled` | Tiempo de pantalla |
| `focus_schedule` | `name`, `days`, `startMinutes`, `endMinutes`, `packages` | Tiempo de pantalla |
| `daily_app_usage` | `date`, `packageName`, `foregroundSeconds`, `sessions` | Copia en caché de lo que informa el sistema |

Las tablas de packs y sesiones guiadas de meditación se agregan cuando se incorporen los audios.

## 6. Línea de tiempo y sistema de bloques

La línea de tiempo es el eje de la app, y lo que hace posible sumar módulos sin tocar la pantalla "Hoy" es una interfaz común: cada módulo entrega sus bloques y un registro los junta.

&#91;embedded content: línea de tiempo · 6 fuentes, un registro, una pantalla\]

Los bloques no se guardan en ninguna tabla: se calculan a partir de los datos de cada módulo cada vez que cambian. Este es un boceto de la idea, no el código final:

```kotlin
data class TimelineBlock(
    val id: String,            // único dentro de su módulo
    val source: String,        // "planner", "gym", "nutrition", etc.
    val date: LocalDate,
    val startMinutes: Int,     // de 0 a 1439
    val durationMinutes: Int,
    val title: String,
    val iconKey: String,
    val colorKey: String,
    val completed: Boolean,
    val route: String,         // pantalla que se abre al tocar el bloque
)

interface TimelineSource {
    fun blocksFor(date: LocalDate): Flow<List<TimelineBlock>>
}
```

| Tema | Decisión |
| --- | --- |
| Registro de fuentes | Con Hilt, cada módulo declara su `TimelineSource` en un conjunto; sumar un módulo es registrar su fuente |
| Combinación | El repositorio de la línea de tiempo une los `Flow` de todas las fuentes y ordena por hora de inicio |
| Acomodo de superposiciones | Una sola función pura recibe todos los bloques, sin importar de qué módulo vengan |
| Qué entra | Solo bloques con hora; las tareas sin hora van a la bandeja |
| Completar | Desde la línea de tiempo solo se completan las tareas; los demás bloques se completan en su propia pantalla |
| Al tocar un bloque | Se navega a la ruta guardada en el bloque |
| Estilo | Cada módulo aporta un color e ícono por defecto; las tareas usan los suyos |
| Rendimiento | Cada fuente emite solo cuando cambian las tablas que usa |
| Dependencias | Las fuentes pertenecen a su módulo y el núcleo solo conoce la interfaz |

## 7. Alarmas, notificaciones y segundo plano

Las alarmas son la función más sensible de la app: tienen que sonar a la hora justa, con la app cerrada y después de reiniciar el teléfono. Las condiciones exactas de los permisos y de las restricciones de energía cambian entre versiones de Android, por lo que cada decisión de esta sección se verifica en la documentación oficial y en el teléfono real.

### 7.1 Cómo se programan

| Pieza | Responsabilidad |
| --- | --- |
| `AlarmScheduler` | Único punto de la app que habla con `AlarmManager`. Recibe un recordatorio y programa o cancela su alarma |
| Alarma exacta | Se usa una alarma exacta que funcione con el teléfono en reposo; antes de programarla se comprueba si el permiso está concedido y, si no, se guía al usuario a Ajustes |
| Ocurrencia siguiente | Solo se programa la próxima ocurrencia de cada recordatorio; al dispararse, se programa la siguiente. Así nunca hay cientos de alarmas pendientes |
| `ReminderReceiver` | Se activa cuando suena la alarma: muestra la notificación o la pantalla de alarma y programa la siguiente |
| `BootReceiver` | Escucha el reinicio del teléfono, el cambio de hora o de zona horaria y la actualización de la app; lanza un trabajo que vuelve a programar todo |
| `RescheduleWorker` | Trabajo de WorkManager que lee de la base los recordatorios próximos y vuelve a programar sus alarmas |

También se reprograma cuando se crea, edita, completa o borra una tarea, y una vez al abrir la app (red de seguridad).

### 7.2 Notificaciones

- **Canales separados** por módulo: tareas, alarmas, entrenamiento, comidas, meditación, presupuesto y tiempo de pantalla, para que el usuario los silencie por separado.
- **Tipos:** aviso común (notificación con acciones "Completar" y "Posponer") y alarma sonora (notificación de importancia máxima con pantalla completa).
- **Pantalla de alarma:** una actividad liviana que se muestra sobre el bloqueo, con "Posponer" y "Descartar".
- **Permiso de notificaciones:** se pide al crear el primer recordatorio, con una explicación previa.

### 7.3 Trabajos en segundo plano

| Trabajo | Mecanismo | Frecuencia |
| --- | --- | --- |
| Reprogramar alarmas | WorkManager (único) | Al reiniciar y al abrir la app |
| Generar transacciones recurrentes | WorkManager periódico | Una vez al día |
| Revisar límites de uso del teléfono | WorkManager periódico | Aproximadamente cada 15 minutos, que es el mínimo que permite el sistema |
| Recordatorio de días sin entrenar | WorkManager periódico | Una vez al día |
| Temporizador de meditación | Servicio en primer plano con notificación | Mientras dure la sesión |
| Fin del descanso entre series | Corrutina mientras la app está abierta; alarma exacta si pasa a segundo plano | Por serie |

### 7.4 Riesgos conocidos

- Algunos fabricantes (Xiaomi, Samsung, Huawei, entre otros) frenan las apps en segundo plano. Se incluye en Ajustes una guía para excluir la app del ahorro de batería y una pantalla de diagnóstico que muestra el estado de los permisos.
- Los avisos de límite de uso pueden llegar con una demora de hasta unos 15 minutos; es una limitación aceptada en la v1.

## 8. Diseño técnico por módulo

Solo se detalla lo que tiene una decisión técnica no obvia; el comportamiento está en el documento funcional.

### 8.1 Planificación

- **Recurrencia:** la regla vive en la tarea; las ocurrencias se calculan al mostrar un día con una función pura (`occurrencesOn(date)`). Solo se guarda una fila en `task_occurrence` cuando una ocurrencia se completa, se salta o se modifica. Editar "esta y siguientes" divide la regla en dos tareas.
- **Acomodo de bloques superpuestos:** función pura que agrupa las tareas que se pisan y asigna columnas; recibe una lista y devuelve posiciones, y se prueba sin Android.
- **Rutinas:** al aplicar una rutina a un día se crean tareas normales enlazadas por `routineId`; editar la rutina no cambia las tareas ya creadas.

### 8.2 Finanzas

- Todo en enteros de centavos; el formato de moneda se resuelve al mostrar, con la moneda de la cuenta.
- **Saldo, presupuesto y estadísticas:** consultas SQL con `SUM` agrupadas por cuenta, categoría o mes, expuestas como `Flow` para que la pantalla se actualice sola.
- **Transferencias:** una sola fila con cuenta de origen y de destino, que usa `transferAccountId` y se excluye de ingresos y gastos en las consultas.
- **Recurrentes:** un trabajo diario crea las transacciones vencidas o, si no son automáticas, una notificación para confirmarlas.
- **Exportar a CSV:** con el selector de archivos del sistema, sin permisos de almacenamiento.

### 8.3 Gimnasio

- **1RM estimado:** fórmula de Epley, `peso × (1 + repeticiones / 30)`, en una función pura.
- **Récords:** al completar una serie se compara con el historial del ejercicio (mayor peso, más repeticiones con un peso, mayor volumen, mayor 1RM). Solo cuentan las series que no son de calentamiento.
- **Pesos:** en gramos enteros; se convierten a kg o lb solo al mostrar.
- **Sesión en curso:** el estado vive en un ViewModel y se guarda en la base serie por serie, para no perder nada si la app se cierra.
- **Superseries y circuitos:** ejercicios con el mismo `supersetGroup` se muestran juntos y el descanso se activa al terminar la última serie de la vuelta.
- **Mapa del cuerpo:** una imagen vectorial con una región por grupo muscular; el color se calcula con el volumen de series normales de los últimos 7 días por grupo, y cada ejercicio declara su grupo principal.
- **Recordatorio de inactividad:** el trabajo diario compara la fecha de la última sesión con el límite elegido.
- **Exportar a CSV:** igual que en finanzas.

### 8.4 Dieta

- Los valores de un alimento se escalan por gramos al registrarlo y se copian al `meal_item`.
- **Open Food Facts:** consulta por código de barras o por texto con su API pública; se respeta su requisito de identificar la app en cada pedido y se muestra la atribución. El resultado se guarda en la tabla `food`, y si no hay conexión solo se usan los alimentos locales.
- **Escáner:** CameraX para la imagen y ML Kit para leer el código; el permiso de cámara se pide al tocar "Escanear".
- **Anillos de macros:** el total del día se calcula con una consulta sobre los `meal_item` del día.

### 8.5 Meditación (MVP)

- **Temporizador:** servicio en primer plano con notificación, basado en el reloj monotónico del sistema (no en contar segundos), con campanas de inicio, fin e intervalo.
- **Campanas:** sonidos cortos incluidos en la app y reproducidos con una API liviana de efectos.
- **Racha:** función pura sobre `meditation_log` que cuenta días seguidos con al menos una sesión completada (80 % o más de lo planeado).
- **Sesiones guiadas (después):** reproductor Media3 con servicio de medios y controles en la notificación; los audios se descargan y se guardan localmente.

### 8.6 Música y auriculares

| Función | Mecanismo | Observaciones |
| --- | --- | --- |
| Canción en reproducción | Un servicio de escucha de notificaciones habilita leer las sesiones de medios activas y sus metadatos | El usuario lo activa a mano en Ajustes; la app guía el paso |
| Controles | Los comandos de transporte del controlador de la sesión activa (reproducir, pausar, siguiente, anterior) | Dependen de lo que permita la app de música |
| Batería de auriculares | En capas: servicio estándar de batería por Bluetooth de bajo consumo si el auricular lo expone; evento de batería del fabricante vía el perfil de manos libres donde exista; si no, "no disponible" | No se usa la API oculta del sistema. Todo se valida con los auriculares reales |

### 8.7 Tiempo de pantalla

- **Lectura de uso:** `UsageStatsManager`, que exige el acceso especial a datos de uso; se comprueba si está concedido y se guía al usuario a Ajustes.
- **Resumen diario:** un trabajo guarda el uso por app en `daily_app_usage` para el historial y para no consultar siempre al sistema.
- **Nombres e íconos de apps:** `PackageManager`; las apps sin ícono usan uno genérico.
- **Límites:** el trabajo periódico compara el uso del día con `usage_limit` y notifica al acercarse y al llegar. No se bloquean apps ni se usa accesibilidad.
- **Modo enfoque:** en la v1 solo silencia avisos propios y muestra un recordatorio; el bloqueo real queda para una versión posterior.

## 9. Permisos del manifiesto de Android

Como la app no se publica en Google Play, no hay revisión de permisos de la tienda. Igual, cada permiso se declara solo cuando su módulo entra en desarrollo y se pide en el momento en que hace falta, con una explicación previa. Los nombres y las condiciones se verifican en la documentación oficial del nivel de API usado.

| Función | Permiso o mecanismo | Tipo | Se pide |
| --- | --- | --- | --- |
| Notificaciones | `POST_NOTIFICATIONS` | En ejecución | Al crear el primer recordatorio |
| Alarmas exactas | `USE_EXACT_ALARM` o `SCHEDULE_EXACT_ALARM` | Especial o de instalación | Al activar alarmas sonoras; se comprueba su estado antes de programar |
| Reprogramar tras reiniciar | `RECEIVE_BOOT_COMPLETED` | De instalación | No se pide |
| Pantalla de alarma sobre el bloqueo | `USE_FULL_SCREEN_INTENT` | De instalación o especial | Al activar alarmas sonoras |
| Vibración | `VIBRATE` | De instalación | No se pide |
| Servicios en primer plano | `FOREGROUND_SERVICE` y el permiso del tipo que corresponda (reproducción de medios) | De instalación | No se pide |
| Internet | `INTERNET` | De instalación | No se pide (entra con Open Food Facts) |
| Cámara | `CAMERA` | En ejecución | Al tocar "Escanear" |
| Auriculares Bluetooth | `BLUETOOTH_CONNECT` | En ejecución | Al abrir la sección de auriculares |
| Música en reproducción | Acceso a notificaciones (servicio de escucha) | Especial, se activa en Ajustes | Al abrir el módulo de música |
| Tiempo de pantalla | Acceso a datos de uso | Especial, se activa en Ajustes | Al abrir el módulo por primera vez |
| Bloqueo de la app | Biometría (`USE_BIOMETRIC`) | De instalación | Al activar el bloqueo |

**Reglas**

- Cualquier cambio en el manifiesto lo confirma el desarrollador antes de hacerlo.
- Si el usuario rechaza un permiso, la función asociada se deshabilita con una explicación y el resto de la app sigue funcionando.
- Una pantalla de diagnóstico en Ajustes muestra el estado de cada permiso y lleva directo a la pantalla de Ajustes del sistema que corresponda.

## 10. Sincronización con Supabase y versión web (futuro)

Nada de esto se construye en el MVP. Se describe para que las decisiones de hoy no obliguen a rehacer la base de datos mañana.

### 10.1 Cómo se prepara el diseño desde ahora

- Todas las tablas tienen `uuid` generado en la app (distinto del `id` numérico interno), `updatedAt` y `status`, que son lo que necesita un esquema de sincronización.
- Los datos no dependen de la zona horaria (la hora de una tarea son minutos desde las 00:00).
- Los borrados son lógicos, así que otro dispositivo se entera de que algo se eliminó.
- Las reglas de negocio están en funciones puras, que se pueden reescribir en otro lenguaje para la web con las mismas pruebas como guía.

### 10.2 Diseño de la sincronización

| Tema | Decisión propuesta |
| --- | --- |
| Servicio | Supabase: base Postgres y autenticación por correo y contraseña |
| Esquema remoto | Una tabla por cada tabla local, con las mismas columnas más `user_id` |
| Seguridad | Reglas de acceso por fila (RLS) para que cada usuario solo vea y escriba sus filas. Sin RLS no hay seguridad, así que es lo primero que se configura |
| Qué se envía | Las filas modificadas desde la última sincronización (marcadas como pendientes en una tabla de cola local) |
| Qué se recibe | Las filas del servidor con `updatedAt` posterior a la última sincronización |
| Conflictos | Gana la modificación más reciente de cada fila; el borrado lógico cuenta como modificación |
| Cuándo | Al abrir la app, tras cambios con conexión y de forma periódica; siempre en segundo plano |
| Módulos | Se puede activar o excluir por módulo (por ejemplo, dejar fuera las finanzas) |
| Datos que no se sincronizan | Los de uso del teléfono (tiempo de pantalla) y los ajustes del dispositivo |

Las claves de Supabase que van en la app son las públicas; la clave de servicio nunca se incluye en el código ni en el repositorio.

### 10.3 Versión web y escritorio

| Tema | Propuesta |
| --- | --- |
| Web | Aplicación web instalable (PWA) en TypeScript, que lee y escribe en Supabase con un caché local para uso sin conexión |
| Escritorio | La misma web empaquetada para Windows, con una herramienta de empaquetado a elegir cuando llegue el momento |
| Código compartido | No se comparte código con Android; se comparten el esquema de datos y las reglas, documentados y con pruebas equivalentes |
| Alcance | Planificación, finanzas, gimnasio (planificación e historial), dieta y meditación. Música y tiempo de pantalla no existen en la web |

## 11. Seguridad, privacidad y respaldo

La app guarda datos de dinero, salud y hábitos, así que se trata como sensible aunque sea de uso personal.

| Tema | Decisión |
| --- | --- |
| Datos en el teléfono | Quedan en el almacenamiento privado de la app, al que otras apps no acceden. Cifrar la base (por ejemplo, con SQLCipher) queda como decisión pendiente |
| Copias automáticas de Android | Se desactivan o se excluyen los datos de la app (reglas de extracción de datos), para que no suban a la nube sin que el usuario lo decida |
| Bloqueo de la app | Opcional, con huella o PIN del teléfono (biometría del sistema) |
| Claves y secretos | Nunca en el código ni en el repositorio. Las claves se guardan en archivos locales fuera de Git o en `local.properties` |
| Firma del APK | La clave de firma se guarda fuera del repositorio y con copia de resguardo, porque sin la misma clave no se puede actualizar la app instalada |
| Registros (logs) | No se escriben montos, alimentos, notas ni datos de salud en los registros |
| Red | Solo HTTPS. En el MVP las únicas conexiones son a Open Food Facts |
| Datos de uso del teléfono y de música | Se procesan en el teléfono y nunca se envían a ningún servidor |
| Sesión de Supabase (futuro) | Los tokens se guardan cifrados con el almacén seguro de claves del sistema |

### Respaldo

Sin sincronización ni copia en la nube, perder el teléfono o reinstalar la app sin respaldo significa perder todo. Por eso:

- **Exportar e importar** todos los datos en un archivo JSON con número de versión del formato, usando el selector de archivos del sistema.
- Exportar **antes de cada actualización** del APK y de forma periódica; la app recuerda hacerlo cada cierto tiempo.
- La importación valida el archivo y, si es de una versión anterior, lo convierte antes de cargarlo.
- Se prueba con regularidad que un archivo exportado se puede importar en una instalación limpia.

## 12. Pruebas, entorno y compilación

### 12.1 Estrategia de pruebas

La mayor parte del esfuerzo va a la lógica de negocio, que es donde un error cuesta más y donde probar es más barato.

| Nivel | Qué cubre | Herramientas | Cuánto |
| --- | --- | --- | --- |
| Unitarias | Funciones puras de `domain`: acomodo de bloques, recurrencias, saldos y presupuestos, 1RM y récords, macros, rachas | JUnit | Mucho: es la base |
| De datos | DAO, consultas y migraciones con una base en memoria | Room en memoria, pruebas instrumentadas | Cada tabla y cada migración |
| De ViewModel | Estados que cambian ante cada evento | Turbine y MockK | Los flujos principales |
| De interfaz | Pantallas clave: mostrar, crear, completar | Pruebas de Compose | Pocas, solo caminos críticos |
| Manuales | Alarmas, permisos, auriculares, música y tiempo de pantalla en el teléfono real | Lista de comprobación de cada mini-especificación | Antes de dar una fase por terminada |

Las alarmas y las funciones que dependen del equipo no se pueden probar bien con pruebas automáticas: se verifican a mano en el teléfono y con el teléfono en reposo.

### 12.2 Entorno

- Android Studio estable, con el SDK del nivel de API objetivo.
- Un teléfono real con depuración USB como entorno principal de prueba; un emulador para pruebas rápidas de interfaz.
- Repositorio Git privado; una rama por tarea y un commit por tarea terminada.
- Opcional: ejecutar las pruebas y la revisión de estilo automáticamente en cada cambio con GitHub Actions.

### 12.3 Compilación e instalación

| Qué | Cómo |
| --- | --- |
| Compilar para probar | `./gradlew assembleDebug` y se instala desde Android Studio o copiando el APK |
| Pruebas | `./gradlew test` y `./gradlew connectedAndroidTest` |
| Revisión de estilo | `./gradlew lint` |
| Versión para uso diario | Un APK firmado con una clave propia y estable, instalado a mano en el teléfono |
| Actualizar la app | Instalar encima del APK anterior: solo funciona si se firma siempre con la misma clave; si no, hay que desinstalar y se pierden los datos |
| Antes de actualizar | Exportar el respaldo JSON |
| Versionado | `versionCode` aumenta en cada APK instalado; `versionName` sigue un esquema simple (0.1, 0.2, …) |

## 13. Plan técnico, riesgos y decisiones pendientes

### 13.1 Qué se construye en cada fase

Sigue las cinco fases de la hoja de ruta del documento funcional; acá se detalla lo técnico de cada una.

| Fase | Entregables técnicos |
| --- | --- |
| 1. Base y planificación | Proyecto con Compose, Hilt, Room y DataStore; tema y navegación; `AppDatabase` con exportación de esquema; entidad `task` y línea de tiempo (mini-especificación 01); subtareas, recurrencia y rutinas; bandeja y arrastre de bloques; sistema de bloques (`TimelineSource`); alarmas con reprogramación tras reiniciar; canales de notificación; widget básico |
| 2. Finanzas y gimnasio | Tablas y consultas de ambos módulos; librería de gráficos; presupuestos con alertas; sesión en curso con descanso, récords y superseries; mapa del cuerpo; exportación a CSV |
| 3. Dieta y meditación | Base de alimentos propia y consulta a Open Food Facts; escáner con CameraX y ML Kit; anillos de macros; temporizador de meditación con servicio en primer plano, campanas y racha |
| 4. Computadora, música y pantalla | Servicio de escucha de notificaciones y controles de música; lectura de batería de auriculares por capas; lectura de uso del teléfono y límites; cuenta y sincronización con Supabase; versión web instalable |
| 5. Pulido y versión final | Widgets restantes, búsqueda global, bloqueo biométrico, resumen semanal, diagnóstico de permisos, respaldo JSON completo, accesibilidad con TalkBack y APK firmado con clave estable |

### 13.2 Riesgos técnicos

| Riesgo | Impacto | Mitigación |
| --- | --- | --- |
| Las alarmas no suenan por el ahorro de batería del fabricante | Alto | Pantalla de diagnóstico, guía para excluir la app del ahorro, pruebas con el teléfono en reposo y reprogramación al abrir la app |
| Pérdida de datos al actualizar con una firma distinta | Alto | Clave de firma estable con copia de resguardo y respaldo JSON antes de cada actualización |
| Primer proyecto en Kotlin: estructura que se desordena | Alto | `CLAUDE.md`, mini-especificaciones chicas, un paso por sesión, revisión del código antes de aceptarlo |
| Migraciones de Room mal hechas | Alto | Esquema exportado, una migración escrita y probada por cada cambio |
| Cambios en las reglas de Android (permisos, restricciones de segundo plano) | Medio | Consultar la documentación oficial al empezar cada módulo y probar en el teléfono real |
| Batería de auriculares o música no disponibles en el equipo | Bajo | Mostrar "no disponible" sin errores; ninguna otra función depende de esto |
| Avisos de límite de uso con demora | Bajo | Limitación aceptada; se informa en la pantalla de límites |
| Conflictos de sincronización (futuro) | Medio | Última modificación gana, borrado lógico, reglas de acceso por fila y pruebas de conflictos |
| El alcance crece más rápido que el aprendizaje | Alto | Una fase por vez; cada fase termina con la app usable |

### 13.3 Decisiones pendientes

- [ ] Librería de gráficos para Compose (se decide al empezar la fase 2)
- [ ] ¿Cifrar la base de datos local (SQLCipher) o alcanza con el almacenamiento privado de la app?
- [ ] ¿Adelantar el respaldo JSON básico a la fase 1, en vez de la 5?
- [ ] ¿Separar la fase 4 en dos: música y tiempo de pantalla por un lado, sincronización y web por otro?
- [ ] Biblioteca de ejercicios precargada: ¿propia o con una licencia abierta?
- [ ] Herramienta para empaquetar el ejecutable de Windows (cuando llegue su momento)
