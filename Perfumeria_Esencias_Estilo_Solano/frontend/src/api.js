export class ErrorApi extends Error {
  constructor(status, data) {
    super(data?.mensaje || `No se pudo completar la operación (${status}).`);
    this.status = status;
    this.codigo = data?.codigo;
  }
}

export function crearCliente({
  fetchImpl = globalThis.fetch,
  onSesionPerdida = () => {},
} = {}) {
  let csrf = "";
  async function request(
    path,
    { method = "GET", body, download = false, signal } = {},
  ) {
    const headers = {
      Accept: download
        ? "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        : "application/json",
    };
    if (body !== undefined) headers["Content-Type"] = "application/json";
    if (method !== "GET") headers["X-CSRF-Token"] = csrf;
    const response = await fetchImpl("/api/v2" + path, {
      method,
      credentials: "same-origin",
      headers,
      signal,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (!response.ok) {
      let data = {};
      try {
        data = await response.json();
      } catch {}
      if (response.status === 401) {
        csrf = "";
        onSesionPerdida();
      }
      throw new ErrorApi(response.status, data);
    }
    if (download)
      return { blob: await response.blob(), filename: "informe-mensual.xlsx" };
    if (response.status === 204) return null;
    const data = await response.json();
    if (data && typeof data.csrf === "string") csrf = data.csrf;
    return data;
  }
  return {
    get: (path, signal) => request(path, { signal }),
    post: (path, body) => request(path, { method: "POST", body }),
    put: (path, body) => request(path, { method: "PUT", body }),
    download: (path) => request(path, { download: true }),
    session: () => request("/auth/sesion"),
    async login(body) {
      if (!csrf) await request("/auth/sesion");
      return request("/auth/login", { method: "POST", body });
    },
    async logout() {
      try {
        return await request("/auth/logout", { method: "POST", body: {} });
      } finally {
        csrf = "";
      }
    },
  };
}

// A retry of the same command must reuse its key after a network interruption.
export function comandoIdempotente(
  cache,
  namespace,
  body,
  uuid = () => crypto.randomUUID(),
) {
  const fingerprint = JSON.stringify(body);
  const old = cache.get(namespace);
  if (old && old.fingerprint !== fingerprint)
    throw new Error(
      "Hay una operación sin confirmar con otros datos. Consulte el registro antes de continuar.",
    );
  const item = old || { fingerprint, key: uuid() };
  cache.set(namespace, item);
  return { ...body, clave_operacion: item.key };
}

// Session-scoped persistence keeps a retry stable across an accidental reload.
export function cacheOperaciones(storage, key = "solano.operaciones.v2") {
  let entries = [];
  try {
    const saved = JSON.parse(storage?.getItem(key) || "[]");
    if (Array.isArray(saved)) entries = saved;
  } catch {}
  const cache = new Map(entries);
  const save = () => {
    try {
      storage?.setItem(key, JSON.stringify([...cache]));
    } catch {}
  };
  return {
    get: (name) => cache.get(name),
    set(name, value) {
      cache.set(name, value);
      save();
    },
    delete(name) {
      const deleted = cache.delete(name);
      save();
      return deleted;
    },
    clear() {
      cache.clear();
      save();
    },
  };
}

export function query(path, values = {}) {
  const entries = Object.entries(values).filter(
    ([, v]) => v !== "" && v != null,
  );
  return path + (entries.length ? "?" + new URLSearchParams(entries) : "");
}
