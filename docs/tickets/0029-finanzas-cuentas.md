# Ticket 0029 – Finanzas: formato de dinero y pantalla de cuentas (FI-05, parte 2)

## Estado
Terminado

## Descripción
Segundo paso de la Fase 2 (la primera versión de este ticket abarcaba toda la interfaz de Finanzas y
era demasiado grande para un solo paso: se divide en 0029, 0031, 0032 y 0033). Este ticket deja la
primera pantalla real de Finanzas: el saldo total y las cuentas con su saldo, con alta, edición y
borrado de cuentas. Usa los repositorios y reglas del ticket 0028.

**Decisiones (tomadas con el desarrollador):**
- Pantalla Finanzas: saldo total arriba + tarjetas por cuenta (los movimientos se suman en 0031).
- Moneda: solo ARS.
- Borrar una cuenta: **se impide si tiene transacciones** (como origen o como destino de una
  transferencia); se muestra un aviso. Sin transacciones se borra (lógico).
- Gestión de categorías y etiquetas: pantallas aparte, a las que se llega desde un menú de la
  pantalla Finanzas (tickets 0032 y 0033).

**Alcance acotado a propósito:**
- Sin lista de movimientos ni formulario de transacción (ticket 0031).
- Sin categorías ni etiquetas en pantalla (0032 y 0033).
- Sin presupuestos ni estadísticas (tickets posteriores).
- Las cuentas no tienen selector de ícono: el ícono lo da el tipo de cuenta (efectivo, banco,
  tarjeta, billetera). Sí tienen selector de color (se reutiliza el de tareas).

## Checklist de cambios
- [x] `core/util/Money.kt`: `formatArs(cents)` (ej. `$ 1.234,56`, negativos con signo) y
      `parseArsToCents(text)` (acepta `1234,56`, `1.234,56`, `1234`; null si no es válido) — puras
- [x] `AccountDao`: `countTransactions(accountId)` (origen o destino) y `softDelete`
- [x] `AccountRepository.delete(id): Boolean` (false si tiene transacciones, sin borrar)
- [x] `FinanceViewModel`: un `StateFlow<FinanceUiState>` con el saldo total y el saldo de cada cuenta
- [x] `FinanceScreen`: saldo total, tarjetas por cuenta (ícono por tipo, color, saldo), estado
      vacío con acción "Crear cuenta", botón flotante para agregar
- [x] `AccountEditorViewModel` + `AccountEditorScreen`: nombre, tipo, saldo inicial, color;
      crear, editar y borrar (con aviso si tiene transacciones)
- [x] Navegación: rutas del editor de cuentas en `StayFocusedApp`
- [x] Textos en `strings.xml`; áreas táctiles ≥ 48 dp; `contentDescription`; el color nunca es el
      único indicador (el saldo negativo lleva signo)
- [x] Pruebas unitarias: `Money`, `AccountRepositoryImpl.delete`, `FinanceViewModel`,
      `AccountEditorViewModel`
- [x] Prueba instrumentada de los DAOs nuevos de cuenta — **escrita, no corrida** (sin `adb`)
- [x] Compilar, correr `test` y `lint`; lista de comprobación manual para el teléfono

## Notas
- Corrió en verde: `assembleDebug`, `assembleDebugAndroidTest`, `test` (29 pruebas nuevas: `MoneyTest`
  11, `AccountRepositoryImplTest` +2, `AccountEditorViewModelTest` 10, `FinanceViewModelTest` 3) y `lint`.
- **No se pudo correr** `connectedAndroidTest` (sin `adb` ni dispositivo): quedan sin ejecutar las
  pruebas nuevas de `FinanceDaoTest` (`countTransactions`, `softDelete` de cuenta) y todas las
  instrumentadas anteriores.
- No hay prueba de Compose: las pantallas son simples (lista + formulario) y la lógica está en los
  ViewModels, que sí tienen pruebas; se cubren con la lista manual de abajo.
- Dividí el ticket original: transacciones → 0031, categorías → 0032, etiquetas → 0033.
- `AccountDao`/`AccountRepository` (ticket 0028) ganaron `countTransactions`, `softDelete` y `delete`.
- El borrado de cuentas se impide si tiene transacciones; no existe todavía forma de crear
  transacciones desde la app, así que ese aviso se verá recién con el 0031.

## Lista de comprobación manual (teléfono)
1. Abrir **Finanzas** sin cuentas: aparece el mensaje y el botón "Crear cuenta".
2. Crear una cuenta "Efectivo" con saldo inicial `1.500,50`: aparece en la lista con `$ 1.500,50` y
   el saldo total coincide.
3. Crear una tarjeta con saldo inicial `-200`: el saldo se ve como `-$ 200,00` y el total baja.
4. Tocar una cuenta: se abre el editor con los datos cargados; cambiar nombre, tipo y color y
   guardar: la lista se actualiza.
5. En el editor escribir `12,345` como saldo: aparece el error y "Guardar" queda deshabilitado.
6. Eliminar una cuenta: pide confirmación y desaparece de la lista.
7. Modo oscuro y fuente grande: nada se corta ni se superpone.
8. Con TalkBack: cada tarjeta se lee como "Cuenta …, tipo …, saldo …. Tocá para editar".
