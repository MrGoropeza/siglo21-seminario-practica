package com.sgcv;

import com.sgcv.dao.IArticuloDAO;
import com.sgcv.dao.IClienteDAO;
import com.sgcv.dao.IEntregaDAO;
import com.sgcv.exception.LimiteCreditoExcedidoException;
import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Articulo;
import com.sgcv.model.ArticuloEstandar;
import com.sgcv.model.ArticuloPromocional;
import com.sgcv.model.Cliente;
import com.sgcv.model.Cobranza;
import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;
import com.sgcv.service.EntregaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Servicio de Entregas y Control de Negocio")
public class EntregaServiceTest {

    private FakeClienteDAO clienteDAO;
    private FakeArticuloDAO articuloDAO;
    private FakeEntregaDAO entregaDAO;
    private FakeCobranzaDAO cobranzaDAO;
    private EntregaService entregaService;
    private Connection dummyConnection;

    @BeforeEach
    void setUp() {
        clienteDAO = new FakeClienteDAO();
        articuloDAO = new FakeArticuloDAO();
        entregaDAO = new FakeEntregaDAO();
        cobranzaDAO = new FakeCobranzaDAO();

        // Conexión simulada para transacciones desacopladas de servidor JDBC
        dummyConnection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getReturnType().equals(boolean.class)) {
                        return false;
                    }
                    if (method.getReturnType().equals(int.class)) {
                        return 0;
                    }
                    return null;
                }
        );

        entregaService = new EntregaService(clienteDAO, articuloDAO, entregaDAO, cobranzaDAO, () -> dummyConnection);
    }


    @Nested
    @DisplayName("1. Control de Stock Físico")
    class ControlStockTest {

        @Test
        @DisplayName("Lanza StockInsuficienteException si la cantidad solicitada supera la existencia física")
        void testLanzaStockInsuficienteException() {
            Cliente cliente = new Cliente(1, "Polleria San Cayetano", "30-71124589-8",
                    "Rivadavia 4520", "11-4567-8901", "Zona Oeste",
                    new BigDecimal("50000.00"), new BigDecimal("300000.00"), true);
            clienteDAO.guardar(cliente);

            ArticuloEstandar art = new ArticuloEstandar(10, "POLL-ENT-20", "Pollo Entero",
                    "Caja x 20kg", new BigDecimal("40000.00"), new BigDecimal("50000.00"),
                    8, 5, true); // Solo 8 en stock
            articuloDAO.guardar(art);

            Entrega entrega = new Entrega();
            entrega.setIdCliente(1);
            entrega.agregarDetalle(new DetalleEntrega(10, "POLL-ENT-20", "Pollo Entero Fresco",
                    15, new BigDecimal("50000.00"), new BigDecimal("40000.00"), BigDecimal.ZERO)); // Solicita 15

            StockInsuficienteException ex = assertThrows(StockInsuficienteException.class, () ->
                    entregaService.registrarEntrega(entrega)
            );

            assertEquals("POLL-ENT-20", ex.getCodigoArticulo());
            assertEquals(8, ex.getStockDisponible());
            assertEquals(15, ex.getCantidadSolicitada());
            assertTrue(ex.getMessage().contains("Stock insuficiente"));
        }
    }

    @Nested
    @DisplayName("2. Control de Riesgo Crediticio y CU-01 S10")
    class ControlCreditoTest {

        @Test
        @DisplayName("Lanza LimiteCreditoExcedidoException si saldo proyectado supera límite autorizado sin autorización")
        void testLanzaLimiteCreditoExcedidoException() {
            // Saldo actual 280.000, Límite 300.000 (Margen disponible: 20.000)
            Cliente cliente = new Cliente(2, "Rotiseria El Sol", "20-28456123-4",
                    "Belgrano 1240", "11-4629-1122", "Zona Oeste",
                    new BigDecimal("280000.00"), new BigDecimal("300000.00"), true);
            clienteDAO.guardar(cliente);

            ArticuloEstandar artEstandar = new ArticuloEstandar(20, "PECH-SUP-15", "Suprema Pechuga",
                    "Caja x 15kg", new BigDecimal("50000.00"), new BigDecimal("70000.00"),
                    50, 10, true);
            articuloDAO.guardar(artEstandar);

            // 1 bulto = 70.000 + recargo frío 3% (72.100) -> saldo proyectado = 352.100 > 300.000
            Entrega entrega = new Entrega();
            entrega.setIdCliente(2);
            entrega.agregarDetalle(new DetalleEntrega(20, "PECH-SUP-15", "Suprema Pechuga",
                    1, new BigDecimal("70000.00"), new BigDecimal("50000.00"), BigDecimal.ZERO));

            LimiteCreditoExcedidoException ex = assertThrows(LimiteCreditoExcedidoException.class, () ->
                    entregaService.registrarEntrega(entrega)
            );

            assertEquals(2, ex.getIdCliente());
            assertEquals(new BigDecimal("280000.00"), ex.getSaldoActual());
            assertEquals(new BigDecimal("300000.00"), ex.getLimiteCredito());
            assertTrue(ex.getExceso().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(ex.getMessage().contains("Límite de crédito excedido"));
        }

        @Test
        @DisplayName("Permite registrar la entrega si se aprueba explícitamente el exceso crediticio (CU-01 S10)")
        void testAutorizacionExcepcionalExcesoCredito() throws Exception {
            Cliente cliente = new Cliente(3, "Granja Don Tito", "27-32987654-2",
                    "Mitre 850", "11-4654-3344", "Zona Norte",
                    new BigDecimal("190000.00"), new BigDecimal("200000.00"), true);
            clienteDAO.guardar(cliente);

            ArticuloEstandar art = new ArticuloEstandar(30, "POLL-ENT", "Pollo Entero",
                    "Caja x 20kg", new BigDecimal("30000.00"), new BigDecimal("40000.00"),
                    30, 5, true);
            articuloDAO.guardar(art);

            Entrega entrega = new Entrega();
            entrega.setIdCliente(3);
            entrega.agregarDetalle(new DetalleEntrega(30, "POLL-ENT", "Pollo Entero",
                    2, new BigDecimal("40000.00"), new BigDecimal("30000.00"), BigDecimal.ZERO));
            // Total = 80.000, proyectado = 270.000 > 200.000

            // Se envía con flag autorizarExcesoCredito = true
            boolean confirmada = entregaService.registrarEntrega(entrega, true);

            assertTrue(confirmada);
            assertNotNull(entrega.getNumeroRemito());
            assertEquals(1, entregaDAO.entregasPersistidas.size());
        }
    }

    @Nested
    @DisplayName("3. Operación Exitosa y Snapshot Inmutable de Precios")
    class EmisionExitosaTest {

        @Test
        @DisplayName("Registro exitoso dentro del límite con stock suficiente y snapshot congelado")
        void testRegistrarEntregaExitosa() throws Exception {
            Cliente cliente = new Cliente(4, "Las Brasas", "30-68954125-9",
                    "Santa Fe 3200", "11-4821-5566", "Zona Norte",
                    new BigDecimal("10000.00"), new BigDecimal("500000.00"), true);
            clienteDAO.guardar(cliente);

            ArticuloPromocional promo = new ArticuloPromocional(40, "POLL-ENT-20", "Pollo Entero",
                    "Caja x 20kg", new BigDecimal("40000.00"), new BigDecimal("50000.00"),
                    100, 10, true);
            articuloDAO.guardar(promo);

            Entrega entrega = new Entrega();
            entrega.setIdCliente(4);
            // 10 bultos con 5% de descuento polimórfico = 47.500 c/u
            entrega.agregarDetalle(new DetalleEntrega(40, "POLL-ENT-20", "Pollo Entero",
                    10, new BigDecimal("50000.00"), new BigDecimal("40000.00"), BigDecimal.ZERO));

            boolean exito = entregaService.registrarEntrega(entrega);

            assertTrue(exito);
            assertEquals(new BigDecimal("47500.00"), entrega.getDetalles().get(0).getPrecioUnitarioCongelado());
            assertEquals(new BigDecimal("475000.00"), entrega.getTotalEntrega());
            assertEquals(90, promo.getStockDeposito(), "El stock debió decrementar 10 bultos");
        }

        @Test
        @DisplayName("Lanza ValidacionException si no contiene renglones o cliente no existe")
        void testValidacionesEstructurales() {
            Entrega vacia = new Entrega();
            assertThrows(ValidacionException.class, () -> entregaService.registrarEntrega(vacia));

            Entrega sinCliente = new Entrega();
            sinCliente.setIdCliente(999);
            sinCliente.agregarDetalle(new DetalleEntrega(1, "COD", "Desc", 1, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ZERO));
            assertThrows(ValidacionException.class, () -> entregaService.registrarEntrega(sinCliente));
        }
    }

    @Nested
    @DisplayName("4. Parada Comercial en Ruta (CU-01 + CU-02)")
    class ParadaComercialTest {

        @Test
        @DisplayName("Registra cobranza de deuda anterior sin dejar mercadería (0 bultos) generando solo recibo")
        void testRegistrarParadaSoloCobranzaDeuda() throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
            Cliente cliente = new Cliente(1, "Rotiseria El Sol", "30-71987654-2",
                    "San Martin 1200", "11-9876-5432", "Zona Oeste",
                    new BigDecimal("100000.00"), new BigDecimal("300000.00"), true);
            clienteDAO.guardar(cliente);

            Cobranza cob = new Cobranza();
            cob.setIdCliente(1);
            cob.setMontoTotal(new BigDecimal("40000.00"));
            cob.agregarDetalle(new com.sgcv.model.DetalleCobranza("EFECTIVO", new BigDecimal("40000.00"), "Cobro en camioneta"));

            com.sgcv.model.ParadaResult res = entregaService.registrarParadaCompleta(null, cob, false);

            assertFalse(res.huboEntrega());
            assertTrue(res.huboCobranza());
            assertNotNull(res.getNumeroRecibo());
            assertEquals(new BigDecimal("40000.00"), res.getTotalCobrado());
            assertEquals(new BigDecimal("60000.00"), res.getNuevoSaldoCliente());
        }

        @Test
        @DisplayName("Registra entrega y cobranza mixta simultáneamente emitiendo remito y recibo")
        void testRegistrarParadaMixtaEntregaYCobranza() throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
            Cliente cliente = new Cliente(2, "Polleria Los Primos", "30-88888888-8",
                    "Mitre 500", "11-2222-3333", "Zona Sur",
                    new BigDecimal("50000.00"), new BigDecimal("400000.00"), true);
            clienteDAO.guardar(cliente);

            ArticuloEstandar art = new ArticuloEstandar(1, "POLL-ENT-20", "Pollo Entero",
                    "Caja x 20kg", new BigDecimal("42000.00"), new BigDecimal("54000.00"),
                    30, 5, true);
            articuloDAO.guardar(art);

            Entrega entrega = new Entrega();
            entrega.setIdCliente(2);
            entrega.agregarDetalle(new DetalleEntrega(1, 2, new BigDecimal("54000.00"), new BigDecimal("42000.00"), BigDecimal.ZERO)); // Total = 108.000

            Cobranza cob = new Cobranza();
            cob.setIdCliente(2);
            cob.setMontoTotal(new BigDecimal("50000.00"));
            cob.agregarDetalle(new com.sgcv.model.DetalleCobranza("EFECTIVO", new BigDecimal("50000.00"), "Pago parcial"));

            com.sgcv.model.ParadaResult res = entregaService.registrarParadaCompleta(entrega, cob, false);

            assertTrue(res.huboEntrega());
            assertTrue(res.huboCobranza());
            assertEquals(new BigDecimal("108000.00"), res.getTotalEntrega());
            assertEquals(new BigDecimal("50000.00"), res.getTotalCobrado());
            // Saldo anterior 50.000 + 108.000 - 50.000 = 108.000
            assertEquals(new BigDecimal("108000.00"), res.getNuevoSaldoCliente());
            assertEquals(28, art.getStockDeposito());
        }

        @Test
        @DisplayName("Permite autorizar excepcionalmente exceso de crédito en parada comercial")
        void testRegistrarParadaExcesoCreditoAutorizado() throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
            Cliente cliente = new Cliente(3, "Supermercado Norte", "30-99999999-9",
                    "Av Belgrano 100", "11-4444-5555", "Zona Norte",
                    new BigDecimal("90000.00"), new BigDecimal("100000.00"), true);
            clienteDAO.guardar(cliente);

            ArticuloEstandar art = new ArticuloEstandar(1, "POLL-ENT-20", "Pollo Entero",
                    "Caja x 20kg", new BigDecimal("42000.00"), new BigDecimal("54000.00"),
                    20, 5, true);
            articuloDAO.guardar(art);

            Entrega entrega = new Entrega();
            entrega.setIdCliente(3);
            entrega.agregarDetalle(new DetalleEntrega(1, 1, new BigDecimal("54000.00"), new BigDecimal("42000.00"), BigDecimal.ZERO)); // Nuevo saldo = 144.000 > 100.000

            // Sin autorizar debe lanzar LimiteCreditoExcedidoException
            assertThrows(LimiteCreditoExcedidoException.class, () ->
                    entregaService.registrarParadaCompleta(entrega, null, false)
            );

            // Con autorizarExcesoCredito = true debe prosperar
            com.sgcv.model.ParadaResult res = entregaService.registrarParadaCompleta(entrega, null, true);
            assertTrue(res.huboEntrega());
            assertEquals(new BigDecimal("144000.00"), res.getNuevoSaldoCliente());
        }
    }

    // =========================================================================
    // Implementaciones Simuladas (Fakes de Prueba) para Ejecución Hermética
    // =========================================================================

    private static class FakeClienteDAO implements IClienteDAO {
        private final Map<Integer, Cliente> repo = new HashMap<>();

        void guardar(Cliente c) { repo.put(c.getIdCliente(), c); }

        @Override
        public Cliente buscarPorId(int idCliente) { return repo.get(idCliente); }

        @Override
        public List<Cliente> listarActivos() { return new ArrayList<>(repo.values()); }

        @Override
        public boolean actualizarSaldo(int idCliente, BigDecimal variacion, Connection conn) {
            Cliente c = repo.get(idCliente);
            if (c != null) {
                c.setSaldoCuentaCorriente(c.getSaldoCuentaCorriente().add(variacion));
                return true;
            }
            return false;
        }

        @Override
        public List<com.sgcv.model.MovimientoCuentaCorriente> obtenerExtractoCuentaCorriente(int idCliente) {
            return new ArrayList<>();
        }
    }

    private static class FakeArticuloDAO implements IArticuloDAO {
        private final Map<Integer, Articulo> repo = new HashMap<>();

        void guardar(Articulo a) { repo.put(a.getIdArticulo(), a); }

        @Override
        public Articulo buscarPorId(int idArticulo) { return repo.get(idArticulo); }

        @Override
        public List<Articulo> listarDisponibles() { return new ArrayList<>(repo.values()); }

        @Override
        public boolean decrementarStock(int idArticulo, int cantidad, Connection conn) {
            Articulo a = repo.get(idArticulo);
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
            int id = repo.size() + 1;
            articulo.setIdArticulo(id);
            repo.put(id, articulo);
            return id;
        }

        @Override
        public boolean actualizarArticulo(Articulo articulo) {
            repo.put(articulo.getIdArticulo(), articulo);
            return true;
        }

        @Override
        public boolean eliminarArticulo(int idArticulo) {
            repo.remove(idArticulo);
            return true;
        }

        @Override
        public boolean existeCodigo(String codigo, int idArticuloExcluir) {
            return repo.values().stream()
                    .anyMatch(a -> a.getIdArticulo() != idArticuloExcluir && a.getCodigo().equalsIgnoreCase(codigo));
        }
    }


    private static class FakeEntregaDAO implements IEntregaDAO {
        final List<Entrega> entregasPersistidas = new ArrayList<>();
        private int contador = 1;

        @Override
        public int insertar(Entrega entrega, Connection conn) {
            entrega.setIdEntrega(contador);
            entregasPersistidas.add(entrega);
            return contador++;
        }

        @Override
        public boolean insertarDetalle(int idEntrega, DetalleEntrega det, Connection conn) {
            return true;
        }

        @Override
        public String obtenerSiguienteNumeroRemito() {
            return String.format("REM-0001-%08d", contador);
        }
    }

    private static class FakeCobranzaDAO implements com.sgcv.dao.ICobranzaDAO {
        private int contador = 1;
        final List<com.sgcv.model.Cobranza> cobranzas = new ArrayList<>();

        @Override
        public int registrarCobranza(com.sgcv.model.Cobranza cobranza) {
            return registrarCobranza(cobranza, null);
        }

        @Override
        public int registrarCobranza(com.sgcv.model.Cobranza cobranza, Connection conn) {
            cobranza.setIdCobranza(contador);
            cobranza.setNumeroRecibo(String.format("REC-0001-%08d", contador++));
            cobranzas.add(cobranza);
            return cobranza.getIdCobranza();
        }

        @Override
        public List<com.sgcv.model.Cobranza> listarPorFecha(java.time.LocalDate fecha) {
            return cobranzas;
        }

        @Override
        public BigDecimal obtenerTotalCobradoHoyPorMedio(String medioPago, java.time.LocalDate fecha, Connection conn) {
            return BigDecimal.ZERO;
        }
    }
}

