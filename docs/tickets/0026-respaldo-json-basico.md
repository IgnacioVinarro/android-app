# Ticket 0026 – Respaldo JSON básico (exportar e importar) en Ajustes

## Estado
En revisión

## Descripción
Decisión pendiente de CLAUDE.md resuelta por el desarrollador: **adelantar un respaldo JSON básico a la
Fase 1** (el documento técnico §11 lo ubicaba en la Fase 5; TR-10 del funcional). Sin sincronización ni nube,
reinstalar o perder el teléfono sin respaldo significa perder todo.

**Alcance (acordado con el desarrollador):**
- **Exportar** todas las tablas de Room (las 7 del planificador) a un archivo JSON con `formatVersion`,
  con el selector de archivos del sistema (`CreateDocument`; sin permisos de almacenamiento).
- **Importar** un archivo JSON: **reemplaza** todos los datos actuales (no fusiona). Valida el archivo y la
  versión del formato **antes** de tocar nada; si falla, no cambia ningún dato.
- Se exportan también las filas con `status = -1` (borrado lógico) para que el borrado viaje con el respaldo.
- Se conservan los `id` numéricos del archivo para que las referencias entre tablas sigan válidas.
- Tras importar se reprograman las alarmas (`ReminderScheduler.rescheduleAll()`).
- JSON con `org.json` (ya viene en Android, sin dependencia nueva en la app).
- Dos botones en la pantalla de Ajustes; importar pide confirmación.

**Fuera de alcance (a propósito):** ajustes de DataStore (tema, duración por defecto), recordatorio periódico de
exportar (decidido: más adelante), exportar a CSV, respaldo automático, cifrado del archivo.

**Excepciones a CLAUDE.md, autorizadas por el desarrollador:**
1. **`DELETE` físico solo al importar**: reemplazar todo exige vaciar las tablas. Va dentro de la misma
   transacción que la inserción (si algo falla, Room revierte). El borrado del día a día sigue siendo lógico.
2. **Librería nueva, solo de pruebas**: `org.json:json` como `testImplementation`, porque en pruebas unitarias
   el `org.json` de Android es un esqueleto que lanza "not mocked". No va dentro del APK.

**Decisiones de diseño:**
- Un único `BackupDao` (en `core/backup`) en lugar de tocar los 7 DAOs: lee todo sin filtrar por `status` y
  reemplaza todo en una `@Transaction`. No cambia el esquema (no sube la versión de la base ni hay migración).
- El código JSON es una función pura de `BackupData` ↔ texto, con pruebas; el acceso al archivo queda detrás de
  `BackupFileStore` para poder simularlo en las pruebas del ViewModel.

## Checklist de cambios
- [x] `org.json:json` 20251224 en `libs.versions.toml` (solo test; verificada en Maven Central)
- [x] `core/backup/BackupData.kt` + `BackupJson.kt` (codificar/decodificar/validar) + `BackupJsonTest`
- [x] `core/backup/BackupDao.kt` (leer todo, reemplazar todo en transacción) y registrarlo en `AppDatabase`
- [x] `core/backup/BackupRepository.kt` + prueba
- [x] `core/backup/BackupFileStore.kt` (lee y escribe por `Uri`)
- [x] `core/settings/ui`: `BackupViewModel` + prueba, y botones en `SettingsScreen` con confirmación
- [x] Textos en `strings.xml`
- [x] Prueba instrumentada de `BackupDao` (ida y vuelta en Room en memoria) — **escrita y compilada, NO ejecutada** (sin teléfono/emulador)
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Probar manualmente en el teléfono (abajo)

## Lista de comprobación manual
Con el teléfono real, **antes de probar, sacá un respaldo con datos reales**:
1. Ajustes (engranaje en Hoy) muestra "Respaldo de datos" con los botones Exportar e Importar.
2. Con algunas tareas (una recurrente con subtareas y recordatorios, una rutina, una borrada) tocar **Exportar**:
   el selector propone `stayfocused-respaldo-AAAA-MM-DD.json`; guardarlo en Descargas. Aparece "Respaldo exportado".
3. Abrir el archivo en un editor de texto: es JSON legible, con `formatVersion` 1 y las 7 tablas.
4. Borrar una tarea y crear otra distinta. Tocar **Importar** → aparece la confirmación de reemplazo;
   Cancelar no hace nada.
5. Confirmar y elegir el archivo: "Respaldo importado"; Hoy vuelve a mostrar exactamente lo exportado
   (la tarea nueva desaparece, la borrada reaparece), con subtareas, recurrencia y rutina.
6. Un recordatorio exportado con hora futura vuelve a sonar a su hora (teléfono en reposo).
7. Importar un archivo cualquiera (una foto o un .txt): "El archivo no es un respaldo válido…" y los datos
   siguen igual.
8. Instalación limpia: desinstalar, instalar la APK, importar el archivo: todo vuelve.
9. Girar la pantalla con el diálogo abierto: no se cierra solo. TalkBack lee ambos botones y los mensajes.
10. Modo avión: exportar e importar funcionan igual.
