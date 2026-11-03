package co.edu.sena.pos.integral.catalogo;

import co.edu.sena.pos.integral.nucleo.Actor;
import co.edu.sena.pos.integral.nucleo.Permitidos;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/productos")
@Permitidos({"ADMINISTRADOR","CAJERO","INVENTARIO"})
public class CatalogoController {
    private final CatalogoService catalogo;
    public CatalogoController(CatalogoService catalogo) { this.catalogo=catalogo; }
    @GetMapping public Object lista(Actor a,@RequestParam(required=false) String q){return catalogo.lista(a,q);}
    @GetMapping("/{id}") public Object uno(Actor a,@PathVariable String id){return catalogo.uno(a,id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Permitidos({"ADMINISTRADOR","INVENTARIO"})
    public Object crear(Actor a,@RequestBody CatalogoService.Producto r){return catalogo.crear(a,r);}
    @PutMapping("/{id}") @Permitidos({"ADMINISTRADOR","INVENTARIO"})
    public Object cambiar(Actor a,@PathVariable String id,@RequestBody CatalogoService.Producto r){return catalogo.cambiar(a,id,r);}
}
