# Adaptadores de catálogo de perfumería

Módulos académicos de Esencias & Estilo Solano sobre el mismo modelo MySQL de 28 tablas del proyecto integral.

- [81: Java y JDBC](81_Catalogo_JDBC/README.md): CRUD de artículos con Connection, PreparedStatement y ResultSet.
- [82: Servlet y JSP](82_Catalogo_Servlet_JSP/README.md): formularios HTML, GET/POST, sesión, CSRF y control del perfil en el servidor.
- [Aplicación integral React/API V2](../Perfumeria_Esencias_Estilo_Solano/README.md): instalación, cuentas y operación del sistema.

Cada módulo conserva artículos, unidades, funciones y capacidad nominal; su edición se limita al nombre y al mínimo. Las referencias impiden el borrado de un artículo utilizado. El precio y el suministro versionado pertenecen a la oferta comercial.

Requieren JDK21, Maven y MySQL8.4; cada README explica la configuración. No contienen cuentas, claves ni datos reales del negocio. Los directorios de compilación se generan localmente.
