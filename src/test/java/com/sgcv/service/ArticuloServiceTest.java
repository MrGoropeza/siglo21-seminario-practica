package com.sgcv.service;

import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloEstandar;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de los algoritmos de ordenamiento y búsqueda de {@link ArticuloService}.
 * No usan base de datos: el DAO no interviene en estos métodos y las listas se
 * construyen a mano. Se pasa {@code null} como DAO para dejar explícito que no
 * debe invocarse persistencia alguna.
 */
class ArticuloServiceTest {

    private final ArticuloService service = new ArticuloService(null);

    private static ArticuloEstandar articulo(String codigo, int stock) {
        return new ArticuloEstandar(
                0,
                codigo,
                "Descripción de " + codigo,
                "Bulto",
                new BigDecimal("10.00"),
                new BigDecimal("20.00"),
                stock,
                1,
                true);
    }

    private static List<String> codigosDe(List<Articulo> articulos) {
        List<String> codigos = new ArrayList<>();
        for (Articulo articulo : articulos) {
            codigos.add(articulo.getCodigo());
        }
        return codigos;
    }

    private static List<Integer> stocksDe(List<Articulo> articulos) {
        List<Integer> stocks = new ArrayList<>();
        for (Articulo articulo : articulos) {
            stocks.add(articulo.getStockDeposito());
        }
        return stocks;
    }

    @Test
    @DisplayName("ordenarPorStockAscendente ordena de menor a mayor stock")
    void ordenaPorStockAscendente() {
        List<Articulo> desordenados = new ArrayList<>(Arrays.asList(
                articulo("ALAS-ENT-10", 30),
                articulo("MUSL-ENT-10", 5),
                articulo("POLL-ENT-20", 20),
                articulo("MILA-ENT-05", 10)));

        List<Articulo> ordenados = service.ordenarPorStockAscendente(desordenados);

        assertEquals(Arrays.asList(5, 10, 20, 30), stocksDe(ordenados),
                "Los stocks deben quedar ordenados de forma ascendente (5, 10, 20, 30)");
        assertEquals(Arrays.asList("MUSL-ENT-10", "MILA-ENT-05", "POLL-ENT-20", "ALAS-ENT-10"),
                codigosDe(ordenados),
                "El orden por stock debe corresponder a los artículos con 5, 10, 20 y 30 bultos");
    }

    @Test
    @DisplayName("ordenarPorStockAscendente no muta la lista de entrada")
    void noMutaLaListaDeEntrada() {
        List<Articulo> original = new ArrayList<>(Arrays.asList(
                articulo("ALAS-ENT-10", 30),
                articulo("MUSL-ENT-10", 5),
                articulo("POLL-ENT-20", 20),
                articulo("MILA-ENT-05", 10)));
        List<String> codigosOriginales = codigosDe(original);
        List<Integer> stocksOriginales = stocksDe(original);

        List<Articulo> ordenados = service.ordenarPorStockAscendente(original);

        assertEquals(codigosOriginales, codigosDe(original),
                "La lista original no debe cambiar de orden");
        assertEquals(stocksOriginales, stocksDe(original),
                "La lista original debe conservar sus stocks sin reordenar");
        assertNotSame(original, ordenados,
                "El método debe devolver una lista nueva (copia defensiva), no la misma instancia");
    }

    @Test
    @DisplayName("ordenarPorStockAscendente sobre lista vacía devuelve lista vacía")
    void ordenaListaVacia() {
        List<Articulo> resultado = service.ordenarPorStockAscendente(Collections.emptyList());

        assertNotNull(resultado, "El resultado nunca debe ser null");
        assertTrue(resultado.isEmpty(), "Una lista vacía debe devolver una lista vacía");
    }

    @Test
    @DisplayName("ordenarPorStockAscendente sobre null devuelve lista vacía")
    void ordenaListaNula() {
        List<Articulo> resultado = service.ordenarPorStockAscendente(null);

        assertNotNull(resultado, "El resultado nunca debe ser null");
        assertTrue(resultado.isEmpty(), "Una lista null debe devolver una lista vacía");
    }

    @Test
    @DisplayName("ordenarPorStockAscendente con un solo elemento devuelve lista de un elemento")
    void ordenaListaDeUnElemento() {
        Articulo unico = articulo("POLL-ENT-20", 7);

        List<Articulo> resultado = service.ordenarPorStockAscendente(
                new ArrayList<>(Collections.singletonList(unico)));

        assertEquals(1, resultado.size(), "La lista resultante debe contener exactamente un elemento");
        assertSame(unico, resultado.get(0), "El único elemento debe ser el mismo artículo de entrada");
    }

    @Test
    @DisplayName("busquedaBinariaPorCodigo encuentra POLL-ENT-20 en el índice 5")
    void busquedaBinariaEncuentraElCodigoExistente() {
        List<Articulo> porCodigo = new ArrayList<>(Arrays.asList(
                articulo("ALAS-ENT-10", 3),
                articulo("ALAS-ENT-12", 4),
                articulo("MILA-ENT-05", 5),
                articulo("MUSL-ENT-10", 6),
                articulo("PECH-ENT-10", 7),
                articulo("POLL-ENT-20", 8),
                articulo("POLL-ENT-30", 9)));

        int indice = service.busquedaBinariaPorCodigo(porCodigo, "POLL-ENT-20");

        assertEquals(5, indice,
                "POLL-ENT-20 ocupa el índice 5 en la lista ordenada por código, pero la búsqueda devolvió " + indice);
    }

    @Test
    @DisplayName("busquedaBinariaPorCodigo devuelve -1 cuando el código no existe")
    void busquedaBinariaDevuelveMenosUnoSiElCodigoNoExiste() {
        List<Articulo> porCodigo = new ArrayList<>(Arrays.asList(
                articulo("ALAS-ENT-10", 3),
                articulo("ALAS-ENT-12", 4),
                articulo("MILA-ENT-05", 5),
                articulo("MUSL-ENT-10", 6),
                articulo("PECH-ENT-10", 7),
                articulo("POLL-ENT-20", 8),
                articulo("POLL-ENT-30", 9)));

        int indice = service.busquedaBinariaPorCodigo(porCodigo, "HUEV-DOC-01");

        assertEquals(-1, indice,
                "Un código ausente de la lista debe devolver -1, pero se obtuvo " + indice);
    }

    @Test
    @DisplayName("busquedaBinariaPorCodigo devuelve -1 con código null")
    void busquedaBinariaDevuelveMenosUnoSiElCodigoEsNulo() {
        List<Articulo> porCodigo = new ArrayList<>(Collections.singletonList(
                articulo("POLL-ENT-20", 8)));

        int indice = service.busquedaBinariaPorCodigo(porCodigo, null);

        assertEquals(-1, indice,
                "Un código null debe devolver -1 sin lanzar excepción, pero se obtuvo " + indice);
    }

    @Test
    @DisplayName("busquedaBinariaPorCodigo devuelve -1 con lista null")
    void busquedaBinariaDevuelveMenosUnoSiLaListaEsNula() {
        int indice = service.busquedaBinariaPorCodigo(null, "POLL-ENT-20");

        assertEquals(-1, indice,
                "Una lista null debe devolver -1 sin lanzar excepción, pero se obtuvo " + indice);
    }
}
