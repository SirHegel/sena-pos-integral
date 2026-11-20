# Catálogo web con Servlet y JSP

**Actividad GA7-220501096-AA2-EV02 · Esencias & Estilo Solano**  
Jhon Steven Alvarez Ruiz · Ficha 3388966 · 17 de noviembre de 2026

Adaptador web académico con formularios HTML, Servlet explícito y páginas JSP. Comparte el DAO JDBC de la actividad anterior. No sustituye ni cambia el contrato de la aplicación React/API V2.

## Compilación y uso

Después de configurar la conexión, ejecute:

```bash
mvn package
java -jar target/catalogo-web-2.0.0.war
```

Abra http://127.0.0.1:9182/catalogo e ingrese con una cuenta activa de la instalación local. La aplicación solo escucha en loopback. `CATALOGO_PORT` permite elegir otro puerto local. No incorpora cuentas ni claves predeterminadas.

## Flujo HTTP y permisos

| Método y vista | Comportamiento | Perfil |
| --- | --- | --- |
| GET `/catalogo` | Acceso personal o consulta y búsqueda | Usuario activo |
| GET `/catalogo?modo=nuevo` | Formulario de creación | Administrador |
| GET `/catalogo?modo=editar&id=...` | Formulario de nombre y mínimo | Administrador |
| POST `accion=login` | Comprueba credenciales y rota sesión y CSRF | Cuenta activa |
| POST `accion=crear` | Inserta artículo validado | Administrador |
| POST `accion=actualizar` | Cambia nombre y mínimo | Administrador |
| POST `accion=eliminar` | Borra solo un artículo sin referencias | Administrador |
| POST `accion=logout` | Invalida la sesión | Usuario autenticado |

GET nunca escribe. POST requiere un token CSRF ligado a la sesión, incluido en el formulario. Tras una escritura correcta devuelve `303` para consultar el resultado sin repetirla al recargar. Los errores muestran `400` por datos inválidos, `401` por ausencia de identidad en una escritura, `403` por confirmación de sesión o permiso inválido, `404` por identificador inexistente, `409` por código duplicado o artículo referenciado y `503` por indisponibilidad de datos. La consulta sin sesión muestra el formulario de acceso.

`CatalogoServlet` implementa `doGet` y `doPost`. Las vistas bajo `WEB-INF/jsp` usan JSTL y `c:out` para escapar el texto. La sesión guarda un identificador de usuario; cada petición vuelve a consultar si la cuenta y su integrante continúan activos y cuál es su perfil. Enviar `perfil=ADMINISTRADOR` desde el navegador no cambia permisos. Vendedora consulta sin controles de escritura y tampoco puede forzar una escritura por HTTP. La cookie es `HttpOnly` y `SameSite=Strict`; la salida usa `Cache-Control: no-store`, CSP y `nosniff`.

## Alcance del catálogo

El artículo identifica un insumo, envase, accesorio o perfume. No es una oferta de venta: el precio se registra mediante versiones de `precio_oferta`, y el costo y el saldo proceden de los movimientos de inventario. Este módulo no modifica esas cantidades ni registra ventas.

La creación fija código, nombre, unidad base, clase, función material, capacidad nominal cuando corresponde y mínimo. La actualización modifica **nombre y mínimo**; conserva código, unidad, clase, función y capacidad. La eliminación física solo procede si el artículo no tiene referencias. Las claves foráneas con `ON DELETE RESTRICT` conservan las fórmulas, suministros, conteos y movimientos existentes.

`ML`, `UNIDAD` y `G` son unidades base. El litro es una unidad de ingreso: el esquema conserva el factor exacto `1 L = 1000 ML`. No existe conversión entre gramos y mililitros sin una medida o densidad aplicable. Los costos desconocidos permanecen `NULL`; no se sustituyen por cero.

## Estructura y persistencia

- `Articulo`: metadatos y validaciones de texto, cantidades, unidades y funciones.
- `ArticuloDao`: `INSERT`, `SELECT`, `UPDATE` y `DELETE` con `Connection`, `PreparedStatement` y `ResultSet` de JDBC.
- `ConnectionFactory`: abre una conexión por operación desde variables del entorno. `try-with-resources` cierra conexión, sentencia y resultado.
- `AuthDao` y `Actor`: autentican mediante los hashes BCrypt existentes y comprueban el perfil del usuario y el estado de su integrante del equipo.
- `sql/01_Estructura_perfumeria.sql`: estructura MySQL 8.4 del proyecto, con 28 tablas y 46 claves foráneas. No incluye cuentas, claves ni movimientos de personas reales.

Las sentencias SQL usan parámetros. No se concatena el texto ingresado por una persona en el SQL. Los códigos son únicos; un duplicado no reemplaza al artículo anterior. Los textos admiten hasta 160 caracteres sin controles y las cantidades tienen hasta seis decimales, dentro de `DECIMAL(18,6)`.

## Configuración local

Requisitos: JDK 21, Maven 3.10 y MySQL 8.4. Cree un esquema de laboratorio vacío y cargue `sql/01_Estructura_perfumeria.sql` con su operador MySQL. Use cuentas activas de la instalación del proyecto; este adaptador no crea cuentas ni restablece claves. Si el esquema es nuevo, inicialice antes la aplicación principal según su [README de instalación](https://github.com/SirHegel/sena-pos-integral/tree/main/Perfumeria_Esencias_Estilo_Solano), con `PS_BOOTSTRAP_PASSWORD` definida por el operador: la aplicación principal crea el Administrador inicial; desde ella se pueden dar de alta las demás cuentas. Después, ambos adaptadores acceden a ese mismo esquema de laboratorio. Para acceder a datos reales se requiere una configuración de seguridad y operación propia del entorno de destino.

Defina `DB_URL`, `DB_USER` y `DB_PASSWORD` en el proceso que ejecuta el módulo. `DB_URL` tiene la forma `jdbc:mysql://127.0.0.1:3306/esquema_asignado?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true`. La cuenta de ejecución requiere lectura de `usuario` e `integrante_equipo` y CRUD de `articulo`; la importación del esquema corresponde al operador. No escriba claves en el código, en el repositorio ni en capturas.

## Verificación del módulo

`mvn test` ejecuta 11 pruebas JUnit propias de las reglas compartidas: límite de texto, caracteres especiales, escala y signo decimal, identidad canónica, frasco/capacidad, perfume abierto, edición limitada y perfil Vendedora. Las dos aplicaciones incluyen la misma suite; no representan 22 reglas diferentes. Son independientes de las 44 unitarias y las 428 comprobaciones de integración de la aplicación principal.

El ensayo conjunto de estos dos adaptadores completó 59 controles JDBC/HTTP sobre datos sintéticos en una base separada. Comprobó CRUD, SQL parametrizado, códigos duplicados, borrado protegido, conservación de metadatos, sesión, roles y CSRF; también verificó el ingreso de un litro como 1000 ml y el costo desconocido como `NULL`. Es una ejecución técnica de laboratorio, sin atribuir operación ni aceptación al negocio.

## Versionamiento

Repositorio del proyecto: https://github.com/SirHegel/sena-pos-integral

El paquete contiene las fuentes y un archivo Git bundle para consultar el historial local con `git clone versionamiento/catalogo.bundle catalogo`. Los directorios `target` se generan al compilar y no forman parte de las fuentes.

## Capturas de la aplicación

![82 01 acceso](documentacion/capturas/82_01_acceso.jpg)

![82 03 formulario](documentacion/capturas/82_03_formulario.jpg)

![82 04 articulo creado](documentacion/capturas/82_04_articulo_creado.jpg)

![82 06 actualizado](documentacion/capturas/82_06_actualizado.jpg)

![82 07 eliminado](documentacion/capturas/82_07_eliminado.jpg)

![82 08 codigo Servlet IDE](documentacion/capturas/82_08_codigo_Servlet_IDE.jpg)

![82 09 referencia conservada](documentacion/capturas/82_09_referencia_conservada.jpg)

![82 10 consulta Vendedora](documentacion/capturas/82_10_consulta_Vendedora.jpg)

