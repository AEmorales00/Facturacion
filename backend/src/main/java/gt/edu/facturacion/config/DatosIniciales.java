package gt.edu.facturacion.config;

import gt.edu.facturacion.modelo.Cliente;
import gt.edu.facturacion.modelo.Producto;
import gt.edu.facturacion.repositorio.ClienteRepositorio;
import gt.edu.facturacion.repositorio.ProductoRepositorio;
import java.math.BigDecimal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Carga los mismos datos de prueba del Proyecto 1 la primera vez que arranca
 * la aplicacion. Se hace en Java y no con un script .sql para que funcione
 * igual en H2, MySQL y PostgreSQL sin reescribir nada.
 *
 * Solo inserta si las tablas estan vacias, asi que reiniciar el servidor no
 * duplica los datos.
 */
@Component
@ConditionalOnProperty(name = "app.datos-iniciales", havingValue = "true", matchIfMissing = true)
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final ClienteRepositorio clientes;
    private final ProductoRepositorio productos;

    public DatosIniciales(ClienteRepositorio clientes, ProductoRepositorio productos) {
        this.clientes = clientes;
        this.productos = productos;
    }

    @Override
    public void run(String... args) {
        if (clientes.count() == 0) {
            clientes.save(cliente("1234567-8", "Juan Perez", "Ciudad, Guatemala", "5555-1111"));
            clientes.save(cliente("9876543-2", "Maria Lopez", "Mixco, Guatemala", "5555-2222"));
            clientes.save(cliente("CF", "Consumidor Final", "Ciudad, Guatemala", null));
            log.info("Se cargaron los clientes de prueba");
        }

        if (productos.count() == 0) {
            productos.save(producto("P001-AZ", "Arroz Gold 2lb", "2.50", 100));
            productos.save(producto("P023-LC", "Leche Entera 1L", "3.10", 80));
            productos.save(producto("P105-PH", "Papel Higienico 4pk", "4.25", 50));
            productos.save(producto("P210-AC", "Aceite Vegetal 1L", "5.75", 40));
            productos.save(producto("P330-FR", "Frijol Negro 1lb", "1.90", 120));
            log.info("Se cargo el catalogo de prueba (sin fotografias todavia)");
        }
    }

    private Cliente cliente(String nit, String nombre, String direccion, String telefono) {
        Cliente c = new Cliente();
        c.setNit(nit);
        c.setNombre(nombre);
        c.setDireccion(direccion);
        c.setTelefono(telefono);
        return c;
    }

    private Producto producto(String codigo, String descripcion, String precio, int stock) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setDescripcion(descripcion);
        p.setPrecioUsd(new BigDecimal(precio));
        p.setStock(stock);
        return p;
    }
}
