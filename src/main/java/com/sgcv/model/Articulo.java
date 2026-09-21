package com.sgcv.model;

import java.math.BigDecimal;

/**
 * Entidad de dominio que representa un bulto avícola mayorista cerrado estándar.
 */
public class Articulo {
    private int idArticulo;
    private String codigo;
    private String descripcion;
    private String unidadEmbalaje;
    private BigDecimal precioCostoReposicion;
    private BigDecimal precioVentaBase;
    private int stockDeposito;
    private int stockMinimoAlerta;
    private boolean activo;

    public Articulo() {
        this.precioCostoReposicion = BigDecimal.ZERO;
        this.precioVentaBase = BigDecimal.ZERO;
        this.activo = true;
    }

    public Articulo(int idArticulo, String codigo, String descripcion, String unidadEmbalaje,
                    BigDecimal precioCostoReposicion, BigDecimal precioVentaBase,
                    int stockDeposito, int stockMinimoAlerta, boolean activo) {
        this.idArticulo = idArticulo;
        this.codigo = codigo;
        this.descripcion = descripcion;
        this.unidadEmbalaje = unidadEmbalaje;
        this.precioCostoReposicion = precioCostoReposicion;
        this.precioVentaBase = precioVentaBase;
        this.stockDeposito = stockDeposito;
        this.stockMinimoAlerta = stockMinimoAlerta;
        this.activo = activo;
    }

    public boolean tieneStock(int cantidad) {
        return this.stockDeposito >= cantidad;
    }

    public int getIdArticulo() { return idArticulo; }
    public void setIdArticulo(int idArticulo) { this.idArticulo = idArticulo; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getUnidadEmbalaje() { return unidadEmbalaje; }
    public void setUnidadEmbalaje(String unidadEmbalaje) { this.unidadEmbalaje = unidadEmbalaje; }

    public BigDecimal getPrecioCostoReposicion() { return precioCostoReposicion; }
    public void setPrecioCostoReposicion(BigDecimal precioCostoReposicion) { this.precioCostoReposicion = precioCostoReposicion; }

    public BigDecimal getPrecioVentaBase() { return precioVentaBase; }
    public void setPrecioVentaBase(BigDecimal precioVentaBase) { this.precioVentaBase = precioVentaBase; }

    public int getStockDeposito() { return stockDeposito; }
    public void setStockDeposito(int stockDeposito) { this.stockDeposito = stockDeposito; }

    public int getStockMinimoAlerta() { return stockMinimoAlerta; }
    public void setStockMinimoAlerta(int stockMinimoAlerta) { this.stockMinimoAlerta = stockMinimoAlerta; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return codigo + " - " + descripcion + " (" + unidadEmbalaje + ") - $" + precioVentaBase + " [Stock: " + stockDeposito + "]";
    }
}
