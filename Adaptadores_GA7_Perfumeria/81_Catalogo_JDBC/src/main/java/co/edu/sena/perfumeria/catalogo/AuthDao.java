package co.edu.sena.perfumeria.catalogo;
import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
/** Reutiliza usuarios y hashes BCrypt del esquema del proyecto; nunca recibe un perfil del cliente. */
public class AuthDao {
    private final ConnectionFactory factory;
    private final BCryptPasswordEncoder bcrypt=new BCryptPasswordEncoder();
    private final Map<String,long[]> intentos=new ConcurrentHashMap<>();
    public AuthDao(ConnectionFactory f) {
        factory=f;
    }
    public Actor autenticar(String nombre,String clave)throws SQLException {
        nombre=Articulo.texto(nombre,"Usuario").toLowerCase(Locale.ROOT);
        if(clave==null||clave.isEmpty()||clave.getBytes(StandardCharsets.UTF_8).length>72)throw new SecurityException("Credenciales inválidas");
        if(intentos.size()>1000)throw new SecurityException("Reinicie el acceso después de un momento");
        var t=intentos.computeIfAbsent(nombre,k->new long[2]);
        synchronized(t) {
            if(t[1]>System.currentTimeMillis())throw new SecurityException("Acceso temporalmente bloqueado");
            try(Connection c=factory.open();PreparedStatement q=c.prepareStatement("SELECT u.usuario_id,u.hash_clave,u.perfil,i.nombre_visible FROM usuario u JOIN integrante_equipo i ON i.integrante_id=u.integrante_id WHERE u.nombre_acceso=? AND u.activo=1 AND i.activo=1")) {
                q.setString(1,nombre);
                try(ResultSet r=q.executeQuery()) {
                    if(r.next()&&bcrypt.matches(clave,r.getString("hash_clave"))) {
                        t[0]=0;
                        t[1]=0;
                        return new Actor(r.getString("usuario_id"),r.getString("nombre_visible"),r.getString("perfil"));
                    }
                }
            }
            if(++t[0]>=3) {
                t[0]=0;
                t[1]=System.currentTimeMillis()+30000;
            }
            throw new SecurityException("Credenciales inválidas");
        }
    }
    public Optional<Actor> actor(String id)throws SQLException {
        try(Connection c=factory.open();PreparedStatement q=c.prepareStatement("SELECT u.usuario_id,u.perfil,i.nombre_visible FROM usuario u JOIN integrante_equipo i ON i.integrante_id=u.integrante_id WHERE u.usuario_id=? AND u.activo=1 AND i.activo=1")) {
            q.setString(1,id);
            try(ResultSet r=q.executeQuery()) {
                return r.next()?Optional.of(new Actor(id,r.getString("nombre_visible"),r.getString("perfil"))):Optional.empty();
            }
        }
    }
}
