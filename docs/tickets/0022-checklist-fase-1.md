# Ticket 0022 – Lista de comprobación manual de la Fase 1 (Planificación)

## Estado
En revisión

## Descripción
CLAUDE.md pide entregar la lista de comprobación manual cuando una fase se da por terminada. Este
ticket junta en un solo lugar lo que hay que probar **en el teléfono real** antes de pasar a la Fase 2.
Es un documento: no cambia código. Las listas detalladas de cada función siguen en su ticket (se
indica cuál); acá van ordenadas para recorrerlas de una sola vez.

**Estado de la fase (Planificación, PL-01 a PL-16):** PL-01, 02, 03, 04, 05, 06, 07, 08, 09, 10, 11, 12,
13, 14, 15 y 16 implementadas. **Fuera de la Fase 1:** PL-17 (depende de los módulos de las fases 2 y 3,
la infraestructura `TimelineSource` ya está) y PL-18 (prioridad baja, búsqueda global en la Fase 5).

**Qué NO está verificado todavía** (ningún entorno de desarrollo tuvo emulador ni teléfono conectado):
- `./gradlew connectedAndroidTest`: nunca se pudo correr. Cubre las pruebas de DAO y las migraciones 1→2
  a 6→7 (`MigrationTest`), que son obligatorias antes de dar por buenos los cambios de esquema.
- Las pruebas manuales de abajo.
- Varios tickets siguen en `En revisión` sin cerrar (0007, 0008, 0009, 0012 a 0021). Se pasan a `Terminado`
  cuando esta lista esté probada y la apruebes.

## Antes de empezar

### Preparación
- [ ] Teléfono real Android 8.0 o superior (ideal: uno con Android 13+ y otro con 12, por los permisos).
- [ ] Instalar la APK de debug: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/`.
- [ ] Conectar el teléfono por USB (depuración activada) y correr, con la app desinstalada la primera vez:
      `./gradlew connectedAndroidTest` → debe pasar completo (DAO + las 6 migraciones).
- [ ] Segunda vez, **actualizar encima** de una versión vieja con datos (si tenés una APK anterior) y
      comprobar que la app abre y conserva tareas: prueba real de migración (esquema actual: versión 7).
- [ ] Recordatorio de CLAUDE.md: antes de actualizar una APK instalada con datos reales, hacer respaldo.
      **El respaldo JSON todavía no existe** (decisión pendiente: adelantarlo a la Fase 1).

### Ajustes del teléfono que conviene tener a mano
- [ ] Permitir notificaciones, alarmas y recordatorios, y pantalla completa para la app (Ajustes → Apps).
- [ ] Optimización de batería: dejarla en "Optimizada" (no "Sin restricciones") para probar como usuario real.

## 1. Línea de tiempo y tareas (PL-01 a PL-07)
- [ ] **PL-01** Hoy muestra la escala de 00:00 a 23:00 y una línea roja de "ahora" que avanza sola (mirar
      1–2 min). Con otro día elegido la línea roja no aparece.
- [ ] **PL-02** Cada tarea es un bloque cuyo alto es proporcional a su duración, con ícono y color propios.
- [ ] **PL-03** Los huecos libres (30 min o más) se ven punteados con "+" y duración; tocar uno abre
      "Nueva tarea" con día y hora del hueco. Detalle: ticket 0020.
- [ ] **PL-04** Crear tarea con título, fecha, hora, duración, ícono, color, notas y subtareas; reabrirla
      y comprobar que se conserva todo.
- [ ] **PL-05** Tocar el bloque completa la tarea (con animación) y sube el progreso del día; las subtareas
      se tildan desde el chip "X/Y". Detalle: ticket 0016.
- [ ] **PL-06** Una tarea "sin hora" va a la bandeja; tocarla permite asignarle hora y pasa a la línea de tiempo.
- [ ] **PL-07** Mantener presionado un bloque y arrastrar cambia su hora; arrastrar el asa inferior cambia
      la duración; al salir y volver a entrar se conserva. (Pendiente desde el ticket 0007.)
- [ ] Superposiciones: dos tareas a la misma hora se dibujan lado a lado, sin taparse.

## 2. Recurrencia y rutinas (PL-08, PL-09, PL-15)
- [ ] **PL-08** Crear una tarea diaria, una semanal, una de días específicos, una mensual y una cada N días,
      cada una con y sin fecha de fin: aparecen los días correctos (mirarlo con la vista semanal).
      Completar una ocurrencia no completa las demás. (Pendiente desde el ticket 0009.)
- [ ] **PL-15** Editar y borrar una recurrente con "Solo esta", "Esta y siguientes" y "Todas": el resultado
      coincide con la opción elegida (una editada no cambia las otras; "esta y siguientes" corta la serie).
- [ ] **PL-09** Crear una rutina con varios ítems, aplicarla a hoy: crea tareas normales a las horas
      relativas. Editar la rutina después no cambia las ya creadas.

## 3. Recordatorios y alarmas (PL-10, PL-11) — zona sensible, con el teléfono en reposo
Seguir las listas completas de los tickets **0013** (12 pasos) y **0014** (10 pasos). Resumen de lo crítico:
- [ ] Permisos: sin notificaciones o alarma exacta, el formulario lo explica y el resto de la app sigue.
- [ ] Aviso a la hora, "X min antes" y varios recordatorios por tarea: suenan a la hora correcta con el
      teléfono **bloqueado y quieto** durante varios minutos (la prueba que importa).
- [ ] Posponer (10 min), Completar y Descartar desde la notificación y desde la pantalla de alarma.
- [ ] Alarma sonora: enciende la pantalla, suena el tono de alarma repetido y se corta sola a los 5 min.
- [ ] Reprogramación: reiniciar el teléfono y cambiar la hora/zona horaria; el aviso sigue sonando bien.
- [ ] Editar la hora o borrar la tarea: no suena el aviso viejo.
- [ ] Modo No molestar y notificación de pantalla completa revocada: anotar qué pasa (ver ticket 0014).

## 4. Vista semanal, atrasadas y widget (PL-12, PL-14, PL-16)
- [ ] **PL-12** La tira semanal muestra lunes a domingo con carga por día; tocar un día cambia la línea de
      tiempo y la bandeja; flechas y "Hoy". Detalle: ticket 0018.
- [ ] **PL-14** "N atrasadas" arriba de Hoy; Reprogramar y A la bandeja desde la hoja. Detalle: ticket 0017.
- [ ] **PL-16** Agregar el widget al inicio: próximas 4 tareas, progreso, "+" abre nueva tarea, se actualiza
      al cambiar datos y cada 30 min, se ve bien en tema oscuro. Detalle: ticket 0021 (13 pasos).

## 5. Temporizador de enfoque (PL-13) — segundo plano, con el teléfono en reposo
Seguir la lista completa del ticket **0019** (10 pasos). Lo crítico:
- [ ] Iniciar desde un bloque: barra en Hoy y notificación persistente con cuenta regresiva.
- [ ] Pausar/Reanudar/Detener desde la barra y desde la notificación.
- [ ] Con la pantalla apagada y el teléfono quieto, al llegar a cero suena el aviso de fin (sonido + vibración).
- [ ] Una pausa más larga que el tiempo restante **no** hace sonar nada; Detener no deja avisos pendientes.

## 6. Transversales

### Accesibilidad (CLAUDE.md)
- [ ] TalkBack activado: recorrer Hoy, el formulario, la tira semanal, los huecos, la hoja de subtareas,
      la de atrasadas, la barra de enfoque y el widget. Todo botón e ícono se anuncia con sentido.
- [ ] El color nunca es el único indicador: seleccionado, atrasado, pausado y hueco libre tienen texto o ícono.
- [ ] Texto del sistema en tamaño grande: nada queda cortado de forma que impida usar la pantalla.
- [ ] Áreas táctiles: los íconos chicos dentro de los bloques (editar, subtareas, temporizador) son difíciles
      de tocar en bloques angostos — **anotar si molesta**; es un compromiso conocido (tickets 0016 y 0020).

### Temas, orientación y estados
- [ ] Tema claro, oscuro y automático (Ajustes del sistema): la app y el widget se leen bien.
- [ ] Girar la pantalla en Hoy, en el formulario y con un diálogo abierto: no se pierde lo escrito ni se
      reabre nada por error.
- [ ] Día sin tareas: estado vacío claro, sin errores. Muchas tareas el mismo día: la lista se desplaza bien.
- [ ] Rendimiento: registrar una tarea simple lleva menos de 5 s y la confirmación se ve al instante.

### Datos y privacidad
- [ ] Sin conexión a internet (modo avión): toda la Fase 1 funciona igual (offline-first).
- [ ] Borrar una tarea y reiniciar la app: no vuelve a aparecer (borrado lógico), ni en la semana ni en el widget.
- [ ] Una tarea borrada con recordatorios no vuelve a avisar.

## Cómo registrar el resultado
- Anotar acá abajo cada falla con: qué hiciste, qué esperabas, qué pasó, versión de Android y modelo.
- Cuando todo esté tildado: pedirme que pase a `Terminado` los tickets 0007 a 0022 y que prepare el commit.
- Si algo falla: crear un ticket nuevo por falla (CLAUDE.md: un ticket por cambio), sin mezclar arreglos.

## Resultados de la prueba
_(se completa después de probar)_

| Fecha | Teléfono / Android | Fallas encontradas | Ticket de arreglo |
| --- | --- | --- | --- |
|  |  |  |  |

## Checklist de cambios
- [x] Reunir las listas manuales de los tickets 0004 a 0021 en una sola lista ordenada por requisito
- [x] Agregar los pendientes de verificación que ningún ticket cerró (`connectedAndroidTest`, migraciones,
      pruebas manuales de 0007, 0009, 0012 y 0015)
- [x] Agregar pruebas transversales (accesibilidad, temas, rotación, sin conexión, borrado lógico)
- [ ] Probar la lista en el teléfono real y completar "Resultados de la prueba" — **lo hacés vos**
- [ ] Aprobación del desarrollador para pasar los tickets en revisión a `Terminado`
