# Operación técnica local — Perfumería 2.0

Este archivo acompaña al manual técnico 121. Los comandos se ejecutan desde la carpeta `software`. Requieren Python 3.10 o superior, Java 21, Maven 3.10.0, Node 22.12.0 o superior, npm y clientes MySQL 8.4 en `PATH`. `JAVA_HOME` debe identificar el JDK 21. La base y la aplicación son locales; el servidor MySQL se instala y administra de forma independiente.

## Configurar el acceso del operador

El operador configura su propio acceso MySQL mediante una pregunta interactiva de contraseña. `ps-admin` es un nombre de configuración local y no es una cuenta incluida en el producto.

```sh
mysql_config_editor set --login-path=ps-admin --host=127.0.0.1 --port=3306 --user=root --password
export PS_MYSQL_LOGIN_PATH=ps-admin
export PS_MYSQL_HOST=127.0.0.1
export PS_MYSQL_PORT=3306
export PS_DB_NAME=perfumeria_ps
python3 documentacion/gestion_tecnica.py verificar
```

El puerto puede cambiarse por el del servicio MySQL instalado. El entorno técnico aislado usa 33193. No se distribuye la contraseña del operador. Para automatización, `PS_MYSQL_DEFAULTS_FILE` puede señalar un archivo `[client]` privado con permisos 0600. Esa opción tiene precedencia sobre el login-path. Las rutas alternativas de los clientes se definen con `PS_MYSQL_BIN` y `PS_MYSQLDUMP_BIN`.

## Instalar, compilar y arrancar

```sh
python3 documentacion/gestion_tecnica.py instalar-bd
python3 documentacion/gestion_tecnica.py crear-usuario --usuario ps_app
python3 documentacion/gestion_tecnica.py compilar
python3 documentacion/gestion_tecnica.py arrancar-api --inicializar-admin
```

`instalar-bd` exige un esquema nuevo y crea las 28 tablas, 46 FK, vistas y cuatro unidades del DDL. No carga clientes, empleados, fórmulas, ventas ni registros de entrevistas. `crear-usuario` solicita una clave privada y concede solo SELECT, INSERT, UPDATE y DELETE en ese esquema. El primer arranque solicita la cuenta MySQL y una clave nueva para el usuario de aplicación `admin`, con un mínimo de 10 caracteres y un máximo de 72 bytes UTF-8. La inicialización de ese Administrador se realiza únicamente cuando `usuario` está vacía. En los arranques posteriores se omite `--inicializar-admin`.

En otra terminal, desde la misma carpeta:

```sh
python3 documentacion/gestion_tecnica.py arrancar-web
```

Abrir `http://127.0.0.1:8094`. La web usa la API 8095 mediante el proxy de Vite. `arrancar-web` sirve la compilación local para operación y comprobación técnica; Vite preview no se presenta como servidor de producción para Internet. Detener cada proceso con Ctrl+C. El servicio MySQL no se detiene con estos comandos.

La configuración opcional de la API usa `PS_DB_URL`, `PS_DB_USER`, `PS_DB_PASSWORD` y `PS_HTTP_PORT`. Si no se proporcionan, el lanzador solicita las credenciales y construye la URL JDBC local con `PS_DB_NAME` y `PS_MYSQL_PORT`. Las claves no deben escribirse en archivos del paquete, argumentos de comandos ni repositorios. El código de aplicación no contiene cuentas productivas.

## Respaldar y restaurar en un esquema nuevo

Detener primero las escrituras de la aplicación. El nombre del archivo identifica el respaldo elegido por el operador; cada archivo se crea una sola vez con permisos 0600.

```sh
python3 documentacion/gestion_tecnica.py respaldar --archivo respaldos/cierre.sql
python3 documentacion/gestion_tecnica.py restaurar --archivo respaldos/cierre.sql --destino perfumeria_recuperada
```

Se genera `cierre.sql.json` con SHA256, tamaño y conteos por tabla. La restauración exige ese manifiesto, verifica la huella, rechaza el esquema original o uno existente y compara tablas, FK y filas al finalizar. No elimina la base de origen ni cambia la conexión de la API. El operador revisa también saldos e importes en la base restaurada antes de elegirla como destino de la aplicación.

El respaldo no incorpora los binarios, la configuración privada, las claves ni los documentos externos referenciados. Esos elementos se conservan por separado y con acceso restringido. La cuenta del operador de respaldo debe poder consultar las tablas y vistas; la restauración requiere privilegios de creación de esquema, tablas y vistas. La cuenta `ps_app` no cumple esa función.
