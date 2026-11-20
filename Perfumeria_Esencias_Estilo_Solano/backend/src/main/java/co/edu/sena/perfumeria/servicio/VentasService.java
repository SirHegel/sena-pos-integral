package co.edu.sena.perfumeria.servicio;
import co.edu.sena.perfumeria.nucleo.*;
import co.edu.sena.perfumeria.dominio.*;
import static co.edu.sena.perfumeria.nucleo.Datos.*;
import java.util.*;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class VentasService {
    private final Db db;
    private final CatalogoService catalogo;
    private final InventarioService inv;
    public VentasService(Db d,CatalogoService c,InventarioService i) {
        db=d;
        catalogo=c;
        inv=i;
    }
    public Map<String,Object> propia(Actor a,String id,boolean lock) {
        var v=db.requerido("SELECT * FROM venta WHERE venta_id=?"+(lock?" FOR UPDATE":""),id);
        a.exigirPropio(v.get("vendedora_usuario_id").toString());
        return v;
    }
    public Venta dominio(String id) {
        var lines=db.lista("SELECT * FROM linea_venta WHERE venta_id=? ORDER BY linea_no",id).stream().map(l->new LineaVenta(n(l.get("linea_no")).intValueExact(),catalogo.especificacion(l.get("especificacion_id").toString()),n(l.get("cantidad_unidades")).intValueExact(),n(l.get("precio_unitario_cop")))).toList();
        BigDecimal desc=n(db.uno("SELECT importe_cop FROM descuento_autorizado WHERE venta_id=?",id).map(x->x.get("importe_cop")).orElse(BigDecimal.ZERO));
        try {
            return new Venta(id,lines,desc);
        }
        catch(IllegalArgumentException e) {
            throw V.error("venta",e.getMessage());
        }
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> crear(Actor a,Map<String,Object> m) {
        String id=id(),client=refOpcional(m,"cliente_id");
        if(client!=null)db.requerido("SELECT cliente_id FROM cliente WHERE cliente_id=? AND activo=true",client);
        var lines=filas(m,"lineas");
        db.cambiar("INSERT INTO venta(venta_id,cliente_id,vendedora_usuario_id,creada_en,estado) VALUES (?,?,?,UTC_TIMESTAMP(6),'BORRADOR')",id,client,a.usuarioId());
        int i=0;
        for(var l:lines) {
            String e=ref(l,"especificacion_id");
            catalogo.especificacion(e);
            var o=db.requerido("SELECT o.nombre_venta,p.importe_unitario_cop FROM especificacion_suministro e JOIN oferta o ON o.oferta_id=e.oferta_id JOIN precio_oferta p ON p.oferta_id=o.oferta_id AND p.vigente_hasta IS NULL WHERE e.especificacion_id=?",e);
            db.cambiar("INSERT INTO linea_venta VALUES (?,?,?,?,?,?)",id,++i,e,unidades(l,"unidades"),o.get("importe_unitario_cop"),o.get("nombre_venta"));
        }
        dominio(id);
        return Map.of("id",id);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> descuento(Actor a,String id,Map<String,Object> m) {
        a.exigirAdmin();
        var v=propia(a,id,true);
        conflicto(!v.get("estado").equals("BORRADOR"),"Sólo se autoriza descuento antes de emitir");
        BigDecimal amount=dinero(m,"importe_cop",false),bruto=n(db.requerido("SELECT bruto FROM v_total_venta WHERE venta_id=?",id).get("bruto"));
        conflicto(amount.compareTo(bruto)>=0,"La rebaja debe ser menor al importe de la venta");
        db.cambiar("INSERT INTO descuento_autorizado VALUES (?,?,?,UTC_TIMESTAMP(6),?) ON DUPLICATE KEY UPDATE importe_cop=VALUES(importe_cop),autorizado_por_usuario_id=VALUES(autorizado_por_usuario_id),autorizado_en=VALUES(autorizado_en),motivo=VALUES(motivo)",id,amount,a.usuarioId(),texto(m,"motivo"));
        return detalle(a,id);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> emitir(Actor a,String id,Map<String,Object> m) {
        var row=propia(a,id,true);
        String key=texto(m,"clave_operacion"),hash=hash(m);
        if(row.get("estado").equals("EMITIDA")) {
            conflicto(!key.equals(row.get("clave_emision"))||!hash.equals(row.get("huella_emision")),"La venta ya fue emitida con otro contenido");
            return detalle(a,id);
        }
        conflicto(!row.get("estado").equals("BORRADOR"),"La venta no admite emisión");
        boolean credito=bool(m,"credito");
        if(credito) {
            a.exigirAdmin();
            conflicto(row.get("cliente_id")==null,"El fiado requiere cliente identificado");
            var fecha=V.fecha(m.get("fecha_vencimiento"),"fecha_vencimiento");
            conflicto(fecha.isBefore(java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota"))),"Vencimiento anterior al día de emisión");
            db.cambiar("INSERT INTO credito_venta VALUES (?,?,?)",id,fecha,texto(m,"condicion_pactada"));
        }
        Venta v=dominio(id);
        Map<Articulo,BigDecimal> demand=v.demanda();
        inv.bloquear(demand.keySet().stream().map(Articulo::id).toList());
        Map<String,BigDecimal> costos=new HashMap<>();
        for(var d:demand.entrySet()) {
            inv.exigirValoracion(d.getKey().id());
            var s=inv.saldo(d.getKey().id());
            conflicto(s.saldo().compareTo(d.getValue())<0,"Existencias insuficientes: "+d.getKey().nombre());
            costos.put(d.getKey().id(),s.unitario());
        }
        for(var l:v.lineas()) {
            String doc=inv.documento(a,"SUMINISTRO","VENTA:"+id+":"+l.numero(),hash,id,l.numero(),null,null,null,"Entrega de venta");
            int i=0;
            for(var c:l.especificacion().consumo(l.unidades()).entrySet())inv.renglon(doc,++i,c.getKey(),c.getValue().negate(),costos.get(c.getKey().id()));
        }
        db.cambiar("UPDATE venta SET estado='EMITIDA',emitida_en=UTC_TIMESTAMP(6),numero_recibo=?,clave_emision=?,huella_emision=? WHERE venta_id=?","PS-"+id,key,hash,id);
        return detalle(a,id);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> anular(Actor a,String id) {
        var r=propia(a,id,true);
        conflicto(!r.get("estado").equals("BORRADOR"),"Sólo se anulan borradores; las ventas emitidas conservan historia");
        db.cambiar("UPDATE venta SET estado='ANULADA_BORRADOR' WHERE venta_id=?",id);
        return detalle(a,id);
    }
    public List<Map<String,Object>> listar(Actor a) {
        return db.lista("SELECT v.*,t.total total_cop,t.cobrado cobrado_cop,t.saldo saldo_cop,c.nombre cliente,u.nombre_acceso vendedora FROM venta v JOIN v_total_venta t ON t.venta_id=v.venta_id LEFT JOIN cliente c ON c.cliente_id=v.cliente_id JOIN usuario u ON u.usuario_id=v.vendedora_usuario_id "+(a.admin()?"":"WHERE v.vendedora_usuario_id=? ")+"ORDER BY v.creada_en DESC LIMIT 500",a.admin()?new Object[] {
        }
        :new Object[] {
            a.usuarioId()
        }
        );
    }
    public Map<String,Object> detalle(Actor a,String id) {
        var r=propia(a,id,false);
        var t=db.requerido("SELECT * FROM v_total_venta WHERE venta_id=?",id);
        r.put("total_cop",t.get("total"));
        r.put("bruto_cop",t.get("bruto"));
        r.put("descuento_cop",t.get("descuento"));
        r.put("cobrado_cop",t.get("cobrado"));
        r.put("saldo_cop",t.get("saldo"));
        r.put("estado_pago",n(t.get("cobrado")).signum()==0?"SIN_PAGO":n(t.get("saldo")).signum()==0?"PAGADA":"PARCIAL");
        r.put("lineas",db.lista("SELECT l.*,o.modalidad,o.volumen_ml,o.fragancia_id FROM linea_venta l JOIN especificacion_suministro e ON e.especificacion_id=l.especificacion_id JOIN oferta o ON o.oferta_id=e.oferta_id WHERE l.venta_id=? ORDER BY l.linea_no",id));
        r.put("cobros",db.lista("SELECT * FROM cobro WHERE venta_id=? ORDER BY confirmado_en",id));
        r.put("credito",db.uno("SELECT * FROM credito_venta WHERE venta_id=?",id).orElse(null));
        r.put("naturaleza","Recibo de operación; no constituye facturación electrónica DIAN");
        if(a.admin())r.put("costo_materiales_cop",db.uno("SELECT costo_materiales FROM v_costo_venta WHERE venta_id=?",id).map(x->x.get("costo_materiales")).orElse(BigDecimal.ZERO));
        return r;
    }
    public List<Map<String,Object>> cartera(Actor a) {
        a.exigirAdmin();
        return db.lista("SELECT v.venta_id,v.numero_recibo,c.nombre cliente,c.contacto_cobranza,cr.fecha_vencimiento,cr.condicion_pactada,t.total total_cop,t.cobrado cobrado_cop,t.saldo saldo_cop FROM venta v JOIN credito_venta cr ON cr.venta_id=v.venta_id JOIN cliente c ON c.cliente_id=v.cliente_id JOIN v_total_venta t ON t.venta_id=v.venta_id WHERE v.estado='EMITIDA' AND t.saldo>0 ORDER BY cr.fecha_vencimiento");
    }
}
