# Ticket 0020 – Huecos libres tocables para crear una tarea (PL-03)

## Estado
En revisión

## Descripción
PL-03: "Mostrar los huecos libres entre bloques como espacio vacío tocable para crear una tarea ahí".
El documento funcional no define los detalles; las decisiones siguientes las confirmó el desarrollador.

**Decisiones de alcance confirmadas con el desarrollador:**
- **Qué es un hueco:** tramo libre de **al menos 30 min** entre un bloque y el siguiente, y también
  antes del primero y después del último. Aspecto: borde punteado con "+" y la duración libre
  (ej. "Libre: 1 h").
- **Precarga al tocarlo:** fecha = día que se está viendo, hora de inicio = inicio del hueco, duración =
  la de por defecto (ajuste del usuario) o menos si el hueco es más corto. El resto, como una tarea nueva.
- **Fecha del botón +:** también usa el día elegido en la vista semanal (cierra el pendiente que quedó
  anotado en el ticket 0018).

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- Regla pura en `core/timeline` (`FreeGaps.kt`): `freeGaps` (junta bloques superpuestos antes de buscar
  huecos, así que cualquier combinación de bloques da huecos correctos) y `durationForGap`. Cuentan los
  bloques de **todos** los módulos, no solo Planificación: un hueco es tiempo libre del día.
- **Un día sin ningún bloque no muestra huecos:** el hueco sería el día entero (empezando a las 00:00),
  que no sirve para precargar nada; para eso está el botón +.
- Los huecos se calculan con la duración real de cada bloque. Un bloque muy corto se dibuja con alto
  mínimo de 48 dp, así que puede tapar visualmente el comienzo de un hueco; los bloques se dibujan encima
  de los huecos, por lo que el toque va al bloque.
- Un hueco de 30 min mide 30 dp (1 dp por minuto), menos que los 48 dp recomendados: mismo criterio
  pragmático que el ícono de editar (ticket 0016). El área mínima real es la del hueco para no pisar bloques.
- La ruta de crear tarea suma argumentos propios (`newDate`, `start`, `gap`); no se reusa `date` porque
  ya significa "fecha de la ocurrencia" al editar una recurrente.
- El texto de duración ("1 h 30 min") se comparte con la vista semanal: se mueve a `core/ui`
  (`formatDuration`) y sus textos pasan a llamarse `duration_*` (norma de reutilizar).
- El hueco lleva ícono y texto además del borde: el estado no depende solo del color.

## Checklist de cambios
- [x] `core/timeline` (`FreeGaps.kt`): `freeGaps`, `durationForGap` + pruebas
- [x] `core/ui`: `formatDuration` compartido; `WeekStrip` lo usa y los textos pasan a `duration_*`
- [x] `TodayViewModel`: `freeGaps` en el estado + pruebas
- [x] `CreateTaskViewModel`: precarga de fecha, hora y duración desde los argumentos + pruebas
- [x] Navegación: argumentos nuevos de la ruta de crear tarea; el + pasa el día elegido
- [x] `TodayScreen`: huecos dibujados y tocables, debajo de los bloques
- [x] Textos nuevos en `strings.xml`
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Correr `./gradlew connectedAndroidTest` — necesita emulador o teléfono conectado
- [ ] Probar manualmente en el emulador/teléfono (abajo)

## Lista de comprobación manual
1. Con dos tareas separadas por más de 30 min aparece un hueco punteado entre ambas, con "+" y su duración.
2. También aparece el hueco antes de la primera tarea y después de la última (si miden 30 min o más).
3. Un tramo libre de menos de 30 min no se dibuja.
4. Un día sin ninguna tarea no muestra huecos.
5. Tocar un hueco abre "Nueva tarea" con el día que se está viendo y la hora de inicio del hueco.
6. La duración precargada es la de por defecto, o la del hueco si es más corta (probar con un hueco de 30 min
   y la duración por defecto en 60).
7. Guardar la tarea: aparece en ese hueco y el hueco se achica o desaparece.
8. Tareas superpuestas: los huecos se calculan sobre el conjunto (no aparece un hueco "dentro" de otra tarea).
9. En otro día de la semana (vista semanal), el botón + crea la tarea en ese día, no en hoy.
10. Con TalkBack: el hueco lee "Hueco libre de X desde HH:MM, tocar para crear una tarea".
