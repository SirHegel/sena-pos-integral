package co.edu.sena.perfumeria.dominio;
import java.util.List;
import java.math.BigDecimal;
public record Formula(String id,BigDecimal volumen,List<Componente> componentes) {
    public Formula {
        componentes=List.copyOf(componentes);
        if(componentes.isEmpty()||componentes.stream().anyMatch(c->c.articulo().unidad()!=Unidad.ML))throw new IllegalArgumentException("La fórmula usa componentes en ML");
        if(componentes.stream().map(Componente::cantidad).reduce(BigDecimal.ZERO,BigDecimal::add).compareTo(volumen)!=0)throw new IllegalArgumentException("Los componentes no suman el volumen de preparación");
    }
}
