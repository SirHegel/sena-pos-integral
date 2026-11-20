package co.edu.sena.perfumeria.nucleo;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.*;
import org.springframework.web.method.support.*;
import org.springframework.web.servlet.config.annotation.*;
@Configuration public class WebConfig implements WebMvcConfigurer {
    private final Sesiones s;
    public WebConfig(Sesiones s) {
        this.s=s;
    }
    public void addInterceptors(InterceptorRegistry r) {
        r.addInterceptor(s).addPathPatterns("/api/v2/**");
    }
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> r) {
        r.add(new HandlerMethodArgumentResolver() {
            public boolean supportsParameter(MethodParameter p) {
                return p.getParameterType()==Actor.class;
            }
            public Object resolveArgument(MethodParameter p,ModelAndViewContainer c,NativeWebRequest req,WebDataBinderFactory f) {
                Object a=req.getAttribute(Sesiones.ACTOR,RequestAttributes.SCOPE_REQUEST);if(a==null)throw new ApiError(401,"SESION_REQUERIDA","Debe iniciar sesión");return a;
            }
        }
        );
    }
}
