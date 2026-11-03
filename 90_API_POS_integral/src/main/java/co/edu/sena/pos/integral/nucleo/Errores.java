package co.edu.sena.pos.integral.nucleo;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class Errores {
    @ExceptionHandler(ApiError.class)
    ResponseEntity<?> negocio(ApiError e) { return respuesta(e.estado(), e.codigo(), e.getMessage()); }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<?> json(HttpMessageNotReadableException e) { return respuesta(400, "JSON_INVALIDO", "El cuerpo JSON no tiene el formato requerido"); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> integridad(DataIntegrityViolationException e) { return respuesta(409, "CONFLICTO_DATOS", "La operación entra en conflicto con una restricción del modelo"); }
    @ExceptionHandler(DataAccessException.class)
    ResponseEntity<?> datos(DataAccessException e) { return respuesta(503, "DATOS_NO_DISPONIBLES", "No se pudo completar la operación de datos"); }
    private ResponseEntity<?> respuesta(int status, String codigo, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("codigo", codigo, "mensaje", mensaje));
    }
}
