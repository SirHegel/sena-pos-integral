package co.edu.sena.pos.integral.identidad;

import co.edu.sena.pos.integral.nucleo.Actor;
import co.edu.sena.pos.integral.nucleo.ApiError;
import co.edu.sena.pos.integral.nucleo.Db;
import co.edu.sena.pos.integral.nucleo.V;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentidadService {
    public static final Set<String> ROLES = Set.of("ADMINISTRADOR", "CAJERO", "INVENTARIO", "NOMINA", "CONTABILIDAD");
    private static final Pattern CORREO = Pattern.compile("^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?)+$");
    private final Db db;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final String dummyHash;
    public IdentidadService(Db db, PasswordEncoder passwords, Clock clock) {
        this.db = db; this.passwords = passwords; this.clock = clock;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }
    public static String correo(String valor) {
        String s = valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
        String local = s.contains("@") ? s.substring(0, s.indexOf('@')) : "";
        if (s.length() > 254 || local.length() > 64 || local.startsWith(".") || local.endsWith(".") || local.contains("..") || !CORREO.matcher(s).matches())
            throw V.error("correo", "correo válido de hasta 254 caracteres requerido");
        return s;
    }
    public static String clave(String valor) {
        if (valor == null || valor.isBlank() || valor.length() < 8 || valor.length() > 64 || valor.getBytes(StandardCharsets.UTF_8).length > 72 || valor.codePoints().anyMatch(Character::isISOControl))
            throw V.error("password", "entre 8 y 64 caracteres, máximo 72 bytes UTF-8, sin controles");
        return valor;
    }
    public record Ingreso(String tenantId, String correo, String password) {}
    public record Alta(String correo, String password, Set<String> roles) {}
    public record Cambio(Boolean activo, Set<String> roles) {}

    @Transactional(noRollbackFor = ApiError.class)
    public Actor ingresar(Ingreso solicitud) {
        if (solicitud == null) throw V.error("solicitud", "cuerpo requerido");
        String t = V.uuid(solicitud.tenantId(), "tenantId"), email = correo(solicitud.correo()), pass = clave(solicitud.password());
        var fila = db.uno("SELECT u.usuario_id,u.hash_password,u.activo,u.intentos_fallidos,u.bloqueado_hasta,t.activo AS negocio_activo FROM usuario u JOIN tenant t ON t.tenant_id=u.tenant_id WHERE u.tenant_id=? AND u.correo=? FOR UPDATE", t, email);
        if (fila.isEmpty() || !activo(fila.get().get("activo")) || !activo(fila.get().get("negocio_activo"))) {
            passwords.matches(pass, dummyHash); throw denegado();
        }
        var u = fila.get();
        Instant ahora = clock.instant();
        Object fechaBloqueo = u.get("bloqueado_hasta");
        Instant bloqueo = fechaBloqueo == null ? null : fechaBloqueo instanceof LocalDateTime local ? local.toInstant(ZoneOffset.UTC) : ((Timestamp) fechaBloqueo).toInstant();
        if (bloqueo != null && ahora.isBefore(bloqueo)) {
            passwords.matches(pass, dummyHash);
            throw new ApiError(429, "BLOQUEO_TEMPORAL", "Acceso bloqueado temporalmente por intentos fallidos");
        }
        int previos = bloqueo == null ? ((Number) u.get("intentos_fallidos")).intValue() : 0;
        if (!passwords.matches(pass, u.get("hash_password").toString())) {
            int intentos = previos + 1;
            Timestamp hasta = intentos >= 3 ? Timestamp.from(ahora.plusSeconds(30)) : null;
            db.cambiar("UPDATE usuario SET intentos_fallidos=?,bloqueado_hasta=? WHERE tenant_id=? AND usuario_id=?", intentos, hasta, t, u.get("usuario_id"));
            if (hasta != null) throw new ApiError(429, "BLOQUEO_TEMPORAL", "Tres intentos fallidos. Acceso bloqueado por 30 segundos");
            throw denegado();
        }
        db.cambiar("UPDATE usuario SET intentos_fallidos=0,bloqueado_hasta=NULL WHERE tenant_id=? AND usuario_id=?", t, u.get("usuario_id"));
        return actor(t, u.get("usuario_id").toString());
    }
    public Actor actor(String tenant, String usuario) {
        if (!db.existe("SELECT 1 FROM usuario u JOIN tenant t ON t.tenant_id=u.tenant_id WHERE u.tenant_id=? AND u.usuario_id=? AND u.activo=1 AND t.activo=1", tenant, usuario))
            throw new ApiError(401, "SESION_NO_VALIDA", "La sesión ya no está disponible");
        Set<String> roles = db.lista("SELECT r.nombre FROM usuario_rol ur JOIN rol r ON r.tenant_id=ur.tenant_id AND r.rol_id=ur.rol_id WHERE ur.tenant_id=? AND ur.usuario_id=?", tenant, usuario).stream()
                .map(r -> r.get("nombre").toString()).filter(ROLES::contains).collect(Collectors.toSet());
        return new Actor(tenant, usuario, roles);
    }
    public List<Map<String,Object>> usuarios(Actor a) {
        return db.lista("SELECT usuario_id,correo,activo FROM usuario WHERE tenant_id=? ORDER BY correo", a.tenantId()).stream().map(u -> {
            var result = new LinkedHashMap<>(u);
            result.put("roles", db.lista("SELECT r.nombre FROM usuario_rol ur JOIN rol r ON r.tenant_id=ur.tenant_id AND r.rol_id=ur.rol_id WHERE ur.tenant_id=? AND ur.usuario_id=? ORDER BY r.nombre", a.tenantId(), u.get("usuario_id")).stream().map(r->r.get("nombre")).toList());
            return (Map<String,Object>) result;
        }).toList();
    }
    public List<Map<String,Object>> roles(Actor a) {
        return db.lista("SELECT rol_id,nombre FROM rol WHERE tenant_id=? ORDER BY nombre", a.tenantId());
    }
    @Transactional
    public Map<String,Object> crear(Actor a, Alta r) {
        if (r == null) throw V.error("solicitud", "cuerpo requerido");
        String email = correo(r.correo()), pass = clave(r.password()), id = UUID.randomUUID().toString();
        validarRoles(r.roles());
        bloquearTenant(a);
        db.cambiar("INSERT INTO usuario(tenant_id,usuario_id,correo,hash_password) VALUES(?,?,?,?)", a.tenantId(), id, email, passwords.encode(pass));
        asignar(a.tenantId(), id, r.roles());
        return Map.of("usuarioId", id, "correo", email, "roles", r.roles(), "activo", true);
    }
    @Transactional
    public Map<String,Object> cambiar(Actor a, String id, Cambio r) {
        id = V.uuid(id, "usuarioId");
        if (r == null || r.activo() == null || r.roles() == null) throw V.error("solicitud", "activo y roles requeridos");
        validarRoles(r.roles());
        bloquearTenant(a);
        var actual = db.requerido("SELECT usuario_id,activo FROM usuario WHERE tenant_id=? AND usuario_id=? FOR UPDATE", a.tenantId(), id);
        boolean eraAdmin = activo(actual.get("activo")) && db.existe("SELECT 1 FROM usuario_rol ur JOIN rol r ON r.tenant_id=ur.tenant_id AND r.rol_id=ur.rol_id WHERE ur.tenant_id=? AND ur.usuario_id=? AND r.nombre='ADMINISTRADOR'", a.tenantId(), id);
        if (eraAdmin && (!r.activo() || !r.roles().contains("ADMINISTRADOR"))) {
            Integer n = db.jdbc().queryForObject("SELECT COUNT(*) FROM usuario u JOIN usuario_rol ur ON ur.tenant_id=u.tenant_id AND ur.usuario_id=u.usuario_id JOIN rol r ON r.tenant_id=ur.tenant_id AND r.rol_id=ur.rol_id WHERE u.tenant_id=? AND u.activo=1 AND r.nombre='ADMINISTRADOR'", Integer.class, a.tenantId());
            if (n == null || n <= 1) throw new ApiError(409, "ULTIMO_ADMINISTRADOR", "Debe conservarse un administrador activo del negocio");
        }
        db.cambiar("UPDATE usuario SET activo=? WHERE tenant_id=? AND usuario_id=?", r.activo(), a.tenantId(), id);
        db.cambiar("DELETE FROM usuario_rol WHERE tenant_id=? AND usuario_id=?", a.tenantId(), id);
        asignar(a.tenantId(), id, r.roles());
        return Map.of("usuarioId", id, "activo", r.activo(), "roles", r.roles());
    }
    private void bloquearTenant(Actor a) { db.requerido("SELECT tenant_id FROM tenant WHERE tenant_id=? FOR UPDATE", a.tenantId()); }
    private void asignar(String t, String u, Set<String> roles) {
        for (String nombre : roles.stream().sorted().toList()) {
            var rol = db.requerido("SELECT rol_id FROM rol WHERE tenant_id=? AND nombre=?", t, nombre);
            db.cambiar("INSERT INTO usuario_rol(tenant_id,usuario_id,rol_id) VALUES(?,?,?)", t, u, rol.get("rol_id"));
        }
    }
    private static void validarRoles(Set<String> roles) {
        if (roles == null || roles.isEmpty() || roles.size() > ROLES.size() || !ROLES.containsAll(roles)) throw V.error("roles", "al menos un rol reconocido requerido");
    }
    private static boolean activo(Object o) { return o instanceof Boolean b ? b : o instanceof Number n && n.intValue() == 1; }
    private ApiError denegado() { return new ApiError(401, "AUTENTICACION_FALLIDA", "Error en la autenticación"); }
}
