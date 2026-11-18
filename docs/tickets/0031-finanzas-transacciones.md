# Ticket 0031 – Finanzas: transacciones (FI-01, FI-02, FI-03 parcial, parte 3)

## Estado
En revisión

## Descripción
Tercer paso de la Fase 2. Lista de movimientos en la pantalla Finanzas y formulario para registrar,
editar y borrar ingresos, gastos y transferencias en pocos toques (< 5 s para un gasto;
confirmación en < 300 ms). Usa `validateTransaction`, las cuentas del 0029 y las categorías
predefinidas ya sembradas.

**Alcance acotado a propósito:**
- Sin etiquetas (ticket 0033) ni gestión de categorías (0032): se usan las categorías que ya hay.
  Al editar una transacción se guardan las etiquetas como `emptySet()`; como todavía no hay forma de
  asignar etiquetas, no se pierde nada. **El 0033 tiene que cargar y conservar las etiquetas al editar.**
- Sin filtros ni buscador (FI-09), sin recurrentes (FI-07), sin widget ni atajo de alta rápida
  (FI-03 completo): solo el botón flotante dentro de la app.
- La lista muestra todos los movimientos, del más nuevo al más viejo; la paginación o el límite
  llegan con los filtros.
- Fecha por defecto: hoy. Cuenta por defecto: la primera de la lista.

**Cambio de comportamiento en la pantalla Finanzas:** el botón flotante pasa a ser "agregar
movimiento" (solo aparece si hay al menos una cuenta); agregar cuenta pasa a un botón junto al
título "Cuentas".

## Checklist de cambios
- [x] `core/util`: `filterMoneyInput` (reutilizada por cuentas y movimientos) y `formatShortDate`
      (`dd/MM/aaaa`) — puras, con pruebas
- [x] `core/ui`: `ChoiceChips` reutilizable (selección única, con tilde además del color)
- [x] `FinanceViewModel`: suma las filas de movimientos (con nombre de categoría y de cuentas)
- [x] `FinanceScreen`: lista de movimientos (ícono por tipo, signo en el monto, fecha, cuenta),
      estado vacío, botón flotante de alta, botón "agregar cuenta" junto a "Cuentas"
- [x] `TransactionEditorViewModel` + `TransactionEditorScreen`: tipo, monto, fecha, cuenta,
      categoría o cuenta destino, nota; validar con `validateTransaction`; crear, editar, borrar
- [x] Navegación: ruta del editor de movimientos en `StayFocusedApp`
- [x] Textos en `strings.xml`; áreas táctiles ≥ 48 dp; `contentDescription`; el color nunca es el
      único indicador (signo `+`/`-` e ícono por tipo)
- [x] Pruebas unitarias: `filterMoneyInput`, `formatShortDate`, `FinanceViewModel`,
      `TransactionEditorViewModel`
- [x] Compilar, correr `test` y `lint`; lista de comprobación manual para el teléfono

## Notas
- Corrió en verde: `assembleDebug`, `assembleDebugAndroidTest`, `test` y `lint`. Pruebas nuevas:
  `TransactionEditorViewModelTest` (15), `FinanceViewModelTest` +1, `MoneyTest` +2, `DateFormatTest` 1.
- `connectedAndroidTest` sigue sin poder correrse (sin `adb`/dispositivo). Este ticket no agregó
  pruebas instrumentadas: no toca DAOs ni el esquema.
- Sin prueba de Compose: la lógica vive en los ViewModels (con pruebas); las pantallas se cubren
  con la lista manual de abajo.
- Cambio visible: en Finanzas el botón flotante ahora agrega un **movimiento** (solo aparece con al
  menos una cuenta); agregar cuenta es el "+" junto al título "Cuentas".
- **Para el 0033:** el editor guarda las etiquetas como `emptySet()` al editar; cuando existan, hay
  que cargarlas y conservarlas.
- Las categorías se muestran como texto con tilde al elegirlas; el ícono por categoría llega con el
  0032 (mapeo de claves `category_*`).

## Lista de comprobación manual (teléfono)
1. Con una cuenta creada, tocar el **+** flotante: se abre "Nuevo movimiento" con el cursor en el
   monto y el teclado numérico.
2. Cargar un **gasto**: monto `1.500,50`, elegir categoría "Comida", Guardar. Vuelve a Finanzas, el
   movimiento aparece arriba con `-$ 1.500,50` y el saldo de la cuenta baja.
3. Cargar un **ingreso** (categoría "Sueldo"): aparece con `+` y el saldo sube.
4. Cargar una **transferencia** entre dos cuentas: se ve como `Cuenta A → Cuenta B`, sin signo; el
   total no cambia y cada cuenta se ajusta.
5. En el formulario, cambiar de Gasto a Ingreso: las categorías cambian y la elegida se descarta.
6. En una transferencia, la cuenta "Desde" no aparece como opción en "Hacia".
7. Escribir `0` o `12,345` como monto: aparece el error y Guardar queda deshabilitado.
8. Cambiar la fecha con el selector: el movimiento se guarda con esa fecha.
9. Tocar un movimiento: se edita con los datos cargados; cambiar el monto y guardar actualiza saldos.
10. Eliminar un movimiento (ícono de papelera): pide confirmación y los saldos se recalculan.
11. Ahora sí: crear una cuenta con movimientos e intentar **eliminarla**: aparece el aviso "No se
    puede eliminar" (pendiente de ver desde el ticket 0029).
12. Con TalkBack: cada fila se lee como "Gasto: Comida, -$ 1.500,50, 05/11/2026 · Efectivo".
