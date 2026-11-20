package co.edu.sena.perfumeria.catalogo;
import java.sql.*;
/** Conexiones JDBC por operación; no conserva conexiones compartidas entre peticiones. */
public class ConnectionFactory {
    public Connection open() throws SQLException {
        return DriverManager.getConnection(required("DB_URL"),required("DB_USER"),required("DB_PASSWORD"));
    }
    private String required(String name) {
        String value=System.getenv(name);
        if(value==null || (!name.equals("DB_PASSWORD") && value.isBlank())) throw new IllegalStateException("Defina "+name);
        return value;
    }
}
