# Ticket 0037 – Gimnasio: datos + reglas de dominio (GY-01…03, 05, 06, 09, 12, 16, 17 base)

## Estado
En progreso

## Descripción
Primer paso del módulo Gimnasio (Fase 2), igual que 0028 en Finanzas: tablas, repositorios y reglas
puras. **Sin pantallas, sin ViewModels, sin navegación** (el destino "Gimnasio" queda como está).

**Decisiones (confirmadas con el desarrollador):**
- Alcance: solo datos + dominio.
- Biblioteca de ejercicios: **propia y chica** (~35 ejercicios comunes), nombres en `strings.xml`,
  sembrados la primera vez como las categorías de Finanzas.

**Decisiones que tomé y conviene que confirmes:**
1. La tabla de sesión usa la columna `state` (`IN_PROGRESS`/`COMPLETED`/`ABANDONED`) y no `status`,
   porque `status` ya es el borrado lógico de todas las tablas (el documento técnico las nombra igual).
2. "Series normales" para volumen y récords = toda serie **completada que no sea calentamiento**
   (normal, fallo y descendente cuentan; CLAUDE.md solo excluye el calentamiento).
3. Récord "mayor volumen" = volumen total del ejercicio **en una sesión** (suma de series válidas).
4. 1RM estimado en gramos enteros (Epley, redondeo half-up); solo para ejercicios con peso y reps.
5. Se agregan `restSeconds` (GY-07) y `link` (GY-02) a `exercise`, y `durationSeconds` a `session_set`
   (ejercicios de tipo tiempo), que el documento técnico no lista.
6. Sin claves foráneas de Room (igual que Finanzas): solo índices; el borrado lógico no cascadea solo.

**Fuera de alcance:** pantallas, línea de tiempo (GY-04), temporizador de descanso (GY-07), mapa del
cuerpo (GY-18), gráficos (GY-11), respaldo JSON de Gimnasio (ticket aparte, como 0030).

## Checklist de cambios
- [ ] `gym/domain`: tipos (grupo muscular, equipo, tipo de registro, tipo de serie, estado, tipo de récord)
- [ ] `gym/domain`: volumen de serie/ejercicio/sesión, 1RM Epley, detección de récords, volumen por grupo (puras) + pruebas
- [ ] `gym/domain`: validación de serie por tipo de registro (pura) + pruebas
- [ ] `gym/data`: entidades `exercise`, `workout_routine`, `routine_exercise`, `workout_session`, `session_exercise_note`, `session_set`, `personal_record`
- [ ] `gym/data`: DAOs (todo con `status = 0`, borrado lógico)
- [ ] `gym/data`: `ExerciseRepository`, `RoutineRepository`, `SessionRepository` (+ módulo Hilt) + pruebas con MockK
- [ ] Biblioteca precargada (`ExerciseSeeder`) y textos; sembrado desde `StayFocusedApplication` + prueba
- [ ] `AppDatabase` versión 9, `MIGRATION_8_9` escrita **y probada** (instrumentada), esquema 9 exportado
- [ ] Compilar, `test` y `lint`
