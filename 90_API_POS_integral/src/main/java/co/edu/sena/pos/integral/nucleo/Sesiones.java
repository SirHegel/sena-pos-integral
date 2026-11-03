package co.edu.sena.pos.integral.nucleo;

import co.edu.sena.pos.integral.identidad.IdentidadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class Sesiones implements HandlerInterceptor {
    public static final String ACTOR = "pos.actor", TENANT = "pos.tenant", USUARIO = "pos.usuario", CSRF = "pos.csrf";
    private static final SecureRandom RANDOM = new SecureRandom();
    private final IdentidadService identidad;
    public Sesiones(IdentidadService identidad) { this.identidad = identidad; }
    public static String token(HttpSession sesion) {
        String actual = (String) sesion.getAttribute(CSRF);
        if (actual == null) {
            byte[] bytes = new byte[32]; RANDOM.nextBytes(bytes);
            actual = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); sesion.setAttribute(CSRF, actual);
        }
        return actual;
    }
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        if (!(handler instanceof HandlerMethod metodo) || !request.getRequestURI().startsWith("/api/v1/")) return true;
        String ruta = request.getRequestURI();
        boolean csrfPublico = request.getMethod().equals("GET") && ruta.equals("/api/v1/auth/csrf");
        boolean ingreso = request.getMethod().equals("POST") && ruta.equals("/api/v1/auth/ingreso");
        if (csrfPublico) return true;
        HttpSession sesion = request.getSession(false);
        if (!SetLecturas.esLectura(request.getMethod())) {
            String esperado = sesion == null ? null : (String) sesion.getAttribute(CSRF);
            String recibido = request.getHeader("X-CSRF-Token");
            if (esperado == null || recibido == null || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.US_ASCII), recibido.getBytes(StandardCharsets.US_ASCII)))
                throw new ApiError(403, "CSRF_INVALIDO", "Token CSRF ausente o no válido");
        }
        if (ingreso) return true;
        if (sesion == null || !(sesion.getAttribute(TENANT) instanceof String t) || !(sesion.getAttribute(USUARIO) instanceof String u))
            throw new ApiError(401, "SESION_REQUERIDA", "Debe iniciar sesión");
        Actor actor;
        try { actor = identidad.actor(t, u); }
        catch (ApiError e) { sesion.invalidate(); throw e; }
        Permitidos permiso = AnnotatedElementUtils.findMergedAnnotation(metodo.getMethod(), Permitidos.class);
        if (permiso == null) permiso = AnnotatedElementUtils.findMergedAnnotation(metodo.getBeanType(), Permitidos.class);
        if (permiso == null) throw new ApiError(403, "PERMISO_NO_DEFINIDO", "La ruta no tiene un permiso definido");
        actor.exigir(permiso.value());
        request.setAttribute(ACTOR, actor);
        return true;
    }
    private static final class SetLecturas {
        static boolean esLectura(String metodo) { return metodo.equals("GET") || metodo.equals("HEAD") || metodo.equals("OPTIONS"); }
    }
}
