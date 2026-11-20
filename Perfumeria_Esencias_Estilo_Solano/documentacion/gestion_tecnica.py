#!/usr/bin/env python3
"""Operación técnica local de Perfumería 2.0. No contiene datos ni claves del negocio."""
import argparse
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
from datetime import datetime, timezone

BASE = Path(__file__).resolve().parents[1]
DDL = BASE / "backend/sql/01_Estructura_perfumeria.sql"


def fallo(mensaje):
    raise SystemExit(mensaje)


def identificador(texto):
    if not re.fullmatch(r"[a-z][a-z0-9_]{0,47}", texto):
        fallo("El nombre debe empezar con letra minúscula y contener solo a-z, 0-9 y _. Máximo 48.")
    return texto


def esquema():
    return identificador(os.environ.get("PS_DB_NAME", "perfumeria_ps"))


def ejecutable(nombre, variable=None):
    candidato = os.environ.get(variable, nombre) if variable else nombre
    ruta = shutil.which(candidato)
    if not ruta:
        fallo(f"No se encuentra {nombre}. Configure PATH" + (f" o {variable}." if variable else "."))
    return ruta


def cliente(programa="mysql"):
    ruta = ejecutable(programa, "PS_MYSQL_BIN" if programa == "mysql" else "PS_MYSQLDUMP_BIN")
    args = [ruta]
    # El archivo de opciones es una alternativa para automatización controlada.
    # Debe contener [client], host, port, user y password; nunca se distribuye.
    privado = os.environ.get("PS_MYSQL_DEFAULTS_FILE")
    if privado:
        p = Path(privado).resolve()
        if not p.is_file() or (p.stat().st_mode & 0o077):
            fallo("El archivo privado de opciones debe existir y tener permisos 0600.")
        args.append("--defaults-extra-file=" + str(p))
    else:
        args.append("--login-path=" + os.environ.get("PS_MYSQL_LOGIN_PATH", "ps-admin"))
    args += ["--protocol=TCP", "--host=" + os.environ.get("PS_MYSQL_HOST", "127.0.0.1"),
             "--port=" + str(int(os.environ.get("PS_MYSQL_PORT", "3306")))]
    return args


def sql(sentencia, db=None):
    args = cliente() + ["--batch", "--skip-column-names", "--default-character-set=utf8mb4"]
    if db:
        args.append(db)
    # Se usa stdin: las sentencias y las credenciales no aparecen en argv.
    p = subprocess.run(args, input=sentencia, text=True, capture_output=True)
    if p.returncode:
        fallo("MySQL rechazó la operación. Revise conexión, privilegios y registros del servidor; no se imprimen secretos.")
    return p.stdout.strip()


def inventario(db):
    tablas = sql("SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='" + db +
                 "' AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_NAME;").splitlines()
    fk = int(sql("SELECT COUNT(*) FROM information_schema.REFERENTIAL_CONSTRAINTS WHERE CONSTRAINT_SCHEMA='" + db + "';"))
    filas = {t: int(sql("SELECT COUNT(*) FROM `" + t + "`;", db)) for t in tablas}
    return {"tablas": len(tablas), "claves_foraneas": fk, "filas_por_tabla": filas}


def crear_base(db):
    existe = sql("SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='" + db + "';")
    if existe != "0":
        fallo("El esquema ya existe. Se exige un nombre nuevo; no se elimina ni sobrescribe ninguna base.")
    sql("CREATE DATABASE `" + db + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_as_cs;")


def instalar(args):
    db = esquema()
    crear_base(db)
    ddl = DDL.read_text(encoding="utf-8")
    sql(ddl, db)
    resultado = inventario(db)
    tablas_previstas = len(re.findall(r"^CREATE TABLE\s", ddl, flags=re.M | re.I))
    fk_previstas = len(re.findall(r"\bFOREIGN KEY\s*\(", ddl, flags=re.I))
    if resultado["tablas"] != tablas_previstas or resultado["claves_foraneas"] != fk_previstas:
        fallo("El esquema no coincide con los objetos y referencias del DDL distribuido. Conserve el diagnóstico antes de continuar.")
    print(json.dumps({"esquema": db, **resultado}, ensure_ascii=False, indent=2))


def crear_usuario(args):
    usuario = identificador(args.usuario)
    clave = getpass.getpass("Nueva clave privada de la cuenta MySQL local: ")
    if len(clave) < 16 or clave != getpass.getpass("Repita la clave: "):
        fallo("Se requieren al menos 16 caracteres y confirmación idéntica.")
    # MySQL QUOTE: se impide ambigüedad de escape usando modo NO_BACKSLASH_ESCAPES.
    valor = clave.replace("'", "''")
    db = esquema()
    sql("SET SESSION sql_mode='NO_BACKSLASH_ESCAPES'; CREATE USER '" + usuario +
        "'@'127.0.0.1' IDENTIFIED BY '" + valor + "'; GRANT SELECT,INSERT,UPDATE,DELETE ON `" +
        db + "`.* TO '" + usuario + "'@'127.0.0.1';")
    print("Cuenta local creada con permisos de datos; sin CREATE, DROP ni administración de usuarios.")


def respaldo(args):
    db = esquema()
    salida = Path(args.archivo).resolve()
    if salida.exists() or salida.with_suffix(salida.suffix + ".json").exists():
        fallo("El respaldo de destino ya existe. Elija un nombre nuevo.")
    salida.parent.mkdir(parents=True, exist_ok=True)
    opciones = ["--single-transaction", "--quick", "--no-tablespaces", "--set-gtid-purged=OFF",
                "--default-character-set=utf8mb4", "--skip-add-drop-table", db]
    fd = os.open(salida, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, "wb") as f:
        p = subprocess.run(cliente("mysqldump") + opciones, stdout=f, stderr=subprocess.PIPE)
    if p.returncode:
        salida.unlink(missing_ok=True)
        fallo("El respaldo falló. No se conserva un archivo parcial; revise permisos del operador y registros.")
    sha = hashlib.sha256(salida.read_bytes()).hexdigest()
    datos = {"origen": db, "archivo": salida.name, "sha256": sha, "bytes": salida.stat().st_size,
             "creado_utc": datetime.now(timezone.utc).isoformat(), **inventario(db)}
    meta = salida.with_suffix(salida.suffix + ".json")
    with os.fdopen(os.open(meta, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600), "w") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)
    print("Respaldo y manifiesto SHA256 guardados. Suspenda escrituras durante la comprobación de conteos.")


def restaurar(args):
    origen = Path(args.archivo).resolve()
    meta = json.loads(origen.with_suffix(origen.suffix + ".json").read_text())
    if hashlib.sha256(origen.read_bytes()).hexdigest() != meta["sha256"]:
        fallo("La huella del respaldo no coincide; no se restaura.")
    db = identificador(args.destino)
    if db == esquema() or db == meta["origen"]:
        fallo("La restauración requiere otro nombre de esquema nuevo.")
    crear_base(db)
    sql(origen.read_text(encoding="utf-8"), db)
    actual = inventario(db)
    esperado = {k: meta[k] for k in ("tablas", "claves_foraneas", "filas_por_tabla")}
    if actual != esperado:
        fallo("La restauración terminó, pero sus conteos difieren del manifiesto. No cambie la conexión de la aplicación.")
    print(json.dumps({"destino": db, "verificacion": "conteos y SHA256 coinciden", **actual}, ensure_ascii=False, indent=2))


def compilar(args):
    subprocess.run([ejecutable("mvn"), "clean", "verify"], cwd=BASE / "backend", check=True)
    subprocess.run([ejecutable("npm"), "ci"], cwd=BASE / "frontend", check=True)
    subprocess.run([ejecutable("npm"), "test"], cwd=BASE / "frontend", check=True)
    subprocess.run([ejecutable("npm"), "run", "build"], cwd=BASE / "frontend", check=True)


def arrancar_api(args):
    jar = BASE / "backend/target/perfumeria-api-2.0.0-ps.jar"
    if not jar.is_file():
        fallo("Compile primero: no se encuentra el JAR esperado.")
    env = os.environ.copy()
    env["PS_DB_URL"] = env.get("PS_DB_URL") or ("jdbc:mysql://127.0.0.1:" +
        str(int(env.get("PS_MYSQL_PORT", "3306"))) + "/" + esquema() + "?connectionTimeZone=UTC")
    env["PS_DB_USER"] = env.get("PS_DB_USER") or input("Usuario MySQL de la aplicación: ").strip()
    env["PS_DB_PASSWORD"] = env.get("PS_DB_PASSWORD") or getpass.getpass("Clave privada MySQL: ")
    env.pop("PS_BOOTSTRAP_PASSWORD", None)
    if args.inicializar_admin:
        clave = getpass.getpass("Clave nueva del Administrador inicial (mínimo 10 caracteres): ")
        if len(clave) < 10 or len(clave.encode("utf-8")) > 72 or clave != getpass.getpass("Repita la clave: "):
            fallo("Se requieren al menos 10 caracteres, máximo 72 bytes UTF-8 y confirmación idéntica.")
        env["PS_BOOTSTRAP_PASSWORD"] = clave
    java = ejecutable("java")
    os.execve(java, [java, "-jar", str(jar)], env)


def arrancar_web(args):
    if not (BASE / "frontend/dist/index.html").is_file():
        fallo("Compile primero: no se encuentra frontend/dist/index.html.")
    os.chdir(BASE / "frontend")
    npm = ejecutable("npm")
    os.execv(npm, [npm, "run", "preview"])


def verificar(args):
    for programa, flags in (("java", ["-version"]), ("mvn", ["-version"]), ("node", ["--version"]),
                            ("npm", ["--version"]), ("mysql", ["--version"]), ("mysqldump", ["--version"])):
        ruta = ejecutable(programa, "PS_MYSQL_BIN" if programa == "mysql" else "PS_MYSQLDUMP_BIN" if programa == "mysqldump" else None)
        subprocess.run([ruta] + flags, check=True)
    for p in (DDL, BASE / "backend/pom.xml", BASE / "frontend/package-lock.json"):
        if not p.is_file():
            fallo("Falta un componente del paquete: " + str(p.relative_to(BASE)))
    print("Herramientas y archivos localizados; compare sus versiones con el manual antes de instalar.")


def main():
    p = argparse.ArgumentParser(description=__doc__)
    s = p.add_subparsers(dest="orden", required=True)
    for nombre, funcion in (("verificar", verificar), ("instalar-bd", instalar), ("compilar", compilar),
                             ("arrancar-api", arrancar_api), ("arrancar-web", arrancar_web),
                             ("crear-usuario", crear_usuario), ("respaldar", respaldo), ("restaurar", restaurar)):
        q = s.add_parser(nombre)
        q.set_defaults(funcion=funcion)
        if nombre == "crear-usuario":
            q.add_argument("--usuario", default="ps_app")
        if nombre == "arrancar-api":
            q.add_argument("--inicializar-admin", action="store_true")
        if nombre in ("respaldar", "restaurar"):
            q.add_argument("--archivo", required=True)
        if nombre == "restaurar":
            q.add_argument("--destino", required=True)
    args = p.parse_args()
    args.funcion(args)


if __name__ == "__main__":
    try:
        main()
    except (OSError, subprocess.CalledProcessError, ValueError, KeyError) as e:
        fallo("Operación detenida: " + type(e).__name__ + ". Revise parámetros, herramientas y permisos.")
