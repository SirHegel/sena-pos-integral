package co.edu.sena.perfumeria.servicio;

import co.edu.sena.perfumeria.dominio.*;
import co.edu.sena.perfumeria.nucleo.ApiError;
import co.edu.sena.perfumeria.nucleo.Db;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static co.edu.sena.perfumeria.servicio.DatosPrueba.*;

class VentasServiceTest {
    @Test void vendedoraNoAutorizaUnaRebajaPorSolicitudDirecta() {
        Db db=mock(Db.class);
        var service=new VentasService(db,mock(CatalogoService.class),mock(InventarioService.class));
        assertEquals(403,assertThrows(ApiError.class,()->service.descuento(VENDEDORA,VENTA,Map.of("importe_cop","1000","motivo","Rebaja DEMO"))).estado());
        verifyNoInteractions(db);
    }

    @Test void rebajaIgualAlBrutoNoDejaUnaVentaConTotalCero() {
        Db db=mock(Db.class);
        when(db.requerido("SELECT * FROM venta WHERE venta_id=? FOR UPDATE",VENTA))
            .thenReturn(fila("vendedora_usuario_id",VENDEDORA.usuarioId(),"estado","BORRADOR"));
        when(db.requerido("SELECT bruto FROM v_total_venta WHERE venta_id=?",VENTA)).thenReturn(Map.of("bruto",n("10000")));
        var service=new VentasService(db,mock(CatalogoService.class),mock(InventarioService.class));
        assertEquals(409,assertThrows(ApiError.class,()->service.descuento(ADMIN,VENTA,Map.of("importe_cop","10000","motivo","Rebaja DEMO"))).estado());
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }

    @Test void agregaDemandaDeVariasLineasAntesDePermitirConsumo() {
        Db db=mock(Db.class);CatalogoService cat=mock(CatalogoService.class);InventarioService inv=mock(InventarioService.class);
        when(db.requerido("SELECT * FROM venta WHERE venta_id=? FOR UPDATE",VENTA))
            .thenReturn(fila("vendedora_usuario_id",VENDEDORA.usuarioId(),"estado","BORRADOR"));
        when(db.lista("SELECT * FROM linea_venta WHERE venta_id=? ORDER BY linea_no",VENTA)).thenReturn(List.of(
            Map.of("linea_no",1,"especificacion_id","spec","cantidad_unidades",2,"precio_unitario_cop",n("10000")),
            Map.of("linea_no",2,"especificacion_id","spec","cantidad_unidades",1,"precio_unitario_cop",n("10000"))));
        var original=new Articulo(ABIERTO,"MADRE","Original DEMO",Unidad.ML,"PERFUME_ABIERTO");
        when(cat.especificacion("spec")).thenReturn(new Especificacion("spec","DECANT",null,List.of(new Componente(original,n("5")))));
        when(inv.saldo(ABIERTO)).thenReturn(new CostoPromedio(n("14"),n("140")));
        ApiError error=assertThrows(ApiError.class,()->new VentasService(db,cat,inv).emitir(VENDEDORA,VENTA,Map.of("clave_operacion","venta-demo","credito",false)));
        assertTrue(error.getMessage().contains("Existencias insuficientes"));
        verify(inv).bloquear(List.of(ABIERTO));
        verify(inv,never()).renglon(anyString(),anyInt(),any(),any(),any());
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }
}
