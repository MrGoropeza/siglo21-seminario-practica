package com.sgcv;

import com.sgcv.dao.IClienteDAO;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Cliente;
import com.sgcv.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Servicio de Clientes y Monitoreo de Riesgo Crediticio")
public class ClienteServiceTest {

    private FakeClienteDAO clienteDAO;
    private ClienteService clienteService;

    @BeforeEach
    void setUp() {
        clienteDAO = new FakeClienteDAO();
        clienteService = new ClienteService(clienteDAO);
    }

    @Nested
    @DisplayName("1. Evaluación de Límites de Crédito y Riesgo (CU-01 / CU-06)")
    class RiesgoCrediticioTest {

        @Test
        @DisplayName("Cliente solvente dentro del margen crediticio no genera estado de riesgo")
        void testClienteDentroDelLimite() throws SQLException {
            // Saldo actual 120.000, Límite 300.000 (Margen: 180.000)
            Cliente cliente = new Cliente(1, "Polleria San Cayetano", "30-71124589-8",
                    "Rivadavia 4520", "11-4567-8901", "Zona Oeste",
                    new BigDecimal("120000.00"), new BigDecimal("300000.00"), true);
            clienteDAO.guardar(cliente);

            // Operación de 100.000 (Proyectado: 220.000 <= 300.000)
            boolean enRiesgo = clienteService.verificarRiesgoCrediticio(1, new BigDecimal("100000.00"));
            assertFalse(enRiesgo, "El cliente tiene margen disponible, no debe alertar riesgo");
        }

        @Test
        @DisplayName("Caso frontera: importe proyectado igual al límite exacto no excede")
        void testCasoBordeLimiteExacto() throws SQLException {
            Cliente cliente = new Cliente(2, "Granja Don Tito", "27-32987654-2",
                    "Mitre 850", "11-4654-3344", "Zona Norte",
                    new BigDecimal("150000.00"), new BigDecimal("250000.00"), true);
            clienteDAO.guardar(cliente);

            // Operación de exactamente 100.000 (Proyectado: 250.000 == Límite)
            boolean enRiesgo = clienteService.verificarRiesgoCrediticio(2, new BigDecimal("100000.00"));
            assertFalse(enRiesgo, "Alcanzar el límite exacto no constituye exceso");
        }

        @Test
        @DisplayName("Alerta de riesgo cuando el pedido proyectado supera el límite autorizado")
        void testClienteExcedeLimite() throws SQLException {
            Cliente cliente = new Cliente(3, "Las Brasas", "30-68954125-9",
                    "Santa Fe 3200", "11-4821-5566", "Zona Norte",
                    new BigDecimal("220000.00"), new BigDecimal("250000.00"), true);
            clienteDAO.guardar(cliente);

            // Pedido por 35.000 (Proyectado: 255.000 > 250.000)
            boolean enRiesgo = clienteService.verificarRiesgoCrediticio(3, new BigDecimal("35000.00"));
            assertTrue(enRiesgo, "Debe activar alerta de exceso crediticio");
        }

        @Test
        @DisplayName("Cliente con deuda previa que ya supera el límite autorizado")
        void testClientePreviamenteExcedido() throws SQLException {
            // Saldo 420.000, Límite 400.000 (Ya en mora o exceso)
            Cliente moroso = new Cliente(4, "Rotiseria El Sol", "20-28456123-4",
                    "Belgrano 1240", "11-4629-1122", "Zona Oeste",
                    new BigDecimal("420000.00"), new BigDecimal("400000.00"), true);
            clienteDAO.guardar(moroso);

            assertTrue(clienteService.verificarRiesgoCrediticio(4, BigDecimal.ZERO),
                    "Incluso con pedido de $0 debe reportar estado de exceso");
        }

        @Test
        @DisplayName("Lanza ValidacionException si el cliente no existe en la base")
        void testClienteInexistente() {
            assertThrows(ValidacionException.class, () ->
                    clienteService.verificarRiesgoCrediticio(999, new BigDecimal("10000.00"))
            );
        }
    }

    @Nested
    @DisplayName("2. Consultas de Maestro de Clientes")
    class ConsultasClienteTest {

        @Test
        @DisplayName("Recuperación por ID y listado de clientes activos")
        void testBuscarYListar() throws SQLException {
            Cliente c1 = new Cliente(1, "Polleria A", "20-11111111-1", "Calle 1", "111", "Zona 1",
                    BigDecimal.ZERO, new BigDecimal("100000.00"), true);
            Cliente c2 = new Cliente(2, "Polleria B", "20-22222222-2", "Calle 2", "222", "Zona 2",
                    BigDecimal.ZERO, new BigDecimal("200000.00"), true);
            clienteDAO.guardar(c1);
            clienteDAO.guardar(c2);

            assertEquals(2, clienteService.listarActivos().size());
            assertEquals("Polleria A", clienteService.buscarPorId(1).getRazonSocial());
            assertEquals("Polleria B", clienteService.buscarPorId(2).getRazonSocial());
            assertNull(clienteService.buscarPorId(99));
        }
    }

    // =========================================================================
    // Fake DAO para Clientes
    // =========================================================================

    private static class FakeClienteDAO implements IClienteDAO {
        private final Map<Integer, Cliente> datos = new HashMap<>();

        void guardar(Cliente c) { datos.put(c.getIdCliente(), c); }

        @Override
        public Cliente buscarPorId(int idCliente) { return datos.get(idCliente); }

        @Override
        public List<Cliente> listarActivos() { return new ArrayList<>(datos.values()); }

        @Override
        public boolean actualizarSaldo(int idCliente, BigDecimal variacion, Connection conn) {
            Cliente c = datos.get(idCliente);
            if (c != null) {
                c.setSaldoCuentaCorriente(c.getSaldoCuentaCorriente().add(variacion));
                return true;
            }
            return false;
        }
    }
}
