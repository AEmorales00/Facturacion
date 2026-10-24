package gt.edu.facturacion.controlador;

import gt.edu.facturacion.dto.PeticionFactura;
import gt.edu.facturacion.dto.RespuestaFactura;
import gt.edu.facturacion.servicio.FacturaServicio;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Facturas.
 *
 *   POST /api/facturas                         -> emitir una factura
 *   GET  /api/facturas                         -> listado
 *   GET  /api/facturas?nit=&desde=&hasta=      -> listado con filtros
 *   GET  /api/facturas/{id}                    -> una factura con su detalle
 */
@RestController
@RequestMapping("/api/facturas")
public class FacturaControlador {

    private final FacturaServicio servicio;

    public FacturaControlador(FacturaServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    public ResponseEntity<RespuestaFactura> crear(@Valid @RequestBody PeticionFactura peticion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(peticion));
    }

    @GetMapping
    public List<RespuestaFactura> listar(
            @RequestParam(required = false) String nit,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        return servicio.listar(nit, desde, hasta);
    }

    @GetMapping("/{id}")
    public RespuestaFactura porId(@PathVariable Long id) {
        return servicio.porId(id);
    }
}
