package co.edu.sena.perfumeria.servicio;

import co.edu.sena.perfumeria.dominio.*;
import co.edu.sena.perfumeria.nucleo.ApiError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static co.edu.sena.perfumeria.servicio.DatosPrueba.*;

class OperacionVentaServiceTest {
    private Venta venta() {
        var original=new Articulo(ABIERTO,"MADRE","Original DEMO",Unidad.ML,"PERFUME_ABIERTO");
        var spec=new Especificacion("spec","DECANT",null,List.of(new Componente(original,n("5"))));
        return new Venta(VENTA,List.of(new LineaVenta(1,spec,1,n("10000"))),n("0"));
    }

    @Test void contadoInexactoNoIniciaEntregaNiCobro() {
        VentasService ventas=mock(VentasService.class);CajaService caja=mock(CajaService.class);
        when(ventas.dominio(VENTA)).thenReturn(venta());
        assertThrows(ApiError.class,()->new OperacionVentaService(ventas,caja).emitir(VENDEDORA,VENTA,Map.of(
            "clave_operacion","contado-demo","credito",false,"cobro",Map.of("importe_cop","9999"))));
        verify(ventas,never()).emitir(any(),anyString(),anyMap());verifyNoInteractions(caja);
    }

    @Test void contadoExactoCoordinaEntregaYCobroConClaveDerivada() {
        VentasService ventas=mock(VentasService.class);CajaService caja=mock(CajaService.class);
        when(ventas.dominio(VENTA)).thenReturn(venta());
        when(ventas.detalle(VENDEDORA,VENTA)).thenReturn(Map.of("estado","EMITIDA","saldo_cop",n("0")));
        var cobro=Map.<String,Object>of("turno_id",TURNO,"medio","NEQUI","importe_cop","10000","confirmado",true,"referencia_externa","DEMO-NEQUI");
        var datos=Map.<String,Object>of("clave_operacion","contado-demo","credito",false,"cobro",cobro);
        var result=new OperacionVentaService(ventas,caja).emitir(VENDEDORA,VENTA,datos);
        @SuppressWarnings("unchecked") ArgumentCaptor<Map<String,Object>> enviado=ArgumentCaptor.forClass(Map.class);
        var orden=inOrder(ventas,caja);orden.verify(ventas).propia(VENDEDORA,VENTA,true);orden.verify(ventas).dominio(VENTA);
        orden.verify(ventas).emitir(VENDEDORA,VENTA,datos);orden.verify(caja).cobrar(eq(VENDEDORA),eq(VENTA),enviado.capture());
        orden.verify(ventas).detalle(VENDEDORA,VENTA);
        assertTrue(enviado.getValue().get("clave_operacion").toString().startsWith("CONTADO:"));
        assertEquals("DEMO-NEQUI",enviado.getValue().get("referencia_externa"));
        assertFalse(cobro.containsKey("clave_operacion"));
        assertEquals(0,n(result.get("saldo_cop").toString()).signum());
    }

    @Test void creditoNoAdmiteCobroAnidado() {
        VentasService ventas=mock(VentasService.class);CajaService caja=mock(CajaService.class);
        assertThrows(ApiError.class,()->new OperacionVentaService(ventas,caja).emitir(ADMIN,VENTA,Map.of("credito",true,"cobro",Map.of("importe_cop","10000"))));
        verify(ventas,never()).emitir(any(),anyString(),anyMap());verifyNoInteractions(caja);
    }

    @Test void creditoDelegaEntregaSinInventarUnPago() {
        VentasService ventas=mock(VentasService.class);CajaService caja=mock(CajaService.class);
        var datos=Map.<String,Object>of("credito",true,"clave_operacion","fiado-demo");
        when(ventas.emitir(ADMIN,VENTA,datos)).thenReturn(Map.of("estado","EMITIDA","saldo_cop",n("10000")));
        var result=new OperacionVentaService(ventas,caja).emitir(ADMIN,VENTA,datos);
        assertEquals(n("10000"),result.get("saldo_cop"));verifyNoInteractions(caja);
    }
}
