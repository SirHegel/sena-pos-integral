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
public class NominaService {
    private final Db db;
    public NominaService(Db db) { this.db = db; }
    public record Empleado(String documento, String nombre, String estado) {}
    public record Novedad(String empleadoId, String periodo, String concepto, String tipo, BigDecimal importe) {}
    public record Liquidacion(String periodo, Integer version) {}
    public record Aprobacion(List<String> novedadesIds) {}
    private void acceso(Actor a) { a.exigir("ADMINISTRADOR", "NOMINA"); }
    private Map<String,Object> empleado(Actor a, String id, boolean bloqueo) {
        return db.requerido("SELECT empleado_id,documento,nombre,estado FROM empleado WHERE tenant_id=? AND empleado_id=?" + (bloqueo ? " FOR UPDATE" : ""), a.tenantId(), V.uuid(id,"empleadoId"));
    }
    private void activo(Actor a, String id) {
        if (!"ACTIVO".equals(empleado(a,id,true).get("estado"))) throw conflicto("El empleado debe estar activo");
    }
    @Transactional(readOnly=true)
    public Object empleados(Actor a) { acceso(a); return limite(db.lista("SELECT empleado_id,documento,nombre,estado FROM empleado WHERE tenant_id=? ORDER BY empleado_id LIMIT 1001", a.tenantId())); }
    @Transactional(readOnly=true)
    public Object consultarEmpleado(Actor a,String id) { acceso(a);return empleado(a,id,false); }
    @Transactional
    public Object crearEmpleado(Actor a, Empleado r) {
        acceso(a); bloquear(db,a);
        String doc=V.texto(r.documento(),"documento",128), nombre=V.texto(r.nombre(),"nombre",128);
        String estado=V.opcion(r.estado(),"estado",Set.of("ACTIVO","INACTIVO")), id=id();
        db.cambiar("INSERT INTO empleado(tenant_id,empleado_id,documento,nombre,estado) VALUES(?,?,?,?,?)",a.tenantId(),id,doc,nombre,estado);
        return empleado(a,id,false);
    }
    @Transactional
    public Object cambiarEmpleado(Actor a,String id,Empleado r) {
        acceso(a); bloquear(db,a); id=V.uuid(id,"empleadoId"); var e=empleado(a,id,true);
        String doc=V.texto(r.documento(),"documento",128), nombre=V.texto(r.nombre(),"nombre",128), estado=V.opcion(r.estado(),"estado",Set.of("ACTIVO","INACTIVO"));
        if ((!doc.equals(e.get("documento")) || !nombre.equals(e.get("nombre"))) && db.existe("SELECT empleado_id FROM detalle_nomina WHERE tenant_id=? AND empleado_id=? LIMIT 1",a.tenantId(),id))
            throw conflicto("La identidad del empleado tiene historial de liquidación y se conserva");
        db.cambiar("UPDATE empleado SET documento=?,nombre=?,estado=? WHERE tenant_id=? AND empleado_id=?",doc,nombre,estado,a.tenantId(),id);
        return empleado(a,id,false);
    }
    private Map<String,Object> novedad(Actor a,String id,boolean bloqueo) {
        return db.requerido("SELECT novedad_id,empleado_id,periodo,concepto,tipo,importe,estado FROM novedad_nomina WHERE tenant_id=? AND novedad_id=?"+(bloqueo?" FOR UPDATE":""),a.tenantId(),V.uuid(id,"novedadId"));
    }
    private void sinConsumir(Actor a,String id) {
        if (db.existe("SELECT novedad_id FROM detalle_nomina_novedad WHERE tenant_id=? AND novedad_id=?",a.tenantId(),id)) throw conflicto("La novedad ya pertenece a una liquidación aprobada");
    }
    @Transactional(readOnly=true)
    public Object consultarNovedad(Actor a,String id) { acceso(a);return novedad(a,id,false); }
    @Transactional(readOnly=true)
    public Object novedades(Actor a,String periodo) {
        acceso(a); String filtro=periodo==null?"":" AND periodo=?";
        return limite(periodo==null ? db.lista("SELECT novedad_id,empleado_id,periodo,concepto,tipo,importe,estado FROM novedad_nomina WHERE tenant_id=? ORDER BY periodo,novedad_id LIMIT 1001",a.tenantId()) : db.lista("SELECT novedad_id,empleado_id,periodo,concepto,tipo,importe,estado FROM novedad_nomina WHERE tenant_id=?"+filtro+" ORDER BY novedad_id LIMIT 1001",a.tenantId(),V.periodo(periodo,"periodo").toString()));
    }
    @Transactional
    public Object crearNovedad(Actor a,Novedad r) {
        acceso(a); bloquear(db,a); String emp=V.uuid(r.empleadoId(),"empleadoId"); activo(a,emp);
        String periodo=V.periodo(r.periodo(),"periodo").toString(), concepto=V.texto(r.concepto(),"concepto",128), tipo=V.opcion(r.tipo(),"tipo",Set.of("DEVENGO","DEDUCCION"));
        BigDecimal importe=V.decimal(r.importe(),"importe",2,CERO,true); String id=id();
        db.cambiar("INSERT INTO novedad_nomina(tenant_id,novedad_id,empleado_id,periodo,concepto,tipo,importe,estado) VALUES(?,?,?,?,?,?,?,'BORRADOR')",a.tenantId(),id,emp,periodo,concepto,tipo,importe);
        return novedad(a,id,false);
    }
    @Transactional
    public Object cambiarNovedad(Actor a,String id,Novedad r) {
        acceso(a); bloquear(db,a); id=V.uuid(id,"novedadId"); var n=novedad(a,id,true); sinConsumir(a,id);
        if (!"BORRADOR".equals(n.get("estado"))) throw conflicto("Sólo se editan novedades en borrador");
        String emp=V.uuid(r.empleadoId(),"empleadoId"); activo(a,emp);
        String periodo=V.periodo(r.periodo(),"periodo").toString(), concepto=V.texto(r.concepto(),"concepto",128), tipo=V.opcion(r.tipo(),"tipo",Set.of("DEVENGO","DEDUCCION"));
        BigDecimal importe=V.decimal(r.importe(),"importe",2,CERO,true);
        db.cambiar("UPDATE novedad_nomina SET empleado_id=?,periodo=?,concepto=?,tipo=?,importe=? WHERE tenant_id=? AND novedad_id=?",emp,periodo,concepto,tipo,importe,a.tenantId(),id);
        return novedad(a,id,false);
    }
    @Transactional
    public Object estadoNovedad(Actor a,String id,boolean autorizar) {
        acceso(a); bloquear(db,a); id=V.uuid(id,"novedadId"); var n=novedad(a,id,true);
        String destino=autorizar?"AUTORIZADA":"RECHAZADA";
        if (destino.equals(n.get("estado"))) return n;
        sinConsumir(a,id);
        if ("RECHAZADA".equals(n.get("estado")) || (autorizar && !"BORRADOR".equals(n.get("estado")))) throw conflicto("La transición de la novedad no está permitida");
        if (autorizar) activo(a,n.get("empleado_id").toString());
        db.cambiar("UPDATE novedad_nomina SET estado=? WHERE tenant_id=? AND novedad_id=?",destino,a.tenantId(),id);
        return novedad(a,id,false);
    }
    private Map<String,Object> cabecera(Actor a,String id,boolean bloqueo) {
        return db.requerido("SELECT liquidacion_id,periodo,version,estado,creada_en FROM liquidacion_nomina WHERE tenant_id=? AND liquidacion_id=?"+(bloqueo?" FOR UPDATE":""),a.tenantId(),V.uuid(id,"liquidacionId"));
    }
    @Transactional(readOnly=true)
    public Object liquidaciones(Actor a,String periodo) {
        acceso(a);
        return limite(periodo==null ? db.lista("SELECT liquidacion_id,periodo,version,estado,creada_en FROM liquidacion_nomina WHERE tenant_id=? ORDER BY periodo,version LIMIT 1001",a.tenantId()) : db.lista("SELECT liquidacion_id,periodo,version,estado,creada_en FROM liquidacion_nomina WHERE tenant_id=? AND periodo=? ORDER BY version LIMIT 1001",a.tenantId(),V.periodo(periodo,"periodo").toString()));
    }
    @Transactional
    public Object crearLiquidacion(Actor a,Liquidacion r) {
        acceso(a); bloquear(db,a); String periodo=V.periodo(r.periodo(),"periodo").toString(); int version=V.entero(r.version(),"version",1,1_000_000); String id=id();
        db.cambiar("INSERT INTO liquidacion_nomina(tenant_id,liquidacion_id,periodo,version,estado,creada_en) VALUES(?,?,?,?,'BORRADOR',?)",a.tenantId(),id,periodo,version,Timestamp.from(Instant.now()));
        return detalle(a,id);
    }
    @Transactional(readOnly=true)
    public Object detalle(Actor a,String id) {
        acceso(a); id=V.uuid(id,"liquidacionId"); var c=new LinkedHashMap<>(cabecera(a,id,false));
        c.put("detalles",db.lista("SELECT d.empleado_id,e.documento,e.nombre,d.devengos,d.deducciones,d.devengos-d.deducciones AS neto FROM detalle_nomina d JOIN empleado e ON e.tenant_id=d.tenant_id AND e.empleado_id=d.empleado_id WHERE d.tenant_id=? AND d.liquidacion_id=? ORDER BY d.empleado_id",a.tenantId(),id));
        c.put("novedadesIds",db.lista("SELECT novedad_id FROM detalle_nomina_novedad WHERE tenant_id=? AND liquidacion_id=? ORDER BY novedad_id",a.tenantId(),id).stream().map(x->x.get("novedad_id")).toList());
        return c;
    }
    private List<Map<String,Object>> candidatas(Actor a,String periodo) {
        return limite(db.lista("SELECT n.novedad_id,n.empleado_id,n.periodo,n.concepto,n.tipo,n.importe,n.estado,e.estado AS empleado_estado FROM novedad_nomina n JOIN empleado e ON e.tenant_id=n.tenant_id AND e.empleado_id=n.empleado_id WHERE n.tenant_id=? AND n.periodo=? AND n.estado='AUTORIZADA' AND NOT EXISTS(SELECT 1 FROM detalle_nomina_novedad d WHERE d.tenant_id=n.tenant_id AND d.novedad_id=n.novedad_id) ORDER BY n.empleado_id,n.novedad_id LIMIT 1001",a.tenantId(),periodo));
    }
    private record Calculo(List<Map<String,Object>> detalles,BigDecimal devengos,BigDecimal deducciones) {}
    private Calculo calcular(List<Map<String,Object>> filas) {
        var sumas=new TreeMap<String,BigDecimal[]>();
        for (var n:filas) {
            if (!"ACTIVO".equals(n.get("empleado_estado"))) throw conflicto("Hay novedades de un empleado inactivo");
            var par=sumas.computeIfAbsent(n.get("empleado_id").toString(),k->new BigDecimal[]{CERO,CERO});
            int i="DEVENGO".equals(n.get("tipo"))?0:1; par[i]=dinero(par[i].add(dinero(n.get("importe"))));
        }
        var detalles=new ArrayList<Map<String,Object>>(); BigDecimal dev=CERO,ded=CERO;
        for (var e:sumas.entrySet()) {
            var v=e.getValue(); if(v[1].compareTo(v[0])>0) throw conflicto("Las deducciones de un empleado exceden sus devengos didácticos");
            detalles.add(Map.of("empleado_id",e.getKey(),"devengos",v[0],"deducciones",v[1],"neto",dinero(v[0].subtract(v[1]))));
            dev=dinero(dev.add(v[0])); ded=dinero(ded.add(v[1]));
        }
        return new Calculo(detalles,dev,ded);
    }
    @Transactional(readOnly=true)
    public Object simulacion(Actor a,String id) {
        acceso(a); var c=cabecera(a,id,false); if(!"BORRADOR".equals(c.get("estado"))) throw conflicto("La liquidación ya tiene un resultado aprobado");
        var filas=candidatas(a,c.get("periodo").toString());var calc=calcular(filas);
        return Map.of("liquidacionId",id,"novedades",filas,"detalles",calc.detalles(),"totales",Map.of("devengos",calc.devengos(),"deducciones",calc.deducciones(),"neto",calc.devengos().subtract(calc.deducciones())));
    }
    @Transactional
    public Object aprobar(Actor a,String id,Aprobacion r) {
        acceso(a); var seleccion=ids(r.novedadesIds()); id=V.uuid(id,"liquidacionId"); bloquear(db,a); var c=cabecera(a,id,true);
        if (!"BORRADOR".equals(c.get("estado"))) {
            var aplicadas=db.lista("SELECT novedad_id FROM detalle_nomina_novedad WHERE tenant_id=? AND liquidacion_id=? ORDER BY novedad_id",a.tenantId(),id).stream().map(x->x.get("novedad_id").toString()).toList();
            if(!seleccion.equals(aplicadas)) throw conflicto("La liquidación aprobada es inmutable y contiene otra selección");
            return detalle(a,id);
        }
        var filas=db.lista("SELECT n.novedad_id,n.empleado_id,n.periodo,n.tipo,n.importe,n.estado,e.estado AS empleado_estado FROM novedad_nomina n JOIN empleado e ON e.tenant_id=n.tenant_id AND e.empleado_id=n.empleado_id WHERE n.tenant_id=? AND n.novedad_id IN ("+marcas(seleccion.size())+") ORDER BY n.empleado_id,n.novedad_id FOR UPDATE",args(a.tenantId(),seleccion));
        if(filas.size()!=seleccion.size()) throw new ApiError(404,"NO_ENCONTRADO","No se encontraron todas las novedades en el negocio actual");
        for(var n:filas) if(!c.get("periodo").equals(n.get("periodo")) || !"AUTORIZADA".equals(n.get("estado"))) throw conflicto("Las novedades deben estar autorizadas y pertenecer al período");
        if(db.existe("SELECT novedad_id FROM detalle_nomina_novedad WHERE tenant_id=? AND novedad_id IN ("+marcas(seleccion.size())+") LIMIT 1",args(a.tenantId(),seleccion))) throw conflicto("Una novedad seleccionada ya fue consumida");
        var calc=calcular(filas); if(calc.devengos().signum()<=0) throw conflicto("La liquidación necesita devengos positivos");
        for(var d:calc.detalles()) db.cambiar("INSERT INTO detalle_nomina(tenant_id,liquidacion_id,empleado_id,devengos,deducciones) VALUES(?,?,?,?,?)",a.tenantId(),id,d.get("empleado_id"),d.get("devengos"),d.get("deducciones"));
        for(var n:filas) db.cambiar("INSERT INTO detalle_nomina_novedad(tenant_id,novedad_id,liquidacion_id,empleado_id) VALUES(?,?,?,?)",a.tenantId(),n.get("novedad_id"),id,n.get("empleado_id"));
        db.cambiar("UPDATE liquidacion_nomina SET estado='APROBADA' WHERE tenant_id=? AND liquidacion_id=?",a.tenantId(),id);
        db.cambiar("INSERT INTO evento_integracion(tenant_id,evento_id,tipo,liquidacion_id,estado,creada_en) VALUES(?,?,'NOMINA_APROBADA',?,'PENDIENTE',?)",a.tenantId(),id(),id,Timestamp.from(Instant.now()));
        return detalle(a,id);
    }
}
