package com.sgcv.model;

import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests de dominio puro para el comportamiento de stock de {@link Articulo}.
 * Se usa {@link ArticuloEstandar} porque {@link Articulo} es abstracta.
 */
class ArticuloTest {

    private static ArticuloEstandar articuloConStock(int stock) {
        return new ArticuloEstandar(
                1,
                "POLL-ENT-20",
                "Pollo entero fresco",
                "Bulto x 20 kg",
                new BigDecimal("1000.00"),
                new BigDecimal("1500.00"),
                stock,
                2,
                true);
    }

    @Test
    @DisplayName("descontarStock(2) reduce el stock de 10 a 8")
    void descontarStockReduceLaExistencia() throws StockInsuficienteException {
        ArticuloEstandar articulo = articuloConStock(10);

        articulo.descontarStock(2);

        assertEquals(8, articulo.getStockDeposito(),
                "Descontar 2 bultos de un stock de 10 debe dejar 8 en depósito");
    }

    @Test
    @DisplayName("descontarStock(0) lanza ValidacionException y no modifica el stock")
    void descontarStockCeroLanzaValidacionException() {
        ArticuloEstandar articulo = articuloConStock(10);

        assertThrows(ValidacionException.class, () -> articulo.descontarStock(0),
                "Descontar una cantidad cero es inválido y debe lanzar ValidacionException");
        assertEquals(10, articulo.getStockDeposito(),
                "Una cantidad inválida no debe modificar el stock en depósito");
    }

    @Test
    @DisplayName("descontarStock(-1) lanza ValidacionException y no modifica el stock")
    void descontarStockNegativoLanzaValidacionException() {
        ArticuloEstandar articulo = articuloConStock(10);

        assertThrows(ValidacionException.class, () -> articulo.descontarStock(-1),
                "Descontar una cantidad negativa es inválido y debe lanzar ValidacionException");
        assertEquals(10, articulo.getStockDeposito(),
                "Una cantidad inválida no debe modificar el stock en depósito");
    }

    @Test
    @DisplayName("descontarStock(11) sobre stock 10 lanza StockInsuficienteException y conserva el stock")
    void descontarStockMayorALaExistenciaLanzaStockInsuficienteException() {
        ArticuloEstandar articulo = articuloConStock(10);

        StockInsuficienteException excepcion = assertThrows(StockInsuficienteException.class,
                () -> articulo.descontarStock(11),
                "Solicitar 11 bultos con stock 10 debe lanzar StockInsuficienteException");

        assertEquals("POLL-ENT-20", excepcion.getCodigoArticulo(),
                "La excepción debe identificar el artículo afectado");
        assertEquals(10, excepcion.getStockDisponible(),
                "La excepción debe informar el stock realmente disponible");
        assertEquals(11, excepcion.getCantidadSolicitada(),
                "La excepción debe informar la cantidad solicitada");
        assertEquals(10, articulo.getStockDeposito(),
                "Una operación rechazada no debe descontar existencias");
    }
}
