package co.edu.sena.perfumeria.web;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
/** Adds stable presentation aliases while preserving relational field names. */
@ControllerAdvice public class PresentacionJson implements ResponseBodyAdvice<Object> {
    public boolean supports(MethodParameter p,Class<? extends HttpMessageConverter<?>> c) {
        return p.getContainingClass().getPackageName().equals("co.edu.sena.perfumeria.web")&&p.getParameterType()!=byte[].class;
    }
    public Object beforeBodyWrite(Object b,MethodParameter p,MediaType t,Class<? extends HttpMessageConverter<?>> c,ServerHttpRequest q,ServerHttpResponse r) {
        return adaptar(b);
    }
    private static void alias(Map<String,Object> r,String from,String to) {
        if(r.containsKey(from)&&!r.containsKey(to))r.put(to,r.get(from));
    }
    private static Object adaptar(Object x) {
        if(x instanceof java.sql.Timestamp ts)return ts.toInstant().toString();
        if(x instanceof java.time.LocalDateTime dt)return dt.toInstant(java.time.ZoneOffset.UTC).toString();
        if(x instanceof List<?> l)return l.stream().map(PresentacionJson::adaptar).toList();
        if(!(x instanceof Map<?,?> m))return x;
        var r=new LinkedHashMap<String,Object>();
        m.forEach((k,v)->r.put(k.toString(),adaptar(v)));
        alias(r,"cliente","cliente_nombre");
        alias(r,"creada_en","fecha_creacion");
        alias(r,"emitida_en","fecha_emision");
        alias(r,"descripcion_entregada","nombre_venta");
        alias(r,"cantidad_unidades","unidades");
        alias(r,"confirmado_en","fecha_cobro");
        alias(r,"abierta_en","fecha_apertura");
        alias(r,"responsable","responsable_nombre");
        alias(r,"fondo_inicial_cop","fondo_cop");
        alias(r,"esperado_cop","importe_esperado_cop");
        alias(r,"soporte_ref","documento_ref");
        alias(r,"registrado_en","fecha");
        alias(r,"registrada_en","fecha");
        alias(r,"costo_unitario_base_cop","costo_unitario");
        if(r.containsKey("cantidad_unidades")&&r.containsKey("precio_unitario_cop"))r.put("importe_cop",new BigDecimal(r.get("cantidad_unidades").toString()).multiply(new BigDecimal(r.get("precio_unitario_cop").toString())));
        if(r.containsKey("abierta_en"))r.put("estado",r.get("cerrada_en")==null?"ABIERTO":"CERRADO");
        if(r.get("cantidad_base")!=null)r.put("direccion",new BigDecimal(r.get("cantidad_base").toString()).signum()<0?"SALIDA":"ENTRADA");
        return r;
    }
}
