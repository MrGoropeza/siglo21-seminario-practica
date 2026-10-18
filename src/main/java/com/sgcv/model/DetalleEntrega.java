package com.sgcv.model;

import com.sgcv.exception.ValidacionException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Renglón de entrega de mercadería que captura una instantánea inmutable (snapshot)
 * de precios unitarios y costos al momento exacto de la emisión del remito.
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
        if (cantidadBultos <= 0) {
            throw new ValidacionException("La cantidad de bultos debe ser mayor a cero: " + cantidadBultos);
        }
        this.idArticulo = idArticulo;
        this.codigoArticulo = codigoArticulo != null ? codigoArticulo.trim() : "";
        this.descripcionArticulo = descripcionArticulo != null ? descripcionArticulo.trim() : "";
        this.cantidadBultos = cantidadBultos;
        this.precioUnitarioCongelado = precioUnitario != null ? precioUnitario : BigDecimal.ZERO;
        this.costoUnitarioCongelado = costoUnitario != null ? costoUnitario : BigDecimal.ZERO;
        this.descuentoUnitario = descuentoUnitario != null ? descuentoUnitario : BigDecimal.ZERO;
        this.subtotal = calcularSubtotal();
    }

    public DetalleEntrega(int idArticulo, int cantidadBultos, BigDecimal precioUnitario,
                          BigDecimal costoUnitario, BigDecimal descuentoUnitario) {
        this(idArticulo, "", "", cantidadBultos, precioUnitario, costoUnitario, descuentoUnitario);
    }


    /**
     * Calcula el subtotal aplicando descuentos unitarios y multiplicando por el volumen.
     * @return subtotal neto del renglón (mínimo cero)
     */
    public BigDecimal calcularSubtotal() {
        BigDecimal unitario = precioUnitarioCongelado != null ? precioUnitarioCongelado : BigDecimal.ZERO;
        BigDecimal descuento = descuentoUnitario != null ? descuentoUnitario : BigDecimal.ZERO;
        BigDecimal precioEfectivo = unitario.subtract(descuento);
        if (precioEfectivo.compareTo(BigDecimal.ZERO) < 0) {
            precioEfectivo = BigDecimal.ZERO;
        }
        return precioEfectivo.multiply(BigDecimal.valueOf(cantidadBultos)).setScale(2, RoundingMode.HALF_UP);
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
        if (cantidadBultos <= 0) {
            throw new ValidacionException("La cantidad de bultos debe ser mayor a cero: " + cantidadBultos);
        }
        this.cantidadBultos = cantidadBultos; 
        this.subtotal = calcularSubtotal();
    }

    public BigDecimal getPrecioUnitarioCongelado() { return precioUnitarioCongelado; }
    public void setPrecioUnitarioCongelado(BigDecimal precioUnitarioCongelado) { 
        this.precioUnitarioCongelado = precioUnitarioCongelado != null ? precioUnitarioCongelado : BigDecimal.ZERO; 
        this.subtotal = calcularSubtotal();
    }

    public BigDecimal getCostoUnitarioCongelado() { return costoUnitarioCongelado; }
    public void setCostoUnitarioCongelado(BigDecimal costoUnitarioCongelado) { 
        this.costoUnitarioCongelado = costoUnitarioCongelado != null ? costoUnitarioCongelado : BigDecimal.ZERO; 
    }

    public BigDecimal getDescuentoUnitario() { return descuentoUnitario; }
    public void setDescuentoUnitario(BigDecimal descuentoUnitario) { 
        this.descuentoUnitario = descuentoUnitario != null ? descuentoUnitario : BigDecimal.ZERO; 
        this.subtotal = calcularSubtotal();
    }

    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
