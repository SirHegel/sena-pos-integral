MÓDULO DE CATÁLOGO CON SPRING — GA7-220501096-AA3-EV01
Jhon Steven Alvarez Ruiz — ficha 3388966

Selección: aplicación web Java del proyecto POS Integral. Spring Boot / Spring MVC / Spring Data JPA, frameworks del CF20, con Hibernate y MySQL 8.4. Las vistas JSP conservan el catálogo de82; la persistencia cambia de JDBC explícito a repositorio JPA. Producto del módulo 81 conserva tipos y validaciones. No se declara una aplicación móvil ni toda la implementación de 19 vistas.

Requisitos y ejecución: Java 21, Maven, MySQL 8.4. Importar sql01,02,03 en una base nueva o usar la base sintética de 81/82. Definir DB_URL, DB_USER, DB_PASSWORD en entorno; el usuario requiere CRUD sobre producto. Ejecutar mvn package y java -jar target/catalogo-spring-1.0.0.war. Abrir http://127.0.0.1:8083/catalogo. Hibernate valida el esquema; no crea ni altera tablas (ddl-auto=validate), de modo que las 26 tablas / 42 FK / 146 CHECK permanecen.

Capas: CatalogoController recibe GET/POST, valida CSRF y prepara las JSP; CatalogoService delimita transacciones y reglas de negocio; ProductoRepository consulta y persiste; ProductoEntity/ProductoKey mapean la clave compuesta y tipos físicos. El DTO Producto evita usar importes binarios. Paquetes minúsculos, clases UpperCamelCase, miembros lowerCamelCase y comentarios de decisiones siguen 77.

Reglas: precio comercial mayor que cero, conforme al prototipo GA5; el CHECK físico>=0 continúa más amplio. Todo acceso usa tenant_id y producto_id; los errores no exponen SQL. La edición conserva activo. La demostración permite sólo dos negocios sintéticos y no representa autorización de roles. Se mantiene alcance de catálogo: otros dominios y la API integral no se dan por implementados. No hay campos de fecha en este módulo; no se inventa un campo para documentar una validación ajena al contrato.

