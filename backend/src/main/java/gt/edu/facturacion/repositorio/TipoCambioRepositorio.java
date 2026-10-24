package gt.edu.facturacion.repositorio;

import gt.edu.facturacion.modelo.TipoCambio;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TipoCambioRepositorio extends JpaRepository<TipoCambio, LocalDate> {

    /** El ultimo valor conocido, sirve de respaldo si el Banguat no responde. */
    Optional<TipoCambio> findTopByOrderByFechaDesc();
}
