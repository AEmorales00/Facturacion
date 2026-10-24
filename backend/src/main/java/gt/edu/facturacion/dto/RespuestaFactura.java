package gt.edu.facturacion.dto;

import gt.edu.facturacion.modelo.Factura;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * La factura tal como la ve el frontend. Se usa un DTO en vez de devolver la
 * entidad para no arrastrar las relaciones de JPA ni exponer campos de mas.
 */
public record RespuestaFactura(
        Long id,
        LocalDateTime fecha,
        String metodoPago,
        Long clienteId,
        String clienteNit,
        String clienteNombre,
        String clienteDireccion,
        BigDecimal tipoCambio,
        BigDecimal subtotalUsd,
        BigDecimal ivaUsd,
        BigDecimal totalUsd,
        BigDecimal totalGtq,
        List<Linea> detalle) {

    public record Linea(
            Long productoId,
            String codigo,
            String descripcion,
            String imagenUrl,
            Integer cantidad,
            BigDecimal precioUnitarioUsd,
            BigDecimal subtotalUsd) {
    }

    /**
     * Traduce la entidad de JPA al DTO que viaja en el JSON.
     *
     * @param urlDeClave funcion que convierte la clave del objeto en S3 en la
     *                   URL que el navegador puede abrir. Se recibe como
     *                   parametro porque la entidad que llega de JPA trae solo
     *                   la clave: la URL se arma al momento de leer.
     */
    public static RespuestaFactura desde(Factura f, UnaryOperator<String> urlDeClave) {
        List<Linea> lineas = f.getDetalle().stream()
                .map(d -> new Linea(
                        d.getProducto().getId(),
                        d.getProducto().getCodigo(),
                        d.getProducto().getDescripcion(),
                        urlDeClave.apply(d.getProducto().getImagenClave()),
                        d.getCantidad(),
                        d.getPrecioUnitarioUsd(),
                        d.getSubtotalUsd()))
                .toList();

        return new RespuestaFactura(
                f.getId(),
                f.getFecha(),
                f.getMetodoPago(),
                f.getCliente().getId(),
                f.getCliente().getNit(),
                f.getCliente().getNombre(),
                f.getCliente().getDireccion(),
                f.getTipoCambio(),
                f.getSubtotalUsd(),
                f.getIvaUsd(),
                f.getTotalUsd(),
                f.getTotalGtq(),
                lineas);
    }
}
