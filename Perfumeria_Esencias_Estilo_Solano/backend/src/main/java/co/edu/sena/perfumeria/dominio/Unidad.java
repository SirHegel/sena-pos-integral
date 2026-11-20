package co.edu.sena.perfumeria.dominio;
import java.math.BigDecimal;
public enum Unidad {
    ML("VOLUMEN","1"),L("VOLUMEN","1000"),G("MASA","1"),UNIDAD("CONTEO","1");
    private final String dimension;
    private final BigDecimal factor;
    Unidad(String d,String f) {
        dimension=d;
        factor=new BigDecimal(f);
    }
    public BigDecimal convertir(BigDecimal cantidad,Unidad destino) {
        if(!dimension.equals(destino.dimension))throw new IllegalArgumentException("Unidades incompatibles: "+this+" y "+destino);
        return cantidad.multiply(factor).divide(destino.factor);
    }
}
