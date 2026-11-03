# Nómina didáctica, contabilidad y reportes

API bajo `/api/v1`. El negocio, el usuario y sus roles proceden de una sesión real del servidor; se releen en cada solicitud. Las escrituras requieren `X-CSRF-Token`. Los DTO rechazan campos desconocidos: el cliente no fija tenant, roles, netos, saldos contables ni importes del origen contable.

Roles: A = ADMINISTRADOR; N = NOMINA; C = CONTABILIDAD; I = INVENTARIO; J = CAJERO. Todos los identificadores de entidad son UUID. Un identificador de otro negocio devuelve404, sin revelar el recurso ajeno.

## Servicios de nómina

| Método y ruta | Roles | Datos | Resultado |
|---|---|---|---|
| GET /empleados | A,N | — | Lista, máximo1000 |
| GET /empleados/{id} | A,N | — | Empleado del negocio |
| POST /empleados | A,N | documento,nombre,estado ACTIVO/INACTIVO | Alta201 |
| PUT /empleados/{id} | A,N | documento,nombre,estado | Cambio; identidad conservada si tiene historial |
| GET /novedades | A,N | periodo AAAA-MM opcional | Lista, máximo1000 |
| GET /novedades/{id} | A,N | — | Novedad del negocio |
| POST /novedades | A,N | empleadoId,periodo,concepto,tipo DEVENGO/DEDUCCION,importe | Borrador201 |
| PUT /novedades/{id} | A,N | Mismos campos de alta | Sólo borrador sin consumir |
| POST /novedades/{id}/autorizar | A,N | — | Autoriza borrador de empleado activo |
| POST /novedades/{id}/rechazar | A,N | — | Rechaza borrador o autorización no consumida |
| GET /liquidaciones | A,N | periodo opcional | Cabeceras, máximo1000 |
| POST /liquidaciones | A,N | periodo,version entera1–1.000.000 | Cabecera BORRADOR201 |
| GET /liquidaciones/{id} | A,N | — | Cabecera, detalles y novedades efectivamente aplicadas |
| GET /liquidaciones/{id}/simulacion | A,N | — | Candidatas autorizadas disponibles y suma de prueba, sin escribir |
| POST /liquidaciones/{id}/aprobar | A,N | novedadesIds:[UUID,...],1–1000 distintos | Aprobación completa e inmutable |

La liquidación suma importes introducidos como novedades en la base. Por empleado: devengos=SUM(DEVENGO), deducciones=SUM(DEDUCCION), neto=devengos−deducciones. No presupone salario, horas, auxilios, prestaciones, tasas laborales, retenciones ni aportes legales. Es aritmética didáctica sobre entradas explícitas, no una liquidación laboral normativa. Los importes de novedades son positivos, de hasta14dígitos con2decimales; las sumas por empleado y por liquidación respetan DECIMAL(14,2). Se rechazan deducciones superiores a devengos y liquidaciones sin devengos positivos.

La simulación lee candidatas en ese momento; no reserva ni consume novedades. Aprobar exige enviar la lista exacta que se decidió aplicar: cada ID debe pertenecer al negocio y período, estar AUTORIZADA y no tener puente de consumo. Dentro de una sola transacción, se bloquea tenant→liquidación→novedades ordenadas por empleado/ID. Se escriben todos los detalles, los puentes, el estado APROBADA y el evento NOMINA_APROBADA/PENDIENTE. La PK existente `(tenant_id,novedad_id)` impide aplicar la misma novedad a otra liquidación, incluso entre versiones. No se añade tabla ni se modifica el modelo.

Repetir la aprobación de la misma liquidación con la misma selección devuelve su resultado; otra selección devuelve409. Una liquidación APROBADA/CONTABILIZADA carece de operaciones de edición o borrado. Una versión posterior sólo puede usar novedades aún disponibles. Las identidades de empleados con historial se conservan; puede cambiarse su estado activo/inactivo, sin alterar cifras anteriores.

## Cuentas, asientos y eventos

| Método y ruta | Roles | Datos | Resultado |
|---|---|---|---|
| GET /cuentas | A,C | — | Lista hasta1000 |
| POST /cuentas | A,C | codigo,nombre,naturaleza DEBITO/CREDITO | Alta201 |
| PUT /cuentas/{id} | A,C | Mismos campos | Sólo mientras no tenga líneas registradas |
| GET /asientos | A,C | — | Cabeceras hasta1000 |
| GET /asientos/{id} | A,C | — | Líneas y totales débito/crédito/diferencia |
| POST /asientos | A,C | eventoId,cuentaDebitoId,cuentaCreditoId,cuentaAuxiliarId opcional | Procesa origen y devuelve asiento registrado201 |
| GET /eventos | A,C | estado opcional PENDIENTE/PROCESADO/ERROR | Lista hasta1000 |
| GET /eventos/{id} | A,C | — | Estado y origen tipado |
| POST /eventos/{id}/procesar | A,C | cuentaDebitoId,cuentaCreditoId,cuentaAuxiliarId opcional | Contabiliza atómicamente o repite el mismo resultado |

Ambas rutas de registro usan el mismo servicio transaccional. El cliente elige cuentas del negocio, no importes. Se exigen cuentas diferentes y naturaleza DEBITO para la primera y CREDITO para las restantes. Esta asignación es académica; no incorpora un plan único de cuentas oficial ni una parametrización tributaria.

* VENTA_CONFIRMADA exige venta PAGADA, factura y pago CONFIRMADO coincidente. Débito=total; crédito principal=base; crédito auxiliar=IVA si es positivo. Los importes proceden de las líneas históricas de `v_total_venta`, no del catálogo vigente.
* NOMINA_APROBADA exige liquidación APROBADA/CONTABILIZADA con detalles. Débito=devengos; crédito principal=neto; crédito auxiliar=deducciones si son positivas. Los créditos de importe cero no generan línea. Si el neto es cero, se registra el crédito de deducciones y se conservan dos líneas positivas.

La cuenta auxiliar es obligatoria cuando IVA/deducciones es positivo y debe omitirse cuando ese importe es cero. Cada asiento tiene al menos dos líneas y suma débito exactamente igual a crédito mediante BigDecimal. Se bloquean tenant→evento→origen y cuentas del mismo negocio; se insertan cabecera y todas sus líneas y se marca el evento PROCESADO, con fecha real y error nulo, en una transacción. Nómina pasa además a CONTABILIZADA. Un rechazo revierte todo y conserva pendiente el evento; no se registra un error ficticio ni un asiento parcial. Un evento ERROR preexistente puede reintentarse si su origen y cuentas son válidos. No existe un trabajador externo que procese eventos automáticamente.

Las UNIQUE existentes por origen y por evento impiden duplicados. Repetir un evento PROCESADO con las mismas cuentas efectivas devuelve el mismo asiento; cambiar sus cuentas devuelve409. Cabecera y líneas REGISTRADO no se editan ni borran. No se implementan reversión, cierre fiscal, integración bancaria ni facturación DIAN.

## Reportes autorizados y CSV

`GET /reportes/{area}?desde=AAAA-MM-DD&hasta=AAAA-MM-DD` entrega JSON. `GET /reportes/{area}/csv` con los mismos parámetros entrega CSV UTF-8. Intervalo inclusivo de1a366días; años1000a9998. Más de10000filas produce413: no se recorta silenciosamente el reporte.

| Área | Roles | Fecha del conjunto | Filas y totales |
|---|---|---|---|
| ventas | A,C,J | venta.pagada_en | Ventas pagadas con pago confirmado; bases,IVA,total,pagado. J sólo ventas propias |
| nomina | A,N | liquidacion.creada_en | Detalles de liquidaciones APROBADA/CONTABILIZADA; devengos,deducciones,neto |
| contabilidad | A,C | asiento.fecha | Líneas REGISTRADO; débito/crédito |
| inventario | A,I | movimiento.registrado_en | Kardex con producto,bodega,unidad y delta; conteo de movimientos |
| caja | A,J | sesion.cerrada_en | Cierres: fondo,esperado,contado,diferencia,externo. J sólo sesiones propias |

La nómina usa fecha de creación porque el modelo no almacena fecha de aprobación; no se presenta esa fecha como aprobación. El reporte de caja usa el esperado conservado al cierre; EXTERNO se suma aparte y no aumenta el efectivo. Inventario no suma cantidades de productos o unidades diferentes como si fueran una magnitud única. Los reportes no son balances fiscales ni certificaciones regulatorias.

Cada día se interpreta en America/Bogota. Se convierte su inicio y el inicio del día siguiente a UTC; el filtro es `[desdeUTC,hasta+1UTC)`. MySQL y JDBC operan en UTC y las fechas de salida muestran el desplazamiento de Bogotá. Las autorizaciones de área se verifican antes de consultar. Los JOIN conservan las claves compuestas y el filtro tenant de la sesión; no se acepta negocio ni propietario desde parámetros del reporte.

JSON incluye `origen` con tenantId,usuarioId,roles vigentes,moneda y emisión en Bogotá, además de área,fechas,zona,columnas,filas y totales. CSV incluye primero el bloque `metadato,valor` con ese mismo origen y período, una fila vacía, el encabezado de la tabla, sus filas y `TOTAL (n filas)`. Los importes del TOTAL se derivan en memoria de las mismas filas autorizadas; no hay otra consulta de agregados con filtros distintos. Entre dos peticiones puede cambiar la base; cada exportación representa su propio corte, no una fotografía inmutable compartida entre solicitudes.

Todas las celdas se entrecomillan y duplican comillas internas. Los campos de texto que puedan iniciar una fórmula (`=`, `+`, `-`, `@`, también tras espacios o en variantes Unicode de ancho completo) o un control inicial reciben apóstrofo. Se usa NFKC sólo para comprobar el prefijo; el texto original se conserva. Las cantidades numéricas negativas siguen siendo números. La sanitización afecta la exportación, no modifica los datos guardados.

Las siete áreas del proyecto son Identidad,Ventas,Inventario,Gastronomía,Nómina,Contabilidad yReportes. No equivalen a siete clases de informe: caja aporta el cierre transaccional de Gastronomía; Reportes es el módulo de consulta/exportación. Identidad se consulta como estado actual mediante `/usuarios` y `/roles` de A; esas tablas no tienen fecha de creación y no se inventa historial por período. Los permisos de cada área se mantienen también al consultar sus datos.

## Validación

Suite `pruebas/C_pruebas_http.py`: 222 de 222 comprobaciones aprobadas mediante HTTP real y MySQL 8.4.13 aislado, con datos sintéticos. Se verificaron permisos por área y negocio, selección explícita y consumo único de novedades, dos aprobaciones simultáneas, dos contabilizaciones simultáneas del mismo evento, asientos completos y equilibrados, inmutabilidad del historial, integración de venta/caja, límites de día en Bogotá con microsegundos y coincidencia de JSON/CSV. La exportación se contrastó además con prefijos de fórmula ASCII y Unicode, comillas y separadores dentro del texto.

La ejecución corresponde al JAR SHA-256 `8d406676b5c743c289a6286fc2684f0e63a14d6912d39e0bf1ab35843682eddd`. El registro conserva cada respuesta y resultado; no contiene las credenciales de laboratorio. Las carreras concurrentes pertenecen a esta suite HTTP. Una colección Postman secuencial no sustituye esas pruebas de concurrencia.

Fuentes técnicas: [MySQL8.4, locking reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html); [Spring, transacciones declarativas](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html); [Java21, BigDecimal](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/math/BigDecimal.html); [OWASP, CSV Injection](https://owasp.org/www-community/attacks/CSV_Injection).
