package gt.edu.facturacion.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Producto del catalogo. Los precios se guardan en dolares y se convierten
 * a quetzales con el tipo de cambio del Banguat.
 *
 * Novedad del Proyecto 2: cada producto guarda la fotografia que vive en el
 * bucket de AWS S3. En la base de datos se guarda unicamente la "clave" del
 * objeto (ej. productos/abc.jpg) y la URL se arma al momento de leer.
 *
 * Se hace asi porque si el bucket es privado la URL va firmada y vence a las
 * pocas horas: guardarla en la BD dejaria enlaces rotos. La clave, en cambio,
 * no cambia nunca.
 */
@Entity
@Table(name = "productos")
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El codigo es obligatorio")
    @Size(max = 20)
    @Column(nullable = false, unique = true, length = 20)
    private String codigo;

    @NotBlank(message = "La descripcion es obligatoria")
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String descripcion;

    @NotNull(message = "El precio es obligatorio")
    @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a cero")
    @Column(name = "precio_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUsd;

    @NotNull(message = "El stock es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    @Column(nullable = false)
    private Integer stock = 0;

    @Column(name = "imagen_clave", length = 255)
    private String imagenClave;

    /** Se calcula a partir de imagenClave; no se guarda en la base de datos. */
    @Transient
    private String imagenUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPrecioUsd() {
        return precioUsd;
    }

    public void setPrecioUsd(BigDecimal precioUsd) {
        this.precioUsd = precioUsd;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public String getImagenClave() {
        return imagenClave;
    }

    public void setImagenClave(String imagenClave) {
        this.imagenClave = imagenClave;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }
}
