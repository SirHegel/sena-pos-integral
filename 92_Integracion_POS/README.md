# POS Integral: integración web 1.1.0

Jhon Steven Alvarez Ruiz · ADSO · SENA · Ficha 3388966.

Este corte conecta las 19 vistas React con la API Java y la persistencia MySQL de las siete áreas del proyecto: identidad, ventas, inventario, gastronomía/caja, nómina, contabilidad y reportes. El servidor determina negocio, usuario, roles, importes, existencias y estados. La aplicación contiene 25 clases Java de producción y 63 operaciones REST.

## Estructura

- `backend/src/main`: controladores, servicios, sesión, validación y acceso con JdbcTemplate.
- `backend/src/test`: pruebas unitarias existentes del corte 1.1.0.
- `backend/sql`: cuatro migraciones ordenadas del esquema MySQL.
- `backend/pruebas`: pruebas HTTP reproducibles; la configuración de conexión se proporciona fuera del repositorio.
- `frontend/src`: vistas, sesión, cliente HTTP y utilidades React.
- `frontend/pruebas`: pruebas del cliente y sus conversiones.
- `documentacion/ENDPOINT.json`: método, ruta, rol, CSRF y estado de respuesta de las 63 operaciones.
- `documentacion/TRAZABILIDAD.md`: relación de RF01–RF09, CU-01–CU-09 e historias US-01–US-09.
- `documentacion/modelos`: diagramas vectoriales editables y diccionario físico completo.

## Preparar el entorno

Requisitos: JDK 21, Maven, MySQL 8.4 y Node compatible con `package.json`. Los puertos del entorno local documentado son frontend 8092, API 8093 y MySQL 33192.

1. En una base de laboratorio nueva, aplicar `backend/sql/01_Estructura_exportada_Workbench.sql`, `02_Restricciones_CHECK.sql`, `03_Aplicacion_operativa.sql` y `04_Minimos_inventario.sql`, en ese orden. Las migraciones son de una sola aplicación y seleccionan `pos_integral_ga6`.
2. Configurar `POS_DB_URL`, `POS_DB_USER` y `POS_DB_PASSWORD` para esa instancia y `POS_HTTP_PORT=8093`. La cuenta de aplicación necesita operaciones sobre los datos, sin permisos para crear o alterar tablas. Una URL de referencia es `jdbc:mysql://127.0.0.1:33192/pos_integral_ga6?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`.
3. Si se necesitan datos sintéticos, habilitar `POS_DEMO_ENABLED=true` y definir localmente `POS_DEMO_PASSWORD` con una contraseña propia. La inicialización de demostración está desactivada por defecto.
4. Construir y ejecutar el backend:

   ```sh
   cd backend
   mvn verify
   java -jar target/pos-integral-api-1.1.0-integracion92.jar
   ```

5. En otra terminal, instalar y ejecutar el cliente:

   ```sh
   cd frontend
   npm ci
   npm test
   npm run build
   npm run dev
   ```

El proxy `/api` conserva el mismo origen para sesión y CSRF. Las variables del segundo paso deben estar presentes al iniciar la API. Para revisar la construcción estática se utiliza `npm run preview -- --host 127.0.0.1 --port 8092`. Una exposición remota requiere HTTPS y `POS_COOKIE_SECURE=true`; el entorno descrito aquí utiliza loopback.

## Reglas de la integración

El esquema vigente tiene 26 tablas, 167 columnas incluidas tres virtuales, 26 claves primarias, 23 claves únicas, 42 claves foráneas, 148 CHECK y tres vistas. La identidad del negocio se conserva mediante `tenant_id`. SQL03 incorpora el medio de pago y SQL04 incorpora el mínimo por existencia.

Preparar una venta valida la disponibilidad conjunta y conserva precio e IVA sin consumir inventario. Confirmar el pago vuelve a validar caja, precio, importe y demanda; guarda pago, venta pagada, saldos, kardex, evento e idempotencia en una transacción. Un reintento equivalente devuelve el mismo comprobante.

El producto DIRECTO descuenta sus unidades. El producto RECETA descuenta sus insumos según cantidad, versión y rendimiento; no descuenta además el plato. Un mínimo igual a cero desactiva la alerta; un mínimo positivo alerta cuando el saldo es menor o igual al umbral.

Nómina utiliza novedades autorizadas seleccionadas explícitamente y evita volver a aplicarlas. La base del período se expresa como novedad DEVENGO. Contabilidad procesa posteriormente el evento de venta o nómina, deriva importes del origen y registra líneas equilibradas. Una venta o liquidación admite como máximo un evento de su tipo y un asiento.

El comprobante es académico; el alcance no integra facturación electrónica DIAN ni una pasarela bancaria. La nómina corresponde a conceptos e importes configurados.

## Pruebas

El corte conserva 92 pruebas unitarias JUnit con dobles y 18 pruebas del cliente. Se ejecutan con `mvn verify` y `npm test`. Las pruebas HTTP de `backend/pruebas` utilizan una instancia MySQL y configuración sintética separada; consultar los argumentos de cada script antes de ejecutarlas.

Los resultados HTTP, las postcondiciones y el recorrido de interfaz documentados en las evidencias 92 y 93 corresponden a sus registros de integración. Compilar o ejecutar unitarias no equivale a repetir esas verificaciones. Las pruebas de Postman91 pertenecen al corte anterior de la API90.

## Relación con las evidencias anteriores

Las carpetas 81, 82, 83, 87, 88, 90 y 91 conservan módulos y pruebas de etapas anteriores. Esta carpeta incorpora el cliente conectado y el refinamiento de mínimos. El prototipo público de [Vercel](https://sena-pos-prototipo-ga5-3388966-2026.vercel.app) utiliza HTML/CSS/JavaScript y datos en memoria; su publicación permite revisar la interacción de GA5 y corresponde a un artefacto distinto del entorno React/API/MySQL descrito aquí.
