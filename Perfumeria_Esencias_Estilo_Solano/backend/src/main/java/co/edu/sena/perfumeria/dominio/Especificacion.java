package co.edu.sena.perfumeria.dominio;
import java.util.*;
import java.math.BigDecimal;
public record Especificacion(String id,String modalidad,Formula formula,List<Componente> materiales) {
    public Especificacion {
        materiales=List.copyOf(materiales);
        if("RECARGA".equals(modalidad)&&materiales.stream().anyMatch(c->"ENVASE".equals(c.articulo().clase())||(!Set.of("ETIQUETA","OTRO").contains(c.articulo().funcionMaterial()))))throw new IllegalArgumentException("La recarga sólo admite etiqueta adicional");
    }
    public Map<Articulo,BigDecimal> consumo(int unidades) {
        if(unidades<=0)throw new IllegalArgumentException("Unidades positivas requeridas");
        var r=new LinkedHashMap<Articulo,BigDecimal>();
        if(formula!=null)formula.componentes().forEach(c->r.merge(c.articulo(),c.cantidad().multiply(BigDecimal.valueOf(unidades)),BigDecimal::add));
        materiales.forEach(c->r.merge(c.articulo(),c.cantidad().multiply(BigDecimal.valueOf(unidades)),BigDecimal::add));
        return r;
    }
}
