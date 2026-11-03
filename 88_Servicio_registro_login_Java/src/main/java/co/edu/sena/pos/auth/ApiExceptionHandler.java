package co.edu.sena.pos.auth;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ApiError.class)
    public ResponseEntity<Map<String, String>> apiError(ApiError ex) {
        return ResponseEntity.status(ex.status()).body(Map.of("codigo", ex.code(), "mensaje", ex.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> malformed() {
        return ResponseEntity.badRequest().body(Map.of("codigo", "JSON_INVALIDO", "mensaje", "Cuerpo JSON no válido"));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> database() {
        // No se exponen nombres internos, SQL ni secretos del servidor.
        return ResponseEntity.status(503).body(Map.of("codigo", "SERVICIO_NO_DISPONIBLE", "mensaje", "Servicio temporalmente no disponible"));
    }
}
