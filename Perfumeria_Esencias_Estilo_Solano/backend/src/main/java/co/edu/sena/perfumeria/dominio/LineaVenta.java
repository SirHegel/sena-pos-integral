package co.edu.sena.perfumeria.dominio;
import java.math.BigDecimal;
public record LineaVenta(int numero,Especificacion especificacion,int unidades,BigDecimal precioUnitario) {
    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(unidades));
    }
}
