package co.edu.sena.perfumeria.nucleo;
import co.edu.sena.perfumeria.servicio.AuthService;
import jakarta.servlet.http.*;
import java.security.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
@Component public class Sesiones implements HandlerInterceptor {
    public static final String ACTOR="ps.actor",USUARIO="ps.usuario",CSRF="ps.csrf";
    private static final SecureRandom RAND=new SecureRandom();
    private final AuthService auth;
    public Sesiones(AuthService a) {
        auth=a;
    }
    public static String token(HttpSession s) {
        String t=(String)s.getAttribute(CSRF);
        if(t==null) {
            byte[] b=new byte[32];
            RAND.nextBytes(b);
            t=Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            s.setAttribute(CSRF,t);
        }
        return t;
    }
    @Override public boolean preHandle(HttpServletRequest r,HttpServletResponse p,Object handler) {
        p.setHeader("Cache-Control","no-store");
        p.setHeader("X-Content-Type-Options","nosniff");
        p.setHeader("X-Frame-Options","DENY");
        String path=r.getRequestURI();
        if(path.equals("/api/v2/auth/sesion"))return true;
        HttpSession s=r.getSession(false);
        if(!Set.of("GET","HEAD","OPTIONS").contains(r.getMethod())) {
            String t=s==null?null:(String)s.getAttribute(CSRF),got=r.getHeader("X-CSRF-Token");
            if(t==null||got==null||!MessageDigest.isEqual(t.getBytes(StandardCharsets.UTF_8),got.getBytes(StandardCharsets.UTF_8)))throw new ApiError(403,"CSRF_INVALIDO","Token CSRF ausente o inválido");
        }
        if(path.equals("/api/v2/auth/login"))return true;
        if(s==null||!(s.getAttribute(USUARIO)instanceof String uid))throw new ApiError(401,"SESION_REQUERIDA","Debe iniciar sesión");
        try {
            r.setAttribute(ACTOR,auth.actor(uid));
        }
        catch(ApiError e) {
            s.invalidate();
            throw new ApiError(401,"SESION_INVALIDA","Cuenta no disponible");
        }
        return true;
    }
}
