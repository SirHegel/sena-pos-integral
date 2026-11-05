package co.edu.sena.pos.integral.nucleo;

public class ApiError extends RuntimeException {
    private final int estado;
    private final String codigo;
    public ApiError(int estado, String codigo, String mensaje) {
        super(mensaje); this.estado = estado; this.codigo = codigo;
    }
    public int estado() { return estado; }
    public String codigo() { return codigo; }
}
