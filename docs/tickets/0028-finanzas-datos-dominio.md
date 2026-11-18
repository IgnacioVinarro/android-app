# Ticket 0028 – Finanzas: datos + reglas de dominio (FI-01, FI-02, FI-04, FI-05, FI-06, parte 1)

## Estado
En revisión

## Descripción
Primer paso de la Fase 2. Crea la base de datos y las reglas puras de Finanzas: cuentas,
categorías, transacciones y presupuestos, con el cálculo de saldos y de progreso/alertas de
presupuesto. **Sin pantallas todavía** (van en el ticket 0029).

**Alcance acotado a propósito:**
- Sin UI, sin ViewModels, sin navegación: el destino "Finanzas" sigue como está.
- Sin transacciones recurrentes (FI-07), vencimientos (FI-13), multimoneda (FI-11), CSV (FI-12),
  filtros (FI-09) ni estadísticas (FI-08): son pasos posteriores.
- Sin notificaciones de presupuesto: acá solo se calcula el nivel de alerta (la función pura);
  el canal y la notificación se conectan más adelante.
- Etiquetas (FI-02): **decidido: tabla aparte**, con lista propia que se puede crear, editar y
  eliminar (borrado lógico). Relación muchos a muchos con las transacciones. La pantalla para
  gestionarlas va en el ticket 0029; el filtro por etiqueta (FI-09) queda para más adelante.
- Moneda: **decidido: solo ARS**. Sin multimoneda (FI-11).
- Presupuesto: **decidido: se repite solo cada mes y es editable**. Al editarlo, el nuevo monto
  rige desde ese mes y se mantiene en los siguientes hasta la próxima edición. Se guarda un monto
  por categoría con el mes desde el que rige (`effectiveFrom`) y el límite de cada mes se calcula
  tomando el más reciente anterior o igual; no se generan filas mensuales.

## Reglas (de CLAUDE.md)
- Dinero: `Long` en centavos. Nunca decimales.
- Saldo = ingresos − gastos − transferencias salientes + transferencias entrantes (se calcula).
- Transferencia = una sola fila con `transferAccountId`; no cuenta como ingreso ni gasto.
- Presupuesto con alertas al 80 % y al 100 %.
- Todas las tablas: `id`, `uuid`, `createdAt`, `updatedAt`, `status` (0 existe / -1 borrada).
  Borrado siempre lógico; las consultas filtran `status = 0`.
- Fecha como `epochDay`. FK al `id` numérico, con índice.
- Borrar una categoría con transacciones exige reasignarlas.

## Checklist de cambios
- [ ] `finance/data`: `AccountEntity` (`account`): nombre, tipo (efectivo/banco/tarjeta/billetera),
      saldo inicial (centavos), ícono, color
- [x] `finance/data`: `CategoryEntity` (`category`): nombre, tipo (ingreso/gasto), ícono, color,
      `isDefault`
- [x] `finance/data`: `TransactionEntity` (`finance_transaction`): tipo (INCOME/EXPENSE/TRANSFER),
      monto, fecha, `accountId`, `categoryId?`, `transferAccountId?`, nota
- [x] `finance/data`: `BudgetEntity` (`budget`): `categoryId?` (`null` = total), mes desde el que
      rige (`effectiveFrom`, año+mes), monto límite (centavos ARS)
- [x] `finance/data`: `TagEntity` (`tag`): nombre (único entre las no borradas)
- [x] `finance/data`: `TransactionTagEntity` (`transaction_tag`): `transactionId`, `tagId`;
      índice único por el par; con las columnas estándar y borrado lógico
- [x] `finance/domain`: `budgetLimitFor(budgets, month)` (pura): devuelve el límite vigente
- [x] DAOs de las entidades (insertar, actualizar, borrado lógico, observar, todo con
      `status = 0`)
- [x] Repositorios por área (`AccountRepository`, `CategoryRepository`, `TransactionRepository`,
      `BudgetRepository`, `TagRepository`) con interfaz + implementación + módulo Hilt
- [x] Al borrar una etiqueta, quitarla (borrado lógico) de las transacciones que la tienen
- [x] Reasignar transacciones al borrar una categoría (operación del repositorio)
- [x] Categorías predefinidas sembradas al crear la base (claves de texto para ícono/color;
      nombres desde `strings.xml`)
- [x] `AppDatabase`: versión 8, nuevas entidades, `MIGRATION_7_8` escrita **y probada**
- [x] Exportar esquema versión 8 a `app/schemas/`
- [x] `finance/domain`: `accountBalance(account, transactions)` (pura)
- [x] `finance/domain`: `budgetProgress(limit, spent)` → porcentaje + nivel de alerta
      (`NONE`, `WARNING_80`, `REACHED_100`) (pura)
- [x] `finance/domain`: validación de transacción (monto > 0, transferencia con cuenta destino
      distinta del origen, gasto/ingreso con categoría) (pura)
- [x] Pruebas unitarias: saldos (ingreso, gasto, transferencia saliente y entrante, saldo
      inicial), presupuesto (bordes 79/80/99/100/101 %, límite 0), validación
- [x] Pruebas unitarias de repositorios (MockK sobre DAOs)
- [x] Pruebas instrumentadas: DAOs (Room en memoria) y migración 7→8 — **escritas, no corridas** (ver notas)
- [x] Compilar (`assembleDebug`, `assembleDebugAndroidTest`), correr `test` y `lint`

## Preguntas abiertas (consultar antes de empezar)
Resueltas: etiquetas (tabla aparte), moneda (solo ARS), presupuesto (repetición automática
editable).
Todas resueltas. Respaldo JSON: **ticket aparte** (0030, depende de este).

## Notas
- Los módulos no se llaman entre sí: `finance` no importa nada de `planner`.
- Las pruebas instrumentales pueden quedar sin correr si no hay emulador/`adb`; declararlo.
- **No se pudieron correr** `./gradlew connectedAndroidTest` ni las pruebas instrumentadas nuevas
  (`FinanceDaoTest`, `MigrationTest.migrarDe7a8…`): este entorno no tiene `adb` ni dispositivo
  (igual que en 0007/0008). Compilan (`assembleDebugAndroidTest` en verde) y quedan para correr
  en el emulador/teléfono. Lo que sí corrió en verde: `assembleDebug`, `assembleDebugAndroidTest`,
  `test` (78 pruebas nuevas de Finanzas) y `lint`.
- **Presupuesto en la UI (para 0029/0030):** `BudgetRepository.setLimit(categoryId, month, amount)`
  crea o reemplaza el límite "desde ese mes"; `budgetLimitFor` resuelve el vigente de cada mes.
- **Decisiones que tomé y conviene que confirmes:**
  1. La transferencia **no lleva categoría** (`validateTransaction` lo exige).
  2. Alerta de presupuesto: `REACHED_100` se dispara al **llegar** a 100 % (no solo al pasarlo),
     según "alertas al llegar al 80 % y al 100 %" de FI-06.
  3. **No hay `delete` de cuentas** en `AccountRepository`: falta definir qué pasa con las
     transacciones de una cuenta borrada (¿impedirlo, o reasignarlas como con las categorías?).
     Pendiente para el ticket 0029.
  4. Al borrar una categoría con transacciones, el destino debe ser **del mismo tipo** (gasto→gasto,
     ingreso→ingreso); sus presupuestos se borran con ella.
  5. Alta de transacción + etiquetas son dos escrituras seguidas, no una sola transacción de Room
     (si falla la segunda queda sin etiquetas, estado válido).
- Las categorías predefinidas se siembran al abrir la app la primera vez (`FinanceSeeder`, desde
  `StayFocusedApplication`), no en la migración, porque los nombres vienen de `strings.xml`. Los
  `iconKey` `category_*` todavía no tienen dibujo: el mapeo clave→ícono llega en 0029.
- **Hasta el ticket 0030** el respaldo JSON no incluye las tablas de Finanzas: exportar no las
  guarda e importar no las borra.
