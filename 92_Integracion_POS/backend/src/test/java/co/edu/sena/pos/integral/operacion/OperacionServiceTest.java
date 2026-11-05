package co.edu.sena.pos.integral.operacion;

import co.edu.sena.pos.integral.PruebaBase;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class OperacionServiceTest extends PruebaBase {
    DbDoble db;OperacionService service;
    @BeforeEach void preparar() { db=new DbDoble();service=new OperacionService(db); }
    Map<String,Object> sesion() { return fila("sesion_id",S,"usuario_id",U,"fondo_inicial",bd("100.00"),"cerrada_en",null); }
    Map<String,Object> venta() { return fila("venta_id",VENTA,"usuario_id",U,"sesion_caja_id",S,"bodega_id",B,"estado","BORRADOR","mesa_id",null); }
    @Test void cajaSumaSoloEfectivoAlFondoYSeparaExterno() {
        db.responder=c->filas(c.contiene("FROM sesion_caja")?sesion():fila("efectivo",bd("30.00"),"externo",bd("70.00")));
        var r=service.resumenCaja(CAJERO,S);assertEquals(bd("130.00"),r.get("esperado_calculado"));assertEquals(bd("70.00"),r.get("externo"));db.sinEscrituras();
    }
    @Test void cajeroNoConsultaCajaDeOtroUsuario() {
        var s=sesion();s.put("usuario_id",OTRO);db.responder=c->filas(s);error(403,()->service.resumenCaja(CAJERO,S));assertEquals(1,db.lecturas.size());db.sinEscrituras();
    }
    @Test void entradaNegativaEsRechazadaSinConsultarPersistencia() {
        error(400,()->service.mover(ADMIN,Map.of("bodegaId",B,"productoId",P,"cantidadDelta",bd("-1.000"),"tipo","ENTRADA","documentoOrigen","E92")));db.sinAcceso();
    }
    @Test void repetirMovimientoConMismaCantidadNoRepiteEscrituras() {
        var previo=fila("movimiento_id",id(10),"cantidad_delta",bd("2.000"));
        db.responder=c->filas(c.contiene("FROM producto")?fila("activo",true,"modo_descuento","DIRECTO"):c.contiene("FROM movimiento_inventario")?previo:fila("bodega_id",B));
        assertEquals(previo,service.mover(ADMIN,Map.of("bodegaId",B,"productoId",P,"cantidadDelta",bd("2.000"),"tipo","ENTRADA","documentoOrigen","E92")));db.sinEscrituras();
    }
    @Test void documentoDeMovimientoNoPuedeCambiarDeCantidad() {
        db.responder=c->filas(c.contiene("FROM producto")?fila("activo",1,"modo_descuento","DIRECTO"):c.contiene("FROM movimiento_inventario")?fila("cantidad_delta",bd("1.000")):fila("bodega_id",B));
        error(409,()->service.mover(ADMIN,Map.of("bodegaId",B,"productoId",P,"cantidadDelta",bd("2.000"),"tipo","ENTRADA","documentoOrigen","E92")));db.sinEscrituras();
    }
    @Test void mesaConVentaAbiertaNoCambiaSuEstado() {
        db.responder=c->filas(fila("mesa_id",id(12)));error(409,()->service.estadoMesa(ADMIN,id(12),Map.of("estado","INACTIVA")));db.sinEscrituras();
    }
    @Test void recetaRechazaComponentesDuplicadosAntesDeEscribir() {
        var componente=Map.<String,Object>of("productoInsumoId",P,"cantidad",bd("1.000"));
        error(400,()->service.crearReceta(ADMIN,Map.of("productoPlatoId",id(13),"version",1,"rendimiento",bd("1.000"),"componentes",List.of(componente,componente))));db.sinAcceso();
    }
    @Test void clienteNoPuedeInyectarTenantAlCrearVenta() {
        error(400,()->service.crearVenta(ADMIN,Map.of("tenantId",OTRO,"bodegaId",B,"sesionCajaId",S,"lineas",List.of())));db.sinAcceso();
    }
    @Test void comprobanteRedondeaIvaPorLineaAntesDeSumar() {
        var line=fila("cantidad",bd("1.000"),"precio_unitario",bd("0.05"),"tasa_iva",bd("0.1000"));
        db.responder=c->c.contiene("FROM venta")?filas(venta()):c.contiene("JOIN producto")?filas(line,line):List.of();
        var r=service.comprobante(CAJERO,VENTA);assertEquals(bd("0.10"),r.get("base"));assertEquals(bd("0.02"),r.get("iva"));assertEquals(bd("0.12"),r.get("total"));db.sinEscrituras();
    }
    @Test void cobroRechazaImporteDistintoAntesDeStockYPago() {
        db.responder=c->{if(c.contiene("FROM operacion_confirmacion"))return List.of();if(c.contiene("FROM venta"))return filas(venta());if(c.contiene("FROM sesion_caja"))return filas(sesion());if(c.contiene("FROM detalle_venta"))return filas(fila("cantidad",bd("1.000"),"precio_unitario",bd("100.00"),"tasa_iva",bd("0.1900")));throw new AssertionError(c.sql());};
        error(409,()->service.confirmar(CAJERO,VENTA,"pago92",Map.of("medio","EFECTIVO","referencia","R92","importe",bd("118.00"))));db.sinEscrituras();
    }
    void recetasCompartidas(String stock) {
        String plato1=id(20),plato2=id(21),r1=id(22),r2=id(23);
        db.responder=c->{
            if(c.contiene("FROM venta"))return filas(venta());
            if(c.contiene("FROM sesion_caja"))return filas(sesion());
            if(c.contiene("FROM producto")) {
                boolean ins=P.equals(c.arg(1));return filas(fila("producto_id",c.arg(1),"activo",true,"tipo",ins?"INSUMO":"PLATO","modo_descuento",ins?"DIRECTO":"RECETA","precio_vigente",bd("10.00"),"tasa_iva",bd("0.0000")));
            }
            if(c.contiene("FROM receta"))return filas(fila("receta_id",plato1.equals(c.arg(1))?r1:r2,"rendimiento",bd("1.000")));
            if(c.contiene("FROM detalle_receta"))return filas(fila("linea_no",1,"producto_insumo_id",P,"cantidad",bd("6.000")));
            if(c.contiene("FROM existencia"))return filas(fila("stock",bd(stock)));
            if(c.contiene("FROM detalle_venta"))return filas(fila("cantidad",bd("1.000"),"precio_unitario",bd("10.00"),"tasa_iva",bd("0.0000")));
            if(c.contiene("FROM pago"))return List.of();
            throw new AssertionError(c.sql());
        };
    }
    Map<String,Object> detalleCompartido() { return Map.of("lineas",List.of(Map.of("productoId",id(20),"cantidad",1),Map.of("productoId",id(21),"cantidad",1))); }
    @Test void dosRecetasNoPuedenReutilizarElMismoStockParaAgregarDetalle() {
        recetasCompartidas("10.000");error(409,()->service.editarDetalle(CAJERO,VENTA,detalleCompartido()));db.sinEscrituras();
    }
    @Test void demandaConjuntaExactaPermiteDetalleSinReservarStock() {
        recetasCompartidas("12.000");service.editarDetalle(CAJERO,VENTA,detalleCompartido());assertEquals(1,db.escritas("DELETE FROM detalle_venta"));assertEquals(2,db.escritas("INSERT INTO detalle_venta"));assertEquals(0,db.escritas("UPDATE existencia"));
    }
}
