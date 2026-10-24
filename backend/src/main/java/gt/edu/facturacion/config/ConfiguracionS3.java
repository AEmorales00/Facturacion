package gt.edu.facturacion.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Crea los clientes de AWS. Solo se activan cuando app.almacenamiento=s3,
 * para que la aplicacion arranque sin problemas mientras se trabaja en local
 * sin credenciales de AWS.
 *
 * DefaultCredentialsProvider busca las credenciales en este orden:
 *   1. Variables de entorno AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
 *   2. El archivo ~/.aws/credentials
 *   3. El rol IAM de la instancia (lo normal en produccion)
 */
@Configuration
@ConditionalOnProperty(name = "app.almacenamiento", havingValue = "s3")
public class ConfiguracionS3 {

    @Bean
    public S3Client s3Client(PropiedadesS3 propiedades) {
        return S3Client.builder()
                .region(Region.of(propiedades.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner(PropiedadesS3 propiedades) {
        return S3Presigner.builder()
                .region(Region.of(propiedades.getRegion()))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
