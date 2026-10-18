package com.sgcv.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Especialización de Articulo para bultos avícolas comerciales de tarifa estándar.
 * Se comercializa al precio de lista vigente sin bonificaciones de escala.
 */
public class ArticuloEstandar extends Articulo {

    public static final String TIPO = "ESTÁNDAR";

    public ArticuloEstandar() {
        super();
    }

    public ArticuloEstandar(int idArticulo, String codigo, String descripcion, String unidadEmbalaje,
                            BigDecimal precioCostoReposicion, BigDecimal precioVentaBase,
                            int stockDeposito, int stockMinimoAlerta, boolean activo) {
        super(idArticulo, codigo, descripcion, unidadEmbalaje, precioCostoReposicion,
                precioVentaBase, stockDeposito, stockMinimoAlerta, activo);
    }

    /**
     * Facturación a precio de venta base de lista sin modificaciones.
     */
    @Override
    public BigDecimal calcularPrecioVentaFinal(int cantidadBultos) {
        BigDecimal base = getPrecioVentaBase() != null ? getPrecioVentaBase() : BigDecimal.ZERO;
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getTipoArticulo() {
        return TIPO;
    }
}
