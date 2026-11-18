# Ticket 0024 – Quinto ícono "Ajustes" al final de la barra inferior (cambio de navegación)

## Estado
Terminado

> **Superado por el ticket 0025.** El desarrollador pidió revertir este cambio: Ajustes vuelve a ser un
> destino secundario (se abre desde "Hoy") y el quinto ícono de la barra pasa a ser "Tiempo de pantalla".
> No hace falta probar la lista de abajo; se cierra junto con el 0025.

## Descripción
Cambio pedido por el desarrollador sobre la navegación de la app:

> Agregar un quinto ícono al final de todo que diga "Ajustes".

**Qué cambia:**
- La **barra inferior** pasa de 4 a **5 destinos**: Hoy, Finanzas, Gimnasio, Meditación y **Ajustes**
  (último). 5 es el máximo que admite la barra de Material 3.
- Nuevo destino `settings` con su ícono (engranaje) y su pantalla de entrada.

**Discrepancia con los documentos fuente (avisada, CLAUDE.md pide avisarlas):** el documento técnico (§4.2) y
CLAUDE.md decían que Ajustes era un destino **secundario** (se abría desde el menú de "Hoy"). Por pedido
explícito del desarrollador pasa a la barra inferior; **se actualizaron esos documentos** y el funcional (TR-01).
Los otros destinos secundarios (Música y auriculares, Tiempo de pantalla) no cambian.

**Decisiones de diseño tomadas en este paso (a revisar en la entrega):**
- **Pantalla provisoria:** la pantalla de Ajustes solo muestra su título, igual que Finanzas, Gimnasio, Dieta y
  Meditación hasta que se implementen. **No se inventó ningún ajuste:** qué ajustes lleva (tema, moneda, unidades,
  etc., TR-08 del documento funcional) es un paso aparte que hay que definir. Lo que ya existe en DataStore
  (`SettingsRepository`: tema y duración por defecto de una tarea) sigue funcionando igual, sin pantalla.
- La pantalla vive en `core/settings/ui`, junto al repositorio de ajustes que ya está en `core/settings`.
- Ícono: el engranaje de Material (`Settings`), que ya viene en el set básico de íconos (sin dependencias nuevas).
- Los textos reusan "Ajustes" (`nav_settings`, que ya existía) y suman el texto de accesibilidad de la pestaña.

## Checklist de cambios
- [x] `core/settings/ui` (`SettingsScreen.kt`): pantalla provisoria de Ajustes
- [x] `StayFocusedDestination`: agregar `SETTINGS` al final (barra de 5 destinos) + prueba actualizada
- [x] `StayFocusedApp`: ruta del destino Ajustes
- [x] Textos: `a11y_nav_settings`
- [x] Documentos: CLAUDE.md, funcional (TR-01) y técnico (§4.2) actualizados
- [x] `./gradlew assembleDebug`, `test` y `lint` en verde
- [ ] Probar manualmente en el emulador/teléfono (abajo)

## Lista de comprobación manual
1. La barra inferior muestra 5 destinos, en este orden: Hoy, Finanzas, Gimnasio, Meditación, Ajustes.
2. "Ajustes" es el último, con el ícono de engranaje y la etiqueta "Ajustes"; ninguna etiqueta se corta ni se
   superpone en pantalla chica (probar en un teléfono angosto y con el texto del sistema en tamaño grande).
3. Tocar Ajustes abre su pantalla (provisoria, solo el título) y el ícono queda marcado como seleccionado.
4. Ir a otro destino y volver a Ajustes funciona; los demás destinos siguen igual (Gimnasio con sus pestañas).
5. Con TalkBack: el destino se anuncia como "Pestaña Ajustes, preferencias de la app" y su estado de selección.
