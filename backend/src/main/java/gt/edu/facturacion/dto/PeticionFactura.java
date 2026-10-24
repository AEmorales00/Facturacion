package gt.edu.facturacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Lo que el frontend envia para crear una factura.
 *
 * Importante: NO se recibe el precio. El backend lo vuelve a leer de la base
 * de datos, porque cualquiera podria modificarlo desde el navegador.
 */
public record PeticionFactura(
        @NotNull(message = "Debe seleccionar un cliente")
        Long clienteId,

        String metodoPago,

        @NotEmpty(message = "La factura no tiene productos")
        @Valid
        List<Linea> items) {

    public record Linea(
            @NotNull(message = "Falta el producto")
            Long productoId,

            @NotNull(message = "Falta la cantidad")
            @Min(value = 1, message = "La cantidad debe ser al menos 1")
            Integer cantidad) {
    }
}
