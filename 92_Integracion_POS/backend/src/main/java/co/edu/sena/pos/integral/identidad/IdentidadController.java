package co.edu.sena.pos.integral.identidad;

import co.edu.sena.pos.integral.nucleo.Actor;
import co.edu.sena.pos.integral.nucleo.Permitidos;
import co.edu.sena.pos.integral.nucleo.Sesiones;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IdentidadController {
    private final IdentidadService identidad;
    public IdentidadController(IdentidadService identidad) { this.identidad = identidad; }
    @GetMapping("/auth/csrf")
    public Object csrf(HttpServletRequest request) { return Map.of("csrfToken", Sesiones.token(request.getSession(true))); }
    @PostMapping("/auth/ingreso")
    public Object ingresar(@RequestBody IdentidadService.Ingreso r, HttpServletRequest request) {
        Actor actor = identidad.ingresar(r);
        var sesion = request.getSession(true);
        request.changeSessionId();
        sesion.setAttribute(Sesiones.TENANT, actor.tenantId());
        sesion.setAttribute(Sesiones.USUARIO, actor.usuarioId());
        sesion.removeAttribute(Sesiones.CSRF);
        return Map.of("actor", actor, "csrfToken", Sesiones.token(sesion));
    }
    @GetMapping("/auth/sesion")
    @Permitidos({"ADMINISTRADOR","CAJERO","INVENTARIO","NOMINA","CONTABILIDAD"})
    public Object sesion(Actor actor) { return actor; }
    @PostMapping("/auth/salir")
    @Permitidos({"ADMINISTRADOR","CAJERO","INVENTARIO","NOMINA","CONTABILIDAD"})
    public Object salir(HttpServletRequest request) { request.getSession(false).invalidate(); return Map.of("mensaje", "Sesión cerrada"); }
    @GetMapping("/usuarios") @Permitidos("ADMINISTRADOR")
    public Object usuarios(Actor actor) { return identidad.usuarios(actor); }
    @PostMapping("/usuarios") @Permitidos("ADMINISTRADOR") @ResponseStatus(HttpStatus.CREATED)
    public Object alta(Actor actor, @RequestBody IdentidadService.Alta r) { return identidad.crear(actor, r); }
    @PutMapping("/usuarios/{id}") @Permitidos("ADMINISTRADOR")
    public Object cambiar(Actor actor, @PathVariable String id, @RequestBody IdentidadService.Cambio r) { return identidad.cambiar(actor, id, r); }
    @GetMapping("/roles") @Permitidos("ADMINISTRADOR")
    public Object roles(Actor actor) { return identidad.roles(actor); }
}
