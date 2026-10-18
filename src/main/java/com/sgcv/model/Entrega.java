package com.sgcv.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entidad que representa el remito oficial de entrega de mercadería en ruta de distribución.
 * Agrupa los renglones de bultos avícolas y consolida la facturación total.
 */
public class Entrega {

    private int idEntrega;
    private String numeroRemito;
    private int idCliente;
    private int idUsuario;
    private LocalDateTime fechaEntrega;
    private BigDecimal totalEntrega;
    private String estado;
    private String observaciones;
    private List<DetalleEntrega> detalles;

    public Entrega() {
        this.fechaEntrega = LocalDateTime.now();
        this.totalEntrega = BigDecimal.ZERO;
        this.estado = "EMITIDA";
        this.detalles = new ArrayList<>();
    }

    public void agregarDetalle(DetalleEntrega detalle) {
        if (detalle != null) {
            this.detalles.add(detalle);
            recalcularTotal();
        }
    }

    public void recalcularTotal() {
        BigDecimal acumulado = BigDecimal.ZERO;
        if (detalles != null) {
            for (DetalleEntrega d : detalles) {
                if (d != null && d.getSubtotal() != null) {
                    acumulado = acumulado.add(d.getSubtotal());
                }
            }
        }
        this.totalEntrega = acumulado.setScale(2, RoundingMode.HALF_UP);
    }

    public int getIdEntrega() { return idEntrega; }
    public void setIdEntrega(int idEntrega) { this.idEntrega = idEntrega; }

    public String getNumeroRemito() { return numeroRemito; }
    public void setNumeroRemito(String numeroRemito) { this.numeroRemito = numeroRemito; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public LocalDateTime getFechaEntrega() { return fechaEntrega; }
    public void setFechaEntrega(LocalDateTime fechaEntrega) { this.fechaEntrega = fechaEntrega; }

    public BigDecimal getTotalEntrega() { return totalEntrega; }
    public void setTotalEntrega(BigDecimal totalEntrega) {
        this.totalEntrega = totalEntrega != null ? totalEntrega.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public List<DetalleEntrega> getDetalles() {
        return detalles != null ? detalles : Collections.emptyList();
    }

    public void setDetalles(List<DetalleEntrega> detalles) { 
        this.detalles = detalles != null ? detalles : new ArrayList<>();
        recalcularTotal();
    }
}
