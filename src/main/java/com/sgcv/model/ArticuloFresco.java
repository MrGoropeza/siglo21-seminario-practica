package com.sgcv.model;

import com.sgcv.exception.ValidacionException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Especialización de Articulo para productos avícolas frescos refrigerados de alta rotación.
 * Posee ciclo de vida perecedero corto y exige estricta conservación térmica entre 0°C y 4°C.
 */
public class ArticuloFresco extends Articulo {

    public static final String CATEGORIA = "REFRIGERADO_FRESCO (0°C a 4°C)";
    public static final int DIAS_CADUCIDAD_DEFECTO = 7;
    public static final double TEMPERATURA_MAX_DEFECTO = 4.0;

    private int diasCaducidad;
    private double temperaturaMaxC;

    public ArticuloFresco() {
        super();
        this.diasCaducidad = DIAS_CADUCIDAD_DEFECTO;
        this.temperaturaMaxC = TEMPERATURA_MAX_DEFECTO;
    }

    public ArticuloFresco(int idArticulo, String codigo, String descripcion, String unidadEmbalaje,
                          BigDecimal precioCostoReposicion, BigDecimal precioVentaBase,
                          int stockDeposito, int stockMinimoAlerta, boolean activo) {
        this(idArticulo, codigo, descripcion, unidadEmbalaje, precioCostoReposicion,
                precioVentaBase, stockDeposito, stockMinimoAlerta, activo,
                DIAS_CADUCIDAD_DEFECTO, TEMPERATURA_MAX_DEFECTO);
    }

    public ArticuloFresco(int idArticulo, String codigo, String descripcion, String unidadEmbalaje,
                          BigDecimal precioCostoReposicion, BigDecimal precioVentaBase,
                          int stockDeposito, int stockMinimoAlerta, boolean activo,
                          int diasCaducidad, double temperaturaMaxC) {
        super(idArticulo, codigo, descripcion, unidadEmbalaje, precioCostoReposicion,
                precioVentaBase, stockDeposito, stockMinimoAlerta, activo);
        if (diasCaducidad <= 0) {
            throw new ValidacionException("Los días de caducidad deben ser mayores a cero: " + diasCaducidad);
        }
        this.diasCaducidad = diasCaducidad;
        this.temperaturaMaxC = temperaturaMaxC;
    }

    /**
     * Regla de Negocio Polimórfica:
     * Si la cantidad de bultos es >= 10, se otorga un 5% de descuento por volumen y alta rotación.
     * De lo contrario, se aplica la tarifa base de lista.
     */
    @Override
    public BigDecimal calcularPrecioVentaFinal(int cantidadBultos) {
        BigDecimal base = getPrecioVentaBase() != null ? getPrecioVentaBase() : BigDecimal.ZERO;
        if (cantidadBultos >= 10) {
            // Factor 0.95 (5% de bonificación)
            return base.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getCategoriaConservacion() {
        return CATEGORIA;
    }

    @Override
    public int getDiasCaducidadRecomendada() {
        return this.diasCaducidad;
    }

    public int getDiasCaducidad() {
        return diasCaducidad;
    }

    public void setDiasCaducidad(int diasCaducidad) {
        if (diasCaducidad <= 0) {
            throw new ValidacionException("Los días de caducidad deben ser mayores a cero: " + diasCaducidad);
        }
        this.diasCaducidad = diasCaducidad;
    }

    public double getTemperaturaMaxC() {
        return temperaturaMaxC;
    }

    public void setTemperaturaMaxC(double temperaturaMaxC) {
        this.temperaturaMaxC = temperaturaMaxC;
    }
}
