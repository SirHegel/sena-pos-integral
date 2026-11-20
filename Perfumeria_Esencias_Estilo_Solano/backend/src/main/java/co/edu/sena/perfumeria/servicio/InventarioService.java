package co.edu.sena.perfumeria.servicio;
import co.edu.sena.perfumeria.nucleo.*;
import co.edu.sena.perfumeria.dominio.*;
import static co.edu.sena.perfumeria.nucleo.Datos.*;
import java.util.*;
import java.math.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class InventarioService {
    private final Db db;
    public InventarioService(Db db) {
        this.db=db;
    }
    public Articulo articulo(String id) {
        var r=db.requerido("SELECT * FROM articulo WHERE articulo_id=? AND activo=true",id);
        return new Articulo(id,r.get("codigo").toString(),r.get("nombre").toString(),Unidad.valueOf(r.get("unidad_base").toString()),r.get("clase").toString(),r.get("funcion_material").toString(),r.get("contenido_nominal_ml")==null?null:n(r.get("contenido_nominal_ml")));
    }
    public CostoPromedio saldo(String id) {
        var r=db.requerido("SELECT COALESCE(SUM(cantidad_base),0)saldo,COALESCE(SUM(cantidad_base*costo_unitario_base_cop),0)valor FROM v_movimiento_base WHERE articulo_id=?",id);
        return new CostoPromedio(n(r.get("saldo")),n(r.get("valor")));
    }
    public boolean valoracionPendiente(String id) {
        return n(db.requerido("SELECT COUNT(*) n FROM v_movimiento_base WHERE articulo_id=? AND costo_unitario_base_cop IS NULL",id).get("n")).signum()>0;
    }
    public void exigirValoracion(String id) {
        conflicto(valoracionPendiente(id),"El artículo tiene movimientos sin valoración; no se sustituye el costo por cero");
    }
    public void bloquear(Collection<String> ids) {
        new TreeSet<>(ids).forEach(id->db.requerido("SELECT articulo_id FROM articulo WHERE articulo_id=? FOR UPDATE",id));
    }
    public void renglon(String doc,int no,Articulo a,BigDecimal delta,BigDecimal cost) {
        if(a.unidad()==Unidad.UNIDAD&&delta.stripTrailingZeros().scale()>0)throw V.error("cantidad","El artículo se cuenta en unidades enteras");
        db.cambiar("INSERT INTO renglon_inventario VALUES (?,?,?,?,?,?)",doc,no,a.id(),delta,a.unidad().name(),cost);
    }
    public String documento(Actor a,String tipo,String key,String huella,String venta,Integer linea,String prov,String factura,String incidencia,String motivo) {
        String id=id();
        db.cambiar("INSERT INTO documento_inventario(documento_id,tipo,registrado_en,estado,registrado_por_usuario_id,venta_id,linea_no,proveedor_id,referencia_factura,incidencia_id,motivo,clave_operacion,huella_solicitud) VALUES (?,?,UTC_TIMESTAMP(6),'CONFIRMADO',?,?,?,?,?,?,?,?,?)",id,tipo,a.usuarioId(),venta,linea,prov,factura,incidencia,motivo,key,huella);
        return id;
    }
    public Optional<String> repetida(String table,String idcol,String key,String hash) {
        var r=db.uno("SELECT "+idcol+",huella_solicitud FROM "+table+" WHERE clave_operacion=?",key);
        if(r.isEmpty())return Optional.empty();
        conflicto(!hash.equals(r.get().get("huella_solicitud")),"La clave ya se usó con otro contenido");
        return Optional.of(r.get().get(idcol).toString());
    }
    public List<Map<String,Object>> articulos(Actor a) {
        var rows=db.lista("SELECT a.*,s.stock_base saldo_base FROM articulo a JOIN v_stock_base s ON s.articulo_id=a.articulo_id ORDER BY a.codigo");
        for(var r:rows) {
            BigDecimal stock=n(r.get("saldo_base"));
            r.put("alerta",n(r.get("minimo_base")).signum()>0&&stock.compareTo(n(r.get("minimo_base")))<=0);
            if(a.admin()) {
                boolean pendiente=valoracionPendiente(r.get("articulo_id").toString());
                r.put("valoracion_pendiente",pendiente);
                r.put("costo_promedio_cop",pendiente?null:saldo(r.get("articulo_id").toString()).unitario());
            }
        }
        return rows;
    }
    public List<Map<String,Object>> movimientos(Actor a,String id) {
        a.exigirAdmin();
        return id==null?db.lista("SELECT m.*,a.codigo,a.nombre FROM v_movimiento_base m JOIN articulo a ON a.articulo_id=m.articulo_id ORDER BY m.registrado_en DESC LIMIT 500"):db.lista("SELECT * FROM v_movimiento_base WHERE articulo_id=? ORDER BY registrado_en DESC LIMIT 500",V.uuid(id,"articulo_id"));
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> recepcion(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        String key=texto(m,"clave_operacion"),hash=hash(Map.of("operacion","RECEPCION","datos",m));
        var old=repetida("documento_inventario","documento_id",key,hash);
        if(old.isPresent())return Map.of("id",old.get());
        var lines=filas(m,"lineas");
        bloquear(lines.stream().map(x->ref(x,"articulo_id")).toList());
        String doc=documento(a,"RECEPCION",key,hash,null,null,ref(m,"proveedor_id"),texto(m,"documento_proveedor"),null,"Recepción de mercancía");
        BigDecimal total=BigDecimal.ZERO;
        int i=0;
        for(var l:lines) {
            Articulo ar=articulo(ref(l,"articulo_id"));
            BigDecimal qty;
            try {
                qty=Unidad.valueOf(op(l,"unidad_codigo","ML","L","UNIDAD","G")).convertir(cant(l,"cantidad"),ar.unidad());
            }
            catch(IllegalArgumentException e) {
                throw V.error("unidad_codigo",e.getMessage());
            }
            BigDecimal cost=dinero(l,"costo_total_cop",true);
            renglon(doc,++i,ar,qty,cost.divide(qty,8,RoundingMode.HALF_UP));
            total=total.add(cost);
        }
        db.cambiar("UPDATE documento_inventario SET total_documento_cop=? WHERE documento_id=?",total,doc);
        return Map.of("id",doc);
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> apertura(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        String key=texto(m,"clave_operacion"),hash=hash(Map.of("operacion","APERTURA","datos",m));
        var old=repetida("documento_inventario","documento_id",key,hash);
        if(old.isPresent())return Map.of("id",old.get());
        String from=ref(m,"articulo_sellado_id"),to=ref(m,"articulo_abierto_id");
        conflicto(from.equals(to),"Origen y destino deben ser diferentes");
        bloquear(List.of(from,to));
        Articulo sellado=articulo(from),abierto=articulo(to);
        conflicto(sellado.unidad()!=Unidad.UNIDAD||!sellado.clase().equals("PERFUME_SELLADO")||abierto.unidad()!=Unidad.ML||!abierto.clase().equals("PERFUME_ABIERTO"),"Apertura requiere perfume sellado en UNIDAD y abierto en ML");
        BigDecimal count=BigDecimal.valueOf(unidades(m,"unidades")),vol=cant(m,"volumen_util_ml"),nom=n(db.requerido("SELECT contenido_nominal_ml FROM articulo WHERE articulo_id=?",from).get("contenido_nominal_ml"));
        conflicto(nom.signum()<=0||vol.compareTo(nom.multiply(count))>0,"El volumen útil no puede superar el nominal de los frascos abiertos");
        exigirValoracion(from);
        var saldo=saldo(from);
        conflicto(saldo.saldo().compareTo(count)<0,"Stock sellado insuficiente");
        BigDecimal cost=saldo.costoSalida(count);
        String doc=documento(a,"TRANSFORMACION",key,hash,null,null,null,null,null,texto(m,"motivo"));
        renglon(doc,1,sellado,count.negate(),saldo.unitario());
        renglon(doc,2,abierto,vol,cost.divide(vol,8,RoundingMode.HALF_UP));
        return Map.of("id",doc);
    }
    public List<Map<String,Object>> recepciones(Actor a) {
        a.exigirAdmin();
        return db.lista("SELECT d.*,p.nombre proveedor_nombre,d.total_documento_cop total_cop FROM documento_inventario d LEFT JOIN proveedor p ON p.proveedor_id=d.proveedor_id WHERE d.tipo='RECEPCION' ORDER BY d.registrado_en DESC LIMIT 500");
    }
    public Map<String,Object> detalleRecepcion(Actor a,String id) {
        a.exigirAdmin();
        var r=db.requerido("SELECT * FROM documento_inventario WHERE documento_id=? AND tipo='RECEPCION'",id);
        r.put("lineas",db.lista("SELECT * FROM renglon_inventario WHERE documento_id=? ORDER BY linea_no",id));
        r.put("total_cop",r.get("total_documento_cop"));
        r.put("proveedor_nombre",db.requerido("SELECT nombre FROM proveedor WHERE proveedor_id=?",r.get("proveedor_id")).get("nombre"));
        return r;
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> conteo(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        var rows=filas(m,"renglones");
        conflicto(rows.stream().map(x->ref(x,"articulo_id")).distinct().count()!=rows.size(),"Un renglón por artículo contado");
        bloquear(rows.stream().map(x->ref(x,"articulo_id")).toList());
        String id=id();
        db.cambiar("INSERT INTO conteo_inventario VALUES (?,UTC_TIMESTAMP(6),?,'CERRADO',?)",id,a.usuarioId(),opcional(m,"observacion"));
        for(var row:rows) {
            Articulo ar=articulo(ref(row,"articulo_id"));
            String unit=op(row,"unidad_medicion","ML","L","UNIDAD","G");
            BigDecimal qty=V.decimal(row.get("cantidad_medida"),"cantidad_medida",6,BigDecimal.ZERO,false);
            try {
                BigDecimal base=Unidad.valueOf(unit).convertir(qty,ar.unidad());
                if(ar.unidad()==Unidad.UNIDAD&&base.stripTrailingZeros().scale()>0)throw new IllegalArgumentException("Unidades enteras requeridas");
            }
            catch(IllegalArgumentException e) {
                throw V.error("unidad_medicion",e.getMessage());
            }
            db.cambiar("INSERT INTO conteo_renglon VALUES (?,?,?,?,?,?)",id,ar.id(),qty,unit,saldo(ar.id()).saldo(),opcional(row,"observacion"));
        }
        return Map.of("id",id);
    }
    public List<Map<String,Object>> conteos(Actor a) {
        a.exigirAdmin();
        var rows=db.lista("SELECT * FROM conteo_inventario ORDER BY corte_en DESC LIMIT 500");
        for(var row:rows)row.put("renglones",db.lista("SELECT r.*,a.codigo,a.nombre,a.unidad_base FROM conteo_renglon r JOIN articulo a ON a.articulo_id=r.articulo_id WHERE r.conteo_id=?",row.get("conteo_id")));
        return rows;
    }
}
