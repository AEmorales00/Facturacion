package gt.edu.facturacion.repositorio;

import gt.edu.facturacion.modelo.Factura;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FacturaRepositorio extends JpaRepository<Factura, Long> {

    List<Factura> findAllByOrderByFechaDesc();

    /**
     * Consulta con filtros opcionales: si un parametro llega nulo, esa parte
     * del WHERE se ignora. Asi una sola query sirve para todos los filtros
     * de la pantalla de facturas registradas.
     */
    @Query("""
            SELECT f FROM Factura f
            WHERE (:nit IS NULL OR LOWER(f.cliente.nit) LIKE LOWER(CONCAT('%', :nit, '%')))
              AND (:desde IS NULL OR f.fecha >= :desde)
              AND (:hasta IS NULL OR f.fecha <= :hasta)
            ORDER BY f.fecha DESC
            """)
    List<Factura> buscarConFiltros(@Param("nit") String nit,
                                   @Param("desde") LocalDateTime desde,
                                   @Param("hasta") LocalDateTime hasta);
}
