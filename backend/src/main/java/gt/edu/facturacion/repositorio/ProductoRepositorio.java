package gt.edu.facturacion.repositorio;

import gt.edu.facturacion.modelo.Producto;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoRepositorio extends JpaRepository<Producto, Long> {

    Optional<Producto> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    /**
     * Busca por codigo o por parte de la descripcion y pone primero las
     * coincidencias exactas de codigo, que es lo que el cajero suele escribir.
     */
    @Query("""
            SELECT p FROM Producto p
            WHERE LOWER(p.codigo) LIKE LOWER(CONCAT('%', :texto, '%'))
               OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :texto, '%'))
            ORDER BY CASE WHEN LOWER(p.codigo) = LOWER(:texto) THEN 0 ELSE 1 END,
                     p.descripcion
            """)
    List<Producto> buscar(@Param("texto") String texto);
}
