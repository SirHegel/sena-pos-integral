import { test } from "node:test";
import assert from "node:assert/strict";
import {
  estadoInicial,
  reducirDemo,
  disponibilidad,
  totalVenta,
  efectivoSesion,
  fechaBogota,
  ventasFiltradas,
  csvVentas,
  resumenNomina,
  decimalEscalado,
} from "../src/modelo.js";

const action = (state, type, data, now = Date.parse("2026-11-03T15:00:00Z")) =>
  reducirDemo(state, { type, data, now });
const add = (s, producto, cantidad) =>
  action(s, "CART_ADD", { producto, cantidad });
const pay = (s) =>
  action(s, "PAY", {
    medio: "Efectivo",
    importe: (totalVenta(s) / 100).toFixed(2),
  });
function dosRecetas() {
  let s = estadoInicial();
  s = action(s, "PRODUCT", {
    codigo: "REC002",
    nombre: "Otro preparado",
    precio: "5000",
    modalidad: "RECETA",
    unidad: "Preparación",
  });
  return action(s, "RECIPE", {
    preparado: "REC002",
    insumo: "INS002",
    cantidad: "100",
  });
}

test("La venta por unidad rechaza fracciones y enteros inseguros sin alterar inventario", () => {
  for (const q of ["0", "-1", "1.5", "9007199254740992"]) {
    const s = estadoInicial(),
      n = add(s, "CAF001", q);
    assert.equal(n.feedback.error, true);
    assert.deepEqual(n.cart, []);
    assert.deepEqual(n.products, s.products);
  }
});
test("El cobro inválido conserva carrito y stock; el válido descuenta exactamente una vez", () => {
  let s = add(estadoInicial(), "CAF001", "2");
  const before = structuredClone(s);
  s = action(s, "PAY", { medio: "Efectivo", importe: "1" });
  assert.equal(s.feedback.error, true);
  assert.deepEqual(s.products, before.products);
  assert.deepEqual(s.cart, before.cart);
  assert.equal(s.sales.length, 0);
  s = pay(s);
  assert.equal(s.products[0].stock, 10);
  assert.equal(s.sales.length, 1);
  assert.equal(s.moves.length, 1);
  assert.equal(s.cart.length, 0);
  const again = action(s, "PAY", { medio: "Efectivo", importe: "10000" });
  assert.equal(again.feedback.error, true);
  assert.equal(again.sales.length, 1);
  assert.equal(again.moves.length, 1);
});
test("Dos preparados comparten insumo: la demanda conjunta insuficiente rechaza todo el cobro", () => {
  let s = dosRecetas();
  s = add(add(s, "REC001", "3"), "REC002", "3");
  const n = pay(s);
  assert.equal(n.feedback.error, true);
  assert.deepEqual(n.ingredients, s.ingredients);
  assert.deepEqual(n.cart, s.cart);
  assert.equal(n.sales.length, 0);
  assert.equal(n.moves.length, 0);
});
test("Un cobro válido de dos recetas agrega la demanda por insumo y evita descontar producto terminado", () => {
  let s = dosRecetas();
  s = pay(add(add(s, "REC001", "2"), "REC002", "3"));
  assert.equal(s.feedback.error, false);
  assert.equal(s.ingredients.find((i) => i.code === "INS002").stock, 0);
  assert.equal(s.ingredients.find((i) => i.code === "INS001").stock, 980000);
  assert.equal(s.moves.length, 2);
  assert.equal(s.products.find((p) => p.code === "REC001").stock, 0);
});
test("Milésimas exactas: 0.3 unidades disponibles permiten tres preparaciones de 0.1", () => {
  const s = estadoInicial();
  s.ingredients[0].stock = decimalEscalado("0.3", 3);
  s.recipeMap.REC001 = [{ code: "INS001", qty: decimalEscalado("0.1", 3) }];
  assert.equal(disponibilidad(s, s.products[2]), 3);
});
test("El detalle conserva el precio histórico aunque el catálogo cambie después de agregar", () => {
  let s = add(estadoInicial(), "CAF001", "1");
  s = structuredClone(s);
  s.products[0].price = 600000;
  s = pay(s);
  assert.equal(s.sales[0].lines[0].price, 500000);
  assert.equal(s.sales[0].total, 500000);
});
test("Caja separa confirmaciones externas y efectivo; abrir otra sesión no arrastra cobros anteriores", () => {
  let s = add(estadoInicial(), "CAF001", "1");
  s = action(s, "PAY", { medio: "Confirmación externa", importe: "5000" });
  assert.equal(efectivoSesion(s), 0);
  s = pay(add(s, "PAN001", "1"));
  assert.equal(efectivoSesion(s), 300000);
  s = action(s, "CAJA", { conteo: "103000" });
  assert.equal(s.cashClosed[0].difference, 0);
  assert.equal(s.caja.open, false);
  const blocked = pay(add(s, "CAF001", "1"));
  assert.equal(blocked.feedback.error, true);
  assert.equal(blocked.sales.length, 2);
  s = action(s, "CAJA", { conteo: "100" });
  assert.equal(s.caja.id, 2);
  assert.equal(efectivoSesion(s), 0);
});
test("Los filtros de ventas y CSV usan el día de Bogotá, incluso cerca de medianoche UTC", () => {
  assert.equal(fechaBogota("2026-11-04T04:59:59Z"), "2026-11-03");
  assert.equal(fechaBogota("2026-11-04T05:00:00Z"), "2026-11-04");
  const s = estadoInicial();
  s.sales = [
    {
      invoice: "A",
      iso: "2026-11-04T04:59:59Z",
      date: "3/11/2026",
      total: 100,
    },
    {
      invoice: "B",
      iso: "2026-11-04T05:00:00Z",
      date: "4/11/2026",
      total: 200,
    },
  ];
  const rows = ventasFiltradas(s, "2026-11-03", "2026-11-03");
  assert.equal(rows.length, 1);
  assert.equal(rows[0].invoice, "A");
  const csv = csvVentas(rows, {
    desde: "2026-11-03",
    hasta: "2026-11-03",
    corte: "2026-11-04T04:00:00Z",
  });
  assert.match(csv, /"A"/);
  assert.doesNotMatch(csv, /"B"/);
  assert.match(csv, /"Desde";"2026-11-03"/);
  assert.match(csv, /"Negocio";"Café Demo A"/);
  assert.match(csv, /"Corte Bogotá"/);
  assert.throws(() => ventasFiltradas(s, "2026-11-04", "2026-11-03"));
});
test("La nómina filtra empleado y período y suma cada novedad una vez", () => {
  let s = estadoInicial();
  s = action(s, "NOVELTY", {
    empleado: "DEMO01",
    periodo: "2026-11",
    concepto: "Deducción de ejemplo",
    tipo: "Deducción",
    valor: "50000",
  });
  s = action(s, "NOVELTY", {
    empleado: "DEMO01",
    periodo: "2026-12",
    concepto: "Otro período",
    tipo: "Devengo",
    valor: "999",
  });
  assert.deepEqual(resumenNomina(s, "DEMO01", "2026-11"), {
    base: 200000000,
    dev: 10000000,
    ded: 5000000,
    neto: 205000000,
  });
});
test("El asiento requiere equilibrio, cuentas distintas activas y fecha de calendario válida", () => {
  const s = estadoInicial(),
    f = {
      soporte: "PRUEBA",
      fecha: "2026-11-03",
      cuentaDebito: "1005",
      cuentaCredito: "4135",
      debito: "10",
      credito: "10",
    };
  for (const change of [
    { credito: "9" },
    { cuentaCredito: "1005" },
    { fecha: "2026-02-30" },
  ]) {
    const n = action(s, "SEAT", { ...f, ...change });
    assert.equal(n.feedback.error, true);
    assert.equal(n.seats.length, 0);
  }
  const n = action(s, "SEAT", f);
  assert.equal(n.feedback.error, false);
  assert.equal(n.seats.length, 1);
});
test("La identidad conserva ceros iniciales y rechaza nacimiento futuro según Bogotá", () => {
  const f = {
      nombre: "Persona",
      apellidos: "Ejemplo",
      documento: "000123",
      nacimiento: "2000-01-01",
      email: "persona@example.test",
      rol: "Cajero",
    },
    now = Date.parse("2026-11-04T02:00:00Z");
  let s = action(
    estadoInicial(),
    "USER",
    { ...f, nacimiento: "2026-11-04" },
    now,
  );
  assert.equal(s.feedback.error, true);
  s = action(s, "USER", f, now);
  assert.equal(s.users.at(-1).document, "000123");
  assert.equal(s.feedback.error, false);
  const dup = action(
    s,
    "USER",
    { ...f, documento: "456", email: "PERSONA@EXAMPLE.TEST" },
    now,
  );
  assert.equal(dup.feedback.error, true);
  assert.equal(dup.users.length, 2);
});
