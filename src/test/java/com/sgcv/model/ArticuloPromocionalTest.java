package com.sgcv.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de frontera para la regla comercial de volumen de {@link ArticuloPromocional}.
 * El descuento del 5% rige desde 10 bultos inclusive.
 */
class ArticuloPromocionalTest {

    private static final BigDecimal PRECIO_BASE = new BigDecimal("100.00");

    private static ArticuloPromocional articuloPromocional() {
        return new ArticuloPromocional(
                1,
                "POLL-PROM-20",
                "Pollo entero promocional",
                "Bulto x 20 kg",
                new BigDecimal("70.00"),
                PRECIO_BASE,
                50,
                5,
                true);
    }

    @Test
    @DisplayName("Con cantidad 9 no aplica descuento: factura el precio base")
    void conNueveBultosNoAplicaDescuento() {
        ArticuloPromocional articulo = articuloPromocional();

        BigDecimal precio = articulo.calcularPrecioVentaFinal(9);

        assertEquals(0, precio.compareTo(new BigDecimal("100.00")),
                "Con 9 bultos debe cobrarse el precio base 100.00, pero fue " + precio);
        assertEquals(new BigDecimal("100.00"), precio,
                "El precio debe devolverse con escala 2 y sin descuento");
    }

    @Test
    @DisplayName("Con cantidad 10 (frontera) aplica el 5% de descuento")
    void conDiezBultosAplicaDescuento() {
        ArticuloPromocional articulo = articuloPromocional();

        BigDecimal precio = articulo.calcularPrecioVentaFinal(10);

        BigDecimal esperado = PRECIO_BASE.multiply(new BigDecimal("0.95"))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(esperado, precio,
                "Con 10 bultos debe aplicarse el descuento del 5% (100.00 x 0.95 = 95.00), pero fue " + precio);
    }

    @Test
    @DisplayName("Con cantidad 11 también aplica el 5% de descuento")
    void conOnceBultosAplicaDescuento() {
        ArticuloPromocional articulo = articuloPromocional();

        BigDecimal precio = articulo.calcularPrecioVentaFinal(11);

        assertEquals(new BigDecimal("95.00"), precio,
                "Con 11 bultos debe mantenerse el descuento del 5% (95.00), pero fue " + precio);
    }

    @Test
    @DisplayName("Un precio base nulo no lanza excepción y devuelve 0.00")
    void conPrecioBaseNuloDevuelveCero() {
        // El setter y el constructor rechazan null, por lo que se fuerza el estado
        // defensivo mediante una subclase de prueba que expone el getter nulo.
        ArticuloPromocional articulo = new ArticuloPromocional() {
            @Override
            public BigDecimal getPrecioVentaBase() {
                return null;
            }
        };

        BigDecimal precio = articulo.calcularPrecioVentaFinal(10);

        assertEquals(new BigDecimal("0.00"), precio,
                "Un precio base nulo debe tratarse como cero y devolver 0.00 sin lanzar excepción");
    }
}
