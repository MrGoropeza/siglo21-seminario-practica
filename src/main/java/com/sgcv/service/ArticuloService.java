package com.sgcv.service;

import com.sgcv.dao.ArticuloDAO;
import com.sgcv.dao.IArticuloDAO;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Articulo;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


/**
 * Servicio de negocio para la administración de artículos e inventario avícola.
 * Incorpora implementaciones explícitas de algoritmos fundamentales de ordenamiento
 * (MergeSort O(n log n)) y búsqueda binaria (O(log n)), complementado con Comparator.
 */
public class ArticuloService implements IArticuloService {

    private final IArticuloDAO articuloDAO;

    public ArticuloService() {
        this(new ArticuloDAO());
    }

    public ArticuloService(IArticuloDAO articuloDAO) {
        this.articuloDAO = articuloDAO;
    }

    @Override
    public List<Articulo> listarDisponibles() throws SQLException {
        return articuloDAO.listarDisponibles();
    }

    @Override
    public Articulo buscarPorId(int id) throws SQLException {
        return articuloDAO.buscarPorId(id);
    }

    // =========================================================================
    // Algoritmo Explícito de Ordenamiento: MergeSort (O(n log n))
    // =========================================================================

    /**
     * Ordena una lista de artículos de menor a mayor existencia de stock físico utilizando
     * el algoritmo clásico divide y vencerás MergeSort. Retorna una copia defensiva ordenada,
     * garantizando estabilidad algorítmica y no mutación de la lista origen.
     *
     * @param lista colección original de artículos
     * @return nueva lista ordenada de forma ascendente por stockDeposito
     */
    @Override
    public List<Articulo> ordenarPorStockAscendente(List<Articulo> lista) {
        if (lista == null) {
            return new ArrayList<>();
        }
        if (lista.size() <= 1) {
            return new ArrayList<>(lista);
        }

        List<Articulo> copia = new ArrayList<>(lista);
        return mergeSort(copia);
    }

    private List<Articulo> mergeSort(List<Articulo> elementos) {
        if (elementos.size() <= 1) {
            return elementos;
        }

        int medio = elementos.size() / 2;
        List<Articulo> izquierda = new ArrayList<>(elementos.subList(0, medio));
        List<Articulo> derecha = new ArrayList<>(elementos.subList(medio, elementos.size()));

        List<Articulo> izqOrdenada = mergeSort(izquierda);
        List<Articulo> derOrdenada = mergeSort(derecha);

        return merge(izqOrdenada, derOrdenada);
    }

    private List<Articulo> merge(List<Articulo> izq, List<Articulo> der) {
        List<Articulo> resultado = new ArrayList<>(izq.size() + der.size());
        int i = 0;
        int j = 0;

        while (i < izq.size() && j < der.size()) {
            Articulo a1 = izq.get(i);
            Articulo a2 = der.get(j);

            // Comparación por stockDeposito ascendente
            if (a1.getStockDeposito() <= a2.getStockDeposito()) {
                resultado.add(a1);
                i++;
            } else {
                resultado.add(a2);
                j++;
            }
        }

        while (i < izq.size()) {
            resultado.add(izq.get(i));
            i++;
        }

        while (j < der.size()) {
            resultado.add(der.get(j));
            j++;
        }

        return resultado;
    }

    /**
     * Demostración complementaria utilizando la API moderna de Comparator de Java.
     * @return comparador por stock depósito ascendente
     */
    public static Comparator<Articulo> comparadorPorStockAscendente() {
        return Comparator.comparingInt(Articulo::getStockDeposito);
    }

    // =========================================================================
    // Algoritmo Explícito de Búsqueda Binaria: O(log n)
    // =========================================================================

    /**
     * Ejecuta una búsqueda dicotómica/binaria iterativa clásica sobre una colección
     * ordenada lexicográficamente por código de producto.
     *
     * @param listaOrdenadaPorCodigo lista ordenada por código de artículo
     * @param codigoBuscado código a localizar
     * @return índice del artículo encontrado en la lista, o -1 si no existe
     */
    @Override
    public int busquedaBinariaPorCodigo(List<Articulo> listaOrdenadaPorCodigo, String codigoBuscado) {
        if (listaOrdenadaPorCodigo == null || codigoBuscado == null || listaOrdenadaPorCodigo.isEmpty()) {
            return -1;
        }

        String clave = codigoBuscado.trim();
        int bajo = 0;
        int alto = listaOrdenadaPorCodigo.size() - 1;

        while (bajo <= alto) {
            int medio = bajo + (alto - bajo) / 2;
            Articulo articuloMedio = listaOrdenadaPorCodigo.get(medio);
            if (articuloMedio == null || articuloMedio.getCodigo() == null) {
                return -1;
            }

            int comparacion = articuloMedio.getCodigo().compareToIgnoreCase(clave);

            if (comparacion == 0) {
                return medio; // Coincidencia exacta
            } else if (comparacion < 0) {
                bajo = medio + 1; // Buscar en la mitad derecha
            } else {
                alto = medio - 1; // Buscar en la mitad izquierda
            }
        }

        return -1; // No encontrado
    }

    @Override
    public int crearArticulo(Articulo articulo) throws SQLException {
        if (articulo == null) {
            throw new ValidacionException("El artículo no puede ser nulo.");
        }
        if (articulo.getCodigo() == null || articulo.getCodigo().trim().isEmpty()) {
            throw new ValidacionException("El código del producto es obligatorio.");
        }
        if (articulo.getDescripcion() == null || articulo.getDescripcion().trim().isEmpty()) {
            throw new ValidacionException("La descripción del producto es obligatoria.");
        }
        if (articulo.getPrecioVentaBase() == null || articulo.getPrecioVentaBase().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El precio de venta base no puede ser negativo.");
        }
        if (articulo.getPrecioCostoReposicion() == null || articulo.getPrecioCostoReposicion().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El costo de reposición no puede ser negativo.");
        }
        if (articuloDAO.existeCodigo(articulo.getCodigo(), 0)) {
            throw new ValidacionException("Ya existe un producto con el código '" + articulo.getCodigo().toUpperCase().trim() + "'.");
        }
        return articuloDAO.crearArticulo(articulo);
    }

    @Override
    public boolean actualizarArticulo(Articulo articulo) throws SQLException {
        if (articulo == null) {
            throw new ValidacionException("El artículo no puede ser nulo.");
        }
        if (articulo.getIdArticulo() <= 0) {
            throw new ValidacionException("Debe especificar un ID de artículo válido para actualizar.");
        }
        if (articulo.getCodigo() == null || articulo.getCodigo().trim().isEmpty()) {
            throw new ValidacionException("El código del producto es obligatorio.");
        }
        if (articulo.getDescripcion() == null || articulo.getDescripcion().trim().isEmpty()) {
            throw new ValidacionException("La descripción del producto es obligatoria.");
        }
        if (articulo.getPrecioVentaBase() == null || articulo.getPrecioVentaBase().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El precio de venta base no puede ser negativo.");
        }
        if (articulo.getPrecioCostoReposicion() == null || articulo.getPrecioCostoReposicion().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El costo de reposición no puede ser negativo.");
        }
        if (articuloDAO.existeCodigo(articulo.getCodigo(), articulo.getIdArticulo())) {
            throw new ValidacionException("Ya existe otro producto con el código '" + articulo.getCodigo().toUpperCase().trim() + "'.");
        }
        return articuloDAO.actualizarArticulo(articulo);
    }

    @Override
    public boolean eliminarArticulo(int idArticulo) throws SQLException {
        if (idArticulo <= 0) {
            throw new ValidacionException("ID de artículo inválido: " + idArticulo);
        }
        return articuloDAO.eliminarArticulo(idArticulo);
    }

    @Override
    public boolean existeCodigo(String codigo, int idArticuloExcluir) throws SQLException {
        return articuloDAO.existeCodigo(codigo, idArticuloExcluir);
    }

    public IArticuloDAO getArticuloDAO() {
        return articuloDAO;
    }
}

