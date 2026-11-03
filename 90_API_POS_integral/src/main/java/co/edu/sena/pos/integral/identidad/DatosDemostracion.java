package co.edu.sena.pos.integral.identidad;

import co.edu.sena.pos.integral.nucleo.Db;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Inicialización opcional y explícita de datos sintéticos en una base de laboratorio. */
@Component
@ConditionalOnProperty(name="pos.demo.enabled",havingValue="true")
public class DatosDemostracion implements ApplicationRunner {
    private final Db db; private final PasswordEncoder passwords; private final String password;
    public DatosDemostracion(Db db,PasswordEncoder passwords,@Value("${pos.demo.password}") String password){this.db=db;this.passwords=passwords;this.password=password;}
    @Override @Transactional public void run(ApplicationArguments args){
        IdentidadService.clave(password);
        String[] roles={"ADMINISTRADOR","CAJERO","INVENTARIO","NOMINA","CONTABILIDAD"};
        for(int n=1;n<=2;n++){
            String t="90000000-0000-4000-8000-00000000000"+n;
            if(!db.existe("SELECT 1 FROM tenant WHERE tenant_id=?",t))
                db.cambiar("INSERT INTO tenant(tenant_id,codigo,nombre,zona_horaria,moneda) VALUES(?,?,?,'America/Bogota','COP')",t,"DEMO"+n,"Negocio sintético "+n);
            for(String rol:roles){
                String rid=id(t+":"+rol),uid=id(t+":usuario:"+rol),email=rol.toLowerCase(java.util.Locale.ROOT)+"@demo.example";
                if(!db.existe("SELECT 1 FROM rol WHERE tenant_id=? AND rol_id=?",t,rid))
                    db.cambiar("INSERT INTO rol(tenant_id,rol_id,nombre) VALUES(?,?,?)",t,rid,rol);
                if(!db.existe("SELECT 1 FROM usuario WHERE tenant_id=? AND usuario_id=?",t,uid))
                    db.cambiar("INSERT INTO usuario(tenant_id,usuario_id,correo,hash_password) VALUES(?,?,?,?)",t,uid,email,passwords.encode(password));
                if(!db.existe("SELECT 1 FROM usuario_rol WHERE tenant_id=? AND usuario_id=? AND rol_id=?",t,uid,rid))
                    db.cambiar("INSERT INTO usuario_rol(tenant_id,usuario_id,rol_id) VALUES(?,?,?)",t,uid,rid);
            }
        }
    }
    private static String id(String text){return UUID.nameUUIDFromBytes(text.getBytes(StandardCharsets.UTF_8)).toString();}
}
