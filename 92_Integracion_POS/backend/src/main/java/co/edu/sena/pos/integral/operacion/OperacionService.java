package co.edu.sena.pos.integral.operacion;

import co.edu.sena.pos.integral.nucleo.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OperacionService {
    private final Db db;
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_MONEY = new BigDecimal("999999999999.99");
    private static final BigDecimal MAX_QTY = new BigDecimal("99999999999.999");
    public OperacionService(Db db) { this.db = db; }
    private static ApiError conflicto(String msg) { return new ApiError(409,"CONFLICTO_OPERATIVO",msg); }
    private static String nuevo() { return UUID.randomUUID().toString(); }
    private static LocalDateTime ahora() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static String s(Map<String,Object> r,String k) { return Objects.toString(r.get(k),null); }
    private static BigDecimal n(Map<String,Object> r,String k) { return new BigDecimal(r.get(k).toString()); }
    private static boolean si(Map<String,Object> r,String k) { return Boolean.TRUE.equals(r.get(k)) || "1".equals(Objects.toString(r.get(k),"")); }
    private static String id(Map<String,Object> b,String k) { return V.uuid(b.get(k),k); }
    private static String opcionalId(Map<String,Object> b,String k) { return b.get(k)==null?null:id(b,k); }
    private static void campos(Map<String,Object> b,String... names) {
        if (b==null || !new HashSet<>(Arrays.asList(names)).containsAll(b.keySet()))
            throw V.error("cuerpo","contiene campos no permitidos");
    }
    private static BigDecimal importe(Map<String,Object>b,String k) { return V.decimal(b.get(k),k,2,ZERO,false); }
    private static BigDecimal positivo(Map<String,Object>b,String k,int escala) { return V.decimal(b.get(k),k,escala,ZERO,true); }
    private static BigDecimal limitado(BigDecimal x, BigDecimal max,String campo) {
        if (x.abs().compareTo(max)>0) throw V.error(campo,"supera la precisión admitida");
        return x;
    }
    private static BigDecimal moneda(BigDecimal x) { return limitado(x.setScale(2,RoundingMode.HALF_UP),MAX_MONEY,"total"); }
    private static void propietario(Actor a,Map<String,Object> r) {
        if (!a.tiene("ADMINISTRADOR") && !a.usuarioId().equals(s(r,"usuario_id")))
            throw new ApiError(403,"PERMISO_DENEGADO","El recurso pertenece a otro operador del negocio");
    }
    private Map<String,Object> producto(Actor a,String id,boolean lock) {
        return db.requerido("SELECT * FROM producto WHERE tenant_id=? AND producto_id=?"+(lock?" FOR UPDATE":""),a.tenantId(),id);
    }
    private Map<String,Object> sesion(Actor a,String id,boolean abierta) {
        var r=db.requerido("SELECT * FROM sesion_caja WHERE tenant_id=? AND sesion_id=? FOR UPDATE",a.tenantId(),id);
        propietario(a,r);
        if (abierta && r.get("cerrada_en")!=null) throw conflicto("La sesión de caja está cerrada");
        return r;
    }
    private Map<String,Object> bloquearVenta(Actor a,String id,boolean abierta) {
        // Confirmación, edición, anulación y cierre toman primero la misma fila de sesión.
        var vista=db.requerido("SELECT * FROM venta WHERE tenant_id=? AND venta_id=?",a.tenantId(),id);
        propietario(a,vista);
        if (vista.get("sesion_caja_id")==null) throw conflicto("La venta requiere sesión de caja");
        sesion(a,s(vista,"sesion_caja_id"),abierta);
        var r=db.requerido("SELECT * FROM venta WHERE tenant_id=? AND venta_id=? FOR UPDATE",a.tenantId(),id);
        propietario(a,r);
        return r;
    }
    private void borrador(Map<String,Object> r) {
        if (!"BORRADOR".equals(s(r,"estado"))) throw conflicto("Sólo se modifica una venta en borrador");
    }
    public List<Map<String,Object>> bodegas(Actor a) { return db.lista("SELECT * FROM bodega WHERE tenant_id=? ORDER BY nombre LIMIT 200",a.tenantId()); }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> crearBodega(Actor a,Map<String,Object>b) {
        campos(b,"nombre");String id=nuevo();
        db.cambiar("INSERT INTO bodega(tenant_id,bodega_id,nombre) VALUES(?,?,?)",a.tenantId(),id,V.texto(b.get("nombre"),"nombre",128));
        return db.requerido("SELECT * FROM bodega WHERE tenant_id=? AND bodega_id=?",a.tenantId(),id);
    }
    public List<Map<String,Object>> existencias(Actor a,String bodega) {
        if (bodega!=null) bodega=V.uuid(bodega,"bodegaId");
        return db.lista("SELECT e.*,p.codigo,p.nombre,p.unidad FROM existencia e JOIN producto p ON p.tenant_id=e.tenant_id AND p.producto_id=e.producto_id WHERE e.tenant_id=? AND (? IS NULL OR e.bodega_id=?) ORDER BY e.bodega_id,p.codigo LIMIT 500",a.tenantId(),bodega,bodega).stream().map(MinimosService::presentar).toList();
    }
    public List<Map<String,Object>> movimientos(Actor a,String bodega) {
        if (bodega!=null) bodega=V.uuid(bodega,"bodegaId");
        return db.lista("SELECT m.*,p.codigo,p.unidad FROM movimiento_inventario m JOIN producto p ON p.tenant_id=m.tenant_id AND p.producto_id=m.producto_id WHERE m.tenant_id=? AND (? IS NULL OR m.bodega_id=?) ORDER BY m.registrado_en DESC,m.movimiento_id LIMIT 500",a.tenantId(),bodega,bodega);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> mover(Actor a,Map<String,Object>b) {
        campos(b,"bodegaId","productoId","cantidadDelta","tipo","documentoOrigen");
        String bid=id(b,"bodegaId"),pid=id(b,"productoId");
        String tipo=V.opcion(b.get("tipo"),"tipo",Set.of("ENTRADA","AJUSTE"));
        String doc=V.texto(b.get("documentoOrigen"),"documentoOrigen",128);
        BigDecimal delta=V.decimal(b.get("cantidadDelta"),"cantidadDelta",3,MAX_QTY.negate(),false);
        if(delta.signum()==0 || (tipo.equals("ENTRADA")&&delta.signum()<0))throw V.error("cantidadDelta","entrada positiva; ajuste distinto de cero");
        db.requerido("SELECT bodega_id FROM bodega WHERE tenant_id=? AND bodega_id=?",a.tenantId(),bid);
        var p=producto(a,pid,true);
        if(!si(p,"activo")||!"DIRECTO".equals(s(p,"modo_descuento")))throw conflicto("Sólo se mueve inventario físico activo de descuento directo");
        var anterior=db.uno("SELECT * FROM movimiento_inventario WHERE tenant_id=? AND tipo_origen=? AND documento_origen=? AND producto_id=? AND bodega_id=? AND componente_no=0 FOR UPDATE",a.tenantId(),tipo,doc,pid,bid);
        if(anterior.isPresent()) {
            if(n(anterior.get(),"cantidad_delta").compareTo(delta)!=0)throw conflicto("El documento ya existe con otra cantidad");
            return anterior.get();
        }
        db.cambiar("INSERT INTO existencia(tenant_id,bodega_id,producto_id,stock) VALUES(?,?,?,0) ON DUPLICATE KEY UPDATE stock=stock",a.tenantId(),bid,pid);
        var stock=db.requerido("SELECT stock FROM existencia WHERE tenant_id=? AND bodega_id=? AND producto_id=? FOR UPDATE",a.tenantId(),bid,pid);
        BigDecimal saldo=limitado(n(stock,"stock").add(delta),MAX_QTY,"stock");
        if(saldo.signum()<0)throw conflicto("El ajuste dejaría existencias negativas");
        String mid=nuevo();
        db.cambiar("UPDATE existencia SET stock=? WHERE tenant_id=? AND bodega_id=? AND producto_id=?",saldo,a.tenantId(),bid,pid);
        db.cambiar("INSERT INTO movimiento_inventario(tenant_id,movimiento_id,bodega_id,producto_id,cantidad_delta,tipo_origen,documento_origen) VALUES(?,?,?,?,?,?,?)",a.tenantId(),mid,bid,pid,delta,tipo,doc);
        return db.requerido("SELECT * FROM movimiento_inventario WHERE tenant_id=? AND movimiento_id=?",a.tenantId(),mid);
    }
    public List<Map<String,Object>> mesas(Actor a) { return db.lista("SELECT * FROM mesa WHERE tenant_id=? ORDER BY codigo LIMIT 200",a.tenantId()); }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> crearMesa(Actor a,Map<String,Object>b) {
        campos(b,"codigo");String mid=nuevo();
        db.cambiar("INSERT INTO mesa(tenant_id,mesa_id,codigo,estado) VALUES(?,?,?,'DISPONIBLE')",a.tenantId(),mid,V.texto(b.get("codigo"),"codigo",64));
        return db.requerido("SELECT * FROM mesa WHERE tenant_id=? AND mesa_id=?",a.tenantId(),mid);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> estadoMesa(Actor a,String mid,Map<String,Object>b) {
        mid=V.uuid(mid,"mesaId");campos(b,"estado");String estado=V.opcion(b.get("estado"),"estado",Set.of("DISPONIBLE","INACTIVA"));
        db.requerido("SELECT mesa_id FROM mesa WHERE tenant_id=? AND mesa_id=? FOR UPDATE",a.tenantId(),mid);
        if(db.existe("SELECT venta_id FROM venta WHERE tenant_id=? AND mesa_id=? AND estado='BORRADOR'",a.tenantId(),mid))throw conflicto("La mesa tiene una venta abierta");
        db.cambiar("UPDATE mesa SET estado=? WHERE tenant_id=? AND mesa_id=?",estado,a.tenantId(),mid);
        return db.requerido("SELECT * FROM mesa WHERE tenant_id=? AND mesa_id=?",a.tenantId(),mid);
    }
    public List<Map<String,Object>> cajas(Actor a) { return db.lista("SELECT * FROM caja WHERE tenant_id=? ORDER BY nombre LIMIT 200",a.tenantId()); }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> crearCaja(Actor a,Map<String,Object>b) {
        campos(b,"nombre");String cid=nuevo();
        db.cambiar("INSERT INTO caja(tenant_id,caja_id,nombre,estado) VALUES(?,?,?,'ACTIVA')",a.tenantId(),cid,V.texto(b.get("nombre"),"nombre",128));
        return db.requerido("SELECT * FROM caja WHERE tenant_id=? AND caja_id=?",a.tenantId(),cid);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> abrirCaja(Actor a,Map<String,Object>b) {
        campos(b,"cajaId","fondoInicial");String cid=id(b,"cajaId");BigDecimal fondo=importe(b,"fondoInicial");
        var c=db.requerido("SELECT * FROM caja WHERE tenant_id=? AND caja_id=? FOR UPDATE",a.tenantId(),cid);
        if(!"ACTIVA".equals(s(c,"estado")))throw conflicto("La caja está inactiva");
        if(db.existe("SELECT sesion_id FROM sesion_caja WHERE tenant_id=? AND caja_id=? AND cerrada_en IS NULL",a.tenantId(),cid))throw conflicto("La caja ya tiene una sesión abierta");
        String sid=nuevo();db.cambiar("INSERT INTO sesion_caja(tenant_id,sesion_id,caja_id,usuario_id,fondo_inicial) VALUES(?,?,?,?,?)",a.tenantId(),sid,cid,a.usuarioId(),fondo);
        return resumenCaja(a,sid);
    }
    public List<Map<String,Object>> sesiones(Actor a) {
        return db.lista("SELECT * FROM sesion_caja WHERE tenant_id=? AND (?=1 OR usuario_id=?) ORDER BY abierta_en DESC LIMIT 200",a.tenantId(),a.tiene("ADMINISTRADOR")?1:0,a.usuarioId());
    }
    public Map<String,Object> resumenCaja(Actor a,String sid) {
        sid=V.uuid(sid,"sesionId");
        var r=db.requerido("SELECT * FROM sesion_caja WHERE tenant_id=? AND sesion_id=?",a.tenantId(),sid);propietario(a,r);
        var tot=db.requerido("SELECT COALESCE(SUM(CASE WHEN p.medio='EFECTIVO' THEN p.importe ELSE 0 END),0) AS efectivo,COALESCE(SUM(CASE WHEN p.medio='EXTERNO' THEN p.importe ELSE 0 END),0) AS externo FROM venta v JOIN pago p ON p.tenant_id=v.tenant_id AND p.venta_id=v.venta_id AND p.estado='CONFIRMADO' WHERE v.tenant_id=? AND v.sesion_caja_id=? AND v.estado='PAGADA'",a.tenantId(),sid);
        var out=new LinkedHashMap<String,Object>(r);out.putAll(tot);out.put("esperado_calculado",moneda(n(r,"fondo_inicial").add(n(tot,"efectivo"))));
        if(r.get("total_contado")!=null)out.put("diferencia",n(r,"total_contado").subtract(n(r,"total_esperado")));
        return out;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> cerrarCaja(Actor a,String sid,Map<String,Object>b) {
        sid=V.uuid(sid,"sesionId");campos(b,"totalContado");BigDecimal contado=importe(b,"totalContado");sesion(a,sid,true);
        // La confirmación no puede cruzar este cierre: espera el mismo bloqueo de sesión.
        var resumen=resumenCaja(a,sid);BigDecimal esperado=(BigDecimal)resumen.get("esperado_calculado");
        db.cambiar("UPDATE sesion_caja SET cerrada_en=?,total_esperado=?,total_contado=? WHERE tenant_id=? AND sesion_id=?",ahora(),esperado,contado,a.tenantId(),sid);
        return resumenCaja(a,sid);
    }
    public List<Map<String,Object>> recetas(Actor a) {
        return db.lista("SELECT r.*,p.codigo,p.nombre FROM receta r JOIN producto p ON p.tenant_id=r.tenant_id AND p.producto_id=r.producto_plato_id WHERE r.tenant_id=? ORDER BY p.codigo,r.version LIMIT 200",a.tenantId());
    }
    public Map<String,Object> receta(Actor a,String rid) {
        rid=V.uuid(rid,"recetaId");var r=new LinkedHashMap<>(db.requerido("SELECT * FROM receta WHERE tenant_id=? AND receta_id=?",a.tenantId(),rid));
        r.put("componentes",db.lista("SELECT d.*,p.codigo,p.nombre,p.unidad FROM detalle_receta d JOIN producto p ON p.tenant_id=d.tenant_id AND p.producto_id=d.producto_insumo_id WHERE d.tenant_id=? AND d.receta_id=? ORDER BY d.linea_no",a.tenantId(),rid));return r;
    }
    @SuppressWarnings("unchecked")
    private static List<Map<String,Object>> filas(Object obj,String campo,int max) {
        if(!(obj instanceof List<?> l)||l.isEmpty()||l.size()>max)throw V.error(campo,"lista de 1 a "+max+" elementos");
        var rows=new ArrayList<Map<String,Object>>();
        for(Object x:l) {
            if(!(x instanceof Map<?,?> m)||m.keySet().stream().anyMatch(k->!(k instanceof String)))throw V.error(campo,"cada elemento debe ser un objeto");
            rows.add((Map<String,Object>)m);
        }return rows;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> crearReceta(Actor a,Map<String,Object>b) {
        campos(b,"productoPlatoId","version","rendimiento","componentes");String plato=id(b,"productoPlatoId");int version=V.entero(b.get("version"),"version",1,1000000);BigDecimal rendimiento=positivo(b,"rendimiento",3);
        var rows=filas(b.get("componentes"),"componentes",50);var ids=new TreeSet<String>();ids.add(plato);var vistos=new HashSet<String>();
        for(var c:rows){campos(c,"productoInsumoId","cantidad");String pid=id(c,"productoInsumoId");positivo(c,"cantidad",3);if(!vistos.add(pid))throw V.error("componentes","no repita un insumo");ids.add(pid);}
        var products=new HashMap<String,Map<String,Object>>();for(String pid:ids)products.put(pid,producto(a,pid,true));
        var p=products.get(plato);if(!si(p,"activo")||!"PLATO".equals(s(p,"tipo"))||!"RECETA".equals(s(p,"modo_descuento")))throw conflicto("El producto debe ser un plato activo con descuento por receta");
        for(String pid:vistos){var i=products.get(pid);if(!si(i,"activo")||!"INSUMO".equals(s(i,"tipo"))||!"DIRECTO".equals(s(i,"modo_descuento")))throw conflicto("Cada componente debe ser un insumo físico activo");}
        String rid=nuevo();db.cambiar("INSERT INTO receta(tenant_id,receta_id,producto_plato_id,version,rendimiento,activa) VALUES(?,?,?,?,?,0)",a.tenantId(),rid,plato,version,rendimiento);
        int no=0;for(var c:rows)db.cambiar("INSERT INTO detalle_receta(tenant_id,receta_id,linea_no,producto_insumo_id,cantidad) VALUES(?,?,?,?,?)",a.tenantId(),rid,++no,id(c,"productoInsumoId"),positivo(c,"cantidad",3));
        return receta(a,rid);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> activarReceta(Actor a,String rid) {
        rid=V.uuid(rid,"recetaId");var r=db.requerido("SELECT * FROM receta WHERE tenant_id=? AND receta_id=?",a.tenantId(),rid);
        var p=producto(a,s(r,"producto_plato_id"),true);
        if(!si(p,"activo")||!"PLATO".equals(s(p,"tipo"))||!"RECETA".equals(s(p,"modo_descuento")))throw conflicto("El plato no admite una receta activa");
        if(!db.existe("SELECT linea_no FROM detalle_receta WHERE tenant_id=? AND receta_id=?",a.tenantId(),rid))throw conflicto("La receta requiere componentes");
        db.cambiar("UPDATE receta SET activa=0 WHERE tenant_id=? AND producto_plato_id=? AND activa=1",a.tenantId(),s(r,"producto_plato_id"));
        db.cambiar("UPDATE receta SET activa=1 WHERE tenant_id=? AND receta_id=?",a.tenantId(),rid);return receta(a,rid);
    }
    public List<Map<String,Object>> ventas(Actor a) {
        return db.lista("SELECT v.*,t.base,t.iva,t.total FROM venta v LEFT JOIN v_total_venta t ON t.tenant_id=v.tenant_id AND t.venta_id=v.venta_id WHERE v.tenant_id=? AND (?=1 OR v.usuario_id=?) ORDER BY v.creada_en DESC,v.venta_id LIMIT 200",a.tenantId(),a.tiene("ADMINISTRADOR")?1:0,a.usuarioId());
    }
    private void ocuparMesa(Actor a,String mid) {
        if(mid==null)return;var m=db.requerido("SELECT * FROM mesa WHERE tenant_id=? AND mesa_id=? FOR UPDATE",a.tenantId(),mid);
        if(!"DISPONIBLE".equals(s(m,"estado")))throw conflicto("La mesa no está disponible");
        db.cambiar("UPDATE mesa SET estado='OCUPADA' WHERE tenant_id=? AND mesa_id=?",a.tenantId(),mid);
    }
    private void liberarMesa(Actor a,String mid) {
        if(mid==null)return;db.requerido("SELECT mesa_id FROM mesa WHERE tenant_id=? AND mesa_id=? FOR UPDATE",a.tenantId(),mid);
        if(!db.existe("SELECT venta_id FROM venta WHERE tenant_id=? AND mesa_id=? AND estado='BORRADOR'",a.tenantId(),mid))db.cambiar("UPDATE mesa SET estado='DISPONIBLE' WHERE tenant_id=? AND mesa_id=?",a.tenantId(),mid);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> crearVenta(Actor a,Map<String,Object>b) {
        campos(b,"bodegaId","sesionCajaId","mesaId","lineas");String bid=id(b,"bodegaId"),sid=id(b,"sesionCajaId"),mid=opcionalId(b,"mesaId");
        sesion(a,sid,true);db.requerido("SELECT bodega_id FROM bodega WHERE tenant_id=? AND bodega_id=?",a.tenantId(),bid);ocuparMesa(a,mid);
        String vid=nuevo();db.cambiar("INSERT INTO venta(tenant_id,venta_id,usuario_id,bodega_id,mesa_id,sesion_caja_id,estado) VALUES(?,?,?,?,?,?,'BORRADOR')",a.tenantId(),vid,a.usuarioId(),bid,mid,sid);
        escribirDetalle(a,vid,b.get("lineas"));return comprobante(a,vid);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> editarDetalle(Actor a,String vid,Map<String,Object>b) {
        vid=V.uuid(vid,"ventaId");campos(b,"lineas");borrador(bloquearVenta(a,vid,true));escribirDetalle(a,vid,b.get("lineas"));return comprobante(a,vid);
    }
    private void escribirDetalle(Actor a,String vid,Object obj) {
        var rows=filas(obj,"lineas",100);var ids=new TreeSet<String>();
        for(var line:rows){campos(line,"productoId","cantidad");if(!ids.add(id(line,"productoId")))throw V.error("lineas","no repita un producto; sume su cantidad");V.entero(line.get("cantidad"),"cantidad",1,1000000);}
        // Prelectura de versiones inmutables para conocer todos los UUID antes de bloquear.
        var candidatas=new HashMap<String,Map<String,Object>>();
        var componentes=new HashMap<String,List<Map<String,Object>>>();
        var todos=new TreeSet<String>(ids);
        for(String pid:ids){var p=producto(a,pid,false);
            if("RECETA".equals(s(p,"modo_descuento"))){var r=db.requerido("SELECT * FROM receta WHERE tenant_id=? AND producto_plato_id=? AND activa=1",a.tenantId(),pid);String rid=s(r,"receta_id");candidatas.put(pid,r);var cs=db.lista("SELECT * FROM detalle_receta WHERE tenant_id=? AND receta_id=? ORDER BY linea_no",a.tenantId(),rid);if(cs.isEmpty())throw conflicto("La receta requiere componentes");componentes.put(rid,cs);for(var c:cs)todos.add(s(c,"producto_insumo_id"));}
        }
        var products=new HashMap<String,Map<String,Object>>();for(String pid:todos)products.put(pid,producto(a,pid,true));
        var demanda=new TreeMap<String,BigDecimal>();
        var recetaLinea=new HashMap<String,String>();
        for(var line:rows){String pid=id(line,"productoId");var p=products.get(pid);BigDecimal qty=new BigDecimal(V.entero(line.get("cantidad"),"cantidad",1,1000000));
            if(!si(p,"activo")||"INSUMO".equals(s(p,"tipo")))throw conflicto("El producto no está disponible para venta");
            if("DIRECTO".equals(s(p,"modo_descuento")))demanda.merge(pid,qty,BigDecimal::add);
            else {
                var candidate=candidatas.get(pid);if(candidate==null)throw conflicto("La modalidad cambió durante la revisión; vuelva a guardar el detalle");
                var actual=db.requerido("SELECT * FROM receta WHERE tenant_id=? AND producto_plato_id=? AND activa=1 FOR UPDATE",a.tenantId(),pid);
                String rid=s(candidate,"receta_id");if(!rid.equals(s(actual,"receta_id")))throw conflicto("La receta cambió durante la revisión; vuelva a guardar el detalle");
                recetaLinea.put(pid,rid);
                for(var c:componentes.get(rid)){String insumo=s(c,"producto_insumo_id");var pi=products.get(insumo);if(!si(pi,"activo")||!"INSUMO".equals(s(pi,"tipo"))||!"DIRECTO".equals(s(pi,"modo_descuento")))throw conflicto("Un componente no es un insumo físico activo");BigDecimal need=limitado(n(c,"cantidad").multiply(qty).divide(n(actual,"rendimiento"),3,RoundingMode.CEILING),MAX_QTY,"consumo");demanda.merge(insumo,need,BigDecimal::add);}
            }
        }
        String bid=s(db.requerido("SELECT bodega_id FROM venta WHERE tenant_id=? AND venta_id=?",a.tenantId(),vid),"bodega_id");
        for(var d:demanda.entrySet()){limitado(d.getValue(),MAX_QTY,"demanda");var stock=db.uno("SELECT stock FROM existencia WHERE tenant_id=? AND bodega_id=? AND producto_id=? FOR UPDATE",a.tenantId(),bid,d.getKey()).orElseThrow(()->conflicto("No hay existencias para un componente requerido"));if(n(stock,"stock").compareTo(d.getValue())<0)throw conflicto("Existencias insuficientes para agregar la demanda conjunta al detalle");}
        // Se verifica disponibilidad al añadir, sin reservar ni descontar. El pago la revalida.
        db.cambiar("DELETE FROM detalle_venta WHERE tenant_id=? AND venta_id=?",a.tenantId(),vid);
        int no=0;
        for(var line:rows){String pid=id(line,"productoId");var p=products.get(pid);db.cambiar("INSERT INTO detalle_venta(tenant_id,venta_id,linea_no,producto_id,receta_id,cantidad,precio_unitario,tasa_iva) VALUES(?,?,?,?,?,?,?,?)",a.tenantId(),vid,++no,pid,recetaLinea.get(pid),new BigDecimal(V.entero(line.get("cantidad"),"cantidad",1,1000000)),n(p,"precio_vigente"),n(p,"tasa_iva"));}
        totales(db.lista("SELECT * FROM detalle_venta WHERE tenant_id=? AND venta_id=? ORDER BY linea_no",a.tenantId(),vid));
    }
    private static Map<String,BigDecimal> totales(List<Map<String,Object>> rows) {
        BigDecimal base=ZERO,iva=ZERO;
        for(var l:rows){BigDecimal bl=moneda(n(l,"cantidad").multiply(n(l,"precio_unitario")));base=moneda(base.add(bl));iva=moneda(iva.add(moneda(bl.multiply(n(l,"tasa_iva")))));}
        return Map.of("base",base,"iva",iva,"total",moneda(base.add(iva)));
    }
    public Map<String,Object> comprobante(Actor a,String vid) {
        vid=V.uuid(vid,"ventaId");var r=db.requerido("SELECT * FROM venta WHERE tenant_id=? AND venta_id=?",a.tenantId(),vid);propietario(a,r);
        var rows=db.lista("SELECT d.*,p.codigo,p.nombre,p.unidad FROM detalle_venta d JOIN producto p ON p.tenant_id=d.tenant_id AND p.producto_id=d.producto_id WHERE d.tenant_id=? AND d.venta_id=? ORDER BY d.linea_no",a.tenantId(),vid);
        var out=new LinkedHashMap<String,Object>(r);out.put("lineas",rows);out.putAll(totales(rows));out.put("pagos",db.lista("SELECT pago_id,referencia,importe,estado,medio,confirmado_en FROM pago WHERE tenant_id=? AND venta_id=? ORDER BY pago_id",a.tenantId(),vid));out.put("naturaleza","Comprobante académico; no constituye facturación electrónica DIAN");return out;
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> anular(Actor a,String vid) {
        vid=V.uuid(vid,"ventaId");var v=bloquearVenta(a,vid,false);borrador(v);db.cambiar("UPDATE venta SET estado='ANULADA' WHERE tenant_id=? AND venta_id=?",a.tenantId(),vid);liberarMesa(a,s(v,"mesa_id"));return comprobante(a,vid);
    }
    private record Consumo(String producto,int linea,String receta,int componente,BigDecimal cantidad) {}
    private static String huella(String vid,String medio,String referencia,BigDecimal importe) {
        try {return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((vid+"\n"+medio+"\n"+referencia+"\n"+importe.toPlainString()).getBytes(StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    private Optional<Map<String,Object>> repeticion(Actor a,String key,String hash,String vid) {
        var r=db.uno("SELECT * FROM operacion_confirmacion WHERE tenant_id=? AND clave_idempotencia=?",a.tenantId(),key);
        if(r.isEmpty())return Optional.empty();
        if(!vid.equals(s(r.get(),"venta_id"))||!hash.equals(s(r.get(),"huella_solicitud")))throw conflicto("La clave de idempotencia ya corresponde a otra solicitud");
        var comp=comprobante(a,vid);if(!"PAGADA".equals(s(comp,"estado")))throw conflicto("El registro de confirmación no corresponde a una venta pagada");return Optional.of(comp);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> confirmar(Actor a,String vid,String key,Map<String,Object>b) {
        vid=V.uuid(vid,"ventaId");campos(b,"medio","referencia","importe");String medio=V.opcion(b.get("medio"),"medio",Set.of("EFECTIVO","EXTERNO"));String ref=V.texto(b.get("referencia"),"referencia",64);BigDecimal monto=positivo(b,"importe",2);
        key=V.texto(key,"Idempotency-Key",128).toLowerCase(Locale.ROOT);if(!key.matches("[a-z0-9._:-]{1,128}"))throw V.error("Idempotency-Key","use letras ASCII, números, punto, guion, dos puntos o guion bajo");
        String hash=huella(vid,medio,ref,monto);var repeat=repeticion(a,key,hash,vid);if(repeat.isPresent())return repeat.get();
        var v=bloquearVenta(a,vid,false);repeat=repeticion(a,key,hash,vid);if(repeat.isPresent())return repeat.get();
        borrador(v);sesion(a,s(v,"sesion_caja_id"),true);
        if(v.get("mesa_id")!=null)db.requerido("SELECT mesa_id FROM mesa WHERE tenant_id=? AND mesa_id=? FOR UPDATE",a.tenantId(),s(v,"mesa_id"));
        var rows=db.lista("SELECT * FROM detalle_venta WHERE tenant_id=? AND venta_id=? ORDER BY linea_no FOR UPDATE",a.tenantId(),vid);if(rows.isEmpty())throw conflicto("La venta requiere al menos una línea");
        var totals=totales(rows);if(monto.compareTo(totals.get("total"))!=0)throw conflicto("El importe debe coincidir exactamente con el total calculado");
        var productIds=new TreeSet<String>();var recipeIds=new TreeSet<String>();
        for(var line:rows){productIds.add(s(line,"producto_id"));if(line.get("receta_id")!=null)recipeIds.add(s(line,"receta_id"));}
        // Las versiones de receta son inmutables; sus insumos se conocen antes de ordenar todos los bloqueos de producto.
        var compByRecipe=new HashMap<String,List<Map<String,Object>>>();
        for(String rid:recipeIds){var comp=db.lista("SELECT * FROM detalle_receta WHERE tenant_id=? AND receta_id=? ORDER BY linea_no",a.tenantId(),rid);if(comp.isEmpty())throw conflicto("La receta no tiene componentes");compByRecipe.put(rid,comp);for(var c:comp)productIds.add(s(c,"producto_insumo_id"));}
        var products=new HashMap<String,Map<String,Object>>();for(String pid:productIds)products.put(pid,producto(a,pid,true));
        var recipes=new HashMap<String,Map<String,Object>>();for(String rid:recipeIds)recipes.put(rid,db.requerido("SELECT * FROM receta WHERE tenant_id=? AND receta_id=? FOR UPDATE",a.tenantId(),rid));
        var consumos=new ArrayList<Consumo>();var demanda=new TreeMap<String,BigDecimal>();
        for(var line:rows){String pid=s(line,"producto_id");var p=products.get(pid);int no=((Number)line.get("linea_no")).intValue();BigDecimal qty=n(line,"cantidad");
            if(!si(p,"activo")||"INSUMO".equals(s(p,"tipo")))throw conflicto("Hay productos inactivos o no vendibles");
            if(qty.stripTrailingZeros().scale()>0 || qty.signum()<=0)throw conflicto("La cantidad vendida debe ser entera positiva");
            if(n(p,"precio_vigente").compareTo(n(line,"precio_unitario"))!=0 || n(p,"tasa_iva").compareTo(n(line,"tasa_iva"))!=0)throw conflicto("Cambió el precio o IVA; revise y actualice el detalle antes de confirmar");
            String rid=s(line,"receta_id");
            if("DIRECTO".equals(s(p,"modo_descuento"))){if(rid!=null)throw conflicto("La modalidad del producto cambió");consumos.add(new Consumo(pid,no,null,0,qty));demanda.merge(pid,qty,BigDecimal::add);}
            else {
                var r=recipes.get(rid);if(r==null||!si(r,"activa")||!pid.equals(s(r,"producto_plato_id")))throw conflicto("La versión de receta ya no está activa para el plato");
                for(var c:compByRecipe.get(rid)){String insumo=s(c,"producto_insumo_id");var pi=products.get(insumo);if(!si(pi,"activo")||!"INSUMO".equals(s(pi,"tipo"))||!"DIRECTO".equals(s(pi,"modo_descuento")))throw conflicto("Un componente no es un insumo físico activo");
                    BigDecimal need=limitado(n(c,"cantidad").multiply(qty).divide(n(r,"rendimiento"),3,RoundingMode.CEILING),MAX_QTY,"consumo");
                    consumos.add(new Consumo(insumo,no,rid,((Number)c.get("linea_no")).intValue(),need));demanda.merge(insumo,need,BigDecimal::add);
                }
            }
        }
        // Demanda conjunta por insumo: ninguna línea puede reutilizar el saldo validado por otra.
        var saldos=new TreeMap<String,BigDecimal>();String bid=s(v,"bodega_id");
        for(var entry:demanda.entrySet()){limitado(entry.getValue(),MAX_QTY,"demanda");var e=db.uno("SELECT stock FROM existencia WHERE tenant_id=? AND bodega_id=? AND producto_id=? FOR UPDATE",a.tenantId(),bid,entry.getKey()).orElseThrow(()->conflicto("No hay existencias para un componente requerido"));BigDecimal saldo=n(e,"stock").subtract(entry.getValue());if(saldo.signum()<0)throw conflicto("Existencias insuficientes para la demanda conjunta");saldos.put(entry.getKey(),saldo);}
        try {db.cambiar("INSERT INTO operacion_confirmacion(tenant_id,clave_idempotencia,venta_id,huella_solicitud) VALUES(?,?,?,?)",a.tenantId(),key,vid,hash);}catch(DuplicateKeyException e){throw conflicto("La clave de idempotencia ya fue utilizada por otra solicitud");}
        for(var e:saldos.entrySet())db.cambiar("UPDATE existencia SET stock=? WHERE tenant_id=? AND bodega_id=? AND producto_id=?",e.getValue(),a.tenantId(),bid,e.getKey());
        for(var c:consumos)db.cambiar("INSERT INTO movimiento_inventario(tenant_id,movimiento_id,bodega_id,producto_id,venta_id,linea_no,receta_id,componente_no,cantidad_delta,tipo_origen,documento_origen) VALUES(?,?,?,?,?,?,?,?,?,?,?)",a.tenantId(),nuevo(),bid,c.producto(),vid,c.linea(),c.receta(),c.componente(),c.cantidad().negate(),c.receta()==null?"VENTA_DIRECTA":"RECETA",vid+":"+c.linea());
        LocalDateTime fecha=ahora();
        db.cambiar("INSERT INTO pago(tenant_id,pago_id,venta_id,referencia,importe,estado,confirmado_en,medio) VALUES(?,?,?,?,?,'CONFIRMADO',?,?)",a.tenantId(),nuevo(),vid,ref,monto,fecha,medio);
        db.cambiar("UPDATE venta SET estado='PAGADA',numero_factura=?,pagada_en=? WHERE tenant_id=? AND venta_id=?","ACA-"+vid,fecha,a.tenantId(),vid);
        db.cambiar("INSERT INTO evento_integracion(tenant_id,evento_id,tipo,venta_id,estado) VALUES(?,?,'VENTA_CONFIRMADA',?,'PENDIENTE')",a.tenantId(),nuevo(),vid);
        liberarMesa(a,s(v,"mesa_id"));return comprobante(a,vid);
    }
}
