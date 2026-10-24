package gt.edu.facturacion.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Una fila del detalle: un producto vendido dentro de una factura. */
@Entity
@Table(name = "factura_detalle")
public class FacturaDetalle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "factura_id", nullable = false)
    private Factura factura;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad;

    /**
     * El precio se copia aqui al momento de facturar. Si manana el producto
     * sube de precio, la factura vieja sigue mostrando lo que se cobro.
     */
    @Column(name = "precio_unitario_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioUnitarioUsd;

    @Column(name = "subtotal_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalUsd;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
    }

    public Producto getProducto() {
        return producto;
    }

    public void setProducto(Producto producto) {
        this.producto = producto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitarioUsd() {
        return precioUnitarioUsd;
    }

    public void setPrecioUnitarioUsd(BigDecimal precioUnitarioUsd) {
        this.precioUnitarioUsd = precioUnitarioUsd;
    }

    public BigDecimal getSubtotalUsd() {
        return subtotalUsd;
    }

    public void setSubtotalUsd(BigDecimal subtotalUsd) {
        this.subtotalUsd = subtotalUsd;
    }
}
