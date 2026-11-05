package co.edu.sena.pos.integral;

import co.edu.sena.pos.integral.catalogo.CatalogoService;
import co.edu.sena.pos.integral.identidad.IdentidadService;
import co.edu.sena.pos.integral.operacion.OperacionService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransaccionesTest extends PruebaBase {
    @Test void falloPosteriorAInsercionDeCatalogoRevierteElRecursoDoble() {
        var db=new DbDoble();var tx=new TxDoble(db);var s=tx.proxy(new CatalogoService(db));
        error(404,()->s.crear(ADMIN,new CatalogoService.Producto("P92","Prueba",bd("1.00"),bd("0.0000"),"VENDIBLE","unidad","DIRECTO",true)));
        assertEquals(1,db.escrituras.size());assertTrue(db.confirmadas.isEmpty());assertTrue(db.pendientes.isEmpty());assertEquals(1,tx.rollbacks);assertEquals(0,tx.commits);
    }
    @Test void altaCorrectaConfirmaLaTransaccionDelRecursoDoble() {
        var db=new DbDoble();db.responder=c->filas(fila("producto_id",c.arg(1)));var tx=new TxDoble(db);var s=tx.proxy(new CatalogoService(db));s.crear(ADMIN,new CatalogoService.Producto("P92","Prueba",bd("1.00"),bd("0.0000"),"VENDIBLE","unidad","DIRECTO",true));assertEquals(1,tx.commits);assertEquals(1,db.confirmadas.size());assertEquals(0,tx.rollbacks);
    }
    @Test void errorDeAutenticacionConservaIntentoFallidoPorNoRollbackFor() {
        var db=new DbDoble();var cod=new CodificadorDoble();db.responder=c->filas(fila("usuario_id",U,"hash_password",cod.encode("ClaveCorrecta92"),"activo",true,"negocio_activo",true,"intentos_fallidos",0,"bloqueado_hasta",null));var tx=new TxDoble(db);var s=tx.proxy(new IdentidadService(db,cod,Clock.fixed(Instant.parse("2026-11-03T15:00:00Z"),ZoneOffset.UTC)));
        error(401,()->s.ingresar(new IdentidadService.Ingreso(T,"a@demo.example","OtraClave92")));assertEquals(1,tx.commits);assertEquals(0,tx.rollbacks);assertEquals(1,db.confirmadas.size());assertEquals(1,db.confirmadas.getFirst().arg(0));
    }
    @Test void ajusteNegativoRevierteInclusoElAltaAuxiliarDeExistencia() {
        var db=new DbDoble();db.responder=c->{if(c.contiene("FROM movimiento_inventario"))return List.of();return filas(c.contiene("FROM producto")?fila("activo",true,"modo_descuento","DIRECTO"):c.contiene("FROM existencia")?fila("stock",bd("3.000")):fila("bodega_id",B));};
        var tx=new TxDoble(db);var s=tx.proxy(new OperacionService(db));error(409,()->s.mover(ADMIN,Map.of("bodegaId",B,"productoId",P,"cantidadDelta",bd("-5.000"),"tipo","AJUSTE","documentoOrigen","AJ92")));assertEquals(1,db.escritas("INSERT INTO existencia"));assertEquals(0,db.escritas("UPDATE existencia"));assertEquals(1,tx.rollbacks);assertTrue(db.confirmadas.isEmpty());
    }
}
