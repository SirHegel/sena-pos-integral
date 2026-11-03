# POS Integral - evidencias de programación

Jhon Steven Alvarez Ruiz · ADSO · SENA · Ficha 3388966.

Este repositorio reúne las evidencias académicas de programación del proyecto POS Integral. Cada carpeta conserva su código, instrucciones y comprobaciones específicas.

| Evidencia | Proyecto | Alcance |
|---|---|---|
| GA7 AA2 EV01 | [81 JDBC](81_Modulo_catalogo_JDBC/) | CRUD de catálogo con JDBC explícito |
| GA7 AA2 EV02 | [82 Servlets/JSP](82_Modulo_web_Servlets_JSP/) | Formularios web, GET/POST y JDBC |
| GA7 AA3 EV01 | [83 Spring](83_Modulo_catalogo_Spring/) | Catálogo con SpringMVC y persistencia JPA |
| GA7 AA4 EV03 | [87 React](87_Frontend_React/) | Interfaz de 19 vistas y siete áreas con datos sintéticos en memoria |
| GA7 AA5 EV01 | [88 API](88_Servicio_registro_login_Java/) | Registro e ingreso, BCrypt y bloqueo temporal |
| GA7 AA5 EV03 | [90 API Integral](90_API_POS_integral/) | 62 operaciones REST, siete áreas, sesión y persistencia MySQL |
| GA7 AA5 EV04 | [91 Pruebas de API](91_Pruebas_API_integral/) | Colección Postman, 180 solicitudes y 314 aserciones aprobadas, documentación y video |

Los proyectos 81, 82, 83 y 88 son módulos independientes. El componente 87 demuestra la interfaz React; sus datos se reinician al recargar y no está conectado a la API 90. El expediente 91 comprueba los servicios de 90 y conserva sus registros originales. Seguir las instrucciones de cada carpeta para instalar dependencias y ejecutar el proyecto elegido.

La API 90 utiliza el diseño físico de 26 tablas y 42 claves foráneas; la migración operativa añade el medio de pago y lleva el esquema a 147 restricciones CHECK. Implementa identidad, catálogo, ventas, inventario, gastronomía y caja, nómina, contabilidad y reportes. Los controles de negocio, sesión, permisos y transacciones se ejecutan en el servidor.

Los datos de prueba son sintéticos. Los servicios se configuran para la interfaz local y las credenciales se reciben mediante variables de entorno. El repositorio aloja código y evidencias de pruebas; las instrucciones de ejecución se encuentran dentro de cada proyecto.
