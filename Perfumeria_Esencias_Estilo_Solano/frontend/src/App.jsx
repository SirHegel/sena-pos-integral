import React, { useEffect, useId, useMemo, useRef, useState } from "react";
import {
  crearCliente,
  ErrorApi,
  comandoIdempotente,
  cacheOperaciones,
  query,
} from "./api.js";
import { fechaNegocio, fechaHora } from "./fechas.js";

const MEDIOS = ["EFECTIVO", "NEQUI", "DAVIPLATA", "TARJETA"];
const money = (value) =>
  value == null
    ? "—"
    : new Intl.NumberFormat("es-CO", {
        style: "currency",
        currency: "COP",
        maximumFractionDigits: 2,
      }).format(Number(value || 0));
const count = (value) =>
  new Intl.NumberFormat("es-CO", { maximumFractionDigits: 4 }).format(
    Number(value || 0),
  );
const today = () => fechaNegocio(new Date());
const text = (value) => (value == null ? "—" : String(value));
const id = (r, key) => r?.[key] || r?.id;
const title = (r) =>
  r?.nombre_venta ||
  r?.nombre_comercial ||
  r?.nombre_visible ||
  r?.nombre ||
  r?.responsable_nombre ||
  r?.cliente_nombre ||
  r?.codigo ||
  r?.codigo_venta ||
  String(r?.venta_id || r?.turno_id || r?.id || "").slice(0, 8);
const option = (rows, key) =>
  (rows || []).map((r) => ({
    value: id(r, key),
    label: [r.codigo_venta || r.codigo || r.codigo_interno, title(r)]
      .filter(Boolean)
      .filter((v, i, a) => a.indexOf(v) === i)
      .join(" · "),
  }));
const select = (name, label, options, required = true) => ({
  name,
  label,
  type: "select",
  options,
  required,
});
const field = (name, label, type = "text", extra = {}) => ({
  name,
  label,
  type,
  required: true,
  ...extra,
});

function Input({ f, value, onChange, inputId }) {
  const props = {
    id: inputId,
    name: f.name,
    value: value ?? "",
    required: f.required !== false,
    onChange: (e) => onChange(e.target.value),
    disabled: f.disabled,
  };
  if (f.type === "select")
    return (
      <select {...props}>
        <option value="">Seleccione una opción</option>
        {f.options.map((o) => {
          const item = typeof o === "string" ? { value: o, label: o } : o;
          return (
            <option key={item.value} value={item.value}>
              {item.label}
            </option>
          );
        })}
      </select>
    );
  if (f.type === "textarea")
    return <textarea {...props} rows="3" maxLength={f.maxLength || 1000} />;
  return (
    <input
      {...props}
      type={f.type || "text"}
      min={f.min}
      max={f.max}
      step={f.step || (["number"].includes(f.type) ? "any" : undefined)}
      autoComplete={f.autoComplete || "off"}
      maxLength={f.maxLength || 200}
      placeholder={f.placeholder || ""}
    />
  );
}
function Form({
  fields,
  onSubmit,
  submit = "Guardar",
  initial = {},
  busy = false,
  children,
  className = "",
}) {
  const [values, setValues] = useState(initial);
  const formId = useId();
  return (
    <form
      className={"form-grid " + className}
      onSubmit={(e) => {
        e.preventDefault();
        onSubmit(values, () => setValues(initial));
      }}
    >
      {fields.map((f) => (
        <label
          key={f.name}
          className={f.type === "textarea" ? "wide" : ""}
          htmlFor={formId + "-" + f.name}
        >
          <span>
            {f.label}
            {f.required !== false ? " *" : ""}
          </span>
          <Input
            f={f}
            inputId={formId + "-" + f.name}
            value={values[f.name]}
            onChange={(v) => setValues((s) => ({ ...s, [f.name]: v }))}
          />
          {f.help && <small>{f.help}</small>}
        </label>
      ))}
      {children}
      <div className="form-actions wide">
        <button disabled={busy} className="primary" type="submit">
          {busy ? "Procesando…" : submit}
        </button>
      </div>
    </form>
  );
}
function Table({
  rows = [],
  columns,
  actions,
  empty = "Todavía no hay registros.",
}) {
  if (!rows.length) return <p className="empty">{empty}</p>;
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.key}>{c.label}</th>
            ))}
            {actions && <th>Acciones</th>}
          </tr>
        </thead>
        <tbody>
          {rows.map((r, i) => (
            <tr
              key={
                (r.id ||
                  r.incidencia_id ||
                  r.movimiento_id ||
                  r.cobro_id ||
                  r.venta_id ||
                  r.oferta_id ||
                  r.articulo_id ||
                  "fila") +
                ":" +
                i
              }
            >
              {columns.map((c) => (
                <td key={c.key}>
                  {c.render
                    ? c.render(r[c.key], r)
                    : /fecha|_en$/.test(c.key)
                      ? fechaHora(r[c.key])
                      : text(r[c.key])}
                </td>
              ))}
              {actions && <td className="row-actions">{actions(r)}</td>}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
function Section({ title: heading, description, children, action }) {
  return (
    <section className="section">
      <div className="section-heading">
        <div>
          <h2>{heading}</h2>
          {description && <p>{description}</p>}
        </div>
        {action}
      </div>
      {children}
    </section>
  );
}
function DateRange({ value, onChange }) {
  return (
    <div className="date-range">
      <label>
        Desde
        <input
          type="date"
          value={value.desde}
          onChange={(e) => onChange({ ...value, desde: e.target.value })}
        />
      </label>
      <label>
        Hasta
        <input
          type="date"
          value={value.hasta}
          onChange={(e) => onChange({ ...value, hasta: e.target.value })}
        />
      </label>
    </div>
  );
}

const navigation = [
  ["inicio", "Resumen"],
  ["venta", "Nueva venta"],
  ["ventas", "Ventas"],
  ["cartera", "Cartera"],
  ["turnos", "Caja y turnos"],
  ["inventario", "Inventario"],
  ["catalogo", "Catálogo"],
  ["formulas", "Fórmulas"],
  ["recepciones", "Recepción de compras"],
  ["incidencias", "Incidencias"],
  ["gastos", "Gastos"],
  ["reportes", "Reportes"],
  ["usuarios", "Usuarios"],
];
const ADMIN_ONLY = new Set([
  "cartera",
  "catalogo",
  "formulas",
  "recepciones",
  "incidencias",
  "gastos",
  "usuarios",
]);

export default function App() {
  const [session, setSession] = useState(null),
    [checking, setChecking] = useState(true),
    [view, setView] = useState("inicio");
  const [data, setData] = useState({}),
    [error, setError] = useState(""),
    [notice, setNotice] = useState(""),
    [busy, setBusy] = useState(false);
  const cache = useRef(null);
  const sessionEpoch = useRef(0);
  if (!cache.current) cache.current = cacheOperaciones(window.sessionStorage);
  const api = useMemo(
    () =>
      crearCliente({
        onSesionPerdida: () => {
          sessionEpoch.current += 1;
          setSession(null);
          setData({});
          setView("inicio");
        },
      }),
    [],
  );
  const admin = session?.perfil === "ADMINISTRADOR";
  async function refresh(actor = session) {
    if (!actor) return;
    const epoch = sessionEpoch.current;
    const names = [
      "inicio",
      "clientes",
      "ofertas",
      "ventas",
      "turnos",
      "articulos",
      "configuracion",
      ...(actor.perfil === "ADMINISTRADOR"
        ? [
            "cartera",
            "proveedores",
            "fragancias",
            "formulas",
            "recepciones",
            "incidencias",
            "gastos",
            "usuarios",
          ]
        : []),
    ];
    const results = await Promise.allSettled(
      names.map((n) => api.get("/" + n)),
    );
    if (epoch !== sessionEpoch.current) return;
    setData(() => {
      const next = {};
      results.forEach((r, i) => {
        if (r.status === "fulfilled") next[names[i]] = r.value;
      });
      return next;
    });
    const first = results.find(
      (r) => r.status === "rejected" && r.reason?.status !== 401,
    );
    if (first) setError(first.reason.message);
  }
  useEffect(() => {
    let active = true;
    api
      .session()
      .then((s) => {
        if (active && s.autenticado) {
          setSession(s.actor);
          refresh(s.actor);
        }
      })
      .catch((e) => active && setError(e.message))
      .finally(() => active && setChecking(false));
    return () => {
      active = false;
    };
  }, [api]);
  async function task(work, message, after, shouldRefresh = true) {
    setBusy(true);
    setError("");
    setNotice("");
    try {
      const result = await work();
      setNotice(message);
      if (after) await after(result);
      if (shouldRefresh) await refresh();
      return result;
    } catch (e) {
      setError(e.message || "La operación no pudo completarse.");
      return undefined;
    } finally {
      setBusy(false);
    }
  }
  async function postOperation(path, body, namespace = path) {
    const scope = session.usuario_id + ":" + namespace;
    const command = comandoIdempotente(cache.current, scope, body);
    try {
      const result = await api.post(path, command);
      cache.current.delete(scope);
      return result;
    } catch (e) {
      if (e instanceof ErrorApi && e.status < 500) cache.current.delete(scope);
      throw e;
    }
  }
  const ctx = {
    api,
    admin,
    data,
    session,
    busy,
    task,
    postOperation,
    refresh,
    setView,
  };
  if (checking)
    return (
      <main className="startup" aria-busy="true">
        Abriendo el punto de venta…
      </main>
    );
  if (!session)
    return (
      <main className="login-shell">
        <div className="login-story">
          <p className="eyebrow">ESENCIAS & ESTILO SOLANO</p>
          <h1>
            Una venta.
            <br />
            Cada mililitro registrado.
          </h1>
          <p>
            Preparaciones, inventario, cartera y caja para el trabajo de
            mostrador.
          </p>
          <div className="brand-detail">Punto de venta · Neiva, Huila</div>
        </div>
        <section className="login-form">
          <h2>Ingresar</h2>
          <p>
            Utilice su cuenta personal para registrar las operaciones de su
            turno.
          </p>
          {error && (
            <div role="alert" className="alert error">
              {error}
            </div>
          )}
          <Form
            busy={busy}
            fields={[
              field("nombre_acceso", "Usuario", "text", {
                autoComplete: "username",
              }),
              field("clave", "Contraseña", "password", {
                autoComplete: "current-password",
              }),
            ]}
            submit="Entrar al sistema"
            onSubmit={(v) =>
              task(
                () => api.login(v),
                "Sesión iniciada.",
                (s) => {
                  sessionEpoch.current += 1;
                  setData({});
                  setSession(s.actor);
                  setView("inicio");
                  return refresh(s.actor);
                },
              )
            }
          />
        </section>
      </main>
    );
  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a
          className="brand"
          href="#inicio"
          onClick={(e) => {
            e.preventDefault();
            setView("inicio");
          }}
        >
          <strong>
            Esencias
            <br />& Estilo Solano
          </strong>
          <span>Punto de venta</span>
        </a>
        <nav aria-label="Principal">
          {navigation
            .filter(([v]) => admin || !ADMIN_ONLY.has(v))
            .map(([key, label]) => (
              <button
                key={key}
                aria-current={view === key ? "page" : undefined}
                onClick={() => {
                  setView(key);
                  setError("");
                  setNotice("");
                }}
              >
                {label}
              </button>
            ))}
        </nav>
        <div className="session">
          <strong>{session.nombre}</strong>
          <span>{admin ? "Administrador" : "Vendedora"}</span>
          <button
            onClick={() =>
              task(
                () => api.logout(),
                "Sesión cerrada.",
                () => {
                  sessionEpoch.current += 1;
                  setSession(null);
                  setData({});
                  cache.current.clear();
                },
                false,
              )
            }
          >
            Cerrar sesión
          </button>
        </div>
      </aside>
      <main className="workspace">
        <header className="topbar">
          <div>
            <p className="eyebrow">OPERACIÓN DEL NEGOCIO</p>
            <h1>{navigation.find(([v]) => v === view)?.[1]}</h1>
          </div>
          <button
            className="secondary"
            disabled={busy}
            onClick={() => task(() => refresh(), "Información actualizada.")}
          >
            Actualizar
          </button>
        </header>
        {error && (
          <div className="alert error" role="alert">
            {error}
          </div>
        )}
        {notice && (
          <div className="alert success" role="status">
            {notice}
          </div>
        )}
        <div aria-busy={busy}>
          {view === "inicio" && <Home {...ctx} />}
          {view === "venta" && <NewSale {...ctx} />}
          {view === "ventas" && <Sales {...ctx} />}
          {view === "cartera" && <Sales {...ctx} cartera />}
          {view === "inventario" && <Inventory {...ctx} />}
          {view === "catalogo" && <Catalog {...ctx} />}
          {view === "formulas" && <FormulaPage {...ctx} />}
          {view === "recepciones" && <Receipts {...ctx} />}
          {view === "incidencias" && <Incidents {...ctx} />}
          {view === "turnos" && <Shifts {...ctx} />}
          {view === "gastos" && <Expenses {...ctx} />}
          {view === "reportes" && <Reports {...ctx} />}
          {view === "usuarios" && <Users {...ctx} />}
        </div>
        <footer className="app-footer">
          Las operaciones quedan vinculadas a su usuario y a sus registros de
          origen.
        </footer>
      </main>
    </div>
  );
}

function Home({ data, admin, setView }) {
  const stock = Array.isArray(data.articulos) ? data.articulos : [];
  const sales = (data.ventas || []).filter(
    (r) =>
      fechaNegocio(r.fecha_emision || r.emitida_en) === today() &&
      r.estado === "EMITIDA",
  );
  const debt = (data.cartera || []).reduce(
    (a, r) => a + Number(r.saldo_cop || 0),
    0,
  );
  const alerts = stock.filter(
    (r) =>
      r.alerta === true ||
      r.alerta === 1 ||
      (Number(r.saldo_base) <= Number(r.minimo_base) &&
        Number(r.minimo_base) > 0),
  );
  return (
    <>
      <div className="metrics">
        <div>
          <span>Ventas de hoy</span>
          <strong>{sales.length}</strong>
          <small>Operaciones emitidas</small>
        </div>
        <div>
          <span>Alertas de inventario</span>
          <strong>{alerts.length}</strong>
          <small>Artículos en su mínimo o por debajo</small>
        </div>
        {admin ? (
          <div>
            <span>Cartera pendiente</span>
            <strong>{money(debt)}</strong>
            <small>Créditos con saldo</small>
          </div>
        ) : (
          <div>
            <span>Mis turnos abiertos</span>
            <strong>
              {(data.turnos || []).filter((t) => t.estado === "ABIERTO").length}
            </strong>
            <small>Turnos disponibles para registrar cobros</small>
          </div>
        )}
      </div>
      <Section
        title="Atender en mostrador"
        description="Seleccione la presentación y conserve el vínculo entre la venta, sus consumos y sus cobros."
        action={
          <button className="primary" onClick={() => setView("venta")}>
            Nueva venta
          </button>
        }
      >
        <div className="workflow">
          <div>
            <b>01</b>
            <h3>Preparar la venta</h3>
            <p>
              Producto terminado, preparación, recarga o decant con su
              presentación y precio.
            </p>
          </div>
          <div>
            <b>02</b>
            <h3>Registrar la entrega</h3>
            <p>
              El sistema fija la composición utilizada y descuenta las
              cantidades correspondientes.
            </p>
          </div>
          <div>
            <b>03</b>
            <h3>Registrar el cobro</h3>
            <p>
              El contado cobra al entregar. Cada abono de crédito queda
              vinculado al medio y al turno que recibe el dinero.
            </p>
          </div>
        </div>
      </Section>
      <Section title="Artículos por reponer">
        <Table
          rows={alerts}
          columns={[
            { key: "codigo", label: "Código" },
            { key: "nombre", label: "Artículo" },
            { key: "saldo_base", label: "Disponible", render: count },
            { key: "unidad_base", label: "Unidad" },
            { key: "minimo_base", label: "Mínimo", render: count },
          ]}
          empty="No hay alertas de mínimo en este momento."
        />
      </Section>
    </>
  );
}

function SaleDetail({ sale, load, ...ctx }) {
  const { api, admin, data, task, busy, postOperation } = ctx;
  const sid = id(sale, "venta_id");
  const open = (data.turnos || []).filter((t) => t.estado === "ABIERTO");
  async function reload() {
    await load(sid);
  }
  return (
    <Section
      title={"Venta " + String(sid).slice(0, 8)}
      description={sale.estado + " · " + (sale.estado_pago || "")}
    >
      <div className="totals">
        <span>
          Total <strong>{money(sale.total_cop)}</strong>
        </span>
        <span>
          Cobrado <strong>{money(sale.cobrado_cop)}</strong>
        </span>
        <span>
          Saldo <strong>{money(sale.saldo_cop)}</strong>
        </span>
      </div>
      <Table
        rows={sale.lineas || []}
        columns={[
          {
            key: "nombre_venta",
            label: "Producto",
            render: (v, r) => v || r.descripcion || r.codigo_venta,
          },
          { key: "unidades", label: "Cantidad", render: count },
          { key: "precio_unitario_cop", label: "Precio", render: money },
          { key: "importe_cop", label: "Importe", render: money },
        ]}
      />
      {sale.estado === "BORRADOR" && (
        <>
          <h3>Entrega de la venta</h3>
          <p>
            El contado confirma el cobro y la entrega juntos. El crédito
            autorizado conserva el saldo para sus abonos.
          </p>
          {admin && (
            <Form
              key={"discount" + sid}
              busy={busy}
              initial={{ importe_cop: "0" }}
              fields={[
                field("importe_cop", "Descuento autorizado (COP)", "number", {
                  min: "0",
                  step: "0.01",
                }),
                field("motivo", "Motivo del descuento"),
              ]}
              submit="Autorizar descuento"
              onSubmit={(v) =>
                task(
                  () => api.post("/ventas/" + sid + "/descuento", v),
                  "Descuento autorizado.",
                  reload,
                )
              }
            />
          )}
          <EmissionForm {...ctx} sale={sale} reload={reload} />
        </>
      )}
      {sale.estado !== "BORRADOR" && Number(sale.saldo_cop) > 0 && (
        <>
          <h3>Registrar cobro o abono</h3>
          <Form
            key={"payment" + sid}
            busy={busy}
            fields={[
              select(
                "turno_id",
                "Turno que recibe el dinero",
                option(open, "turno_id"),
              ),
              select("medio", "Medio de cobro", MEDIOS),
              field("importe_cop", "Importe recibido (COP)", "number", {
                min: "0.01",
                step: "0.01",
                max: String(sale.saldo_cop),
              }),
              field(
                "referencia_externa",
                "Referencia de transferencia o tarjeta",
                "text",
                {
                  required: false,
                  help: "En medios electrónicos, confirme el ingreso y registre la referencia del comprobante.",
                },
              ),
            ]}
            submit="Confirmar dinero recibido"
            onSubmit={(v) =>
              task(
                () =>
                  postOperation("/ventas/" + sid + "/cobros", {
                    ...v,
                    confirmado: true,
                  }),
                "Cobro registrado.",
                reload,
              )
            }
          />
        </>
      )}
      <h3>Cobros de esta venta</h3>
      <Table
        rows={sale.cobros || []}
        columns={[
          { key: "fecha_cobro", label: "Fecha" },
          { key: "medio", label: "Medio" },
          { key: "importe_cop", label: "Importe", render: money },
          { key: "referencia_externa", label: "Referencia" },
        ]}
        empty="Esta venta aún no tiene cobros."
      />
      {sale.estado !== "BORRADOR" && (
        <button
          className="secondary"
          onClick={() =>
            task(async () => {
              const r = await api.get("/ventas/" + sid + "/comprobante");
              const event = new CustomEvent("mostrar-comprobante", {
                detail: r,
              });
              window.dispatchEvent(event);
            }, "Comprobante preparado.")
          }
        >
          Ver comprobante
        </button>
      )}
    </Section>
  );
}

function EmissionForm({
  sale,
  reload,
  admin,
  data,
  task,
  busy,
  postOperation,
}) {
  const [mode, setMode] = useState("false");
  const sid = id(sale, "venta_id");
  const open = (data.turnos || []).filter((t) => t.estado === "ABIERTO");
  const credit = admin && mode === "true";
  const fields = credit
    ? [
        field("fecha_vencimiento", "Vencimiento del crédito", "date"),
        field("condicion_pactada", "Condición pactada de pago", "textarea"),
      ]
    : [
        select(
          "turno_id",
          "Turno que recibe el contado",
          option(open, "turno_id"),
        ),
        select("medio", "Medio de cobro", MEDIOS),
        field("referencia_externa", "Referencia electrónica", "text", {
          required: false,
          help: "Para Nequi, Daviplata o tarjeta, verifique el ingreso y registre su referencia.",
        }),
      ];
  return (
    <div className="emission">
      {admin && (
        <label className="sale-condition">
          Condición de venta
          <select value={mode} onChange={(e) => setMode(e.target.value)}>
            <option value="false">Contado</option>
            <option value="true">Crédito autorizado por administración</option>
          </select>
        </label>
      )}
      <Form
        key={sid + mode}
        busy={busy}
        fields={fields}
        submit={credit ? "Entregar a crédito" : "Confirmar cobro y entregar"}
        onSubmit={(v) =>
          task(
            () =>
              postOperation(
                "/ventas/" + sid + "/emitir",
                credit
                  ? {
                      credito: true,
                      fecha_vencimiento: v.fecha_vencimiento,
                      condicion_pactada: v.condicion_pactada,
                    }
                  : {
                      credito: false,
                      cobro: {
                        ...v,
                        importe_cop: sale.total_cop,
                        confirmado: true,
                      },
                    },
              ),
            credit
              ? "Venta a crédito registrada."
              : "Cobro y entrega registrados en la misma operación.",
            reload,
          )
        }
      />
    </div>
  );
}
function NewSale(ctx) {
  const { data, task, api, busy } = ctx;
  const [cart, setCart] = useState([]),
    [sale, setSale] = useState(null),
    [client, setClient] = useState("");
  const offers = data.ofertas || [];
  const load = async (sid) => setSale(await api.get("/ventas/" + sid));
  return (
    <>
      <Section
        title="Productos de la venta"
        description="Elija una oferta vigente. Su fórmula y empaque se conservarán en la operación."
      >
        <Form
          busy={busy}
          initial={{ unidades: "1" }}
          fields={[
            select(
              "especificacion_id",
              "Producto y presentación",
              offers.map((o) => ({
                value: o.especificacion_id,
                label:
                  title(o) +
                  " · " +
                  money(o.precio_cop) +
                  " · Disponible: " +
                  count(o.disponible_unidades),
              })),
            ),
            field("unidades", "Unidades", "number", { min: "1", step: "1" }),
          ]}
          submit="Añadir producto"
          onSubmit={(v) => {
            const o = offers.find(
              (r) => r.especificacion_id === v.especificacion_id,
            );
            if (o)
              setCart((s) => [
                ...s,
                {
                  ...v,
                  nombre_venta: o.nombre_venta,
                  precio_cop: o.precio_cop,
                },
              ]);
          }}
        />
        <Table
          rows={cart}
          columns={[
            { key: "nombre_venta", label: "Producto" },
            { key: "unidades", label: "Cantidad" },
            { key: "precio_cop", label: "Precio de lista", render: money },
          ]}
          actions={(r) => (
            <button
              className="text-button"
              onClick={() => setCart((s) => s.filter((x) => x !== r))}
            >
              Quitar
            </button>
          )}
        />
        <div className="form-grid sale-client">
          <label htmlFor="cliente_venta">
            <span>Cliente de la venta</span>
            <select
              id="cliente_venta"
              value={client}
              onChange={(e) => setClient(e.target.value)}
            >
              <option value="">Venta de contado sin cliente registrado</option>
              {(data.clientes || []).map((c) => (
                <option key={c.cliente_id} value={c.cliente_id}>
                  {c.nombre}
                </option>
              ))}
            </select>
            <small>El crédito requiere un cliente registrado.</small>
          </label>
          <button
            disabled={!cart.length || busy}
            className="primary"
            onClick={() =>
              task(
                () =>
                  api.post("/ventas", {
                    ...(client ? { cliente_id: client } : {}),
                    lineas: cart.map(({ especificacion_id, unidades }) => ({
                      especificacion_id,
                      unidades,
                    })),
                  }),
                "Borrador creado.",
                async (r) => {
                  await load(r.id || r.venta_id);
                  setCart([]);
                },
              )
            }
          >
            Crear venta
          </button>
        </div>
      </Section>
      <Section title="Registrar cliente">
        <Form
          busy={busy}
          fields={[
            field("nombre", "Nombre del cliente"),
            field("contacto_cobranza", "Contacto para cobranza", "text", {
              required: false,
            }),
          ]}
          onSubmit={(v, reset) =>
            task(() => api.post("/clientes", v), "Cliente registrado.", reset)
          }
        />
      </Section>
      {sale && <SaleDetail {...ctx} sale={sale} load={load} />}
      <ReceiptView />
    </>
  );
}
function Sales({ ...ctx }) {
  const { api, data, task, cartera } = ctx;
  const [sale, setSale] = useState(null);
  const load = async (sid) => setSale(await api.get("/ventas/" + sid));
  return (
    <>
      <Section
        title={cartera ? "Créditos con saldo" : "Historial de ventas"}
        description={
          cartera
            ? "Consulte el vencimiento y registre cada abono en el turno que recibe el dinero."
            : "Los borradores, las entregas y los cobros conservan su historial."
        }
      >
        <Table
          rows={(cartera ? data.cartera : data.ventas) || []}
          columns={[
            {
              key: "venta_id",
              label: "Venta",
              render: (v) => String(v).slice(0, 8),
            },
            { key: "cliente_nombre", label: "Cliente" },
            { key: "estado", label: "Estado" },
            { key: "total_cop", label: "Total", render: money },
            { key: "saldo_cop", label: "Saldo", render: money },
            ...(cartera
              ? [{ key: "fecha_vencimiento", label: "Vencimiento" }]
              : []),
          ]}
          actions={(r) => (
            <button
              className="text-button"
              onClick={() => task(() => load(r.venta_id), "Venta consultada.")}
            >
              Abrir
            </button>
          )}
        />
      </Section>
      {sale && <SaleDetail {...ctx} sale={sale} load={load} />}
      <ReceiptView />
    </>
  );
}
function ReceiptView() {
  const [value, setValue] = useState(null);
  useEffect(() => {
    const fn = (e) => setValue(e.detail);
    window.addEventListener("mostrar-comprobante", fn);
    return () => window.removeEventListener("mostrar-comprobante", fn);
  }, []);
  if (!value) return null;
  return (
    <dialog open className="receipt-dialog" aria-label="Comprobante de venta">
      <div className="receipt">
        <h2>Esencias & Estilo Solano</h2>
        <p>Recibo de venta · Neiva, Huila</p>
        <p>
          {String(value.venta_id || value.id).slice(0, 8)} ·{" "}
          {fechaHora(value.fecha_emision || value.emitida_en)}
        </p>
        {(value.lineas || []).map((r, i) => (
          <p key={i}>
            {r.nombre_venta || r.descripcion} × {r.unidades}
            <strong>{money(r.importe_cop)}</strong>
          </p>
        ))}
        <hr />
        <p>
          Total <strong>{money(value.total_cop)}</strong>
        </p>
        <p>
          Cobrado <strong>{money(value.cobrado_cop)}</strong>
        </p>
        <p>
          Saldo <strong>{money(value.saldo_cop)}</strong>
        </p>
      </div>
      <div className="no-print row-actions">
        <button className="primary" onClick={() => window.print()}>
          Imprimir
        </button>
        <button className="secondary" onClick={() => setValue(null)}>
          Cerrar
        </button>
      </div>
    </dialog>
  );
}

function Inventory({ data, api, admin, busy, task }) {
  const [movements, setMovements] = useState([]),
    [filter, setFilter] = useState("");
  const rows = (data.articulos || []).filter((r) =>
    (r.nombre + " " + r.codigo).toLowerCase().includes(filter.toLowerCase()),
  );
  return (
    <>
      <Section
        title="Existencias por artículo"
        description="Las cantidades se muestran en la unidad base de cada artículo."
      >
        <label className="search">
          Buscar artículo
          <input value={filter} onChange={(e) => setFilter(e.target.value)} />
        </label>
        <Table
          rows={rows}
          columns={[
            { key: "codigo", label: "Código" },
            { key: "nombre", label: "Artículo" },
            { key: "clase", label: "Clase" },
            { key: "saldo_base", label: "Disponible", render: count },
            { key: "unidad_base", label: "Unidad" },
            { key: "minimo_base", label: "Mínimo", render: count },
            ...(admin
              ? [
                  {
                    key: "costo_promedio_cop",
                    label: "Costo por unidad base",
                    render: money,
                  },
                ]
              : []),
          ]}
          actions={
            admin
              ? (r) => (
                  <button
                    className="text-button"
                    onClick={() =>
                      task(
                        async () =>
                          setMovements(
                            await api.get(
                              query("/movimientos", {
                                articulo_id: r.articulo_id,
                              }),
                            ),
                          ),
                        "Movimientos consultados.",
                      )
                    }
                  >
                    Movimientos
                  </button>
                )
              : undefined
          }
        />
      </Section>
      {admin && (
        <Section title="Mínimo de reposición">
          <Form
            busy={busy}
            fields={[
              select(
                "articulo_id",
                "Artículo",
                option(data.articulos, "articulo_id"),
              ),
              field("minimo_base", "Mínimo en unidad base", "number", {
                min: "0",
                step: "0.0001",
              }),
            ]}
            onSubmit={(v) =>
              task(
                () =>
                  api.put("/articulos/" + v.articulo_id + "/minimo", {
                    minimo_base: v.minimo_base,
                  }),
                "Mínimo actualizado.",
              )
            }
          />
        </Section>
      )}
      {admin && (
        <Section title="Movimientos del artículo">
          <Table
            rows={movements}
            columns={[
              { key: "fecha", label: "Fecha" },
              { key: "tipo", label: "Origen" },
              { key: "direccion", label: "Dirección" },
              { key: "cantidad_base", label: "Cantidad", render: count },
              ...(admin
                ? [{ key: "costo_unitario_cop", label: "Costo", render: money }]
                : []),
              { key: "motivo", label: "Motivo" },
            ]}
            empty="Seleccione un artículo para consultar sus movimientos."
          />
        </Section>
      )}
    </>
  );
}

function ComponentEditor({
  articles,
  value,
  onChange,
  quantityLabel = "Cantidad en unidad base",
}) {
  return (
    <div className="component-editor wide">
      <h4>Componentes</h4>
      {value.map((c, i) => (
        <div className="component-line" key={i}>
          <label>
            <span>Artículo</span>
            <select
              aria-label={"Artículo componente " + (i + 1)}
              required
              value={c.articulo_id || ""}
              onChange={(e) =>
                onChange(
                  value.map((x, j) =>
                    j === i ? { ...x, articulo_id: e.target.value } : x,
                  ),
                )
              }
            >
              <option value="">Seleccione artículo</option>
              {(articles || []).map((a) => (
                <option key={a.articulo_id} value={a.articulo_id}>
                  {a.nombre} ({a.unidad_base})
                </option>
              ))}
            </select>
          </label>
          <label>
            <span>{quantityLabel}</span>
            <input
              aria-label={"Cantidad componente " + (i + 1)}
              required
              type="number"
              min="0.0001"
              step="0.0001"
              value={c.cantidad_base || ""}
              onChange={(e) =>
                onChange(
                  value.map((x, j) =>
                    j === i ? { ...x, cantidad_base: e.target.value } : x,
                  ),
                )
              }
            />
          </label>
          <button
            type="button"
            className="text-button"
            onClick={() => onChange(value.filter((_, j) => i !== j))}
          >
            Quitar
          </button>
        </div>
      ))}
      <button
        className="secondary"
        type="button"
        onClick={() =>
          onChange([...value, { articulo_id: "", cantidad_base: "" }])
        }
      >
        Añadir componente
      </button>
    </div>
  );
}
function Catalog({ data, api, busy, task }) {
  const [components, setComponents] = useState([]),
    [replacement, setReplacement] = useState([]);
  return (
    <>
      <Section
        title="Ofertas de venta"
        description="Cada oferta une presentación, precio y versión de suministro."
      >
        <Table
          rows={data.ofertas || []}
          columns={[
            { key: "codigo_venta", label: "Código" },
            { key: "nombre_venta", label: "Producto" },
            { key: "modalidad", label: "Modalidad" },
            { key: "volumen_ml", label: "Volumen (ml)", render: count },
            {
              key: "precio_cop",
              label: "Precio",
              render: (v) => (v == null ? "Configurar precio" : money(v)),
            },
            { key: "disponible_unidades", label: "Disponible", render: count },
          ]}
        />
      </Section>
      <Section title="Nueva oferta">
        <Form
          busy={busy}
          fields={[
            field("codigo_venta", "Código de venta"),
            field("nombre_venta", "Nombre de la oferta"),
            select("modalidad", "Modalidad", [
              "TERMINADO",
              "PREPARACION",
              "RECARGA",
              "DECANT",
              "ADICIONAL",
            ]),
            select(
              "fragancia_id",
              "Fragancia",
              option(data.fragancias, "fragancia_id"),
              false,
            ),
            field("volumen_ml", "Volumen vendido (ml)", "number", {
              min: "0.0001",
              step: "0.0001",
              required: false,
            }),
            select(
              "formula_id",
              "Fórmula líquida",
              (data.formulas || []).map((f) => ({
                value: f.formula_id,
                label:
                  (f.nombre_comercial || f.fragancia_nombre || "Fórmula") +
                  " · " +
                  f.variante +
                  " · " +
                  f.volumen_preparado_ml +
                  " ml · v" +
                  f.version,
              })),
              false,
            ),
            field("precio_cop", "Precio de venta (COP)", "number", {
              min: "0",
              step: "0.01",
              required: false,
            }),
          ]}
          submit="Crear oferta y suministro"
          onSubmit={(v, reset) =>
            task(
              () =>
                api.post("/ofertas", {
                  ...Object.fromEntries(
                    Object.entries(v).filter(([, x]) => x !== ""),
                  ),
                  componentes: components,
                }),
              "Oferta creada.",
              () => {
                reset();
                setComponents([]);
              },
            )
          }
        >
          <ComponentEditor
            articles={data.articulos}
            value={components}
            onChange={setComponents}
          />
          <p className="form-help wide">
            Para preparaciones seleccione la fórmula y añada el empaque. Para
            recargas, configure únicamente los accesorios que utilice. Para
            decants seleccione el perfume abierto y su atomizador completo.
          </p>
        </Form>
      </Section>
      <Section title="Cambiar precio">
        <Form
          busy={busy}
          fields={[
            select("oferta_id", "Oferta", option(data.ofertas, "oferta_id")),
            field("importe_unitario_cop", "Nuevo precio (COP)", "number", {
              min: "0",
              step: "0.01",
            }),
          ]}
          onSubmit={(v) =>
            task(
              () =>
                api.put("/ofertas/" + v.oferta_id + "/precio", {
                  importe_unitario_cop: v.importe_unitario_cop,
                }),
              "Nueva vigencia de precio registrada.",
            )
          }
        />
      </Section>
      <Section
        title="Nueva versión de suministro"
        description="Los cambios de fórmula o empaque se aplican a nuevas ventas; el historial conserva la versión que utilizó."
      >
        <Form
          busy={busy}
          fields={[
            select(
              "oferta_id",
              "Oferta que cambia",
              option(data.ofertas, "oferta_id"),
            ),
            select(
              "formula_id",
              "Fórmula para la nueva versión",
              (data.formulas || []).map((f) => ({
                value: f.formula_id,
                label:
                  (f.nombre_comercial || f.fragancia_nombre || "Fórmula") +
                  " · " +
                  f.variante +
                  " · " +
                  f.volumen_preparado_ml +
                  " ml · v" +
                  f.version,
              })),
              false,
            ),
          ]}
          submit="Guardar nuevo suministro"
          onSubmit={(v, reset) =>
            task(
              () =>
                api.put("/ofertas/" + v.oferta_id + "/suministro", {
                  ...(v.formula_id ? { formula_id: v.formula_id } : {}),
                  componentes: replacement,
                }),
              "Nueva versión de suministro registrada.",
              () => {
                reset();
                setReplacement([]);
              },
            )
          }
        >
          <ComponentEditor
            articles={data.articulos}
            value={replacement}
            onChange={setReplacement}
          />
        </Form>
      </Section>
      <Section title="Artículos físicos">
        <Form
          busy={busy}
          initial={{ minimo_base: "0" }}
          fields={[
            field("codigo", "Código del artículo"),
            field("nombre", "Nombre"),
            select("unidad_base", "Unidad base", ["ML", "UNIDAD", "G"]),
            select("clase", "Clase", [
              "LIQUIDO",
              "ENVASE",
              "ACCESORIO",
              "PERFUME_SELLADO",
              "PERFUME_ABIERTO",
              "MUESTRA",
            ]),
            select("funcion_material", "Función del material", [
              "ESENCIA",
              "ALCOHOL",
              "FIJADOR",
              "FEROMONAS",
              "FRASCO",
              "VALVULA",
              "TAPA",
              "ETIQUETA",
              "ATOMIZADOR",
              "PERFUME",
              "OTRO",
            ]),
            field(
              "contenido_nominal_ml",
              "Capacidad del frasco o perfume sellado (ml)",
              "number",
              { min: "0.0001", required: false },
            ),
            field("minimo_base", "Mínimo de reposición", "number", {
              min: "0",
            }),
          ]}
          onSubmit={(v, reset) =>
            task(
              () =>
                api.post(
                  "/articulos",
                  Object.fromEntries(
                    Object.entries(v).filter(([, x]) => x !== ""),
                  ),
                ),
              "Artículo creado.",
              reset,
            )
          }
        />
      </Section>
      <Section title="Referencias de fragancias">
        <Form
          busy={busy}
          fields={[
            field("codigo_interno", "Código interno"),
            field("nombre_comercial", "Nombre comercial"),
          ]}
          onSubmit={(v, reset) =>
            task(
              () => api.post("/fragancias", v),
              "Fragancia registrada.",
              reset,
            )
          }
        />
      </Section>
    </>
  );
}
function FormulaPage({ data, api, busy, task }) {
  const [components, setComponents] = useState([]);
  return (
    <>
      <Section
        title="Fórmulas versionadas"
        description="Una versión utilizada en ventas conserva sus cantidades originales."
      >
        <Table
          rows={data.formulas || []}
          columns={[
            {
              key: "nombre_comercial",
              label: "Fragancia",
              render: (v, r) => v || r.fragancia_nombre,
            },
            { key: "variante", label: "Variante" },
            {
              key: "volumen_preparado_ml",
              label: "Preparación (ml)",
              render: count,
            },
            { key: "version", label: "Versión" },
            {
              key: "componentes",
              label: "Composición",
              render: (v) =>
                (v || [])
                  .map(
                    (c) =>
                      (c.nombre || c.articulo_nombre || "Componente") +
                      ": " +
                      count(c.cantidad_ml_por_preparacion) +
                      " ml",
                  )
                  .join(" · "),
            },
          ]}
        />
      </Section>
      <Section title="Registrar nueva versión">
        <Form
          busy={busy}
          fields={[
            select(
              "fragancia_id",
              "Fragancia",
              option(data.fragancias, "fragancia_id"),
            ),
            select("variante", "Variante", [
              "NORMAL",
              "EXTRA_FIJADOR",
              "EXTRA_FEROMONAS",
            ]),
            field("volumen_preparado_ml", "Volumen final (ml)", "number", {
              min: "0.0001",
              step: "0.0001",
            }),
          ]}
          submit="Guardar versión de fórmula"
          onSubmit={(v, reset) =>
            task(
              () =>
                api.post("/formulas", {
                  ...v,
                  componentes: components.map((c) => ({
                    articulo_id: c.articulo_id,
                    cantidad_ml_por_preparacion: c.cantidad_base,
                  })),
                }),
              "Fórmula registrada.",
              () => {
                reset();
                setComponents([]);
              },
            )
          }
        >
          <ComponentEditor
            articles={(data.articulos || []).filter(
              (a) => a.unidad_base === "ML",
            )}
            value={components}
            onChange={setComponents}
            quantityLabel="Cantidad por preparación (ml)"
          />
          <p className="form-help wide">
            La suma de los líquidos debe coincidir con el volumen de la
            presentación. El sistema valida las cantidades antes de guardar.
          </p>
        </Form>
      </Section>
    </>
  );
}

function Receipts({ data, api, busy, task, postOperation }) {
  const [lines, setLines] = useState([]),
    [selected, setSelected] = useState(""),
    [qty, setQty] = useState(""),
    [unit, setUnit] = useState(""),
    [cost, setCost] = useState("");
  return (
    <>
      <Section title="Recepciones de mercancía">
        <Table
          rows={data.recepciones || []}
          columns={[
            { key: "fecha", label: "Fecha" },
            { key: "proveedor_nombre", label: "Proveedor" },
            {
              key: "documento_proveedor",
              label: "Documento",
              render: (v, r) => v || r.referencia_factura,
            },
            { key: "total_cop", label: "Costo", render: money },
          ]}
        />
      </Section>
      <Section title="Registrar compra">
        <div className="component-line">
          <label>
            Artículo
            <select
              value={selected}
              onChange={(e) => setSelected(e.target.value)}
            >
              <option value="">Seleccione artículo</option>
              {option(data.articulos, "articulo_id").map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Cantidad
            <input
              type="number"
              min="0.0001"
              step="0.0001"
              value={qty}
              onChange={(e) => setQty(e.target.value)}
            />
          </label>
          <label>
            Unidad
            <select value={unit} onChange={(e) => setUnit(e.target.value)}>
              <option value="">Unidad</option>
              {["L", "ML", "UNIDAD", "G"].map((u) => (
                <option key={u}>{u}</option>
              ))}
            </select>
          </label>
          <label>
            Costo total (COP)
            <input
              type="number"
              min="0"
              step="0.01"
              value={cost}
              onChange={(e) => setCost(e.target.value)}
            />
          </label>
          <button
            className="secondary"
            disabled={!selected || !(Number(qty) > 0) || !unit || cost === ""}
            onClick={() => {
              setLines((s) => [
                ...s,
                {
                  articulo_id: selected,
                  cantidad: qty,
                  unidad_codigo: unit,
                  costo_total_cop: cost,
                },
              ]);
              setSelected("");
              setQty("");
              setCost("");
            }}
          >
            Añadir renglón
          </button>
        </div>
        <Table
          rows={lines}
          columns={[
            {
              key: "articulo_id",
              label: "Artículo",
              render: (v) =>
                title((data.articulos || []).find((a) => a.articulo_id === v)),
            },
            { key: "cantidad", label: "Cantidad" },
            { key: "unidad_codigo", label: "Unidad" },
            { key: "costo_total_cop", label: "Costo", render: money },
          ]}
          actions={(r) => (
            <button
              className="text-button"
              onClick={() => setLines((s) => s.filter((x) => x !== r))}
            >
              Quitar
            </button>
          )}
        />
        <Form
          busy={busy || !lines.length}
          fields={[
            select(
              "proveedor_id",
              "Proveedor",
              option(data.proveedores, "proveedor_id"),
            ),
            field("documento_proveedor", "Número de factura o documento"),
          ]}
          submit="Confirmar recepción"
          onSubmit={(v, reset) =>
            task(
              () => postOperation("/recepciones", { ...v, lineas: lines }),
              "Recepción registrada con sus costos.",
              () => {
                reset();
                setLines([]);
              },
            )
          }
        />
      </Section>
      <Section
        title="Apertura de botella para decants"
        description="Traslada existencias de una botella sellada al volumen utilizable del perfume abierto y conserva su costo."
      >
        <Form
          busy={busy}
          fields={[
            select(
              "articulo_sellado_id",
              "Perfume sellado",
              option(
                (data.articulos || []).filter(
                  (a) => a.clase === "PERFUME_SELLADO",
                ),
                "articulo_id",
              ),
            ),
            select(
              "articulo_abierto_id",
              "Referencia de perfume abierto",
              option(
                (data.articulos || []).filter(
                  (a) => a.clase === "PERFUME_ABIERTO",
                ),
                "articulo_id",
              ),
            ),
            field("unidades", "Botellas abiertas", "number", {
              min: "1",
              step: "1",
            }),
            field("volumen_util_ml", "Volumen útil total (ml)", "number", {
              min: "0.0001",
              step: "0.0001",
            }),
            field("motivo", "Motivo o referencia de apertura"),
          ]}
          submit="Registrar apertura"
          onSubmit={(v, reset) =>
            task(
              () => postOperation("/aperturas", v),
              "Apertura registrada.",
              reset,
            )
          }
        />
      </Section>
      <Section title="Registrar proveedor">
        <Form
          busy={busy}
          fields={[
            field("nombre", "Nombre del proveedor"),
            field("contacto", "Contacto", "text", { required: false }),
          ]}
          onSubmit={(v, reset) =>
            task(
              () => api.post("/proveedores", v),
              "Proveedor registrado.",
              reset,
            )
          }
        />
      </Section>
    </>
  );
}

function Incidents({ data, busy, task, postOperation }) {
  const [lines, setLines] = useState([]),
    [article, setArticle] = useState(""),
    [qty, setQty] = useState(""),
    [direction, setDirection] = useState("SALIDA"),
    [returnCost, setReturnCost] = useState("");
  return (
    <>
      <Section title="Incidencias registradas">
        <Table
          rows={data.incidencias || []}
          columns={[
            { key: "fecha", label: "Fecha" },
            { key: "tipo", label: "Tipo" },
            {
              key: "motivo",
              label: "Motivo",
              render: (v, r) => v || r.descripcion,
            },
            {
              key: "venta_id",
              label: "Venta",
              render: (v) => (v ? String(v).slice(0, 8) : "—"),
            },
          ]}
        />
      </Section>
      <Section
        title="Merma, muestra, reposición o devolución"
        description="Registre cantidades físicas. Una mezcla cuyos insumos ya se consumieron se documenta sin descontarlos por segunda vez."
      >
        <div className="component-line">
          <label>
            Artículo
            <select
              value={article}
              onChange={(e) => setArticle(e.target.value)}
            >
              <option value="">Seleccione artículo</option>
              {option(data.articulos, "articulo_id").map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </label>
          <label>
            Cantidad en unidad base
            <input
              type="number"
              min="0.0001"
              step="0.0001"
              value={qty}
              onChange={(e) => setQty(e.target.value)}
            />
          </label>
          <label>
            Movimiento
            <select
              value={direction}
              onChange={(e) => setDirection(e.target.value)}
            >
              <option value="SALIDA">Salida física</option>
              <option value="ENTRADA">Entrada utilizable</option>
            </select>
          </label>
          {direction === "ENTRADA" && (
            <label>
              Costo por unidad base recuperada (COP)
              <input
                type="number"
                min="0"
                step="0.00000001"
                value={returnCost}
                onChange={(e) => setReturnCost(e.target.value)}
              />
              <small>
                Con venta y renglón de origen, el sistema puede recuperar el
                costo de esa entrega.
              </small>
            </label>
          )}
          <button
            className="secondary"
            disabled={!article || !(Number(qty) > 0)}
            onClick={() => {
              setLines((s) => [
                ...s,
                {
                  articulo_id: article,
                  cantidad_base: qty,
                  direccion: direction,
                  ...(direction === "ENTRADA" && returnCost !== ""
                    ? { costo_unitario_base_cop: returnCost }
                    : {}),
                },
              ]);
              setArticle("");
              setQty("");
              setReturnCost("");
            }}
          >
            Añadir movimiento
          </button>
        </div>
        <Table
          rows={lines}
          columns={[
            {
              key: "articulo_id",
              label: "Artículo",
              render: (v) =>
                title((data.articulos || []).find((a) => a.articulo_id === v)),
            },
            { key: "cantidad_base", label: "Cantidad" },
            { key: "direccion", label: "Movimiento" },
            {
              key: "costo_unitario_base_cop",
              label: "Costo de entrada",
              render: (v) => (v == null ? "—" : money(v)),
            },
          ]}
          actions={(r) => (
            <button
              className="text-button"
              onClick={() => setLines((s) => s.filter((x) => x !== r))}
            >
              Quitar
            </button>
          )}
        />
        <Form
          busy={busy}
          fields={[
            select("tipo", "Tipo de incidencia", [
              "MERMA",
              "MUESTRA",
              "REPOSICION",
              "DEVOLUCION",
              "ERROR_MEZCLA",
              "CAMBIO_ENVASE",
              "REPOSICION_LIQUIDO",
            ]),
            select(
              "venta_id",
              "Venta de origen (si corresponde)",
              option(data.ventas, "venta_id"),
              false,
            ),
            field("linea_no", "Renglón de venta (si corresponde)", "number", {
              min: "1",
              step: "1",
              required: false,
            }),
            field("motivo", "Descripción y motivo", "textarea"),
            field("devolucion_cop", "Dinero devuelto (COP)", "number", {
              min: "0",
              step: "0.01",
              required: false,
            }),
            select(
              "turno_id",
              "Turno de devolución de dinero",
              option(
                (data.turnos || []).filter((t) => t.estado === "ABIERTO"),
                "turno_id",
              ),
              false,
            ),
          ]}
          submit="Registrar incidencia"
          onSubmit={(v, reset) =>
            task(
              () =>
                postOperation("/incidencias", {
                  ...Object.fromEntries(
                    Object.entries(v).filter(([, x]) => x !== ""),
                  ),
                  lineas: lines,
                }),
              "Incidencia registrada.",
              () => {
                reset();
                setLines([]);
              },
            )
          }
        />
      </Section>
    </>
  );
}
function Shifts({ api, data, busy, task }) {
  const [detail, setDetail] = useState(null),
    [values, setValues] = useState(
      Object.fromEntries(MEDIOS.map((m) => [m, "0"])),
    ),
    [observation, setObservation] = useState("");
  return (
    <>
      <Section title="Turnos de caja">
        <Table
          rows={data.turnos || []}
          columns={[
            {
              key: "turno_id",
              label: "Turno",
              render: (v) => String(v).slice(0, 8),
            },
            { key: "responsable_nombre", label: "Responsable" },
            { key: "fecha_apertura", label: "Apertura" },
            { key: "estado", label: "Estado" },
            { key: "fondo_cop", label: "Fondo inicial", render: money },
          ]}
          actions={(r) => (
            <button
              className="text-button"
              onClick={() =>
                task(
                  async () => setDetail(await api.get("/turnos/" + r.turno_id)),
                  "Turno consultado.",
                )
              }
            >
              Consultar
            </button>
          )}
        />
      </Section>
      <Section title="Abrir mi turno">
        <Form
          busy={busy}
          initial={{ fondo_cop: "0" }}
          fields={[
            field("fondo_cop", "Efectivo de apertura (COP)", "number", {
              min: "0",
              step: "0.01",
            }),
          ]}
          submit="Abrir turno"
          onSubmit={(v) => task(() => api.post("/turnos", v), "Turno abierto.")}
        />
      </Section>
      {detail && (
        <Section
          title={
            "Conciliación del turno " +
            String(detail.turno_id || detail.id).slice(0, 8)
          }
        >
          <div className="totals">
            <span>
              Efectivo esperado
              <strong>{money(detail.esperado_efectivo_cop)}</strong>
            </span>
          </div>
          <Table
            rows={detail.cobros || []}
            columns={[
              { key: "medio", label: "Medio" },
              { key: "importe_cop", label: "Cobro", render: money },
              { key: "referencia_externa", label: "Referencia" },
            ]}
          />
          {detail.estado === "ABIERTO" && (
            <form
              className="form-grid"
              onSubmit={(e) => {
                e.preventDefault();
                task(
                  () =>
                    api.post(
                      "/turnos/" + (detail.turno_id || detail.id) + "/cerrar",
                      {
                        conciliaciones: MEDIOS.map((m) => ({
                          medio: m,
                          importe_verificado_cop: values[m],
                        })),
                        observacion: observation,
                      },
                    ),
                  "Cierre registrado.",
                  async (r) => setDetail(r),
                );
              }}
            >
              {MEDIOS.map((m) => (
                <label key={m}>
                  {m === "EFECTIVO"
                    ? "Efectivo contado"
                    : "Total verificado en " + m}
                  <input
                    required
                    min="0"
                    step="0.01"
                    type="number"
                    value={values[m]}
                    onChange={(e) =>
                      setValues((s) => ({ ...s, [m]: e.target.value }))
                    }
                  />
                </label>
              ))}
              <label className="wide">
                Observación del cierre
                <textarea
                  value={observation}
                  onChange={(e) => setObservation(e.target.value)}
                  rows="3"
                />
              </label>
              <button className="primary" disabled={busy}>
                Registrar conciliación y cerrar
              </button>
            </form>
          )}
          {detail.estado === "CERRADO" && (
            <Table
              rows={detail.conciliaciones || []}
              columns={[
                { key: "medio", label: "Medio" },
                {
                  key: "importe_esperado_cop",
                  label: "Esperado",
                  render: money,
                },
                {
                  key: "importe_verificado_cop",
                  label: "Verificado",
                  render: money,
                },
                { key: "diferencia_cop", label: "Diferencia", render: money },
              ]}
            />
          )}
        </Section>
      )}
    </>
  );
}
function Expenses({ data, busy, task, postOperation }) {
  return (
    <>
      <Section title="Gastos del negocio">
        <Table
          rows={data.gastos || []}
          columns={[
            { key: "fecha", label: "Fecha" },
            { key: "concepto", label: "Concepto" },
            { key: "importe_cop", label: "Importe", render: money },
            { key: "medio", label: "Medio" },
            { key: "documento_ref", label: "Soporte" },
          ]}
        />
      </Section>
      <Section title="Registrar gasto">
        <Form
          busy={busy}
          initial={{ fecha: today() }}
          fields={[
            field("fecha", "Fecha", "date"),
            field("concepto", "Concepto"),
            field("importe_cop", "Importe (COP)", "number", {
              min: "0.01",
              step: "0.01",
            }),
            field("documento_ref", "Referencia del soporte"),
            select("medio", "Medio de pago", MEDIOS),
            select(
              "turno_id",
              "Turno (si sale de caja)",
              option(
                (data.turnos || []).filter((t) => t.estado === "ABIERTO"),
                "turno_id",
              ),
              false,
            ),
          ]}
          onSubmit={(v, reset) =>
            task(
              () =>
                postOperation(
                  "/gastos",
                  Object.fromEntries(
                    Object.entries(v).filter(([, x]) => x !== ""),
                  ),
                ),
              "Gasto registrado.",
              reset,
            )
          }
        />
      </Section>
    </>
  );
}
function Reports({ api, admin, busy, task }) {
  const [type, setType] = useState("ventas"),
    [range, setRange] = useState({
      desde: today().slice(0, 8) + "01",
      hasta: today(),
    }),
    [rows, setRows] = useState([]),
    [month, setMonth] = useState(today().slice(0, 7));
  const labels = {
    ventas: "Ventas",
    medios: "Cobros por medio",
    rotacion: "Fragancias y presentaciones",
    costos: "Costos y margen bruto",
    vendedoras: "Ventas por asesora",
    gastos: "Gastos",
    devoluciones: "Devoluciones y reintegros",
  };
  const reportFields = {
    ventas: [
      ["numero_recibo", "Recibo"],
      ["emitida_en", "Fecha"],
      ["vendedora", "Responsable"],
      ["cliente", "Cliente"],
      ["bruto_cop", "Subtotal"],
      ["descuento_cop", "Descuento"],
      ["total_cop", "Total"],
      ["cobrado_cop", "Cobrado"],
      ["saldo_cop", "Saldo"],
      ["devoluciones_acumuladas_cop", "Devuelto"],
      ["ingreso_ajustado_cop", "Venta ajustada"],
    ],
    medios: [
      ["medio", "Medio"],
      ["cobros", "Número de cobros"],
      ["cobrado_cop", "Cobrado"],
    ],
    rotacion: [
      ["codigo_venta", "Código"],
      ["nombre_venta", "Presentación"],
      ["modalidad", "Modalidad"],
      ["volumen_ml", "Volumen (ml)"],
      ["fragancia", "Fragancia"],
      ["unidades_vendidas", "Unidades vendidas"],
      ["bruto_cop", "Subtotal"],
    ],
    costos: [
      ["numero_recibo", "Recibo"],
      ["ingreso_cop", "Venta original"],
      ["costo_materiales_cop", "Materiales originales"],
      ["margen_bruto_cop", "Margen original"],
      ["devoluciones_acumuladas_cop", "Devuelto"],
      ["costo_ajustes_cop", "Ajuste de materiales"],
      ["costo_materiales_ajustado_cop", "Materiales ajustados"],
      ["margen_bruto_ajustado_cop", "Margen ajustado"],
    ],
    vendedoras: [
      ["nombre_visible", "Responsable"],
      ["nombre_acceso", "Usuario"],
      ["perfil", "Perfil"],
      ["ventas", "Ventas"],
      ["bruto_cop", "Subtotal"],
      ["descuento_cop", "Descuentos"],
      ["vendido_cop", "Vendido"],
      ["cobrado_cop", "Cobrado"],
    ],
    gastos: [
      ["fecha", "Fecha"],
      ["concepto", "Concepto"],
      ["importe_cop", "Importe"],
      ["medio", "Medio"],
      ["soporte_ref", "Soporte"],
    ],
    devoluciones: [
      ["registrado_en", "Fecha"],
      ["numero_recibo", "Recibo"],
      ["devolucion_cop", "Dinero devuelto"],
      ["motivo", "Motivo"],
    ],
  };
  const columns = (reportFields[type] || []).map(([key, label]) => ({
    key,
    label,
    render: (value) =>
      key.endsWith("_cop")
        ? money(value)
        : /_en$|^fecha$/.test(key)
          ? fechaHora(value)
          : key === "numero_recibo"
            ? text(value).slice(0, 11)
            : text(value),
  }));
  return (
    <>
      <Section
        title="Consultar reportes"
        description="Las ventas, los cobros y los gastos mantienen sus fechas y conceptos propios."
      >
        <div className="report-controls">
          <label>
            Reporte
            <select
              value={type}
              onChange={(e) => {
                setType(e.target.value);
                setRows([]);
              }}
            >
              {Object.entries(labels)
                .filter(([k]) => admin || k === "ventas")
                .map(([k, v]) => (
                  <option key={k} value={k}>
                    {v}
                  </option>
                ))}
            </select>
          </label>
          <DateRange
            value={range}
            onChange={(value) => {
              setRange(value);
              setRows([]);
            }}
          />
          <button
            className="primary"
            disabled={busy}
            onClick={() =>
              task(async () => {
                const r = await api.get(
                  query("/reportes", { tipo: type, ...range }),
                );
                setRows(r.filas || []);
              }, "Reporte consultado.")
            }
          >
            Consultar
          </button>
        </div>
        <Table
          rows={rows}
          columns={columns}
          empty="Seleccione el reporte y el período para consultar."
        />
      </Section>
      {admin && (
        <Section
          title="Archivo mensual para el contador"
          description="Descargue un libro Excel con ventas, cobros, gastos, cartera y devoluciones."
        >
          <div className="report-controls">
            <label>
              Mes
              <input
                type="month"
                required
                value={month}
                onChange={(e) => setMonth(e.target.value)}
              />
            </label>
            <button
              className="primary"
              disabled={busy}
              onClick={() =>
                task(async () => {
                  const r = await api.download(
                    query("/exportacion.xlsx", { mes: month }),
                  );
                  const url = URL.createObjectURL(r.blob);
                  const a = document.createElement("a");
                  a.href = url;
                  a.download = "Solano-" + month + ".xlsx";
                  document.body.append(a);
                  a.click();
                  a.remove();
                  setTimeout(() => URL.revokeObjectURL(url), 2000);
                }, "Archivo descargado.")
              }
            >
              Descargar Excel
            </button>
          </div>
        </Section>
      )}
    </>
  );
}
function Users({ data, api, busy, task }) {
  return (
    <>
      <Section title="Cuentas personales">
        <Table
          rows={data.usuarios || []}
          columns={[
            { key: "nombre_visible", label: "Nombre" },
            { key: "nombre_acceso", label: "Usuario" },
            { key: "perfil", label: "Perfil" },
            {
              key: "activo",
              label: "Activo",
              render: (v) => (v ? "Sí" : "No"),
            },
          ]}
        />
      </Section>
      <Section title="Registrar cuenta">
        <Form
          busy={busy}
          fields={[
            field("nombre_visible", "Nombre de la persona"),
            field("nombre_acceso", "Nombre de acceso"),
            field("clave", "Contraseña inicial", "password", {
              autoComplete: "new-password",
            }),
            select("perfil", "Perfil", ["ADMINISTRADOR", "VENDEDORA"]),
          ]}
          onSubmit={(v, reset) =>
            task(() => api.post("/usuarios", v), "Cuenta registrada.", reset)
          }
        />
      </Section>
    </>
  );
}
