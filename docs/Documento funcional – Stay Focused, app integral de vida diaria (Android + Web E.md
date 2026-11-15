# Documento funcional – Stay Focused, app integral de vida diaria (Android + Web/Escritorio)

Nov 14, 2026 · @Ignacio

## 1. Introducción

La app organiza el día en una línea de tiempo visual y reúne en un solo lugar la planificación, las finanzas, el entrenamiento, la alimentación, la meditación y el control de la música. Nace para Android y se complementa con una versión para computadora (navegador y ejecutable) que comparte los mismos datos.

**Nombre e identidad.** La app se llama **Stay Focused**. El ícono queda pendiente: se diseña y se aprueba antes de la publicación (ver preguntas abiertas en la sección 18.3).

**Visión.** Que una persona abra una sola app por la mañana y vea qué tiene que hacer, entrenar, comer y meditar ese día, y pueda registrar cada cosa sin salir de la línea de tiempo.

**Inspiraciones.** Se toma la *experiencia de uso* de cada referencia; el diseño visual, los textos y los contenidos son propios.

| Módulo | Referencia | Qué se toma | Qué no se copia |
| --- | --- | --- | --- |
| Planificación | Structured | Línea de tiempo vertical, bloques con duración, marca de "ahora", bandeja de tareas sin hora | Íconos, paleta, textos e identidad de marca |
| Meditación | Medito | Interfaz calma, packs de sesiones, temporizador libre, rachas | Audios y marca (el código abierto no implica que el contenido sea reutilizable) |
| Gimnasio | Symmetry | Registro de series rápido, rutinas, progreso por ejercicio (funciones asumidas, a validar) | Diseño, nombres y biblioteca de ejercicios propietaria |
| Tiempo de pantalla y enfoque | StayFree | Medición del tiempo de uso por app con gráficos e historial, límites de uso con avisos, recordatorios de uso excesivo y modo enfoque con horarios | Marca, íconos, textos y diseño visual (funciones asumidas a partir de su descripción pública, a validar) |

**Principios de diseño.**

- La línea de tiempo es el centro: los demás módulos aportan bloques a ella.
- Registrar algo debe llevar pocos toques (idealmente menos de 5 segundos para un gasto o una serie).
- Offline-first: todo funciona sin conexión; la sincronización es un extra.
- Las funciones que dependen del dispositivo (auriculares, audios) se degradan con elegancia.
- Interfaz calma y sin ruido visual, con modo claro y oscuro.

## 2. Alcance

La versión 1 entrega la línea de tiempo y seis módulos funcionales en Android; la versión web con datos sincronizados llega en una fase posterior, cuando la app del teléfono ya funciona. Lo que depende del hardware o de contenido con licencia se entrega en una versión básica y crece después.

| Área | Entra en la v1 | Queda para versiones siguientes |
| --- | --- | --- |
| Planificación | Tareas, subtareas, rutinas, recurrencia, línea de tiempo, alarmas, bandeja, vista semanal, temporizador de enfoque | Integración con calendarios externos (Google Calendar), tareas compartidas |
| Finanzas | Ingresos, gastos, categorías, presupuestos, estadísticas, múltiples cuentas | Importación de extractos bancarios, conexión con bancos, multimoneda con cotización automática |
| Gimnasio | Ejercicios propios y precargados, rutinas, sesiones, descanso, récords, gráficos | Planes sugeridos, medidas corporales con fotos, integración con relojes |
| Dieta | Registro manual, alimentos propios, objetivos de macros, búsqueda en base abierta, escaneo de código de barras | Recetas, plan semanal de comidas, micronutrientes |
| Meditación | Temporizador con campanas y rachas | Sesiones guiadas con audios, packs de contenido, sonidos de sueño avanzados |
| Música y auriculares | Canción en reproducción, controles básicos, estado y batería cuando el dispositivo lo permita | Controles avanzados por marca de auriculares |
| Computadora | Versión web instalable (PWA) con sincronización mediante Supabase, una vez que la app del teléfono funcione | Ejecutable de escritorio empaquetado, solo para Windows |
| Tiempo de pantalla | Tiempo de uso total y por app, historial con gráficos, límites con aviso, recordatorio de uso excesivo, modo enfoque ligado a la línea de tiempo | Bloqueo de apps al superar el límite, seguimiento de sitios web, límites compartidos entre dispositivos, versión para computadora |

**Supuestos.**

- Primer idioma: español (Argentina), con la estructura preparada para otros idiomas.
- Moneda inicial: peso argentino (ARS), con soporte para elegir otras.
- Versión mínima de Android: 8.0 (API 26) o superior (confirmado).
- El usuario crea una cuenta solo si quiere sincronizar con la computadora; sin cuenta, todo queda en el teléfono.
- Las funciones de otras apps (por ejemplo, la música) dependen de lo que esas apps expongan.

**Fuera de alcance.** Asesoramiento médico, nutricional o financiero; red social; compra de contenido dentro de la app; versión para iOS; publicación en Google Play (la app es de uso personal, gratuita y se instala directamente en el teléfono).

## 3. Usuarios y casos de uso

La app apunta a adultos que quieren ordenar su rutina y sus hábitos en una sola herramienta, sin pagar ni configurar cinco apps distintas.

| Perfil | Necesidad principal | Módulos que más usa |
| --- | --- | --- |
| Profesional con agenda cargada | Ver el día completo y no olvidar nada | Planificación, Finanzas |
| Persona que entrena con regularidad | Registrar series y ver progreso | Gimnasio, Dieta, Planificación |
| Persona que busca equilibrio | Armar hábitos y bajar el estrés | Meditación, Planificación, Música |
| Estudiante | Organizar estudio y gastos chicos | Planificación, Finanzas, Música |

**Casos de uso principales.**

| ID | Caso de uso | Resultado esperado |
| --- | --- | --- |
| CU-01 | Armar mi día a la mañana | Veo en la línea de tiempo tareas, entrenamiento, comidas y meditación, y arrastro las pendientes de la bandeja |
| CU-02 | Registrar un gasto al instante | Cargo monto y categoría en pocos toques y veo cuánto queda del presupuesto |
| CU-03 | Entrenar según mi rutina | Abro la sesión, marco series, uso el descanso y la app detecta mis récords |
| CU-04 | Cargar lo que comí | Registro una comida y comparo mis macros contra el objetivo del día |
| CU-05 | Meditar 10 minutos | Inicio el temporizador o una sesión guiada y se suma a mi racha |
| CU-06 | Controlar la música sin cambiar de app | Veo la canción actual, pauso o salto, y consulto la batería de mis auriculares |
| CU-07 | Seguir mi día desde la computadora | Abro la web o el ejecutable, veo mi línea de tiempo y cargo tareas o gastos con teclado |
| CU-08 | Revisar mi semana | Consulto estadísticas de gastos, volumen entrenado, macros y meditación |

## 4. Arquitectura funcional y plataformas

La app funciona primero en el teléfono, sin conexión; un servicio de cuenta y sincronización, opcional en Android, lleva los mismos datos a la web y al programa de escritorio.

&#91;embedded content: arquitectura · teléfono, servidor de sincronización y computadora\]

El teléfono guarda todo en una base local y funciona por sí solo; el servidor de sincronización (resaltado) es lo único que une el teléfono con la computadora.

### 4.1 Decisiones de arquitectura

| Tema | Propuesta | Motivo |
| --- | --- | --- |
| App móvil | Android nativo con Kotlin y Jetpack Compose | Acceso directo a alarmas, notificaciones, sesiones multimedia y Bluetooth |
| Organización | Una sola app con un paquete por área sobre un núcleo común (tema, base de datos, utilidades de fecha); se puede separar en módulos más adelante | Mantiene el proyecto simple para una sola persona y permite entregar un área por vez |
| Datos | Base de datos local en el teléfono como fuente principal (offline-first) | Funciona sin conexión y la privacidad es la opción por defecto |
| Computadora | Aplicación web instalable, empaquetada después como programa de escritorio | Un solo código para navegador y ejecutable (sección 13) |
| Sincronización | Servicio de cuenta opcional (se usará Supabase, en una fase posterior); ante un conflicto gana el cambio más reciente | Simple y suficiente para uso personal |
| Línea de tiempo | Sistema de bloques genérico: cada módulo registra sus propios tipos de evento | Se agregan módulos sin tocar el núcleo |

### 4.2 Bloques que cada módulo aporta a la línea de tiempo

| Módulo | Tipo de bloque | Al tocarlo |
| --- | --- | --- |
| Planificación | Tarea, rutina | Abre el detalle y permite completarla |
| Finanzas | Vencimiento de pago | Abre el alta de la transacción |
| Gimnasio | Sesión programada | Abre la sesión en curso |
| Dieta | Comida planificada | Abre el diario de comidas |
| Meditación | Sesión programada | Inicia el temporizador o la sesión guiada |
| Música | No aporta bloques | No aplica |

## 5. Módulo Planificación diaria

Es el módulo central: muestra el día como una línea de tiempo vertical con una marca de "ahora", y es donde aparecen también los bloques de todos los demás módulos. Se inspira en la experiencia de Structured.

### 5.1 Requisitos funcionales

| ID | Requisito | Prioridad |
| --- | --- | --- |
| PL-01 | Mostrar el día en una línea de tiempo vertical con escala horaria y marca de "ahora" que avanza sola | Alta |
| PL-02 | Mostrar cada tarea como un bloque cuyo alto es proporcional a su duración, con ícono y color propios | Alta |
| PL-03 | Mostrar los huecos libres entre bloques como espacio vacío tocable para crear una tarea ahí | Media |
| PL-04 | Crear tarea con título, fecha, hora de inicio, duración, ícono, color, notas y subtareas | Alta |
| PL-05 | Marcar tareas y subtareas como completadas con un toque, con animación y progreso del día | Alta |
| PL-06 | Bandeja de entrada (Inbox) para tareas sin hora, que se arrastran a la línea de tiempo | Alta |
| PL-07 | Mover y redimensionar bloques arrastrando sobre la línea de tiempo | Alta |
| PL-08 | Tareas recurrentes: diaria, semanal, días específicos, mensual, cada N días, con fecha de fin opcional | Alta |
| PL-09 | Rutinas: conjuntos de tareas con horarios relativos (por ejemplo, rutina de mañana) que se aplican a un día con un toque | Media |
| PL-10 | Recordatorios y alarmas por tarea: a la hora, X minutos antes, o varias por tarea | Alta |
| PL-11 | Tipos de alarma: notificación simple o alarma sonora con pantalla completa, con posponer y descartar | Alta |
| PL-12 | Vista semanal con navegación por día y resumen de carga | Media |
| PL-13 | Temporizador de enfoque para la tarea actual, con pausa y aviso de fin | Media |
| PL-14 | Tareas atrasadas: mostrar las no completadas y permitir reprogramarlas o llevarlas a la bandeja | Media |
| PL-15 | Editar o borrar una tarea recurrente con opciones "solo esta", "esta y siguientes" o "todas" | Media |
| PL-16 | Widget de pantalla de inicio con las próximas tareas y accesos rápidos | Media |
| PL-17 | Mostrar en la línea de tiempo los bloques de otros módulos (entrenamiento, comida, meditación) con su propio estilo | Alta |
| PL-18 | Buscar tareas por texto y filtrar por estado, color o etiqueta | Baja |

### 5.2 Reglas de negocio

- Las tareas pueden superponerse en horario; en ese caso se muestran lado a lado y la app avisa del conflicto sin impedirlo.
- Una tarea sin hora va a la bandeja; una con hora pero sin duración usa 30 minutos por defecto (configurable).
- Una tarea recurrente genera ocurrencias; completar una ocurrencia no completa las demás.
- Las alarmas deben reprogramarse tras reiniciar el teléfono, cambiar la zona horaria o editar la tarea.
- Una tarea completada queda visible (tachada) en la línea de tiempo del día en que se hizo.

### 5.3 Flujo principal: crear una tarea

1. El usuario toca el botón "+" o un hueco libre de la línea de tiempo.
2. Ingresa título y, opcionalmente, hora, duración, ícono, color, notas y subtareas.
3. Elige repetición y recordatorios si los necesita.
4. Guarda: el bloque aparece en la línea de tiempo (o en la bandeja si no tiene hora) y se programan las alarmas.

### 5.4 Pantallas

- Línea de tiempo del día (pantalla principal)
- Bandeja de entrada
- Vista semanal
- Detalle y edición de tarea
- Editor de rutinas
- Temporizador de enfoque
- Pantalla de alarma (pantalla completa)

## 6. Módulo Finanzas personales

Permite registrar ingresos y gastos en pocos toques, fijar presupuestos por categoría y ver estadísticas claras de adónde va el dinero. No se conecta a bancos: todo es registro manual.

### 6.1 Requisitos funcionales

| ID | Requisito | Prioridad |
| --- | --- | --- |
| FI-01 | Registrar transacciones de tipo ingreso, gasto y transferencia entre cuentas | Alta |
| FI-02 | Cada transacción tiene monto, fecha, categoría, cuenta, nota y etiquetas opcionales | Alta |
| FI-03 | Alta rápida de gasto desde un acceso directo (botón flotante, widget o atajo) | Alta |
| FI-04 | Categorías predefinidas y personalizadas, con ícono y color | Alta |
| FI-05 | Múltiples cuentas (efectivo, banco, tarjeta, billetera virtual) con saldo calculado | Alta |
| FI-06 | Presupuestos mensuales por categoría y total, con barra de progreso y alertas al llegar al 80 % y al 100 % | Alta |
| FI-07 | Transacciones recurrentes (alquiler, suscripciones, sueldo) que se generan solas o con confirmación | Media |
| FI-08 | Estadísticas: gasto por categoría (torta), evolución mensual (barras o líneas), ingresos contra gastos | Alta |
| FI-09 | Filtros por período, categoría, cuenta y etiqueta; buscador por nota | Media |
| FI-10 | Resumen del mes: ingresos, gastos, ahorro y comparación con el mes anterior | Media |
| FI-11 | Elegir moneda principal; soporte para registrar en otras monedas con tipo de cambio manual | Baja |
| FI-12 | Exportar transacciones a CSV | Media |
| FI-13 | Vencimientos de pagos como recordatorios que aparecen en la línea de tiempo | Media |

### 6.2 Reglas de negocio

- El saldo de una cuenta es la suma de sus ingresos menos gastos y transferencias salientes, más las entrantes.
- Una transferencia no cuenta como ingreso ni como gasto en las estadísticas.
- El presupuesto se calcula sobre el mes calendario, con opción de elegir el día de inicio del ciclo.
- Borrar una categoría con transacciones exige reasignarlas a otra.
- Los montos se guardan como enteros en la unidad mínima (centavos) para evitar errores de redondeo.

### 6.3 Pantallas

- Resumen financiero (saldo total, presupuesto del mes, últimas transacciones)
- Lista de transacciones con filtros
- Alta y edición de transacción
- Cuentas
- Presupuestos
- Estadísticas
- Categorías y etiquetas

## 7. Módulo Gimnasio

Permite armar rutinas, registrar cada serie con muy pocos toques durante el entrenamiento y seguir el progreso y los récords personales. Se inspira en el espíritu de Symmetry; las funciones concretas de esa app están por validar con el equipo.

### 7.1 Requisitos funcionales

| ID | Requisito | Prioridad |
| --- | --- | --- |
| GY-01 | Biblioteca de ejercicios con nombre, grupo muscular, equipamiento y tipo de registro (peso y repeticiones, tiempo, solo repeticiones) | Alta |
| GY-02 | Crear, editar y borrar ejercicios propios, con notas y enlace o imagen opcional | Alta |
| GY-03 | Crear rutinas (por ejemplo Push, Pull, Legs) con ejercicios ordenados, series, repeticiones y peso objetivo | Alta |
| GY-04 | Programar una rutina en días de la semana; aparece como bloque en la línea de tiempo | Alta |
| GY-05 | Modo sesión: pantalla de entrenamiento en curso donde se marca cada serie con pocos toques | Alta |
| GY-06 | Mostrar el resultado de la sesión anterior del mismo ejercicio para tomarlo de referencia | Alta |
| GY-07 | Temporizador de descanso automático entre series, configurable por ejercicio, con aviso sonoro y vibración | Alta |
| GY-08 | Agregar, quitar o reordenar ejercicios y series durante la sesión | Media |
| GY-09 | Detectar récords personales: mayor peso, más repeticiones con un peso, mayor volumen y 1RM estimado | Alta |
| GY-10 | Historial de sesiones con duración, volumen total y ejercicios realizados | Alta |
| GY-11 | Gráficos de progreso por ejercicio (peso, volumen, 1RM) y volumen semanal por grupo muscular | Media |
| GY-12 | Tipos de serie: calentamiento, normal, fallo y descendente | Baja |
| GY-13 | Registro de peso corporal y medidas con gráfico | Baja |
| GY-14 | Guardar una sesión libre sin rutina previa y convertirla en rutina | Media |
| GY-15 | Unidades configurables (kg o lb) y discos disponibles para sugerir cargas | Baja |
| GY-16 | Superseries y circuitos: agrupar dos o más ejercicios que se hacen seguidos, con el descanso al final de cada vuelta | Media |
| GY-17 | Notas por ejercicio y por sesión, visibles al repetir la rutina | Media |
| GY-18 | Mapa del cuerpo con los músculos trabajados en la semana, con intensidad según el volumen de series normales de los últimos 7 días | Media |
| GY-19 | Recordatorio si pasan varios días sin entrenar, con la cantidad de días configurable | Baja |
| GY-20 | Exportar el historial de sesiones a CSV | Baja |

### 7.2 Reglas de negocio

- El volumen de una serie es peso por repeticiones; el volumen del ejercicio es la suma de sus series normales.
- El 1RM estimado se calcula con la fórmula de Epley:

```latex
1RM = peso \times \left(1 + \frac{repeticiones}{30}\right)
```

- Las series de calentamiento no cuentan para récords ni para el volumen.
- Un récord se marca al terminar la serie y se confirma al finalizar la sesión.
- Si se abandona una sesión, las series ya marcadas se conservan como sesión incompleta.

### 7.3 Flujo principal: hacer una sesión

1. El usuario abre la rutina del día desde la línea de tiempo y toca "Empezar".
2. La app muestra el primer ejercicio con las series objetivo y los valores de la sesión anterior.
3. Ajusta peso o repeticiones y marca la serie como hecha.
4. Arranca el descanso automático; al terminar, avisa y pasa a la siguiente serie.
5. Al finalizar, se muestra el resumen con volumen, duración y récords nuevos, y el bloque queda completado en la línea de tiempo.

### 7.4 Pantallas

- Rutinas y ejercicios
- Editor de rutina
- Sesión en curso
- Resumen de sesión
- Historial
- Progreso, récords por ejercicio y mapa del cuerpo semanal

## 8. Módulo Dieta

Permite registrar lo que se come en el día y comparar los macronutrientes consumidos (proteínas, carbohidratos y grasas) y las calorías con objetivos diarios. Es una herramienta de registro, no de asesoramiento nutricional.

### 8.1 Requisitos funcionales

| ID | Requisito | Prioridad |
| --- | --- | --- |
| AL-01 | Registrar comidas por momento del día (desayuno, almuerzo, merienda, cena, colación) con fecha y hora | Alta |
| AL-02 | Agregar alimentos a una comida indicando la cantidad en gramos o porciones | Alta |
| AL-03 | Base de alimentos propia: nombre, porción, calorías, proteínas, carbohidratos y grasas por 100 g o por porción | Alta |
| AL-04 | Crear, editar y marcar alimentos como favoritos o recientes para cargarlos rápido | Alta |
| AL-05 | Definir objetivos diarios de calorías y macros, en gramos o en porcentaje | Alta |
| AL-06 | Mostrar el progreso del día con anillos o barras de cada macro contra su objetivo | Alta |
| AL-07 | Buscar alimentos en una base abierta (Open Food Facts) y guardarlos en la base propia | Media |
| AL-08 | Escanear código de barras con la cámara para traer los datos de un producto | Media |
| AL-09 | Crear comidas guardadas (combinaciones de alimentos) y reutilizarlas | Media |
| AL-10 | Copiar una comida o un día completo a otra fecha | Media |
| AL-11 | Objetivos distintos por día de la semana (por ejemplo, día de entrenamiento y día de descanso) | Baja |
| AL-12 | Historial y gráficos semanales o mensuales de calorías y macros | Media |
| AL-13 | Registro de agua | Baja |
| AL-14 | Comidas planificadas como bloques en la línea de tiempo, con recordatorio | Media |

### 8.2 Reglas de negocio

- Las calorías de un alimento pueden ingresarse o calcularse a partir de los macros: 4 kcal por gramo de proteína o carbohidrato y 9 kcal por gramo de grasa.
- Los valores de un alimento se escalan según la cantidad registrada.
- Cambiar los datos de un alimento no modifica los registros pasados; cada registro guarda sus valores al momento de cargarse.
- Los datos que vienen de bases abiertas pueden ser inexactos y se muestran como tales; el usuario puede corregirlos.
- La app no da recomendaciones médicas; las calculadoras de objetivos, si se agregan, son orientativas y lo indican.

### 8.3 Pantallas

- Diario del día (comidas y progreso de macros)
- Buscar y agregar alimento
- Escáner de código de barras
- Mis alimentos y comidas guardadas
- Objetivos
- Historial y gráficos

## 9. Módulo Meditación

Ofrece un temporizador libre y sesiones guiadas organizadas en packs, con una interfaz calma y minimalista, inspirada en Medito. El MVP sale solo con el temporizador y las campanas; las sesiones guiadas con audio se agregan después, cuando haya contenido propio o con licencia.

### 9.1 Requisitos funcionales

| ID | Requisito | Prioridad |
| --- | --- | --- |
| ME-01 | Temporizador libre con duración configurable (de 1 a 120 minutos) | Alta |
| ME-02 | Campana de inicio y de fin, con varios sonidos a elegir y volumen propio | Alta |
| ME-03 | Campanas de intervalo opcionales (cada N minutos) | Media |
| ME-04 | Preparación previa de unos segundos antes de empezar | Media |
| ME-05 | Biblioteca de sesiones guiadas organizada en packs o cursos (por ejemplo, introducción, ansiedad, sueño) (después del MVP) | Alta |
| ME-06 | Cada sesión muestra título, descripción, duración y autor; se puede reproducir con la pantalla apagada (después del MVP) | Alta |
| ME-07 | Reproducción en segundo plano con controles en la notificación y en la pantalla de bloqueo (después del MVP) | Alta |
| ME-08 | Descarga de sesiones para usarlas sin conexión (después del MVP) | Media |
| ME-09 | Historial de meditaciones con duración total y número de sesiones | Alta |
| ME-10 | Racha de días seguidos y calendario con los días meditados | Media |
| ME-11 | Meditación del día destacada en la pantalla de inicio del módulo (después del MVP) | Baja |
| ME-12 | Sonidos ambientales (lluvia, bosque, ruido blanco) que se pueden mezclar con el temporizador | Baja |
| ME-13 | Programar una meditación como bloque en la línea de tiempo con recordatorio | Alta |
| ME-14 | Modo "No molestar" opcional durante la sesión | Baja |

### 9.2 Reglas de negocio

- Una sesión cuenta para la racha si se completa al menos el 80 % de su duración (porcentaje configurable).
- Las sesiones interrumpidas se guardan como incompletas y no suman a la racha.
- Todo el contenido de audio debe ser propio o contar con licencia que permita su uso en la app. No se reutilizan audios de otras apps sin verificar su licencia.
- El audio de una sesión se pausa si entra una llamada o si se desconectan los auriculares.

### 9.3 Pantallas

- Inicio de meditación (meditación del día, racha, packs)
- Detalle de pack y de sesión
- Temporizador libre y su configuración
- Reproductor de sesión
- Historial y calendario de rachas

## 10. Módulo Música y auriculares

Muestra la canción que suena en cualquier app de música del teléfono y ofrece controles básicos; cuando el dispositivo lo permite, muestra el estado y la batería de los auriculares conectados. La app no reproduce música propia: controla lo que ya suena.

### 10.1 Requisitos funcionales

| ID | Requisito | Prioridad |
| --- | --- | --- |
| MU-01 | Mostrar título, artista, álbum y carátula de la canción en reproducción | Alta |
| MU-02 | Controles de reproducir, pausar, anterior y siguiente | Alta |
| MU-03 | Barra de progreso de la canción, con avance y retroceso si la app de origen lo permite | Media |
| MU-04 | Indicar qué app de música está reproduciendo y permitir abrirla | Media |
| MU-05 | Mini reproductor visible en la pantalla de inicio de la app y como widget | Media |
| MU-06 | Control de volumen | Baja |
| MU-07 | Mostrar auriculares Bluetooth conectados con nombre y estado de conexión | Media |
| MU-08 | Mostrar el nivel de batería de los auriculares (y estuche, o cada auricular por separado) cuando el dispositivo lo informe | Media |
| MU-09 | Alerta opcional cuando la batería de los auriculares baja del 20 % | Baja |
| MU-10 | Mostrar "no disponible" de forma clara cuando una función no es compatible, sin errores | Alta |
| MU-11 | Sugerir música de fondo al iniciar un entrenamiento o una meditación (abre la app de música elegida) | Baja |

### 10.2 Reglas de negocio y limitaciones

- Para leer la reproducción de otras apps, el usuario debe habilitar el acceso a notificaciones desde los ajustes de Android; la app lo explica en una pantalla guiada.
- La batería de los auriculares depende de que el modelo la informe al teléfono: con algunos se ve bien, con otros solo parcialmente y con otros no se ve.
- Si no hay música ni auriculares, el módulo muestra un estado vacío con la explicación y no ocupa espacio en la pantalla principal.
- La app no almacena ni sube información de lo que el usuario escucha.

### 10.3 Pantallas

- Reproductor (canción actual, controles, progreso)
- Auriculares (estado y batería)
- Pantalla guiada para habilitar los permisos
- Mini reproductor y widget

## 11. Módulo Tiempo de pantalla y enfoque

Muestra cuánto tiempo se usa cada app del teléfono y ayuda a poner límites y a evitar distracciones. Se inspira en la experiencia de StayFree, una app que mide el uso del teléfono, permite fijar límites y ofrece modos de enfoque; sus funciones exactas están por validar con el equipo.

### 11.1 Requisitos funcionales

| ID | Requisito | Prioridad |
| --- | --- | --- |
| TP-01 | Mostrar el tiempo de pantalla total del día y por app, con gráfico | Alta |
| TP-02 | Historial por día, semana y mes, con promedio diario y comparación con el período anterior | Media |
| TP-03 | Detalle de una app: tiempo por día, cantidad de sesiones y duración media de cada una | Media |
| TP-04 | Límites de uso diarios por app o por categoría, con aviso al acercarse y al alcanzarlo | Media |
| TP-05 | Recordatorio de uso excesivo cuando se supera el tiempo definido en una app | Media |
| TP-06 | Modo enfoque: horarios en los que se silencian las apps elegidas como distractoras | Media |
| TP-07 | Iniciar el modo enfoque durante una tarea o con el temporizador de enfoque de la línea de tiempo | Media |
| TP-08 | Resumen de tiempo de pantalla en la pantalla "Hoy" y en el resumen semanal | Baja |
| TP-09 | Bloquear una app al superar el límite, con opción de posponer | Baja (versión posterior) |

### 11.2 Reglas de negocio

- Los datos de uso se guardan en el teléfono y no se sincronizan, salvo que el usuario lo active.
- El módulo no está disponible en la versión de computadora en la v1, porque depende de datos propios de Android.
- Ningún límite ni modo enfoque se aplica sin consentimiento explícito, y el usuario puede desactivarlo cuando quiera.
- Los permisos se piden al abrir el módulo y se explican; sin ellos el resto de la app sigue funcionando.
- Fase sugerida: la 4, junto con música y auriculares, porque ambos módulos dependen de permisos especiales del sistema.

### 11.3 Pantallas

- Resumen del día (tiempo total y por app)
- Historial y estadísticas
- Detalle de una app
- Límites de uso
- Modo enfoque y horarios
- Pantalla guiada para habilitar el permiso de uso

## 12. Funciones transversales

Son las funciones que comparten todos los módulos y que dan coherencia a la app.

| ID | Función | Descripción |
| --- | --- | --- |
| TR-01 | Navegación | Barra inferior en el teléfono (Hoy, Finanzas, Gimnasio, Dieta, Meditación) y menú lateral en computadora; Música accesible desde un mini reproductor |
| TR-02 | Pantalla "Hoy" | Línea de tiempo con tarjetas resumen de cada módulo (gasto del día, macros, próxima rutina, racha) |
| TR-03 | Sistema de bloques | Cada módulo registra sus tipos de evento en la línea de tiempo, con ícono, color y acción propia al tocarlo |
| TR-04 | Notificaciones | Canales separados por módulo (tareas, alarmas, entrenamiento, comidas, meditación, presupuesto) que se activan o silencian por separado |
| TR-05 | Widgets | Próximas tareas, alta rápida de gasto, macros del día y mini reproductor |
| TR-06 | Búsqueda global | Busca tareas, transacciones, ejercicios y alimentos desde un único campo |
| TR-07 | Etiquetas | Etiquetas comunes que se pueden usar en tareas, transacciones y sesiones |
| TR-08 | Ajustes | Tema (claro, oscuro, automático), idioma, moneda, unidades, inicio de semana, formato de hora y gestión de notificaciones |
| TR-09 | Onboarding | Pantallas de bienvenida que explican los módulos y piden los permisos solo cuando hacen falta |
| TR-10 | Backup y exportación | Exportar e importar todos los datos en un archivo (JSON) y exportar tablas a CSV |
| TR-11 | Cuenta y sincronización | Opcional; permite usar los mismos datos en el teléfono, el navegador y el ejecutable (ver sección 13) |
| TR-12 | Bloqueo de la app | Opcional, con huella o PIN, útil por los datos financieros |
| TR-13 | Resumen semanal | Pantalla con un repaso de la semana: tareas cumplidas, gasto, entrenamientos, macros y meditaciones |
| TR-14 | Estados vacíos y errores | Mensajes claros y acciones sugeridas cuando no hay datos o falla algo |

## 13. Versión para computadora (web y ejecutable)

La app se puede usar desde la computadora de dos maneras que comparten el mismo código y los mismos datos: en el navegador (web instalable) y como programa de escritorio. Ambas se sincronizan con el teléfono mediante una cuenta del usuario.

### 13.1 Formas de acceso

| Forma | Descripción | Cuándo conviene |
| --- | --- | --- |
| Web (navegador) | Se abre desde una dirección, sin instalar nada, en Chrome, Edge, Firefox o Safari | Uso ocasional o desde una computadora ajena |
| Web instalable (PWA) | La misma web, instalable desde el navegador con ícono propio, ventana independiente y uso básico sin conexión | Uso diario sin pasos extra |
| Ejecutable de escritorio | La misma web empaquetada como programa para Windows, con acceso a notificaciones y bandeja del sistema | Quien prefiere un programa instalado y atajos de teclado globales |

La web se desarrolla primero; el ejecutable se obtiene empaquetándola, por lo que no requiere reescribir la app. Se decidió que el ejecutable puede esperar y que, cuando se haga, será solo para Windows. La app de Android sigue siendo nativa y se desarrolla por separado.

### 13.2 Sincronización y cuenta

- Es necesario crear una cuenta (correo y contraseña, o acceso con Google) para usar la computadora. En el teléfono es opcional.
- Los datos se guardan primero en el dispositivo y se envían a un servidor cuando hay conexión.
- Cada registro tiene fecha de última modificación; ante un conflicto gana la edición más reciente.
- Los borrados se sincronizan como "marcas de borrado" para que no reaparezcan datos eliminados.
- Los datos viajan cifrados y el usuario puede borrar su cuenta y todos sus datos desde Ajustes.
- La sincronización de todos los módulos se activa por módulo; el usuario puede dejar fuera, por ejemplo, las finanzas.

### 13.3 Paridad de funciones

| Módulo | Web y escritorio | Notas |
| --- | --- | --- |
| Planificación | Completa | Arrastrar bloques con el mouse, atajos de teclado, vista de día y semana más amplia |
| Finanzas | Completa | Mejor para cargar muchos gastos y ver estadísticas; exportación a CSV |
| Gimnasio | Armado de rutinas, historial y progreso completos; sesión en curso básica | La sesión se pensó para el teléfono, que es el que se lleva al gimnasio |
| Dieta | Completa, salvo escáner de código de barras | El código se puede escribir a mano |
| Meditación | Temporizador con campanas y rachas; sesiones guiadas después | La reproducción sigue mientras la pestaña o ventana esté abierta |
| Música y auriculares | No disponible en la v1 | Depende de funciones propias del teléfono Android |
| Alarmas | Avisos mientras la app esté abierta o instalada | El teléfono es el dispositivo principal para alarmas confiables |

### 13.4 Requisitos propios de la versión de computadora

| ID | Requisito | Prioridad |
| --- | --- | --- |
| PC-01 | Diseño adaptable: columnas y paneles laterales en pantallas anchas, sin deformar la versión móvil | Alta |
| PC-02 | Atajos de teclado (nueva tarea, nueva transacción, buscar, cambiar de día) | Media |
| PC-03 | Arrastrar y soltar con el mouse en la línea de tiempo y la bandeja | Alta |
| PC-04 | Instalación como PWA con ícono y ventana propia | Media |
| PC-05 | Ejecutable con instalador para Windows, con actualización automática (versión posterior) | Media |
| PC-06 | Notificaciones de escritorio para recordatorios de tareas | Media |
| PC-07 | Cambios hechos en el teléfono visibles en la computadora en menos de 10 segundos con conexión | Alta |
| PC-08 | Uso sin conexión con los datos ya descargados y sincronización al reconectar | Media |
| PC-09 | Importar y exportar datos desde la versión de computadora | Baja |

## 14. Modelo de datos conceptual

Es una vista funcional de las entidades principales; el diseño técnico de tablas se define en la fase de desarrollo. Todas las entidades comparten los campos de control: identificador único, fecha de creación, fecha de modificación y marca de borrado, necesarios para la sincronización.

| Entidad | Módulo | Campos principales | Relaciones |
| --- | --- | --- | --- |
| Tarea | Planificación | título, fecha, hora de inicio, duración, ícono, color, notas, estado, repetición | Tiene subtareas, recordatorios y etiquetas; puede venir de una rutina |
| Subtarea | Planificación | título, completada | Pertenece a una tarea |
| Rutina | Planificación | nombre, tareas con horarios relativos | Genera tareas |
| Recordatorio | Planificación | momento, tipo (aviso o alarma), sonido | Pertenece a una tarea u otro bloque |
| Etiqueta | Transversal | nombre, color | Se usa en tareas y transacciones |
| Cuenta | Finanzas | nombre, tipo, moneda, saldo inicial | Tiene transacciones |
| Categoría | Finanzas | nombre, ícono, color, tipo (ingreso o gasto) | Agrupa transacciones y presupuestos |
| Transacción | Finanzas | monto, fecha, tipo, nota | Pertenece a una cuenta y una categoría |
| Presupuesto | Finanzas | período, monto, categoría | Se compara con las transacciones de la categoría |
| Ejercicio | Gimnasio | nombre, grupo muscular, equipamiento, tipo de registro | Está en rutinas y series |
| Rutina de gimnasio | Gimnasio | nombre, días programados | Tiene ejercicios con series objetivo |
| Sesión | Gimnasio | fecha, duración, estado | Pertenece a una rutina (opcional); tiene series realizadas |
| Serie | Gimnasio | peso, repeticiones, tipo, completada | Pertenece a una sesión y a un ejercicio |
| Récord personal | Gimnasio | tipo, valor, fecha | Se deriva de las series de un ejercicio |
| Alimento | Dieta | nombre, porción, calorías, proteínas, carbohidratos, grasas, origen | Se usa en registros de comida |
| Comida | Dieta | momento del día, fecha y hora | Contiene registros de alimentos con su cantidad y valores guardados |
| Objetivo nutricional | Dieta | calorías, proteínas, carbohidratos, grasas, día de la semana | Se compara con el total del día |
| Pack de meditación | Meditación | título, descripción | Contiene sesiones guiadas |
| Sesión guiada | Meditación | título, duración, audio, autor | Pertenece a un pack |
| Meditación realizada | Meditación | fecha, duración, completada, tipo (libre o guiada) | Alimenta la racha |
| Bloque de línea de tiempo | Núcleo | tipo, módulo de origen, referencia, hora, duración | Apunta a una tarea, sesión, comida o meditación programada |
| Ajustes | Núcleo | tema, idioma, moneda, unidades, notificaciones | Uno por usuario |
| Registro de uso de app | Tiempo de pantalla | app, fecha, tiempo de uso, cantidad de sesiones | Se resume por día y por app |
| Límite de uso | Tiempo de pantalla | app o categoría, tiempo máximo diario, aviso previo, activo | Se compara con el registro de uso del día |
| Horario de enfoque | Tiempo de pantalla | nombre, días, hora de inicio y fin, apps incluidas | Puede aparecer como bloque en la línea de tiempo |

## 15. Requisitos no funcionales

Los valores son objetivos de partida y se ajustan al medir la app en dispositivos reales.

| ID | Categoría | Requisito |
| --- | --- | --- |
| NF-01 | Rendimiento | La pantalla "Hoy" abre en menos de 2 segundos en un teléfono de gama media y se desplaza a 60 cuadros por segundo |
| NF-02 | Rendimiento | Registrar un gasto o una serie confirma en menos de 300 ms |
| NF-03 | Disponibilidad offline | Todos los módulos, salvo búsqueda en bases abiertas y descargas, funcionan sin conexión |
| NF-04 | Batería | La app no mantiene servicios en segundo plano innecesarios; el consumo en reposo debe ser imperceptible |
| NF-05 | Fiabilidad de alarmas | Las alarmas suenan a la hora programada, incluso con la app cerrada, y se reprograman tras reiniciar |
| NF-06 | Privacidad | Los datos viven en el dispositivo por defecto; solo se suben si el usuario crea una cuenta y activa la sincronización |
| NF-07 | Privacidad | Ninguna información de música, salud o finanzas se usa con fines publicitarios ni se vende a terceros |
| NF-08 | Seguridad | Comunicación cifrada con el servidor, contraseñas con almacenamiento seguro y bloqueo opcional de la app |
| NF-09 | Seguridad | Los datos locales se excluyen de los backups automáticos de otras apps, o se cifran, según elija el usuario |
| NF-10 | Accesibilidad | Compatible con lector de pantalla (TalkBack), tamaños de texto grandes, contraste suficiente y áreas táctiles de al menos 48 dp |
| NF-11 | Accesibilidad | El color nunca es el único indicador de un estado (se suma ícono o texto) |
| NF-12 | Idiomas | Español (Argentina) en la v1, con todos los textos externalizados para sumar otros idiomas |
| NF-13 | Compatibilidad | Android 8.0 o superior; teléfonos y tabletas, vertical y horizontal |
| NF-14 | Compatibilidad web | Últimas dos versiones de Chrome, Edge, Firefox y Safari |
| NF-15 | Mantenibilidad | Arquitectura modular, pruebas automáticas de la lógica de negocio y de las reglas de cálculo |
| NF-16 | Datos | Migraciones de base de datos sin pérdida de información entre versiones |
| NF-17 | Cumplimiento | Política de privacidad clara y cumplimiento de la ley argentina de protección de datos personales y, si más adelante se publica, de las políticas de Google Play; la app avisa que no reemplaza el consejo profesional |

## 16. Permisos y restricciones de Android

La app pide cada permiso en el momento en que hace falta y explica para qué, nunca todos juntos al abrirla. Las condiciones exactas de cada permiso cambian entre versiones de Android y de las políticas de Google Play, por lo que deben revisarse en la documentación oficial de Android.

| Función | Permiso o mecanismo | Cuándo se pide | Notas |
| --- | --- | --- | --- |
| Notificaciones | Permiso de notificaciones (Android 13 o superior) | Al crear el primer recordatorio | Sin él, no hay avisos |
| Alarmas exactas | Permiso de alarmas exactas | Al activar una alarma sonora | Android 12 en adelante lo restringe; al instalar el APK directamente no hace falta justificarlo ante Google Play |
| Alarmas tras reiniciar | Aviso de arranque del sistema | Automático | Reprograma las alarmas |
| Pantalla de alarma | Notificación a pantalla completa | Al activar alarmas sonoras | Puede requerir habilitación manual |
| Música en reproducción | Acceso a notificaciones (se activa a mano en Ajustes) | Al abrir el módulo de música | Es un permiso especial y delicado; se explica con una pantalla guiada |
| Auriculares Bluetooth | Permiso de dispositivos cercanos (Android 12 o superior) | Al abrir la sección de auriculares | La batería depende del modelo |
| Escáner de código de barras | Cámara | Al tocar "Escanear" | Opcional |
| Audio en segundo plano | Servicio en primer plano de reproducción multimedia | Al reproducir una sesión | Muestra notificación con controles |
| Descargas de sesiones | Conexión a internet | Al descargar | Se recomienda ofrecer solo con Wi-Fi |
| Bloqueo de la app | Biometría | Al activar el bloqueo | Opcional |
| Ahorro de batería | Excepción de optimización de batería | Si las alarmas fallan por ahorro de energía | Solo se sugiere, no se exige |
| Tiempo de pantalla | Acceso a datos de uso (permiso especial que se activa en Ajustes) | Al abrir el módulo por primera vez | Sin él no se puede medir el uso. Bloquear apps exigiría además un permiso de superposición o de accesibilidad, que son más delicados de configurar; por eso el bloqueo queda para una versión posterior |

**Restricciones que afectan el diseño.**

- Algunos fabricantes (por ejemplo, Xiaomi, Samsung, Huawei) limitan agresivamente las apps en segundo plano; se incluye una guía en la app para que el usuario excluya la app del ahorro de batería si las alarmas fallan.
- Como la app no se publica en Google Play, el acceso a notificaciones para la música no pasa por una revisión de la tienda; si más adelante se publica, habrá que declarar y justificar ese uso.
- Si el usuario rechaza un permiso, la función asociada queda deshabilitada con una explicación y la app sigue funcionando en el resto.

## 17. Hoja de ruta y criterios de aceptación

El desarrollo avanza en cinco fases y los módulos no se construyen en paralelo: con la planificación y un módulo más funcionando ya hay algo usable. Las duraciones de cada fase se estiman con el equipo una vez definidas las preguntas abiertas.

&#91;embedded content: hoja de ruta · 5 fases con puerta de salida\]

Cada fase tiene una puerta de salida; la tabla detalla qué incluye y cómo se verifica.

| Fase | Contenido | Requisitos cubiertos | Criterio de aceptación |
| --- | --- | --- | --- |
| 1. Base y planificación | Proyecto, navegación, tema, base de datos local y núcleo; línea de tiempo, tareas, subtareas, recurrencia, rutinas, bandeja, alarmas y widget básico | PL-01 a PL-17, TR-01 a TR-04, TR-08, TR-09 | Se arma un día completo con tareas y rutinas, se mueven los bloques y las alarmas suenan a la hora, también después de reiniciar el teléfono |
| 2. Finanzas y gimnasio | Transacciones, cuentas, presupuestos y estadísticas; ejercicios, rutinas, sesión en curso y récords | FI-01 a FI-10, GY-01 a GY-11, GY-16 a GY-18 | Un gasto se registra en menos de 5 segundos, el presupuesto avisa al 80 % y al 100 %, y una sesión completa detecta un récord y queda marcada en la línea de tiempo |
| 3. Dieta y meditación | Registro de comidas, macros y objetivos; temporizador con campanas y rachas (las sesiones guiadas llegan después) | AL-01 a AL-10, AL-12, ME-01 a ME-04, ME-09, ME-10, ME-13 | Los macros del día se comparan con el objetivo y una meditación completa suma a la racha |
| 4. Computadora y música | Cuenta y sincronización con Supabase, web instalable y luego, más adelante, el programa de escritorio para Windows; música en reproducción y auriculares; tiempo de pantalla y enfoque | TR-11, PC-01 a PC-09, MU-01 a MU-10, TP-01 a TP-08 | Un cambio hecho en el teléfono aparece en la computadora en menos de 10 segundos; el tiempo de pantalla se muestra con el permiso concedido; la música y la batería se muestran o indican "no disponible" sin errores |
| 5. Pulido y versión final | Widgets, búsqueda global, bloqueo, backup y exportación, resumen semanal y los requisitos de prioridad media y baja que quedaron pendientes | TR-05 a TR-07, TR-10, TR-12 a TR-14, NF-01 a NF-17 | Prueba en el teléfono propio durante el uso diario, revisión con TalkBack y respaldo de datos verificado |

## 18. Riesgos, aspectos legales y preguntas abiertas

Los mayores riesgos son el alcance (siete módulos más una versión de computadora) y las funciones que dependen del teléfono o de contenido ajeno.

### 18.1 Riesgos

| ID | Riesgo | Impacto | Mitigación |
| --- | --- | --- | --- |
| R-01 | Alcance demasiado grande para un solo equipo | Alto | Entregar por fases; cada módulo es usable por sí solo |
| R-02 | Alarmas poco fiables por ahorro de batería de los fabricantes | Alto | Pruebas en varias marcas, guía al usuario y avisos de diagnóstico |
| R-03 | Si más adelante se publica, Google Play rechaza el permiso de alarmas exactas o el acceso a notificaciones | Medio | Preparar la justificación desde el inicio y tener una versión degradada de cada función |
| R-04 | La batería de auriculares no se ve en muchos modelos | Bajo | Presentarla como función opcional que se degrada con elegancia |
| R-05 | Dos bases de código (Android nativo y web) que se desalinean | Medio | Especificación de datos y reglas compartida; pruebas de las mismas reglas en ambos lados |
| R-06 | Conflictos o pérdida de datos en la sincronización | Alto | Marcas de borrado, pruebas de conflictos y exportación manual como respaldo |
| R-07 | Falta de contenido de meditación | Medio | Salir con el temporizador y sumar audios propios o licenciados de a poco |
| R-08 | Datos nutricionales inexactos | Medio | Mostrarlos como orientativos y permitir correcciones |
| R-09 | Parecido excesivo con las apps de referencia | Medio | Diseño visual, textos y contenidos propios; revisión legal antes de publicar |
| R-10 | El nombre "Stay Focused" ya está en uso por otra app, o se parece demasiado a StayFree (poco relevante mientras sea de uso personal) | Medio | Buscar el nombre en Google Play y en registros de marcas antes de publicar o compartir la app |
| R-11 | Una sola persona, en su primer proyecto con Kotlin y Android | Alto | Un solo módulo de Gradle, una fase por vez, empezar por la planificación y aprender con la documentación oficial y asistencia de IA mientras se construye |

### 18.2 Aspectos legales y de licencias

- **Apps de referencia.** Structured, Medito y Symmetry son inspiración de experiencia de uso. No se copian sus íconos, textos, imágenes, nombres ni contenidos.
- **Medito.** Es de código abierto, pero el código y los audios pueden tener licencias distintas. Si se quiere reutilizar algo, hay que revisar la licencia del repositorio y los términos del contenido antes.
- **Open Food Facts.** Es una base abierta con condiciones de uso y atribución que hay que respetar y citar en la app.
- **Ejercicios.** Las imágenes o videos de ejercicios deben ser propios o con licencia.
- **Datos personales.** Finanzas, salud y alimentación son datos sensibles: se requiere política de privacidad, consentimiento explícito para sincronizar y opción de borrado de cuenta.
- **Salud y nutrición.** La app es una herramienta de registro; no brinda consejo médico, nutricional ni financiero, y lo indica.

### 18.3 Decisiones y preguntas abiertas

**Decisiones confirmadas**

| Tema | Decisión |
| --- | --- |
| Uso y distribución | Solo uso personal; no se publica en Google Play y se instala el APK directamente en el teléfono |
| Costo | Gratuita |
| Desarrollo | Una persona, sin experiencia previa en Kotlin |
| Android mínimo | 8.0 (API 26) |
| Computadora | Primero la web instalable; el ejecutable puede esperar y será solo para Windows |
| Nombre | Stay Focused |
| Ícono | Pendiente de diseñar y aprobar |
| Meditación (MVP) | Solo temporizador y campanas; las sesiones guiadas con audio se agregan después |
| Sincronización | Después: primero solo el teléfono y, con la app andando, se suma la web usando Supabase |
| Gimnasio | Se suman superseries y circuitos, notas, mapa del cuerpo semanal, recordatorio de inactividad y exportación a CSV |

**Preguntas abiertas**

- [ ] ¿Cómo será el ícono de la app?
