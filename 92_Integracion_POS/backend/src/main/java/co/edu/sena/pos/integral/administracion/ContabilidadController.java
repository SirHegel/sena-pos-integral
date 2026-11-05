package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.nucleo.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Permitidos({"ADMINISTRADOR","CONTABILIDAD"})
public class ContabilidadController {
    private final ContabilidadService servicio;
    public ContabilidadController(ContabilidadService servicio) { this.servicio=servicio; }
    @GetMapping("/cuentas") public Object cuentas(Actor a) { return servicio.cuentas(a); }
    @PostMapping("/cuentas") @ResponseStatus(HttpStatus.CREATED) public Object crear(Actor a,@RequestBody ContabilidadService.Cuenta r) { return servicio.crearCuenta(a,r); }
    @PutMapping("/cuentas/{id}") public Object editar(Actor a,@PathVariable String id,@RequestBody ContabilidadService.Cuenta r) { return servicio.cambiarCuenta(a,id,r); }
    @GetMapping("/asientos") public Object asientos(Actor a) { return servicio.asientos(a); }
    @GetMapping("/asientos/{id}") public Object asiento(Actor a,@PathVariable String id) { return servicio.asiento(a,id); }
    @PostMapping("/asientos") @ResponseStatus(HttpStatus.CREATED) public Object registrar(Actor a,@RequestBody ContabilidadService.AltaAsiento r) { return servicio.crearAsiento(a,r); }
    @GetMapping("/eventos") public Object eventos(Actor a,@RequestParam(required=false) String estado) { return servicio.eventos(a,estado); }
    @GetMapping("/eventos/{id}") public Object evento(Actor a,@PathVariable String id) { return servicio.evento(a,id); }
    @PostMapping("/eventos/{id}/procesar") public Object procesar(Actor a,@PathVariable String id,@RequestBody ContabilidadService.Contabilizacion r) { return servicio.procesar(a,id,r); }
}
