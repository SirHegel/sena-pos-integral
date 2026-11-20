package co.edu.sena.perfumeria.servicio;

import co.edu.sena.perfumeria.dominio.Articulo;
import co.edu.sena.perfumeria.dominio.Unidad;
import co.edu.sena.perfumeria.nucleo.ApiError;
import co.edu.sena.perfumeria.nucleo.Db;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static co.edu.sena.perfumeria.servicio.DatosPrueba.*;

class CatalogoServiceTest {
    @Test void frascoSinCapacidadNoSeRegistraComoArticuloValido() {
        Db db=mock(Db.class);
        var service=new CatalogoService(db,mock(InventarioService.class));
        ApiError error=assertThrows(ApiError.class,()->service.articulo(ADMIN,Map.of(
            "unidad_base","UNIDAD","clase","ENVASE","funcion_material","FRASCO",
            "codigo","FRASCO-DEMO","nombre","Frasco DEMO")));
        assertEquals(409,error.estado());
        assertTrue(error.getMessage().contains("capacidad"));
        verifyNoInteractions(db);
    }

    @Test void recargaRechazaConsumirFrascoAunqueExistaUnaFormulaValida() {
        Db db=mock(Db.class); InventarioService inv=mock(InventarioService.class);
        when(db.requerido("SELECT * FROM oferta WHERE oferta_id=? FOR UPDATE",OFERTA))
            .thenReturn(fila("modalidad","RECARGA","fragancia_id",FRAGANCIA,"volumen_ml",n("30")));
        when(db.requerido("SELECT * FROM formula_version WHERE formula_id=? AND estado='APROBADA'",FORMULA))
            .thenReturn(fila("fragancia_id",FRAGANCIA,"volumen_preparado_ml",n("30")));
        when(inv.articulo(ARTICULO)).thenReturn(new Articulo(ARTICULO,"F30","Frasco DEMO",Unidad.UNIDAD,"ENVASE","FRASCO",n("30")));
        ApiError error=assertThrows(ApiError.class,()->new CatalogoService(db,inv).suministro(ADMIN,OFERTA,Map.of(
            "formula_id",FORMULA,"componentes",List.of(Map.of("articulo_id",ARTICULO,"cantidad_base","1")))));
        assertEquals(409,error.estado());
        assertTrue(error.getMessage().contains("etiqueta opcional"));
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }

    @Test void decantDeCincoMlNoAdmiteAtomizadorDeDiezMl() {
        Db db=mock(Db.class); InventarioService inv=mock(InventarioService.class);
        when(db.requerido("SELECT * FROM oferta WHERE oferta_id=? FOR UPDATE",OFERTA))
            .thenReturn(fila("modalidad","DECANT","volumen_ml",n("5")));
        when(inv.articulo(ABIERTO)).thenReturn(new Articulo(ABIERTO,"MADRE","Original DEMO",Unidad.ML,"PERFUME_ABIERTO","PERFUME",null));
        when(inv.articulo(ARTICULO)).thenReturn(new Articulo(ARTICULO,"A10","Atomizador DEMO",Unidad.UNIDAD,"ENVASE","ATOMIZADOR",n("10")));
        when(inv.articulo(ETIQUETA)).thenReturn(new Articulo(ETIQUETA,"ET","Etiqueta DEMO",Unidad.UNIDAD,"ACCESORIO","ETIQUETA",null));
        ApiError error=assertThrows(ApiError.class,()->new CatalogoService(db,inv).suministro(ADMIN,OFERTA,Map.of("componentes",List.of(
            Map.of("articulo_id",ABIERTO,"cantidad_base","5"),
            Map.of("articulo_id",ARTICULO,"cantidad_base","1"),
            Map.of("articulo_id",ETIQUETA,"cantidad_base","1")))));
        assertTrue(error.getMessage().contains("Capacidad"));
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }
}
