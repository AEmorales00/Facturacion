package gt.edu.facturacion.servicio;

import gt.edu.facturacion.dto.PeticionFactura;
import gt.edu.facturacion.dto.RespuestaFactura;
import gt.edu.facturacion.modelo.Cliente;
import gt.edu.facturacion.modelo.Factura;
import gt.edu.facturacion.modelo.FacturaDetalle;
import gt.edu.facturacion.modelo.Producto;
import gt.edu.facturacion.repositorio.ClienteRepositorio;
import gt.edu.facturacion.repositorio.FacturaRepositorio;
import gt.edu.facturacion.repositorio.ProductoRepositorio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * ============================================================
 *  LOGICA DE FACTURACION
 * ------------------------------------------------------------
 *  Esta clase es el corazon del sistema. Tres reglas que se
 *  respetan siempre:
 *
 *  1. Los precios NO se reciben del navegador. Solo llegan el id
 *     del producto y la cantidad; el precio se vuelve a leer de
 *     la base de datos. Asi nadie puede cambiar un precio desde
 *     las herramientas de desarrollador.
 *
 *  2. Todo va dentro de una transaccion (@Transactional). Si algo
 *     falla a medias, no queda una factura incompleta ni stock
 *     descontado de mas: se deshace todo.
 *
 *  3. El tipo de cambio se copia dentro de la factura, porque
 *     manana el Banguat dara otro valor y la factura de hoy debe
 *     conservar el que se cobro.
 * ============================================================
 */
@Service
public class FacturaServicio {

    private final FacturaRepositorio facturaRepositorio;
    private final ClienteRepositorio clienteRepositorio;
    private final ProductoRepositorio productoRepositorio;
    private final BanguatServicio banguat;
    private final AlmacenamientoImagenes almacenamiento;

    /** El IVA en Guatemala es del 12%. */
    private final BigDecimal iva;

    public FacturaServicio(FacturaRepositorio facturaRepositorio,
                           ClienteRepositorio clienteRepositorio,
                           ProductoRepositorio productoRepositorio,
                           BanguatServicio banguat,
                           AlmacenamientoImagenes almacenamiento,
                           @Value("${app.iva:0.12}") BigDecimal iva) {
        this.facturaRepositorio = facturaRepositorio;
        this.clienteRepositorio = clienteRepositorio;
        this.productoRepositorio = productoRepositorio;
        this.banguat = banguat;
        this.almacenamiento = almacenamiento;
        this.iva = iva;
    }

    @Transactional
    public RespuestaFactura crear(PeticionFactura peticion) {

        Cliente cliente = clienteRepositorio.findById(peticion.clienteId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "El cliente seleccionado no existe"));

        // Si el mismo producto viene repetido en el carrito, se suman las
        // cantidades en vez de crear dos lineas iguales.
        Map<Long, Integer> cantidadPorProducto = new LinkedHashMap<>();
        for (PeticionFactura.Linea linea : peticion.items()) {
            cantidadPorProducto.merge(linea.productoId(), linea.cantidad(), Integer::sum);
        }

        Factura factura = new Factura();
        factura.setCliente(cliente);
        factura.setFecha(LocalDateTime.now());
        factura.setMetodoPago(
                peticion.metodoPago() == null || peticion.metodoPago().isBlank()
                        ? "Efectivo" : peticion.metodoPago());

        BigDecimal subtotal = BigDecimal.ZERO;

        for (Map.Entry<Long, Integer> item : cantidadPorProducto.entrySet()) {
            Producto producto = productoRepositorio.findById(item.getKey())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Hay un producto invalido en el detalle (id " + item.getKey() + ")"));

            int cantidad = item.getValue();

            // Validacion que faltaba en el Proyecto 1: no vender mas de lo que hay.
            if (producto.getStock() < cantidad) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Stock insuficiente de %s: hay %d y se piden %d"
                                .formatted(producto.getDescripcion(), producto.getStock(), cantidad));
            }

            // El precio se lee de la BD, nunca del navegador.
            BigDecimal precio = producto.getPrecioUsd();
            BigDecimal subtotalLinea = precio.multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);

            FacturaDetalle linea = new FacturaDetalle();
            linea.setProducto(producto);
            linea.setCantidad(cantidad);
            linea.setPrecioUnitarioUsd(precio);
            linea.setSubtotalUsd(subtotalLinea);
            factura.agregarLinea(linea);

            subtotal = subtotal.add(subtotalLinea);

            // Se descuenta el inventario dentro de la misma transaccion.
            producto.setStock(producto.getStock() - cantidad);
            productoRepositorio.save(producto);
        }

        BigDecimal tipoCambio = banguat.referenciaDelDia();
        BigDecimal montoIva = subtotal.multiply(iva).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalUsd = subtotal.add(montoIva).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalGtq = totalUsd.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);

        factura.setTipoCambio(tipoCambio);
        factura.setSubtotalUsd(subtotal.setScale(2, RoundingMode.HALF_UP));
        factura.setIvaUsd(montoIva);
        factura.setTotalUsd(totalUsd);
        factura.setTotalGtq(totalGtq);

        return RespuestaFactura.desde(facturaRepositorio.save(factura), almacenamiento::url);
    }

    @Transactional(readOnly = true)
    public List<RespuestaFactura> listar(String nit, LocalDate desde, LocalDate hasta) {
        // Las fechas llegan como dia suelto; se convierten a rango completo
        // para que "hasta = hoy" incluya las facturas de hoy a cualquier hora.
        LocalDateTime inicio = desde == null ? null : desde.atStartOfDay();
        LocalDateTime fin = hasta == null ? null : hasta.atTime(LocalTime.MAX);
        String filtroNit = (nit == null || nit.isBlank()) ? null : nit.trim();

        return facturaRepositorio.buscarConFiltros(filtroNit, inicio, fin).stream()
                .map(f -> RespuestaFactura.desde(f, almacenamiento::url))
                .toList();
    }

    @Transactional(readOnly = true)
    public RespuestaFactura porId(Long id) {
        return facturaRepositorio.findById(id)
                .map(f -> RespuestaFactura.desde(f, almacenamiento::url))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe la factura " + id));
    }
}
