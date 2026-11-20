package co.edu.sena.perfumeria.servicio;

import co.edu.sena.perfumeria.nucleo.ApiError;
import co.edu.sena.perfumeria.nucleo.Db;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static co.edu.sena.perfumeria.servicio.DatosPrueba.*;

class ReportesServiceTest {
    @Test void vendedoraNoObtieneCostosPorCambiarTipoDeReporte() {
        Db db=mock(Db.class);
        var service=new ReportesService(db,mock(InventarioService.class),mock(VentasService.class));
        assertEquals(403,assertThrows(ApiError.class,()->service.reporte(VENDEDORA,"costos","2026-11-17","2026-11-17")).estado());
        verifyNoInteractions(db);
    }

    @Test void diaComercialDeBogotaSeConsultaConLimitesUtcCorrectos() {
        Db db=mock(Db.class);
        var service=new ReportesService(db,mock(InventarioService.class),mock(VentasService.class));
        var result=service.reporte(ADMIN,"medios","2026-11-17","2026-11-17");
        verify(db).lista(contains("c.confirmado_en>=? AND c.confirmado_en<?"),
            eq(Timestamp.from(Instant.parse("2026-11-17T05:00:00Z"))),
            eq(Timestamp.from(Instant.parse("2026-11-18T05:00:00Z"))));
        assertEquals("America/Bogota",result.get("zona"));
    }

    @Test void costoPendienteNoSePresentaComoMargenCalculado() {
        Db db=mock(Db.class);
        var row=fila("venta_id",VENTA,"ingreso_cop",n("38000"),"costo_materiales_cop",n("0"),"costos_pendientes",1,"margen_bruto_cop",n("38000"));
        when(db.lista(startsWith("SELECT v.venta_id,v.numero_recibo,t.total ingreso_cop"),any(Object[].class))).thenReturn(List.of(row));
        when(db.requerido(anyString(),eq(VENTA))).thenReturn(Map.of("n",n("0")));
        new ReportesService(db,mock(InventarioService.class),mock(VentasService.class)).reporte(ADMIN,"costos","2026-11-17","2026-11-17");
        assertNull(row.get("margen_bruto_cop"));assertNull(row.get("margen_bruto_ajustado_cop"));assertNull(row.get("costo_materiales_ajustado_cop"));
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }
}
