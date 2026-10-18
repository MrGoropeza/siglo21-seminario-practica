package com.sgcv.model;

import com.sgcv.exception.ValidacionException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de dominio que representa un Recibo Oficial de Cobranza a cliente minorista.
 * Soporta cobranza multi-tender (efectivo y/o transferencias).
 */
public class Cobranza {

    private int idCobranza;
    private String numeroRecibo;
    private int idCliente;
    private int idUsuario;
    private LocalDateTime fechaCobranza;
    private BigDecimal montoTotal;
    private String observaciones;
    private final List<DetalleCobranza> detalles;

    public Cobranza() {
        this.detalles = new ArrayList<>();
        this.montoTotal = BigDecimal.ZERO;
        this.fechaCobranza = LocalDateTime.now();
    }

    public Cobranza(int idCobranza, String numeroRecibo, int idCliente, int idUsuario,
                    LocalDateTime fechaCobranza, BigDecimal montoTotal, String observaciones) {
        if (numeroRecibo == null || numeroRecibo.trim().isEmpty()) {
            throw new ValidacionException("El número de recibo de cobranza no puede ser vacío.");
        }
        if (idCliente <= 0) {
            throw new ValidacionException("Debe asociar un cliente válido a la cobranza.");
        }
        this.idCobranza = idCobranza;
        this.numeroRecibo = numeroRecibo.trim();
        this.idCliente = idCliente;
        this.idUsuario = idUsuario > 0 ? idUsuario : 1;
        this.fechaCobranza = fechaCobranza != null ? fechaCobranza : LocalDateTime.now();
        this.montoTotal = montoTotal != null ? montoTotal : BigDecimal.ZERO;
        this.observaciones = observaciones;
        this.detalles = new ArrayList<>();
    }

    public void agregarDetalle(DetalleCobranza detalle) {
        if (detalle == null) return;
        detalles.add(detalle);
        recalcularTotal();
    }

    public void recalcularTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCobranza d : detalles) {
            if (d.getMonto() != null) {
                total = total.add(d.getMonto());
            }
        }
        this.montoTotal = total;
    }

    public BigDecimal getTotalEfectivo() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCobranza d : detalles) {
            if ("EFECTIVO".equalsIgnoreCase(d.getMedioPago()) && d.getMonto() != null) {
                total = total.add(d.getMonto());
            }
        }
        return total;
    }

    public BigDecimal getTotalTransferencia() {
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleCobranza d : detalles) {
            if ("TRANSFERENCIA".equalsIgnoreCase(d.getMedioPago()) && d.getMonto() != null) {
                total = total.add(d.getMonto());
            }
        }
        return total;
    }

    public int getIdCobranza() { return idCobranza; }
    public void setIdCobranza(int idCobranza) { this.idCobranza = idCobranza; }

    public String getNumeroRecibo() { return numeroRecibo; }
    public void setNumeroRecibo(String numeroRecibo) { this.numeroRecibo = numeroRecibo; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public LocalDateTime getFechaCobranza() { return fechaCobranza; }
    public void setFechaCobranza(LocalDateTime fechaCobranza) { this.fechaCobranza = fechaCobranza; }

    public BigDecimal getMontoTotal() { return montoTotal; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public List<DetalleCobranza> getDetalles() { return detalles; }
}
