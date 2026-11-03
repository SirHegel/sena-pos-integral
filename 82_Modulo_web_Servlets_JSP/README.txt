MÓDULO WEB CON SERVLETS Y JSP — GA7-220501096-AA2-EV02
Jhon Steven Alvarez Ruiz — ficha 3388966

Alcance: catálogo de productos del proyecto POS Integral, con formularios HTML enviados a un HttpServlet real, doGet/doPost, parámetros de consulta y formulario, y vistas JSP compiladas por Jasper. Reutiliza Producto y ProductoDao del módulo81 con el mismo contrato y MySQL8.4. Es un módulo académico acotado, no declara implementado todo el POS ni sus 19 vistas ni autorización de usuarios.

Ejecución: Java21, Maven, MySQL8.4. Importar SQL del módulo81 o usar la misma base de prueba con los dos negocios sintéticos. Definir DB_URL, DB_USER y DB_PASSWORD; el usuario requiere CRUD sobre producto. Ejecutar mvn package y java -jar target/catalogo-web-1.0.0.war. Abrir http://127.0.0.1:8082/catalogo. El formato WAR se usa porque el soporte JSP de Spring Boot/Tomcat no admite el archivo ejecutable JAR.

Flujos: GET /catalogo?tenant=UUID&q=texto consulta el catálogo; modo=nuevo muestra el formulario; modo=editar&id=UUID recupera un producto del negocio. POST /catalogo recibe accion=crear/actualizar/eliminar, parámetros del producto y token CSRF de sesión. Después redirige con303 a GET. Las JSP se conservan en WEB-INF para evitar acceso directo y emplean directivas page/taglib, EL, c:forEach/c:if y c:out. Las salidas con texto del usuario se escapan. No se escriben cambios mediante GET.

Validaciones: límites de Producto, decimales exactos, UUID, códigos duplicados, claves de negocio, CSRF y conflictos de referencias. Los errores de SQL no se muestran al navegador. El selector de dos negocios es una demostración local explícita; no es un sistema de permisos. Sólo se escucha en loopback. No exponer públicamente este módulo sin implementar autenticación/autorización, configuración HTTPS y despliegue correspondientes.

Diseño: interfaz de catálogo relacionada con el prototipoGA5; capas Servlet→DAO→JDBC→MySQL; vistas JSP separadas del acceso a datos; estándar77 y versionamiento local. Preserva esquemaGA6 de26tablas. El módulo no altera la arquitectura Java del proyecto.

Fuentes: SENA Guía7 AA2-EV02 y CF20; https://docs.spring.io/spring-boot/reference/web/servlet.html ; https://tomcat.apache.org/tomcat-11.0-doc/jasper-howto.html ; https://jakarta.ee/specifications/servlet/6.1/ ; https://jakarta.ee/specifications/pages/4.0/

Regla comercial de catálogo: precio mayor que cero; el CHECK físico GA6 conserva precio mayor o igual a cero. La aplicación impone la condición comercial más estricta y usa la misma regla en las versiones JDBC, Servlet/JSP y Spring.
