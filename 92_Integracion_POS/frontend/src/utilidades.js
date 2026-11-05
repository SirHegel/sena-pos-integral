const A='ADMINISTRADOR',J='CAJERO',I='INVENTARIO',N='NOMINA',C='CONTABILIDAD';
export const RUTAS=[
 ['inicio','Inicio','General',[A,J,I,N,C]],['usuarios','Usuarios','Identidad',[A]],['roles','Roles','Identidad',[A]],
 ['nueva-venta','Nueva venta','Ventas',[A,J]],['pago','Pago','Ventas',[A,J]],['comprobante','Comprobante','Ventas',[A,J]],
 ['productos','Productos','Inventario',[A,J,I]],['existencias','Existencias','Inventario',[A,J,I]],['movimientos','Movimientos','Inventario',[A,I]],
 ['mesas','Mesas','Gastronomía y caja',[A,J]],['caja','Caja','Gastronomía y caja',[A,J]],['recetas','Recetas','Gastronomía y caja',[A,J,I]],
 ['empleados','Empleados','Nómina',[A,N]],['novedades','Novedades','Nómina',[A,N]],['liquidacion','Liquidación','Nómina',[A,N]],
 ['cuentas','Cuentas','Contabilidad',[A,C]],['asientos','Asientos','Contabilidad',[A,C]],['reportes','Reportes','Reportes',[A,J,I,N,C]]
];
export const puede=(actor,roles)=>Boolean(actor?.roles?.some(r=>roles.includes(r)));
export const AREAS_REPORTE=[['ventas','Ventas',[A,C,J]],['inventario','Inventario',[A,I]],['caja','Caja',[A,J]],['nomina','Nómina',[A,N]],['contabilidad','Contabilidad',[A,C]]];
export const dinero=v=>v==null?'—':new Intl.NumberFormat('es-CO',{style:'currency',currency:'COP',minimumFractionDigits:2,maximumFractionDigits:2}).format(Number(v));
export function fechaBogota(instant=new Date()){
 const p=Object.fromEntries(new Intl.DateTimeFormat('en',{timeZone:'America/Bogota',year:'numeric',month:'2-digit',day:'2-digit'}).formatToParts(instant).map(x=>[x.type,x.value]));return`${p.year}-${p.month}-${p.day}`;
}
export const instanteBogota=v=>v?new Intl.DateTimeFormat('es-CO',{timeZone:'America/Bogota',dateStyle:'short',timeStyle:'medium'}).format(new Date(typeof v==='number'||/([zZ]|[+-]\d\d:\d\d)$/.test(v)?v:v+'Z')):'—';
export const etiqueta=v=>String(v??'').replaceAll('_',' ');
export const opciones=(rows,id,label=r=>r.nombre||r.codigo||r.correo||r[id])=>(rows||[]).map(r=>[r[id],label(r)]);
export function cantidadPositiva(v){const n=Number(v);if(!Number.isSafeInteger(n)||n<1||n>1000000)throw new Error('Cantidad entera entre 1 y 1000000 requerida.');return n;}
export function filasDetalle(lineas){if(!lineas.length)throw new Error('Añada por lo menos un producto.');return lineas.map(l=>({productoId:l.productoId,cantidad:cantidadPositiva(l.cantidad)}));}
export function seleccionNovedades(ids){if(!ids.length||new Set(ids).size!==ids.length)throw new Error('Seleccione novedades distintas.');return{novedadesIds:ids};}
