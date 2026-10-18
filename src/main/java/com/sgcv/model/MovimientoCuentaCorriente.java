package com.sgcv.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO que representa un renglón del Libro Mayor / Extracto de Cuenta Corriente del Cliente.
 * Registra débitos (remitos entregados) y créditos (cobranzas recibidas) con saldo progresivo.
 */
public class MovimientoCuentaCorriente {

    private LocalDateTime fecha;
    private String tipoComprobante; // "REMITO" o "RECIBO"
    private String numeroComprobante;
    private String concepto;
    private BigDecimal debe;   // Incrementa la deuda (Remito de mercadería)
    private BigDecimal haber;  // Reduce la deuda (Pago recibido en efectivo o transferencia)
    private BigDecimal saldoAcumulado;

    public MovimientoCuentaCorriente() {
        this.debe = BigDecimal.ZERO;
        this.haber = BigDecimal.ZERO;
        this.saldoAcumulado = BigDecimal.ZERO;
    }

    public MovimientoCuentaCorriente(LocalDateTime fecha, String tipoComprobante, String numeroComprobante,
                                     String concepto, BigDecimal debe, BigDecimal haber, BigDecimal saldoAcumulado) {
        this.fecha = fecha != null ? fecha : LocalDateTime.now();
        this.tipoComprobante = tipoComprobante;
        this.numeroComprobante = numeroComprobante;
        this.concepto = concepto;
        this.debe = debe != null ? debe : BigDecimal.ZERO;
        this.haber = haber != null ? haber : BigDecimal.ZERO;
        this.saldoAcumulado = saldoAcumulado != null ? saldoAcumulado : BigDecimal.ZERO;
    }

    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }

    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String tipoComprobante) { this.tipoComprobante = tipoComprobante; }

    public String getTipo() { return tipoComprobante; }

    public String getNumeroComprobante() { return numeroComprobante; }
    public void setNumeroComprobante(String numeroComprobante) { this.numeroComprobante = numeroComprobante; }

    public String getComprobante() { return numeroComprobante; }


    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }

    public BigDecimal getDebe() { return debe; }
    public void setDebe(BigDecimal debe) { this.debe = debe; }

    public BigDecimal getHaber() { return haber; }
    public void setHaber(BigDecimal haber) { this.haber = haber; }

    public BigDecimal getSaldoAcumulado() { return saldoAcumulado; }
    public void setSaldoAcumulado(BigDecimal saldoAcumulado) { this.saldoAcumulado = saldoAcumulado; }
}
