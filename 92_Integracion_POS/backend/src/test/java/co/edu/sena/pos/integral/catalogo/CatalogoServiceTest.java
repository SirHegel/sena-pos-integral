package co.edu.sena.pos.integral.catalogo;

import co.edu.sena.pos.integral.PruebaBase;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class CatalogoServiceTest extends PruebaBase {
    DbDoble db;CatalogoService service;
    @BeforeEach void preparar() { db=new DbDoble();service=new CatalogoService(db); }
    CatalogoService.Producto producto(String precio,String iva,String tipo,String modo) { return new CatalogoService.Producto(" P92 "," Producto de prueba ",bd(precio),bd(iva),tipo,"unidad",modo,true); }
    @Test void altaGuardaNegocioDelActorYTextoNormalizado() {
        db.responder=c->filas(fila("producto_id",c.arg(1)));service.crear(ADMIN,producto("10.00","0.1900","VENDIBLE","DIRECTO"));var c=db.escrituras.getFirst();assertEquals(T,c.arg(0));assertEquals("P92",c.arg(2));assertEquals("Producto de prueba",c.arg(3));
    }
    @Test void precioCeroEsRechazadoAntesDePersistir() { error(400,()->service.crear(ADMIN,producto("0.00","0.0000","VENDIBLE","DIRECTO")));db.sinAcceso(); }
    @Test void ivaMayorAUnoEsRechazado() { error(400,()->service.crear(ADMIN,producto("10.00","1.0001","VENDIBLE","DIRECTO")));db.sinAcceso(); }
    @Test void recetaSoloAdmiteTipoPlato() { error(400,()->service.crear(ADMIN,producto("10.00","0.0000","VENDIBLE","RECETA")));db.sinAcceso(); }
    @Test void estructuraDeProductoConHistorialNoSeReescribe() {
        db.responder=c->c.contiene("SELECT 1")?filas(fila("1",1)):filas(fila("tipo","VENDIBLE","modo_descuento","DIRECTO","unidad","unidad"));
        error(409,()->service.cambiar(ADMIN,P,producto("10.00","0.1900","INSUMO","DIRECTO")));db.sinEscrituras();
    }
    @Test void busquedaSeEntregaComoParametroYNoComoSql() {
        String q="' OR 1=1 --";service.lista(ADMIN,q);var c=db.lecturas.getFirst();assertFalse(c.sql().contains(q));assertEquals(T,c.arg(0));assertEquals(q,c.arg(1));assertEquals(q,c.arg(2));
    }
}
