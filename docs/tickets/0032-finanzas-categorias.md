# Ticket 0032 – Finanzas: gestión de categorías (FI-04, parte 2)

## Estado
Terminado

## Descripción
Pantalla para crear, editar y borrar categorías de ingreso y de gasto, con ícono y color. Al borrar una
categoría con transacciones hay que elegir otra del mismo tipo a la que reasignarlas
(`CategoryRepository.delete`). Se abre desde un menú de la pantalla Finanzas.

## Checklist de cambios
- [x] Mapeo clave→ícono de categorías (`category_*`) en `core/ui` y selector de ícono reutilizable
- [x] Pantalla de categorías (gasto / ingreso) y editor
- [x] Borrado con diálogo de reasignación
- [x] Menú en la pantalla Finanzas
- [x] Textos, accesibilidad, pruebas de ViewModel, compilar, `test`, `lint`
