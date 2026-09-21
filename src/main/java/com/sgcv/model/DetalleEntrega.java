package com.sgcv.model;

import java.math.BigDecimal;

/**
 * Renglón de entrega de mercadería con snapshot temporal inmutable de precios.
 */
public class DetalleEntrega {
    private int idDetalleEntrega;
    private int idEntrega;
    private int idArticulo;
    private String codigoArticulo;
    private String descripcionArticulo;
    private int cantidadBultos;
    private BigDecimal precioUnitarioCongelado;
    private BigDecimal costoUnitarioCongelado;
    private BigDecimal descuentoUnitario;
    private BigDecimal subtotal;

    public DetalleEntrega() {
        this.precioUnitarioCongelado = BigDecimal.ZERO;
        this.costoUnitarioCongelado = BigDecimal.ZERO;
        this.descuentoUnitario = BigDecimal.ZERO;
        this.subtotal = BigDecimal.ZERO;
    }

    public DetalleEntrega(int idArticulo, String codigoArticulo, String descripcionArticulo,
                          int cantidadBultos, BigDecimal precioUnitario, BigDecimal costoUnitario,
                          BigDecimal descuentoUnitario) {
        this.idArticulo = idArticulo;
        this.codigoArticulo = codigoArticulo;
        this.descripcionArticulo = descripcionArticulo;
        this.cantidadBultos = cantidadBultos;
        this.precioUnitarioCongelado = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
        this.costoUnitarioCongelado = costoUnitario != null ? costoUnitario : BigDecimal.ZERO;
        this.descuentoUnitario = descuentoUnitario != null ? descuentoUnitario : BigDecimal.ZERO;
        this.subtotal = calcularSubtotal();
    }

    public BigDecimal calcularSubtotal() {
        BigDecimal precioEfectivo = precioUnitarioCongelado.subtract(descuentoUnitario);
        if (precioEfectivo.compareTo(BigDecimal.ZERO) < 0) precioEfectivo = BigDecimal.ZERO;
        return precioEfectivo.multiply(BigDecimal.valueOf(cantidadBultos));
    }

    public int getIdDetalleEntrega() { return idDetalleEntrega; }
    public void setIdDetalleEntrega(int idDetalleEntrega) { this.idDetalleEntrega = idDetalleEntrega; }

    public int getIdEntrega() { return idEntrega; }
    public void setIdEntrega(int idEntrega) { this.idEntrega = idEntrega; }

    public int getIdArticulo() { return idArticulo; }
    public void setIdArticulo(int idArticulo) { this.idArticulo = idArticulo; }

    public String getCodigoArticulo() { return codigoArticulo; }
    public void setCodigoArticulo(String codigoArticulo) { this.codigoArticulo = codigoArticulo; }

    public String getDescripcionArticulo() { return descripcionArticulo; }
    public void setDescripcionArticulo(String descripcionArticulo) { this.descripcionArticulo = descripcionArticulo; }

    public int getCantidadBultos() { return cantidadBultos; }
    public void setCantidadBultos(int cantidadBultos) { 
        this.cantidadBultos = cantidadBultos; 
        this.subtotal = calcularSubtotal();
    }

    public BigDecimal getPrecioUnitarioCongelado() { return precioUnitarioCongelado; }
    public void setPrecioUnitarioCongelado(BigDecimal precioUnitarioCongelado) { 
        this.precioUnitarioCongelado = precioUnitarioCongelado; 
        this.subtotal = calcularSubtotal();
    }

    public BigDecimal getCostoUnitarioCongelado() { return costoUnitarioCongelado; }
    public void setCostoUnitarioCongelado(BigDecimal costoUnitarioCongelado) { this.costoUnitarioCongelado = costoUnitarioCongelado; }

    public BigDecimal getDescuentoUnitario() { return descuentoUnitario; }
    public void setDescuentoUnitario(BigDecimal descuentoUnitario) { 
        this.descuentoUnitario = descuentoUnitario; 
        this.subtotal = calcularSubtotal();
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
