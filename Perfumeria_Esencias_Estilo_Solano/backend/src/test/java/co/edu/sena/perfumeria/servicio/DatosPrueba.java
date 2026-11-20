package co.edu.sena.perfumeria.servicio;

import co.edu.sena.perfumeria.nucleo.Actor;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.mockito.ArgumentMatchers.argThat;

/** Datos exclusivamente didácticos; las pruebas no abren una conexión a MySQL. */
final class DatosPrueba {
    static final String VENTA = "10000000-0000-4000-8000-000000000001";
    static final String TURNO = "10000000-0000-4000-8000-000000000002";
    static final String ARTICULO = "10000000-0000-4000-8000-000000000003";
    static final String ABIERTO = "10000000-0000-4000-8000-000000000004";
    static final String ETIQUETA = "10000000-0000-4000-8000-000000000005";
    static final String OFERTA = "10000000-0000-4000-8000-000000000006";
    static final String FORMULA = "10000000-0000-4000-8000-000000000007";
    static final String FRAGANCIA = "10000000-0000-4000-8000-000000000008";
    static final Actor ADMIN = new Actor("admin-demo", "Administrador DEMO", "ADMINISTRADOR");
    static final Actor VENDEDORA = new Actor("vendedora-demo", "Vendedora DEMO", "VENDEDORA");

    static BigDecimal n(String value) { return new BigDecimal(value); }
    static BigDecimal decimalIgual(String value) {
        return argThat((BigDecimal actual) -> actual != null && actual.compareTo(n(value)) == 0);
    }
    static Map<String,Object> fila(Object... pares) {
        var result = new LinkedHashMap<String,Object>();
        for (int i=0; i<pares.length; i+=2) result.put((String)pares[i], pares[i+1]);
        return result;
    }
    private DatosPrueba() {}
}
