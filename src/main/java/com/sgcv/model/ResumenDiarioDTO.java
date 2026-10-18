package com.sgcv.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO que consolida las métricas operativas y financieras del día para el Arqueo (CU-04).
 */
public class ResumenDiarioDTO {

    private LocalDate fecha;
    private int bultosEntregadosTotal;
    private BigDecimal facturacionEntregasTotal;
    private BigDecimal costoReposicionTotal; // Fondo a segregar para frigoríficos
    private BigDecimal margenBrutoEstimado;  // Ganancia real del día
    private BigDecimal cobranzasEfectivoTotal;
    private BigDecimal cobranzasTransferenciaTotal;
    private BigDecimal totalCobrado;
    private BigDecimal pagosProveedorEfectivoTotal;
    private BigDecimal pagosProveedorTransferenciaTotal;
    private BigDecimal saldoInicialEfectivo;
    private BigDecimal efectivoEsperado;

    public ResumenDiarioDTO() {
        this.fecha = LocalDate.now();
        this.facturacionEntregasTotal = BigDecimal.ZERO;
        this.costoReposicionTotal = BigDecimal.ZERO;
        this.margenBrutoEstimado = BigDecimal.ZERO;
        this.cobranzasEfectivoTotal = BigDecimal.ZERO;
        this.cobranzasTransferenciaTotal = BigDecimal.ZERO;
        this.totalCobrado = BigDecimal.ZERO;
        this.pagosProveedorEfectivoTotal = BigDecimal.ZERO;
        this.pagosProveedorTransferenciaTotal = BigDecimal.ZERO;
        this.saldoInicialEfectivo = BigDecimal.ZERO;
        this.efectivoEsperado = BigDecimal.ZERO;
    }

    public void recalcularEfectivoEsperado() {
        this.efectivoEsperado = this.saldoInicialEfectivo
                .add(this.cobranzasEfectivoTotal)
                .subtract(this.pagosProveedorEfectivoTotal);
        this.totalCobrado = this.cobranzasEfectivoTotal.add(this.cobranzasTransferenciaTotal);
        this.margenBrutoEstimado = this.facturacionEntregasTotal.subtract(this.costoReposicionTotal);
    }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public int getBultosEntregadosTotal() { return bultosEntregadosTotal; }
    public void setBultosEntregadosTotal(int bultosEntregadosTotal) { this.bultosEntregadosTotal = bultosEntregadosTotal; }

    public BigDecimal getFacturacionEntregasTotal() { return facturacionEntregasTotal; }
    public void setFacturacionEntregasTotal(BigDecimal facturacionEntregasTotal) { this.facturacionEntregasTotal = facturacionEntregasTotal; }

    public BigDecimal getCostoReposicionTotal() { return costoReposicionTotal; }
    public void setCostoReposicionTotal(BigDecimal costoReposicionTotal) { this.costoReposicionTotal = costoReposicionTotal; }

    public BigDecimal getMargenBrutoEstimado() { return margenBrutoEstimado; }
    public void setMargenBrutoEstimado(BigDecimal margenBrutoEstimado) { this.margenBrutoEstimado = margenBrutoEstimado; }

    public BigDecimal getCobranzasEfectivoTotal() { return cobranzasEfectivoTotal; }
    public void setCobranzasEfectivoTotal(BigDecimal cobranzasEfectivoTotal) { this.cobranzasEfectivoTotal = cobranzasEfectivoTotal; }

    public BigDecimal getCobranzasTransferenciaTotal() { return cobranzasTransferenciaTotal; }
    public void setCobranzasTransferenciaTotal(BigDecimal cobranzasTransferenciaTotal) { this.cobranzasTransferenciaTotal = cobranzasTransferenciaTotal; }

    public BigDecimal getTotalCobrado() { return totalCobrado; }
    public void setTotalCobrado(BigDecimal totalCobrado) { this.totalCobrado = totalCobrado; }

    public BigDecimal getPagosProveedorEfectivoTotal() { return pagosProveedorEfectivoTotal; }
    public void setPagosProveedorEfectivoTotal(BigDecimal pagosProveedorEfectivoTotal) { this.pagosProveedorEfectivoTotal = pagosProveedorEfectivoTotal; }

    public BigDecimal getPagosProveedorTransferenciaTotal() { return pagosProveedorTransferenciaTotal; }
    public void setPagosProveedorTransferenciaTotal(BigDecimal pagosProveedorTransferenciaTotal) { this.pagosProveedorTransferenciaTotal = pagosProveedorTransferenciaTotal; }

    public BigDecimal getSaldoInicialEfectivo() { return saldoInicialEfectivo; }
    public void setSaldoInicialEfectivo(BigDecimal saldoInicialEfectivo) { this.saldoInicialEfectivo = saldoInicialEfectivo; }

    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(BigDecimal efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }
}
