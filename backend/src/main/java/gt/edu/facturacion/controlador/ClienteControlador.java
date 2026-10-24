package gt.edu.facturacion.controlador;

import gt.edu.facturacion.modelo.Cliente;
import gt.edu.facturacion.repositorio.ClienteRepositorio;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Clientes.
 *
 *   GET  /api/clientes            -> todos
 *   GET  /api/clientes?texto=x    -> busqueda parcial (autocompletado)
 *   GET  /api/clientes/nit/{nit}  -> busqueda exacta por NIT
 *   POST /api/clientes            -> registrar un cliente nuevo
 */
@RestController
@RequestMapping("/api/clientes")
public class ClienteControlador {

    private final ClienteRepositorio repositorio;

    public ClienteControlador(ClienteRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @GetMapping
    public List<Cliente> listar(@RequestParam(required = false) String texto) {
        if (texto == null || texto.isBlank()) {
            return repositorio.findAll();
        }
        String t = texto.trim();
        return repositorio.findTop10ByNitContainingIgnoreCaseOrNombreContainingIgnoreCase(t, t);
    }

    @GetMapping("/nit/{nit}")
    public Cliente porNit(@PathVariable String nit) {
        return repositorio.findByNit(nit.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    @GetMapping("/{id}")
    public Cliente porId(@PathVariable Long id) {
        return repositorio.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cliente no encontrado"));
    }

    @PostMapping
    public ResponseEntity<Cliente> crear(@Valid @RequestBody Cliente cliente) {
        String nit = cliente.getNit().trim();

        if (repositorio.existsByNit(nit)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ese NIT ya esta registrado");
        }

        cliente.setId(null);   // el id lo asigna la base de datos
        cliente.setNit(nit);
        if (cliente.getDireccion() == null || cliente.getDireccion().isBlank()) {
            cliente.setDireccion("Ciudad, Guatemala");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(repositorio.save(cliente));
    }
}
