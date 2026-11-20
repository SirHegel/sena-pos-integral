package co.edu.sena.perfumeria.web;
import co.edu.sena.perfumeria.nucleo.*;
import co.edu.sena.perfumeria.servicio.*;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestController @RequestMapping("/api/v2") public class ApiController {
    private final Db db;
    private final AuthService auth;
    private final CatalogoService cat;
    private final InventarioService inv;
    private final VentasService ventas;
    private final CajaService caja;
    private final ReportesService rep;
    private final OperacionVentaService operacion;
    public ApiController(Db d,AuthService a,CatalogoService c,InventarioService i,VentasService v,CajaService j,ReportesService r,OperacionVentaService op) {
        operacion=op;
        db=d;
        auth=a;
        cat=c;
        inv=i;
        ventas=v;
        caja=j;
        rep=r;
    }
    @GetMapping("/inicio")public Object inicio(Actor a) {
        return rep.inicio(a);
    }
    @GetMapping("/configuracion")public Object config(Actor a) {
        return Map.of("unidades",db.lista("SELECT * FROM unidad ORDER BY unidad_codigo"),"medios",CajaService.MEDIOS,"cajas",db.lista("SELECT * FROM caja WHERE activa=true"));
    }
    @GetMapping("/usuarios")public Object users(Actor a) {
        return auth.usuarios(a);
    }
    @PostMapping("/usuarios")public Object user(Actor a,@RequestBody Map<String,Object>m) {
        return auth.crear(a,m);
    }
    @GetMapping( {
        "/clientes","/proveedores","/fragancias"
    }
    )public Object simples(Actor a,jakarta.servlet.http.HttpServletRequest r) {
        return cat.listar(a,r.getRequestURI().substring(8));
    }
    @PostMapping( {
        "/clientes","/proveedores","/fragancias"
    }
    )public Object simple(Actor a,jakarta.servlet.http.HttpServletRequest r,@RequestBody Map<String,Object> m) {
        return cat.crear(a,r.getRequestURI().substring(8),m);
    }
    @GetMapping("/articulos")public Object arts(Actor a) {
        return inv.articulos(a);
    }
    @PostMapping("/articulos")public Object art(Actor a,@RequestBody Map<String,Object> m) {
        return cat.articulo(a,m);
    }
    @PutMapping("/articulos/{id}/minimo")public Object min(Actor a,@PathVariable String id,@RequestBody Map<String,Object> m) {
        return cat.minimo(a,id,m);
    }
    @GetMapping("/movimientos")public Object mov(Actor a,@RequestParam(required=false)String articulo_id) {
        return inv.movimientos(a,articulo_id);
    }
    @GetMapping("/formulas")public Object forms(Actor a) {
        return cat.formulas(a);
    }
    @PostMapping("/formulas")public Object form(Actor a,@RequestBody Map<String,Object> m) {
        return cat.crearFormula(a,m);
    }
    @GetMapping("/ofertas")public Object offers(Actor a) {
        return cat.ofertas(a);
    }
    @PostMapping("/ofertas")public Object offer(Actor a,@RequestBody Map<String,Object> m) {
        return cat.oferta(a,m);
    }
    @PutMapping("/ofertas/{id}/precio")public Object price(Actor a,@PathVariable String id,@RequestBody Map<String,Object> m) {
        return cat.precio(a,id,m);
    }
    @PutMapping("/ofertas/{id}/suministro")public Object supply(Actor a,@PathVariable String id,@RequestBody Map<String,Object> m) {
        return cat.suministro(a,id,m);
    }
    @GetMapping("/recepciones")public Object receipts(Actor a) {
        return inv.recepciones(a);
    }
    @GetMapping("/recepciones/{id}")public Object receipt(Actor a,@PathVariable String id) {
        return inv.detalleRecepcion(a,id);
    }
    @PostMapping("/recepciones")public Object receive(Actor a,@RequestBody Map<String,Object> m) {
        return inv.recepcion(a,m);
    }
    @PostMapping("/aperturas")public Object open(Actor a,@RequestBody Map<String,Object> m) {
        return inv.apertura(a,m);
    }
    @GetMapping("/ventas")public Object sales(Actor a) {
        return ventas.listar(a);
    }
    @PostMapping("/ventas")public Object sale(Actor a,@RequestBody Map<String,Object> m) {
        return ventas.crear(a,m);
    }
    @GetMapping( {
        "/ventas/{id}","/ventas/{id}/comprobante"
    }
    )public Object detail(Actor a,@PathVariable String id) {
        return ventas.detalle(a,id);
    }
    @PostMapping("/ventas/{id}/descuento")public Object discount(Actor a,@PathVariable String id,@RequestBody Map<String,Object> m) {
        return ventas.descuento(a,id,m);
    }
    @PostMapping("/ventas/{id}/emitir")public Object issue(Actor a,@PathVariable String id,@RequestBody Map<String,Object> m) {
        return operacion.emitir(a,id,m);
    }
    @PostMapping("/ventas/{id}/anular")public Object cancel(Actor a,@PathVariable String id) {
        return ventas.anular(a,id);
    }
    @PostMapping("/ventas/{id}/cobros")public Object collect(Actor a,@PathVariable String id,@RequestBody Map<String,Object> m) {
        return caja.cobrar(a,id,m);
    }
    @GetMapping("/cartera")public Object ledger(Actor a) {
        return ventas.cartera(a);
    }
    @GetMapping("/turnos")public Object shifts(Actor a) {
        return caja.turnos(a);
    }
    @PostMapping("/turnos")public Object shift(Actor a,@RequestBody Map<String,Object> m) {
        return caja.abrir(a,m);
    }
    @GetMapping("/turnos/{id}")public Object shiftd(Actor a,@PathVariable String id) {
        return caja.detalle(a,id);
    }
    @PostMapping("/turnos/{id}/cerrar")public Object close(Actor a,@PathVariable String id,@RequestBody Map<String,Object> m) {
        return caja.cerrar(a,id,m);
    }
    @GetMapping("/incidencias")public Object incidents(Actor a) {
        return caja.incidencias(a);
    }
    @PostMapping("/incidencias")public Object incident(Actor a,@RequestBody Map<String,Object> m) {
        return caja.incidencia(a,m);
    }
    @GetMapping("/gastos")public Object expenses(Actor a) {
        return caja.gastos(a);
    }
    @PostMapping("/gastos")public Object expense(Actor a,@RequestBody Map<String,Object> m) {
        return caja.gasto(a,m);
    }
    @GetMapping("/reportes")public Object reports(Actor a,@RequestParam(defaultValue="ventas")String tipo,@RequestParam(required=false)String desde,@RequestParam(required=false)String hasta) {
        return rep.reporte(a,tipo,desde,hasta);
    }
    @GetMapping("/exportacion.xlsx")public ResponseEntity<byte[]> excel(Actor a,@RequestParam String mes) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).header("Content-Disposition","attachment; filename=\"ventas-gastos-"+V.periodo(mes,"mes")+".xlsx\"").body(rep.excel(a,mes));
    }
    @GetMapping("/conteos")public Object counts(Actor a) {
        return inv.conteos(a);
    }
    @PostMapping("/conteos")public Object count(Actor a,@RequestBody Map<String,Object>m) {
        return inv.conteo(a,m);
    }
}
