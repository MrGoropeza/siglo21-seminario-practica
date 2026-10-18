package com.sgcv.model;

import java.math.BigDecimal;

/**
 * Resultado transaccional de una parada comercial en ruta (CU-01 + CU-02).
 */
public class ParadaResult {

    private int idEntrega;
    private String numeroRemito;
    private int idCobranza;
    private String numeroRecibo;
    private BigDecimal totalEntrega = BigDecimal.ZERO;
    private BigDecimal totalCobrado = BigDecimal.ZERO;
    private BigDecimal nuevoSaldoCliente = BigDecimal.ZERO;
    private String mensaje;

    public ParadaResult() {}

    public int getIdEntrega() { return idEntrega; }
    public void setIdEntrega(int idEntrega) { this.idEntrega = idEntrega; }

    public String getNumeroRemito() { return numeroRemito; }
    public void setNumeroRemito(String numeroRemito) { this.numeroRemito = numeroRemito; }

    public int getIdCobranza() { return idCobranza; }
    public void setIdCobranza(int idCobranza) { this.idCobranza = idCobranza; }

    public String getNumeroRecibo() { return numeroRecibo; }
    public void setNumeroRecibo(String numeroRecibo) { this.numeroRecibo = numeroRecibo; }

    public BigDecimal getTotalEntrega() { return totalEntrega; }
    public void setTotalEntrega(BigDecimal totalEntrega) { this.totalEntrega = totalEntrega; }

    public BigDecimal getTotalCobrado() { return totalCobrado; }
    public void setTotalCobrado(BigDecimal totalCobrado) { this.totalCobrado = totalCobrado; }

    public BigDecimal getNuevoSaldoCliente() { return nuevoSaldoCliente; }
    public void setNuevoSaldoCliente(BigDecimal nuevoSaldoCliente) { this.nuevoSaldoCliente = nuevoSaldoCliente; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public boolean huboEntrega() { return idEntrega > 0; }
    public boolean huboCobranza() { return idCobranza > 0; }
}
