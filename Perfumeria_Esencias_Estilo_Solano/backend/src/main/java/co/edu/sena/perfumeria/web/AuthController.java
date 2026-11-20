package co.edu.sena.perfumeria.web;
import co.edu.sena.perfumeria.servicio.AuthService;
import co.edu.sena.perfumeria.nucleo.*;
import jakarta.servlet.http.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v2/auth") public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService a) {
        auth=a;
    }
    private Map<String,Object> estado(HttpServletRequest r) {
        var s=r.getSession(true);
        var m=new LinkedHashMap<String,Object>();
        m.put("csrf",Sesiones.token(s));
        boolean logged=s.getAttribute(Sesiones.USUARIO)!=null;
        m.put("autenticado",logged);
        if(logged) {
            Actor a=auth.actor(s.getAttribute(Sesiones.USUARIO).toString());
            m.put("actor",Map.of("usuario_id",a.usuarioId(),"nombre",a.nombre(),"perfil",a.perfil()));
        }
        return m;
    }
    @GetMapping("/sesion")public Object sesion(HttpServletRequest r) {
        return estado(r);
    }
    @PostMapping("/login")public Object login(@RequestBody Map<String,Object> m,HttpServletRequest r) {
        Actor a=auth.login(m);
        r.changeSessionId();
        r.getSession().setAttribute(Sesiones.USUARIO,a.usuarioId());
        r.getSession().removeAttribute(Sesiones.CSRF);
        return estado(r);
    }
    @PostMapping("/logout")public Object logout(HttpServletRequest r) {
        r.getSession().invalidate();
        return estado(r);
    }
}
