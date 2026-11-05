# Ventas, inventario, recetas, mesas y caja

API bajo `/api/v1`. Las identidades y roles se obtienen de la sesión del servidor. Una escritura requiere además `X-CSRF-Token`. Los cuerpos no admiten campos adicionales: `tenantId`, `usuarioId`, roles, precio, IVA, número de factura y saldo no son autorizaciones ni importes aceptados del cliente.

Roles: A = ADMINISTRADOR, J = CAJERO, I = INVENTARIO. El cajero sólo consulta y opera sus ventas y sesiones. El administrador puede operar las del negocio actual. Los identificadores de recursos son UUID; un recurso ajeno al negocio no se recupera.

| Método y ruta | Roles | Cuerpo o filtro | Resultado |
|---|---|---|---|
| GET /bodegas | A,J,I | — | Lista hasta200 |
| POST /bodegas | A,I | nombre | Bodega creada201 |
| GET /existencias | A,J,I | bodegaId opcional | Saldos y unidad, hasta500 |
| GET /movimientos | A,I | bodegaId opcional | Kardex firmado, hasta500 |
| POST /movimientos | A,I | bodegaId,productoId,cantidadDelta,tipo,documentoOrigen | Entrada o ajuste201 |
| GET /mesas | A,J | — | Mesas hasta200 |
| POST /mesas | A | codigo | Mesa disponible201 |
| PATCH /mesas/{id}/estado | A | estado:DISPONIBLE o INACTIVA | Rechaza si existe venta abierta |
| GET /cajas | A,J | — | Cajas hasta200 |
| POST /cajas | A | nombre | Caja activa201 |
| GET /sesiones-caja | A,J | — | Sesiones autorizadas hasta200 |
| POST /sesiones-caja | A,J | cajaId,fondoInicial | Sesión abierta201 |
| GET /sesiones-caja/{id} | A,J | — | Efectivo, externo, esperado, diferencia si cerrada |
| POST /sesiones-caja/{id}/cerrar | A,J | totalContado | Cierre inmutable |
| GET /recetas | A,J,I | — | Versiones hasta200 |
| GET /recetas/{id} | A,J,I | — | Versión y componentes con unidad |
| POST /recetas | A,I | productoPlatoId,version,rendimiento,componentes:[{productoInsumoId,cantidad}] | Versión inactiva201 |
| POST /recetas/{id}/activar | A,I | — | Activa una versión y desactiva la anterior |
| GET /ventas | A,J | — | Ventas autorizadas hasta200 |
| POST /ventas | A,J | bodegaId,sesionCajaId,mesaId opcional,lineas:[{productoId,cantidad}] | Borrador con precio/IVA del catálogo201 |
| PUT /ventas/{id}/detalle | A,J | lineas:[{productoId,cantidad}] | Reemplaza detalle del borrador y revisa precios |
| GET /ventas/{id}/comprobante | A,J | — | Líneas, importes históricos y pagos |
| POST /ventas/{id}/anular | A,J | — | Sólo borrador; libera mesa |
| POST /ventas/{id}/confirmar | A,J | medio:EFECTIVO o EXTERNO,referencia,importe; cabecera Idempotency-Key | Confirma atómicamente o devuelve el mismo comprobante |

Los límites de consulta son explícitos; no son exportaciones completas. Los reportes del módulo administrativo proporcionan consultas por período y CSV autorizado.

## Reglas transaccionales

Cada escritura usa el mismo datasource MySQL y `@Transactional` con aislamiento READ_COMMITTED. Confirmar, editar, anular y cerrar adquieren primero el bloqueo de la sesión de caja; confirmar continúa con venta, mesa, productos en orden de UUID, recetas en orden de UUID y existencias por producto. El cierre espera los cobros en curso y el cobro posterior a un cierre se rechaza. Los errores de negocio son RuntimeException y provocan rollback de toda la operación.

La cantidad vendida es entera, entre1y1.000.000 por producto. No se repite producto en el detalle; para aumentar unidades se modifica su cantidad. Los insumos tienen tres decimales en su propia unidad. Una receta almacena cantidades para su rendimiento: consumo de cada componente = cantidad_de_receta × unidades_vendidas / rendimiento, redondeado hacia arriba a la milésima por línea/componente. La demanda de todos los productos directos y de todos los componentes se suma por producto antes de verificar existencias y escribir. Crear o editar el borrador rechaza la adición sin disponibilidad conjunta, sin reservar ni descontar. La confirmación revalida esa disponibilidad porque otro cobro o ajuste puede modificarla después. Un plato de receta consume sólo sus insumos; no se descuenta además el plato terminado. El kardex conserva origen venta/línea/receta/componente y la misma cantidad que se restó del saldo.

Las versiones de receta y su composición no se editan ni borran por esta API. Cambiar una preparación exige una nueva versión. Sólo una versión permanece activa por plato. Confirmar verifica que el plato y sus insumos sigan activos y que la receta fijada en el borrador siga vigente. La existencia del origen queda protegida por las claves compuestas del modelo.

El precio y la tasa se toman del catálogo al escribir el detalle. La confirmación rechaza cambios posteriores del catálogo y exige revisar el detalle; un pago no cambia silenciosamente su importe. Tras pagar, los valores de línea permanecen históricos aunque cambie el catálogo. Por línea: base=ROUND(cantidad×precio,2), IVA=ROUND(base×tasa,2); total=suma de bases e IVA. Se usa BigDecimal y redondeo HALF_UP para importes, en concordancia con `v_total_venta`. Se rechazan desbordamientos de DECIMAL(14,2)/(14,3).

La confirmación registra, en una sola transacción, clave/huella de idempotencia, disminución de stock, kardex, pago, estado PAGADA, identificador académico de factura y evento VENTA_CONFIRMADA/PENDIENTE. Una clave repetida con venta, medio, referencia e importe idénticos devuelve el comprobante previo, incluso tras cerrar caja. Otra solicitud con esa clave devuelve409. Otra clave sobre venta pagada devuelve409; nunca vuelve a consumir inventario. Las claves ASCII se normalizan a minúsculas y pertenecen al negocio de la sesión. La PK existente `(tenant_id,clave_idempotencia)` y SHA-256 de solicitud son suficientes; no se crea otra tabla de idempotencia. Una referencia de pago ya usada provoca rollback completo.

Entradas/ajustes usan el identificador documental del origen y la UNIQUE existente. Repetir el mismo origen/producto/bodega/tipo con igual delta devuelve el movimiento previo; modificar su delta devuelve409. Una entrada es positiva; un ajuste no puede ser cero ni producir saldo negativo. Se mueve sólo inventario físico DIRECTO activo. Los saldos nuevos empiezan en cero y se justifican con una entrada.

Una mesa se ocupa al crear un borrador y se libera al confirmar o anular, si no quedan borradores. No se admiten dos borradores simultáneos en la mesa ni cambios manuales de estado mientras tenga una venta abierta.

El cierre conserva `fondo_inicial + pagos CONFIRMADO/EFECTIVO` como esperado. Los pagos EXTERNO se informan aparte y no aumentan el conteo de efectivo. Diferencia=contado−esperado. EXTERNO representa una confirmación registrada por el operador: esta API no conecta bancos ni certifica una pasarela. Los comprobantes e identificadores ACA son académicos; no implementan habilitación de facturación electrónica DIAN ni pagos reales. No se implementan devoluciones ni anulación de ventas pagadas; su ampliación exige un flujo contable e inventario propio.

## Evolución del esquema

El DDL y los146CHECK del modelo61 se conservan byte a byte. Una migración posterior03 añade `pago.medio VARCHAR(16) NOT NULL` y `CHECK(medio IN ('EFECTIVO','EXTERNO'))`. Se ejecuta con la tabla pago vacía antes de la semilla de esta aplicación; no se asigna un medio supuesto a pagos históricos. Permanecen26tablas. El corte92 añade después la migración04: existencia.minimo DECIMAL(14,3) NOT NULL DEFAULT0.000 y su CHECK no negativo. El esquema vigente conserva26tablas,23UK,42FK y3vistas; su total es148CHECK después de SQL01→02→03→04.

## Errores y fuentes técnicas

400: formato, escala, campo inesperado, cantidad o UUID inválido.401: sesión requerida.403: rol o propiedad no autorizados.404: recurso no encontrado en negocio actual.409: caja/mesa/venta/receta no disponible, existencias, precio o importe no coincidente, clave/referencia duplicada u otra restricción operativa. Los mensajes no devuelven SQL ni datos de otros negocios.

MySQL8.4, locking reads: https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html

Spring Framework, transacciones declarativas: https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html

Guía7 y rúbricas GA7-220501096-AA5-EV03/EV04: servicios según requerimientos, validaciones, versionamiento y pruebas posteriores con Postman. La implementación local se somete a pruebas antes de componer la evidencia91.

## Mínimos por existencia en el corte1.1.0

`PUT /api/v1/existencias/minimo` admite ADMINISTRADOR e INVENTARIO, sesión y CSRF. El cuerpo contiene exactamente `bodegaId`, `productoId` y `minimo`. El producto debe estar activo y utilizar descuento DIRECTO; el mínimo es decimal no negativo con hasta tres decimales. La bodega y el producto pertenecen al negocio del actor.

La transacción configura el umbral sin modificar el saldo. Si no existe la fila de existencia, la crea con stock0. La consulta de existencias devuelve `minimo`, `alerta` y `agotado`: mínimo0 desactiva alerta; mínimo positivo y stock menor o igual al mínimo la activa; agotado corresponde a stock0.
