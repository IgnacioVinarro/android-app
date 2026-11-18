# Ticket 0029 – Finanzas: pantallas de cuentas, categorías y transacciones (FI-01…FI-05, parte 2)

## Estado
Pendiente (depende del ticket 0028)

## Descripción
Segundo paso de la Fase 2. Primera pantalla real de Finanzas, usando los repositorios y reglas del
ticket 0028: ver el saldo de las cuentas, listar transacciones y registrar ingresos, gastos y
transferencias en pocos toques.

**Alcance acotado a propósito:**
- Sin presupuestos en pantalla ni estadísticas (ticket 0030).
- Sin widget ni atajo de alta rápida (FI-03 completo queda para después); sí un botón flotante
  dentro de la pantalla.
- Sin filtros ni buscador (FI-09).

## Checklist de cambios
- [ ] `FinanceViewModel` con un único `StateFlow<FinanceUiState>` y eventos
- [ ] Pantalla Finanzas: resumen de saldo por cuenta + lista de transacciones recientes
- [ ] Formulario de transacción (ingreso/gasto/transferencia): monto, fecha, categoría, cuenta,
      nota; teclado numérico, confirmación en < 300 ms, flujo < 5 s para un gasto
- [ ] Gestión de cuentas (crear, editar, borrar lógico)
- [ ] Gestión de categorías (crear, editar, borrar con reasignación obligatoria)
- [ ] Selector de ícono/color **reutilizando** los componentes de `core/ui` (extraer si hoy están
      en `planner`)
- [ ] Formato de moneda en `core/util` (reutilizable)
- [ ] Gestión de etiquetas (crear, renombrar, eliminar) y selector de etiquetas en el formulario
      de transacción
- [ ] Estados vacíos y errores con acción sugerida
- [ ] Textos en `strings.xml`; áreas táctiles ≥ 48 dp; `contentDescription`; color nunca como
      único indicador (ingreso/gasto con ícono o signo además del color)
- [ ] Pruebas de ViewModel (Turbine + MockK) y alguna prueba de Compose del camino crítico
- [ ] Compilar, correr `test` y `lint`; checklist manual para el teléfono
