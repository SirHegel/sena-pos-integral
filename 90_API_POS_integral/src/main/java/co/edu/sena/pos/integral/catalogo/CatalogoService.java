package co.edu.sena.pos.integral.catalogo;

import co.edu.sena.pos.integral.nucleo.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CatalogoService {
    private final Db db;
    private static final String CAMPOS = "producto_id,codigo,nombre,precio_vigente,tasa_iva,tipo,unidad,modo_descuento,activo";
    public CatalogoService(Db db) { this.db = db; }
    public record Producto(String codigo, String nombre, BigDecimal precioVigente, BigDecimal tasaIva, String tipo, String unidad, String modoDescuento, Boolean activo) {}
    public List<Map<String,Object>> lista(Actor a, String busqueda) {
        String q = busqueda == null ? "" : busqueda.strip();
        if (q.length() > 128 || q.codePoints().anyMatch(Character::isISOControl)) throw V.error("q", "máximo128 caracteres sin controles");
        return db.lista("SELECT " + CAMPOS + " FROM producto WHERE tenant_id=? AND (LOCATE(LOWER(?),LOWER(codigo))>0 OR LOCATE(LOWER(?),LOWER(nombre))>0) ORDER BY codigo", a.tenantId(), q, q);
    }
    public Map<String,Object> uno(Actor a, String id) {
        return db.requerido("SELECT " + CAMPOS + " FROM producto WHERE tenant_id=? AND producto_id=?", a.tenantId(), V.uuid(id,"productoId"));
    }
    @Transactional
    public Map<String,Object> crear(Actor a, Producto r) {
        Producto p = validar(r); String id = UUID.randomUUID().toString();
        db.cambiar("INSERT INTO producto(tenant_id,producto_id,codigo,nombre,precio_vigente,tasa_iva,tipo,unidad,modo_descuento,activo) VALUES(?,?,?,?,?,?,?,?,?,?)", a.tenantId(), id, p.codigo(), p.nombre(), p.precioVigente(), p.tasaIva(), p.tipo(), p.unidad(), p.modoDescuento(), p.activo());
        return uno(a,id);
    }
    @Transactional
    public Map<String,Object> cambiar(Actor a, String id, Producto r) {
        id = V.uuid(id, "productoId"); Producto p = validar(r);
        var viejo = db.requerido("SELECT " + CAMPOS + " FROM producto WHERE tenant_id=? AND producto_id=? FOR UPDATE", a.tenantId(),id);
        boolean estructuraCambia = !viejo.get("tipo").equals(p.tipo()) || !viejo.get("modo_descuento").equals(p.modoDescuento()) || !viejo.get("unidad").equals(p.unidad());
        if (estructuraCambia && (db.existe("SELECT 1 FROM detalle_venta WHERE tenant_id=? AND producto_id=? LIMIT 1",a.tenantId(),id) || db.existe("SELECT 1 FROM detalle_receta WHERE tenant_id=? AND producto_insumo_id=? LIMIT 1",a.tenantId(),id) || db.existe("SELECT 1 FROM receta WHERE tenant_id=? AND producto_plato_id=? LIMIT 1",a.tenantId(),id) || db.existe("SELECT 1 FROM existencia WHERE tenant_id=? AND producto_id=? LIMIT 1",a.tenantId(),id)))
            throw new ApiError(409,"PRODUCTO_REFERENCIADO","No se puede cambiar tipo, modalidad o unidad de un producto ya utilizado");
        db.cambiar("UPDATE producto SET codigo=?,nombre=?,precio_vigente=?,tasa_iva=?,tipo=?,unidad=?,modo_descuento=?,activo=? WHERE tenant_id=? AND producto_id=?",p.codigo(),p.nombre(),p.precioVigente(),p.tasaIva(),p.tipo(),p.unidad(),p.modoDescuento(),p.activo(),a.tenantId(),id);
        return uno(a,id);
    }
    private Producto validar(Producto p) {
        if (p == null || p.activo() == null) throw V.error("producto", "cuerpo y activo booleano requeridos");
        String codigo=V.texto(p.codigo(),"codigo",64),nombre=V.texto(p.nombre(),"nombre",128),unidad=V.texto(p.unidad(),"unidad",32);
        String tipo=V.opcion(p.tipo(),"tipo",Set.of("VENDIBLE","INSUMO","PLATO")),modo=V.opcion(p.modoDescuento(),"modoDescuento",Set.of("DIRECTO","RECETA"));
        BigDecimal precio=V.decimal(p.precioVigente(),"precioVigente",2,BigDecimal.ZERO,true),iva=V.decimal(p.tasaIva(),"tasaIva",4,BigDecimal.ZERO,false);
        if (iva.compareTo(BigDecimal.ONE)>0) throw V.error("tasaIva","debe estar entre0 y1");
        if (modo.equals("RECETA")&&!tipo.equals("PLATO")) throw V.error("modoDescuento","RECETA requiere tipo PLATO");
        return new Producto(codigo,nombre,precio,iva,tipo,unidad,modo,p.activo());
    }
}
