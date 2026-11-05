package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.PruebaBase;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class NominaServiceTest extends PruebaBase {
    DbDoble db;NominaService service;
    final String LIQ=id(30),EMP=id(31),N1=id(32),N2=id(33);
    @BeforeEach void preparar() { db=new DbDoble();service=new NominaService(db); }
    Map<String,Object> cabecera(String estado) { return fila("liquidacion_id",LIQ,"periodo","2026-11","estado",estado,"version",1); }
    Map<String,Object> novedad(String id,String tipo,String importe) { return fila("novedad_id",id,"empleado_id",EMP,"periodo","2026-11","estado","AUTORIZADA","empleado_estado","ACTIVO","tipo",tipo,"importe",bd(importe)); }
    void simulacion(String devengo,String deduccion) { db.responder=c->c.contiene("FROM liquidacion_nomina")?filas(cabecera("BORRADOR")):filas(novedad(N1,"DEVENGO",devengo),novedad(N2,"DEDUCCION",deduccion)); }
    @Test void rolAjenoANominaNoConsultaDatos() { error(403,()->service.empleados(CAJERO));db.sinAcceso(); }
    @Test void simulacionCalculaNetoExactoSinEscrituras() {
        simulacion("100.10","30.05");var r=(Map<String,Object>)service.simulacion(ADMIN,LIQ);var total=(Map<String,Object>)r.get("totales");assertEquals(bd("70.05"),total.get("neto"));assertEquals(bd("100.10"),total.get("devengos"));db.sinEscrituras();
    }
    @Test void deduccionNoPuedeSuperarDevengoDeUnEmpleado() { simulacion("10.00","10.01");error(409,()->service.simulacion(ADMIN,LIQ));db.sinEscrituras(); }
    @Test void empleadoInactivoImpideLiquidarSuNovedad() {
        var n=novedad(N1,"DEVENGO","10.00");n.put("empleado_estado","INACTIVO");db.responder=c->c.contiene("FROM liquidacion_nomina")?filas(cabecera("BORRADOR")):filas(n);error(409,()->service.simulacion(ADMIN,LIQ));db.sinEscrituras();
    }
    @Test void seleccionDuplicadaFallaAntesDeBloquearOEscribir() { error(400,()->service.aprobar(ADMIN,LIQ,new NominaService.Aprobacion(List.of(N1,N1))));db.sinAcceso(); }
    void seleccion(boolean consumida) {
        db.responder=c->{
            if(c.contiene("FROM tenant"))return filas(fila("tenant_id",T));
            if(c.contiene("FROM liquidacion_nomina"))return filas(cabecera(db.escritas("UPDATE liquidacion_nomina")>0?"APROBADA":"BORRADOR"));
            if(c.contiene("FROM novedad_nomina"))return filas(novedad(N1,"DEVENGO","100.00"),novedad(N2,"DEDUCCION","20.00"));
            if(c.contiene("detalle_nomina_novedad"))return consumida||!c.contiene(" IN (")?filas(fila("novedad_id",N1),fila("novedad_id",N2)):List.of();
            if(c.contiene("FROM detalle_nomina"))return List.of();
            throw new AssertionError(c.sql());
        };
    }
    @Test void novedadYaConsumidaNoSeAplicaDosVeces() { seleccion(true);error(409,()->service.aprobar(ADMIN,LIQ,new NominaService.Aprobacion(List.of(N1,N2))));db.sinEscrituras(); }
    @Test void aprobarGeneraDetalleConsumosYEventoEnLaMismaOperacion() {
        seleccion(false);service.aprobar(ADMIN,LIQ,new NominaService.Aprobacion(List.of(N2,N1)));assertEquals(1,db.escritas("INSERT INTO detalle_nomina("));assertEquals(2,db.escritas("INSERT INTO detalle_nomina_novedad"));assertEquals(1,db.escritas("INSERT INTO evento_integracion"));
        var detalle=db.escrituras.stream().filter(c->c.contiene("INSERT INTO detalle_nomina(")).findFirst().orElseThrow();assertEquals(List.of(T,LIQ,EMP,bd("100.00"),bd("20.00")),detalle.args());
    }
    @Test void liquidacionAprobadaNoAceptaOtraSeleccion() {
        db.responder=c->c.contiene("FROM tenant")?filas(fila("tenant_id",T)):c.contiene("FROM liquidacion_nomina")?filas(cabecera("APROBADA")):filas(fila("novedad_id",N1));error(409,()->service.aprobar(ADMIN,LIQ,new NominaService.Aprobacion(List.of(N2))));db.sinEscrituras();
    }
    @Test void seleccionQueNoPerteneceAlNegocioNoProduceLiquidacionParcial() {
        db.responder=c->c.contiene("FROM tenant")?filas(fila("tenant_id",T)):c.contiene("FROM liquidacion_nomina")?filas(cabecera("BORRADOR")):List.of();error(404,()->service.aprobar(ADMIN,LIQ,new NominaService.Aprobacion(List.of(N1))));db.sinEscrituras();assertTrue(db.lecturas.stream().allMatch(c->T.equals(c.arg(0))));
    }
}
