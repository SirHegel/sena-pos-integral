package co.edu.sena.perfumeria.catalogo;
import java.sql.*;
import java.math.BigDecimal;
import java.util.*;
/** CRUD con SQL parametrizado. La FK RESTRICT decide el borrado de un artículo referenciado. */
public class ArticuloDao {
    private final ConnectionFactory factory;
    public ArticuloDao(ConnectionFactory factory) {
        this.factory=factory;
    }
    public List<Articulo> listar(Actor actor)throws SQLException {
        Objects.requireNonNull(actor);
        var result=new ArrayList<Articulo>();
        try(Connection c=factory.open();PreparedStatement q=c.prepareStatement("SELECT * FROM articulo ORDER BY codigo");ResultSet r=q.executeQuery()) {
            while(r.next())result.add(row(r));
        }
        return result;
    }
    public Optional<Articulo> buscar(Actor actor,String id)throws SQLException {
        Objects.requireNonNull(actor);
        Articulo.uuid(id);
        try(Connection c=factory.open();PreparedStatement q=c.prepareStatement("SELECT * FROM articulo WHERE articulo_id=?")) {
            q.setString(1,id);
            try(ResultSet r=q.executeQuery()) {
                return r.next()?Optional.of(row(r)):Optional.empty();
            }
        }
    }
    public void crear(Actor actor,Articulo a)throws SQLException {
        actor.exigirAdmin();
        try(Connection c=factory.open();PreparedStatement q=c.prepareStatement("INSERT INTO articulo(articulo_id,codigo,nombre,unidad_base,clase,contenido_nominal_ml,minimo_base,activo,funcion_material) VALUES(?,?,?,?,?,?,?,?,?)")) {
            q.setString(1,a.getId());
            q.setString(2,a.getCodigo());
            q.setString(3,a.getNombre());
            q.setString(4,a.getUnidad());
            q.setString(5,a.getClase());
            q.setBigDecimal(6,a.getNominal());
            q.setBigDecimal(7,a.getMinimo());
            q.setBoolean(8,a.isActivo());
            q.setString(9,a.getFuncion());
            q.executeUpdate();
        }
    }
    public boolean actualizar(Actor actor,String id,String nombre,BigDecimal minimo)throws SQLException {
        actor.exigirAdmin();
        Articulo.uuid(id);
        nombre=Articulo.texto(nombre,"Nombre");
        Articulo.decimal(minimo,false);
        // La sentencia sólo modifica nombre y mínimo; conserva unidad, capacidad y clasificación históricas.
        try(Connection c=factory.open();PreparedStatement q=c.prepareStatement("UPDATE articulo SET nombre=?,minimo_base=? WHERE articulo_id=?")) {
            q.setString(1,nombre);
            q.setBigDecimal(2,minimo);
            q.setString(3,id);
            return q.executeUpdate()==1;
        }
    }
    public boolean eliminar(Actor actor,String id)throws SQLException {
        actor.exigirAdmin();
        Articulo.uuid(id);
        try(Connection c=factory.open();PreparedStatement q=c.prepareStatement("DELETE FROM articulo WHERE articulo_id=?")) {
            q.setString(1,id);
            return q.executeUpdate()==1;
        }
    }
    private Articulo row(ResultSet r)throws SQLException {
        return new Articulo(r.getString("articulo_id"),r.getString("codigo"),r.getString("nombre"),r.getString("unidad_base"),r.getString("clase"),r.getString("funcion_material"),r.getBigDecimal("contenido_nominal_ml"),r.getBigDecimal("minimo_base"),r.getBoolean("activo"));
    }
}
