package co.edu.sena.perfumeria.web;
import co.edu.sena.perfumeria.catalogo.*;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.*;
/** GET sólo consulta; POST autentica o escribe. La sesión identifica a la persona, nunca al perfil enviado. */
public class CatalogoServlet extends HttpServlet {
    private final ArticuloDao dao;
    private final AuthDao auth;
    public CatalogoServlet(ArticuloDao dao,AuthDao auth) {
        this.dao=dao;
        this.auth=auth;
    }
    @Override protected void doGet(HttpServletRequest q,HttpServletResponse s)throws IOException,ServletException {
        preparar(q,s);
        token(q);
        try {
            Actor a=actual(q);
            if(a==null) {
                vista(q,s,"acceso");
                return;
            }
            q.setAttribute("actor",a);
            q.setAttribute("admin",a.isAdmin());
            String modo=Objects.requireNonNullElse(q.getParameter("modo"),"lista");
            if(modo.equals("nuevo")||modo.equals("editar")) {
                a.exigirAdmin();
                Articulo x=modo.equals("editar")?dao.buscar(a,q.getParameter("id")).orElse(null):null;
                if(modo.equals("editar")&&x==null) {
                    error(q,s,404,"Artículo no encontrado");
                    return;
                }
                q.setAttribute("articulo",x);
                vista(q,s,"formulario");
                return;
            }
            if(!modo.equals("lista"))throw new IllegalArgumentException("Vista no válida");
            String busqueda=Objects.requireNonNullElse(q.getParameter("q"),"").strip();
            if(busqueda.codePointCount(0,busqueda.length())>160)throw new IllegalArgumentException("Búsqueda: máximo 160 caracteres");
            String normal=busqueda.toLowerCase(Locale.ROOT);
            q.setAttribute("q",busqueda);
            q.setAttribute("articulos",dao.listar(a).stream().filter(x->x.getCodigo().toLowerCase(Locale.ROOT).contains(normal)||x.getNombre().toLowerCase(Locale.ROOT).contains(normal)).toList());
            var mensajes=Map.of("creado","Artículo creado","actualizado","Artículo actualizado","eliminado","Artículo eliminado");
            q.setAttribute("mensaje",mensajes.getOrDefault(Objects.requireNonNullElse(q.getParameter("msg"),""),""));
            vista(q,s,"catalogo");
        }
        catch(SecurityException e) {
            error(q,s,403,e.getMessage());
        }
        catch(IllegalArgumentException e) {
            error(q,s,400,e.getMessage());
        }
        catch(SQLException e) {
            error(q,s,503,"No fue posible consultar el catálogo");
        }
    }
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse s)throws IOException,ServletException {
        preparar(q,s);
        HttpSession ses=q.getSession(false);
        String supplied=q.getParameter("csrf"),expected=ses==null?null:(String)ses.getAttribute("csrf");
        if(supplied==null||expected==null||!MessageDigest.isEqual(supplied.getBytes(StandardCharsets.UTF_8),expected.getBytes(StandardCharsets.UTF_8))) {
            error(q,s,403,"Confirmación de sesión inválida. Abra de nuevo el formulario.");
            return;
        }
        try {
            String accion=Objects.requireNonNullElse(q.getParameter("accion"),"");
            if(accion.equals("login")) {
                Actor a=auth.autenticar(q.getParameter("nombre_acceso"),q.getParameter("clave"));
                q.changeSessionId();
                ses.setAttribute("usuario",a.id());
                ses.removeAttribute("csrf");
                redirigir(q,s,"");
                return;
            }
            Actor a=actual(q);
            if(a==null) {
                error(q,s,401,"Inicie sesión para continuar");
                return;
            }
            if(accion.equals("logout")) {
                ses.invalidate();
                redirigir(q,s,"");
                return;
            }
            a.exigirAdmin();
            String msg;
            switch(accion) {
                case "crear"-> {
                    var x=new Articulo(UUID.randomUUID().toString(),q.getParameter("codigo"),q.getParameter("nombre"),q.getParameter("unidad"),q.getParameter("clase"),q.getParameter("funcion"),opcional(q.getParameter("nominal")),new BigDecimal(q.getParameter("minimo")),true);
                    dao.crear(a,x);
                    msg="creado";
                }
                case "actualizar"-> {
                    if(!dao.actualizar(a,q.getParameter("id"),q.getParameter("nombre"),new BigDecimal(q.getParameter("minimo")))) {
                        error(q,s,404,"Artículo no encontrado");
                        return;
                    }
                    msg="actualizado";
                }
                case "eliminar"-> {
                    if(!dao.eliminar(a,q.getParameter("id"))) {
                        error(q,s,404,"Artículo no encontrado");
                        return;
                    }
                    msg="eliminado";
                }
                default->throw new IllegalArgumentException("Operación no válida");
            }
            // Post/Redirect/Get; recargar la consulta no repite la escritura.
            redirigir(q,s,"?msg="+msg);
        }
        catch(SecurityException e) {
            error(q,s,403,e.getMessage());
        }
        catch(IllegalArgumentException|NullPointerException e) {
            error(q,s,400,"Revise los datos: textos hasta 160 caracteres y cantidades no negativas con hasta 6 decimales; capacidad positiva cuando aplique.");
        }
        catch(SQLException e) {
            error(q,s,e.getErrorCode()==1062||e.getErrorCode()==1451?409:503,switch(e.getErrorCode()) {
                case 1062->"Ya existe ese código";case 1451->"Artículo referenciado: conserve su historia; no puede eliminarse";default->"No fue posible guardar el cambio";
            }
            );
        }
    }
    private Actor actual(HttpServletRequest q)throws SQLException {
        var ses=q.getSession(false);
        if(ses==null||ses.getAttribute("usuario")==null)return null;
        var a=auth.actor(ses.getAttribute("usuario").toString());
        if(a.isEmpty())ses.removeAttribute("usuario");
        return a.orElse(null);
    }
    private static BigDecimal opcional(String s) {
        return s==null||s.isBlank()?null:new BigDecimal(s);
    }
    private void token(HttpServletRequest q) {
        var ses=q.getSession();
        if(ses.getAttribute("csrf")==null)ses.setAttribute("csrf",UUID.randomUUID().toString());
        q.setAttribute("csrf",ses.getAttribute("csrf"));
    }
    private void preparar(HttpServletRequest q,HttpServletResponse s)throws IOException {
        q.setCharacterEncoding("UTF-8");
        s.setCharacterEncoding("UTF-8");
        s.setHeader("Cache-Control","no-store");
        s.setHeader("X-Content-Type-Options","nosniff");
        s.setHeader("Content-Security-Policy","default-src 'self'; style-src 'self'; form-action 'self'; frame-ancestors 'none'");
    }
    private void redirigir(HttpServletRequest q,HttpServletResponse s,String query) {
        s.setStatus(303);
        s.setHeader("Location",q.getContextPath()+"/catalogo"+query);
    }
    private void error(HttpServletRequest q,HttpServletResponse s,int status,String mensaje)throws ServletException,IOException {
        s.setStatus(status);
        q.setAttribute("error",mensaje);
        vista(q,s,"error");
    }
    private void vista(HttpServletRequest q,HttpServletResponse s,String nombre)throws ServletException,IOException {
        q.getRequestDispatcher("/WEB-INF/jsp/"+nombre+".jsp").forward(q,s);
    }
}
