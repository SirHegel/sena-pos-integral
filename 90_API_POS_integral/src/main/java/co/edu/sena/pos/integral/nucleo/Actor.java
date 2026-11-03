package co.edu.sena.pos.integral.nucleo;

import java.util.Arrays;
import java.util.Set;

public record Actor(String tenantId, String usuarioId, Set<String> roles) {
    public Actor { roles = Set.copyOf(roles); }
    public boolean tiene(String... permitidos) {
        return Arrays.stream(permitidos).anyMatch(roles::contains);
    }
    public void exigir(String... permitidos) {
        if (!tiene(permitidos)) throw new ApiError(403, "PERMISO_DENEGADO", "La operación no está autorizada para el rol actual");
    }
}
