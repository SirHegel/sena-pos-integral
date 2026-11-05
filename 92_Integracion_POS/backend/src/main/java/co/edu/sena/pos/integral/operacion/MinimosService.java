package co.edu.sena.pos.integral.operacion;

import co.edu.sena.pos.integral.nucleo.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** Parámetro operativo por negocio, bodega y producto físico. */
@Service
public class MinimosService {
    private final Db db;
    public MinimosService(Db db) { this.db=db; }

    static Map<String,Object> presentar(Map<String,Object> fila) {
        var salida=new LinkedHashMap<String,Object>(fila);
        BigDecimal stock=new BigDecimal(fila.get("stock").toString());
        BigDecimal minimo=new BigDecimal(fila.get("minimo").toString());
        salida.put("alerta",minimo.signum()>0 && stock.compareTo(minimo)<=0);
        salida.put("agotado",stock.signum()==0);
        return salida;
    }

    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Map<String,Object> configurar(Actor actor,Map<String,Object> cuerpo) {
        actor.exigir("ADMINISTRADOR","INVENTARIO");
        if(cuerpo==null || !cuerpo.keySet().equals(Set.of("bodegaId","productoId","minimo")))
            throw V.error("cuerpo","se requieren únicamente bodegaId, productoId y minimo");
        String bodega=V.uuid(cuerpo.get("bodegaId"),"bodegaId");
        String producto=V.uuid(cuerpo.get("productoId"),"productoId");
        BigDecimal minimo=V.decimal(cuerpo.get("minimo"),"minimo",3,BigDecimal.ZERO,false);
        db.requerido("SELECT bodega_id FROM bodega WHERE tenant_id=? AND bodega_id=?",actor.tenantId(),bodega);
        var p=db.requerido("SELECT activo,modo_descuento FROM producto WHERE tenant_id=? AND producto_id=? FOR UPDATE",actor.tenantId(),producto);
        boolean activo=Boolean.TRUE.equals(p.get("activo")) || "1".equals(Objects.toString(p.get("activo"),""));
        if(!activo || !"DIRECTO".equals(p.get("modo_descuento")))
            throw new ApiError(409,"CONFLICTO_OPERATIVO","El mínimo requiere un producto físico activo con descuento directo");
        db.cambiar("INSERT INTO existencia(tenant_id,bodega_id,producto_id,stock,minimo) VALUES(?,?,?,0,?) ON DUPLICATE KEY UPDATE minimo=?",actor.tenantId(),bodega,producto,minimo,minimo);
        return presentar(db.requerido("SELECT e.*,p.codigo,p.nombre,p.unidad FROM existencia e JOIN producto p ON p.tenant_id=e.tenant_id AND p.producto_id=e.producto_id WHERE e.tenant_id=? AND e.bodega_id=? AND e.producto_id=?",actor.tenantId(),bodega,producto));
    }
}
