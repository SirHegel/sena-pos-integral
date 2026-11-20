package co.edu.sena.perfumeria.servicio;

import co.edu.sena.perfumeria.dominio.Articulo;
import co.edu.sena.perfumeria.dominio.Unidad;
import co.edu.sena.perfumeria.nucleo.ApiError;
import co.edu.sena.perfumeria.nucleo.Db;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static co.edu.sena.perfumeria.servicio.DatosPrueba.*;

class InventarioServiceTest {
    @Test void unaFraccionDeEnvaseNoProduceMovimiento() {
        Db db=mock(Db.class);
        var envase=new Articulo(ARTICULO,"F","Frasco DEMO",Unidad.UNIDAD,"ENVASE");
        assertThrows(ApiError.class,()->new InventarioService(db).renglon("doc",1,envase,n("0.5"),n("1000")));
        verifyNoInteractions(db);
    }

    @Test void reintentoIdenticoRecuperaDocumentoPeroOtroContenidoSeRechaza() {
        Db db=mock(Db.class);
        when(db.uno("SELECT documento_id,huella_solicitud FROM documento_inventario WHERE clave_operacion=?","clave-demo"))
            .thenReturn(Optional.of(Map.of("documento_id","documento-demo","huella_solicitud","huella-original")));
        var service=new InventarioService(db);
        assertEquals(Optional.of("documento-demo"),service.repetida("documento_inventario","documento_id","clave-demo","huella-original"));
        assertThrows(ApiError.class,()->service.repetida("documento_inventario","documento_id","clave-demo","otro-contenido"));
        verify(db,never()).cambiar(anyString(),any(Object[].class));
    }

    @Test void aperturaTransfiereUnidadYValorAlVolumenSinCompraAdicional() {
        Db db=mock(Db.class);
        when(db.requerido("SELECT * FROM articulo WHERE articulo_id=? AND activo=true",ARTICULO))
            .thenReturn(fila("codigo","SELLADO","nombre","Original DEMO","unidad_base","UNIDAD","clase","PERFUME_SELLADO","funcion_material","PERFUME"));
        when(db.requerido("SELECT * FROM articulo WHERE articulo_id=? AND activo=true",ABIERTO))
            .thenReturn(fila("codigo","ABIERTO","nombre","Original abierto DEMO","unidad_base","ML","clase","PERFUME_ABIERTO","funcion_material","PERFUME"));
        when(db.requerido("SELECT contenido_nominal_ml FROM articulo WHERE articulo_id=?",ARTICULO)).thenReturn(Map.of("contenido_nominal_ml",n("100")));
        when(db.requerido("SELECT COUNT(*) n FROM v_movimiento_base WHERE articulo_id=? AND costo_unitario_base_cop IS NULL",ARTICULO)).thenReturn(Map.of("n",0));
        when(db.requerido("SELECT COALESCE(SUM(cantidad_base),0)saldo,COALESCE(SUM(cantidad_base*costo_unitario_base_cop),0)valor FROM v_movimiento_base WHERE articulo_id=?",ARTICULO))
            .thenReturn(Map.of("saldo",n("2"),"valor",n("60000")));
        var result=new InventarioService(db).apertura(ADMIN,Map.of("clave_operacion","apertura-demo",
            "articulo_sellado_id",ARTICULO,"articulo_abierto_id",ABIERTO,"unidades",1,"volumen_util_ml","100","motivo","Fraccionamiento DEMO"));
        var doc=ArgumentCaptor.forClass(String.class);
        verify(db).cambiar(eq("INSERT INTO renglon_inventario VALUES (?,?,?,?,?,?)"),doc.capture(),eq(1),eq(ARTICULO),decimalIgual("-1"),eq("UNIDAD"),decimalIgual("30000"));
        verify(db).cambiar(eq("INSERT INTO renglon_inventario VALUES (?,?,?,?,?,?)"),eq(doc.getValue()),eq(2),eq(ABIERTO),decimalIgual("100"),eq("ML"),decimalIgual("300"));
        assertEquals(doc.getValue(),result.get("id"));
        verify(db,times(3)).cambiar(anyString(),any(Object[].class));
    }
}
