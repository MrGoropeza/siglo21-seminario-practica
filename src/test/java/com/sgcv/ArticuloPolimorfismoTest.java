package com.sgcv;

import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloEstandar;
import com.sgcv.model.ArticuloPromocional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Modelo de Dominio y Polimorfismo de Artículos (DER)")
public class ArticuloPolimorfismoTest {

    @Nested
    @DisplayName("1. Encapsulamiento e Invariantes de Dominio")
    class EncapsulamientoEInvariantesTest {

        @Test
        @DisplayName("Rechazo de código nulo o en blanco")
        void testValidacionCodigoInvalido() {
            assertThrows(ValidacionException.class, () ->
                    new ArticuloEstandar(1, "", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("42000.00"), new BigDecimal("54000.00"), 50, 10, true)
            );
            assertThrows(ValidacionException.class, () ->
                    new ArticuloEstandar(1, null, "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("42000.00"), new BigDecimal("54000.00"), 50, 10, true)
            );
        }

        @Test
        @DisplayName("Rechazo de descripción nula o vacía")
        void testValidacionDescripcionInvalida() {
            assertThrows(ValidacionException.class, () ->
                    new ArticuloPromocional(2, "POLL-ENT-20", "  ", "Caja x 20kg",
                            new BigDecimal("42000.00"), new BigDecimal("54000.00"), 30, 5, true)
            );
        }

        @Test
        @DisplayName("Rechazo de valores monetarios o stock negativos")
        void testValidacionPreciosYStockNegativos() {
            assertThrows(ValidacionException.class, () ->
                    new ArticuloEstandar(3, "POLL-01", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("-50.00"), new BigDecimal("1500.00"), 10, 5, true)
            );
            assertThrows(ValidacionException.class, () ->
                    new ArticuloEstandar(3, "POLL-01", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("1000.00"), new BigDecimal("-10.00"), 10, 5, true)
            );
            assertThrows(ValidacionException.class, () ->
                    new ArticuloEstandar(3, "POLL-01", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("1000.00"), new BigDecimal("1500.00"), -5, 5, true)
            );
            assertThrows(ValidacionException.class, () ->
                    new ArticuloEstandar(3, "POLL-01", "Pollo Entero", "Caja x 20kg",
                            new BigDecimal("1000.00"), new BigDecimal("1500.00"), 10, -1, true)
            );
        }

        @Test
        @DisplayName("Comportamiento de tieneStock y descontarStock")
        void testGestionStockFisico() throws StockInsuficienteException {
            ArticuloEstandar art = new ArticuloEstandar(10, "POLL-ENT-20", "Pollo Entero Grado A",
                    "Caja x 20kg", new BigDecimal("42000.00"), new BigDecimal("54000.00"), 50, 10, true);

            assertTrue(art.tieneStock(30));
            assertTrue(art.tieneStock(50));
            assertFalse(art.tieneStock(51));
            assertFalse(art.tieneStock(0));

            // Descuento válido
            art.descontarStock(20);
            assertEquals(30, art.getStockDeposito(), "El stock físico debe decrementar a 30 bultos");

            // Reposición válida
            art.reponerStock(15);
            assertEquals(45, art.getStockDeposito(), "El stock físico debe incrementar a 45 bultos");

            // Descuento excedido lanza StockInsuficienteException
            assertThrows(StockInsuficienteException.class, () -> art.descontarStock(46));

            // Descuento con valor no positivo lanza ValidacionException
            assertThrows(ValidacionException.class, () -> art.descontarStock(0));
            assertThrows(ValidacionException.class, () -> art.descontarStock(-5));
        }
    }

    @Nested
    @DisplayName("2. Polimorfismo en ArticuloEstandar")
    class ArticuloEstandarPolimorfismoTest {

        @Test
        @DisplayName("Cálculo de precio: mantiene tarifa base para cualquier volumen")
        void testCalcularPrecioVentaFinalEstandar() {
            BigDecimal base = new BigDecimal("10000.00");
            ArticuloEstandar estandar = new ArticuloEstandar(1, "PECH-SUP-15", "Suprema de Pechuga", "Caja x 15kg",
                    new BigDecimal("8000.00"), base, 100, 20, true);

            // Factura a tarifa base sin importar el volumen
            assertEquals(new BigDecimal("10000.00"), estandar.calcularPrecioVentaFinal(1));
            assertEquals(new BigDecimal("10000.00"), estandar.calcularPrecioVentaFinal(10));
            assertEquals(new BigDecimal("10000.00"), estandar.calcularPrecioVentaFinal(50));
        }

        @Test
        @DisplayName("Verificación de tipo comercial ESTÁNDAR")
        void testTipoArticuloEstandar() {
            ArticuloEstandar estandar = new ArticuloEstandar(1, "PECH-SUP-15", "Suprema de Pechuga", "Caja x 15kg",
                    new BigDecimal("8000.00"), new BigDecimal("10000.00"), 100, 20, true);

            assertEquals("ESTÁNDAR", estandar.getTipoArticulo());
            assertTrue(estandar.toString().contains("ESTÁNDAR"));
        }
    }

    @Nested
    @DisplayName("3. Polimorfismo en ArticuloPromocional")
    class ArticuloPromocionalPolimorfismoTest {

        @Test
        @DisplayName("Cálculo de precio: base si < 10 bultos, bonificación 5% si >= 10 bultos")
        void testCalcularPrecioVentaFinalPromocional() {
            BigDecimal base = new BigDecimal("10000.00");
            ArticuloPromocional promo = new ArticuloPromocional(2, "POLL-ENT-20", "Pollo Entero Cajon",
                    "Caja x 20kg", new BigDecimal("7000.00"), base, 80, 15, true);

            // 1. Pedido regular < 10 bultos: tarifa base de lista
            BigDecimal precioRegular9 = promo.calcularPrecioVentaFinal(9);
            assertEquals(new BigDecimal("10000.00"), precioRegular9,
                    "Para menos de 10 bultos se factura a precio base");

            // 2. Pedido mayorista escala >= 10 bultos: bonificación del 5% (10000 * 0.95 = 9500.00)
            BigDecimal precioPromo10 = promo.calcularPrecioVentaFinal(10);
            assertEquals(new BigDecimal("9500.00"), precioPromo10,
                    "Para 10 bultos aplica 5% de descuento por escala mayorista");

            BigDecimal precioPromo30 = promo.calcularPrecioVentaFinal(30);
            assertEquals(new BigDecimal("9500.00"), precioPromo30,
                    "Para 30 bultos aplica 5% de descuento por escala mayorista");
        }

        @Test
        @DisplayName("Verificación de tipo comercial PROMOCIÓN VOLUMEN")
        void testTipoArticuloPromocional() {
            ArticuloPromocional promo = new ArticuloPromocional(2, "PATA-MUS-15", "Pata Muslo Seleccionada",
                    "Cajon x 15kg", new BigDecimal("30000.00"), new BigDecimal("39000.00"), 40, 10, true);

            assertEquals("PROMOCIÓN VOLUMEN", promo.getTipoArticulo());
            assertTrue(promo.toString().contains("PROMOCIÓN VOLUMEN"));
        }
    }

    @Nested
    @DisplayName("4. Despacho Polimórfico en Colecciones Homogéneas")
    class DespachoPolimorficoColeccionTest {

        @Test
        @DisplayName("Invocación dinámica polimórfica sobre lista de tipo Articulo")
        void testPolimorfismoColeccion() {
            Articulo a1 = new ArticuloEstandar(1, "PECH-SUP-15", "Suprema Pechuga", "Caja",
                    new BigDecimal("1500.00"), new BigDecimal("2000.00"), 50, 10, true);

            Articulo a2 = new ArticuloPromocional(2, "POLL-ENT-20", "Pollo Entero", "Caja",
                    new BigDecimal("1000.00"), new BigDecimal("2000.00"), 50, 10, true);

            List<Articulo> articulos = List.of(a1, a2);

            // Evaluación para 10 bultos:
            // a1 (Estándar): sin descuento sobre 2000 = 2000.00
            // a2 (Promocional): 5% descuento sobre 2000 = 1900.00
            assertEquals(new BigDecimal("2000.00"), articulos.get(0).calcularPrecioVentaFinal(10));
            assertEquals(new BigDecimal("1900.00"), articulos.get(1).calcularPrecioVentaFinal(10));

            assertEquals("ESTÁNDAR", articulos.get(0).getTipoArticulo());
            assertEquals("PROMOCIÓN VOLUMEN", articulos.get(1).getTipoArticulo());
        }
    }
}
