# POS Integral — API del proyecto

Jhon Steven Alvarez Ruiz · Ficha 3388966  
GA7-220501096-AA5-EV03 · Diseño y desarrollo de servicios web — proyecto

Monolito modular Java 21/Spring Boot 4.1.2 con persistencia MySQL 8.4. El servicio contiene identidad, catálogo, ventas, inventario, gastronomía/caja, nómina didáctica, contabilidad académica y reportes. Expone contratos REST para las funciones de las 19 vistas del proyecto; la interfaz React 87 es un artefacto separado y su conexión a estos contratos no forma parte de esta carpeta.

## Construcción y ejecución

Requisitos: JDK21, Maven y MySQL 8.4. Importar en una base de laboratorio vacía, en orden:

1. `sql/01_Estructura_exportada_Workbench.sql`.
2. `sql/02_Restricciones_CHECK.sql`.
3. `sql/03_Aplicacion_operativa.sql`.

Los dos primeros archivos son copias byte idénticas de la evidencia 61: 26 tablas, 42 FK, 146 CHECK y 3 vistas. La migración 03 añade únicamente `pago.medio` con valores EFECTIVO/EXTERNO y un CHECK adicional. La aplicación conserva 26 tablas y 42 FK; después de la migración tiene 147 CHECK. El medio se agrega antes de cargar pagos, sin asignar un medio ficticio a registros históricos.

Crear una cuenta de base de datos exclusiva con SELECT,INSERT,UPDATE,DELETE sobre este esquema. La aplicación no necesita permisos de administración del servidor ni de alteración de tablas. Configurar variables de entorno:

```text
POS_DB_URL=jdbc:mysql://127.0.0.1:33119/pos_integral_ga6?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
POS_DB_USER=cuenta_de_aplicacion
POS_DB_PASSWORD=clave_configurada_localmente
POS_HTTP_PORT=8090
```

No se incluyen claves de conexión predeterminadas. MySQL y JDBC deben operar en UTC. El puerto HTTP queda limitado a 127.0.0.1. Para un despliegue HTTPS se requiere configurar el terminador TLS y `POS_COOKIE_SECURE=true`; no se publica un servidor de producción con esta configuración local.

```sh
mvn verify
java -jar target/pos-integral-api-1.0.0.jar
```

`mvn verify` comprueba compilación y empaquetado. Las pruebas HTTP descritas abajo se ejecutan por separado contra el servicio y MySQL reales; no se presentan como pruebas unitarias de Maven.

## Datos de demostración

Opcional, sólo para una base sintética de laboratorio: `POS_DEMO_ENABLED=true` y `POS_DEMO_PASSWORD` con una clave sintética de 8–64 caracteres y máximo 72 bytes UTF-8. Crea dos negocios DEMO1/DEMO2, cinco roles y cinco cuentas por negocio, sin información de personas reales. Los negocios son:

* `90000000-0000-4000-8000-000000000001`.
* `90000000-0000-4000-8000-000000000002`.

En cada uno existen administrador,cajero,inventario,nomina,contabilidad en el dominio reservado `demo.example`; por ejemplo `administrador@demo.example`. Las claves se almacenan mediante BCrypt de costo 12 con sales independientes. Reiniciar no sustituye contraseñas existentes. El inicializador está desactivado por defecto.

## Sesión y autorización

Consultar `GET /api/v1/auth/csrf` conservando la cookie; después enviar `POST /api/v1/auth/ingreso` con tenantId,correo,password y `X-CSRF-Token`. Usar el token nuevo devuelto al ingresar para las siguientes escrituras. La sesión cambia de identificador al autenticar; la cookie es HttpOnly/SameSite=Strict. El servidor deriva negocio/usuario de la sesión y relee roles y actividad en cada solicitud. Un selector del cliente no concede permisos. El contador persistente bloquea 30 segundos después de tres fallos.

Los roles aplicados son ADMINISTRADOR,CAJERO,INVENTARIO,NOMINA y CONTABILIDAD. Las escrituras exigen CSRF y los controladores declaran sus roles; una ruta sin declaración no recibe permiso implícito. Los datos de otro negocio no se revelan. Las consultas SQL usan parámetros y las respuestas excluyen hashes, contraseñas y sentencias SQL.

## Documentación por servicio

* `documentacion/A_Servicios_identidad_catalogo.md`: autenticación, sesión, usuarios/roles y catálogo.
* `documentacion/B_Operacion.md` y `B_Matriz_endpoints.json`: stock, kardex, mesas, cajas, recetas y ventas.
* `documentacion/C_Administracion.md`: empleados/novedades/liquidación, cuentas/asientos/eventos, reportes y CSV.

La confirmación de venta es una transacción local con demanda conjunta de insumos, bloqueos ordenados, comprobación de precio/IVA, stock, pago, kardex, idempotencia y evento de integración. Guardar un borrador valida disponibilidad, pero no reserva ni descuenta; pagar vuelve a validarla. Los reintentos no duplican efectos.

Nómina suma novedades explícitas; no calcula obligaciones laborales o tributarias legales. Contabilidad deriva importes del origen de venta o liquidación y exige equilibrio exacto. El comprobante es académico y no es facturación electrónica DIAN. La confirmación del cajero no implementa una pasarela bancaria ni una conciliación externa. Los reportes usan los días de America/Bogota y las mismas filas autorizadas para sus totales/exportación.

## Pruebas reproducibles

Python 3 y dependencias de `pruebas/requirements.txt`. Preparar fuera del repositorio un JSON privado con POS_HTTP_PORT,POS_DEMO_PASSWORD y, para A, POS_TEST_URL opcional. Nunca adjuntar contraseñas, cookies o tokens a los resultados. La sintaxis exacta de B/C puede consultarse con `--help`.

```sh
POS_TEST_CONFIG=/ruta/configuracion_privada.json POS_TEST_OUTPUT=/ruta/A_resultados.json python3 pruebas/A_seguridad_catalogo.py
python3 pruebas/B_pruebas_http.py --help
python3 pruebas/C_pruebas_http.py --help
```

A prueba sesión, CSRF, roles, aislamiento, cambios de permisos y bloqueo persistente. B prueba operaciones, demanda conjunta, rollback y concurrencia. C prueba consumo único de novedades, asientos/orígenes, reportes, fechas/roles y CSV. Los registros de QA distinguen sus ejecuciones y la revisión integrada final. Ninguna prueba usa datos del aula o credenciales personales.

Repositorio público del módulo: https://github.com/SirHegel/sena-pos-integral/tree/main/90_API_POS_integral

El expediente de pruebas se encuentra en https://github.com/SirHegel/sena-pos-integral/tree/main/91_Pruebas_API_integral
