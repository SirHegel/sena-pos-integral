CASO DE REGISTRO E INICIO DE SESIÓN — GA7-220501096-AA5-EV01
Jhon Steven Alvarez Ruiz — ficha 3388966

Alcance
Servicio Java 21 + Spring Boot 4.1.2 + JDBC y MySQL 8.4. Registra una cuenta y verifica credenciales dentro de un negocio. El campo usuario es un correo normalizado. Emplea las tablas tenant y usuario del modelo físico GA6; no sustituye los demás servicios de POS Integral ni expide una sesión para operar ventas.

Preparación
1. Instalar Java 21, Maven 3.6.3 o superior y MySQL 8.4.
2. Importar el esquema GA6 y sus restricciones CHECK en una base nueva. Para la prueba se usa sena_pos_ga7_auth, conservando las 26 tablas.
3. Crear un usuario MySQL restringido con SELECT sobre tenant y SELECT, INSERT, UPDATE sobre usuario. Definir DB_URL, DB_USER y DB_PASSWORD como variables del entorno. No guardar contraseñas en el repositorio.
4. Ejecutar mvn verify y luego java -jar target/auth-caso-1.0.0.jar. Escucha sólo en 127.0.0.1:8088.

Endpoints
POST /api/v1/auth/registro
POST /api/v1/auth/ingreso
Content-Type: application/json
Solicitud: {"tenantId":"11111111-1111-4111-8111-111111111111","usuario":"aprendiz@example.test","password":"ClaveDemo88!"}
Las credenciales del ejemplo son sintéticas; el negocio debe existir y estar activo.
Registro: 201. Inicio correcto: 200 y mensaje Autenticación satisfactoria.
Datos inválidos: 400. Credenciales incorrectas: 401. Duplicado en el mismo negocio: 409. Bloqueo temporal: 429.

Validaciones
UUID canónico; correo ASCII normalizado y hasta 254 caracteres; contraseña de 8 a 64 caracteres y hasta 72 bytes UTF-8, sin controles. BCrypt de coste 12, sal por cuenta, consultas parametrizadas y transacciones. Un usuario puede repetirse en otro negocio; no se mezclan sus credenciales. Tres fallos bloquean 30 segundos; el éxito reinicia el contador. Los mensajes no retornan hash ni contraseña.

Límites
Este caso local comprueba registro y autenticación. No incorpora autorización de roles, emisión de tokens, recuperación de contraseña, verificación por correo ni las operaciones de los otros seis dominios. Esas funciones no se declaran realizadas por este módulo. La prueba audiovisual de la API corresponde a la evidencia AA5-EV02.

