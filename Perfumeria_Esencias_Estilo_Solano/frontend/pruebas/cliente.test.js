import test from "node:test";
import assert from "node:assert/strict";
import {
  crearCliente,
  ErrorApi,
  comandoIdempotente,
  cacheOperaciones,
  query,
} from "../src/api.js";

const json = (body, status = 200) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
test("el acceso obtiene CSRF y rota el token antes de una operación", async () => {
  const calls = [];
  const responses = [
    json({ autenticado: false, csrf: "antes" }),
    json({
      autenticado: true,
      csrf: "despues",
      actor: { perfil: "VENDEDORA" },
    }),
    json({ id: "venta" }),
  ];
  const api = crearCliente({
    fetchImpl: async (path, options) => {
      calls.push({ path, ...options });
      return responses.shift();
    },
  });
  await api.login({ nombre_acceso: "asesora", clave: "solo-prueba" });
  await api.post("/ventas", { lineas: [] });
  assert.equal(calls[0].path, "/api/v2/auth/sesion");
  assert.equal(calls[1].headers["X-CSRF-Token"], "antes");
  assert.equal(calls[2].headers["X-CSRF-Token"], "despues");
  assert.equal(calls[2].credentials, "same-origin");
});
test("una sesión vencida borra el token y solicita nuevo acceso", async () => {
  let lost = 0;
  const calls = [];
  const responses = [
    json({ csrf: "viejo" }),
    json({ codigo: "SESION", mensaje: "Ingrese de nuevo" }, 401),
    json({ csrf: "nuevo" }),
    json({ autenticado: true }),
  ];
  const api = crearCliente({
    onSesionPerdida: () => lost++,
    fetchImpl: async (p, o) => {
      calls.push({ p, o });
      return responses.shift();
    },
  });
  await api.session();
  await assert.rejects(
    api.get("/ventas"),
    (e) =>
      e instanceof ErrorApi &&
      e.status === 401 &&
      e.message === "Ingrese de nuevo",
  );
  await api.login({ nombre_acceso: "a", clave: "b" });
  assert.equal(lost, 1);
  assert.equal(calls[2].p, "/api/v2/auth/sesion");
  assert.equal(calls[3].o.headers["X-CSRF-Token"], "nuevo");
});
test("un error al exportar se conserva como error y no como archivo Excel", async () => {
  const api = crearCliente({
    fetchImpl: async () =>
      json({ codigo: "PERMISO", mensaje: "Acceso restringido" }, 403),
  });
  await assert.rejects(
    api.download("/exportacion.xlsx?mes=2026-11"),
    (e) => e instanceof ErrorApi && e.codigo === "PERMISO",
  );
});
test("el reintento conserva su clave y bloquea cambiar un comando no confirmado", () => {
  const cache = new Map();
  const body = { importe_cop: "38000", medio: "EFECTIVO" };
  const first = comandoIdempotente(cache, "cobro", body, () => "clave-uno");
  assert.deepEqual(
    comandoIdempotente(cache, "cobro", body, () => "clave-dos"),
    first,
  );
  assert.throws(
    () => comandoIdempotente(cache, "cobro", { ...body, importe_cop: "65000" }),
    /sin confirmar/,
  );
  cache.delete("cobro");
  assert.equal(
    comandoIdempotente(cache, "cobro", body, () => "clave-tres")
      .clave_operacion,
    "clave-tres",
  );
});
test("una recarga de la página conserva el comando pendiente hasta su confirmación", () => {
  const values = new Map();
  const storage = {
    getItem: (k) => values.get(k),
    setItem: (k, v) => values.set(k, v),
  };
  const a = cacheOperaciones(storage);
  comandoIdempotente(a, "venta/emitir", { credito: false }, () => "estable");
  const b = cacheOperaciones(storage);
  assert.equal(
    comandoIdempotente(
      b,
      "venta/emitir",
      { credito: false },
      () => "incorrecta",
    ).clave_operacion,
    "estable",
  );
  b.delete("venta/emitir");
  assert.equal(cacheOperaciones(storage).get("venta/emitir"), undefined);
});
test("la indisponibilidad del almacenamiento mantiene un reintento en memoria", () => {
  const broken = {
    getItem() {
      throw new Error("bloqueado");
    },
    setItem() {
      throw new Error("bloqueado");
    },
  };
  const cache = cacheOperaciones(broken);
  comandoIdempotente(cache, "compra", { documento: "FC-1" }, () => "memoria");
  assert.equal(
    comandoIdempotente(cache, "compra", { documento: "FC-1" }).clave_operacion,
    "memoria",
  );
  cache.clear();
  assert.equal(cache.get("compra"), undefined);
});
test("los filtros codifican nombres y excluyen parámetros vacíos", () => {
  assert.equal(
    query("/reportes", {
      tipo: "ventas",
      desde: "",
      hasta: null,
      referencia: "A&B /",
    }),
    "/reportes?tipo=ventas&referencia=A%26B+%2F",
  );
});
