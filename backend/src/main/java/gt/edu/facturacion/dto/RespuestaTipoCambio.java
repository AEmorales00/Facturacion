package gt.edu.facturacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tipo de cambio del dia.
 * "origen" le dice al usuario de donde salio el dato: del Web Service, de la
 * cache en base de datos, o del respaldo si el Banguat no respondio.
 */
public record RespuestaTipoCambio(
        BigDecimal referencia,
        String origen,
        LocalDate fecha) {
}
