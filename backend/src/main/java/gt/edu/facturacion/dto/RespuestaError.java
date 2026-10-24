package gt.edu.facturacion.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Formato unico de error para toda la API, para que el frontend siempre
 * sepa donde leer el mensaje.
 */
public record RespuestaError(
        int estado,
        String mensaje,
        Map<String, String> errores,
        LocalDateTime momento) {

    public static RespuestaError de(int estado, String mensaje) {
        return new RespuestaError(estado, mensaje, Map.of(), LocalDateTime.now());
    }

    public static RespuestaError de(int estado, String mensaje, Map<String, String> errores) {
        return new RespuestaError(estado, mensaje, errores, LocalDateTime.now());
    }
}
