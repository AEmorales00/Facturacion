package gt.edu.facturacion.controlador;

import gt.edu.facturacion.modelo.Producto;
import gt.edu.facturacion.servicio.ProductoServicio;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Catalogo de productos.
 *
 * El alta y la edicion usan multipart/form-data porque viaja la fotografia
 * junto con los datos. El backend recibe la imagen, la sube al bucket de
 * AWS S3 y guarda la referencia en la base de datos.
 *
 *   GET    /api/productos          -> catalogo completo
 *   GET    /api/productos?texto=x  -> busqueda por codigo o descripcion
 *   GET    /api/productos/{id}     -> un producto
 *   POST   /api/productos          -> alta (con imagen)
 *   PUT    /api/productos/{id}     -> edicion (imagen opcional)
 *   DELETE /api/productos/{id}     -> baja
 */
@RestController
@RequestMapping("/api/productos")
public class ProductoControlador {

    private final ProductoServicio servicio;

    public ProductoControlador(ProductoServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public List<Producto> listar(@RequestParam(required = false) String texto) {
        return servicio.buscar(texto);
    }

    @GetMapping("/{id}")
    public Producto porId(@PathVariable Long id) {
        return servicio.porId(id);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Producto> crear(
            @RequestParam String codigo,
            @RequestParam String descripcion,
            @RequestParam BigDecimal precioUsd,
            @RequestParam(defaultValue = "0") Integer stock,
            @RequestPart(required = false) MultipartFile imagen) {

        Producto creado = servicio.crear(codigo, descripcion, precioUsd, stock, imagen);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Producto actualizar(
            @PathVariable Long id,
            @RequestParam(required = false) String codigo,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) BigDecimal precioUsd,
            @RequestParam(required = false) Integer stock,
            @RequestPart(required = false) MultipartFile imagen) {

        return servicio.actualizar(id, codigo, descripcion, precioUsd, stock, imagen);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
