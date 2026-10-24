package gt.edu.facturacion.servicio;

import gt.edu.facturacion.dto.RespuestaTipoCambio;
import gt.edu.facturacion.modelo.TipoCambio;
import gt.edu.facturacion.repositorio.TipoCambioRepositorio;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * ============================================================
 *  WEB SERVICE DEL BANCO DE GUATEMALA
 * ------------------------------------------------------------
 *  URL      : https://www.banguat.gob.gt/variables/ws/TipoCambio.asmx
 *  Metodo   : TipoCambioDia
 *  Protocolo: SOAP 1.1 (se envia un XML y devuelve otro XML)
 *
 *  Ojo: la URL tiene que ir con https. Con http el Banguat responde un
 *  redireccionamiento 302 y no el XML.
 *
 *  Estrategia de obtencion (la misma del Proyecto 1):
 *    1. Si ya se consulto hoy, se lee de la tabla tipo_cambio (mas rapido).
 *    2. Si no, se llama al Web Service y se guarda el resultado.
 *    3. Si el Web Service falla, se usa el ultimo valor guardado para que
 *       el sistema pueda seguir facturando.
 * ============================================================
 */
@Service
public class BanguatServicio {

    private static final Logger log = LoggerFactory.getLogger(BanguatServicio.class);

    /** Este NO es una direccion web, es el nombre del metodo que pide SOAP. */
    private static final String ACCION_SOAP = "http://www.banguat.gob.gt/variables/ws/TipoCambioDia";

    private static final String SOBRE_SOAP = """
            <?xml version="1.0" encoding="utf-8"?>
            <soap:Envelope xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                           xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                           xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
              <soap:Body>
                <TipoCambioDia xmlns="http://www.banguat.gob.gt/variables/ws/" />
              </soap:Body>
            </soap:Envelope>
            """;

    /** Valor de ultimo recurso: solo se usa si la BD esta vacia y el WS falla. */
    private static final BigDecimal RESPALDO = new BigDecimal("7.80000");

    private final TipoCambioRepositorio repositorio;
    private final HttpClient http;
    private final String url;

    public BanguatServicio(TipoCambioRepositorio repositorio,
                           @Value("${app.banguat.url:https://www.banguat.gob.gt/variables/ws/TipoCambio.asmx}") String url,
                           @Value("${app.banguat.timeout-segundos:10}") int timeout) {
        this.repositorio = repositorio;
        this.url = url;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeout))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /**
     * Devuelve el tipo de cambio que debe usar el sistema hoy.
     *
     * @param forzar si es true se ignora la cache y se llama al WS de nuevo
     *               (lo usa el boton "Actualizar" de la demostracion)
     */
    @Transactional
    public RespuestaTipoCambio obtener(boolean forzar) {
        LocalDate hoy = LocalDate.now();

        // 1. Ya lo consultamos hoy al Banguat
        if (!forzar) {
            Optional<TipoCambio> guardado = repositorio.findById(hoy);
            if (guardado.isPresent() && "Banguat WS".equals(guardado.get().getOrigen())) {
                return new RespuestaTipoCambio(
                        guardado.get().getReferencia(),
                        "Base de datos (consultado hoy al Banguat)",
                        hoy);
            }
        }

        // 2. Le preguntamos al Web Service
        Optional<BigDecimal> delServicio = consultarWebService();
        if (delServicio.isPresent()) {
            TipoCambio registro = new TipoCambio(hoy, delServicio.get(), "Banguat WS");
            repositorio.save(registro);
            return new RespuestaTipoCambio(delServicio.get(), "Web Service Banguat", hoy);
        }

        // 3. El WS no respondio: usamos el ultimo valor conocido
        BigDecimal ultimo = repositorio.findTopByOrderByFechaDesc()
                .map(TipoCambio::getReferencia)
                .orElse(RESPALDO);

        return new RespuestaTipoCambio(ultimo, "Respaldo (el WS no respondio)", hoy);
    }

    /** Atajo para el resto del sistema: solo el numero. */
    public BigDecimal referenciaDelDia() {
        return obtener(false).referencia();
    }

    /**
     * Habla con el Web Service y saca la etiqueta <referencia> del XML.
     * Devuelve Optional.empty() si algo falla, nunca lanza excepcion: el
     * sistema tiene que poder seguir facturando aunque el Banguat este caido.
     */
    private Optional<BigDecimal> consultarWebService() {
        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "\"" + ACCION_SOAP + "\"")
                    .POST(HttpRequest.BodyPublishers.ofString(SOBRE_SOAP, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());

            if (respuesta.statusCode() != 200) {
                log.warn("El Banguat respondio con codigo {}", respuesta.statusCode());
                return Optional.empty();
            }

            return leerReferencia(respuesta.body());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Se interrumpio la consulta al Banguat");
            return Optional.empty();
        } catch (Exception e) {
            log.warn("No se pudo consultar el Banguat: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * El XML trae ...<VarDolar><fecha>..</fecha><referencia>7.66</referencia></VarDolar>
     * Se busca directamente la etiqueta "referencia" sin importar los prefijos.
     */
    private Optional<BigDecimal> leerReferencia(String xml) {
        try {
            var fabrica = DocumentBuilderFactory.newInstance();
            // Hace falta para poder buscar la etiqueta sin importar su prefijo:
            // la respuesta del Banguat viene dentro de varios namespaces.
            fabrica.setNamespaceAware(true);
            // Buenas practicas de seguridad al leer XML de un tercero:
            // se desactivan las entidades externas (ataque XXE).
            fabrica.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            fabrica.setXIncludeAware(false);
            fabrica.setExpandEntityReferences(false);

            Document documento = fabrica.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList nodos = documento.getElementsByTagNameNS("*", "referencia");
            if (nodos.getLength() == 0) {
                log.warn("El Banguat no devolvio la etiqueta <referencia>");
                return Optional.empty();
            }

            String texto = nodos.item(0).getTextContent().trim();
            return Optional.of(new BigDecimal(texto));

        } catch (Exception e) {
            log.warn("La respuesta del Banguat no se pudo leer: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Version para la pantalla de demostracion: devuelve tambien el XML que se
     * envio y el que se recibio, para poder ensenarlos en la presentacion.
     */
    public DemostracionWs demostrar() {
        String xmlRecibido;
        BigDecimal referencia = null;
        String error = null;

        try {
            HttpRequest peticion = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "text/xml; charset=utf-8")
                    .header("SOAPAction", "\"" + ACCION_SOAP + "\"")
                    .POST(HttpRequest.BodyPublishers.ofString(SOBRE_SOAP, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            xmlRecibido = respuesta.body();
            referencia = leerReferencia(xmlRecibido).orElse(null);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            xmlRecibido = "";
            error = "Se interrumpio la consulta";
        } catch (Exception e) {
            xmlRecibido = "";
            error = e.getMessage();
        }

        return new DemostracionWs(url, ACCION_SOAP, SOBRE_SOAP, xmlRecibido, referencia, error);
    }

    /** Todo lo que necesita la pantalla de demostracion del Web Service. */
    public record DemostracionWs(
            String url,
            String accionSoap,
            String xmlEnviado,
            String xmlRecibido,
            BigDecimal referencia,
            String error) {
    }
}
