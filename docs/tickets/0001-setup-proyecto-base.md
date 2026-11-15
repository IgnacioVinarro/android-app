# Ticket 0001 – Setup del proyecto Android base

## Estado
Terminado

## Descripción
Configurar la estructura inicial del proyecto Android con Gradle (Kotlin DSL), dependencias base (Compose, Hilt, Room, DataStore, Navigation), tema Material 3, navegación básica y MainActivity.

## Checklist de cambios
- [x] Crear `build.gradle.kts` (root) y `app/build.gradle.kts` con Kotlin DSL
- [x] Configurar `libs.versions.toml` con versiones estables de todas las dependencias
- [x] Crear estructura de paquetes: `core/{database,timeline,notifications,settings,ui,util}`, `planner/{data,domain,ui}`, `finance/{data,domain,ui}`, `gym/{data,domain,ui}`, `nutrition/{data,domain,ui}`, `meditation/{data,domain,ui}`, `media`, `screentime/{data,domain,ui}`
- [x] Configurar `AppDatabase` vacía con exportación de esquemas a `schemas/` — **parcial, ver nota**
- [x] Configurar Hilt (`@HiltAndroidApp`, módulo de base de datos) — módulo de base de datos pendiente de la primera entidad (ver nota)
- [x] Configurar DataStore para ajustes (tema, moneda, unidades, notificaciones)
- [x] Implementar tema Material 3 en `core/ui/theme/` (claro, oscuro, automático)
- [x] Configurar Navigation Compose con NavHost y barra inferior (5 destinos principales)
- [x] Crear `MainActivity` con scaffold raíz y barra inferior
- [x] Configurar `AndroidManifest.xml` con permisos base y `Application` de Hilt
- [x] Verificar compilación: `./gradlew assembleDebug`
- [x] Ejecutar tests (vacíos por ahora): `./gradlew test`

## Notas
- Android mínimo: API 26 (8.0)
- Target/compile: API estable más reciente
- Un solo módulo Gradle (`app`)
- Usar versiones de `libs.versions.toml`, no hardcodeadas
- Todos los textos en `strings.xml` (español Argentina)

## Notas de la implementación (revisión)
- **Room**: `@Database(entities = [])` no compila (KSP lo rechaza). Se decidió con el desarrollador
  dejar `AppDatabase` sin la anotación `@Database` por ahora (clase vacía) y sin `DatabaseModule`;
  se retoma al crear la primera entidad real (ticket de Planificación, PL-01).
- Se corrigieron varios errores del scaffolding inicial para que compilara: alias de plugins mal
  escritos (`plugins.*` → `libs.plugins.*`), kapt duplicado con ksp para Hilt (se dejó solo ksp),
  plugin `org.jetbrains.kotlin.plugin.compose` faltante (obligatorio con Kotlin 2.0 + Compose),
  `SettingsRepository` reescrito de RxJava3 a `Flow` (coroutines, que es el stack definido en
  CLAUDE.md) porque la integración RxJava3 de DataStore estaba mal usada y no compilaba,
  `Theme.kt` usaba una función de system-ui-controller inexistente en AndroidX,
  versiones de dependencias inexistentes en Maven (`ksp`, `hilt-navigation-compose`, `turbine`).
- Se generó el wrapper de Gradle (`gradlew`, `gradlew.bat`, `gradle/wrapper/`) con Gradle 8.10.2,
  que no existía en el repo.
- Se creó `local.properties` (fuera de Git) con `sdk.dir` y `gradle.properties` con
  `android.useAndroidX=true`, que faltaban.
- Se agregaron recursos que el manifiesto exigía y no existían: `themes.xml`, `backup_rules.xml`,
  `data_extraction_rules.xml`, ícono adaptativo placeholder (pendiente de diseño definitivo).
- `AndroidManifest.xml`: se agregó `<uses-feature android:name="android.hardware.camera"
  android:required="false">` (lint) y se suprimió un falso positivo de lint en
  `SCHEDULE_EXACT_ALARM` (`tools:ignore="ProtectedPermissions"`) — ambos cambios confirmados con
  el desarrollador antes de aplicarlos.
- `./gradlew assembleDebug`, `./gradlew test` y `./gradlew lint` corren en verde.
- Probado manualmente en emulador: la app ejecuta correctamente y se ven las 5 pantallas
  (Hoy, Finanzas, Gimnasio, Dieta, Meditación), accesibles solo desde la barra inferior.

## Limpieza posterior (auditoría de residuos)
- Se borró `app/schemas/.../1.json`, que había quedado de cuando `AppDatabase` todavía tenía
  `@Database(exportSchema = true)` antes de sacarle la anotación.
- Se creó `app/proguard-rules.pro` (vacío, documentado) porque `build.gradle.kts` lo referenciaba
  y no existía.
- Se sacaron dependencias sin un solo uso en el código: `androidx-fragment-ktx`,
  `navigation-fragment-ktx`, `navigation-ui-ktx` (navegación por Fragments, la app usa solo
  Navigation Compose), `androidx-constraintlayout`, `androidx-swiperefreshlayout` (widgets de
  View system, la app es 100% Compose), `kotlinx-serialization-json` (nada la usa) y
  `junit-jupiter` (declarada pero el build no tiene `useJUnitPlatform()`, así que un test JUnit 5
  nunca se hubiera ejecutado; se unificó en JUnit 4, que es lo que ya usa `ConvertersTest`).
- Se destraplicó Material 3 e íconos: estaban declarados dos veces (una vez con versión fija vía
  catálogo, otra vez como string literal manejado por el BOM de Compose). Se dejó solo la versión
  manejada por el BOM.