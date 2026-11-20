package co.edu.sena.perfumeria.dominio;
import java.math.BigDecimal;
public record Componente(Articulo articulo,BigDecimal cantidad) {
    public Componente {
        if(cantidad.signum()<=0)throw new IllegalArgumentException("Cantidad positiva requerida");
    }
}
