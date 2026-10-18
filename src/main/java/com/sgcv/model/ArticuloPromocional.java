package com.sgcv.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Especialización de Articulo para bultos avícolas de alta demanda mayorista sujetos a
 * política comercial de incentivo por volumen.
 * Aplica un 5% de descuento directo sobre el precio de lista para pedidos de 10 o más bultos.
 */
public class ArticuloPromocional extends Articulo {

    public static final String TIPO = "PROMOCIÓN VOLUMEN";

    public ArticuloPromocional() {
        super();
    }

    public ArticuloPromocional(int idArticulo, String codigo, String descripcion, String unidadEmbalaje,
                               BigDecimal precioCostoReposicion, BigDecimal precioVentaBase,
                               int stockDeposito, int stockMinimoAlerta, boolean activo) {
        super(idArticulo, codigo, descripcion, unidadEmbalaje, precioCostoReposicion,
                precioVentaBase, stockDeposito, stockMinimoAlerta, activo);
    }

    /**
     * Regla comercial polimórfica:
     * Si la cantidad de bultos es >= 10, aplica una bonificación del 5% (factor 0.95).
     * De lo contrario, se factura al precio base de lista.
     */
    @Override
    public BigDecimal calcularPrecioVentaFinal(int cantidadBultos) {
        BigDecimal base = getPrecioVentaBase() != null ? getPrecioVentaBase() : BigDecimal.ZERO;
        if (cantidadBultos >= 10) {
            return base.multiply(new BigDecimal("0.95")).setScale(2, RoundingMode.HALF_UP);
        }
        return base.setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getTipoArticulo() {
        return TIPO;
    }
}
