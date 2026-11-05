package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.nucleo.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static co.edu.sena.pos.integral.administracion.CReglas.*;

@Service
public class ContabilidadService {
    private final Db db;
    public ContabilidadService(Db db) { this.db=db; }
    public record Cuenta(String codigo,String nombre,String naturaleza) {}
    public record Contabilizacion(String cuentaDebitoId,String cuentaCreditoId,String cuentaAuxiliarId) {}
    public record AltaAsiento(String eventoId,String cuentaDebitoId,String cuentaCreditoId,String cuentaAuxiliarId) {}
    private void acceso(Actor a) { a.exigir("ADMINISTRADOR","CONTABILIDAD"); }
    private Map<String,Object> cuenta(Actor a,String id,boolean lock) {
        return db.requerido("SELECT cuenta_id,codigo,nombre,naturaleza FROM cuenta WHERE tenant_id=? AND cuenta_id=?"+(lock?" FOR UPDATE":""),a.tenantId(),V.uuid(id,"cuentaId"));
    }
    @Transactional(readOnly=true)
    public Object cuentas(Actor a) { acceso(a); return limite(db.lista("SELECT cuenta_id,codigo,nombre,naturaleza FROM cuenta WHERE tenant_id=? ORDER BY codigo LIMIT 1001",a.tenantId())); }
    @Transactional
    public Object crearCuenta(Actor a,Cuenta r) {
        acceso(a); bloquear(db,a); String codigo=V.texto(r.codigo(),"codigo",64), nombre=V.texto(r.nombre(),"nombre",128), naturaleza=V.opcion(r.naturaleza(),"naturaleza",Set.of("DEBITO","CREDITO")); String id=id();
        db.cambiar("INSERT INTO cuenta(tenant_id,cuenta_id,codigo,nombre,naturaleza) VALUES(?,?,?,?,?)",a.tenantId(),id,codigo,nombre,naturaleza);
        return cuenta(a,id,false);
    }
    @Transactional
    public Object cambiarCuenta(Actor a,String id,Cuenta r) {
        acceso(a); bloquear(db,a); id=V.uuid(id,"cuentaId"); cuenta(a,id,true);
        if(db.existe("SELECT cuenta_id FROM linea_asiento WHERE tenant_id=? AND cuenta_id=? LIMIT 1",a.tenantId(),id)) throw conflicto("La cuenta tiene asientos registrados y se conserva su definición");
        String codigo=V.texto(r.codigo(),"codigo",64),nombre=V.texto(r.nombre(),"nombre",128),naturaleza=V.opcion(r.naturaleza(),"naturaleza",Set.of("DEBITO","CREDITO"));
        db.cambiar("UPDATE cuenta SET codigo=?,nombre=?,naturaleza=? WHERE tenant_id=? AND cuenta_id=?",codigo,nombre,naturaleza,a.tenantId(),id);
        return cuenta(a,id,false);
    }
    @Transactional(readOnly=true)
    public Object eventos(Actor a,String estado) {
        acceso(a);
        if(estado==null) return limite(db.lista("SELECT evento_id,tipo,venta_id,liquidacion_id,estado,creada_en,procesada_en,ultimo_error FROM evento_integracion WHERE tenant_id=? ORDER BY creada_en,evento_id LIMIT 1001",a.tenantId()));
        estado=V.opcion(estado,"estado",Set.of("PENDIENTE","ERROR","PROCESADO"));
        return limite(db.lista("SELECT evento_id,tipo,venta_id,liquidacion_id,estado,creada_en,procesada_en,ultimo_error FROM evento_integracion WHERE tenant_id=? AND estado=? ORDER BY creada_en,evento_id LIMIT 1001",a.tenantId(),estado));
    }
    @Transactional(readOnly=true)
    public Object evento(Actor a,String id) {
        acceso(a); return db.requerido("SELECT evento_id,tipo,venta_id,liquidacion_id,estado,creada_en,procesada_en,ultimo_error FROM evento_integracion WHERE tenant_id=? AND evento_id=?",a.tenantId(),V.uuid(id,"eventoId"));
    }
    @Transactional(readOnly=true)
    public Object asientos(Actor a) {
        acceso(a); return limite(db.lista("SELECT asiento_id,fecha,tipo_origen,venta_id,liquidacion_id,documento_origen,estado FROM asiento_contable WHERE tenant_id=? ORDER BY fecha,asiento_id LIMIT 1001",a.tenantId()));
    }
    private Map<String,Object> detalleAsiento(Actor a,String id) {
        var asiento=new LinkedHashMap<>(db.requerido("SELECT asiento_id,fecha,tipo_origen,venta_id,liquidacion_id,documento_origen,estado FROM asiento_contable WHERE tenant_id=? AND asiento_id=?",a.tenantId(),id));
        var lineas=db.lista("SELECT l.linea_no,l.cuenta_id,c.codigo,c.nombre,l.debito,l.credito FROM linea_asiento l JOIN cuenta c ON c.tenant_id=l.tenant_id AND c.cuenta_id=l.cuenta_id WHERE l.tenant_id=? AND l.asiento_id=? ORDER BY l.linea_no",a.tenantId(),id);
        BigDecimal debito=CERO,credito=CERO;
        for(var l:lineas) { debito=debito.add(dinero(l.get("debito"))); credito=credito.add(dinero(l.get("credito"))); }
        asiento.put("lineas",lineas);asiento.put("totales",Map.of("debito",debito,"credito",credito,"diferencia",debito.subtract(credito)));
        return asiento;
    }
    @Transactional(readOnly=true)
    public Object asiento(Actor a,String id) { acceso(a); return detalleAsiento(a,V.uuid(id,"asientoId")); }
    private record Linea(String cuentaId,BigDecimal debito,BigDecimal credito) {}
    private void validarCuenta(Actor a,String id,String naturaleza) {
        if(!naturaleza.equals(cuenta(a,id,true).get("naturaleza"))) throw conflicto("La cuenta debe tener naturaleza "+naturaleza);
    }
    private List<Linea> lineas(Actor a,Contabilizacion r,BigDecimal debito,BigDecimal principal,BigDecimal auxiliar) {
        String de=V.uuid(r.cuentaDebitoId(),"cuentaDebitoId"),cr=V.uuid(r.cuentaCreditoId(),"cuentaCreditoId");
        String aux=r.cuentaAuxiliarId()==null?null:V.uuid(r.cuentaAuxiliarId(),"cuentaAuxiliarId");
        if(de.equals(cr) || (aux!=null && (de.equals(aux) || cr.equals(aux)))) throw V.error("cuentas","se requieren cuentas diferentes");
        validarCuenta(a,de,"DEBITO");validarCuenta(a,cr,"CREDITO");
        var resultado=new ArrayList<Linea>();resultado.add(new Linea(de,dinero(debito),CERO));
        if(principal.signum()>0) resultado.add(new Linea(cr,CERO,dinero(principal)));
        if(auxiliar.signum()>0) {
            if(aux==null) throw V.error("cuentaAuxiliarId","requerida para IVA o deducciones positivas");
            validarCuenta(a,aux,"CREDITO");resultado.add(new Linea(aux,CERO,dinero(auxiliar)));
        } else if(aux!=null) throw V.error("cuentaAuxiliarId","debe omitirse cuando el importe auxiliar es cero");
        BigDecimal totalDe=CERO,totalCr=CERO;
        for(var l:resultado) { totalDe=totalDe.add(l.debito());totalCr=totalCr.add(l.credito()); }
        if(resultado.size()<2 || totalDe.signum()<=0 || totalDe.compareTo(totalCr)!=0) throw conflicto("El asiento debe tener dos o más líneas, importes positivos y equilibrio exacto");
        return resultado;
    }
    @Transactional
    public Object crearAsiento(Actor a,AltaAsiento r) {
        return procesar(a,r.eventoId(),new Contabilizacion(r.cuentaDebitoId(),r.cuentaCreditoId(),r.cuentaAuxiliarId()));
    }
    @Transactional
    public Object procesar(Actor a,String eventoId,Contabilizacion r) {
        acceso(a); bloquear(db,a); eventoId=V.uuid(eventoId,"eventoId");
        var ev=db.requerido("SELECT evento_id,tipo,venta_id,liquidacion_id,estado FROM evento_integracion WHERE tenant_id=? AND evento_id=? FOR UPDATE",a.tenantId(),eventoId);
        String tipo,origen,documento; BigDecimal de,principal,auxiliar;
        if("VENTA_CONFIRMADA".equals(ev.get("tipo"))) {
            tipo="VENTA"; origen=ev.get("venta_id").toString();
            var venta=db.requerido("SELECT venta_id,estado,numero_factura FROM venta WHERE tenant_id=? AND venta_id=? FOR UPDATE",a.tenantId(),origen);
            if(!"PAGADA".equals(venta.get("estado")) || venta.get("numero_factura")==null) throw conflicto("El origen debe ser una venta pagada y facturada");
            var total=db.requerido("SELECT base,iva,total FROM v_total_venta WHERE tenant_id=? AND venta_id=?",a.tenantId(),origen);
            var pago=db.requerido("SELECT importe FROM pago WHERE tenant_id=? AND venta_id=? AND estado='CONFIRMADO'",a.tenantId(),origen);
            de=dinero(total.get("total")); principal=dinero(total.get("base")); auxiliar=dinero(total.get("iva"));
            if(de.compareTo(dinero(pago.get("importe")))!=0) throw conflicto("El pago confirmado no coincide con la venta");
            documento=venta.get("numero_factura").toString();
        } else if("NOMINA_APROBADA".equals(ev.get("tipo"))) {
            tipo="NOMINA";origen=ev.get("liquidacion_id").toString();
            var liq=db.requerido("SELECT liquidacion_id,periodo,version,estado FROM liquidacion_nomina WHERE tenant_id=? AND liquidacion_id=? FOR UPDATE",a.tenantId(),origen);
            if(!Set.of("APROBADA","CONTABILIZADA").contains(liq.get("estado"))) throw conflicto("El origen debe ser una liquidación aprobada");
            var total=db.requerido("SELECT COUNT(*) AS cantidad,SUM(devengos) AS devengos,SUM(deducciones) AS deducciones FROM detalle_nomina WHERE tenant_id=? AND liquidacion_id=?",a.tenantId(),origen);
            if(((Number)total.get("cantidad")).longValue()==0) throw conflicto("La liquidación no tiene detalles");
            de=dinero(total.get("devengos"));auxiliar=dinero(total.get("deducciones"));principal=dinero(de.subtract(auxiliar));
            documento="NOMINA:"+liq.get("periodo")+":V"+liq.get("version");
        } else throw conflicto("Tipo de evento no soportado");
        if(principal.signum()<0 || auxiliar.signum()<0 || de.signum()<=0) throw conflicto("El origen contiene importes incompatibles con la contabilización");
        var lineas=lineas(a,r,de,principal,auxiliar);
        String columna="VENTA".equals(tipo)?"venta_id":"liquidacion_id";
        var previo=db.uno("SELECT asiento_id,estado,documento_origen FROM asiento_contable WHERE tenant_id=? AND "+columna+"=?",a.tenantId(),origen);
        if("PROCESADO".equals(ev.get("estado"))) {
            var p=previo.orElseThrow(()->conflicto("El evento procesado no tiene asiento asociado"));
            var existentes=db.lista("SELECT cuenta_id,debito,credito FROM linea_asiento WHERE tenant_id=? AND asiento_id=? ORDER BY linea_no",a.tenantId(),p.get("asiento_id"));
            if(!"REGISTRADO".equals(p.get("estado")) || existentes.size()!=lineas.size() || !documento.equals(p.get("documento_origen"))) throw conflicto("El asiento previo no coincide con el origen");
            for(int i=0;i<lineas.size();i++) {
                var l=lineas.get(i);var e=existentes.get(i);
                if(!l.cuentaId().equals(e.get("cuenta_id")) || l.debito().compareTo(dinero(e.get("debito")))!=0 || l.credito().compareTo(dinero(e.get("credito")))!=0) throw conflicto("El evento ya fue procesado con otras cuentas");
            }
            return detalleAsiento(a,p.get("asiento_id").toString());
        }
        if(previo.isPresent()) throw conflicto("El origen ya tiene un asiento; no puede contabilizarse dos veces");
        String asientoId=id();Timestamp ahora=Timestamp.from(Instant.now());
        db.cambiar("INSERT INTO asiento_contable(tenant_id,asiento_id,fecha,tipo_origen,venta_id,liquidacion_id,documento_origen,estado) VALUES(?,?,?,?,?,?,?,'REGISTRADO')",a.tenantId(),asientoId,ahora,tipo,"VENTA".equals(tipo)?origen:null,"NOMINA".equals(tipo)?origen:null,documento);
        int n=0;for(var l:lineas) db.cambiar("INSERT INTO linea_asiento(tenant_id,asiento_id,linea_no,cuenta_id,debito,credito) VALUES(?,?,?,?,?,?)",a.tenantId(),asientoId,++n,l.cuentaId(),l.debito(),l.credito());
        if("NOMINA".equals(tipo)) db.cambiar("UPDATE liquidacion_nomina SET estado='CONTABILIZADA' WHERE tenant_id=? AND liquidacion_id=?",a.tenantId(),origen);
        db.cambiar("UPDATE evento_integracion SET estado='PROCESADO',procesada_en=?,ultimo_error=NULL WHERE tenant_id=? AND evento_id=?",ahora,a.tenantId(),eventoId);
        return detalleAsiento(a,asientoId);
    }
}
