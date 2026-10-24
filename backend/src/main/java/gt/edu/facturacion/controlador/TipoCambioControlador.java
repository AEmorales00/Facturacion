package gt.edu.facturacion.controlador;

import gt.edu.facturacion.dto.RespuestaTipoCambio;
import gt.edu.facturacion.servicio.AlmacenamientoImagenes;
import gt.edu.facturacion.servicio.BanguatServicio;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tipo de cambio del Banguat y estado del sistema.
 *
 *   GET /api/tipo-cambio               -> valor del dia (usa la cache)
 *   GET /api/tipo-cambio?refrescar=true-> obliga a llamar al Web Service
 *   GET /api/tipo-cambio/demo          -> XML enviado y recibido, para la presentacion
 *   GET /api/salud                     -> comprobacion rapida del despliegue
 */
@RestController
@RequestMapping("/api")
public class TipoCambioControlador {

    private final BanguatServicio banguat;
    private final AlmacenamientoImagenes almacenamiento;

    public TipoCambioControlador(BanguatServicio banguat, AlmacenamientoImagenes almacenamiento) {
        this.banguat = banguat;
        this.almacenamiento = almacenamiento;
    }

    @GetMapping("/tipo-cambio")
    public RespuestaTipoCambio tipoCambio(@RequestParam(defaultValue = "false") boolean refrescar) {
        return banguat.obtener(refrescar);
    }

    /**
     * Devuelve el intercambio SOAP completo. Sirve para la demostracion del
     * Web Service: se puede ensenar el XML que se envia y el que responde el
     * Banco de Guatemala.
     */
    @GetMapping("/tipo-cambio/demo")
    public BanguatServicio.DemostracionWs demo() {
        return banguat.demostrar();
    }

    /** Para confirmar que la API desplegada esta viva y como quedo configurada. */
    @GetMapping("/salud")
    public Map<String, Object> salud() {
        return Map.of(
                "estado", "ok",
                "almacenamiento", almacenamiento.descripcion());
    }
}
