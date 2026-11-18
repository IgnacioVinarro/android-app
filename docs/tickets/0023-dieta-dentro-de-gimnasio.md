# Ticket 0023 – "Dieta" pasa a ser una pestaña dentro de "Gimnasio" (cambio de navegación)

## Estado
Terminado

## Descripción
Cambio pedido por el desarrollador sobre la navegación de la app:

> "Dieta" debe estar en "Gimnasio" y en su header debe haber un tab bar que permita alternar las
> vistas entre uno y otro. Primero "Gimnasio" (predeterminado al abrir), luego "Dieta".

**Qué cambia:**
- La **barra inferior** pasa de 5 a **4 destinos**: Hoy, Finanzas, Gimnasio, Meditación.
- La pantalla **Gimnasio** lleva arriba un **tab bar** con dos pestañas, en este orden: **Gimnasio**
  (la que se ve por defecto) y **Dieta**.
- "Dieta" deja de ser un destino de navegación propio (se elimina la ruta `nutrition`).

**Discrepancia con los documentos fuente (avisada, CLAUDE.md pide avisarlas):** el documento funcional
(TR-01) y el técnico (§4.2) decían que Dieta era un destino de la barra inferior. Por pedido explícito del
desarrollador mandan los cambios de este ticket y **se actualizaron esos dos documentos y CLAUDE.md**
(abajo, en la checklist).

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- **Respeta la regla de capas:** `gym` y `nutrition` no se llaman entre sí. La pantalla que junta las dos
  pestañas (`GymDietScreen`) vive en `core/ui/navigation`, que ya conoce todas las pantallas (es donde está
  el `NavHost`); cada módulo sigue ignorando al otro.
- **Tab bar reutilizable:** `SectionTabRow` en `core/ui` (norma de reutilizar): recibe los títulos y la
  pestaña elegida, no sabe nada de gimnasio ni dieta. Sirve para cualquier otra pantalla con pestañas.
- **Pestaña por defecto:** Gimnasio. La pestaña elegida se conserva al girar la pantalla y al volver a
  Gimnasio desde otro destino de la barra inferior (la barra guarda y restaura el estado de cada destino).
  Al abrir la app de cero, siempre arranca en Gimnasio.
- Los textos de las pestañas reusan "Gimnasio" y "Dieta"; se elimina el texto de accesibilidad de la
  pestaña inferior "Dieta", que ya no existe.
- Las pantallas de Gimnasio y Dieta siguen siendo provisorias (se implementan en las fases 2 y 3).

## Checklist de cambios
- [x] `core/ui` (`SectionTabs.kt`): `SectionTabRow` reutilizable
- [x] `core/ui/navigation` (`GymDietScreen.kt`): pantalla con las pestañas Gimnasio / Dieta + pruebas
- [x] `StayFocusedDestination`: quitar `NUTRITION` (barra inferior de 4 destinos) + prueba
- [x] `StayFocusedApp`: el destino Gimnasio muestra `GymDietScreen`; se elimina la ruta de Dieta
- [x] Textos: quitar `a11y_nav_nutrition` (las pestañas se anuncian solas con su texto y su estado de selección)
- [x] Documentos: CLAUDE.md, funcional (TR-01) y técnico (§4.2) actualizados
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Probar manualmente en el emulador/teléfono (abajo)

## Lista de comprobación manual
1. La barra inferior muestra 4 destinos: Hoy, Finanzas, Gimnasio, Meditación (ya no hay "Dieta").
2. Al tocar Gimnasio se ve arriba un tab bar con "Gimnasio" (seleccionada) y "Dieta", en ese orden.
3. Tocar "Dieta" cambia el contenido a la vista de Dieta; tocar "Gimnasio" vuelve.
4. Abrir la app de cero y entrar a Gimnasio: arranca en la pestaña "Gimnasio".
5. Elegir "Dieta", ir a Hoy y volver a Gimnasio: se conserva la pestaña elegida. Girar la pantalla: se conserva.
6. La pestaña elegida se distingue por el indicador y el texto (no solo por el color).
7. El tab bar queda arriba y fijo; el contenido de cada pestaña ocupa el resto de la pantalla.
8. Con TalkBack: cada pestaña se anuncia con su nombre y si está seleccionada.
