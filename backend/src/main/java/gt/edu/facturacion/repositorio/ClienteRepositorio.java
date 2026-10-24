package gt.edu.facturacion.repositorio;

import gt.edu.facturacion.modelo.Cliente;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepositorio extends JpaRepository<Cliente, Long> {

    /** Busqueda exacta por NIT (es el identificador del cliente). */
    Optional<Cliente> findByNit(String nit);

    boolean existsByNit(String nit);

    /** Busqueda parcial para el autocompletado del frontend. */
    List<Cliente> findTop10ByNitContainingIgnoreCaseOrNombreContainingIgnoreCase(String nit, String nombre);
}
