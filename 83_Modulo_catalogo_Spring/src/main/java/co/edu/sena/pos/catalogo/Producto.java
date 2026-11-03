package co.edu.sena.pos.catalogo;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

/** Contrato del catálogo GA6: importes exactos y claves compuestas por negocio. */
public record Producto(UUID tenantId, UUID productoId, String codigo, String nombre,
        BigDecimal precio, BigDecimal iva, String tipo, String unidad,
        String modoDescuento, boolean activo) {
    public Producto {
        if (tenantId == null || productoId == null) {
            throw new IllegalArgumentException("Las claves UUID son obligatorias");
        }
        codigo = texto(codigo, 64, "código");
        nombre = texto(nombre, 128, "nombre");
        unidad = texto(unidad, 32, "unidad");
        if (precio == null || precio.signum() < 0 || precio.compareTo(new BigDecimal("999999999999.99")) > 0
                || precio.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("Precio inválido: no negativo, máximo 12 enteros y 2 decimales");
        }
        if (iva == null || iva.signum() < 0 || iva.compareTo(BigDecimal.ONE) > 0
                || iva.stripTrailingZeros().scale() > 4) {
            throw new IllegalArgumentException("IVA inválido: de 0 a 1, máximo 4 decimales");
        }
        if (tipo == null || !Set.of("VENDIBLE", "INSUMO", "PLATO").contains(tipo)) {
            throw new IllegalArgumentException("Tipo de producto inválido");
        }
        if (modoDescuento == null || !Set.of("DIRECTO", "RECETA").contains(modoDescuento)
                || ("RECETA".equals(modoDescuento) && !"PLATO".equals(tipo))) {
            throw new IllegalArgumentException("Modo de descuento incompatible");
        }
    }

    private static String texto(String value, int max, String campo) {
        if (value == null || value.isBlank() || value.strip().length() > max
                || value.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Campo " + campo + " inválido");
        }
        return value.strip();
    }
}
