package gt.edu.facturacion.config;

import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * ============================================================
 *  CORS Y ARCHIVOS ESTATICOS
 * ------------------------------------------------------------
 *  CORS es el permiso que el navegador exige para que una pagina
 *  servida desde un dominio (el Angular) pueda llamar a una API
 *  que vive en otro dominio (el Spring Boot). Sin esto el
 *  navegador bloquea las peticiones aunque la API funcione.
 *
 *  Los dominios permitidos se configuran en la propiedad
 *  app.cors.origenes, para no tener que recompilar al desplegar.
 * ============================================================
 */
@Configuration
public class ConfiguracionWeb implements WebMvcConfigurer {

    private final String[] origenes;
    private final String carpetaImagenes;
    private final String urlBaseImagenes;

    public ConfiguracionWeb(
            @Value("${app.cors.origenes:http://localhost:4200}") String origenes,
            @Value("${app.local.carpeta:uploads}") String carpetaImagenes,
            @Value("${app.local.url-base:/imagenes}") String urlBaseImagenes) {

        this.origenes = origenes.split("\\s*,\\s*");
        this.carpetaImagenes = carpetaImagenes;
        this.urlBaseImagenes = urlBaseImagenes;
    }

    @Override
    public void addCorsMappings(CorsRegistry registro) {
        registro.addMapping("/api/**")
                .allowedOriginPatterns(origenes)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // Las imagenes del modo local tambien deben poder leerse desde Angular.
        registro.addMapping(urlBaseImagenes + "/**")
                .allowedOriginPatterns(origenes)
                .allowedMethods("GET");
    }

    /**
     * Publica la carpeta de imagenes del modo local como si fuera una ruta web.
     * En modo S3 esto no estorba: simplemente nunca se piden esas URLs.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        String ruta = Paths.get(carpetaImagenes).toAbsolutePath().normalize().toUri().toString();
        registro.addResourceHandler(urlBaseImagenes + "/**")
                .addResourceLocations(ruta);
    }
}
