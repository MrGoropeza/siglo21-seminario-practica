package com.sgcv.model;

import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad base abstracta que modela un bulto avícola mayorista cerrado estándar.
 * Se alinea 100% con el Modelo Entidad-Relación (DER) y la tabla relacional 'articulos'.
 * Aplica principios fundamentales de Programación Orientada a Objetos:
 * Encapsulamiento con invariantes de dominio y polimorfismo comercial.
 */
public abstract class Articulo {

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
        this.stockDeposito = 0;
        this.stockMinimoAlerta = 0;
        this.activo = true;
    }

    public Articulo(int idArticulo, String codigo, String descripcion, String unidadEmbalaje,
                    BigDecimal precioCostoReposicion, BigDecimal precioVentaBase,
                    int stockDeposito, int stockMinimoAlerta, boolean activo) {
        validarParametros(codigo, descripcion, precioCostoReposicion, precioVentaBase, stockDeposito, stockMinimoAlerta);
        this.idArticulo = idArticulo;
        this.codigo = codigo.trim();
        this.descripcion = descripcion.trim();
        this.unidadEmbalaje = unidadEmbalaje != null ? unidadEmbalaje.trim() : "";
        this.precioCostoReposicion = precioCostoReposicion;
        this.precioVentaBase = precioVentaBase;
        this.stockDeposito = stockDeposito;
        this.stockMinimoAlerta = stockMinimoAlerta;
        this.activo = activo;
    }

    private static void validarParametros(String codigo, String descripcion,
                                          BigDecimal costo, BigDecimal venta,
                                          int stock, int stockMinimo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new ValidacionException("El código del artículo no puede ser nulo ni vacío.");
        }
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new ValidacionException("La descripción del artículo no puede ser nula ni vacía.");
        }
        if (costo == null || costo.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El costo de reposición no puede ser nulo ni negativo.");
        }
        if (venta == null || venta.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El precio de venta base no puede ser nulo ni negativo.");
        }
        if (stock < 0) {
            throw new ValidacionException("El stock en depósito no puede ser negativo: " + stock);
        }
        if (stockMinimo < 0) {
            throw new ValidacionException("El stock mínimo de alerta no puede ser negativo: " + stockMinimo);
        }
    }

    // =========================================================================
    // Métodos concretos de comportamiento de dominio
    // =========================================================================

    public boolean tieneStock(int cantidad) {
        return cantidad > 0 && this.stockDeposito >= cantidad;
    }

    public void descontarStock(int cantidad) throws StockInsuficienteException {
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad de bultos a descontar debe ser mayor a cero: " + cantidad);
        }
        if (!tieneStock(cantidad)) {
            throw new StockInsuficienteException(this.codigo, this.stockDeposito, cantidad);
        }
        this.stockDeposito -= cantidad;
    }

    public void reponerStock(int cantidad) {
        if (cantidad <= 0) {
            throw new ValidacionException("La cantidad a reponer debe ser mayor a cero: " + cantidad);
        }
        this.stockDeposito += cantidad;
    }

    // =========================================================================
    // Métodos abstractos polimórficos
    // =========================================================================

    /**
     * Calcula polimórficamente el precio unitario final de venta considerando la
     * categoría comercial del artículo (estándar vs promocional por volumen).
     * @param cantidadBultos volumen solicitado en bultos cerrados
     * @return precio unitario final con descuentos comerciales aplicados
     */
    public abstract BigDecimal calcularPrecioVentaFinal(int cantidadBultos);

    /**
     * Denominación de la modalidad comercial del artículo en el catálogo mayorista.
     * @return tipo comercial del artículo ("ESTÁNDAR" o "PROMOCIÓN VOLUMEN")
     */
    public abstract String getTipoArticulo();

    // =========================================================================
    // Getters y Setters con validación de invariantes
    // =========================================================================

    public int getIdArticulo() { return idArticulo; }
    public void setIdArticulo(int idArticulo) { this.idArticulo = idArticulo; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new ValidacionException("El código del artículo no puede ser nulo ni vacío.");
        }
        this.codigo = codigo.trim();
    }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) {
        if (descripcion == null || descripcion.trim().isEmpty()) {
            throw new ValidacionException("La descripción del artículo no puede ser nula ni vacía.");
        }
        this.descripcion = descripcion.trim();
    }

    public String getUnidadEmbalaje() { return unidadEmbalaje; }
    public void setUnidadEmbalaje(String unidadEmbalaje) {
        this.unidadEmbalaje = unidadEmbalaje != null ? unidadEmbalaje.trim() : "";
    }

    public BigDecimal getPrecioCostoReposicion() { return precioCostoReposicion; }
    public void setPrecioCostoReposicion(BigDecimal precioCostoReposicion) {
        if (precioCostoReposicion == null || precioCostoReposicion.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El precio de costo de reposición no puede ser negativo.");
        }
        this.precioCostoReposicion = precioCostoReposicion;
    }

    public BigDecimal getPrecioVentaBase() { return precioVentaBase; }
    public void setPrecioVentaBase(BigDecimal precioVentaBase) {
        if (precioVentaBase == null || precioVentaBase.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El precio de venta base no puede ser negativo.");
        }
        this.precioVentaBase = precioVentaBase;
    }

    public int getStockDeposito() { return stockDeposito; }
    public void setStockDeposito(int stockDeposito) {
        if (stockDeposito < 0) {
            throw new ValidacionException("El stock en depósito no puede ser negativo: " + stockDeposito);
        }
        this.stockDeposito = stockDeposito;
    }

    public int getStockMinimoAlerta() { return stockMinimoAlerta; }
    public void setStockMinimoAlerta(int stockMinimoAlerta) {
        if (stockMinimoAlerta < 0) {
            throw new ValidacionException("El stock mínimo de alerta no puede ser negativo: " + stockMinimoAlerta);
        }
        this.stockMinimoAlerta = stockMinimoAlerta;
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Articulo articulo = (Articulo) o;
        return idArticulo == articulo.idArticulo && Objects.equals(codigo, articulo.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idArticulo, codigo);
    }

    @Override
    public String toString() {
        return codigo + " - " + descripcion + " (" + unidadEmbalaje + ") - $" + precioVentaBase +
                " [Stock: " + stockDeposito + " | " + getTipoArticulo() + "]";
    }
}
