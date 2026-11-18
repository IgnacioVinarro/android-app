# Ticket 0025 – "Tiempo de pantalla" pasa a la barra inferior; "Ajustes" vuelve a abrirse desde "Hoy"

## Estado
En revisión

## Descripción
Cambio pedido por el desarrollador, que **reemplaza el del ticket 0024** (Ajustes como quinto ícono):

> No sabía eso. Entonces dejá ajustes en la pantalla de "Hoy" y que el quinto icono sea "Tiempo de pantalla".

("Eso" = que los documentos fuente ya definían Ajustes como destino secundario, abierto desde "Hoy".)

**Qué cambia:**
- La **barra inferior** sigue con 5 destinos, pero el quinto (último) es **Tiempo de pantalla**:
  Hoy, Finanzas, Gimnasio, Meditación y **Tiempo de pantalla**.
- **Ajustes vuelve a ser un destino secundario**, como decían los documentos: se abre desde la pantalla "Hoy"
  y tiene botón para volver.
- El ticket 0024 queda **superado** por este (su cambio se revierte).

**Discrepancia con los documentos fuente (avisada, CLAUDE.md pide avisarlas):**
- Tiempo de pantalla figuraba como destino secundario ("se abre desde el menú de Hoy"); por pedido explícito
  pasa a la barra inferior. Se actualizaron CLAUDE.md, el funcional (TR-01) y el técnico (§4.2).
- Los documentos dicen que Ajustes se abre "desde el menú de Hoy", pero **todavía no existe ese menú**. En este
  paso se abre con un **ícono de engranaje** en el encabezado de Hoy (ver decisiones). Si preferís un menú
  desplegable (⋮) cuando haya más entradas (Música, etc.), es un ajuste chico.
- **Fase:** la función de Tiempo de pantalla (`UsageStatsManager`, permiso de uso) es de la **Fase 4**. Acá solo
  se agrega su lugar en la navegación con una pantalla provisoria; **no se adelanta nada** de la función ni se
  toca el manifiesto.

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- **Engranaje en Hoy:** va en la fila del resumen de la vista semanal (a la derecha, junto a "Hoy"), para no
  gastar una fila más de alto en una pantalla ya cargada. La tira semanal recibe una "acción final" opcional
  (un slot), así no sabe nada de Ajustes: lo pone la pantalla.
- **Ajustes como pantalla secundaria:** con barra superior y flecha para volver (mismo patrón que Rutinas). La
  barra inferior queda visible y sin ningún destino marcado, como en las demás pantallas secundarias.
- **Pantallas provisorias:** Ajustes y Tiempo de pantalla solo muestran su título hasta que se implementen
  (los ajustes concretos y la función de uso del teléfono son pasos aparte).
- Ícono de Tiempo de pantalla: el del teléfono de Material (`Smartphone`), del set extendido que el proyecto ya usa.
- La ruta de Ajustes pasa a una constante compartida (`ROUTE_SETTINGS`) en vez de ser un destino de la barra.

## Checklist de cambios
- [x] `StayFocusedDestination`: quinto destino `SCREEN_TIME` en lugar de `SETTINGS` + prueba actualizada
- [x] `screentime/ui` (`ScreenTimeScreen.kt`): pantalla provisoria
- [x] `core/settings/ui` (`SettingsScreen.kt`): barra superior con flecha para volver
- [x] `StayFocusedApp`: ruta secundaria de Ajustes, ruta de Tiempo de pantalla, y Hoy abre Ajustes
- [x] `WeekStrip` (acción final opcional) y `TodayScreen` (engranaje que abre Ajustes)
- [x] Textos: quitar `a11y_nav_settings`; agregar `a11y_nav_screentime` y `a11y_open_settings`
- [x] Documentos: CLAUDE.md, funcional (TR-01) y técnico (§4.2) actualizados; ticket 0024 marcado como superado
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Probar manualmente en el emulador/teléfono (abajo)

## Lista de comprobación manual
1. La barra inferior muestra, en orden: Hoy, Finanzas, Gimnasio, Meditación, Tiempo de pantalla (ya no hay "Ajustes").
2. "Tiempo de pantalla" es el último; su etiqueta no se corta ni se superpone con la de al lado en una pantalla
   angosta ni con el texto del sistema en tamaño grande (es la etiqueta más larga de la barra).
3. Tocar Tiempo de pantalla abre su pantalla provisoria (solo el título) y el ícono queda seleccionado.
4. En Hoy aparece un engranaje a la derecha del resumen del día; tocarlo abre Ajustes.
5. Ajustes muestra su título y una flecha para volver; la flecha lleva de vuelta a Hoy y se conserva el día elegido.
6. Con Ajustes abierto, ningún ícono de la barra inferior aparece seleccionado; tocar uno navega bien.
7. El engranaje tiene un área táctil de al menos 48 dp y no empuja ni tapa el botón "Hoy" de la vista semanal.
8. Con TalkBack: el engranaje se anuncia como "Abrir Ajustes"; el destino inferior como "Pestaña Tiempo de pantalla...".
