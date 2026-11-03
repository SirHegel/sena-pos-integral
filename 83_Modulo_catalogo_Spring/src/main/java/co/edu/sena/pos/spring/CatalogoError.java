package co.edu.sena.pos.spring;
public class CatalogoError extends RuntimeException {
    public final int status;
    public CatalogoError(int status, String message) { super(message); this.status=status; }
}
