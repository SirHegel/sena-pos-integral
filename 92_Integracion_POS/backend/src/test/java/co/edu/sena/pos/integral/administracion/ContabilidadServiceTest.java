package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.PruebaBase;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ContabilidadServiceTest extends PruebaBase {
    final String EVENTO=id(40),DE=id(41),CR=id(42),AUX=id(43);
    DbDoble db;ContabilidadService service;
    @BeforeEach void preparar() { db=new DbDoble();service=new ContabilidadService(db); }
    void venta(String total,String pago) {
        db.responder=c->{
            if(c.contiene("FROM tenant"))return filas(fila("tenant_id",T));
            if(c.contiene("FROM evento_integracion"))return filas(fila("tipo","VENTA_CONFIRMADA","venta_id",VENTA,"estado","PENDIENTE"));
            if(c.contiene("FROM venta"))return filas(fila("venta_id",VENTA,"estado","PAGADA","numero_factura","ACA92"));
            if(c.contiene("FROM v_total_venta"))return filas(fila("base",bd("100.00"),"iva",bd("19.00"),"total",bd(total)));
            if(c.contiene("FROM pago"))return filas(fila("importe",bd(pago)));
            if(c.contiene("FROM cuenta"))return filas(fila("naturaleza",DE.equals(c.arg(1))?"DEBITO":"CREDITO"));
            if(c.contiene("FROM asiento_contable"))return c.contiene("AND asiento_id")?filas(fila("asiento_id",c.arg(1))):List.of();
            if(c.contiene("FROM linea_asiento"))return List.of();
            throw new AssertionError(c.sql());
        };
    }
    @Test void rolDeNominaNoConsultaContabilidad() { error(403,()->service.cuentas(new co.edu.sena.pos.integral.nucleo.Actor(T,U,Set.of("NOMINA"))));db.sinAcceso(); }
    @Test void ventaConIvaGeneraTresLineasYEquilibrioExacto() {
        venta("119.00","119.00");service.procesar(ADMIN,EVENTO,new ContabilidadService.Contabilizacion(DE,CR,AUX));var lineas=db.escrituras.stream().filter(c->c.contiene("INSERT INTO linea_asiento")).toList();assertEquals(3,lineas.size());
        var debito=lineas.stream().map(c->(java.math.BigDecimal)c.arg(4)).reduce(bd("0.00"),java.math.BigDecimal::add);var credito=lineas.stream().map(c->(java.math.BigDecimal)c.arg(5)).reduce(bd("0.00"),java.math.BigDecimal::add);assertEquals(bd("119.00"),debito);assertEquals(debito,credito);assertEquals(1,db.escritas("UPDATE evento_integracion"));
    }
    @Test void pagoDistintoAlTotalNoProduceAsiento() { venta("119.00","118.00");error(409,()->service.procesar(ADMIN,EVENTO,new ContabilidadService.Contabilizacion(DE,CR,AUX)));db.sinEscrituras(); }
    @Test void ivaPositivoRequiereCuentaAuxiliar() { venta("119.00","119.00");error(400,()->service.procesar(ADMIN,EVENTO,new ContabilidadService.Contabilizacion(DE,CR,null)));db.sinEscrituras(); }
    @Test void importeDeOrigenDesequilibradoSeRechazaAntesDelAsiento() { venta("120.00","120.00");error(409,()->service.procesar(ADMIN,EVENTO,new ContabilidadService.Contabilizacion(DE,CR,AUX)));db.sinEscrituras(); }
    @Test void mismaCuentaNoPuedeRecibirAmbosLados() { venta("119.00","119.00");error(400,()->service.procesar(ADMIN,EVENTO,new ContabilidadService.Contabilizacion(DE,DE,AUX)));db.sinEscrituras(); }
    @Test void origenYaAsentadoNoSeDuplica() {
        venta("119.00","119.00");var original=db.responder;db.responder=c->c.contiene("FROM asiento_contable")?filas(fila("asiento_id",id(44))):original.apply(c);error(409,()->service.procesar(ADMIN,EVENTO,new ContabilidadService.Contabilizacion(DE,CR,AUX)));db.sinEscrituras();
    }
}
