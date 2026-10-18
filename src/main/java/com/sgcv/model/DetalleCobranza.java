package com.sgcv.model;

import com.sgcv.exception.ValidacionException;

import java.math.BigDecimal;

/**
 * Renglón de desglose de medio de pago para una cobranza (Efectivo o Transferencia).
 */
public class DetalleCobranza {

    private int idDetalleCobranza;
    private int idCobranza;
    private String medioPago; // 'EFECTIVO' o 'TRANSFERENCIA'
    private BigDecimal monto;
    private String referenciaOperacion;

    public DetalleCobranza() {
        this.medioPago = "EFECTIVO";
        this.monto = BigDecimal.ZERO;
    }

    public DetalleCobranza(int idDetalleCobranza, int idCobranza, String medioPago,
                           BigDecimal monto, String referenciaOperacion) {
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("El importe del medio de pago debe ser positivo.");
        }
        if (medioPago == null || (!medioPago.equalsIgnoreCase("EFECTIVO") && !medioPago.equalsIgnoreCase("TRANSFERENCIA"))) {
            throw new ValidacionException("Medio de pago no válido: " + medioPago + " (use EFECTIVO o TRANSFERENCIA).");
        }
        this.idDetalleCobranza = idDetalleCobranza;
        this.idCobranza = idCobranza;
        this.medioPago = medioPago.toUpperCase();
        this.monto = monto;
        this.referenciaOperacion = referenciaOperacion;
    }

    public DetalleCobranza(String medioPago, BigDecimal monto, String referenciaOperacion) {
        this(0, 0, medioPago, monto, referenciaOperacion);
    }


    public int getIdDetalleCobranza() { return idDetalleCobranza; }
    public void setIdDetalleCobranza(int idDetalleCobranza) { this.idDetalleCobranza = idDetalleCobranza; }

    public int getIdCobranza() { return idCobranza; }
    public void setIdCobranza(int idCobranza) { this.idCobranza = idCobranza; }

    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago != null ? medioPago.toUpperCase() : "EFECTIVO"; }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }

    public String getReferenciaOperacion() { return referenciaOperacion; }
    public void setReferenciaOperacion(String referenciaOperacion) { this.referenciaOperacion = referenciaOperacion; }
}
