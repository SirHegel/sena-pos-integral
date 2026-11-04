# Servicios de identidad y catálogo

Base local: `http://127.0.0.1:8090/api/v1`. JSON UTF-8. Los ejemplos usan únicamente datos sintéticos. Este bloque es una parte de la API integral; las operaciones B y C se documentan por separado.

## Sesión y permisos

1. Consultar `GET /auth/csrf` conservando la cookie de sesión. Respuesta `{"csrfToken":"..."}`.
2. Enviar `POST /auth/ingreso`, cabecera `X-CSRF-Token`, cookie y cuerpo `{"tenantId":"UUID del negocio","correo":"administrador@demo.example","password":"clave de demostración configurada"}`. La respuesta devuelve `actor` y un token CSRF nuevo. El identificador de sesión cambia.
3. En las operaciones siguientes conservar la cookie y enviar el token nuevo en todas las escrituras. El servidor relee usuario, negocio y roles; no acepta un tenant/rol del cliente como autorización.
4. `POST /auth/salir` invalida la sesión. Expiración por inactividad: 20 minutos.

Cookies: HttpOnly, SameSite=Strict; Secure es configurable para HTTPS. La prueba local usa exclusivamente loopback HTTP; no equivale a un despliegue público con TLS. No hay CORS abierto. Cada controlador declara roles permitidos, con denegación si falta la declaración. GET no modifica datos de negocio.

Roles: ADMINISTRADOR, CAJERO, INVENTARIO, NOMINA, CONTABILIDAD. La asignación sólo la realiza ADMINISTRADOR dentro de su negocio. Un usuario desactivado pierde acceso en su próxima solicitud. No se puede retirar o desactivar el último administrador activo.

El ingreso conserva el bloqueo persistente del caso 88: BCrypt de costo 12, máximo tres fallos antes de 30 segundos de bloqueo, actualización transaccional del contador y consulta bloqueante del usuario. No se retornan contraseñas, hashes ni detalles SQL. La API integral no incluye registro público ni permite elegir un rol al iniciar sesión.

## Catálogo de endpoints A

| Método y ruta | Permiso | Entrada y resultado |
|---|---|---|
| GET /auth/csrf | Público | Abre sesión y entrega token;200. |
| POST /auth/ingreso | Público con CSRF | tenantId, correo, password; actor+token;200. Datos inválidos400, acceso401, bloqueo429. |
| GET /auth/sesion | Todos los roles reconocidos | Actor vigente del servidor;200, sesión ausente401. |
| POST /auth/salir | Todos | Invalida cookie en servidor;200. |
| GET /usuarios | ADMINISTRADOR | Usuarios del tenant con roles y estado;200. Excluye hash. |
| POST /usuarios | ADMINISTRADOR | correo, password, roles (lista no vacía reconocida);201. Correo duplicado409. |
| PUT /usuarios/{id} | ADMINISTRADOR | activo booleano y roles;200. Recurso ajeno404; último administrador409. |
| GET /roles | ADMINISTRADOR | Identificadores y nombres de roles del negocio;200. |
| GET /productos?q=texto | ADMINISTRADOR,CAJERO,INVENTARIO | Búsqueda literal en código/nombre;200. Sin q lista el catálogo del negocio. |
| GET /productos/{id} | ADMINISTRADOR,CAJERO,INVENTARIO | Producto propio200; ajeno/inexistente404. |
| POST /productos | ADMINISTRADOR,INVENTARIO | Producto completo;201. Código duplicado409. |
| PUT /productos/{id} | ADMINISTRADOR,INVENTARIO | Producto completo;200. Referencia ajena404; cambio estructural de producto usado409. |

Producto: `codigo` 1–64, `nombre` 1–128, `precioVigente` decimal positivo con máximo 12 enteros y 2 decimales, `tasaIva` 0–1 máximo 4 decimales, `tipo` VENDIBLE/INSUMO/PLATO, `unidad` 1–32, `modoDescuento` DIRECTO/RECETA y `activo` booleano. RECETA exige PLATO. No se cambia tipo, unidad o modalidad después de referenciar el producto en stock, venta o receta. La desactivación conserva su historia. El precio positivo es una regla de aplicación más estricta que el CHECK base >=0.

Respuestas de producto usan los nombres físicos: producto_id,codigo,nombre,precio_vigente,tasa_iva,tipo,unidad,modo_descuento,activo. Las cantidades monetarias proceden de BigDecimal. Los campos JSON desconocidos se rechazan. Los errores tienen `codigo` y `mensaje`; CSRF ausente/inválido o permiso insuficiente403. Un error de integridad devuelve409 sin revelar la sentencia.

## Preparación reproducible

Java 21, Maven, MySQL 8.4. Importar los SQL originales01/02 y la migración operativa03 antes de arrancar. Configurar POS_DB_URL, POS_DB_USER, POS_DB_PASSWORD como variables del entorno; no hay credenciales de conexión predeterminadas en el proyecto. `mvn verify` compila; `java -jar target/pos-integral-api-1.0.0.jar` arranca en 127.0.0.1:8090. La conexión no crea ni altera tablas automáticamente.

El inicializador de demostración está desactivado por defecto. Sólo en una base de laboratorio vacía se habilita POS_DEMO_ENABLED=true y se proporciona POS_DEMO_PASSWORD (clave sintética válida). Crea DEMO1 y DEMO2 con UUID 90000000-0000-4000-8000-000000000001/2, cinco roles y cinco cuentas por negocio: administrador,cajero,inventario,nomina,contabilidad en el dominio reservado demo.example. Sus contraseñas se almacenan como hashes BCrypt con sales diferentes. No hay información personal o laboral real en esas semillas. Volver a arrancar no sustituye contraseñas existentes.

## Verificaciones registradas

Las comprobaciones del módulo A se conservan en A_resultados_http.json: 42 de 42 aprobadas. El expediente integral 91 registra 180 solicitudes y 314 aserciones como un instrumento separado.
