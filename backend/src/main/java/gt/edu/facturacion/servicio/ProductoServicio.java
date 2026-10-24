package gt.edu.facturacion.servicio;

import gt.edu.facturacion.modelo.Producto;
import gt.edu.facturacion.repositorio.ProductoRepositorio;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Logica del catalogo de productos.
 *
 * Es la clase que une la base de datos con el almacenamiento de imagenes:
 * en la BD queda la clave del objeto y aqui se le arma la URL a cada producto
 * antes de devolverlo al frontend.
 */
@Service
public class ProductoServicio {

    private final ProductoRepositorio repositorio;
    private final AlmacenamientoImagenes almacenamiento;

    public ProductoServicio(ProductoRepositorio repositorio, AlmacenamientoImagenes almacenamiento) {
        this.repositorio = repositorio;
        this.almacenamiento = almacenamiento;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return repositorio.findAll().stream().map(this::conUrl).toList();
    }

    @Transactional(readOnly = true)
    public List<Producto> buscar(String texto) {
        if (texto == null || texto.isBlank()) {
            return listar();
        }
        return repositorio.buscar(texto.trim()).stream().map(this::conUrl).toList();
    }

    @Transactional(readOnly = true)
    public Producto porId(Long id) {
        return repositorio.findById(id)
                .map(this::conUrl)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el producto " + id));
    }

    /**
     * Crea un producto. La imagen es opcional: si no viene, el producto queda
     * en el catalogo sin fotografia y se le puede agregar despues.
     */
    @Transactional
    public Producto crear(String codigo, String descripcion, BigDecimal precioUsd,
                          Integer stock, MultipartFile imagen) {

        String limpio = codigo == null ? "" : codigo.trim();
        if (repositorio.existsByCodigo(limpio)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un producto con el codigo " + limpio);
        }

        Producto producto = new Producto();
        producto.setCodigo(limpio);
        producto.setDescripcion(descripcion == null ? "" : descripcion.trim());
        producto.setPrecioUsd(precioUsd);
        producto.setStock(stock == null ? 0 : stock);

        if (imagen != null && !imagen.isEmpty()) {
            producto.setImagenClave(almacenamiento.subir(imagen));
        }

        return conUrl(repositorio.save(producto));
    }

    /**
     * Actualiza un producto. Si llega una imagen nueva se sube y se borra la
     * anterior del bucket, para no dejar archivos huerfanos acumulandose.
     */
    @Transactional
    public Producto actualizar(Long id, String codigo, String descripcion,
                               BigDecimal precioUsd, Integer stock, MultipartFile imagen) {

        Producto producto = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el producto " + id));

        if (codigo != null && !codigo.isBlank()) {
            String limpio = codigo.trim();
            if (!limpio.equals(producto.getCodigo()) && repositorio.existsByCodigo(limpio)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un producto con el codigo " + limpio);
            }
            producto.setCodigo(limpio);
        }
        if (descripcion != null && !descripcion.isBlank()) {
            producto.setDescripcion(descripcion.trim());
        }
        if (precioUsd != null) {
            producto.setPrecioUsd(precioUsd);
        }
        if (stock != null) {
            producto.setStock(stock);
        }

        if (imagen != null && !imagen.isEmpty()) {
            String anterior = producto.getImagenClave();
            producto.setImagenClave(almacenamiento.subir(imagen));
            almacenamiento.eliminar(anterior);
        }

        return conUrl(repositorio.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No existe el producto " + id));
        try {
            repositorio.delete(producto);
            repositorio.flush();
        } catch (RuntimeException e) {
            // Si el producto ya salio en una factura, la llave foranea lo protege.
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede borrar: el producto ya aparece en facturas emitidas");
        }
        almacenamiento.eliminar(producto.getImagenClave());
    }

    /** Le pone al producto la URL con la que el navegador vera su imagen. */
    private Producto conUrl(Producto producto) {
        producto.setImagenUrl(almacenamiento.url(producto.getImagenClave()));
        return producto;
    }
}
