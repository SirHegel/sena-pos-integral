package co.edu.sena.pos.catalogo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** DAO con JDBC explícito; cada lectura y escritura delimita el tenant_id. */
public class ProductoDao {
    private final ConnectionFactory connections;
    private static final String COLUMNS = "tenant_id,producto_id,codigo,nombre,precio_vigente,tasa_iva,tipo,unidad,modo_descuento,activo";

    public ProductoDao(ConnectionFactory connections) { this.connections = connections; }

    public void create(Producto p) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO producto (" + COLUMNS + ") VALUES (?,?,?,?,?,?,?,?,?,?)")) {
            bind(s, p);
            s.executeUpdate();
        }
    }

    public Optional<Producto> find(UUID tenant, UUID id) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM producto WHERE tenant_id=? AND producto_id=?")) {
            s.setString(1, tenant.toString()); s.setString(2, id.toString());
            try (ResultSet rs = s.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    public List<Producto> list(UUID tenant) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(
                "SELECT " + COLUMNS + " FROM producto WHERE tenant_id=? ORDER BY codigo,producto_id")) {
            s.setString(1, tenant.toString());
            try (ResultSet rs = s.executeQuery()) {
                List<Producto> result = new ArrayList<>();
                while (rs.next()) { result.add(map(rs)); }
                return result;
            }
        }
    }

    public boolean update(Producto p) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(
                "UPDATE producto SET codigo=?,nombre=?,precio_vigente=?,tasa_iva=?,tipo=?,unidad=?,modo_descuento=?,activo=? "
                    + "WHERE tenant_id=? AND producto_id=?")) {
            s.setString(1, p.codigo()); s.setString(2, p.nombre()); s.setBigDecimal(3, p.precio());
            s.setBigDecimal(4, p.iva()); s.setString(5, p.tipo()); s.setString(6, p.unidad());
            s.setString(7, p.modoDescuento()); s.setBoolean(8, p.activo());
            s.setString(9, p.tenantId().toString()); s.setString(10, p.productoId().toString());
            return s.executeUpdate() == 1;
        }
    }

    public boolean delete(UUID tenant, UUID id) throws SQLException {
        try (Connection c = connections.open(); PreparedStatement s = c.prepareStatement(
                "DELETE FROM producto WHERE tenant_id=? AND producto_id=?")) {
            s.setString(1, tenant.toString()); s.setString(2, id.toString());
            return s.executeUpdate() == 1;
        }
    }

    private static void bind(PreparedStatement s, Producto p) throws SQLException {
        s.setString(1, p.tenantId().toString()); s.setString(2, p.productoId().toString());
        s.setString(3, p.codigo()); s.setString(4, p.nombre()); s.setBigDecimal(5, p.precio());
        s.setBigDecimal(6, p.iva()); s.setString(7, p.tipo()); s.setString(8, p.unidad());
        s.setString(9, p.modoDescuento()); s.setBoolean(10, p.activo());
    }

    private static Producto map(ResultSet r) throws SQLException {
        return new Producto(UUID.fromString(r.getString("tenant_id")), UUID.fromString(r.getString("producto_id")),
            r.getString("codigo"), r.getString("nombre"), r.getBigDecimal("precio_vigente"),
            r.getBigDecimal("tasa_iva"), r.getString("tipo"), r.getString("unidad"),
            r.getString("modo_descuento"), r.getBoolean("activo"));
    }
}
