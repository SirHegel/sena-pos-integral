package co.edu.sena.pos.integral.administracion;

import co.edu.sena.pos.integral.nucleo.*;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reportes")
@Permitidos({"ADMINISTRADOR","CAJERO","INVENTARIO","NOMINA","CONTABILIDAD"})
public class ReportesController {
    private final ReportesService servicio;
    public ReportesController(ReportesService servicio) { this.servicio=servicio; }
    @GetMapping("/{area}") public Object consultar(Actor a,@PathVariable String area,@RequestParam String desde,@RequestParam String hasta) { return servicio.consultar(a,area,desde,hasta); }
    @GetMapping("/{area}/csv") public ResponseEntity<byte[]> csv(Actor a,@PathVariable String area,@RequestParam String desde,@RequestParam String hasta) {
        var r=servicio.consultar(a,area,desde,hasta);
        return ResponseEntity.ok().header("Content-Type","text/csv; charset=UTF-8").header("Content-Disposition","attachment; filename=\"reporte-"+r.area()+"-"+r.desde()+"_"+r.hasta()+".csv\"").body(servicio.csv(r).getBytes(StandardCharsets.UTF_8));
    }
}
