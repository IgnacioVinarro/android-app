# Ticket 0027 – Gráficos propios con Canvas (decisión de librería de gráficos)

## Estado
En revisión

## Descripción
Decisión pendiente de CLAUDE.md resuelta por el desarrollador: **no usar librería de gráficos; dibujar con
`Canvas` de Compose** (Fase 2: Finanzas, Gimnasio; después Dieta y Tiempo de pantalla).

**Por qué no Vico:** las versiones actuales (2.5.2 y 3.3.1) están compiladas con Kotlin 2.4.x / AGP 9.x /
Compose 1.11–1.12; el proyecto usa Kotlin 2.0.21 / AGP 8.13.2 / BOM 2024.10.00. Subir el toolchain solo por
los gráficos es desproporcionado. Además se mantiene la regla de "pocas librerías".

**Alcance de este ticket (solo la base reutilizable, sin pantallas de módulos):**
- Dos tipos de gráfico: **línea** (progreso en el tiempo: peso, 1RM, peso corporal) y **columnas** (volumen
  semanal, gasto por mes, tiempo de pantalla por día, calorías).
- Componentes en `core/ui` (los usarán todos los módulos; los módulos no se llaman entre sí).
- Matemática en **funciones puras en `core/util`** (sin clases de Android) con pruebas JUnit:
  escala del eje Y (mínimo/máximo "lindos" y marcas) y conversión valor → posición.
- Colores del tema Material 3 (claro/oscuro). Etiquetas del eje X dadas por quien llama (ej. "L M X J V S D").
- Accesibilidad: `contentDescription` con un resumen textual del gráfico; el color no es el único indicador
  (valores/etiquetas visibles). Estado vacío ("Sin datos todavía") con mensaje claro.
- Sin dependencias nuevas ni cambios de manifiesto ni de base de datos.

**Fuera de alcance:** torta/dona (macros: barra apilada más adelante), animaciones elaboradas, zoom/arrastre,
conectar los gráficos a datos reales de cada módulo.

## Checklist de cambios
- [x] `core/util/ChartScale.kt`: escala "linda" del eje Y + mapeo valor → posición (función pura) + `ChartScaleTest`
- [x] `core/ui/chart/LineChart.kt` (Canvas, tema M3, etiquetas X, descripción accesible)
- [x] `core/ui/chart/ColumnChart.kt` (Canvas, tema M3, etiquetas X, descripción accesible)
- [x] Estado vacío compartido para ambos gráficos
- [x] Textos en `strings.xml`
- [x] Previsualizaciones (`@Preview`) de ambos en claro y oscuro
- [x] Compilar (`assembleDebug`), `./gradlew test` y `lint` en verde
- [x] Actualizar la decisión en documentos (CLAUDE.md "Decisiones pendientes" y documento técnico §librería de
      gráficos) — confirmado por el desarrollador

## Lista de comprobación manual
1. Ver las previsualizaciones en claro y oscuro: ejes, etiquetas y valores legibles.
2. TalkBack sobre un gráfico: lee el resumen ("Peso: de X a Y en N semanas").
3. Texto del sistema grande: las etiquetas no se superponen de forma ilegible.
4. Con una sola barra / un solo punto / todos los valores iguales / valores en cero: no falla y se ve razonable.
