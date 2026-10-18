package com.sgcv;

import com.sgcv.dao.IArqueoDAO;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.ArqueoCaja;
import com.sgcv.model.ResumenDiarioDTO;
import com.sgcv.service.ArqueoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - Servicio de Conciliación y Arqueo Diario de Caja")
public class ArqueoServiceTest {

    private FakeArqueoDAO fakeArqueoDAO;
    private ArqueoService arqueoService;

    @BeforeEach
    void setUp() {
        fakeArqueoDAO = new FakeArqueoDAO();
        arqueoService = new ArqueoService(fakeArqueoDAO);
    }

    @Nested
    @DisplayName("1. Validaciones e Invariantes de Arqueo de Caja")
    class ValidacionesArqueoTest {

        @Test
        @DisplayName("Rechazar registro si el objeto de arqueo es nulo")
        void testArqueoNulo() {
            assertThrows(ValidacionException.class, () -> arqueoService.registrarArqueoCaja(null));
        }

        @Test
        @DisplayName("Rechazar registro si no se ingresó el conteo físico de billetes")
        void testEfectivoRealNulo() {
            ArqueoCaja arq = new ArqueoCaja();
            arq.setFechaArqueo(LocalDate.now());
            arq.setEfectivoEsperado(new BigDecimal("150000.00"));
            arq.setEfectivoRealContado(null);

            assertThrows(ValidacionException.class, () -> arqueoService.registrarArqueoCaja(arq));
        }

        @Test
        @DisplayName("Registrar arqueo exitosamente con fondos segregados para frigorífico")
        void testRegistrarArqueoExitoso() throws SQLException {
            ArqueoCaja arq = new ArqueoCaja();
            arq.setIdUsuario(1);
            arq.setFechaArqueo(LocalDate.now());
            arq.setSaldoInicialEfectivo(new BigDecimal("50000.00"));
            arq.setEfectivoEsperado(new BigDecimal("230000.00"));
            arq.setEfectivoRealContado(new BigDecimal("230000.00"));
            arq.setDiferenciaEfectivo(BigDecimal.ZERO);
            arq.setFondoReposicionSegregado(new BigDecimal("150000.00"));
            arq.setMargenNetoDia(new BigDecimal("30000.00"));
            arq.setEstado("CERRADO");

            int idGenerado = arqueoService.registrarArqueoCaja(arq);
            assertTrue(idGenerado > 0);
            assertEquals(1, fakeArqueoDAO.historico.size());
            assertEquals(new BigDecimal("150000.00"), fakeArqueoDAO.historico.get(0).getFondoReposicionSegregado());
            assertEquals(new BigDecimal("30000.00"), fakeArqueoDAO.historico.get(0).getMargenNetoDia());
        }
    }

    @Nested
    @DisplayName("2. Consultas y Cálculo de Métricas Diarias")
    class ConsultasArqueoTest {

        @Test
        @DisplayName("Calcular resumen diario devuelve DTO con cobranzas y costos")
        void testCalcularResumenDiario() throws SQLException {
            ResumenDiarioDTO resumen = arqueoService.calcularResumenDiario(LocalDate.now());
            assertNotNull(resumen);
            assertEquals(new BigDecimal("180000.00"), resumen.getCobranzasEfectivoTotal());
            assertEquals(new BigDecimal("140000.00"), resumen.getCostoReposicionTotal());
        }

        @Test
        @DisplayName("Obtener último arqueo e histórico registrado")
        void testHistorico() throws SQLException {
            ArqueoCaja arq = new ArqueoCaja();
            arq.setFechaArqueo(LocalDate.now());
            arq.setEfectivoRealContado(new BigDecimal("100000.00"));
            arqueoService.registrarArqueoCaja(arq);

            List<ArqueoCaja> lista = arqueoService.listarHistorico();
            assertEquals(1, lista.size());

            ArqueoCaja ultimo = arqueoService.obtenerUltimoArqueo();
            assertNotNull(ultimo);
        }
    }

    static class FakeArqueoDAO implements IArqueoDAO {
        final List<ArqueoCaja> historico = new ArrayList<>();
        int nextId = 1;

        @Override
        public ResumenDiarioDTO calcularResumenDiario(LocalDate fecha) {
            ResumenDiarioDTO dto = new ResumenDiarioDTO();
            dto.setFecha(fecha);
            dto.setBultosEntregadosTotal(25);
            dto.setFacturacionEntregasTotal(new BigDecimal("210000.00"));
            dto.setTotalCobrado(new BigDecimal("210000.00"));
            dto.setCobranzasEfectivoTotal(new BigDecimal("180000.00"));
            dto.setCobranzasTransferenciaTotal(new BigDecimal("30000.00"));
            dto.setCostoReposicionTotal(new BigDecimal("140000.00"));
            dto.setMargenBrutoEstimado(new BigDecimal("70000.00"));
            return dto;
        }

        @Override
        public int registrarArqueo(ArqueoCaja arq) {
            arq.setIdArqueo(nextId++);
            historico.add(arq);
            return arq.getIdArqueo();
        }

        @Override
        public ArqueoCaja obtenerUltimoArqueo() {
            return historico.isEmpty() ? null : historico.get(historico.size() - 1);
        }

        @Override
        public List<ArqueoCaja> listarHistorico() {
            return new ArrayList<>(historico);
        }
    }
}
