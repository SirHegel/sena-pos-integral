import test from "node:test";
import assert from "node:assert/strict";
import { fechaNegocio, fechaHora } from "../src/fechas.js";
test("el cierre comercial usa Bogotá aunque el instante UTC corresponda al día siguiente", () => {
  assert.equal(fechaNegocio("2026-11-20T02:42:44.930828Z"), "2026-11-19");
  assert.equal(fechaNegocio("2026-11-20T05:00:00Z"), "2026-11-20");
});
test("un DATETIME de la API guardado en UTC conserva su día comercial", () => {
  assert.equal(fechaNegocio("2026-11-20T02:42:44.930828"), "2026-11-19");
});
test("una fecha de vencimiento sin hora mantiene el día acordado", () => {
  assert.equal(fechaNegocio("2026-12-04"), "2026-12-04");
  assert.equal(fechaHora("2026-12-04"), "04/12/2026");
});
