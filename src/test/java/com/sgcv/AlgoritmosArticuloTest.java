package com.sgcv;

import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloEstandar;
import com.sgcv.model.ArticuloPromocional;
import com.sgcv.service.ArticuloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Algoritmos Explícitos de Ordenamiento y Búsqueda Binaria")
public class AlgoritmosArticuloTest {

    private ArticuloService articuloService;

    @BeforeEach
    void setUp() {
        articuloService = new ArticuloService();
    }

    private Articulo crearArticulo(int id, String codigo, String descripcion, int stock) {
        BigDecimal costo = new BigDecimal("1000.00");
        BigDecimal venta = new BigDecimal("1500.00");
        if (codigo.startsWith("POLL-ENT") || codigo.startsWith("PATA-MUS")) {
            return new ArticuloPromocional(id, codigo, descripcion, "Bulto", costo, venta, stock, 10, true);
        } else {
            return new ArticuloEstandar(id, codigo, descripcion, "Bulto", costo, venta, stock, 10, true);
        }
    }

    @Nested
    @DisplayName("1. Algoritmo de Ordenamiento MergeSort por Stock Depósito")
    class OrdenamientoMergeSortTest {

        @Test
        @DisplayName("Ordena ascendentemente una lista desordenada de bultos avícolas")
        void testOrdenarPorStockAscendenteColeccionDesordenada() {
            List<Articulo> desordenada = new ArrayList<>(List.of(
                    crearArticulo(1, "POLL-ENT-20", "Pollo Entero", 140),
                    crearArticulo(2, "PATA-MUS-15", "Pata Muslo", 85),
                    crearArticulo(3, "PECH-SUP-15", "Suprema Pechuga", 50),
                    crearArticulo(4, "MILA-PEC-05", "Milanesas Pechuga", 95),
                    crearArticulo(5, "ALIT-POL-10", "Alitas Frescas", 35),
                    crearArticulo(6, "MENU-POL-10", "Menudos Frescos", 18)
            ));

            List<Articulo> ordenada = articuloService.ordenarPorStockAscendente(desordenada);

            assertEquals(6, ordenada.size(), "Debe conservar la totalidad de elementos");

            // Validar orden ascendente estricto por stock: 18, 35, 50, 85, 95, 140
            assertEquals(18, ordenada.get(0).getStockDeposito());
            assertEquals("MENU-POL-10", ordenada.get(0).getCodigo());

            assertEquals(35, ordenada.get(1).getStockDeposito());
            assertEquals("ALIT-POL-10", ordenada.get(1).getCodigo());

            assertEquals(50, ordenada.get(2).getStockDeposito());
            assertEquals("PECH-SUP-15", ordenada.get(2).getCodigo());

            assertEquals(85, ordenada.get(3).getStockDeposito());
            assertEquals("PATA-MUS-15", ordenada.get(3).getCodigo());

            assertEquals(95, ordenada.get(4).getStockDeposito());
            assertEquals("MILA-PEC-05", ordenada.get(4).getCodigo());

            assertEquals(140, ordenada.get(5).getStockDeposito());
            assertEquals("POLL-ENT-20", ordenada.get(5).getCodigo());

            // Verificar inmutabilidad de la colección de entrada
            assertEquals(140, desordenada.get(0).getStockDeposito(), "La lista original no debe ser modificada");
        }

        @Test
        @DisplayName("Manejo de casos bordes: lista vacía, un solo elemento y nulos")
        void testOrdenarCasosBorde() {
            assertTrue(articuloService.ordenarPorStockAscendente(null).isEmpty());
            assertTrue(articuloService.ordenarPorStockAscendente(Collections.emptyList()).isEmpty());

            Articulo unico = crearArticulo(1, "POLL-ENT-20", "Pollo Entero", 40);
            List<Articulo> resultadoUnico = articuloService.ordenarPorStockAscendente(List.of(unico));
            assertEquals(1, resultadoUnico.size());
            assertEquals(40, resultadoUnico.get(0).getStockDeposito());
        }

        @Test
        @DisplayName("Estabilidad con elementos de stock idéntico")
        void testEstabilidadConStocksDuplicados() {
            List<Articulo> duplicados = List.of(
                    crearArticulo(1, "ART-A", "Articulo A", 50),
                    crearArticulo(2, "ART-B", "Articulo B", 20),
                    crearArticulo(3, "ART-C", "Articulo C", 50),
                    crearArticulo(4, "ART-D", "Articulo D", 10)
            );

            List<Articulo> ordenada = articuloService.ordenarPorStockAscendente(duplicados);
            assertEquals(10, ordenada.get(0).getStockDeposito());
            assertEquals(20, ordenada.get(1).getStockDeposito());
            assertEquals(50, ordenada.get(2).getStockDeposito());
            assertEquals(50, ordenada.get(3).getStockDeposito());
        }
    }

    @Nested
    @DisplayName("2. Algoritmo de Búsqueda Binaria por Código")
    class BusquedaBinariaTest {

        private List<Articulo> listaOrdenadaPorCodigo;

        @BeforeEach
        void prepararListaOrdenada() {
            listaOrdenadaPorCodigo = List.of(
                    crearArticulo(5, "ALIT-POL-10", "Alitas Frescas", 35),
                    crearArticulo(6, "MENU-POL-10", "Menudos Frescos", 18),
                    crearArticulo(4, "MILA-PEC-05", "Milanesas Pechuga", 95),
                    crearArticulo(2, "PATA-MUS-15", "Pata Muslo", 85),
                    crearArticulo(3, "PECH-SUP-15", "Suprema Pechuga", 50),
                    crearArticulo(1, "POLL-ENT-20", "Pollo Entero", 140)
            );
        }

        @Test
        @DisplayName("Localización exitosa en inicio, medio y fin de la lista")
        void testBusquedaBinariaExitosa() {
            // Primer elemento (extremo izquierdo)
            int indexInicio = articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, "ALIT-POL-10");
            assertEquals(0, indexInicio);
            assertEquals("ALIT-POL-10", listaOrdenadaPorCodigo.get(indexInicio).getCodigo());

            // Elemento central
            int indexMedio = articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, "PATA-MUS-15");
            assertEquals(3, indexMedio);
            assertEquals("PATA-MUS-15", listaOrdenadaPorCodigo.get(indexMedio).getCodigo());

            // Último elemento (extremo derecho)
            int indexFin = articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, "POLL-ENT-20");
            assertEquals(5, indexFin);
            assertEquals("POLL-ENT-20", listaOrdenadaPorCodigo.get(indexFin).getCodigo());
        }

        @Test
        @DisplayName("Búsqueda case-insensitive tolerante a minúsculas y espacios")
        void testBusquedaBinariaCaseInsensitive() {
            int index = articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, " mila-pec-05 ");
            assertEquals(2, index);
            assertEquals("MILA-PEC-05", listaOrdenadaPorCodigo.get(index).getCodigo());
        }

        @Test
        @DisplayName("Retorna -1 cuando el código buscado no existe")
        void testBusquedaBinariaNoEncontrado() {
            int indexInexistente = articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, "CODIGO-INEXISTENTE");
            assertEquals(-1, indexInexistente, "Debe retornar -1 al no hallar coincidencia");

            int indexIntermedioInexistente = articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, "HUEV-DOC-12");
            assertEquals(-1, indexIntermedioInexistente);
        }

        @Test
        @DisplayName("Retorna -1 ante listas vacías o parámetros nulos")
        void testBusquedaBinariaCasosExtremos() {
            assertEquals(-1, articuloService.busquedaBinariaPorCodigo(Collections.emptyList(), "ALIT-POL-10"));
            assertEquals(-1, articuloService.busquedaBinariaPorCodigo(null, "ALIT-POL-10"));
            assertEquals(-1, articuloService.busquedaBinariaPorCodigo(listaOrdenadaPorCodigo, null));
        }
    }
}
