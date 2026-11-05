package co.edu.sena.pos.integral.operacion;

import co.edu.sena.pos.integral.nucleo.*;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/existencias")
public class MinimosController {
    private final MinimosService service;
    public MinimosController(MinimosService service) { this.service=service; }
    @PutMapping("/minimo")
    @Permitidos({"ADMINISTRADOR","INVENTARIO"})
    public Object configurar(Actor actor,@RequestBody Map<String,Object> cuerpo) {
        return service.configurar(actor,cuerpo);
    }
}
