package com.sgcv.model;

import com.sgcv.exception.ValidacionException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Especialización de Articulo para productos avícolas congelados individualmente (IQF)
 * o supercongelados que requieren mantenimiento estricto de -18°C.
 * Posee un ciclo de vida prolongado (180 días) y estructura de pricing adaptada al costo de frío.
 */
public class ArticuloCongelado extends Articulo {

    public static final String CATEGORIA = "CONGELADO_IQF (-18°C)";
    public static final int DIAS_CADUCIDAD_DEFECTO = 180;
    public static final double TEMPERATURA_CONGELACION_DEFECTO = -18.0;
    public static final BigDecimal RECARGO_FRIO_DEFECTO = new BigDecimal("3.00");

    private double temperaturaCongelacionC;
    private BigDecimal recargoCadenaFrioPorcentaje;

    public ArticuloCongelado() {
        super();
        this.temperaturaCongelacionC = TEMPERATURA_CONGELACION_DEFECTO;
        this.recargoCadenaFrioPorcentaje = RECARGO_FRIO_DEFECTO;
    }

    public ArticuloCongelado(int idArticulo, String codigo, String descripcion, String unidadEmbalaje,
                             BigDecimal precioCostoReposicion, BigDecimal precioVentaBase,
                             int stockDeposito, int stockMinimoAlerta, boolean activo) {
        this(idArticulo, codigo, descripcion, unidadEmbalaje, precioCostoReposicion,
                precioVentaBase, stockDeposito, stockMinimoAlerta, activo,
                TEMPERATURA_CONGELACION_DEFECTO, RECARGO_FRIO_DEFECTO);
    }

    public ArticuloCongelado(int idArticulo, String codigo, String descripcion, String unidadEmbalaje,
                             BigDecimal precioCostoReposicion, BigDecimal precioVentaBase,
                             int stockDeposito, int stockMinimoAlerta, boolean activo,
                             double temperaturaCongelacionC, BigDecimal recargoCadenaFrioPorcentaje) {
        super(idArticulo, codigo, descripcion, unidadEmbalaje, precioCostoReposicion,
                precioVentaBase, stockDeposito, stockMinimoAlerta, activo);
        if (recargoCadenaFrioPorcentaje == null || recargoCadenaFrioPorcentaje.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El porcentaje de recargo de frío no puede ser negativo.");
        }
        this.temperaturaCongelacionC = temperaturaCongelacionC;
        this.recargoCadenaFrioPorcentaje = recargoCadenaFrioPorcentaje;
    }

    /**
     * Regla de Negocio Polimórfica:
     * - Si cantidadBultos >= 20: se aplica un 4% de descuento mayorista por escala.
     * - Si cantidadBultos < 5: se aplica un recargo porcentual por preservación de frío (3%).
     * - De lo contrario: se factura al precio base estándar.
     */
    @Override
    public BigDecimal calcularPrecioVentaFinal(int cantidadBultos) {
        BigDecimal base = getPrecioVentaBase() != null ? getPrecioVentaBase() : BigDecimal.ZERO;
        if (cantidadBultos >= 20) {
            // Factor 0.96 (4% de bonificación por volumen mayorista)
            return base.multiply(new BigDecimal("0.96")).setScale(2, RoundingMode.HALF_UP);
        } else if (cantidadBultos < 5) {
            // Recargo por logística térmica en compras pequeñas
            BigDecimal pct = recargoCadenaFrioPorcentaje != null ? recargoCadenaFrioPorcentaje : RECARGO_FRIO_DEFECTO;
            BigDecimal factor = pct.compareTo(BigDecimal.ONE) > 0
                    ? pct.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                    : pct;
            BigDecimal recargo = base.multiply(factor);
            return base.add(recargo).setScale(2, RoundingMode.HALF_UP);
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getCategoriaConservacion() {
        return CATEGORIA;
    }

    @Override
    public int getDiasCaducidadRecomendada() {
        return DIAS_CADUCIDAD_DEFECTO;
    }

    public double getTemperaturaCongelacionC() {
        return temperaturaCongelacionC;
    }

    public void setTemperaturaCongelacionC(double temperaturaCongelacionC) {
        this.temperaturaCongelacionC = temperaturaCongelacionC;
    }

    public BigDecimal getRecargoCadenaFrioPorcentaje() {
        return recargoCadenaFrioPorcentaje;
    }

    public void setRecargoCadenaFrioPorcentaje(BigDecimal recargoCadenaFrioPorcentaje) {
        if (recargoCadenaFrioPorcentaje == null || recargoCadenaFrioPorcentaje.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El porcentaje de recargo de frío no puede ser negativo.");
        }
        this.recargoCadenaFrioPorcentaje = recargoCadenaFrioPorcentaje;
    }
}
