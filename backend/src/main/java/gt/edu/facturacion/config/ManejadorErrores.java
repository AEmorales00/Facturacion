package gt.edu.facturacion.config;

import gt.edu.facturacion.dto.RespuestaError;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Convierte cualquier error en un JSON con el mismo formato, para que el
 * frontend siempre sepa donde leer el mensaje y pueda mostrarselo al usuario
 * en vez de un "500" sin explicacion.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Errores que el propio codigo lanza a proposito (404, 409, 400...). */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaError> deEstado(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode())
                .body(RespuestaError.de(e.getStatusCode().value(), e.getReason()));
    }

    /** Falla alguna validacion de @Valid: se devuelve campo por campo. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> deValidacion(MethodArgumentNotValidException e) {
        Map<String, String> errores = new HashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(error -> errores.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(RespuestaError.de(
                HttpStatus.BAD_REQUEST.value(), "Hay datos invalidos en el formulario", errores));
    }

    /** Llaves duplicadas o foraneas: normalmente un NIT o codigo repetido. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RespuestaError> deIntegridad(DataIntegrityViolationException e) {
        log.warn("Violacion de integridad: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(RespuestaError.de(
                HttpStatus.CONFLICT.value(),
                "El dato ya existe o esta siendo usado por otro registro"));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<RespuestaError> deArchivoGrande(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(RespuestaError.de(
                HttpStatus.PAYLOAD_TOO_LARGE.value(),
                "La imagen pesa demasiado. El maximo permitido son 5 MB."));
    }

    /** Red de seguridad: cualquier otro error inesperado. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> inesperado(Exception e) {
        log.error("Error no controlado", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(RespuestaError.de(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Ocurrio un error en el servidor: " + e.getMessage()));
    }
}
