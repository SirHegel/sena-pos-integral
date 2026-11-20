const ZONA = "America/Bogota";
function instante(value) {
  const normalized =
    typeof value === "string" &&
    /^\d{4}-\d\d-\d\dT\d\d:\d\d/.test(value) &&
    !/(Z|[+-]\d\d:\d\d)$/.test(value)
      ? value + "Z"
      : value;
  const date = new Date(normalized);
  return Number.isNaN(date.getTime()) ? null : date;
}
export function fechaNegocio(value) {
  if (!value) return "";
  if (typeof value === "string" && /^\d{4}-\d\d-\d\d$/.test(value))
    return value;
  const date = instante(value);
  if (!date) return "";
  const parts = Object.fromEntries(
    new Intl.DateTimeFormat("es-CO", {
      timeZone: ZONA,
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    })
      .formatToParts(date)
      .map((p) => [p.type, p.value]),
  );
  return `${parts.year}-${parts.month}-${parts.day}`;
}
export function fechaHora(value) {
  if (!value) return "—";
  if (typeof value === "string" && /^\d{4}-\d\d-\d\d$/.test(value))
    return value.split("-").reverse().join("/");
  const date = instante(value);
  return date
    ? new Intl.DateTimeFormat("es-CO", {
        timeZone: ZONA,
        dateStyle: "short",
        timeStyle: "short",
      }).format(date)
    : String(value);
}
