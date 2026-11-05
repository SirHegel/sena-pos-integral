package co.edu.sena.pos.integral.nucleo;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Db {
    private final JdbcTemplate jdbc;
    public Db(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public JdbcTemplate jdbc() { return jdbc; }
    public List<Map<String, Object>> lista(String sql, Object... args) { return jdbc.queryForList(sql, args); }
    public Optional<Map<String, Object>> uno(String sql, Object... args) {
        var rows = lista(sql, args);
        if (rows.size() > 1) throw new IllegalStateException("Se esperaba una sola fila");
        return rows.stream().findFirst();
    }
    public Map<String, Object> requerido(String sql, Object... args) {
        return uno(sql, args).orElseThrow(() -> new ApiError(404, "NO_ENCONTRADO", "No se encontró el recurso en el negocio actual"));
    }
    public int cambiar(String sql, Object... args) { return jdbc.update(sql, args); }
    public boolean existe(String sql, Object... args) { return !lista(sql, args).isEmpty(); }
}
