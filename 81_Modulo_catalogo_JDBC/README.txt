MÓDULO DE CATÁLOGO CON JDBC — GA7-220501096-AA2-EV01
Jhon Steven Alvarez Ruiz — ficha 3388966

Alcance
CRUD real de producto, perteneciente al catálogo de POS Integral y a la pantalla de productos del diseño. Java 21, JDBC explícito mediante DriverManager, Connection, PreparedStatement y ResultSet, MySQL 8.4. Las tablas siguen el modelo GA6 de 26 entidades. Este módulo acotado no declara implementadas ventas, gastronomía, nómina, contabilidad, autorización ni toda la interfaz de 19 vistas.

Ejecución
1. Importar sql/01_Estructura_exportada_Workbench.sql, sql/02_Restricciones_CHECK.sql y sql/03_Negocios_sinteticos.sql en una base nueva MySQL8.4. No ejecutar sobre datos personales.
2. Definir DB_URL, DB_USER y DB_PASSWORD en el entorno. El usuario de ejecución requiere SELECT,INSERT,UPDATE,DELETE de producto, sin privilegios globales. Ejemplo de URL: jdbc:mysql://127.0.0.1:33117/sena_pos_ga7_auth?connectionTimeZone=UTC
3. Compilar y obtener el controlador: mvn package dependency:copy-dependencies
4. Ejecutar: java -cp "target/classes:target/dependency/*" co.edu.sena.pos.catalogo.CliCatalogo --demo
5. Consultar catálogo: java -cp "target/classes:target/dependency/*" co.edu.sena.pos.catalogo.CliCatalogo --listar UUID_DEL_NEGOCIO
En Windows, el separador del classpath es punto y coma. Las credenciales se suministran por variables, no por archivos versionados.

Diseño y estándar
Producto conserva UUID del negocio y del producto, atributos del catálogo, BigDecimal para importes e IVA. ProductoDao concentra SQL y delimita tenant_id en cada lectura y escritura. ConnectionFactory configura la conexión por entorno. Clases UpperCamelCase, miembros lowerCamelCase, paquetes minúsculos, constantes UPPER_SNAKE_CASE y comentarios para decisiones relevantes (estándar77). try-with-resources cierra conexiones, sentencias y resultados. No se concatenan entradas al SQL.

Reglas
Nombre, código y unidad obligatorios con límites del modelo; precio estrictamente positivo de hasta 12 enteros y 2 decimales; IVA entre0y1 con hasta4decimales; tipos VENDIBLE/INSUMO/PLATO; RECETA requiere PLATO. UNIQUE(tenant_id,codigo) controla duplicados; FK protege referencias. Borrar productos referenciados se rige por RESTRICT del esquema, no por una eliminación en cascada.

Demostración
--demo crea sólo productos sintéticos con identificadores aleatorios en dos negocios de prueba ya existentes, comprueba CRUD/duplicados/decimales/validaciones/aislamiento de claves y retira esos productos al finalizar. El predicado tenant_id evita cruces por clave, pero no representa un sistema de autorización de usuarios: el programa local no expide sesiones ni roles.

Regla comercial de catálogo: precio mayor que cero; el CHECK físico GA6 conserva precio mayor o igual a cero. La aplicación impone la condición comercial más estricta y usa la misma regla en las versiones JDBC, Servlet/JSP y Spring.
