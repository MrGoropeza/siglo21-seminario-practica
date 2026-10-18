package com.sgcv;

import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloCongelado;
import com.sgcv.model.ArticuloFresco;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Modelo de Dominio y Polimorfismo de Artículos")
public class ArticuloPolimorfismoTest {

    @Nested
    @DisplayName("1. Encapsulamiento e Invariantes de Dominio")
    class EncapsulamientoEInvariantesTest {

        @Test
        @DisplayName("Rechazo de código nulo o en blanco")
        void testValidacionCodigoInvalido() {
            assertThrows(ValidacionException.class, () ->
                    new ArticuloFresco(1, "", "Pollo Fresco", "Caja x 20kg",
                            new BigDecimal("1000.00"), new BigDecimal("1500.00"), 50, 10, true)
            );
            assertThrows(ValidacionException.class, () ->
                    new ArticuloFresco(1, null, "Pollo Fresco", "Caja x 20kg",
                            new BigDecimal("1000.00"), new BigDecimal("1500.00"), 50, 10, true)
            );
        }

        @Test
        @DisplayName("Rechazo de descripción nula o vacía")
        void testValidacionDescripcionInvalida() {
            assertThrows(ValidacionException.class, () ->
                    new ArticuloCongelado(2, "PECH-01", "  ", "Caja x 15kg",
                            new BigDecimal("2000.00"), new BigDecimal("2800.00"), 30, 5, true)
            );
        }

        @Test
        @DisplayName("Rechazo de valores monetarios o stock negativos")
        void testValidacionPreciosYStockNegativos() {
            assertThrows(ValidacionException.class, () ->
                    new ArticuloFresco(3, "POLL-01", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("-50.00"), new BigDecimal("1500.00"), 10, 5, true)
            );
            assertThrows(ValidacionException.class, () ->
                    new ArticuloFresco(3, "POLL-01", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("1000.00"), new BigDecimal("-10.00"), 10, 5, true)
            );
            assertThrows(ValidacionException.class, () ->
                    new ArticuloFresco(3, "POLL-01", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("1000.00"), new BigDecimal("1500.00"), -5, 5, true)
            );
            assertThrows(ValidacionException.class, () ->
                    new ArticuloFresco(3, "POLL-01", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("1000.00"), new BigDecimal("1500.00"), 10, -1, true)
            );
        }

        @Test
        @DisplayName("Comportamiento de tieneStock y descontarStock")
        void testGestionStockFisico() throws StockInsuficienteException {
            ArticuloFresco fresco = new ArticuloFresco(10, "POLL-ENT-20", "Pollo Entero Fresco",
                    "Caja x 20kg", new BigDecimal("42000.00"), new BigDecimal("54000.00"), 50, 10, true);

            assertTrue(fresco.tieneStock(30));
            assertTrue(fresco.tieneStock(50));
            assertFalse(fresco.tieneStock(51));
            assertFalse(fresco.tieneStock(0));

            // Descuento válido
            fresco.descontarStock(20);
            assertEquals(30, fresco.getStockDeposito(), "El stock físico debe decrementar a 30 bultos");

            // Reposición válida
            fresco.reponerStock(15);
            assertEquals(45, fresco.getStockDeposito(), "El stock físico debe incrementar a 45 bultos");

            // Descuento excedido lanza StockInsuficienteException
            assertThrows(StockInsuficienteException.class, () -> fresco.descontarStock(46));

            // Descuento con valor no positivo lanza ValidacionException
            assertThrows(ValidacionException.class, () -> fresco.descontarStock(0));
            assertThrows(ValidacionException.class, () -> fresco.descontarStock(-5));
        }
    }

    @Nested
    @DisplayName("2. Polimorfismo en ArticuloFresco")
    class ArticuloFrescoPolimorfismoTest {

        @Test
        @DisplayName("Cálculo de precio: base si < 10 bultos, bonificación 5% si >= 10 bultos")
        void testCalcularPrecioVentaFinalFresco() {
            BigDecimal base = new BigDecimal("10000.00");
            ArticuloFresco fresco = new ArticuloFresco(1, "POLL-ENT", "Pollo Entero", "Caja x 20kg",
                    new BigDecimal("8000.00"), base, 100, 20, true, 7, 4.0);

            // Caso minorista < 10 bultos: tarifa base sin bonificación
            BigDecimal precioMinorista = fresco.calcularPrecioVentaFinal(9);
            assertEquals(new BigDecimal("10000.00"), precioMinorista, "Para 9 bultos se debe facturar a precio base");

            // Caso mayorista rotación >= 10 bultos: 5% de descuento (10000 * 0.95 = 9500)
            BigDecimal precioRotacion10 = fresco.calcularPrecioVentaFinal(10);
            assertEquals(new BigDecimal("9500.00"), precioRotacion10, "Para 10 bultos aplica 5% de descuento por alta rotación");

            BigDecimal precioRotacion25 = fresco.calcularPrecioVentaFinal(25);
            assertEquals(new BigDecimal("9500.00"), precioRotacion25, "Para 25 bultos aplica 5% de descuento por alta rotación");
        }

        @Test
        @DisplayName("Verificación de categoría de conservación y vida útil")
        void testMetodosPolimorficosFresco() {
            ArticuloFresco fresco = new ArticuloFresco(1, "POLL-ENT", "Pollo Entero", "Caja x 20kg",
                    new BigDecimal("8000.00"), new BigDecimal("10000.00"), 100, 20, true, 5, 3.5);

            assertEquals("REFRIGERADO_FRESCO (0°C a 4°C)", fresco.getCategoriaConservacion());
            assertEquals(5, fresco.getDiasCaducidadRecomendada());
            assertEquals(3.5, fresco.getTemperaturaMaxC());
        }
    }

    @Nested
    @DisplayName("3. Polimorfismo en ArticuloCongelado")
    class ArticuloCongeladoPolimorfismoTest {

        @Test
        @DisplayName("Cálculo de precio: recargo 3% si < 5 bultos, base entre 5 y 19, 4% descuento si >= 20")
        void testCalcularPrecioVentaFinalCongelado() {
            BigDecimal base = new BigDecimal("10000.00");
            ArticuloCongelado congelado = new ArticuloCongelado(2, "PECH-SUP-15", "Suprema Pechuga Congelada",
                    "Caja x 15kg", new BigDecimal("7000.00"), base, 80, 15, true,
                    -18.0, new BigDecimal("3.00"));

            // 1. Pedido pequeño < 5 bultos: recargo 3% (10000 * 1.03 = 10300.00)
            BigDecimal precioPequeno = congelado.calcularPrecioVentaFinal(3);
            assertEquals(new BigDecimal("10300.00"), precioPequeno,
                    "Para menos de 5 bultos debe aplicar recargo de preservación térmica del 3%");

            // 2. Pedido intermedio entre 5 y 19 bultos: precio base de lista
            BigDecimal precioIntermedio5 = congelado.calcularPrecioVentaFinal(5);
            assertEquals(new BigDecimal("10000.00"), precioIntermedio5,
                    "Para 5 bultos corresponde precio base sin recargo ni descuento");

            BigDecimal precioIntermedio19 = congelado.calcularPrecioVentaFinal(19);
            assertEquals(new BigDecimal("10000.00"), precioIntermedio19,
                    "Para 19 bultos corresponde precio base");

            // 3. Pedido mayorista escala >= 20 bultos: bonificación del 4% (10000 * 0.96 = 9600.00)
            BigDecimal precioMayorista20 = congelado.calcularPrecioVentaFinal(20);
            assertEquals(new BigDecimal("9600.00"), precioMayorista20,
                    "Para 20 bultos aplica 4% de descuento mayorista");

            BigDecimal precioMayorista30 = congelado.calcularPrecioVentaFinal(30);
            assertEquals(new BigDecimal("9600.00"), precioMayorista30,
                    "Para 30 bultos aplica 4% de descuento mayorista");
        }

        @Test
        @DisplayName("Verificación de categoría técnica IQF y 180 días de caducidad")
        void testMetodosPolimorficosCongelado() {
            ArticuloCongelado congelado = new ArticuloCongelado(2, "MILA-PEC-05", "Milanesas Congeladas",
                    "Bolsa x 5kg", new BigDecimal("17000.00"), new BigDecimal("23500.00"), 40, 10, true);

            assertEquals("CONGELADO_IQF (-18°C)", congelado.getCategoriaConservacion());
            assertEquals(180, congelado.getDiasCaducidadRecomendada());
            assertEquals(-18.0, congelado.getTemperaturaCongelacionC());
        }
    }

    @Nested
    @DisplayName("4. Despacho Polimórfico en Colecciones Homogéneas")
    class DespachoPolimorficoColeccionTest {

        @Test
        @DisplayName("Invocación dinámica polimórfica sobre lista de tipo Articulo")
        void testPolimorfismoColeccion() {
            Articulo a1 = new ArticuloFresco(1, "POLL-ENT", "Pollo Entero", "Caja",
                    new BigDecimal("1000.00"), new BigDecimal("2000.00"), 50, 10, true, 7, 4.0);

            Articulo a2 = new ArticuloCongelado(2, "PECH-SUP", "Suprema Pechuga", "Caja",
                    new BigDecimal("1500.00"), new BigDecimal("2000.00"), 50, 10, true, -18.0, new BigDecimal("3.00"));

            List<Articulo> articulos = List.of(a1, a2);

            // Evaluación para 20 bultos:
            // a1 (Fresco): 5% descuento sobre 2000 = 1900.00
            // a2 (Congelado): 4% descuento sobre 2000 = 1920.00
            assertEquals(new BigDecimal("1900.00"), articulos.get(0).calcularPrecioVentaFinal(20));
            assertEquals(new BigDecimal("1920.00"), articulos.get(1).calcularPrecioVentaFinal(20));

            // Categorías distintas
            assertTrue(articulos.get(0).getCategoriaConservacion().contains("REFRIGERADO_FRESCO"));
            assertTrue(articulos.get(1).getCategoriaConservacion().contains("CONGELADO_IQF"));
        }
    }
}
