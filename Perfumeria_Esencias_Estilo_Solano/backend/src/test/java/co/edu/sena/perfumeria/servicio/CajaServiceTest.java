package co.edu.sena.perfumeria.servicio;

import co.edu.sena.perfumeria.nucleo.ApiError;
import co.edu.sena.perfumeria.nucleo.Db;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static co.edu.sena.perfumeria.servicio.DatosPrueba.*;

class CajaServiceTest {
    private void turnoAbierto(Db db) {
        when(db.requerido("SELECT * FROM turno_caja WHERE turno_id=? FOR UPDATE",TURNO))
            .thenReturn(fila("responsable_usuario_id",VENDEDORA.usuarioId(),"cerrada_en",null));
        when(db.requerido("SELECT nombre_acceso FROM usuario WHERE usuario_id=?",VENDEDORA.usuarioId()))
            .thenReturn(Map.of("nombre_acceso","vendedora.demo"));
    }

    @Test void abonoMayorAlSaldoNoRegistraCobro() {
        Db db=mock(Db.class);InventarioService inv=mock(InventarioService.class);VentasService ventas=mock(VentasService.class);
        turnoAbierto(db);
        when(ventas.propia(VENDEDORA,VENTA,true)).thenReturn(Map.of("estado","EMITIDA"));
        when(db.requerido("SELECT saldo FROM v_total_venta WHERE venta_id=?",VENTA)).thenReturn(Map.of("saldo",n("5000")));
        ApiError error=assertThrows(ApiError.class,()->new CajaService(db,inv,ventas).cobrar(VENDEDORA,VENTA,Map.of(
            "clave_operacion","abono-demo","turno_id",TURNO,"medio","EFECTIVO","importe_cop","5001","confirmado",true)));
        assertTrue(error.getMessage().contains("supera el saldo"));
        verify(db,never()).cambiar(anyString(),any(Object[].class));
        verify(inv,never()).renglon(anyString(),anyInt(),any(),any(),any());
    }

    @ParameterizedTest @ValueSource(strings={"NEQUI","DAVIPLATA","TARJETA"})
    void medioElectronicoSinReferenciaNoSeConfirma(String medio) {
        Db db=mock(Db.class);InventarioService inv=mock(InventarioService.class);VentasService ventas=mock(VentasService.class);
        turnoAbierto(db);
        when(ventas.propia(VENDEDORA,VENTA,true)).thenReturn(Map.of("estado","EMITIDA"));
        ApiError error=assertThrows(ApiError.class,()->new CajaService(db,inv,ventas).cobrar(VENDEDORA,VENTA,Map.of(
            "clave_operacion","pago-demo","turno_id",TURNO,"medio",medio,"importe_cop","5000","confirmado",true)));
        assertTrue(error.getMessage().contains("referencia verificable"));
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }

    @Test void reembolsoRespetaCobradoMenosDevolucionesPrevias() {
        Db db=mock(Db.class);InventarioService inv=mock(InventarioService.class);VentasService ventas=mock(VentasService.class);
        when(ventas.propia(ADMIN,VENTA,true)).thenReturn(Map.of("estado","EMITIDA"));
        when(db.requerido("SELECT cobrado FROM v_total_venta WHERE venta_id=?",VENTA)).thenReturn(Map.of("cobrado",n("10000")));
        when(db.requerido("SELECT COALESCE(SUM(mc.importe_cop),0)n FROM movimiento_caja mc JOIN incidencia i ON i.incidencia_id=mc.incidencia_id WHERE i.venta_id=?",VENTA)).thenReturn(Map.of("n",n("3000")));
        ApiError error=assertThrows(ApiError.class,()->new CajaService(db,inv,ventas).incidencia(ADMIN,Map.of(
            "clave_operacion","devolucion-demo","tipo","DEVOLUCION","venta_id",VENTA,"linea_no",1,
            "motivo","Reembolso DEMO","devolucion_cop","7001","turno_id",TURNO)));
        assertTrue(error.getMessage().contains("aún no se devolvió"));
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }

    @Test void mermaNoPuedeAumentarExistencias() {
        Db db=mock(Db.class);InventarioService inv=mock(InventarioService.class);
        ApiError error=assertThrows(ApiError.class,()->new CajaService(db,inv,mock(VentasService.class)).incidencia(ADMIN,Map.of(
            "clave_operacion","merma-demo","tipo","MERMA","motivo","Pérdida DEMO",
            "lineas",List.of(Map.of("articulo_id",ARTICULO,"cantidad_base","1","direccion","ENTRADA")))));
        assertTrue(error.getMessage().contains("sólo permiten salidas"));
        verify(db,never()).cambiar(anyString(),any(Object[].class));verify(inv,never()).saldo(anyString());
    }

    @Test void detalleConservaDiferenciaRealSinAlterarCobros() {
        Db db=mock(Db.class);
        when(db.requerido("SELECT * FROM turno_caja WHERE turno_id=?",TURNO))
            .thenReturn(fila("responsable_usuario_id",VENDEDORA.usuarioId(),"fondo_inicial_cop",n("10000")));
        when(db.requerido("SELECT nombre_acceso FROM usuario WHERE usuario_id=?",VENDEDORA.usuarioId())).thenReturn(Map.of("nombre_acceso","vendedora.demo"));
        for(String medio:CajaService.MEDIOS)when(db.requerido("SELECT COALESCE(SUM(importe_cop),0)n FROM cobro WHERE turno_id=? AND medio=?",TURNO,medio))
            .thenReturn(Map.of("n",n(medio.equals("EFECTIVO")?"38000":medio.equals("NEQUI")?"5000":"0")));
        when(db.lista("SELECT * FROM movimiento_caja WHERE turno_id=? ORDER BY registrado_en",TURNO)).thenReturn(List.of(
            Map.of("importe_cop",n("2000"),"sentido","SALIDA"),Map.of("importe_cop",n("1000"),"sentido","ENTRADA")));
        var contado=fila("medio","EFECTIVO","importe_verificado_cop",n("47001"));
        var nequi=fila("medio","NEQUI","importe_verificado_cop",n("5000"));
        when(db.lista("SELECT * FROM conciliacion_turno WHERE turno_id=?",TURNO)).thenReturn(List.of(contado,nequi));
        var result=new CajaService(db,mock(InventarioService.class),mock(VentasService.class)).detalle(VENDEDORA,TURNO);
        assertEquals(0,n(result.get("esperado_efectivo_cop").toString()).compareTo(n("47000")));
        assertEquals(0,n(contado.get("diferencia_cop").toString()).compareTo(n("1")));
        assertEquals(0,n(nequi.get("diferencia_cop").toString()).signum());
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }
}
