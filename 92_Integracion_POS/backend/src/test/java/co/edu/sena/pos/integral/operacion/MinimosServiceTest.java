package co.edu.sena.pos.integral.operacion;

import co.edu.sena.pos.integral.PruebaBase;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class MinimosServiceTest extends PruebaBase {
    @ParameterizedTest @CsvSource({"0.000,0.000,false,true","1.000,0.000,false,false","2.000,3.000,true,false","3.000,3.000,true,false","4.000,3.000,false,false","0.000,3.000,true,true"})
    void alertaYAgotadoRespetanUmbralInclusivoYDesactivacion(String stock,String minimo,boolean alerta,boolean agotado) {
        var entrada=fila("stock",bd(stock),"minimo",bd(minimo));var salida=MinimosService.presentar(entrada);
        assertEquals(alerta,salida.get("alerta"));assertEquals(agotado,salida.get("agotado"));assertFalse(entrada.containsKey("alerta"));
    }
    @Test void cajeroNoConfiguraUmbralNiConsultaLaBase() { var db=new DbDoble();error(403,()->new MinimosService(db).configurar(CAJERO,Map.of("bodegaId",B,"productoId",P,"minimo",1)));db.sinAcceso(); }
    @Test void tenantEntranteEsRechazado() { var db=new DbDoble();error(400,()->new MinimosService(db).configurar(ADMIN,Map.of("tenantId",OTRO,"bodegaId",B,"productoId",P,"minimo",1)));db.sinAcceso(); }
    @ParameterizedTest @CsvSource({"-0.001","0.0001","100000000000.000"}) void umbralInvalidoNoAccedeALaBase(String minimo) {
        var db=new DbDoble();error(400,()->new MinimosService(db).configurar(ADMIN,Map.of("bodegaId",B,"productoId",P,"minimo",minimo)));db.sinAcceso();
    }
    @Test void recursoDeOtroNegocioNoSeActualiza() {
        var db=new DbDoble();error(404,()->new MinimosService(db).configurar(ADMIN,Map.of("bodegaId",B,"productoId",P,"minimo",1)));db.sinEscrituras();assertEquals(T,db.lecturas.getFirst().arg(0));
    }
    @Test void productoPorRecetaNoConfiguraInventarioFisico() {
        var db=new DbDoble();db.responder=c->filas(c.contiene("FROM producto")?fila("activo",true,"modo_descuento","RECETA"):fila("bodega_id",B));error(409,()->new MinimosService(db).configurar(ADMIN,Map.of("bodegaId",B,"productoId",P,"minimo",1)));db.sinEscrituras();
    }
    @Test void configurarConservaSaldoYUsaTenantDelActor() {
        var db=new DbDoble();db.responder=c->filas(c.contiene("FROM producto")?fila("activo",1,"modo_descuento","DIRECTO"):c.contiene("FROM existencia")?fila("stock",bd("7.000"),"minimo",bd("8.000")):fila("bodega_id",B));
        var r=new MinimosService(db).configurar(ADMIN,Map.of("bodegaId",B,"productoId",P,"minimo","8.000"));assertEquals(bd("7.000"),r.get("stock"));assertEquals(true,r.get("alerta"));var c=db.escrituras.getFirst();assertEquals(T,c.arg(0));assertTrue(c.sql().endsWith("UPDATE minimo=?"));assertFalse(c.sql().contains("UPDATE stock"));
    }
    @Test void getExistenciasEnriqueceEnServidorYFiltraPorNegocio() {
        var db=new DbDoble();db.responder=c->filas(fila("stock",bd("2.000"),"minimo",bd("3.000")));var r=new OperacionService(db).existencias(CAJERO,B);assertEquals(true,r.getFirst().get("alerta"));assertEquals(false,r.getFirst().get("agotado"));assertEquals(List.of(T,B,B),db.lecturas.getFirst().args());db.sinEscrituras();
    }
}
