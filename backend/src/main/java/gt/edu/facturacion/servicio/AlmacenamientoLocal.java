package gt.edu.facturacion.servicio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * Guarda las imagenes en una carpeta del disco en vez de en S3.
 *
 * Existe para poder trabajar y probar el sistema completo antes de tener la
 * cuenta de AWS lista. Expone las mismas operaciones que AlmacenamientoS3, asi
 * que cambiar a S3 es solo cambiar app.almacenamiento=s3 en la configuracion;
 * ni una linea del resto del proyecto se toca.
 */
@Service
@ConditionalOnProperty(name = "app.almacenamiento", havingValue = "local", matchIfMissing = true)
public class AlmacenamientoLocal implements AlmacenamientoImagenes {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoLocal.class);

    private final Path carpeta;
    private final String urlBase;

    public AlmacenamientoLocal(
            @Value("${app.local.carpeta:uploads}") String carpeta,
            @Value("${app.local.url-base:/imagenes}") String ruta,
            @Value("${app.url-publica:http://localhost:8080}") String urlPublica) {

        this.carpeta = Paths.get(carpeta).toAbsolutePath().normalize();

        // La URL tiene que ser absoluta: el Angular corre en otro puerto
        // (4200) y una ruta relativa la buscaria en si mismo, no en la API.
        this.urlBase = urlPublica.replaceAll("/+$", "") + ruta;

        try {
            Files.createDirectories(this.carpeta);
            log.info("Las imagenes se guardaran en {}", this.carpeta);
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de imagenes " + this.carpeta, e);
        }
    }

    @Override
    public String subir(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se envio ninguna imagen");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo debe ser una imagen (jpg, png, webp)");
        }

        String clave = UUID.randomUUID() + extension(archivo);
        Path destino = carpeta.resolve(clave).normalize();

        // Defensa contra nombres tipo "../../algo": el destino debe quedar dentro
        // de la carpeta de imagenes.
        if (!destino.startsWith(carpeta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo invalido");
        }

        try {
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar la imagen", e);
        }

        return clave;
    }

    @Override
    public String url(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }
        return urlBase + "/" + clave;
    }

    @Override
    public void eliminar(String clave) {
        if (clave == null || clave.isBlank()) {
            return;
        }
        try {
            Path destino = carpeta.resolve(clave).normalize();
            if (destino.startsWith(carpeta)) {
                Files.deleteIfExists(destino);
            }
        } catch (IOException e) {
            log.warn("No se pudo borrar la imagen {}: {}", clave, e.getMessage());
        }
    }

    @Override
    public String descripcion() {
        return "Disco local (" + carpeta + ") - modo de desarrollo, aun no se usa S3";
    }

    private String extension(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto).toLowerCase();
    }
}
