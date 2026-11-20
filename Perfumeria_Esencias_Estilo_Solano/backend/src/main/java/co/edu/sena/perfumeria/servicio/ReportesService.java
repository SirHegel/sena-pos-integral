package co.edu.sena.perfumeria.servicio;
import co.edu.sena.perfumeria.nucleo.*;
import static co.edu.sena.perfumeria.nucleo.Datos.*;
import java.util.*;
import java.math.*;
import java.time.*;
import java.sql.Timestamp;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;
import org.springframework.stereotype.Service;
@Service public class ReportesService {
    private final Db db;
    private final InventarioService inv;
    private final VentasService ventas;
    public ReportesService(Db d,InventarioService i,VentasService v) {
        db=d;
        inv=i;
        ventas=v;
    }
    private List<Map<String,Object>> acotado(String sql,Object... args) {
        var r=db.lista(sql+" LIMIT 10001",args);
        conflicto(r.size()>10000,"El reporte supera10000filas; acote el período");
        return r;
    }
    public Map<String,Object> reporte(Actor a,String tipo,String desde,String hasta) {
        LocalDate from=desde==null?LocalDate.now(ZoneId.of("America/Bogota")):V.fecha(desde,"desde"),to=hasta==null?from:V.fecha(hasta,"hasta");
        conflicto(to.isBefore(from)||java.time.temporal.ChronoUnit.DAYS.between(from,to)>365,"Período debe contener1a366días");
        Timestamp start=Timestamp.from(from.atStartOfDay(ZoneId.of("America/Bogota")).toInstant()),end=Timestamp.from(to.plusDays(1).atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        List<Map<String,Object>> rows;
        String own=a.admin()?"":" AND v.vendedora_usuario_id=?";
        Object[] ventaArgs=a.admin()?new Object[] {
            start,end
        }
        :new Object[] {
            start,end,a.usuarioId()
        }
        ;
        if(!a.admin()&&!tipo.equals("ventas"))a.exigirAdmin();
        switch(tipo) {
            case "ventas"->rows=acotado("SELECT v.venta_id,v.numero_recibo,v.emitida_en,u.nombre_acceso vendedora,c.nombre cliente,t.bruto bruto_cop,t.descuento descuento_cop,t.total total_cop,t.cobrado cobrado_cop,t.saldo saldo_cop FROM venta v JOIN v_total_venta t ON t.venta_id=v.venta_id JOIN usuario u ON u.usuario_id=v.vendedora_usuario_id LEFT JOIN cliente c ON c.cliente_id=v.cliente_id WHERE v.estado='EMITIDA' AND v.emitida_en>=? AND v.emitida_en<?"+own+" ORDER BY v.emitida_en",ventaArgs);
            case "medios"->rows=acotado("SELECT c.medio,COUNT(*) cobros,SUM(c.importe_cop) cobrado_cop FROM cobro c WHERE c.confirmado_en>=? AND c.confirmado_en<? GROUP BY c.medio ORDER BY c.medio",start,end);
            case "rotacion"->rows=acotado("SELECT o.codigo_venta,o.nombre_venta,o.modalidad,o.volumen_ml,g.nombre_comercial fragancia,SUM(l.cantidad_unidades) unidades_vendidas,SUM(l.cantidad_unidades*l.precio_unitario_cop) bruto_cop FROM linea_venta l JOIN venta v ON v.venta_id=l.venta_id JOIN especificacion_suministro e ON e.especificacion_id=l.especificacion_id JOIN oferta o ON o.oferta_id=e.oferta_id LEFT JOIN fragancia g ON g.fragancia_id=o.fragancia_id WHERE v.estado='EMITIDA' AND v.emitida_en>=? AND v.emitida_en<? GROUP BY o.oferta_id,o.codigo_venta,o.nombre_venta,o.modalidad,o.volumen_ml,g.nombre_comercial ORDER BY unidades_vendidas DESC,o.codigo_venta",start,end);
            case "costos"-> {
                rows=acotado("SELECT v.venta_id,v.numero_recibo,t.total ingreso_cop,c.costo_materiales costo_materiales_cop,c.costos_pendientes,(t.total-c.costo_materiales) margen_bruto_cop FROM venta v JOIN v_total_venta t ON t.venta_id=v.venta_id LEFT JOIN v_costo_venta c ON c.venta_id=v.venta_id WHERE v.estado='EMITIDA' AND v.emitida_en>=? AND v.emitida_en<? ORDER BY v.emitida_en",start,end);
                for(var r:rows)if(r.get("costo_materiales_cop")==null||n(r.get("costos_pendientes")).signum()>0)r.put("margen_bruto_cop",null);
            }
            case "vendedoras"->rows=acotado("SELECT u.usuario_id,u.nombre_acceso,u.perfil,i.nombre_visible,COUNT(*) ventas,SUM(t.bruto) bruto_cop,SUM(t.descuento) descuento_cop,SUM(t.total) vendido_cop,SUM(t.cobrado) cobrado_cop FROM venta v JOIN v_total_venta t ON t.venta_id=v.venta_id JOIN usuario u ON u.usuario_id=v.vendedora_usuario_id JOIN integrante_equipo i ON i.integrante_id=u.integrante_id WHERE v.estado='EMITIDA' AND v.emitida_en>=? AND v.emitida_en<? GROUP BY u.usuario_id,u.nombre_acceso,u.perfil,i.nombre_visible ORDER BY vendido_cop DESC",start,end);
            case "devoluciones"->rows=acotado("SELECT mc.movimiento_caja_id,i.incidencia_id,i.venta_id,v.numero_recibo,mc.registrado_en,mc.importe_cop devolucion_cop,mc.motivo,mc.turno_id FROM movimiento_caja mc JOIN incidencia i ON i.incidencia_id=mc.incidencia_id JOIN venta v ON v.venta_id=i.venta_id WHERE mc.registrado_en>=? AND mc.registrado_en<? ORDER BY mc.registrado_en",start,end);
            case "gastos"->rows=acotado("SELECT gasto_id,fecha,concepto,importe_cop,medio,soporte_ref FROM gasto WHERE fecha>=? AND fecha<=? ORDER BY fecha",from,to);
            default->throw V.error("tipo","ventas,medios,rotacion,costos,vendedoras,gastos o devoluciones");
        }
        if(Set.of("ventas","costos").contains(tipo))for(var row:rows) {
            String sale=row.get("venta_id").toString();
            BigDecimal refund=n(db.requerido("SELECT COALESCE(SUM(m.importe_cop),0)n FROM movimiento_caja m JOIN incidencia i ON i.incidencia_id=m.incidencia_id WHERE i.venta_id=?",sale).get("n"));
            row.put("devoluciones_acumuladas_cop",refund);
            BigDecimal original=n(row.get(tipo.equals("ventas")?"total_cop":"ingreso_cop"));
            row.put("ingreso_ajustado_cop",original.subtract(refund));
            if(tipo.equals("costos")) {
                BigDecimal ajuste=n(db.requerido("SELECT COALESCE(-SUM(r.cantidad_base*r.costo_unitario_base_cop),0)n FROM v_movimiento_base r JOIN documento_inventario d ON d.documento_id=r.documento_id JOIN incidencia i ON i.incidencia_id=d.incidencia_id WHERE i.venta_id=?",sale).get("n"));
                row.put("costo_ajustes_cop",ajuste);
                if(row.get("margen_bruto_cop")!=null) {
                    BigDecimal cost=n(row.get("costo_materiales_cop")).add(ajuste);
                    row.put("costo_materiales_ajustado_cop",cost);
                    row.put("margen_bruto_ajustado_cop",original.subtract(refund).subtract(cost));
                }
                else {
                    row.put("costo_materiales_ajustado_cop",null);
                    row.put("margen_bruto_ajustado_cop",null);
                }
            }
        }
        return Map.of("tipo",tipo,"desde",from.toString(),"hasta",to.toString(),"zona","America/Bogota","filas",rows);
    }
    public Map<String,Object> inicio(Actor a) {
        var arts=inv.articulos(a);
        var rows=ventas.listar(a);
        var r=new LinkedHashMap<String,Object>();
        r.put("articulos",arts.size());
        r.put("alertas",arts.stream().filter(x->Boolean.TRUE.equals(x.get("alerta"))).toList());
        r.put("ventas",rows.size());
        if(a.admin())r.put("cartera",ventas.cartera(a).stream().map(x->n(x.get("saldo_cop"))).reduce(BigDecimal.ZERO,BigDecimal::add));
        return r;
    }
    @SuppressWarnings("unchecked") public byte[] excel(Actor a,String mes) {
        a.exigirAdmin();
        YearMonth ym=V.periodo(mes,"mes");
        String desde=ym.atDay(1).toString(),hasta=ym.atEndOfMonth().toString();
        var sheets=new LinkedHashMap<String,List<Map<String,Object>>>();
        sheets.put("Ventas",(List<Map<String,Object>>)reporte(a,"ventas",desde,hasta).get("filas"));
        Timestamp start=Timestamp.from(ym.atDay(1).atStartOfDay(ZoneId.of("America/Bogota")).toInstant()),end=Timestamp.from(ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.of("America/Bogota")).toInstant());
        sheets.put("Cobros",acotado("SELECT cobro_id,venta_id,confirmado_en,medio,importe_cop,referencia_externa FROM cobro WHERE confirmado_en>=? AND confirmado_en<? ORDER BY confirmado_en",start,end));
        sheets.put("Gastos",(List<Map<String,Object>>)reporte(a,"gastos",desde,hasta).get("filas"));
        var cartera=ventas.cartera(a);
        String corte=java.time.Instant.now().toString();
        for(var row:cartera) {
            row.put("naturaleza_corte","CARTERA_VIGENTE_AL_GENERAR");
            row.put("consultada_en_utc",corte);
        }
        sheets.put("Cartera",cartera);
        sheets.put("Devoluciones",(List<Map<String,Object>>)reporte(a,"devoluciones",desde,hasta).get("filas"));
        return libro(sheets);
    }
    private static String xml(Object v) {
        return v==null?"":(v instanceof java.sql.Timestamp ts?ts.toInstant().toString():v instanceof java.time.LocalDateTime dt?dt.toInstant(java.time.ZoneOffset.UTC).toString():v.toString()).replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;").replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]","");
    }
    private static void entry(ZipOutputStream zip,String name,String text)throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(text.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
    public static byte[] libro(LinkedHashMap<String,List<Map<String,Object>>> sheets) {
        try {
            var out=new ByteArrayOutputStream();
            try(var z=new ZipOutputStream(out)) {
                StringBuilder ct=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default Extension=\"xml\" ContentType=\"application/xml\"/><Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
                StringBuilder book=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"),rels=new StringBuilder("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
                int no=0;
                for(var sheet:sheets.entrySet()) {
                    no++;
                    ct.append("<Override PartName=\"/xl/worksheets/sheet").append(no).append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                    book.append("<sheet name=\"").append(xml(sheet.getKey())).append("\" sheetId=\"").append(no).append("\" r:id=\"rId").append(no).append("\"/>");
                    rels.append("<Relationship Id=\"rId").append(no).append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet").append(no).append(".xml\"/>");
                    StringBuilder data=new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
                    List<String> headers=sheet.getValue().isEmpty()?List.of("Sin registros en el período"):new ArrayList<>(sheet.getValue().getFirst().keySet());
                    data.append("<row>");
                    headers.forEach(h->data.append("<c t=\"inlineStr\"><is><t>").append(xml(h)).append("</t></is></c>"));
                    data.append("</row>");
                    for(var row:sheet.getValue()) {
                        data.append("<row>");
                        for(String h:headers) {
                            Object v=row.get(h);
                            if(v instanceof Number)data.append("<c><v>").append(xml(v)).append("</v></c>");
                            else data.append("<c t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(xml(v)).append("</t></is></c>");
                        }
                        data.append("</row>");
                    }
                    data.append("</sheetData></worksheet>");
                    entry(z,"xl/worksheets/sheet"+no+".xml",data.toString());
                }
                entry(z,"[Content_Types].xml",ct.append("</Types>").toString());
                entry(z,"_rels/.rels","<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/></Relationships>");
                entry(z,"xl/workbook.xml",book.append("</sheets></workbook>").toString());
                entry(z,"xl/_rels/workbook.xml.rels",rels.append("</Relationships>").toString());
            }
            return out.toByteArray();
        }
        catch(IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
