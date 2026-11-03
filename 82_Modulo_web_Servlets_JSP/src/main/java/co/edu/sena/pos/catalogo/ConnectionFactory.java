package co.edu.sena.pos.catalogo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionFactory {
    public Connection open() throws SQLException {
        return DriverManager.getConnection(required("DB_URL"), required("DB_USER"), required("DB_PASSWORD"));
    }

    private String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Defina la variable " + name);
        }
        return value;
    }
}
