import React, { useState } from "react";
import {
  Card,
  DataTable,
  Field,
  DemoForm,
  RouteLink,
  RoleMatrix,
} from "./componentes.jsx";
import {
  areas,
  dinero,
  fechaBogota,
  disponibilidad,
  totalVenta,
  efectivoSesion,
  resumenNomina,
  ventasFiltradas,
  csvVentas,
} from "./modelo.js";

const codeOptions = (rows) =>
  rows.map((x) => [x.code, x.code + " · " + x.name]);
const activeOptions = [
  ["ACTIVO", "Activo"],
  ["INACTIVO", "Inactivo"],
];
const dineroInput = (value) => (value / 100).toFixed(2);
const cleanSearch = (value) =>
  value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();

export function Acceso({ act }) {
  const [visible, setVisible] = useState(false);
  const [clave, setClave] = useState("");
  return (
    <Card title="Ingresa a tu negocio" heading="h1">
      <span className="badge">Acceso local de demostración</span>
      <p className="muted">Usa los datos sintéticos del ejemplo.</p>
      <DemoForm
        onSubmit={(data) => {
          act("LOGIN", data, "inicio");
          setClave("");
        }}
        button="Ingresar"
      >
        <Field
          name="negocio"
          label="Negocio"
          options={[["DEMO-A", "Café Demo A"]]}
        />
        <Field
          name="correo"
          label="Correo"
          type="email"
          defaultValue="operador@example.invalid"
          autoComplete="username"
        />
        <Field
          name="clave"
          label="Contraseña"
          type={visible ? "text" : "password"}
          value={clave}
          onChange={(e) => setClave(e.target.value)}
          autoComplete="current-password"
        />
        <button
          type="button"
          className="secondary"
          onClick={() => setVisible(!visible)}
        >
          {visible ? "Ocultar" : "Mostrar"} contraseña
        </button>
      </DemoForm>
      <p className="muted">
        <small>
          operador@example.invalid · demo1234
          <br />
          Bloqueo local de 30 segundos después de tres fallos.
        </small>
      </p>
    </Card>
  );
}
export function Inicio({ state }) {
  const entry = [
    "usuarios",
    "nueva-venta",
    "existencias",
    "mesas",
    "empleados",
    "cuentas",
    "reportes",
  ];
  return (
    <>
      <div className="stats">
        <Card title="Ventas confirmadas">
          <span className="value">{state.sales.length}</span>
          <small>Sesión de demostración</small>
        </Card>
        <Card title="Total cobrado">
          <span className="value">
            {dinero(state.sales.reduce((a, s) => a + s.total, 0))}
          </span>
          <small>COP · datos locales</small>
        </Card>
        <Card title="Caja">
          <span className="value">
            {state.caja.open ? "Abierta" : "Cerrada"}
          </span>
          <small>Fondo {dinero(state.caja.fondo)}</small>
        </Card>
      </div>
      <h2>Áreas del negocio</h2>
      <div className="tiles">
        {areas.map((a, i) => (
          <RouteLink className="tile" to={entry[i]} key={a}>
            <strong>{a}</strong>
            <span>Abrir área</span>
          </RouteLink>
        ))}
      </div>
    </>
  );
}
export function Usuarios({ state, act }) {
  return (
    <div className="grid">
      <Card title="Usuarios del negocio">
        <DataTable
          headers={["Nombre", "Correo", "Rol"]}
          rows={state.users.map((u) => [u.name, u.email, u.role])}
        />
      </Card>
      <Card title="Agregar usuario de ejemplo">
        <DemoForm onSubmit={(d) => act("USER", d)} button="Guardar usuario">
          <Field name="nombre" label="Nombres" maxLength={80} />
          <Field name="apellidos" label="Apellidos" maxLength={80} />
          <Field
            name="documento"
            label="Cédula de ejemplo"
            inputMode="numeric"
            pattern="[0-9]+"
            maxLength={20}
            help="Sólo dígitos; se conserva como texto."
          />
          <Field
            name="nacimiento"
            label="Fecha de nacimiento de ejemplo"
            type="date"
            max={fechaBogota()}
          />
          <Field name="email" label="Correo" type="email" maxLength={254} />
          <Field
            name="rol"
            label="Rol"
            options={[
              "Cajero",
              "Administrador",
              "Inventario",
              "Nómina",
              "Contabilidad",
            ].map((x) => [x, x])}
          />
        </DemoForm>
      </Card>
    </div>
  );
}
export function Roles() {
  return (
    <Card title="Matriz de permisos propuesta">
      <RoleMatrix />
    </Card>
  );
}
export function NuevaVenta({ state, act }) {
  const [query, setQuery] = useState("");
  const filtered = state.products.filter((p) =>
    cleanSearch(p.code + " " + p.name).includes(cleanSearch(query)),
  );
  return (
    <div className="grid">
      <Card title="Agregar producto">
        <Field
          name="buscar"
          label="Buscar por código o nombre"
          type="search"
          required={false}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="CAF001 o café"
        />
        <DemoForm
          key={query}
          onSubmit={(d) => act("CART_ADD", d)}
          button="Agregar a la venta"
          disabled={!filtered.length}
        >
          <Field
            name="producto"
            label="Producto"
            options={filtered.map((p) => [
              p.code,
              `${p.code} · ${p.name} · ${dinero(p.price)} · disponible ${disponibilidad(state, p)}`,
            ])}
          />
          <Field
            name="cantidad"
            label="Cantidad"
            type="number"
            min="1"
            step="1"
            defaultValue="1"
          />
          <p className="muted">
            Mesa: {state.selectedMesa} · Bodega principal · IVA del ejemplo: 0 %
          </p>
        </DemoForm>
      </Card>
      <Card title="Detalle de la venta">
        <DataTable
          headers={["Producto", "Cantidad", "Subtotal", "Acción"]}
          rows={state.cart.map((l) => ({
            key: l.code,
            cells: [
              l.name,
              l.qty,
              dinero(l.price * l.qty),
              <button
                type="button"
                className="secondary"
                onClick={() => act("CART_REMOVE", { code: l.code })}
                aria-label={"Quitar " + l.name}
              >
                Quitar
              </button>,
            ],
          }))}
        />
        <div className="row total">
          <span>Total</span>
          <span>{dinero(totalVenta(state))}</span>
        </div>
        <div className="actions">
          <RouteLink to="pago" className="button">
            Revisar pago
          </RouteLink>
          <RouteLink to="mesas">Elegir mesa</RouteLink>
        </div>
      </Card>
    </div>
  );
}
export function Pago({ state, act }) {
  return (
    <Card title="Resumen de cobro">
      <DataTable
        headers={["Producto", "Cantidad", "Importe"]}
        rows={state.cart.map((l) => [l.name, l.qty, dinero(l.price * l.qty)])}
      />
      <div className="row total">
        <span>Total</span>
        <span>{dinero(totalVenta(state))}</span>
      </div>
      <p>
        Caja: <strong>{state.caja.open ? "Abierta" : "Cerrada"}</strong> · Mesa:{" "}
        {state.selectedMesa}
      </p>
      <DemoForm
        onSubmit={(d) => act("PAY", d, "comprobante")}
        button="Confirmar pago una vez"
        disabled={!state.cart.length || !state.caja.open}
      >
        <Field
          name="medio"
          label="Medio de pago"
          options={["Efectivo", "Confirmación externa"].map((x) => [x, x])}
        />
        <Field
          name="importe"
          label="Importe confirmado (COP)"
          type="number"
          min="0"
          step="0.01"
          defaultValue={dineroInput(totalVenta(state))}
        />
        <p className="muted">
          Confirmación de demostración por el total exacto; no hay conexión con
          bancos.
        </p>
        <RouteLink to="nueva-venta">Volver al detalle</RouteLink>
      </DemoForm>
    </Card>
  );
}
export function Comprobante({ state }) {
  const s = state.lastSale;
  if (!s)
    return (
      <Card title="Sin comprobante">
        <p>Confirma una venta de demostración para consultar el resultado.</p>
        <RouteLink to="nueva-venta">Crear venta</RouteLink>
      </Card>
    );
  return (
    <Card title="Venta de demostración confirmada">
      <span className="badge">PAGADA</span>
      <h2>{s.invoice}</h2>
      <p>
        Fecha y hora Bogotá: {s.date}
        <br />
        Mesa: {s.mesa}
        <br />
        Medio: {s.method}
      </p>
      <DataTable
        headers={["Producto", "Cantidad", "Precio histórico", "Subtotal"]}
        rows={s.lines.map((l) => [
          l.name,
          l.qty,
          dinero(l.price),
          dinero(l.price * l.qty),
        ])}
      />
      <div className="total">Total {dinero(s.total)}</div>
      <p>
        El inventario de esta factura se descontó una sola vez en esta sesión.
      </p>
      <div className="actions">
        <RouteLink to="movimientos">Ver kardex</RouteLink>
        <RouteLink to="nueva-venta">Nueva venta</RouteLink>
      </div>
    </Card>
  );
}
export function Productos({ state, act }) {
  return (
    <div className="grid">
      <Card title="Catálogo">
        <DataTable
          headers={["Código", "Producto", "Precio", "Modalidad", "Unidad"]}
          rows={state.products.map((p) => [
            p.code,
            p.name,
            dinero(p.price),
            p.mode,
            p.unit,
          ])}
        />
      </Card>
      <Card title="Agregar producto">
        <DemoForm onSubmit={(d) => act("PRODUCT", d)} button="Guardar producto">
          <Field name="codigo" label="Código" maxLength={64} />
          <Field name="nombre" label="Nombre" maxLength={128} />
          <Field
            name="precio"
            label="Precio (COP)"
            type="number"
            min="0.01"
            step="0.01"
            max="999999999999.99"
          />
          <Field
            name="modalidad"
            label="Modalidad"
            options={["DIRECTO", "RECETA"].map((x) => [x, x])}
          />
          <Field
            name="unidad"
            label="Unidad base"
            defaultValue="Unidad"
            maxLength={32}
          />
          <p className="muted">
            Saldo inicial cero. Un preparado requiere composición en Recetas.
          </p>
        </DemoForm>
      </Card>
    </div>
  );
}
export function StockTable({ state, query = "" }) {
  const rows = [
    ...state.products.map((p) => [
      p.code,
      p.name,
      disponibilidad(state, p),
      p.mode === "RECETA" ? "Preparaciones" : p.unit,
    ]),
    ...state.ingredients.map((i) => [i.code, i.name, i.stock / 1000, i.unit]),
  ].filter((r) => cleanSearch(r[0] + " " + r[1]).includes(cleanSearch(query)));
  return (
    <DataTable
      headers={["Código", "Producto o insumo", "Saldo", "Unidad"]}
      rows={rows}
    />
  );
}
export function Existencias({ state }) {
  const [query, setQuery] = useState("");
  return (
    <Card title="Consulta de existencias">
      <Field
        name="bodega"
        label="Bodega"
        options={[["Principal", "Principal"]]}
      />
      <Field
        name="filtro-stock"
        label="Producto o insumo"
        type="search"
        required={false}
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />
      <StockTable state={state} query={query} />
      <p className="muted">
        La disponibilidad por receta depende del insumo limitante. No descuenta
        además un producto terminado.
      </p>
    </Card>
  );
}
export function Movimientos({ state }) {
  const [filter, setFilter] = useState({ desde: "", hasta: "", q: "" });
  let rows = [];
  let error = "";
  if (filter.desde && filter.hasta && filter.desde > filter.hasta)
    error = "La fecha inicial debe ser igual o anterior a la final.";
  else
    rows = state.moves.filter(
      (m) =>
        (!filter.desde || fechaBogota(m.iso) >= filter.desde) &&
        (!filter.hasta || fechaBogota(m.iso) <= filter.hasta) &&
        cleanSearch(m.name).includes(cleanSearch(filter.q)),
    );
  return (
    <Card title="Movimientos de ventas confirmadas">
      <div className="filters">
        <Field
          name="desde-kardex"
          label="Desde"
          type="date"
          required={false}
          value={filter.desde}
          onChange={(e) => setFilter({ ...filter, desde: e.target.value })}
        />
        <Field
          name="hasta-kardex"
          label="Hasta"
          type="date"
          required={false}
          value={filter.hasta}
          onChange={(e) => setFilter({ ...filter, hasta: e.target.value })}
        />
        <Field
          name="producto-kardex"
          label="Producto o insumo"
          type="search"
          required={false}
          value={filter.q}
          onChange={(e) => setFilter({ ...filter, q: e.target.value })}
        />
      </div>
      {error && (
        <p role="alert" className="message error">
          {error}
        </p>
      )}
      <DataTable
        headers={[
          "Fecha y hora Bogotá",
          "Factura",
          "Producto o insumo",
          "Salida",
          "Saldo posterior",
        ]}
        rows={rows.map((m) => [
          m.date,
          m.invoice,
          m.name,
          `${m.qty} ${m.unit}`,
          `${m.balance} ${m.unit}`,
        ])}
      />
    </Card>
  );
}
export function Mesas({ state, act }) {
  return (
    <>
      <div className="tiles">
        {state.mesas.map((m) => (
          <Card key={m.code} title={"Mesa " + m.code}>
            <p className="status">{m.status}</p>
            <button onClick={() => act("MESA", { code: m.code })}>
              Seleccionar {m.code}
            </button>
          </Card>
        ))}
      </div>
      <Card title="Mesa de la siguiente venta">
        <p>
          Selección actual: <strong>{state.selectedMesa}</strong>. La ocupación
          es informativa.
        </p>
        <RouteLink to="nueva-venta">Continuar a venta</RouteLink>
      </Card>
    </>
  );
}
export function Caja({ state, act }) {
  return (
    <Card title="Sesión de caja de ejemplo">
      <span className="badge">{state.caja.open ? "ABIERTA" : "CERRADA"}</span>
      <div className="row">
        <span>Fondo de apertura</span>
        <strong>{dinero(state.caja.fondo)}</strong>
      </div>
      <div className="row">
        <span>Cobros en efectivo de esta sesión</span>
        <strong>{dinero(efectivoSesion(state))}</strong>
      </div>
      <DemoForm
        key={state.caja.id + "-" + state.caja.open}
        onSubmit={(d) => act("CAJA", d)}
        button={
          state.caja.open
            ? "Revisar y cerrar sesión"
            : "Abrir sesión de ejemplo"
        }
      >
        <Field
          name="conteo"
          label="Conteo de ejemplo (COP)"
          type="number"
          min="0"
          step="0.01"
          defaultValue={dineroInput(
            state.caja.open ? state.caja.fondo + efectivoSesion(state) : 0,
          )}
        />
      </DemoForm>
      <p className="muted">
        Los cobros externos no se suman al efectivo. Abrir una sesión nueva
        separa sus cobros de los anteriores.
      </p>
      {state.cashClosed.length > 0 && (
        <DataTable
          headers={["Sesión", "Esperado", "Conteo", "Diferencia"]}
          rows={state.cashClosed.map((s) => [
            s.session,
            dinero(s.expected),
            dinero(s.count),
            dinero(s.difference),
          ])}
        />
      )}
    </Card>
  );
}
export function Recetas({ state, act }) {
  const [ingredient, setIngredient] = useState(
    state.ingredients[0]?.code ?? "",
  );
  const selected = state.ingredients.find((i) => i.code === ingredient);
  const products = state.products.filter((p) => p.mode === "RECETA");
  return (
    <>
      <Card title="Composición de preparado">
        <DemoForm
          onSubmit={(d) => act("RECIPE", d)}
          button="Guardar composición"
          disabled={!products.length}
        >
          <Field
            name="preparado"
            label="Preparado"
            options={codeOptions(products)}
          />
          <Field
            name="insumo"
            label="Insumo"
            options={codeOptions(state.ingredients)}
            value={ingredient}
            onChange={(e) => setIngredient(e.target.value)}
          />
          <Field
            name="cantidad"
            label="Cantidad por preparación"
            type="number"
            min="0.001"
            step="0.001"
            defaultValue="10"
            help={"Unidad del insumo: " + (selected?.unit ?? "")}
          />
        </DemoForm>
      </Card>
      <Card title="Composiciones actuales">
        <DataTable
          headers={["Preparado", "Insumo", "Cantidad", "Unidad"]}
          rows={Object.entries(state.recipeMap).flatMap(([code, rows]) =>
            rows.map((row) => {
              const i = state.ingredients.find((i) => i.code === row.code);
              return [code, i.name, row.qty / 1000, i.unit];
            }),
          )}
        />
      </Card>
    </>
  );
}
export function Empleados({ state, act }) {
  return (
    <div className="grid">
      <Card title="Empleados">
        <DataTable
          headers={["Código", "Nombre", "Base COP", "Estado"]}
          rows={state.employees.map((e) => [
            e.code,
            e.name,
            dinero(e.base),
            e.active ? "Activo" : "Inactivo",
          ])}
        />
      </Card>
      <Card title="Agregar empleado de ejemplo">
        <DemoForm
          onSubmit={(d) => act("EMPLOYEE", d)}
          button="Guardar empleado"
        >
          <Field name="codigo" label="Código del empleado" maxLength={64} />
          <Field name="nombre" label="Nombres" maxLength={128} />
          <Field
            name="base"
            label="Base configurable (COP)"
            type="number"
            min="0.01"
            step="0.01"
          />
          <Field name="estado" label="Estado" options={activeOptions} />
        </DemoForm>
      </Card>
    </div>
  );
}
export function Novedades({ state, act }) {
  return (
    <div className="grid">
      <Card title="Novedades">
        <DataTable
          headers={["Empleado", "Período", "Concepto", "Tipo", "Importe"]}
          rows={state.novelties.map((n) => [
            n.employee,
            n.period,
            n.concept,
            n.type,
            dinero(n.value),
          ])}
        />
      </Card>
      <Card title="Agregar novedad">
        <DemoForm onSubmit={(d) => act("NOVELTY", d)} button="Guardar novedad">
          <Field
            name="empleado"
            label="Empleado"
            options={codeOptions(state.employees)}
          />
          <Field
            name="periodo"
            label="Período"
            type="month"
            defaultValue={fechaBogota().slice(0, 7)}
          />
          <Field name="concepto" label="Concepto" maxLength={128} />
          <Field
            name="tipo"
            label="Tipo"
            options={["Devengo", "Deducción"].map((x) => [x, x])}
          />
          <Field
            name="valor"
            label="Importe (COP)"
            type="number"
            min="0.01"
            step="0.01"
          />
        </DemoForm>
      </Card>
    </div>
  );
}
export function Liquidacion({ state, act }) {
  const [employee, setEmployee] = useState(state.employees[0].code),
    [period, setPeriod] = useState(fechaBogota().slice(0, 7));
  const result = resumenNomina(state, employee, period);
  return (
    <Card title="Previsualizar liquidación">
      <div className="filters">
        <Field
          name="empleado-nomina"
          label="Empleado"
          options={codeOptions(state.employees)}
          value={employee}
          onChange={(e) => setEmployee(e.target.value)}
        />
        <Field
          name="periodo-nomina"
          label="Período"
          type="month"
          value={period}
          onChange={(e) => setPeriod(e.target.value)}
        />
      </div>
      <DataTable
        headers={["Concepto", "Importe COP"]}
        rows={["base", "dev", "ded", "neto"].map((key, i) => [
          ["Base", "Devengos", "Deducciones", "Neto ilustrativo"][i],
          dinero(result[key]),
        ])}
      />
      <p className="muted">
        BORRADOR didáctico. Cada novedad entra una vez en la suma. No incluye
        tasas legales ni representa una liquidación laboral real.
      </p>
      <button className="secondary" onClick={() => act("REVIEW")}>
        Registrar revisión de ejemplo
      </button>
    </Card>
  );
}
export function Cuentas({ state, act }) {
  return (
    <div className="grid">
      <Card title="Catálogo didáctico">
        <DataTable
          headers={["Código", "Nombre", "Naturaleza", "Estado"]}
          rows={state.accounts.map((a) => [
            a.code,
            a.name,
            a.nature,
            a.active ? "Activa" : "Inactiva",
          ])}
        />
      </Card>
      <Card title="Agregar cuenta">
        <DemoForm onSubmit={(d) => act("ACCOUNT", d)} button="Guardar cuenta">
          <Field name="codigo" label="Código de cuenta" maxLength={64} />
          <Field name="nombre" label="Nombre" maxLength={128} />
          <Field
            name="naturaleza"
            label="Naturaleza"
            options={["Débito", "Crédito"].map((x) => [x, x])}
          />
          <Field
            name="estado"
            label="Estado"
            options={activeOptions.map(([v, t]) => [
              v,
              v === "ACTIVO" ? "Activa" : "Inactiva",
            ])}
          />
        </DemoForm>
      </Card>
    </div>
  );
}
export function Asientos({ state, act }) {
  const active = state.accounts.filter((a) => a.active);
  return (
    <div className="grid">
      <Card title="Proponer asiento de dos líneas">
        <DemoForm
          onSubmit={(d) => act("SEAT", d)}
          button="Validar y guardar propuesta"
          disabled={active.length < 2}
        >
          <Field
            name="soporte"
            label="Documento de origen"
            defaultValue={state.lastSale?.invoice ?? "SOPORTE-DEMO"}
            maxLength={128}
          />
          <Field
            name="fecha"
            label="Fecha"
            type="date"
            defaultValue={fechaBogota()}
          />
          <Field
            name="cuentaDebito"
            label="Cuenta débito"
            options={codeOptions(active)}
            defaultValue="1005"
          />
          <Field
            name="debito"
            label="Débito (COP)"
            type="number"
            min="0.01"
            step="0.01"
            defaultValue="5000"
          />
          <Field
            name="cuentaCredito"
            label="Cuenta crédito"
            options={codeOptions(active)}
            defaultValue="4135"
          />
          <Field
            name="credito"
            label="Crédito (COP)"
            type="number"
            min="0.01"
            step="0.01"
            defaultValue="5000"
          />
        </DemoForm>
      </Card>
      <Card title="Asientos propuestos">
        <DataTable
          headers={["Soporte", "Fecha", "Débito", "Crédito", "Estado"]}
          rows={state.seats.map((s) => [
            s.doc,
            s.date,
            dinero(s.debit),
            dinero(s.credit),
            "Balanceado · propuesta",
          ])}
        />
      </Card>
    </div>
  );
}
export function Reportes({ state }) {
  const [filters, setFilters] = useState({
    area: "Todas",
    from: "",
    to: "",
    status: "Todos",
  });
  let sales = [];
  let error = "";
  try {
    sales = ventasFiltradas(state, filters.from, filters.to);
  } catch (e) {
    error = e.message;
  }
  const show = (area) => filters.area === "Todas" || filters.area === area,
    active = (x) =>
      filters.status === "Todos" ||
      (filters.status === "ACTIVO" ? x.active : !x.active);
  const set = (key, value) => setFilters({ ...filters, [key]: value });
  function exportar() {
    const csv = csvVentas(sales, {
      desde: filters.from,
      hasta: filters.to,
      negocio: "Café Demo A",
      rol: state.role,
      corte: new Date().toISOString(),
    });
    const url = URL.createObjectURL(
      new Blob([csv], { type: "text/csv;charset=utf-8" }),
    );
    const link = document.createElement("a");
    link.href = url;
    link.download = "ventas_demostracion_filtradas.csv";
    link.click();
    setTimeout(() => URL.revokeObjectURL(url), 0);
  }
  return (
    <>
      <Card title="Filtros de consulta">
        <p className="muted">
          Origen: Café Demo A · Administrador de demostración. Todos los datos
          son sintéticos y locales.
        </p>
        <div className="filters">
          <Field
            name="area-reporte"
            label="Área"
            options={["Todas", ...areas.filter((a) => a !== "Reportes")].map(
              (x) => [x, x],
            )}
            value={filters.area}
            onChange={(e) => set("area", e.target.value)}
          />
          <Field
            name="desde-reporte"
            label="Desde para ventas"
            type="date"
            required={false}
            value={filters.from}
            onChange={(e) => set("from", e.target.value)}
          />
          <Field
            name="hasta-reporte"
            label="Hasta para ventas"
            type="date"
            required={false}
            value={filters.to}
            onChange={(e) => set("to", e.target.value)}
          />
          <Field
            name="estado-reporte"
            label="Estado de empleados y cuentas"
            options={[
              ["Todos", "Todos"],
              ["ACTIVO", "Activos"],
              ["INACTIVO", "Inactivos"],
            ]}
            value={filters.status}
            onChange={(e) => set("status", e.target.value)}
          />
        </div>
        {error && (
          <p className="message error" role="alert">
            {error}
          </p>
        )}
      </Card>
      {show("Identidad") && (
        <Card title="Identidad">
          <DataTable
            headers={["Usuario", "Correo", "Rol"]}
            rows={state.users.map((u) => [u.name, u.email, u.role])}
          />
        </Card>
      )}
      {show("Ventas") && (
        <Card title="Ventas">
          <DataTable
            headers={["Factura", "Fecha y hora Bogotá", "Total COP"]}
            rows={sales.map((s) => [s.invoice, s.date, dinero(s.total)])}
          />
          <div className="total">
            Total filtrado {dinero(sales.reduce((a, s) => a + s.total, 0))}
          </div>
          <button disabled={!!error} onClick={exportar}>
            Exportar ventas filtradas en CSV
          </button>
        </Card>
      )}
      {show("Inventario") && (
        <Card title="Inventario">
          <StockTable state={state} />
        </Card>
      )}
      {show("Gastronomía") && (
        <Card title="Gastronomía">
          <DataTable
            headers={["Mesa", "Estado"]}
            rows={state.mesas.map((m) => [m.code, m.status])}
          />
          <p>
            Caja {state.caja.open ? "abierta" : "cerrada"} · efectivo de la
            sesión {dinero(efectivoSesion(state))} ·{" "}
            {Object.keys(state.recipeMap).length} composiciones.
          </p>
        </Card>
      )}
      {show("Nómina") && (
        <Card title="Nómina">
          <DataTable
            headers={["Empleado", "Base COP", "Estado"]}
            rows={state.employees
              .filter(active)
              .map((e) => [
                e.code,
                dinero(e.base),
                e.active ? "Activo" : "Inactivo",
              ])}
          />
          <p>{state.novelties.length} novedades sintéticas registradas.</p>
        </Card>
      )}
      {show("Contabilidad") && (
        <Card title="Contabilidad">
          <DataTable
            headers={["Cuenta", "Nombre", "Naturaleza"]}
            rows={state.accounts
              .filter(active)
              .map((a) => [a.code, a.name, a.nature])}
          />
          <p>{state.seats.length} propuestas de asiento balanceadas.</p>
        </Card>
      )}
    </>
  );
}

export const vistas = {
  acceso: Acceso,
  inicio: Inicio,
  usuarios: Usuarios,
  roles: Roles,
  "nueva-venta": NuevaVenta,
  pago: Pago,
  comprobante: Comprobante,
  productos: Productos,
  existencias: Existencias,
  movimientos: Movimientos,
  mesas: Mesas,
  caja: Caja,
  recetas: Recetas,
  empleados: Empleados,
  novedades: Novedades,
  liquidacion: Liquidacion,
  cuentas: Cuentas,
  asientos: Asientos,
  reportes: Reportes,
};
