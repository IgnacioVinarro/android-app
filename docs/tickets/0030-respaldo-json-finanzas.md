# Ticket 0030 – Respaldo JSON: incluir las tablas de Finanzas

## Estado
Pendiente (depende del ticket 0028)

## Descripción
Amplía el respaldo JSON del ticket 0026 para que exporte e importe las tablas de Finanzas
(`account`, `category`, `finance_transaction`, `budget`, `tag`, `transaction_tag`). Sube el número
de versión del formato de respaldo; la importación sigue aceptando respaldos de la versión
anterior (sin datos de Finanzas).

## Checklist de cambios
- [ ] Exportar las tablas de Finanzas (incluidas las filas con `status = -1`, igual que el resto)
- [ ] Importar validando y convirtiendo desde la versión anterior del formato
- [ ] Subir la versión del formato de respaldo
- [ ] Pruebas unitarias de exportación/importación, incluido un respaldo de versión anterior
- [ ] Compilar, correr `test` y `lint`
