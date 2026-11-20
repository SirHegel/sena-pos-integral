package co.edu.sena.perfumeria.catalogo;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
class ArticuloTest {
 private Articulo crear(String nombre,String unidad,String clase,String funcion,String nominal,String minimo){return new Articulo("11111111-1111-4111-8111-111111111111","DEMO",nombre,unidad,clase,funcion,nominal==null?null:new BigDecimal(nominal),new BigDecimal(minimo),true);}
 @Test void nombreConComillasYHtmlEsDato(){assertEquals("Esencia <DEMO> & 'á'",crear("Esencia <DEMO> & 'á'","ML","LIQUIDO","ESENCIA",null,"0").getNombre());}
 @Test void minimoAdmiteSeisDecimales(){assertEquals(new BigDecimal("0.000001"),crear("DEMO","ML","LIQUIDO","ESENCIA",null,"0.000001").getMinimo());}
 @Test void rechazaSeptimoDecimal(){assertThrows(IllegalArgumentException.class,()->crear("DEMO","ML","LIQUIDO","ESENCIA",null,"0.0000001"));}
 @Test void rechazaMinimoNegativo(){assertThrows(IllegalArgumentException.class,()->crear("DEMO","ML","LIQUIDO","ESENCIA",null,"-1"));}
 @Test void nombre160Y161(){assertDoesNotThrow(()->crear("Á".repeat(160),"ML","LIQUIDO","ESENCIA",null,"0"));assertThrows(IllegalArgumentException.class,()->crear("Á".repeat(161),"ML","LIQUIDO","ESENCIA",null,"0"));}
 @Test void litroNoEsUnidadBase(){assertThrows(IllegalArgumentException.class,()->crear("DEMO","L","LIQUIDO","ESENCIA",null,"0"));}
 @Test void frascoRequiereCapacidadYConteo(){assertThrows(IllegalArgumentException.class,()->crear("DEMO","UNIDAD","ENVASE","FRASCO",null,"0"));assertThrows(IllegalArgumentException.class,()->crear("DEMO","ML","ENVASE","FRASCO","30","0"));}
 @Test void edicionConservaMetadatos(){var a=crear("Frasco DEMO","UNIDAD","ENVASE","FRASCO","30","2");var editado=a.editar("Nuevo nombre",new BigDecimal("3"));assertAll(()->assertEquals(a.getCodigo(),editado.getCodigo()),()->assertEquals(a.getUnidad(),editado.getUnidad()),()->assertEquals(a.getNominal(),editado.getNominal()),()->assertEquals(a.getFuncion(),editado.getFuncion()));}
 @Test void vendedoraNoModifica(){assertThrows(SecurityException.class,()->new Actor("id","DEMO","VENDEDORA").exigirAdmin());}
 @Test void uuidDebeSerCanonico(){assertThrows(IllegalArgumentException.class,()->Articulo.uuid("1-1-1-1-1"));}
 @Test void perfumeAbiertoUsaMl(){assertThrows(IllegalArgumentException.class,()->crear("DEMO","UNIDAD","PERFUME_ABIERTO","PERFUME","100","0"));}
}
