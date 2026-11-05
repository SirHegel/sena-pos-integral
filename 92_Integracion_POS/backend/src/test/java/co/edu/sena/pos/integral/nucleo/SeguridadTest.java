package co.edu.sena.pos.integral.nucleo;

import co.edu.sena.pos.integral.PruebaBase;
import co.edu.sena.pos.integral.identidad.IdentidadService;
import co.edu.sena.pos.integral.operacion.OperacionController;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.*;
import org.springframework.web.method.HandlerMethod;
import static org.junit.jupiter.api.Assertions.*;

class SeguridadTest extends PruebaBase {
    static class IdentidadDoble extends IdentidadService {
        Actor vigente=ADMIN;boolean revocada;int consultas;
        IdentidadDoble() { super(new DbDoble(),new CodificadorDoble(),Clock.systemUTC()); }
        @Override public Actor actor(String t,String u) { consultas++;if(revocada)throw new ApiError(401,"SESION_NO_VALIDA","Revocada");assertEquals(T,t);assertEquals(U,u);return vigente; }
    }
    static class Rutas {
        @Permitidos({"ADMINISTRADOR","NOMINA"}) public void protegida() {}
        public void sinPermiso() {}
    }
    IdentidadDoble identidad;Sesiones filtro;
    @BeforeEach void preparar() { identidad=new IdentidadDoble();filtro=new Sesiones(identidad); }
    HandlerMethod ruta(String metodo) throws Exception { return new HandlerMethod(new Rutas(),Rutas.class.getMethod(metodo)); }
    MockHttpServletRequest request(String metodo,String uri,boolean login) {
        var r=new MockHttpServletRequest(metodo,uri);
        if(login) { r.getSession().setAttribute(Sesiones.TENANT,T);r.getSession().setAttribute(Sesiones.USUARIO,U); }
        return r;
    }
    @Test void csrfSeGeneraUnaVezPorSesionCon32BytesAleatorios() {
        var s1=new MockHttpSession();var s2=new MockHttpSession();String a=Sesiones.token(s1);
        assertEquals(32,Base64.getUrlDecoder().decode(a).length);assertEquals(a,Sesiones.token(s1));assertNotEquals(a,Sesiones.token(s2));
    }
    @Test void lecturaProtegidaSinSesionNoObtieneActor() throws Exception {
        var r=request("GET","/api/v1/empleados",false);var h=ruta("protegida");error(401,()->filtro.preHandle(r,new MockHttpServletResponse(),h));assertEquals(0,identidad.consultas);
    }
    @Test void ingresoPublicoTambienExigeCsrf() throws Exception {
        var r=request("POST","/api/v1/auth/ingreso",false);var h=ruta("sinPermiso");error(403,()->filtro.preHandle(r,new MockHttpServletResponse(),h));
        r.addHeader("X-CSRF-Token",Sesiones.token(r.getSession()));assertTrue(filtro.preHandle(r,new MockHttpServletResponse(),h));assertEquals(0,identidad.consultas);
    }
    @Test void csrfIncorrectoRechazaAntesDeConsultarRoles() throws Exception {
        var r=request("POST","/api/v1/empleados",true);Sesiones.token(r.getSession());r.addHeader("X-CSRF-Token","otro");var h=ruta("protegida");error(403,()->filtro.preHandle(r,new MockHttpServletResponse(),h));assertEquals(0,identidad.consultas);
    }
    @Test void rolSeRecargaEnCadaPeticionYNoSeAceptaDelCuerpo() throws Exception {
        var r=request("GET","/api/v1/empleados",true);var h=ruta("protegida");assertTrue(filtro.preHandle(r,new MockHttpServletResponse(),h));assertEquals(ADMIN,r.getAttribute(Sesiones.ACTOR));
        identidad.vigente=CAJERO;r.addParameter("roles","ADMINISTRADOR");error(403,()->filtro.preHandle(r,new MockHttpServletResponse(),h));assertEquals(2,identidad.consultas);
    }
    @Test void revocacionInvalidaLaSesion() throws Exception {
        var r=request("GET","/api/v1/empleados",true);var s=(MockHttpSession)r.getSession();identidad.revocada=true;var h=ruta("protegida");error(401,()->filtro.preHandle(r,new MockHttpServletResponse(),h));assertTrue(s.isInvalid());
    }
    @Test void rutaSinPermisoDeclaradoFallaCerrada() throws Exception {
        var r=request("GET","/api/v1/nueva",true);var h=ruta("sinPermiso");assertEquals("PERMISO_NO_DEFINIDO",error(403,()->filtro.preHandle(r,new MockHttpServletResponse(),h)).codigo());
    }
    @Test void tokenCorrectoPermiteEscrituraConRolVigente() throws Exception {
        var r=request("POST","/api/v1/empleados",true);r.addHeader("X-CSRF-Token",Sesiones.token(r.getSession()));var response=new MockHttpServletResponse();assertTrue(filtro.preHandle(r,response,ruta("protegida")));assertEquals("no-store",response.getHeader("Cache-Control"));assertEquals("DENY",response.getHeader("X-Frame-Options"));
    }
    @Test void movimientoDeInventarioNoEstaPermitidoACajero() throws Exception {
        var m=OperacionController.class.getMethod("mover",Actor.class,Map.class);assertEquals(Set.of("ADMINISTRADOR","INVENTARIO"),Set.of(m.getAnnotation(Permitidos.class).value()));
    }
    @Test void actorCopiaRolesYConservaSuInmutabilidad() {
        var entrada=new HashSet<>(Set.of("CAJERO"));var actor=new Actor(T,U,entrada);entrada.add("ADMINISTRADOR");assertFalse(actor.tiene("ADMINISTRADOR"));assertThrows(UnsupportedOperationException.class,()->actor.roles().add("NOMINA"));
    }
    @ParameterizedTest @ValueSource(strings={"2026-02-29","2026-13-01","03/11/2026","2026-1-01"}) void fechasInvalidasNoSeNormalizan(String dato) { error(400,()->V.fecha(dato,"fecha")); }
    @ParameterizedTest @ValueSource(strings={"0","-0.01","1.001","1000000000000.00","NaN"}) void dineroRechazaCeroNegativoEscalaOPrecision(String dato) { error(400,()->V.decimal(dato,"precio",2,bd("0"),true)); }
    @Test void cantidadYPeriodoConservanLimites() { assertEquals(1,V.entero("1","cantidad",1,100));error(400,()->V.entero("1.1","cantidad",1,100));assertEquals(YearMonth.of(2026,11),V.periodo("2026-11","periodo"));error(400,()->V.periodo("2026-00","periodo")); }
}
