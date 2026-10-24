package gt.edu.facturacion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuracion del bucket de AWS S3. Se lee de application.properties o, en
 * produccion, de las variables de entorno del servidor de hosting.
 *
 * Las credenciales (access key / secret key) NO van aqui: las resuelve el SDK
 * de AWS por su cuenta leyendo las variables de entorno AWS_ACCESS_KEY_ID y
 * AWS_SECRET_ACCESS_KEY, o el rol IAM de la maquina. De esa forma nunca
 * terminan subidas al repositorio de git.
 */
@Component
@ConfigurationProperties(prefix = "app.s3")
public class PropiedadesS3 {

    /** Nombre del bucket, por ejemplo "facturacion-productos-usac". */
    private String bucket = "";

    /** Region donde se creo el bucket, por ejemplo us-east-1. */
    private String region = "us-east-1";

    /** Carpeta dentro del bucket donde se guardan las fotos. */
    private String prefijo = "productos/";

    /**
     * true  -> el bucket es privado y se firman URLs temporales (recomendado).
     * false -> el bucket es publico y la URL es directa y permanente.
     */
    private boolean urlsFirmadas = true;

    /** Cuanto dura una URL firmada antes de vencer. */
    private long minutosFirma = 60;

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPrefijo() {
        return prefijo;
    }

    public void setPrefijo(String prefijo) {
        this.prefijo = prefijo;
    }

    public boolean isUrlsFirmadas() {
        return urlsFirmadas;
    }

    public void setUrlsFirmadas(boolean urlsFirmadas) {
        this.urlsFirmadas = urlsFirmadas;
    }

    public long getMinutosFirma() {
        return minutosFirma;
    }

    public void setMinutosFirma(long minutosFirma) {
        this.minutosFirma = minutosFirma;
    }
}
