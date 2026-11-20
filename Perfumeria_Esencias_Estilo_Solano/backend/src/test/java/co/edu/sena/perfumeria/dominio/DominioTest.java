package co.edu.sena.perfumeria.dominio;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.math.*;
import java.util.*;
import co.edu.sena.perfumeria.nucleo.*;
import co.edu.sena.perfumeria.servicio.ReportesService;
import java.util.zip.*;
import java.io.*;
class DominioTest {
    Articulo a(String id,Unidad u,String clase) {
        return new Articulo(id,id,id,u,clase);
    }
    @ParameterizedTest @CsvSource( {
        "30,10,0.6,19.4","50,16.5,1,32.5","100,33,2,65","30,12,1.2,16.8","50,20,2,28","100,40,4,56"
    }
    ) void conservaCantidadesReportadas(String volumen,String e,String f,String al) {
        var cc=List.of(new Componente(a("esencia",Unidad.ML,"LIQUIDO"),new BigDecimal(e)),new Componente(a("fijador",Unidad.ML,"LIQUIDO"),new BigDecimal(f)),new Componente(a("alcohol",Unidad.ML,"LIQUIDO"),new BigDecimal(al)));
        var formula=new Formula("v1",new BigDecimal(volumen),cc);
        var spec=new Especificacion("s1","PREPARACION",formula,List.of(new Componente(a("envase",Unidad.UNIDAD,"ENVASE"),BigDecimal.ONE)));
        assertEquals(0,spec.consumo(2).get(cc.getFirst().articulo()).compareTo(new BigDecimal(e).multiply(new BigDecimal("2"))));
        assertEquals(new BigDecimal("2"),spec.consumo(2).get(a("envase",Unidad.UNIDAD,"ENVASE")));
    }
    @ParameterizedTest @CsvSource( {
        "30,12,0.6,0.6,16.8","50,20,1,1,28","100,40,2,2,56"
    }
    ) void feromonasNoDuplicanVolumen(String vol,String e,String f,String p,String al) {
        var q=List.of(new Componente(a("e",Unidad.ML,"LIQUIDO"),new BigDecimal(e)),new Componente(a("f",Unidad.ML,"LIQUIDO"),new BigDecimal(f)),new Componente(a("p",Unidad.ML,"LIQUIDO"),new BigDecimal(p)),new Componente(a("al",Unidad.ML,"LIQUIDO"),new BigDecimal(al)));
        assertDoesNotThrow(()->new Formula("f",new BigDecimal(vol),q));
    }
    @Test void litrosMlExactosYMasaNoSeAdivina() {
        assertEquals(0,Unidad.L.convertir(new BigDecimal("1.125"),Unidad.ML).compareTo(new BigDecimal("1125")));
        assertThrows(IllegalArgumentException.class,()->Unidad.G.convertir(BigDecimal.ONE,Unidad.ML));
    }
    @Test void formulaNoMezclaUnidadesNiSumaIncorrecta() {
        assertThrows(IllegalArgumentException.class,()->new Formula("f",new BigDecimal("30"),List.of(new Componente(a("env",Unidad.UNIDAD,"ENVASE"),new BigDecimal("30")))));
        assertThrows(IllegalArgumentException.class,()->new Formula("f",new BigDecimal("30"),List.of(new Componente(a("e",Unidad.ML,"LIQUIDO"),new BigDecimal("29.9")))));
    }
    @Test void recargaNoConsumeEnvase() {
        assertThrows(IllegalArgumentException.class,()->new Especificacion("r","RECARGA",null,List.of(new Componente(a("env",Unidad.UNIDAD,"ENVASE"),BigDecimal.ONE))));
    }
    @Test void promedioPonderadoYSalidaHistorica() {
        var p=new CostoPromedio(new BigDecimal("10"),new BigDecimal("100")).recibir(new BigDecimal("10"),new BigDecimal("300"));
        assertEquals(0,p.unitario().compareTo(new BigDecimal("20")));
        assertEquals(0,p.costoSalida(new BigDecimal("5")).compareTo(new BigDecimal("100")));
        assertThrows(IllegalArgumentException.class,()->p.costoSalida(new BigDecimal("21")));
    }
    @Test void decantNoTieneMermaImplicita() {
        var original=a("abierto",Unidad.ML,"PERFUME_ABIERTO");
        var e=new Especificacion("d","DECANT",null,List.of(new Componente(original,new BigDecimal("5"))));
        assertEquals(0,e.consumo(19).get(original).compareTo(new BigDecimal("95")));
    }
    @Test void ventaAgregaDemandaYConservaDescuento() {
        var ar=a("e",Unidad.ML,"LIQUIDO");
        var es=new Especificacion("d","DECANT",null,List.of(new Componente(ar,new BigDecimal("5"))));
        var v=new Venta("v",List.of(new LineaVenta(1,es,2,new BigDecimal("10000")),new LineaVenta(2,es,1,new BigDecimal("9000"))),new BigDecimal("3000"));
        assertEquals(0,v.total().compareTo(new BigDecimal("26000")));
        assertEquals(0,v.demanda().get(ar).compareTo(new BigDecimal("15")));
    }
    @Test void permisoNoDependeDePantalla() {
        var v=new Actor("u","Demo","VENDEDORA");
        assertThrows(ApiError.class,v::exigirAdmin);
        assertThrows(ApiError.class,()->v.exigirPropio("otra"));
        assertDoesNotThrow(()->v.exigirPropio("u"));
    }
    @Test void hashCanonicoYContenidoDiferente() {
        assertEquals(Datos.hash(Map.of("a",1,"b",2)),Datos.hash(Map.of("b",2,"a",1)));
        assertNotEquals(Datos.hash(Map.of("a",1)),Datos.hash(Map.of("a",2)));
    }
    @Test void libroExcelEsZipOpenXmlYTextoLiteral()throws Exception {
        var sheets=new LinkedHashMap<String,List<Map<String,Object>>>();
        var row=new LinkedHashMap<String,Object>();
        row.put("nombre","=SUM(A1:A2)");
        row.put("importe",new BigDecimal("25000.50"));
        sheets.put("Ventas",List.of(row));
        var bytes=ReportesService.libro(sheets);
        var found=new HashMap<String,String>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) {
            for(ZipEntry e;(e=zip.getNextEntry())!=null;)found.put(e.getName(),new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8));
        }
        assertTrue(found.containsKey("[Content_Types].xml"));
        String xml=found.get("xl/worksheets/sheet1.xml");
        assertTrue(xml.contains("t=\"inlineStr\""));
        assertTrue(xml.contains("=SUM(A1:A2)"));
        assertFalse(xml.contains("<f>"));
        assertTrue(xml.contains("25000.50"));
    }
    @Test void hashNoConfundeDelimitadoresNiTipos() {
        assertNotEquals(Datos.hash(Map.of("motivo","A;nota:B","tipo","MERMA")),Datos.hash(Map.of("motivo","A","nota","B","tipo","MERMA")));
        assertNotEquals(Datos.hash(Map.of("x",true)),Datos.hash(Map.of("x","true")));
        assertNotEquals(Datos.hash(Map.of("x",1)),Datos.hash(Map.of("x","1")));
        assertNotEquals(Datos.hash(Arrays.asList((Object)null)),Datos.hash(List.of("null")));
    }
    @Test void claveConservaEspaciosYLimitaBytesBcrypt() {
        assertEquals(" 1234567890 ",V.clave(" 1234567890 ","clave",true));
        assertThrows(ApiError.class,()->V.clave("á".repeat(37),"clave",true));
        assertDoesNotThrow(()->V.clave("á".repeat(36),"clave",true));
        assertThrows(ApiError.class,()->V.clave("corta","clave",true));
    }
}
