package co.edu.sena.pos.integral.nucleo;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final Sesiones sesiones;
    public WebConfig(Sesiones sesiones) { this.sesiones = sesiones; }
    @Override public void addInterceptors(InterceptorRegistry registry) { registry.addInterceptor(sesiones).addPathPatterns("/api/v1/**"); }
    @Override public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new HandlerMethodArgumentResolver() {
            @Override public boolean supportsParameter(MethodParameter p) { return p.getParameterType().equals(Actor.class); }
            @Override public Object resolveArgument(MethodParameter p, ModelAndViewContainer container, NativeWebRequest request, WebDataBinderFactory factory) {
                Object actor = request.getAttribute(Sesiones.ACTOR, RequestAttributes.SCOPE_REQUEST);
                if (!(actor instanceof Actor)) throw new ApiError(401, "SESION_REQUERIDA", "Debe iniciar sesión");
                return actor;
            }
        });
    }
}
