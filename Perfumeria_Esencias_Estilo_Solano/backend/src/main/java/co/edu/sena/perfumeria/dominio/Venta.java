package co.edu.sena.perfumeria.dominio;
import java.util.*;
import java.math.BigDecimal;
public record Venta(String id,List<LineaVenta> lineas,BigDecimal descuento) {
    public Venta {
        lineas=List.copyOf(lineas);
        if(lineas.isEmpty()||descuento.signum()<0)throw new IllegalArgumentException("Venta sin detalle o descuento inválido");
        if(descuento.compareTo(lineas.stream().map(LineaVenta::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add))>=0)throw new IllegalArgumentException("Descuento debe ser menor al subtotal");
    }
    public BigDecimal total() {
        return lineas.stream().map(LineaVenta::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add).subtract(descuento);
    }
    public Map<Articulo,BigDecimal> demanda() {
        var r=new LinkedHashMap<Articulo,BigDecimal>();
        lineas.forEach(l->l.especificacion().consumo(l.unidades()).forEach((a,n)->r.merge(a,n,BigDecimal::add)));
        return r;
    }
}
