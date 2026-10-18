package com.sgcv;

import com.sgcv.dao.IArticuloDAO;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloEstandar;
import com.sgcv.model.ArticuloPromocional;
import com.sgcv.service.ArticuloService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias de ArticuloService (Catálogo Dinámico N Productos y Reglas de Negocio)")
class ArticuloServiceTest {

    private FakeArticuloDAO fakeDAO;
    private ArticuloService articuloService;

    @BeforeEach
    void setUp() {
        fakeDAO = new FakeArticuloDAO();
        articuloService = new ArticuloService(fakeDAO);
    }

    @Nested
    @DisplayName("Validación y Creación de Nuevos Productos")
    class CreacionArticuloTest {

        @Test
        @DisplayName("Crea exitosamente un nuevo artículo estándar en el catálogo")
        void testCrearArticuloExitoso() throws SQLException {
            Articulo nuevo = new ArticuloEstandar(0, "ALAS-CONG-10", "Alitas Congeladas IQF", "Caja x 10kg",
                    new BigDecimal("18000.00"), new BigDecimal("23000.00"), 40, 5, true);

            int idGenerado = articuloService.crearArticulo(nuevo);
            assertTrue(idGenerado > 0, "Debe retornar un ID positivo al persistir");
            assertEquals(1, fakeDAO.articulos.size());
            assertEquals("ALAS-CONG-10", fakeDAO.articulos.get(0).getCodigo());
        }

        @Test
        @DisplayName("Rechaza creación si el código ya existe en el catálogo")
        void testRechazaCodigoDuplicado() {
            Articulo existente = new ArticuloEstandar(1, "POLL-ENT-20", "Cajón Pollo Entero", "Cajón x 20kg",
                    new BigDecimal("30000.00"), new BigDecimal("38000.00"), 50, 10, true);
            fakeDAO.articulos.add(existente);

            Articulo duplicado = new ArticuloEstandar(0, "POLL-ENT-20", "Pollo Repetido", "Cajón",
                    new BigDecimal("30000.00"), new BigDecimal("38000.00"), 10, 2, true);

            ValidacionException ex = assertThrows(ValidacionException.class, () ->
                    articuloService.crearArticulo(duplicado));
            assertTrue(ex.getMessage().contains("Ya existe un producto con el código"));
        }

        @Test
        @DisplayName("Rechaza precio de venta o costo negativo")
        void testRechazaPreciosInvalidos() {
            assertThrows(ValidacionException.class, () -> {
                Articulo a = new ArticuloEstandar(0, "TEST-01", "Test", "Caja",
                        new BigDecimal("-100.00"), new BigDecimal("1000.00"), 10, 2, true);
                articuloService.crearArticulo(a);
            });

            assertThrows(ValidacionException.class, () -> {
                Articulo a = new ArticuloEstandar(0, "TEST-02", "Test", "Caja",
                        new BigDecimal("500.00"), new BigDecimal("-50.00"), 10, 2, true);
                articuloService.crearArticulo(a);
            });
        }
    }

    @Nested
    @DisplayName("Actualización de Precios y Catálogo Dinámico")
    class ActualizacionArticuloTest {

        @Test
        @DisplayName("Actualiza el precio base de venta y costo de reposición en el maestro")
        void testActualizarPrecios() throws SQLException {
            Articulo original = new ArticuloEstandar(5, "PATA-MUS-15", "Pata y Muslo", "Caja x 15kg",
                    new BigDecimal("22000.00"), new BigDecimal("29000.00"), 30, 5, true);
            fakeDAO.articulos.add(original);

            Articulo modificado = new ArticuloPromocional(5, "PATA-MUS-15", "Pata y Muslo Seleccionado", "Caja x 15kg",
                    new BigDecimal("24000.00"), new BigDecimal("32000.00"), 45, 5, true);

            boolean actualizado = articuloService.actualizarArticulo(modificado);
            assertTrue(actualizado);

            Articulo recuperado = articuloService.buscarPorId(5);
            assertNotNull(recuperado);
            assertEquals(new BigDecimal("32000.00"), recuperado.getPrecioVentaBase());
            assertEquals(new BigDecimal("24000.00"), recuperado.getPrecioCostoReposicion());
        }

        @Test
        @DisplayName("Desactivación lógica de producto (baja sin romper integridad referencial)")
        void testDesactivarArticulo() throws SQLException {
            Articulo original = new ArticuloEstandar(10, "MENUD-10", "Menudos Avícolas", "Bolsa x 10kg",
                    new BigDecimal("8000.00"), new BigDecimal("11000.00"), 20, 2, true);
            fakeDAO.articulos.add(original);

            boolean eliminado = articuloService.eliminarArticulo(10);
            assertTrue(eliminado);

            Articulo recuperado = articuloService.buscarPorId(10);
            assertNotNull(recuperado);
            assertFalse(recuperado.isActivo(), "Debe marcarse como inactivo (soft-delete)");
        }
    }

    // =========================================================================
    // Fake DAO para testing aislado sin depender de conexión SQL activa
    // =========================================================================

    private static class FakeArticuloDAO implements IArticuloDAO {
        final List<Articulo> articulos = new ArrayList<>();
        private int autoId = 100;

        @Override
        public Articulo buscarPorId(int idArticulo) {
            return articulos.stream().filter(a -> a.getIdArticulo() == idArticulo).findFirst().orElse(null);
        }

        @Override
        public List<Articulo> listarDisponibles() {
            return articulos.stream().filter(Articulo::isActivo).toList();
        }

        @Override
        public boolean decrementarStock(int idArticulo, int cantidad, Connection conn) {
            Articulo a = buscarPorId(idArticulo);
            if (a != null && a.tieneStock(cantidad)) {
                try {
                    a.descontarStock(cantidad);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            return false;
        }

        @Override
        public int crearArticulo(Articulo articulo) {
            int newId = ++autoId;
            articulo.setIdArticulo(newId);
            articulos.add(articulo);
            return newId;
        }

        @Override
        public boolean actualizarArticulo(Articulo articulo) {
            for (int i = 0; i < articulos.size(); i++) {
                if (articulos.get(i).getIdArticulo() == articulo.getIdArticulo()) {
                    articulos.set(i, articulo);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean eliminarArticulo(int idArticulo) {
            Articulo a = buscarPorId(idArticulo);
            if (a != null) {
                a.setActivo(false);
                return true;
            }
            return false;
        }

        @Override
        public boolean existeCodigo(String codigo, int idArticuloExcluir) {
            return articulos.stream()
                    .anyMatch(a -> a.getCodigo().equalsIgnoreCase(codigo) && a.getIdArticulo() != idArticuloExcluir);
        }
    }
}
