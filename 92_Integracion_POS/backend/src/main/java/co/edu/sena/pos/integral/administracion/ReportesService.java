package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.nucleo.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.Normalizer;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportesService {
    private static final ZoneId BOGOTA=ZoneId.of("America/Bogota");
    private final Db db;
    public ReportesService(Db db) { this.db=db; }
    public record Informe(Map<String,Object> origen,String area,String desde,String hasta,String zonaHoraria,List<String> columnas,List<Map<String,Object>> filas,Map<String,Object> totales) {}
    @Transactional(readOnly=true)
    public Informe consultar(Actor a,String area,String desde,String hasta) {
        area=V.opcion(area,"area",Set.of("ventas","nomina","contabilidad","inventario","caja"));
        switch(area) {
            case "ventas" -> a.exigir("ADMINISTRADOR","CONTABILIDAD","CAJERO");
            case "nomina" -> a.exigir("ADMINISTRADOR","NOMINA");
            case "contabilidad" -> a.exigir("ADMINISTRADOR","CONTABILIDAD");
            case "inventario" -> a.exigir("ADMINISTRADOR","INVENTARIO");
            case "caja" -> a.exigir("ADMINISTRADOR","CAJERO");
            default -> throw V.error("area","área no admitida");
        }
        LocalDate d=V.fecha(desde,"desde"),h=V.fecha(hasta,"hasta");
        if(d.getYear()<1000 || h.getYear()>9998 || h.isBefore(d) || ChronoUnit.DAYS.between(d,h)>365)
            throw V.error("fechas","intervalo de 1 a 366 días, años 1000 a 9998");
        var args=new ArrayList<Object>();args.add(a.tenantId());args.add(Timestamp.from(d.atStartOfDay(BOGOTA).toInstant()));args.add(Timestamp.from(h.plusDays(1).atStartOfDay(BOGOTA).toInstant()));
        String sql;List<String> columnas;List<String> sumas;
        switch(area) {
            case "ventas" -> {
                columnas=List.of("venta_id","fecha","numero_factura","usuario_id","medio","base","iva","total","pagado");sumas=List.of("base","iva","total","pagado");
                sql="SELECT v.venta_id,v.pagada_en AS fecha,v.numero_factura,v.usuario_id,p.medio,t.base,t.iva,t.total,p.importe AS pagado FROM venta v JOIN v_total_venta t ON t.tenant_id=v.tenant_id AND t.venta_id=v.venta_id JOIN pago p ON p.tenant_id=v.tenant_id AND p.venta_id=v.venta_id AND p.estado='CONFIRMADO' WHERE v.tenant_id=? AND v.pagada_en>=? AND v.pagada_en<? AND v.estado='PAGADA'";
                if(!a.tiene("ADMINISTRADOR","CONTABILIDAD")) { sql+=" AND v.usuario_id=?";args.add(a.usuarioId()); }
                sql+=" ORDER BY v.pagada_en,v.venta_id LIMIT 10001";
            }
            case "nomina" -> {
                columnas=List.of("liquidacion_id","fecha","periodo","version","empleado_id","documento","nombre","devengos","deducciones","neto");sumas=List.of("devengos","deducciones","neto");
                sql="SELECT l.liquidacion_id,l.creada_en AS fecha,l.periodo,l.version,d.empleado_id,e.documento,e.nombre,d.devengos,d.deducciones,d.devengos-d.deducciones AS neto FROM liquidacion_nomina l JOIN detalle_nomina d ON d.tenant_id=l.tenant_id AND d.liquidacion_id=l.liquidacion_id JOIN empleado e ON e.tenant_id=d.tenant_id AND e.empleado_id=d.empleado_id WHERE l.tenant_id=? AND l.creada_en>=? AND l.creada_en<? AND l.estado IN ('APROBADA','CONTABILIZADA') ORDER BY l.creada_en,l.liquidacion_id,d.empleado_id LIMIT 10001";
            }
            case "contabilidad" -> {
                columnas=List.of("asiento_id","fecha","tipo_origen","documento_origen","linea_no","codigo","nombre","debito","credito");sumas=List.of("debito","credito");
                sql="SELECT a.asiento_id,a.fecha,a.tipo_origen,a.documento_origen,l.linea_no,c.codigo,c.nombre,l.debito,l.credito FROM asiento_contable a JOIN linea_asiento l ON l.tenant_id=a.tenant_id AND l.asiento_id=a.asiento_id JOIN cuenta c ON c.tenant_id=l.tenant_id AND c.cuenta_id=l.cuenta_id WHERE a.tenant_id=? AND a.fecha>=? AND a.fecha<? AND a.estado='REGISTRADO' ORDER BY a.fecha,a.asiento_id,l.linea_no LIMIT 10001";
            }
            case "inventario" -> {
                columnas=List.of("movimiento_id","fecha","bodega","producto_id","producto","unidad","cantidad_delta","tipo_origen","documento_origen");sumas=List.of();
                sql="SELECT m.movimiento_id,m.registrado_en AS fecha,b.nombre AS bodega,m.producto_id,p.nombre AS producto,p.unidad,m.cantidad_delta,m.tipo_origen,m.documento_origen FROM movimiento_inventario m JOIN bodega b ON b.tenant_id=m.tenant_id AND b.bodega_id=m.bodega_id JOIN producto p ON p.tenant_id=m.tenant_id AND p.producto_id=m.producto_id WHERE m.tenant_id=? AND m.registrado_en>=? AND m.registrado_en<? ORDER BY m.registrado_en,m.movimiento_id LIMIT 10001";
            }
            case "caja" -> {
                columnas=List.of("sesion_id","fecha","caja","usuario_id","fondo_inicial","total_esperado","total_contado","diferencia","externo");sumas=List.of("fondo_inicial","total_esperado","total_contado","diferencia","externo");
                sql="SELECT s.sesion_id,s.cerrada_en AS fecha,c.nombre AS caja,s.usuario_id,s.fondo_inicial,s.total_esperado,s.total_contado,s.total_contado-s.total_esperado AS diferencia,COALESCE((SELECT SUM(p.importe) FROM pago p JOIN venta v ON v.tenant_id=p.tenant_id AND v.venta_id=p.venta_id WHERE v.tenant_id=s.tenant_id AND v.sesion_caja_id=s.sesion_id AND p.estado='CONFIRMADO' AND p.medio='EXTERNO'),0) AS externo FROM sesion_caja s JOIN caja c ON c.tenant_id=s.tenant_id AND c.caja_id=s.caja_id WHERE s.tenant_id=? AND s.cerrada_en>=? AND s.cerrada_en<?";
                if(!a.tiene("ADMINISTRADOR")) { sql+=" AND s.usuario_id=?";args.add(a.usuarioId()); }
                sql+=" ORDER BY s.cerrada_en,s.sesion_id LIMIT 10001";
            }
            default -> throw new IllegalStateException();
        }
        var filas=db.lista(sql,args.toArray());
        if(filas.size()>10000) throw new ApiError(413,"CONSULTA_AMPLIA","El reporte excede 10000 filas; reduzca el intervalo");
        var totales=new LinkedHashMap<String,Object>();totales.put("filas",filas.size());
        for(String k:sumas) {
            BigDecimal suma=new BigDecimal("0.00");
            for(var f:filas) {
                Object n=f.get(k);if(n==null) throw CReglas.conflicto("El reporte contiene un importe incompleto");
                suma=suma.add(n instanceof BigDecimal b?b:new BigDecimal(n.toString()));
            }
            totales.put(k,suma);
        }
        var normalizadas=new ArrayList<Map<String,Object>>();
        for(var fila:filas) {
            var n=new LinkedHashMap<String,Object>();
            for(String c:columnas) {
                Object v=fila.get(c);
                if(v instanceof Timestamp t) v=t.toInstant().atZone(BOGOTA).toOffsetDateTime().toString();
                else if(v instanceof LocalDateTime l) v=l.toInstant(ZoneOffset.UTC).atZone(BOGOTA).toOffsetDateTime().toString();
                n.put(c,v);
            }
            normalizadas.add(Collections.unmodifiableMap(n));
        }
        var negocio=db.requerido("SELECT moneda FROM tenant WHERE tenant_id=?",a.tenantId());
        var origen=new LinkedHashMap<String,Object>();
        origen.put("tenantId",a.tenantId());origen.put("usuarioId",a.usuarioId());origen.put("roles",a.roles().stream().sorted().toList());origen.put("moneda",negocio.get("moneda"));origen.put("emitidoEn",Instant.now().atZone(BOGOTA).toOffsetDateTime().toString());
        return new Informe(Collections.unmodifiableMap(origen),area,d.toString(),h.toString(),BOGOTA.getId(),columnas,List.copyOf(normalizadas),Collections.unmodifiableMap(totales));
    }
    static String celda(Object valor) {
        String s=valor==null?"":valor instanceof BigDecimal b?b.toPlainString():valor.toString();
        if(valor instanceof CharSequence) {
            String inicio=Normalizer.normalize(s,Normalizer.Form.NFKC);
            int i=0;while(i<inicio.length() && (Character.isWhitespace(inicio.charAt(i)) || Character.isSpaceChar(inicio.charAt(i)))) i++;
            boolean control=!inicio.isEmpty() && Character.isISOControl(inicio.charAt(0));
            if(control || (i<inicio.length() && "=+-@".indexOf(inicio.charAt(i))>=0)) s="'"+s;
        }
        return "\""+s.replace("\"","\"\"")+"\"";
    }
    public String csv(Informe r) {
        var b=new StringBuilder("\uFEFF");
        b.append(celda("metadato")).append(',').append(celda("valor")).append("\r\n");
        for(var e:r.origen().entrySet()) b.append(celda(e.getKey())).append(',').append(celda(e.getValue() instanceof Collection<?> c?String.join(";",c.stream().map(Object::toString).toList()):e.getValue())).append("\r\n");
        for(var e:Map.of("area",r.area(),"desde",r.desde(),"hasta",r.hasta(),"zonaHoraria",r.zonaHoraria()).entrySet()) b.append(celda(e.getKey())).append(',').append(celda(e.getValue())).append("\r\n");
        b.append("\r\n");
        b.append(String.join(",",r.columnas().stream().map(ReportesService::celda).toList())).append("\r\n");
        for(var f:r.filas()) b.append(String.join(",",r.columnas().stream().map(k->celda(f.get(k))).toList())).append("\r\n");
        var total=new ArrayList<String>();
        for(int i=0;i<r.columnas().size();i++) total.add(celda(i==0?"TOTAL ("+r.filas().size()+" filas)":r.totales().get(r.columnas().get(i))));
        return b.append(String.join(",",total)).append("\r\n").toString();
    }
}
