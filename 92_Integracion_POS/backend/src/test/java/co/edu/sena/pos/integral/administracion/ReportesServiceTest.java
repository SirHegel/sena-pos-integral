package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.PruebaBase;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class ReportesServiceTest extends PruebaBase {
    DbDoble db;ReportesService service;
    @BeforeEach void preparar() { db=new DbDoble();service=new ReportesService(db); }
    @Test void cajeroNoObtieneReporteDeNomina() { error(403,()->service.consultar(CAJERO,"nomina","2026-11-03","2026-11-03"));db.sinAcceso(); }
    @Test void fechasInvertidasNoLleganASql() { error(400,()->service.consultar(ADMIN,"ventas","2026-11-04","2026-11-03"));db.sinAcceso(); }
    @Test void reporteDeVentasUsaDiaBogotaFiltroDePropietarioYOrigenDelActor() {
        db.responder=c->c.contiene("FROM tenant")?filas(fila("moneda","COP")):filas(fila("venta_id",VENTA,"fecha",LocalDateTime.parse("2026-11-03T05:00:00"),"numero_factura","A92","usuario_id",U,"medio","EFECTIVO","base",bd("100.00"),"iva",bd("19.00"),"total",bd("119.00"),"pagado",bd("119.00")));
        var r=service.consultar(CAJERO,"ventas","2026-11-03","2026-11-03");var q=db.lecturas.getFirst();assertEquals(T,q.arg(0));assertEquals(Instant.parse("2026-11-03T05:00:00Z"),((Timestamp)q.arg(1)).toInstant());assertEquals(Instant.parse("2026-11-04T05:00:00Z"),((Timestamp)q.arg(2)).toInstant());assertEquals(U,q.arg(3));assertEquals("2026-11-03T00:00-05:00",r.filas().getFirst().get("fecha"));assertEquals(T,r.origen().get("tenantId"));assertEquals(List.of("CAJERO"),r.origen().get("roles"));assertEquals(bd("119.00"),r.totales().get("total"));db.sinEscrituras();
    }
    @Test void inventarioNoSumaCantidadesDeUnidadesDistintas() {
        db.responder=c->c.contiene("FROM tenant")?filas(fila("moneda","COP")):filas(fila("unidad","kg","cantidad_delta",bd("1.000")),fila("unidad","unidad","cantidad_delta",bd("2.000")));
        var r=service.consultar(ADMIN,"inventario","2026-11-03","2026-11-03");assertEquals(Map.of("filas",2),r.totales());assertEquals(3,db.lecturas.getFirst().args().size());
    }
    @Test void limiteDeDiezMilFilasEvitaExportacionParcial() {
        db.responder=c->Collections.nCopies(10001,fila("total",bd("1.00")));error(413,()->service.consultar(ADMIN,"ventas","2026-11-03","2026-11-03"));db.sinEscrituras();
    }
    @ParameterizedTest @ValueSource(strings={"=SUM(A1:A2)"," +1","\t=2","\uFF1D1+2","\u00A0@x"}) void textoPeligrosoEnCsvSeProtegeSinModificarDato(String valor) { assertEquals("\"'"+valor+"\"",ReportesService.celda(valor)); }
    @Test void csvEscapaComasComillasYConservaNegativosNumericos() {
        assertEquals("\"a,\"\"b\"\"\"",ReportesService.celda("a,\"b\""));assertEquals("\"-2.50\"",ReportesService.celda(bd("-2.50")));assertEquals("\"'-2.50\"",ReportesService.celda("-2.50"));
    }
    @Test void csvUtilizaMismasFilasTotalesYMetadatosDelInforme() {
        var origen=Map.<String,Object>of("tenantId",T,"usuarioId",U,"roles",List.of("ADMINISTRADOR"));var dato=fila("nombre","\uFF1D1+2","total",bd("4.50"));
        var r=new ReportesService.Informe(origen,"ventas","2026-11-03","2026-11-03","America/Bogota",List.of("nombre","total"),List.of(dato),Map.of("total",bd("4.50")));
        String csv=service.csv(r);assertTrue(csv.startsWith("\uFEFF\"metadato\",\"valor\""));assertTrue(csv.contains("\"tenantId\",\""+T+"\""));assertTrue(csv.contains("\"'\uFF1D1+2\",\"4.50\""));assertTrue(csv.contains("\"TOTAL (1 filas)\",\"4.50\""));assertEquals("\uFF1D1+2",dato.get("nombre"));
    }
}
