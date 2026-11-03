package co.edu.sena.pos.web;

import co.edu.sena.pos.catalogo.Producto;
import co.edu.sena.pos.catalogo.ProductoDao;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.util.*;

/** Servlet explícito: GET consulta/formulario; POST crea, actualiza y elimina. */
public class CatalogoServlet extends HttpServlet {
    private final ProductoDao dao;
    private static final String A = "11111111-1111-4111-8111-111111111111";
    private static final String B = "22222222-2222-4222-8222-222222222222";

    public CatalogoServlet(ProductoDao dao) { this.dao = dao; }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        prepare(req, res);
        try {
            UUID tenant = tenant(req.getParameter("tenant"));
            req.setAttribute("tenant", tenant.toString());
            String token = (String) req.getSession().getAttribute("csrf");
            if (token == null) { token = UUID.randomUUID().toString(); req.getSession().setAttribute("csrf", token); }
            req.setAttribute("csrf", token);
            String mode = Objects.requireNonNullElse(req.getParameter("modo"), "lista");
            if (mode.equals("nuevo") || mode.equals("editar")) {
                Producto p = mode.equals("editar") ? dao.find(tenant, canonicalUuid(req.getParameter("id"))).orElseThrow(
                    () -> new IllegalArgumentException("Producto no encontrado en el negocio")) : null;
                req.setAttribute("producto", p);
                view(req, res, "formulario");
                return;
            }
            if (!mode.equals("lista")) { throw new IllegalArgumentException("Vista no válida"); }
            String query = Objects.requireNonNullElse(req.getParameter("q"), "").strip();
            if (query.length() > 128) { throw new IllegalArgumentException("Búsqueda demasiado larga"); }
            String normalized = query.toLowerCase(Locale.ROOT);
            req.setAttribute("q", query);
            req.setAttribute("productos", dao.list(tenant).stream().filter(p ->
                p.nombre().toLowerCase(Locale.ROOT).contains(normalized) || p.codigo().toLowerCase(Locale.ROOT).contains(normalized)).toList());
            Map<String,String> messages = Map.of("creado", "Producto creado", "actualizado", "Producto actualizado", "eliminado", "Producto eliminado");
            req.setAttribute("mensaje", messages.getOrDefault(Objects.requireNonNullElse(req.getParameter("msg"), ""), ""));
            view(req, res, "catalogo");
        } catch (IllegalArgumentException ex) { error(req, res, 400, ex.getMessage()); }
        catch (SQLException ex) { error(req, res, 503, "No fue posible consultar el catálogo"); }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        prepare(req, res);
        HttpSession session = req.getSession(false);
        String supplied = req.getParameter("csrf");
        String expected = session == null ? null : (String) session.getAttribute("csrf");
        if (supplied == null || expected == null || !MessageDigest.isEqual(
                supplied.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
            error(req, res, 403, "La solicitud no tiene una confirmación de sesión válida. Abra de nuevo el formulario.");
            return;
        }
        try {
            UUID tenant = tenant(req.getParameter("tenant"));
            String action = Objects.requireNonNullElse(req.getParameter("accion"), "");
            String message;
            if (action.equals("crear") || action.equals("actualizar")) {
                UUID id = action.equals("crear") ? UUID.randomUUID() : canonicalUuid(req.getParameter("id"));
                boolean active = true;
                if (action.equals("actualizar")) {
                    Optional<Producto> existing = dao.find(tenant, id);
                    if (existing.isEmpty()) { error(req, res, 404, "Producto no encontrado en el negocio"); return; }
                    active = existing.get().activo();
                }
                Producto p = new Producto(tenant, id, req.getParameter("codigo"), req.getParameter("nombre"),
                    new BigDecimal(req.getParameter("precio")), new BigDecimal(req.getParameter("iva")),
                    req.getParameter("tipo"), req.getParameter("unidad"), req.getParameter("modoDescuento"), active);
                if (action.equals("crear")) { dao.create(p); message = "creado"; }
                else {
                    if (!dao.update(p)) { error(req, res, 404, "Producto no encontrado en el negocio"); return; }
                    message = "actualizado";
                }
            } else if (action.equals("eliminar")) {
                if (!dao.delete(tenant, canonicalUuid(req.getParameter("id")))) {
                    error(req, res, 404, "Producto no encontrado en el negocio"); return;
                }
                message = "eliminado";
            } else { throw new IllegalArgumentException("Operación no válida"); }
            // Post/Redirect/Get evita repetir la escritura al recargar la página de resultado.
            res.setStatus(303);
            res.setHeader("Location", req.getContextPath() + "/catalogo?tenant=" + tenant + "&msg=" + message);
        } catch (IllegalArgumentException | NullPointerException ex) { error(req, res, 400, "Revise los datos y límites del producto"); }
        catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) { error(req, res, 409, "Ya existe ese código en el negocio"); }
            else if (ex.getErrorCode() == 1451) { error(req, res, 409, "El producto tiene referencias y no puede eliminarse"); }
            else { error(req, res, 503, "No fue posible guardar el cambio"); }
        }
    }

    private static UUID tenant(String value) {
        String selected = value == null ? A : value;
        if (!Set.of(A, B).contains(selected)) { throw new IllegalArgumentException("Seleccione un negocio de demostración"); }
        return UUID.fromString(selected);
    }

    private static UUID canonicalUuid(String value) {
        if (value == null) { throw new IllegalArgumentException("Falta el identificador del producto"); }
        UUID parsed = UUID.fromString(value);
        if (!parsed.toString().equalsIgnoreCase(value)) { throw new IllegalArgumentException("Identificador no válido"); }
        return parsed;
    }

    private static void prepare(HttpServletRequest req, HttpServletResponse res) throws IOException {
        req.setCharacterEncoding("UTF-8"); res.setCharacterEncoding("UTF-8");
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("Content-Security-Policy", "default-src 'self'; style-src 'self'; form-action 'self'; frame-ancestors 'none'");
        res.setHeader("Cache-Control", "no-store");
    }

    private static void error(HttpServletRequest req, HttpServletResponse res, int status, String message) throws ServletException, IOException {
        res.setStatus(status); req.setAttribute("error", message); view(req, res, "error");
    }

    private static void view(HttpServletRequest req, HttpServletResponse res, String name) throws ServletException, IOException {
        req.getRequestDispatcher("/WEB-INF/jsp/" + name + ".jsp").forward(req, res);
    }
}
