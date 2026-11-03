import React, { useId } from "react";

export function Card({ title, children, heading = "h2" }) {
  const Heading = heading;
  return (
    <section className="card">
      <Heading>{title}</Heading>
      {children}
    </section>
  );
}
export function DataTable({
  headers,
  rows,
  caption,
  empty = "No hay registros para esta consulta.",
}) {
  return (
    <div className="tablewrap">
      <table>
        {caption && <caption>{caption}</caption>}
        <thead>
          <tr>
            {headers.map((h) => (
              <th key={h} scope="col">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length ? (
            rows.map((row, i) => (
              <tr key={row.key ?? i}>
                {row.cells
                  ? row.cells.map((cell, j) => <td key={j}>{cell}</td>)
                  : row.map((cell, j) => <td key={j}>{cell}</td>)}
              </tr>
            ))
          ) : (
            <tr>
              <td colSpan={headers.length} className="empty">
                {empty}
              </td>
            </tr>
          )}
        </tbody>
      </table>
    </div>
  );
}
export function Field({
  name,
  label,
  type = "text",
  options,
  help,
  required = true,
  ...props
}) {
  const unique = useId(),
    id = name + "-" + unique,
    helpId = id + "-ayuda";
  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      {options ? (
        <select
          id={id}
          name={name}
          required={required}
          aria-describedby={help ? helpId : undefined}
          {...props}
        >
          {options.map(([v, t]) => (
            <option key={v} value={v}>
              {t}
            </option>
          ))}
        </select>
      ) : (
        <input
          id={id}
          name={name}
          type={type}
          required={required}
          aria-describedby={help ? helpId : undefined}
          {...props}
        />
      )}
      {help && (
        <small id={helpId} className="muted">
          {help}
        </small>
      )}
    </div>
  );
}
export function DemoForm({
  children,
  onSubmit,
  button = "Guardar",
  disabled = false,
  ...props
}) {
  return (
    <form
      {...props}
      onSubmit={(event) => {
        event.preventDefault();
        onSubmit(Object.fromEntries(new FormData(event.currentTarget)));
      }}
    >
      {children}
      <button type="submit" disabled={disabled}>
        {button}
      </button>
    </form>
  );
}
export function RouteLink({ to, children, ...props }) {
  return (
    <a href={"#" + to} {...props}>
      {children}
    </a>
  );
}
export function RoleMatrix() {
  return (
    <>
      <DataTable
        headers={["Rol", "Funciones de la propuesta", "Restricción"]}
        rows={[
          [
            "Administrador",
            "Usuarios, catálogo y las siete áreas",
            "Sólo su negocio",
          ],
          [
            "Cajero",
            "Venta, pago, comprobante, mesas y caja",
            "Sin nómina ni edición contable",
          ],
          [
            "Inventario",
            "Productos, existencias y kardex",
            "Sin confirmar pagos",
          ],
          ["Nómina", "Empleados, novedades y revisión", "Sin caja"],
          ["Contabilidad", "Cuentas, asientos y reportes", "Sin asignar roles"],
        ]}
      />
      <p className="muted">
        Esta matriz describe la autorización que deberá validar el servidor. El
        acceso local de esta demostración no implementa esos permisos.
      </p>
    </>
  );
}
