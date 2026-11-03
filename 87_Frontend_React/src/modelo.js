// Reglas de la demostración de GA6-68, separadas del renderizado React.
// El estado es sintético y volátil. No sustituye transacciones ni permisos del servidor.
export const rutas = [
  ["acceso", "Acceso", "Identidad"],
  ["inicio", "Inicio", "General"],
  ["usuarios", "Usuarios", "Identidad"],
  ["roles", "Roles y permisos", "Identidad"],
  ["nueva-venta", "Nueva venta", "Ventas"],
  ["pago", "Revisar pago", "Ventas"],
  ["comprobante", "Comprobante", "Ventas"],
  ["productos", "Productos", "Inventario"],
  ["existencias", "Existencias", "Inventario"],
  ["movimientos", "Kardex", "Inventario"],
  ["mesas", "Mesas", "Gastronomía"],
  ["caja", "Sesión de caja", "Gastronomía"],
  ["recetas", "Recetas", "Gastronomía"],
  ["empleados", "Empleados", "Nómina"],
  ["novedades", "Novedades", "Nómina"],
  ["liquidacion", "Liquidación", "Nómina"],
  ["cuentas", "Cuentas", "Contabilidad"],
  ["asientos", "Asientos", "Contabilidad"],
  ["reportes", "Reportes", "Reportes"],
];
export const areas = [
  "Identidad",
  "Ventas",
  "Inventario",
  "Gastronomía",
  "Nómina",
  "Contabilidad",
  "Reportes",
];
export const dinero = (centavos) =>
  new Intl.NumberFormat("es-CO", {
    style: "currency",
    currency: "COP",
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(centavos / 100);
export function fechaBogota(instant = new Date()) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Bogota",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(new Date(instant));
  const get = (type) => parts.find((p) => p.type === type).value;
  return `${get("year")}-${get("month")}-${get("day")}`;
}
export const horaBogota = (instant) =>
  new Date(instant).toLocaleString("es-CO", { timeZone: "America/Bogota" });
const ensure = (ok, message) => {
  if (!ok) throw new Error(message);
};
function fechaValida(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value ?? "")) return false;
  const date = new Date(value + "T12:00:00Z");
  return (
    Number.isFinite(date.getTime()) && date.toISOString().slice(0, 10) === value
  );
}
function texto(value, max, label) {
  const result = String(value ?? "").trim();
  ensure(
    result.length > 0 &&
      result.length <= max &&
      !/[\u0000-\u001f\u007f]/.test(result),
    `${label}: escribe de 1 a ${max} caracteres válidos.`,
  );
  return result;
}
// Las cantidades de insumo se calculan en milésimas; los importes, en centavos.
// Así no se acumulan errores binarios en cantidades como 0.1 y 0.3.
export function decimalEscalado(raw, decimales, minimo = 0) {
  const str = String(raw ?? "").trim();
  ensure(
    new RegExp(`^\\d+(?:\\.\\d{1,${decimales}})?$`).test(str),
    "Escribe un número positivo con la precisión indicada.",
  );
  const [entero, decimal = ""] = str.split(".");
  const result =
    Number(entero) * 10 ** decimales + Number(decimal.padEnd(decimales, "0"));
  ensure(
    Number.isSafeInteger(result) &&
      result >= minimo &&
      result <= 99999999999999,
    "Importe o cantidad fuera de rango.",
  );
  return result;
}
const enteroPositivo = (raw) => {
  const n = Number(raw);
  ensure(
    Number.isSafeInteger(n) && n > 0,
    "La cantidad por unidad debe ser un entero positivo.",
  );
  return n;
};
export function estadoInicial() {
  return {
    session: false,
    role: "Administrador",
    failures: 0,
    lockedUntil: 0,
    cart: [],
    sales: [],
    lastSale: null,
    selectedMesa: "Sin mesa",
    caja: { open: true, fondo: 10000000, id: 1 },
    cashClosed: [],
    moves: [],
    seats: [],
    users: [
      {
        name: "Operador de demostración",
        email: "operador@example.invalid",
        role: "Administrador",
      },
    ],
    products: [
      {
        code: "CAF001",
        name: "Café filtrado",
        price: 500000,
        stock: 12,
        mode: "DIRECTO",
        unit: "Unidad",
      },
      {
        code: "PAN001",
        name: "Pan artesanal",
        price: 300000,
        stock: 8,
        mode: "DIRECTO",
        unit: "Unidad",
      },
      {
        code: "REC001",
        name: "Bebida especial",
        price: 700000,
        stock: 0,
        mode: "RECETA",
        unit: "Preparación",
      },
    ],
    ingredients: [
      { code: "INS001", name: "Café molido", stock: 1000000, unit: "g" },
      { code: "INS002", name: "Leche", stock: 500000, unit: "ml" },
    ],
    recipeMap: {
      REC001: [
        { code: "INS001", qty: 10000 },
        { code: "INS002", qty: 100000 },
      ],
    },
    employees: [
      {
        code: "DEMO01",
        name: "Empleado de demostración",
        base: 200000000,
        active: true,
      },
    ],
    novelties: [
      {
        employee: "DEMO01",
        period: "2026-11",
        concept: "Bonificación de ejemplo",
        type: "Devengo",
        value: 10000000,
      },
    ],
    accounts: [
      { code: "1005", name: "Caja", nature: "Débito", active: true },
      { code: "4135", name: "Ventas", nature: "Crédito", active: true },
    ],
    mesas: [
      { code: "M01", status: "Disponible" },
      { code: "M02", status: "Ocupada" },
      { code: "M03", status: "Disponible" },
    ],
    feedback: null,
  };
}
export function disponibilidad(state, product) {
  if (product.mode === "DIRECTO") return product.stock;
  const rows = state.recipeMap[product.code] ?? [];
  return rows.length
    ? Math.min(
        ...rows.map((r) =>
          Math.floor(
            (state.ingredients.find((i) => i.code === r.code)?.stock ?? 0) /
              r.qty,
          ),
        ),
      )
    : 0;
}
export function totalVenta(state) {
  const total = state.cart.reduce(
    (sum, line) => sum + line.price * line.qty,
    0,
  );
  ensure(
    Number.isSafeInteger(total),
    "El total supera el límite de precisión de la demostración.",
  );
  return total;
}
export const efectivoSesion = (state) =>
  state.sales
    .filter((v) => v.method === "Efectivo" && v.cashSession === state.caja.id)
    .reduce((sum, v) => sum + v.total, 0);
export function ventasFiltradas(state, from = "", to = "") {
  ensure(
    !from || !to || from <= to,
    "La fecha inicial debe ser igual o anterior a la final.",
  );
  return state.sales.filter(
    (v) =>
      (!from || fechaBogota(v.iso) >= from) &&
      (!to || fechaBogota(v.iso) <= to),
  );
}
export function resumenNomina(state, employee, period) {
  const person = state.employees.find((e) => e.code === employee);
  ensure(person, "Empleado no disponible.");
  const rows = state.novelties.filter(
    (n) => n.employee === employee && n.period === period,
  );
  const dev = rows
      .filter((n) => n.type === "Devengo")
      .reduce((a, n) => a + n.value, 0),
    ded = rows
      .filter((n) => n.type === "Deducción")
      .reduce((a, n) => a + n.value, 0);
  return { base: person.base, dev, ded, neto: person.base + dev - ded };
}
export function csvVentas(rows, contexto = {}) {
  const cell = (value) => '"' + String(value).replaceAll('"', '""') + '"';
  const metadata = [
    ["Origen", "Demostración local"],
    ["Negocio", contexto.negocio ?? "Café Demo A"],
    ["Rol de consulta", contexto.rol ?? "Administrador de demostración"],
    ["Área", "Ventas"],
    ["Desde", contexto.desde ?? ""],
    ["Hasta", contexto.hasta ?? ""],
    ["Corte Bogotá", horaBogota(contexto.corte ?? new Date())],
    [],
  ];
  return (
    "\uFEFF" +
    [
      ...metadata,
      ["Factura", "Fecha y hora Bogotá", "Total COP"],
      ...rows.map((v) => [v.invoice, v.date, (v.total / 100).toFixed(2)]),
    ]
      .map((row) => row.map(cell).join(";"))
      .join("\r\n")
  );
}
function operar(s, a) {
  const f = a.data ?? {};
  const now = a.now ?? Date.now();
  switch (a.type) {
    case "LOGIN":
      ensure(
        now >= s.lockedUntil,
        "Acceso bloqueado temporalmente. Espera a que termine el bloqueo de 30 segundos.",
      );
      if (f.correo === "operador@example.invalid" && f.clave === "demo1234") {
        s.session = true;
        s.failures = 0;
        return "Sesión local de demostración iniciada.";
      }
      s.failures++;
      if (s.failures >= 3) {
        s.lockedUntil = now + 30000;
        s.failures = 0;
        return {
          message:
            "Tres intentos fallidos. Acceso bloqueado temporalmente por 30 segundos.",
          error: true,
        };
      }
      return {
        message: `Datos de ejemplo incorrectos. Intentos restantes: ${3 - s.failures}.`,
        error: true,
      };
    case "LOGOUT":
      s.session = false;
      s.cart = [];
      return "Sesión local cerrada.";
    case "USER": {
      const doc = texto(f.documento, 20, "Cédula de ejemplo"),
        birth = f.nacimiento,
        email = texto(f.email, 254, "Correo").toLowerCase();
      ensure(
        /^\d+$/.test(doc),
        "La cédula de ejemplo debe conservarse como cadena de dígitos.",
      );
      ensure(
        fechaValida(birth) && birth <= fechaBogota(now),
        "La fecha de nacimiento debe ser válida y no puede ser futura.",
      );
      ensure(
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email),
        "Escribe un correo válido.",
      );
      ensure(
        !s.users.some((u) => u.document === doc || u.email === email),
        "La cédula o el correo ya existen en el negocio de ejemplo.",
      );
      ensure(
        [
          "Cajero",
          "Administrador",
          "Inventario",
          "Nómina",
          "Contabilidad",
        ].includes(f.rol),
        "Selecciona un rol de la propuesta.",
      );
      s.users.push({
        name:
          texto(f.nombre, 80, "Nombres") +
          " " +
          texto(f.apellidos, 80, "Apellidos"),
        document: doc,
        birth,
        email,
        role: f.rol,
      });
      return "Usuario de ejemplo agregado.";
    }
    case "CART_ADD": {
      const p = s.products.find((p) => p.code === f.producto);
      ensure(p, "Selecciona un producto.");
      const qty = enteroPositivo(f.cantidad);
      const current = s.cart.find((l) => l.code === p.code);
      ensure(
        Number.isSafeInteger(qty + (current?.qty ?? 0)) &&
          qty + (current?.qty ?? 0) <= disponibilidad(s, p),
        "Stock insuficiente. Revisa la cantidad disponible.",
      );
      if (current) current.qty += qty;
      else
        s.cart.push({
          code: p.code,
          name: p.name,
          price: p.price,
          mode: p.mode,
          qty,
        });
      totalVenta(s);
      return "Producto agregado. El inventario aún no se ha descontado.";
    }
    case "CART_REMOVE":
      s.cart = s.cart.filter((l) => l.code !== f.code);
      return "Producto retirado del detalle.";
    case "PAY": {
      ensure(
        s.caja.open && s.cart.length,
        "Se requiere caja abierta y una venta con productos.",
      );
      ensure(
        ["Efectivo", "Confirmación externa"].includes(f.medio),
        "Selecciona un medio de pago de ejemplo.",
      );
      ensure(
        decimalEscalado(f.importe, 2) === totalVenta(s),
        "El importe confirmado debe coincidir con el total de la venta.",
      );
      const demand = {};
      for (const line of s.cart) {
        const p = s.products.find((p) => p.code === line.code);
        ensure(p, "Producto no disponible.");
        if (p.mode === "RECETA") {
          const rows = s.recipeMap[p.code] ?? [];
          ensure(rows.length, "El preparado requiere una composición.");
          for (const row of rows)
            demand[row.code] = (demand[row.code] ?? 0) + row.qty * line.qty;
        } else
          ensure(
            line.qty <= p.stock,
            "La disponibilidad cambió. Regresa al detalle.",
          );
      }
      // Primero se verifica la demanda conjunta de todas las recetas; sólo después se escribe el estado.
      for (const [code, qty] of Object.entries(demand)) {
        const ingredient = s.ingredients.find((i) => i.code === code);
        ensure(
          ingredient && Number.isSafeInteger(qty) && qty <= ingredient.stock,
          "Stock insuficiente de insumos. Regresa al detalle.",
        );
      }
      const iso = new Date(now).toISOString(),
        date = horaBogota(iso),
        invoice = `DEMO-${String(s.sales.length + 1).padStart(4, "0")}`;
      const sale = {
        invoice,
        date,
        iso,
        lines: s.cart.map((l) => ({ ...l })),
        total: totalVenta(s),
        mesa: s.selectedMesa,
        method: f.medio,
        cashSession: s.caja.id,
      };
      for (const line of s.cart) {
        const p = s.products.find((p) => p.code === line.code);
        if (p.mode === "DIRECTO") {
          p.stock -= line.qty;
          s.moves.push({
            date,
            iso,
            invoice,
            name: p.name,
            qty: line.qty,
            balance: p.stock,
            unit: p.unit,
          });
        }
      }
      for (const [code, qty] of Object.entries(demand)) {
        const i = s.ingredients.find((i) => i.code === code);
        i.stock -= qty;
        s.moves.push({
          date,
          iso,
          invoice,
          name: i.name,
          qty: qty / 1000,
          balance: i.stock / 1000,
          unit: i.unit,
        });
      }
      s.sales.push(sale);
      s.lastSale = sale;
      s.cart = [];
      return "Pago de demostración confirmado una sola vez.";
    }
    case "PRODUCT": {
      const code = texto(f.codigo, 64, "Código").toUpperCase();
      ensure(
        !s.products.some((p) => p.code === code),
        "El código ya existe en el negocio.",
      );
      ensure(
        ["DIRECTO", "RECETA"].includes(f.modalidad),
        "Selecciona una modalidad válida.",
      );
      s.products.push({
        code,
        name: texto(f.nombre, 128, "Nombre"),
        price: decimalEscalado(f.precio, 2, 1),
        mode: f.modalidad,
        unit: texto(f.unidad, 32, "Unidad"),
        stock: 0,
      });
      return "Producto guardado con saldo inicial cero.";
    }
    case "MESA":
      ensure(
        s.mesas.some((m) => m.code === f.code),
        "Mesa no disponible.",
      );
      s.selectedMesa = f.code;
      return `Mesa ${f.code} asociada a la siguiente venta.`;
    case "CAJA": {
      const count = decimalEscalado(f.conteo, 2);
      if (s.caja.open) {
        const expected = s.caja.fondo + efectivoSesion(s);
        s.cashClosed.push({
          session: s.caja.id,
          expected,
          count,
          difference: count - expected,
        });
        s.caja.open = false;
        return `Caja cerrada. Esperado ${dinero(expected)}; conteo ${dinero(count)}; diferencia ${dinero(count - expected)}.`;
      }
      s.caja = { open: true, fondo: count, id: s.caja.id + 1 };
      return "Nueva sesión de ejemplo abierta.";
    }
    case "RECIPE": {
      ensure(
        s.products.some((p) => p.code === f.preparado && p.mode === "RECETA"),
        "Selecciona un preparado.",
      );
      ensure(
        s.ingredients.some((i) => i.code === f.insumo),
        "Selecciona un insumo.",
      );
      const qty = decimalEscalado(f.cantidad, 3, 1),
        rows = (s.recipeMap[f.preparado] ??= []);
      const line = rows.find((r) => r.code === f.insumo);
      if (line) line.qty = qty;
      else rows.push({ code: f.insumo, qty });
      return "Composición guardada en esta sesión.";
    }
    case "EMPLOYEE": {
      const code = texto(f.codigo, 64, "Código del empleado").toUpperCase();
      ensure(
        !s.employees.some((e) => e.code === code),
        "El código de empleado ya existe.",
      );
      s.employees.push({
        code,
        name: texto(f.nombre, 128, "Nombre"),
        base: decimalEscalado(f.base, 2, 1),
        active: f.estado === "ACTIVO",
      });
      return "Empleado de ejemplo guardado.";
    }
    case "NOVELTY":
      ensure(
        s.employees.some((e) => e.code === f.empleado),
        "Selecciona un empleado.",
      );
      ensure(/^\d{4}-(0[1-9]|1[0-2])$/.test(f.periodo), "Período inválido.");
      ensure(
        ["Devengo", "Deducción"].includes(f.tipo),
        "Selecciona un tipo válido.",
      );
      s.novelties.push({
        employee: f.empleado,
        period: f.periodo,
        concept: texto(f.concepto, 128, "Concepto"),
        type: f.tipo,
        value: decimalEscalado(f.valor, 2, 1),
      });
      return "Novedad de ejemplo registrada.";
    case "REVIEW":
      return "Revisión de ejemplo marcada; no aprueba ni paga una nómina.";
    case "ACCOUNT": {
      const code = texto(f.codigo, 64, "Código de cuenta");
      ensure(!s.accounts.some((c) => c.code === code), "La cuenta ya existe.");
      ensure(
        ["Débito", "Crédito"].includes(f.naturaleza),
        "Selecciona una naturaleza válida.",
      );
      s.accounts.push({
        code,
        name: texto(f.nombre, 128, "Nombre"),
        nature: f.naturaleza,
        active: f.estado === "ACTIVA",
      });
      return "Cuenta de ejemplo guardada.";
    }
    case "SEAT": {
      const debit = decimalEscalado(f.debito, 2, 1),
        credit = decimalEscalado(f.credito, 2, 1);
      ensure(
        debit === credit,
        "El asiento no está equilibrado: débitos y créditos deben coincidir.",
      );
      ensure(
        f.cuentaDebito !== f.cuentaCredito,
        "Selecciona cuentas diferentes para las dos líneas.",
      );
      ensure(
        [f.cuentaDebito, f.cuentaCredito].every((code) =>
          s.accounts.some((c) => c.code === code && c.active),
        ),
        "Selecciona cuentas activas.",
      );
      ensure(fechaValida(f.fecha), "Selecciona una fecha válida.");
      s.seats.push({
        doc: texto(f.soporte, 128, "Soporte"),
        date: f.fecha,
        debit,
        credit,
        accountDebit: f.cuentaDebito,
        accountCredit: f.cuentaCredito,
      });
      return "Propuesta balanceada guardada en esta sesión.";
    }
    default:
      throw new Error("Acción de demostración no reconocida.");
  }
}
export function reducirDemo(state, action) {
  if (action.type === "CLEAR")
    return { ...state, feedback: null, navigateTo: null };
  if (action.type === "NAV_DONE") return { ...state, navigateTo: null };
  // La copia evita escribir parcialmente en el estado anterior si falla una validación.
  const next = structuredClone(state);
  try {
    const answer = operar(next, action);
    next.feedback =
      typeof answer === "string" ? { message: answer, error: false } : answer;
    next.navigateTo = next.feedback.error ? null : (action.goto ?? null);
    return next;
  } catch (error) {
    return {
      ...state,
      navigateTo: null,
      feedback: { message: error.message, error: true },
    };
  }
}
