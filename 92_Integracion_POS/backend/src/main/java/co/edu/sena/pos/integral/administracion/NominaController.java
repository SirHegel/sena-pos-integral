package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.nucleo.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Permitidos({"ADMINISTRADOR","NOMINA"})
public class NominaController {
    private final NominaService servicio;
    public NominaController(NominaService servicio) { this.servicio=servicio; }
    @GetMapping("/empleados") public Object empleados(Actor a) { return servicio.empleados(a); }
    @GetMapping("/empleados/{id}") public Object empleado(Actor a,@PathVariable String id) { return servicio.consultarEmpleado(a,id); }
    @PostMapping("/empleados") @ResponseStatus(HttpStatus.CREATED) public Object altaEmpleado(Actor a,@RequestBody NominaService.Empleado r) { return servicio.crearEmpleado(a,r); }
    @PutMapping("/empleados/{id}") public Object cambiarEmpleado(Actor a,@PathVariable String id,@RequestBody NominaService.Empleado r) { return servicio.cambiarEmpleado(a,id,r); }
    @GetMapping("/novedades") public Object novedades(Actor a,@RequestParam(required=false) String periodo) { return servicio.novedades(a,periodo); }
    @GetMapping("/novedades/{id}") public Object novedad(Actor a,@PathVariable String id) { return servicio.consultarNovedad(a,id); }
    @PostMapping("/novedades") @ResponseStatus(HttpStatus.CREATED) public Object altaNovedad(Actor a,@RequestBody NominaService.Novedad r) { return servicio.crearNovedad(a,r); }
    @PutMapping("/novedades/{id}") public Object cambiarNovedad(Actor a,@PathVariable String id,@RequestBody NominaService.Novedad r) { return servicio.cambiarNovedad(a,id,r); }
    @PostMapping("/novedades/{id}/autorizar") public Object autorizar(Actor a,@PathVariable String id) { return servicio.estadoNovedad(a,id,true); }
    @PostMapping("/novedades/{id}/rechazar") public Object rechazar(Actor a,@PathVariable String id) { return servicio.estadoNovedad(a,id,false); }
    @GetMapping("/liquidaciones") public Object liquidaciones(Actor a,@RequestParam(required=false) String periodo) { return servicio.liquidaciones(a,periodo); }
    @GetMapping("/liquidaciones/{id}") public Object detalle(Actor a,@PathVariable String id) { return servicio.detalle(a,id); }
    @GetMapping("/liquidaciones/{id}/simulacion") public Object simular(Actor a,@PathVariable String id) { return servicio.simulacion(a,id); }
    @PostMapping("/liquidaciones") @ResponseStatus(HttpStatus.CREATED) public Object crear(Actor a,@RequestBody NominaService.Liquidacion r) { return servicio.crearLiquidacion(a,r); }
    @PostMapping("/liquidaciones/{id}/aprobar") public Object aprobar(Actor a,@PathVariable String id,@RequestBody NominaService.Aprobacion r) { return servicio.aprobar(a,id,r); }
}
