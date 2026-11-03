# POS Integral — evidencias de programación

Jhon Steven Alvarez Ruiz · ADSO · SENA · Ficha3388966.

Este repositorio conserva cuatro módulos académicos ejecutados y probados sobre el diseño físico MySQL de26tablas. Cada carpeta contiene su proyecto Maven, instrucciones y pruebas específicas.

| Evidencia | Proyecto | Alcance |
|---|---|---|
| GA7 AA2 EV01 | [81 JDBC](81_Modulo_catalogo_JDBC/) | CRUD de catálogo con JDBC explícito |
| GA7 AA2 EV02 | [82 Servlets/JSP](82_Modulo_web_Servlets_JSP/) | Formularios web, GET/POST y JDBC |
| GA7 AA3 EV01 | [83 Spring](83_Modulo_catalogo_Spring/) | Catálogo con SpringMVC y persistencia JPA |
| GA7 AA5 EV01 | [88 API](88_Servicio_registro_login_Java/) | Registro e ingreso, BCrypt y bloqueo temporal |

Las cuatro carpetas son proyectos independientes. Ejecutar Maven dentro de la carpeta elegida y seguir su README. Los datos de prueba son sintéticos y los servicios escuchan en la interfaz local. Las credenciales de la base se reciben por variables de entorno.

La implementación conserva los contratos del proyecto: negocio, claves compuestas, importes decimales y validaciones. El catálogo y el caso de registro/ingreso son módulos acotados; las otras operaciones de POS Integral se describen en sus artefactos de análisis y diseño.
