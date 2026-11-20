package co.edu.sena.perfumeria.servicio;
import co.edu.sena.perfumeria.nucleo.*;
import co.edu.sena.perfumeria.dominio.*;
import static co.edu.sena.perfumeria.nucleo.Datos.*;
import java.util.*;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class CajaService {
    private final Db db;
    private final InventarioService inv;
    private final VentasService ventas;
    public static final List<String> MEDIOS=List.of("EFECTIVO","NEQUI","DAVIPLATA","TARJETA");
    public CajaService(Db d,InventarioService i,VentasService v) {
        db=d;
        inv=i;
        ventas=v;
    }
    private Map<String,Object> turno(Actor a,String id,boolean lock) {
        var t=db.requerido("SELECT * FROM turno_caja WHERE turno_id=?"+(lock?" FOR UPDATE":""),id);
        a.exigirPropio(t.get("responsable_usuario_id").toString());
        t.put("responsable",db.requerido("SELECT nombre_acceso FROM usuario WHERE usuario_id=?",t.get("responsable_usuario_id")).get("nombre_acceso"));
        return t;
    }
    public void abierto(Actor a,String id) {
        var t=turno(a,id,true);
        conflicto(t.get("cerrada_en")!=null,"El turno ya está cerrado");
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> abrir(Actor a,Map<String,Object> m) {
        String caja=refOpcional(m,"caja_id");
        if(caja==null)caja=db.requerido("SELECT caja_id FROM caja WHERE activa=true ORDER BY nombre LIMIT 1").get("caja_id").toString();
        db.requerido("SELECT caja_id FROM caja WHERE caja_id=? AND activa=true FOR UPDATE",caja);
        conflicto(db.existe("SELECT turno_id FROM turno_caja WHERE cerrada_en IS NULL AND (caja_id=? OR responsable_usuario_id=?)",caja,a.usuarioId()),"Ya existe turno abierto para caja o responsable");
        String id=id();
        db.cambiar("INSERT INTO turno_caja(turno_id,caja_id,responsable_usuario_id,abierta_en,fondo_inicial_cop) VALUES (?,?,?,UTC_TIMESTAMP(6),?)",id,caja,a.usuarioId(),dinero(m,"fondo_cop",true));
        return Map.of("id",id);
    }
    public List<Map<String,Object>> turnos(Actor a) {
        return db.lista("SELECT t.*,u.nombre_acceso responsable,c.nombre caja FROM turno_caja t JOIN usuario u ON u.usuario_id=t.responsable_usuario_id JOIN caja c ON c.caja_id=t.caja_id "+(a.admin()?"":"WHERE t.responsable_usuario_id=? ")+"ORDER BY t.abierta_en DESC LIMIT 500",a.admin()?new Object[] {
        }
        :new Object[] {
            a.usuarioId()
        }
        );
    }
    public Map<String,Object> detalle(Actor a,String id) {
        var r=turno(a,id,false);
        var totals=new LinkedHashMap<String,BigDecimal>();
        for(String medio:MEDIOS)totals.put(medio,n(db.requerido("SELECT COALESCE(SUM(importe_cop),0)n FROM cobro WHERE turno_id=? AND medio=?",id,medio).get("n")));
        var mov=db.lista("SELECT * FROM movimiento_caja WHERE turno_id=? ORDER BY registrado_en",id);
        BigDecimal delta=BigDecimal.ZERO;
        for(var x:mov)delta=delta.add(n(x.get("importe_cop")).multiply(x.get("sentido").equals("ENTRADA")?BigDecimal.ONE:BigDecimal.ONE.negate()));
        r.put("totales_por_medio",totals);
        r.put("esperado_efectivo_cop",n(r.get("fondo_inicial_cop")).add(totals.get("EFECTIVO")).add(delta));
        r.put("movimientos",mov);
        r.put("cobros",db.lista("SELECT * FROM cobro WHERE turno_id=? ORDER BY confirmado_en",id));
        var conc=db.lista("SELECT * FROM conciliacion_turno WHERE turno_id=?",id);
        for(var c:conc) {
            BigDecimal esperado=c.get("medio").equals("EFECTIVO")?n(r.get("esperado_efectivo_cop")):totals.get(c.get("medio").toString());
            c.put("esperado_cop",esperado);
            c.put("diferencia_cop",n(c.get("importe_verificado_cop")).subtract(esperado));
        }
        r.put("conciliaciones",conc);
        return r;
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> cerrar(Actor a,String id,Map<String,Object> m) {
        abierto(a,id);
        var cc=filas(m,"conciliaciones");
        conflicto(cc.size()!=4||cc.stream().map(c->op(c,"medio",MEDIOS.toArray(String[]::new))).distinct().count()!=4,"Se requieren conciliaciones de los cuatro medios");
        for(var c:cc)db.cambiar("INSERT INTO conciliacion_turno VALUES (?,?,?,?)",id,op(c,"medio",MEDIOS.toArray(String[]::new)),dinero(c,"importe_verificado_cop",true),opcional(c,"soporte_privado_ref"));
        db.cambiar("UPDATE turno_caja SET cerrada_en=UTC_TIMESTAMP(6),observacion_cierre=? WHERE turno_id=?",opcional(m,"observacion"),id);
        return detalle(a,id);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> cobrar(Actor a,String venta,Map<String,Object> m) {
        String key=texto(m,"clave_operacion"),hash=hash(Map.of("venta_id",venta,"datos",m));
        var old=inv.repetida("cobro","cobro_id",key,hash);
        if(old.isPresent()) {
            ventas.propia(a,venta,false);
            return Map.of("id",old.get());
        }
        var v=ventas.propia(a,venta,true);
        conflicto(!v.get("estado").equals("EMITIDA"),"Emita la venta antes de cobrar");
        String turno=ref(m,"turno_id");
        abierto(a,turno);
        String medio=op(m,"medio",MEDIOS.toArray(String[]::new)),referencia=opcional(m,"referencia_externa");
        conflicto(!bool(m,"confirmado"),"El cobro requiere confirmación humana de recepción");
        conflicto(!medio.equals("EFECTIVO")&&referencia==null,"Medio electrónico requiere referencia verificable");
        BigDecimal importe=dinero(m,"importe_cop",false),saldo=n(db.requerido("SELECT saldo FROM v_total_venta WHERE venta_id=?",venta).get("saldo"));
        conflicto(importe.compareTo(saldo)>0,"El cobro supera el saldo pendiente");
        String id=id();
        db.cambiar("INSERT INTO cobro VALUES (?,?,?,?,?,?,UTC_TIMESTAMP(6),?,?,?,?)",id,venta,turno,a.usuarioId(),medio,importe,referencia,opcional(m,"soporte_privado_ref"),key,hash);
        return Map.of("id",id);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> gasto(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        String key=texto(m,"clave_operacion"),hash=hash(m);
        var old=inv.repetida("gasto","gasto_id",key,hash);
        if(old.isPresent())return Map.of("id",old.get());
        String turno=refOpcional(m,"turno_id"),medio=op(m,"medio",MEDIOS.toArray(String[]::new));
        if(turno!=null)abierto(a,turno);
        String id=id(),doc=texto(m,"documento_ref"),concepto=texto(m,"concepto");
        BigDecimal val=dinero(m,"importe_cop",false);
        db.cambiar("INSERT INTO gasto(gasto_id,fecha,concepto,importe_cop,proveedor_id,soporte_ref,registrado_por_usuario_id,medio,clave_operacion,huella_solicitud) VALUES (?,?,?,?,?,?,?,?,?,?)",id,V.fecha(m.get("fecha"),"fecha"),concepto,val,refOpcional(m,"proveedor_id"),doc,a.usuarioId(),medio,key,hash);
        if(turno!=null&&medio.equals("EFECTIVO"))db.cambiar("INSERT INTO movimiento_caja(movimiento_caja_id,turno_id,sentido,importe_cop,motivo,gasto_id,registrado_por_usuario_id,registrado_en) VALUES (?,?,'SALIDA',?,?,?,?,UTC_TIMESTAMP(6))",id(),turno,val,concepto,id,a.usuarioId());
        return Map.of("id",id);
    }
    public List<Map<String,Object>> gastos(Actor a) {
        a.exigirAdmin();
        return db.lista("SELECT * FROM gasto ORDER BY fecha DESC LIMIT 500");
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> incidencia(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        String key=texto(m,"clave_operacion"),hash=hash(m);
        var old=inv.repetida("incidencia","incidencia_id",key,hash);
        if(old.isPresent())return Map.of("id",old.get());
        String tipo=op(m,"tipo","MERMA","MUESTRA","REPOSICION","DEVOLUCION","ERROR_MEZCLA","CAMBIO_ENVASE","REPOSICION_LIQUIDO");
        String venta=refOpcional(m,"venta_id"),motivo=texto(m,"motivo");
        Integer no=m.get("linea_no")==null?null:unidades(m,"linea_no");
        conflicto((venta==null)!=(no==null),"Origen de incidencia requiere venta y línea conjuntamente");
        Map<String,Object> origen=venta==null?null:ventas.propia(a,venta,true);
        List<Map<String,Object>> lines=m.get("lineas")instanceof List<?> ll&&!ll.isEmpty()?filas(m,"lineas"):List.of();
        conflicto(lines.stream().map(l->ref(l,"articulo_id")).distinct().count()!=lines.size(),"Un renglón por artículo en cada incidencia");
        inv.bloquear(lines.stream().map(l->ref(l,"articulo_id")).toList());
        Map<String,BigDecimal> costos=new HashMap<>();
        for(var l:lines) {
            String ar=ref(l,"articulo_id");
            BigDecimal q=cant(l,"cantidad_base");
            String dir=op(l,"direccion","SALIDA","ENTRADA");
            conflicto(Set.of("MERMA","MUESTRA").contains(tipo)&&dir.equals("ENTRADA"),"Merma y muestra sólo permiten salidas de inventario");
            var stock=inv.saldo(ar);
            if(dir.equals("SALIDA")) {
                inv.exigirValoracion(ar);
                conflicto(stock.saldo().compareTo(q)<0,"La incidencia supera las existencias");
                costos.put(ar,stock.unitario());
            }
            else {
                Object costo=l.get("costo_unitario_base_cop");
                if(venta!=null) {
                    var hist=db.requerido("SELECT -SUM(v.cantidad_base) cantidad,SUM(-v.cantidad_base*v.costo_unitario_base_cop)/NULLIF(-SUM(v.cantidad_base),0) costo FROM v_movimiento_base v JOIN documento_inventario d ON d.documento_id=v.documento_id WHERE d.tipo='SUMINISTRO' AND d.venta_id=? AND d.linea_no=? AND v.articulo_id=?",venta,no,ar);
                    var prev=db.requerido("SELECT COALESCE(SUM(v.cantidad_base),0) n FROM v_movimiento_base v JOIN documento_inventario d ON d.documento_id=v.documento_id JOIN incidencia i ON i.incidencia_id=d.incidencia_id WHERE i.venta_id=? AND i.linea_no=? AND v.articulo_id=? AND v.cantidad_base>0",venta,no,ar);
                    conflicto(q.add(n(prev.get("n"))).compareTo(n(hist.get("cantidad")))>0,"No se puede devolver más material del suministrado");
                    conflicto(costo!=null&&n(costo).compareTo(n(hist.get("costo")))!=0,"El material retornado conserva el costo histórico del suministro");
                    costo=hist.get("costo");
                }
                conflicto(costo==null,"La entrada requiere valoración explícita o costo del suministro de origen");
                costos.put(ar,V.decimal(costo,"costo_unitario_base_cop",8,BigDecimal.ZERO,false));
            }
        }
        BigDecimal devolucion=m.get("devolucion_cop")==null?null:dinero(m,"devolucion_cop",false);
        String turno=null;
        if(devolucion!=null) {
            conflicto(!tipo.equals("DEVOLUCION")||origen==null||!origen.get("estado").equals("EMITIDA"),"La devolución monetaria requiere venta emitida y tipo DEVOLUCION");
            BigDecimal cobrado=n(db.requerido("SELECT cobrado FROM v_total_venta WHERE venta_id=?",venta).get("cobrado"));
            BigDecimal devuelto=n(db.requerido("SELECT COALESCE(SUM(mc.importe_cop),0)n FROM movimiento_caja mc JOIN incidencia i ON i.incidencia_id=mc.incidencia_id WHERE i.venta_id=?",venta).get("n"));
            conflicto(devolucion.compareTo(cobrado.subtract(devuelto))>0,"La devolución supera lo cobrado que aún no se devolvió");
            turno=ref(m,"turno_id");
            abierto(a,turno);
        }
        String id=id();
        db.cambiar("INSERT INTO incidencia VALUES (?,?,?,?,?,UTC_TIMESTAMP(6),?,?,?)",id,tipo,venta,no,motivo,a.usuarioId(),key,hash);
        if(!lines.isEmpty()) {
            String doc=inv.documento(a,"CONSUMO_NO_VENTA",key,hash,null,null,null,null,id,motivo);
            int i=0;
            for(var l:lines) {
                Articulo ar=inv.articulo(ref(l,"articulo_id"));
                BigDecimal q=cant(l,"cantidad_base").multiply(l.get("direccion").equals("SALIDA")?BigDecimal.ONE.negate():BigDecimal.ONE);
                inv.renglon(doc,++i,ar,q,costos.get(ar.id()));
            }
        }
        if(devolucion!=null)db.cambiar("INSERT INTO movimiento_caja(movimiento_caja_id,turno_id,sentido,importe_cop,motivo,gasto_id,registrado_por_usuario_id,registrado_en,incidencia_id) VALUES (?,?,'SALIDA',?,?,NULL,?,UTC_TIMESTAMP(6),?)",id(),turno,devolucion,motivo,a.usuarioId(),id);
        return Map.of("id",id);
    }
    public List<Map<String,Object>> incidencias(Actor a) {
        a.exigirAdmin();
        return db.lista("SELECT * FROM incidencia ORDER BY registrada_en DESC LIMIT 500");
    }
}
