# Esencias & Estilo Solano — versión 2.0

Aplicación local de punto de venta y control de insumos de perfumería. Incluye catálogo, fórmulas y suministros versionados, ventas de contado atómicas, fiados y abonos, inventario, caja, incidencias, gastos y reportes Excel. Perfiles: Administrador y Vendedora.

## Contenido

- `backend/src`: API Java21/SpringBoot/JDBC y pruebas JUnit.
- `backend/sql`: esquema MySQL8.4, 28 tablas y migraciones de evolución.
- `frontend/src`: interfaz React; `frontend/pruebas`: pruebas del cliente.
- `pruebas_integracion`: campaña HTTP/MySQL portable para una base DEMO separada.
- `documentacion`: contrato API y operación/instalación mediante el script técnico.
- `backend/target` y `frontend/dist`: se generan al compilar. La distribución en Drive incluye el JAR y la web compilada; el repositorio público contiene las fuentes y no versiona binarios.

El contrato es PS-API-V2-r3. La aplicación mantiene la autoridad del servidor sobre permisos, existencias y dinero. Usa cantidades decimales, costo promedio ponderado y claves de operación para impedir duplicaciones. Una venta de contado sólo se entrega junto con su cobro exacto y confirmado; el crédito autorizado consume al entregar y cobra mediante abonos separados.

## Instalación y comprobaciones

Lea `documentacion/OPERACION_TECNICA.md` y el manual técnico. Requiere JDK21, Maven3.10.0, MySQL8.4, Python3.10+, Node22.12+ y npm. No se distribuyen cuentas ni contraseñas. El operador crea un esquema nuevo y una cuenta limitada, compila e inicializa una clave Admin privada. Las unidades estándar son los únicos datos incluidos por el DDL; el catálogo debe configurarse explícitamente.

Desde `backend`, `mvn test` ejecuta las pruebas unitarias. Desde `frontend`, `npm ci`, `npm test` y `npm run build` verifican y compilan el cliente. La campaña de integración se explica en `pruebas_integracion/LEEME.txt`; crea operaciones sintéticas y exige una base separada. Nunca se ejecuta contra registros operativos del negocio.

El conjunto unitario ampliado ejecutó 44 pruebas conformes: las 21 del corte inicial y 23 adicionales de catálogo, inventario, ventas, coordinación de emisión/cobro, caja y reportes. JUnit Jupiter 6.0.3 y Mockito aíslan reglas y colaboradores. La regresión de acceso cubre AuthService. Las unitarias no abren MySQL ni comprueban el rollback de Spring; esos efectos se verifican en la campaña de integración. Esa campaña verificó 428 comprobaciones HTTP/MySQL, contabilizadas por separado de las unitarias. Se conservan el mismo código de aplicación y el mismo JAR del corte probado.

La instalación técnica comprobada utiliza web 8094, API 8095 y MySQL 33193 en 127.0.0.1. Estos puertos son configurables y corresponden a laboratorio local. No representan alojamiento público ni infraestructura productiva. La carpeta `frontend/dist` se sirve mediante la configuración de proxy del frontend, no abriendo el HTML directamente.

## Versiones y alcance

Las pruebas técnicas con datos sintéticos no acreditan una aceptación de usuario, un piloto del negocio ni una capacitación. No se incluyen facturación DIAN, conexión bancaria, nómina ni cálculo automático de comisiones.
