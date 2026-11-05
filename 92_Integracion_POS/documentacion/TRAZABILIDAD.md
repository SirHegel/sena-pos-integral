# Trazabilidad del corte integrado1.1.0

| RF | Caso | Historias | Alcance |
|---|---|---|---|
| RF01 | CU-01 | US-01 | Acceso e identidad del negocio |
| RF02 | CU-09 | US-08 | Usuarios y roles |
| RF03 | CU-03 | US-02 | Preparación de venta |
| RF04 | CU-04 | US-03 | Pago y comprobante |
| RF05 | CU-02 | US-03, US-09 | Catálogo, bodegas e inventario |
| RF06 | CU-06 | US-05 | Gastronomía y caja |
| RF07 | CU-08 | US-07 | Nómina |
| RF08 | CU-07 | US-06 | Contabilidad |
| RF09 | CU-05 | US-04 | Reportes |

## Reglas del corte

- Negocio, empresa y tenant identifican la misma frontera. La columna física es tenant_id.
- Preparar un borrador no reserva ni descuenta stock. Confirmar pago revalida y registra pago, venta, saldo, kardex, evento e idempotencia en una transacción.
- DIRECTO consume el producto; RECETA consume insumos según versión y rendimiento, redondeando hacia arriba a tres decimales.
- Nómina aplica una sola vez las novedades autorizadas del empleado y período seleccionadas explícitamente.
- Contabilidad procesa después el evento persistido y deriva sus importes del documento de origen.
- El esquema tiene26tablas,167columnas,26PK,23UK,42FK,148CHECKy3vistas después de SQL01→02→03→04.
- Un origen admite como máximo un evento de su tipo y un asiento. Las referencias a venta y nómina son excluyentes.

Los identificadores RF01–RF09 corresponden al contrato integrado. Los RF-01–RF-08 de análisis anteriores conservan su significado histórico; no se equiparan solo por su número.
