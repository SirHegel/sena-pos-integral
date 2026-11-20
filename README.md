# POS Integral - evidencias de programación

Jhon Steven Alvarez Ruiz · ADSO · SENA · Ficha 3388966.

La adaptación vigente del proyecto para perfumería se encuentra en [Perfumeria_Esencias_Estilo_Solano](Perfumeria_Esencias_Estilo_Solano/). Integra React, API Java 21 y MySQL para preparados, recargas, sellados y decants, con los perfiles Administrador y Vendedora. Incluye ventas de contado atómicas, crédito y abonos, insumos, caja, incidencias, gastos y reportes Excel.

Este corte contiene 62 archivos fuente del contrato PS-API-V2-r3: esquema de 28 tablas y 46 claves foráneas, código y pruebas. Se verificaron 44 pruebas unitarias (21 del corte inicial y 23 adicionales); las 428 comprobaciones HTTP/MySQL corresponden a integración y se contabilizan por separado. Las instrucciones de instalación están en el README y en `documentacion/OPERACION_TECNICA.md` de esa carpeta. Los binarios se generan localmente y no se versionan aquí.

Se publican código y escenarios DEMO. El esquema carga únicamente cuatro unidades estándar; los escenarios crean personas y operaciones ficticias en una instalación de laboratorio separada. Las contraseñas se reciben de forma privada al ejecutar. Los resultados técnicos no acreditan aceptación del negocio, capacitación ni operación productiva.

## Cortes académicos anteriores

Las carpetas siguientes conservan íntegros los ejercicios y sus comprobaciones de cada etapa. El corte integrado anterior está en [92_Integracion_POS](92_Integracion_POS/), con 19 vistas y siete áreas. Su gastronomía, nómina y contabilidad didáctica corresponden a ese diseño histórico y no forman parte del alcance vigente de perfumería.

| Evidencia | Proyecto | Alcance |
|---|---|---|
| GA7 AA2 EV01 | [81 JDBC](81_Modulo_catalogo_JDBC/) | CRUD de catálogo con JDBC explícito |
| GA7 AA2 EV02 | [82 Servlets/JSP](82_Modulo_web_Servlets_JSP/) | Formularios web, GET/POST y JDBC |
| GA7 AA3 EV01 | [83 Spring](83_Modulo_catalogo_Spring/) | Catálogo con SpringMVC y persistencia JPA |
| GA7 AA4 EV03 | [87 React](87_Frontend_React/) | Interfaz de 19 vistas y siete áreas con datos sintéticos en memoria |
| GA7 AA5 EV01 | [88 API](88_Servicio_registro_login_Java/) | Registro e ingreso, BCrypt y bloqueo temporal |
| GA7 AA5 EV03 | [90 API Integral](90_API_POS_integral/) | 62 operaciones REST, siete áreas, sesión y persistencia MySQL |
| GA7 AA5 EV04 | [91 Pruebas de API](91_Pruebas_API_integral/) | Colección Postman, 180 solicitudes y 314 aserciones aprobadas, documentación y video |
| GA8 AA1 EV01 | [92 Integración web](92_Integracion_POS/) | React conectado a la API y MySQL; sesión, permisos y 19 vistas |

Los proyectos 81, 82, 83 y 88 son módulos independientes. El componente 87 demuestra la interfaz React; sus datos se reinician al recargar y no está conectado a la API 90. El expediente 91 comprueba los servicios de 90 y conserva sus registros originales. Seguir las instrucciones de cada carpeta para instalar dependencias y ejecutar el proyecto elegido.

La API 90 conserva el diseño físico de su etapa: 26 tablas, 42 claves foráneas y 147 CHECK después de incorporar el medio de pago. La integración 92 añade el mínimo por existencia mediante la migración SQL04 y alcanza 148 CHECK; conserva 26 tablas, 23 claves únicas, 42 claves foráneas y tres vistas. Incorpora el cliente React que consulta y modifica esos recursos mediante el servidor. Las siete áreas son identidad, catálogo e inventario, ventas, gastronomía/caja, nómina, contabilidad y reportes.

La confirmación de venta registra pago, inventario y evento en una transacción; la contabilidad procesa después ese evento. Negocio y roles proceden de la sesión del servidor. El [prototipo Vercel de GA5](https://sena-pos-prototipo-ga5-3388966-2026.vercel.app) usa datos locales en memoria y se distingue de la integración React/API/MySQL.

Los datos de prueba son sintéticos. Los servicios se configuran para la interfaz local y las credenciales se reciben mediante variables de entorno. El repositorio aloja código y evidencias de pruebas; las instrucciones de ejecución se encuentran dentro de cada proyecto.
