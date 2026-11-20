package co.edu.sena.perfumeria.catalogo;
public record Actor(String id,String nombre,String perfil) {
    public boolean isAdmin() {
        return "ADMINISTRADOR".equals(perfil);
    }
    public String getNombre() {
        return nombre;
    }
    public String getPerfil() {
        return perfil;
    }
    public void exigirAdmin() {
        if(!isAdmin())throw new SecurityException("La modificación del catálogo requiere Administrador");
    }
}
