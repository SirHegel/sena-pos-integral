package co.edu.sena.perfumeria.servicio;
import co.edu.sena.perfumeria.nucleo.*;
import static co.edu.sena.perfumeria.nucleo.Datos.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service public class OperacionVentaService {
    private final VentasService ventas;
    private final CajaService caja;
    public OperacionVentaService(VentasService ventas,CajaService caja) {
        this.ventas=ventas;
        this.caja=caja;
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> emitir(Actor actor,String id,Map<String,Object> datos) {
        ventas.propia(actor,id,true);
        boolean credito=V.booleano(datos.get("credito"),"credito");
        if(credito) {
            conflicto(datos.containsKey("cobro"),"El fiado se emite sin cobro anidado; registre abonos separados");
            return ventas.emitir(actor,id,datos);
        }
        if(!(datos.get("cobro") instanceof Map<?,?>))throw V.error("cobro","Contado requiere cobro confirmado en la misma operación");
        @SuppressWarnings("unchecked") var cobro=new LinkedHashMap<String,Object>((Map<String,Object>)datos.get("cobro"));
        String key=texto(datos,"clave_operacion");
        conflicto(dinero(cobro,"importe_cop",false).compareTo(ventas.dominio(id).total())!=0,"El contado debe cobrar el total exacto de la venta");
        cobro.put("clave_operacion","CONTADO:"+hash(Map.of("venta_id",id,"clave",key)));
        ventas.emitir(actor,id,datos);
        caja.cobrar(actor,id,cobro);
        return ventas.detalle(actor,id);
    }
}
