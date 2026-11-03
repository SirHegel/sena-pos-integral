package co.edu.sena.pos.auth;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AuthRepository {
    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean tenantActive(String tenant) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM tenant WHERE tenant_id=? AND activo=1)",
            Boolean.class, tenant));
    }

    public void create(String tenant, String id, String email, String hash) {
        // Valores enlazados: no se concatena entrada del cliente al SQL.
        jdbc.update("INSERT INTO usuario (tenant_id,usuario_id,correo,hash_password) VALUES (?,?,?,?)",
            tenant, id, email, hash);
    }

    public Optional<User> lockUser(String tenant, String email) {
        // La fila se bloquea dentro de la transacción para no perder fallos concurrentes.
        List<User> users = jdbc.query(
            "SELECT usuario_id,hash_password,intentos_fallidos,bloqueado_hasta,activo "
                + "FROM usuario WHERE tenant_id=? AND correo=? FOR UPDATE",
            (rs, row) -> {
                Timestamp locked = rs.getTimestamp("bloqueado_hasta");
                return new User(rs.getString("usuario_id"), rs.getString("hash_password"),
                    rs.getInt("intentos_fallidos"), locked == null ? null : locked.toInstant(),
                    rs.getBoolean("activo"));
            }, tenant, email);
        return users.stream().findFirst();
    }

    public void updateAttempts(String tenant, String user, int attempts, Instant until) {
        jdbc.update("UPDATE usuario SET intentos_fallidos=?,bloqueado_hasta=? "
                + "WHERE tenant_id=? AND usuario_id=?", attempts,
            until == null ? null : Timestamp.from(until), tenant, user);
    }

    public record User(String id, String hash, int attempts, Instant lockedUntil, boolean active) {}
}
