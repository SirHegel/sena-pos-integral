package co.edu.sena.pos.catalogo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.UUID;

/** Demostración reproducible con datos sintéticos, sin interacción con datos personales. */
public class CliCatalogo {
    private static int passed;

    public static void main(String[] args) throws Exception {
        ProductoDao dao = new ProductoDao(new ConnectionFactory());
        if (args.length == 2 && args[0].equals("--listar")) {
            for (Producto p : dao.list(UUID.fromString(args[1]))) {
                System.out.println(p.codigo() + " | " + p.nombre() + " | " + p.precio().toPlainString());
            }
            return;
        }
        if (args.length != 1 || !args[0].equals("--demo")) {
            System.out.println("Uso: --demo | --listar UUID_DEL_NEGOCIO");
            return;
        }
        UUID a = UUID.fromString("11111111-1111-4111-8111-111111111111");
        UUID b = UUID.fromString("22222222-2222-4222-8222-222222222222");
        String code = "JDBC-" + UUID.randomUUID().toString().substring(0, 8);
        Producto initial = product(a, UUID.randomUUID(), code, "Café d'ejemplo", "1200.50", "0.1900");
        Producto other = product(b, UUID.randomUUID(), code, "Otro negocio sintético", "2500", "0");
        try {
            dao.create(initial);
            check("C: insertar por JDBC", dao.find(a, initial.productoId()).isPresent());
            Producto read = dao.find(a, initial.productoId()).orElseThrow();
            check("R: nombre con apóstrofo preservado", read.nombre().equals("Café d'ejemplo"));
            check("R: precio decimal exacto", read.precio().compareTo(new BigDecimal("1200.50")) == 0);
            check("R: listar sólo negocio elegido", dao.list(a).stream().anyMatch(p -> p.productoId().equals(initial.productoId())));
            check("R: clave de otro negocio no visible", dao.find(b, initial.productoId()).isEmpty());
            dao.create(other);
            check("Código repetible entre negocios", dao.find(b, other.productoId()).isPresent());
            boolean duplicate = false;
            try { dao.create(product(a, UUID.randomUUID(), code, "Duplicado", "1", "0")); }
            catch (SQLException ex) { duplicate = ex.getErrorCode() == 1062; }
            check("Duplicado en mismo negocio rechazado por UNIQUE", duplicate);
            Producto change = product(a, initial.productoId(), code, "Café actualizado", "1450.75", "0.19");
            check("U: actualizar existente", dao.update(change));
            check("U: valor persistido", dao.find(a, initial.productoId()).orElseThrow().precio().compareTo(new BigDecimal("1450.75")) == 0);
            check("U: otro negocio no actualiza la clave", !dao.update(product(b, initial.productoId(), code + "X", "Ajeno", "2", "0")));
            check("D: otro negocio no elimina la clave", !dao.delete(b, initial.productoId()));
            invalid("Rechazar precio negativo", () -> product(a, UUID.randomUUID(), code, "X", "-1", "0"));
            invalid("Rechazar tres decimales monetarios", () -> product(a, UUID.randomUUID(), code, "X", "1.234", "0"));
            invalid("Rechazar desborde DECIMAL14,2", () -> product(a, UUID.randomUUID(), code, "X", "1000000000000", "0"));
            invalid("Rechazar IVA superior a 1", () -> product(a, UUID.randomUUID(), code, "X", "1", "1.01"));
            invalid("Rechazar precio cero", () -> product(a, UUID.randomUUID(), code, "X", "0E+99", "0"));
            check("Aceptar límite monetario", product(a, UUID.randomUUID(), code, "X", "999999999999.99", "1").precio().compareTo(new BigDecimal("999999999999.99")) == 0);
            invalid("Rechazar tipo ausente", () -> new Producto(a, UUID.randomUUID(), code, "X", BigDecimal.ONE, BigDecimal.ZERO, null, "kg", "DIRECTO", true));
            invalid("Rechazar modo ausente", () -> new Producto(a, UUID.randomUUID(), code, "X", BigDecimal.ONE, BigDecimal.ZERO, "INSUMO", "kg", null, true));
            invalid("Rechazar texto vacío", () -> product(a, UUID.randomUUID(), code, " ", "1", "0"));
            invalid("Rechazar nombre demasiado largo", () -> product(a, UUID.randomUUID(), code, "x".repeat(129), "1", "0"));
            invalid("Rechazar controles", () -> product(a, UUID.randomUUID(), code, "X\n", "1", "0"));
            invalid("Rechazar RECETA para INSUMO", () -> new Producto(a, UUID.randomUUID(), code, "X", BigDecimal.ONE, BigDecimal.ZERO, "INSUMO", "kg", "RECETA", true));
            check("D: eliminar producto sin referencias", dao.delete(a, initial.productoId()));
            check("D: eliminación persistida", dao.find(a, initial.productoId()).isEmpty());
            check("D: eliminar inexistente devuelve false", !dao.delete(a, initial.productoId()));
        } finally {
            // Se eliminan únicamente los dos productos sintéticos identificados por esta ejecución.
            dao.delete(a, initial.productoId());
            dao.delete(b, other.productoId());
        }
        System.out.println("TOTAL|" + passed + "|0 fallos");
    }

    private static Producto product(UUID tenant, UUID id, String code, String name, String price, String tax) {
        return new Producto(tenant, id, code, name, new BigDecimal(price), new BigDecimal(tax), "VENDIBLE", "unidad", "DIRECTO", true);
    }

    private static void check(String name, boolean condition) {
        if (!condition) { throw new AssertionError(name); }
        passed++;
        System.out.println("PASS|" + name);
    }

    private static void invalid(String name, Runnable action) {
        try { action.run(); }
        catch (IllegalArgumentException ex) { check(name, true); return; }
        throw new AssertionError(name);
    }
}
