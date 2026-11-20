package co.edu.sena.perfumeria.catalogo;
import java.math.BigDecimal;
import java.util.*;
/** Metadatos del artículo: no contiene precio, costo ni saldo editable. */
public class Articulo {
    private final String id,codigo,nombre,unidad,clase,funcion;
    private final BigDecimal nominal,minimo;
    private final boolean activo;
    public Articulo(String id,String codigo,String nombre,String unidad,String clase,String funcion,BigDecimal nominal,BigDecimal minimo,boolean activo) {
        this.id=uuid(id);
        this.codigo=texto(codigo,"Código");
        this.nombre=texto(nombre,"Nombre");
        this.unidad=op(unidad,Set.of("ML","UNIDAD","G"));
        this.clase=op(clase,Set.of("LIQUIDO","ENVASE","ACCESORIO","PERFUME_SELLADO","PERFUME_ABIERTO","MUESTRA"));
        this.funcion=op(funcion,Set.of("ESENCIA","ALCOHOL","FIJADOR","FEROMONAS","FRASCO","VALVULA","TAPA","ETIQUETA","ATOMIZADOR","PERFUME","OTRO"));
        this.nominal=nominal==null?null:decimal(nominal,true);
        this.minimo=decimal(minimo,false);
        this.activo=activo;
        if(Set.of("ENVASE","ACCESORIO","PERFUME_SELLADO").contains(clase)&&!unidad.equals("UNIDAD"))bad("Envases, accesorios y sellados requieren UNIDAD");
        if(clase.equals("PERFUME_ABIERTO")&&!unidad.equals("ML"))bad("Perfume abierto requiere ML");
        if(Set.of("ESENCIA","ALCOHOL","FIJADOR","FEROMONAS").contains(funcion)&&!unidad.equals("ML"))bad("Los insumos líquidos requieren ML");
        if(Set.of("FRASCO","VALVULA","TAPA","ETIQUETA","ATOMIZADOR").contains(funcion)&&!unidad.equals("UNIDAD"))bad("El material físico requiere UNIDAD");
        if(funcion.equals("PERFUME")&&!Set.of("PERFUME_SELLADO","PERFUME_ABIERTO").contains(clase))bad("PERFUME requiere una clase de perfume");
        if(Set.of("FRASCO","ATOMIZADOR").contains(funcion)&&nominal==null)bad("Frasco y atomizador requieren capacidad nominal ML");
    }
    private static void bad(String s) {
        throw new IllegalArgumentException(s);
    }
    public static String texto(String s,String campo) {
        if(s==null||s.isBlank())throw new IllegalArgumentException(campo+": obligatorio");
        s=s.strip();
        if(s.codePointCount(0,s.length())>160||s.codePoints().anyMatch(Character::isISOControl))bad(campo+": máximo 160 caracteres sin controles");
        return s;
    }
    public static String uuid(String s) {
        try {
            if(s!=null&&UUID.fromString(s).toString().equals(s))return s;
        }
        catch(IllegalArgumentException ignored) {
        }
        throw new IllegalArgumentException("Identificador no válido");
    }
    private static String op(String s,Set<String> values) {
        if(!values.contains(s))bad("Opción de catálogo no válida");
        return s;
    }
    public static BigDecimal decimal(BigDecimal d,boolean positivo) {
        if(d==null||d.signum()<0||(positivo&&d.signum()==0)||Math.max(0,d.stripTrailingZeros().scale())>6||d.compareTo(new BigDecimal("999999999999.999999"))>0)bad("Cantidad fuera del rango DECIMAL(18,6)");
        return d;
    }
    public Articulo editar(String nuevoNombre,BigDecimal nuevoMinimo) {
        return new Articulo(id,codigo,nuevoNombre,unidad,clase,funcion,nominal,nuevoMinimo,activo);
    }
    public String getId() {
        return id;
    }
    public String getCodigo() {
        return codigo;
    }
    public String getNombre() {
        return nombre;
    }
    public String getUnidad() {
        return unidad;
    }
    public String getClase() {
        return clase;
    }
    public String getFuncion() {
        return funcion;
    }
    public BigDecimal getNominal() {
        return nominal;
    }
    public BigDecimal getMinimo() {
        return minimo;
    }
    public boolean isActivo() {
        return activo;
    }
}
