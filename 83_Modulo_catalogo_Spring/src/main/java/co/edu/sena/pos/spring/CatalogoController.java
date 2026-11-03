package co.edu.sena.pos.spring;
import co.edu.sena.pos.catalogo.Producto;
import jakarta.servlet.http.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Controller
public class CatalogoController {
    private final CatalogoService service;
    private static final String A="11111111-1111-4111-8111-111111111111";
    private static final String B="22222222-2222-4222-8222-222222222222";
    public CatalogoController(CatalogoService service) { this.service=service; }
    @GetMapping("/") public String home(){ return "redirect:/catalogo"; }

    @GetMapping("/catalogo")
    public String list(@RequestParam MultiValueMap<String,String> params, HttpSession session,Model model) {
        UUID tenant=tenant(params.getFirst("tenant"));model.addAttribute("tenant",tenant.toString());
        if(session.getAttribute("csrf")==null)session.setAttribute("csrf",UUID.randomUUID().toString());
        model.addAttribute("csrf",session.getAttribute("csrf"));
        String mode=Objects.requireNonNullElse(params.getFirst("modo"),"lista");
        if(mode.equals("nuevo"))return "formulario";
        if(mode.equals("editar")){ model.addAttribute("producto",service.find(tenant,id(params.getFirst("id"))));return "formulario"; }
        if(!mode.equals("lista"))throw new CatalogoError(400,"Vista no válida");
        String q=Objects.requireNonNullElse(params.getFirst("q"),"").strip();
        if(q.length()>128)throw new CatalogoError(400,"Búsqueda demasiado larga");
        model.addAttribute("q",q);model.addAttribute("productos",service.list(tenant,q));
        model.addAttribute("mensaje",Map.of("creado","Producto creado","actualizado","Producto actualizado","eliminado","Producto eliminado").getOrDefault(Objects.requireNonNullElse(params.getFirst("msg"),""),""));
        return "catalogo";
    }

    @PostMapping("/catalogo")
    public String save(@RequestParam MultiValueMap<String,String> params,HttpSession session,HttpServletResponse response) {
        String actual=params.getFirst("csrf"),expected=(String)session.getAttribute("csrf");
        if(actual==null||expected==null||!MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8),expected.getBytes(StandardCharsets.UTF_8)))throw new CatalogoError(403,"Abra de nuevo el formulario para confirmar la solicitud");
        UUID tenant=tenant(params.getFirst("tenant"));String action=Objects.requireNonNullElse(params.getFirst("accion"),"");String message;
        if(action.equals("crear")||action.equals("actualizar")){
            Producto p=new Producto(tenant,action.equals("crear")?UUID.randomUUID():id(params.getFirst("id")),params.getFirst("codigo"),params.getFirst("nombre"),new BigDecimal(params.getFirst("precio")),new BigDecimal(params.getFirst("iva")),params.getFirst("tipo"),params.getFirst("unidad"),params.getFirst("modoDescuento"),true);
            if(action.equals("crear")){service.create(p);message="creado";}else{service.update(p);message="actualizado";}
        }else if(action.equals("eliminar")){service.delete(tenant,id(params.getFirst("id")));message="eliminado";}
        else throw new CatalogoError(400,"Operación no válida");
        response.setStatus(303);response.setHeader("Location","/catalogo?tenant="+tenant+"&msg="+message);return null;
    }
    @ExceptionHandler(CatalogoError.class)
    public String business(CatalogoError ex,HttpServletResponse res,Model model){res.setStatus(ex.status);model.addAttribute("error",ex.getMessage());return "error";}
    @ExceptionHandler({IllegalArgumentException.class,NullPointerException.class})
    public String invalid(Exception ex,HttpServletResponse res,Model model){res.setStatus(400);model.addAttribute("error","Revise los datos y límites del producto");return "error";}
    @ExceptionHandler(DataIntegrityViolationException.class)
    public String integrity(Exception ex,HttpServletResponse res,Model model){res.setStatus(409);model.addAttribute("error","Código duplicado o producto con referencias existentes");return "error";}
    @ExceptionHandler(DataAccessException.class)
    public String data(Exception ex,HttpServletResponse res,Model model){res.setStatus(503);model.addAttribute("error","Servicio temporalmente no disponible");return "error";}
    private static UUID tenant(String value){String v=value==null?A:value;if(!Set.of(A,B).contains(v))throw new CatalogoError(400,"Seleccione un negocio de demostración");return UUID.fromString(v);}
    private static UUID id(String value){if(value==null)throw new CatalogoError(400,"Falta identificador");UUID id=UUID.fromString(value);if(!id.toString().equalsIgnoreCase(value))throw new CatalogoError(400,"Identificador inválido");return id;}
}
