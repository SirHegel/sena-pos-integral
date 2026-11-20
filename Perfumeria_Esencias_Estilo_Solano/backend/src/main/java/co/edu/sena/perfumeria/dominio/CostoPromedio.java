package co.edu.sena.perfumeria.dominio;
import java.math.*;
public record CostoPromedio(BigDecimal saldo,BigDecimal valor) {
    public BigDecimal unitario() {
        return saldo.signum()==0?BigDecimal.ZERO:valor.divide(saldo,8,RoundingMode.HALF_UP);
    }
    public CostoPromedio recibir(BigDecimal cantidad,BigDecimal costoTotal) {
        if(cantidad.signum()<=0||costoTotal.signum()<0)throw new IllegalArgumentException("Recepción inválida");
        return new CostoPromedio(saldo.add(cantidad),valor.add(costoTotal));
    }
    public BigDecimal costoSalida(BigDecimal cantidad) {
        if(cantidad.signum()<=0||cantidad.compareTo(saldo)>0)throw new IllegalArgumentException("Existencias insuficientes");
        return unitario().multiply(cantidad).setScale(8,RoundingMode.HALF_UP);
    }
}
