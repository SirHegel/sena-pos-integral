package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.nucleo.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

final class CReglas {
    static final BigDecimal CERO = new BigDecimal("0.00");
    private CReglas() {}
    static ApiError conflicto(String mensaje) { return new ApiError(409, "ESTADO_INCOMPATIBLE", mensaje); }
    static String id() { return UUID.randomUUID().toString(); }
    static void bloquear(Db db, Actor actor) {
        db.requerido("SELECT tenant_id FROM tenant WHERE tenant_id=? FOR UPDATE", actor.tenantId());
    }
    static BigDecimal dinero(Object n) {
        BigDecimal b = n instanceof BigDecimal d ? d : new BigDecimal(n.toString());
        b = b.setScale(2, RoundingMode.UNNECESSARY);
        if (b.precision() > 14) throw conflicto("El importe acumulado excede DECIMAL(14,2)");
        return b;
    }
    static List<String> ids(List<String> entrada) {
        if (entrada == null || entrada.isEmpty() || entrada.size() > 1000)
            throw V.error("novedadesIds", "entre 1 y 1000 identificadores");
        var ids = new TreeSet<String>();
        for (String s : entrada) if (!ids.add(V.uuid(s, "novedadesIds")))
            throw V.error("novedadesIds", "no se permiten duplicados");
        return List.copyOf(ids);
    }
    static String marcas(int n) { return String.join(",", Collections.nCopies(n, "?")); }
    static Object[] args(String tenant, List<String> ids) {
        var args = new ArrayList<Object>(); args.add(tenant); args.addAll(ids); return args.toArray();
    }
    static <T> List<T> limite(List<T> filas) {
        if (filas.size() > 1000) throw new ApiError(413, "CONSULTA_AMPLIA", "La consulta excede 1000 filas; limite el período");
        return filas;
    }
}
