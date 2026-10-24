package gt.edu.facturacion.servicio;

import org.springframework.web.multipart.MultipartFile;

/**
 * Contrato para guardar las fotografias de los productos.
 *
 * Hay dos implementaciones:
 *   - AlmacenamientoS3     -> sube al bucket de AWS S3 (lo que pide el proyecto)
 *   - AlmacenamientoLocal  -> guarda en una carpeta del disco
 *
 * Se elige con la propiedad app.almacenamiento (s3 | local). Gracias a esta
 * interfaz el resto del codigo es identico en los dos casos: nadie mas en la
 * aplicacion sabe si la imagen esta en S3 o en el disco.
 */
public interface AlmacenamientoImagenes {

    /**
     * Sube el archivo y devuelve la clave con la que quedo guardado.
     * Esa clave es lo unico que se escribe en la base de datos.
     */
    String subir(MultipartFile archivo);

    /** Arma la URL con la que el navegador puede mostrar la imagen. */
    String url(String clave);

    /** Borra el objeto. No falla si la clave ya no existe. */
    void eliminar(String clave);

    /** Nombre legible del backend activo, se muestra en /api/salud. */
    String descripcion();
}
