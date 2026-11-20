package co.edu.sena.perfumeria.dominio;
import java.math.BigDecimal;
public record Articulo(String id,String codigo,String nombre,Unidad unidad,String clase,String funcionMaterial,BigDecimal contenidoNominalMl) {
    public Articulo(String id,String codigo,String nombre,Unidad unidad,String clase) {
        this(id,codigo,nombre,unidad,clase,"OTRO",null);
    }
}
