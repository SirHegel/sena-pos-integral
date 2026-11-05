package co.edu.sena.pos.integral.identidad;

import co.edu.sena.pos.integral.PruebaBase;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class IdentidadServiceTest extends PruebaBase {
    final Instant ahora=Instant.parse("2026-11-03T15:00:00Z");
    DbDoble db;CodificadorDoble cod;IdentidadService service;Map<String,Object> usuario;
    @BeforeEach void preparar() {
        db=new DbDoble();cod=new CodificadorDoble();service=new IdentidadService(db,cod,Clock.fixed(ahora,ZoneOffset.UTC));
        usuario=fila("usuario_id",U,"hash_password",cod.encode("ClavePrueba92"),"activo",true,"negocio_activo",1,"intentos_fallidos",0,"bloqueado_hasta",null);
        db.responder=c->{if(c.contiene("u.hash_password"))return filas(usuario);if(c.contiene("SELECT 1 FROM usuario"))return filas(fila("1",1));if(c.contiene("r.nombre"))return filas(fila("nombre","CAJERO"));throw new AssertionError(c.sql());};
    }
    IdentidadService.Ingreso ingreso(String password) { return new IdentidadService.Ingreso(T,"  PRUEBA@DEMO.EXAMPLE  ",password); }
    @Test void ingresoValidoNormalizaCorreoYDerivaRolesDePersistencia() {
        var a=service.ingresar(ingreso("ClavePrueba92"));assertEquals(Set.of("CAJERO"),a.roles());assertEquals(T,a.tenantId());assertEquals("prueba@demo.example",db.lecturas.getFirst().arg(1));assertEquals(1,db.escritas("intentos_fallidos=0"));
    }
    @Test void bloqueoLocalDateTimeNoFallaYRechazaInclusoClaveCorrecta() {
        usuario.put("bloqueado_hasta",LocalDateTime.ofInstant(ahora.plusSeconds(20),ZoneOffset.UTC));error(429,()->service.ingresar(ingreso("ClavePrueba92")));db.sinEscrituras();assertEquals(1,cod.comparaciones);
    }
    @Test void bloqueoTimestampTambienEsReconocido() {
        usuario.put("bloqueado_hasta",Timestamp.from(ahora.plusSeconds(20)));error(429,()->service.ingresar(ingreso("ClavePrueba92")));db.sinEscrituras();
    }
    @Test void tercerIntentoRegistraBloqueoDe30Segundos() {
        usuario.put("intentos_fallidos",2);error(429,()->service.ingresar(ingreso("OtraClave92")));var c=db.escrituras.getFirst();assertEquals(3,c.arg(0));assertEquals(ahora.plusSeconds(30),((Timestamp)c.arg(1)).toInstant());assertEquals(T,c.arg(2));assertEquals(U,c.arg(3));
    }
    @Test void bloqueoVencidoPermiteAutenticarYReiniciaContador() {
        usuario.put("intentos_fallidos",3);usuario.put("bloqueado_hasta",LocalDateTime.ofInstant(ahora.minusSeconds(1),ZoneOffset.UTC));assertEquals(U,service.ingresar(ingreso("ClavePrueba92")).usuarioId());assertEquals(1,db.escritas("intentos_fallidos=0"));
    }
    @Test void usuarioInexistenteComparaHashSenueloSinEscrituras() {
        db.responder=c->List.of();assertEquals("AUTENTICACION_FALLIDA",error(401,()->service.ingresar(ingreso("ClavePrueba92"))).codigo());assertEquals(1,cod.comparaciones);db.sinEscrituras();
    }
    @Test void correoConPuntosConsecutivosYClaveDeMasDe72BytesSonRechazados() {
        error(400,()->IdentidadService.correo("a..b@example.test"));error(400,()->IdentidadService.clave("á".repeat(40)));db.sinAcceso();
    }
}
