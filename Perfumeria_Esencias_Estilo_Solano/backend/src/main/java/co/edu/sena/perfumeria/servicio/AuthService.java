package co.edu.sena.perfumeria.servicio;
import co.edu.sena.perfumeria.nucleo.*;
import static co.edu.sena.perfumeria.nucleo.Datos.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
@Service public class AuthService {
    private final Db db;
    private final BCryptPasswordEncoder encoder=new BCryptPasswordEncoder(12);
    private final Map<String,long[]> intentos=new ConcurrentHashMap<>();
    @Value("${ps.bootstrap.password:}")private String bootstrap;
    public AuthService(Db db) {
        this.db=db;
    }
    @EventListener(ApplicationReadyEvent.class) @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public void inicializar() {
        if(bootstrap!=null&&!bootstrap.isBlank()&&!db.existe("SELECT usuario_id FROM usuario LIMIT 1")) {
            var p=id();
            db.cambiar("INSERT INTO integrante_equipo VALUES (?,?,?,true)",p,"Administrador inicial","PROPIETARIO");
            db.cambiar("INSERT INTO usuario VALUES (?,?,?,?,?,true)",id(),p,"admin",encoder.encode(V.clave(bootstrap,"PS_BOOTSTRAP_PASSWORD",true)),"ADMINISTRADOR");
        }
        if(!db.existe("SELECT caja_id FROM caja LIMIT 1"))db.cambiar("INSERT INTO caja VALUES (?,?,true)",id(),"Caja mostrador");
    }
    public Actor actor(String id) {
        var u=db.requerido("SELECT u.usuario_id,u.perfil,i.nombre_visible FROM usuario u JOIN integrante_equipo i ON i.integrante_id=u.integrante_id WHERE u.usuario_id=? AND u.activo=true AND i.activo=true",id);
        return new Actor(u.get("usuario_id").toString(),u.get("nombre_visible").toString(),u.get("perfil").toString());
    }
    public Actor login(Map<String,Object> m) {
        String nombre=texto(m,"nombre_acceso"),clave=V.clave(m.get("clave"),"clave",false);
        var t=intentos.computeIfAbsent(nombre,k->new long[2]);
        synchronized(t) {
            if(t[1]>System.currentTimeMillis())throw new ApiError(429,"BLOQUEO_TEMPORAL","Acceso bloqueado temporalmente");
            var row=db.uno("SELECT usuario_id,hash_clave,activo FROM usuario WHERE nombre_acceso=?",nombre);
            if(row.isEmpty()||n(row.get().get("activo")).signum()==0||!encoder.matches(clave,row.get().get("hash_clave").toString())) {
                if(++t[0]>=3) {
                    t[1]=System.currentTimeMillis()+30000;
                    t[0]=0;
                }
                throw new ApiError(401,"CREDENCIALES_INVALIDAS","Credenciales inválidas");
            }
            t[0]=0;
            t[1]=0;
            return actor(row.get().get("usuario_id").toString());
        }
    }
    @Transactional(isolation=org.springframework.transaction.annotation.Isolation.READ_COMMITTED) public Map<String,Object> crear(Actor a,Map<String,Object> m) {
        a.exigirAdmin();
        String perfil=op(m,"perfil","ADMINISTRADOR","VENDEDORA"),clave=V.clave(m.get("clave"),"clave",true);
        if(clave.length()<10)throw V.error("clave","mínimo10caracteres");
        String i=id(),u=id();
        db.cambiar("INSERT INTO integrante_equipo VALUES (?,?,?,true)",i,texto(m,"nombre_visible"),perfil.equals("ADMINISTRADOR")?"PROPIETARIO":"COLABORADORA");
        db.cambiar("INSERT INTO usuario VALUES (?,?,?,?,?,true)",u,i,texto(m,"nombre_acceso").toLowerCase(Locale.ROOT),encoder.encode(clave),perfil);
        return Map.of("id",u);
    }
    public List<Map<String,Object>> usuarios(Actor a) {
        a.exigirAdmin();
        return db.lista("SELECT u.usuario_id,u.nombre_acceso,u.perfil,u.activo,i.nombre_visible FROM usuario u JOIN integrante_equipo i ON i.integrante_id=u.integrante_id ORDER BY i.nombre_visible");
    }
}
