package co.edu.sena.pos.integral.operacion;

import co.edu.sena.pos.integral.nucleo.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class OperacionController {
    private final OperacionService service;
    public OperacionController(OperacionService service){this.service=service;}
    @GetMapping("/bodegas") @Permitidos({"ADMINISTRADOR","CAJERO","INVENTARIO"})
    public Object bodegas(Actor a){return service.bodegas(a);}
    @PostMapping("/bodegas") @ResponseStatus(HttpStatus.CREATED) @Permitidos({"ADMINISTRADOR","INVENTARIO"})
    public Object crearBodega(Actor a,@RequestBody Map<String,Object>b){return service.crearBodega(a,b);}
    @GetMapping("/existencias") @Permitidos({"ADMINISTRADOR","CAJERO","INVENTARIO"})
    public Object existencias(Actor a,@RequestParam(required=false)String bodegaId){return service.existencias(a,bodegaId);}
    @GetMapping("/movimientos") @Permitidos({"ADMINISTRADOR","INVENTARIO"})
    public Object movimientos(Actor a,@RequestParam(required=false)String bodegaId){return service.movimientos(a,bodegaId);}
    @PostMapping("/movimientos") @ResponseStatus(HttpStatus.CREATED) @Permitidos({"ADMINISTRADOR","INVENTARIO"})
    public Object mover(Actor a,@RequestBody Map<String,Object>b){return service.mover(a,b);}
    @GetMapping("/mesas") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object mesas(Actor a){return service.mesas(a);}
    @PostMapping("/mesas") @ResponseStatus(HttpStatus.CREATED) @Permitidos({"ADMINISTRADOR"})
    public Object crearMesa(Actor a,@RequestBody Map<String,Object>b){return service.crearMesa(a,b);}
    @PatchMapping("/mesas/{id}/estado") @Permitidos({"ADMINISTRADOR"})
    public Object estadoMesa(Actor a,@PathVariable String id,@RequestBody Map<String,Object>b){return service.estadoMesa(a,id,b);}
    @GetMapping("/cajas") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object cajas(Actor a){return service.cajas(a);}
    @PostMapping("/cajas") @ResponseStatus(HttpStatus.CREATED) @Permitidos({"ADMINISTRADOR"})
    public Object crearCaja(Actor a,@RequestBody Map<String,Object>b){return service.crearCaja(a,b);}
    @GetMapping("/sesiones-caja") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object sesiones(Actor a){return service.sesiones(a);}
    @PostMapping("/sesiones-caja") @ResponseStatus(HttpStatus.CREATED) @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object abrir(Actor a,@RequestBody Map<String,Object>b){return service.abrirCaja(a,b);}
    @GetMapping("/sesiones-caja/{id}") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object resumen(Actor a,@PathVariable String id){return service.resumenCaja(a,id);}
    @PostMapping("/sesiones-caja/{id}/cerrar") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object cerrar(Actor a,@PathVariable String id,@RequestBody Map<String,Object>b){return service.cerrarCaja(a,id,b);}
    @GetMapping("/recetas") @Permitidos({"ADMINISTRADOR","CAJERO","INVENTARIO"})
    public Object recetas(Actor a){return service.recetas(a);}
    @GetMapping("/recetas/{id}") @Permitidos({"ADMINISTRADOR","CAJERO","INVENTARIO"})
    public Object receta(Actor a,@PathVariable String id){return service.receta(a,id);}
    @PostMapping("/recetas") @ResponseStatus(HttpStatus.CREATED) @Permitidos({"ADMINISTRADOR","INVENTARIO"})
    public Object crearReceta(Actor a,@RequestBody Map<String,Object>b){return service.crearReceta(a,b);}
    @PostMapping("/recetas/{id}/activar") @Permitidos({"ADMINISTRADOR","INVENTARIO"})
    public Object activar(Actor a,@PathVariable String id){return service.activarReceta(a,id);}
    @GetMapping("/ventas") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object ventas(Actor a){return service.ventas(a);}
    @PostMapping("/ventas") @ResponseStatus(HttpStatus.CREATED) @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object crearVenta(Actor a,@RequestBody Map<String,Object>b){return service.crearVenta(a,b);}
    @PutMapping("/ventas/{id}/detalle") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object detalle(Actor a,@PathVariable String id,@RequestBody Map<String,Object>b){return service.editarDetalle(a,id,b);}
    @GetMapping("/ventas/{id}/comprobante") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object comprobante(Actor a,@PathVariable String id){return service.comprobante(a,id);}
    @PostMapping("/ventas/{id}/anular") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object anular(Actor a,@PathVariable String id){return service.anular(a,id);}
    @PostMapping("/ventas/{id}/confirmar") @Permitidos({"ADMINISTRADOR","CAJERO"})
    public Object confirmar(Actor a,@PathVariable String id,@RequestHeader(value="Idempotency-Key",required=false)String key,@RequestBody Map<String,Object>b){return service.confirmar(a,id,key,b);}
}
