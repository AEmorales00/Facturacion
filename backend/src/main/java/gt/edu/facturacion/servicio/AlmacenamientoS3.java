package gt.edu.facturacion.servicio;

import gt.edu.facturacion.config.PropiedadesS3;
import java.io.IOException;
import java.time.Duration;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Guarda las fotografias de los productos en un bucket de Amazon S3.
 *
 * Soporta las dos formas que permite el instructivo:
 *   - Bucket publico: la URL se arma directa y nunca vence.
 *   - Bucket privado: se firma una URL temporal (presigned URL) cada vez que
 *     se lee el producto. Es la opcion recomendada porque no obliga a abrir
 *     el bucket a todo internet.
 */
@Service
@ConditionalOnProperty(name = "app.almacenamiento", havingValue = "s3")
public class AlmacenamientoS3 implements AlmacenamientoImagenes {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoS3.class);

    private final S3Client s3;
    private final S3Presigner firmador;
    private final PropiedadesS3 propiedades;

    public AlmacenamientoS3(S3Client s3, S3Presigner firmador, PropiedadesS3 propiedades) {
        this.s3 = s3;
        this.firmador = firmador;
        this.propiedades = propiedades;
    }

    @Override
    public String subir(MultipartFile archivo) {
        validar(archivo);

        // Se le pone un UUID al nombre para que dos productos con fotos que se
        // llaman igual ("foto.jpg") no se pisen dentro del bucket.
        String clave = propiedades.getPrefijo() + UUID.randomUUID() + extension(archivo);

        try {
            PutObjectRequest peticion = PutObjectRequest.builder()
                    .bucket(propiedades.getBucket())
                    .key(clave)
                    .contentType(archivo.getContentType())
                    .build();

            s3.putObject(peticion, RequestBody.fromInputStream(
                    archivo.getInputStream(), archivo.getSize()));

            log.info("Imagen subida a S3: s3://{}/{}", propiedades.getBucket(), clave);
            return clave;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se pudo leer el archivo enviado", e);
        } catch (RuntimeException e) {
            log.error("Fallo la subida a S3", e);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "No se pudo subir la imagen a S3: " + e.getMessage(), e);
        }
    }

    @Override
    public String url(String clave) {
        if (clave == null || clave.isBlank()) {
            return null;
        }

        if (!propiedades.isUrlsFirmadas()) {
            // Bucket publico: la URL es siempre la misma y no vence.
            return "https://%s.s3.%s.amazonaws.com/%s"
                    .formatted(propiedades.getBucket(), propiedades.getRegion(), clave);
        }

        // Bucket privado: se firma una URL que vence sola.
        GetObjectRequest objeto = GetObjectRequest.builder()
                .bucket(propiedades.getBucket())
                .key(clave)
                .build();

        GetObjectPresignRequest peticion = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(propiedades.getMinutosFirma()))
                .getObjectRequest(objeto)
                .build();

        return firmador.presignGetObject(peticion).url().toString();
    }

    @Override
    public void eliminar(String clave) {
        if (clave == null || clave.isBlank()) {
            return;
        }
        try {
            s3.deleteObject(DeleteObjectRequest.builder()
                    .bucket(propiedades.getBucket())
                    .key(clave)
                    .build());
        } catch (RuntimeException e) {
            // Que no se caiga la peticion por no poder borrar una imagen vieja.
            log.warn("No se pudo borrar {} de S3: {}", clave, e.getMessage());
        }
    }

    @Override
    public String descripcion() {
        return "AWS S3 (bucket %s, region %s, %s)".formatted(
                propiedades.getBucket(),
                propiedades.getRegion(),
                propiedades.isUrlsFirmadas() ? "URLs firmadas" : "bucket publico");
    }

    /** Solo se aceptan imagenes y con un tamano razonable. */
    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se envio ninguna imagen");
        }
        String tipo = archivo.getContentType();
        if (tipo == null || !tipo.startsWith("image/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo debe ser una imagen (jpg, png, webp)");
        }
    }

    /** Saca la extension del nombre original para conservarla en el bucket. */
    private String extension(MultipartFile archivo) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null) {
            return "";
        }
        int punto = nombre.lastIndexOf('.');
        return punto < 0 ? "" : nombre.substring(punto).toLowerCase();
    }
}
