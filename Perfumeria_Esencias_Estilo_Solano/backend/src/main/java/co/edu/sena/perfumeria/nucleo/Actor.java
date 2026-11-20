package co.edu.sena.perfumeria.nucleo;
public record Actor(String usuarioId,String nombre,String perfil) {
    public boolean admin() {
        return "ADMINISTRADOR".equals(perfil);
    }
    public void exigirAdmin() {
        if(!admin())throw new ApiError(403,"PERMISO_DENEGADO","Esta operación requiere Administrador");
    }
    public void exigirPropio(String usuario) {
        if(!admin()&&!usuarioId.equals(usuario))throw new ApiError(403,"PERMISO_DENEGADO","Sólo puede operar documentos propios");
    }
}
