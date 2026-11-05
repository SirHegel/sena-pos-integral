package co.edu.sena.pos.integral.nucleo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

public final class V {
    private V() {}
    public static ApiError error(String campo, String regla) {
        return new ApiError(400, "DATOS_INVALIDOS", campo + ": " + regla);
    }
    public static String uuid(Object valor, String campo) {
        if (!(valor instanceof String s) || !s.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
            throw error(campo, "UUID válido requerido");
        return UUID.fromString(s).toString();
    }
    public static String texto(Object valor, String campo, int maximo) {
        if (!(valor instanceof String s) || s.isBlank() || s.length() > maximo || s.codePoints().anyMatch(Character::isISOControl))
            throw error(campo, "texto no vacío, sin controles, máximo " + maximo + " caracteres");
        return s.strip();
    }
    public static String opcion(Object valor, String campo, Set<String> opciones) {
        String s = texto(valor, campo, 128);
        if (!opciones.contains(s)) throw error(campo, "opción no válida");
        return s;
    }
    public static BigDecimal decimal(Object valor, String campo, int escala, BigDecimal minimo, boolean estricto) {
        try {
            if (valor == null || valor instanceof Boolean) throw new NumberFormatException();
            BigDecimal n = new BigDecimal(valor.toString()).setScale(escala, RoundingMode.UNNECESSARY);
            if (n.precision() > 14 || (estricto ? n.compareTo(minimo) <= 0 : n.compareTo(minimo) < 0)) throw new NumberFormatException();
            return n;
        } catch (ArithmeticException | NumberFormatException e) {
            throw error(campo, "decimal de hasta 14 dígitos, escala " + escala + ", " + (estricto ? "mayor que " : "mínimo ") + minimo);
        }
    }
    public static int entero(Object valor, String campo, int minimo, int maximo) {
        try {
            if (valor == null || valor instanceof Boolean) throw new NumberFormatException();
            int n = new BigDecimal(valor.toString()).intValueExact();
            if (n < minimo || n > maximo) throw new NumberFormatException();
            return n;
        } catch (ArithmeticException | NumberFormatException e) { throw error(campo, "entero entre " + minimo + " y " + maximo); }
    }
    public static LocalDate fecha(Object valor, String campo) {
        try {
            if (!(valor instanceof String s) || !s.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException();
            return LocalDate.parse(s);
        } catch (RuntimeException e) { throw error(campo, "fecha válida AAAA-MM-DD"); }
    }
    public static YearMonth periodo(Object valor, String campo) {
        try {
            if (!(valor instanceof String s) || !s.matches("\\d{4}-\\d{2}")) throw new IllegalArgumentException();
            return YearMonth.parse(s);
        } catch (RuntimeException e) { throw error(campo, "período válido AAAA-MM"); }
    }
    public static boolean booleano(Object valor, String campo) {
        if (!(valor instanceof Boolean b)) throw error(campo, "booleano requerido");
        return b;
    }
}
