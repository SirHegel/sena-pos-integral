package co.edu.sena.perfumeria.dominio;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import co.edu.sena.perfumeria.nucleo.*;
import co.edu.sena.perfumeria.servicio.AuthService;
class AccesoRegresionTest {
    @Test void activoTinyintMySqlNoSeConfundeConBoolean() {
        Db db=mock(Db.class);
        String pass="SoloLaboratorio123";
        when(db.uno("SELECT usuario_id,hash_clave,activo FROM usuario WHERE nombre_acceso=?","demo")).thenReturn(Optional.of(Map.of("usuario_id","u","hash_clave",new BCryptPasswordEncoder(12).encode(pass),"activo",1)));
        when(db.requerido("SELECT u.usuario_id,u.perfil,i.nombre_visible FROM usuario u JOIN integrante_equipo i ON i.integrante_id=u.integrante_id WHERE u.usuario_id=? AND u.activo=true AND i.activo=true","u")).thenReturn(Map.of("usuario_id","u","perfil","VENDEDORA","nombre_visible","DEMO"));
        assertEquals("u",new AuthService(db).login(Map.of("nombre_acceso","demo","clave",pass)).usuarioId());
    }
}
