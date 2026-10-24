package gt.edu.facturacion.modelo;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encabezado de la factura.
 *
 * El tipo de cambio se guarda DENTRO de la factura a proposito: el valor del
 * Banguat cambia todos los dias, pero la factura debe conservar para siempre
 * el valor con el que realmente se cobro.
 */
@Entity
@Table(name = "facturas")
public class Factura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(name = "metodo_pago", nullable = false, length = 20)
    private String metodoPago = "Efectivo";

    /** 1 USD = X GTQ, segun el Web Service del Banco de Guatemala. */
    @Column(name = "tipo_cambio", nullable = false, precision = 10, scale = 5)
    private BigDecimal tipoCambio;

    @Column(name = "subtotal_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalUsd;

    @Column(name = "iva_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal ivaUsd;

    @Column(name = "total_usd", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalUsd;

    @Column(name = "total_gtq", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalGtq;

    /**
     * orphanRemoval + CascadeType.ALL hacen que el detalle se guarde y se borre
     * junto con su factura, sin tener que manejarlo aparte.
     */
    @OneToMany(mappedBy = "factura", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<FacturaDetalle> detalle = new ArrayList<>();

    /** Agrega una linea y deja los dos lados de la relacion apuntandose. */
    public void agregarLinea(FacturaDetalle linea) {
        linea.setFactura(this);
        this.detalle.add(linea);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public BigDecimal getTipoCambio() {
        return tipoCambio;
    }

    public void setTipoCambio(BigDecimal tipoCambio) {
        this.tipoCambio = tipoCambio;
    }

    public BigDecimal getSubtotalUsd() {
        return subtotalUsd;
    }

    public void setSubtotalUsd(BigDecimal subtotalUsd) {
        this.subtotalUsd = subtotalUsd;
    }

    public BigDecimal getIvaUsd() {
        return ivaUsd;
    }

    public void setIvaUsd(BigDecimal ivaUsd) {
        this.ivaUsd = ivaUsd;
    }

    public BigDecimal getTotalUsd() {
        return totalUsd;
    }

    public void setTotalUsd(BigDecimal totalUsd) {
        this.totalUsd = totalUsd;
    }

    public BigDecimal getTotalGtq() {
        return totalGtq;
    }

    public void setTotalGtq(BigDecimal totalGtq) {
        this.totalGtq = totalGtq;
    }

    public List<FacturaDetalle> getDetalle() {
        return detalle;
    }

    public void setDetalle(List<FacturaDetalle> detalle) {
        this.detalle = detalle;
    }
}
