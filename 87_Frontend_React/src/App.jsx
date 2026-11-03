import React, { useEffect, useReducer, useRef, useState } from "react";
import { rutas, areas, estadoInicial, reducirDemo } from "./modelo.js";
import { vistas } from "./vistas.jsx";
import { RouteLink } from "./componentes.jsx";

const leerRuta = () => window.location.hash.slice(1) || "acceso";
const descripciones = {
  inicio: "Consulta la operación y accede a las siete áreas del negocio.",
  usuarios: "Personas de ejemplo y roles propuestos para el negocio.",
  roles:
    "Diseño de permisos por función; su validación definitiva corresponde al servidor.",
  "nueva-venta":
    "Busca por código o nombre. El precio queda conservado en el detalle.",
  pago: "El inventario cambia sólo al confirmar la venta pagada.",
  comprobante: "Resultado de la confirmación y trazabilidad del inventario.",
  productos: "Catálogo y modalidad de descuento directo o por receta.",
  existencias:
    "Bodega principal. Disponibilidad de preparados por insumo limitante.",
  movimientos: "Salidas confirmadas con factura de origen y fecha de Bogotá.",
  mesas: "Selecciona la mesa para la siguiente venta de demostración.",
  caja: "Apertura, efectivo cobrado y revisión del cierre de la sesión.",
  recetas: "Cantidades por preparación, expresadas en la unidad del insumo.",
  empleados: "Datos ficticios y base configurable para la previsualización.",
  novedades: "Devengos y deducciones vinculados a un empleado y período.",
  liquidacion: "Previsualización didáctica; no constituye liquidación laboral.",
  cuentas: "Catálogo didáctico; no representa un plan contable aprobado.",
  asientos: "Propuesta de dos líneas equilibradas y con soporte de origen.",
  reportes:
    "Consulta las áreas con filtros y exporta las ventas del mismo resultado.",
};
export default function App() {
  const [state, dispatch] = useReducer(reducirDemo, undefined, estadoInicial);
  const [requested, setRequested] = useState(leerRuta);
  const [menuOpen, setMenuOpen] = useState(false);
  const mainRef = useRef(null),
    feedbackRef = useRef(null);
  const route = state.session
    ? rutas.some((r) => r[0] === requested) && requested !== "acceso"
      ? requested
      : "inicio"
    : "acceso";
  const descriptor = rutas.find((r) => r[0] === route),
    View = vistas[route];
  const act = (type, data, goto) =>
    dispatch({ type, data, goto, now: Date.now() });
  useEffect(() => {
    const change = () => {
      setRequested(leerRuta());
      setMenuOpen(false);
    };
    window.addEventListener("hashchange", change);
    return () => window.removeEventListener("hashchange", change);
  }, []);
  useEffect(() => {
    document.title = "POS Integral | " + descriptor[1];
    mainRef.current?.focus({ preventScroll: true });
    window.scrollTo({ top: 0, left: 0, behavior: "instant" });
  }, [route, descriptor]);
  useEffect(() => {
    if (state.feedback) feedbackRef.current?.focus({ preventScroll: true });
  }, [state.feedback]);
  useEffect(() => {
    if (state.navigateTo) {
      window.location.hash = state.navigateTo;
      dispatch({ type: "NAV_DONE" });
    }
  }, [state.navigateTo]);
  function clearWhenNavigating(event) {
    const link = event.target.closest('a[href^="#"]');
    if (link && link.getAttribute("href") !== "#contenido")
      dispatch({ type: "CLEAR" });
  }
  const notice = state.feedback && (
    <div
      ref={feedbackRef}
      tabIndex="-1"
      className={"message" + (state.feedback.error ? " error" : "")}
      role={state.feedback.error ? "alert" : "status"}
    >
      {state.feedback.message}
    </div>
  );
  return (
    <div onClickCapture={clearWhenNavigating}>
      <a
        className="skip"
        href="#contenido"
        onClick={(event) => {
          event.preventDefault();
          mainRef.current?.focus();
        }}
      >
        Saltar al contenido
      </a>
      {route === "acceso" ? (
        <div className="login">
          <div className="brand">
            POS <span>Integral</span>
          </div>
          <p className="muted">Operación del negocio, en un solo lugar</p>
          <main ref={mainRef} id="contenido" tabIndex="-1">
            {notice}
            <View state={state} act={act} />
          </main>
          <div className="footer">
            Datos sintéticos · La sesión se reinicia al recargar
          </div>
        </div>
      ) : (
        <>
          <header>
            <div>
              <div className="brand">
                POS <span>Integral</span>
              </div>
              <small className="muted">Café Demo A · {state.role}</small>
            </div>
            <span className="badge">Demostración local</span>
            <div className="actions no-margin">
              <button
                type="button"
                className="secondary menu"
                aria-expanded={menuOpen}
                aria-controls="navegacion"
                onClick={() => setMenuOpen(!menuOpen)}
              >
                Menú
              </button>
              <button
                type="button"
                className="secondary"
                onClick={() => act("LOGOUT", {}, "acceso")}
              >
                Cerrar sesión
              </button>
            </div>
          </header>
          <div className="shell">
            <aside
              id="navegacion"
              className={"sidebar" + (menuOpen ? " open" : "")}
            >
              <nav aria-label="Navegación principal">
                <RouteLink
                  to="inicio"
                  aria-current={route === "inicio" ? "page" : undefined}
                >
                  Inicio
                </RouteLink>
                {areas.map((group) => (
                  <React.Fragment key={group}>
                    <strong>{group}</strong>
                    {rutas
                      .filter((r) => r[2] === group && r[0] !== "acceso")
                      .map((r) => (
                        <RouteLink
                          key={r[0]}
                          to={r[0]}
                          aria-current={route === r[0] ? "page" : undefined}
                        >
                          {r[1]}
                        </RouteLink>
                      ))}
                  </React.Fragment>
                ))}
              </nav>
            </aside>
            <main ref={mainRef} id="contenido" tabIndex="-1">
              <div className="breadcrumb">
                <RouteLink to="inicio">Inicio</RouteLink> / {descriptor[2]} /{" "}
                {descriptor[1]}
              </div>
              <h1>{descriptor[1]}</h1>
              <p className="lede">{descripciones[route]}</p>
              {notice}
              <View key={route} state={state} act={act} />
              <div className="footer">
                Estado sintético en memoria · No se realizan cobros, pagos
                laborales ni registros contables reales.
              </div>
            </main>
          </div>
        </>
      )}
    </div>
  );
}
