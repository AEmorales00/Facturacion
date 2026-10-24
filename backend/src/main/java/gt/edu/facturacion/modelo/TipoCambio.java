package gt.edu.facturacion.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Guarda lo que respondio el Web Service del Banguat cada dia.
 * Sirve para dos cosas: no consultar el WS a cada rato, y poder seguir
 * facturando con el ultimo valor conocido si el WS esta caido.
 */
@Entity
@Table(name = "tipo_cambio")
public class TipoCambio {

    /** La fecha es la llave: un solo registro por dia. */
    @Id
    private LocalDate fecha;

    @Column(nullable = false, precision = 10, scale = 5)
    private BigDecimal referencia;

    @Column(nullable = false, length = 20)
    private String origen = "Banguat WS";

    @Column(name = "consultado_en", nullable = false)
    private LocalDateTime consultadoEn = LocalDateTime.now();

    public TipoCambio() {
    }

    public TipoCambio(LocalDate fecha, BigDecimal referencia, String origen) {
        this.fecha = fecha;
        this.referencia = referencia;
        this.origen = origen;
        this.consultadoEn = LocalDateTime.now();
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getReferencia() {
        return referencia;
    }

    public void setReferencia(BigDecimal referencia) {
        this.referencia = referencia;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public LocalDateTime getConsultadoEn() {
        return consultadoEn;
    }

    public void setConsultadoEn(LocalDateTime consultadoEn) {
        this.consultadoEn = consultadoEn;
    }
}
