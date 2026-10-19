package com.sgcv.service;

import com.sgcv.exception.LimiteCreditoExcedidoException;
import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Cliente;
import com.sgcv.model.Cobranza;
import com.sgcv.model.DetalleCobranza;
import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;
import com.sgcv.model.ParadaResult;
import com.sgcv.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de integración de la cobranza contra el MySQL real ({@code sgcv_db}).
 *
 * <p>El primer test es un test RED deliberado sobre la fragilidad <strong>S3</strong>:
 * el sistema permite cobrar un importe mayor al saldo deudor del cliente, dejando la
 * cuenta corriente con saldo acreedor (negativo) y un recibo por más plata de la que
 * el cliente debía. La spec S3 exige rechazar esa operación con
 * {@link ValidacionException} y mensaje exacto
 * {@code "El monto cobrado no puede superar el saldo deudor del cliente."} sin persistir
 * ni la cobranza ni la modificación del saldo. Ese test debe observarse en RED hasta que
 * el código de producción valide la regla; no se lo debe adaptar para que pase.</p>
 *
 * <p>Los otros dos tests describen comportamientos que hoy sí funcionan (cobranza parcial
 * consistente y fecha de cobranza del mismo día que la entrega) y funcionan como red de
 * seguridad de la regresión.</p>
 */
class CobranzaIntegrationTest {

    /** {@code MENU-POL-10} es el artículo del seed con precio de venta base 9000.00. */
    private static final int ID_ARTICULO_MENU_POL_10 = 6;
    private static final BigDecimal PRECIO_MENU_POL_10 = new BigDecimal("9000.00");
    private static final BigDecimal COSTO_MENU_POL_10 = new BigDecimal("6500.00");

    private static final BigDecimal LIMITE_CREDITO_AMPLIO = new BigDecimal("250000.00");
    private static final int ID_USUARIO = 1;

    private final EntregaService entregaSvc = new EntregaService();

    // =========================================================================
    // S3 (RED): el cobro no puede superar el saldo deudor
    // =========================================================================

    @Test
    @DisplayName("S3: rechaza cobrar más que el saldo deudor y no persiste nada")
    void noPermiteCobrarMasQueElSaldoDeudor() throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
        Cliente cliente = IntegrationTestSupport.crearClienteDePrueba(
                "Cliente Test S3 Cobro Excesivo", LIMITE_CREDITO_AMPLIO);
        int idCliente = cliente.getIdCliente();
        try {
            // Deuda real del cliente: 1 bulto de MENU-POL-10 = $9000.00
            entregaSvc.registrarParadaCompleta(entregaDeUnBultoMenuPollo(cliente), null, false);
            assertMonto("9000.00", IntegrationTestSupport.leerSaldoCliente(idCliente),
                    "La entrega de 1 x MENU-POL-10 debe dejar una deuda de $9000.00, "
                            + "pero el saldo leído de la base fue "
                            + IntegrationTestSupport.leerSaldoCliente(idCliente));

            // Se intenta cobrar $50000.00 sobre una deuda de $9000.00
            Cobranza cobranzaExcesiva = cobranzaEnEfectivo(cliente, "50000.00",
                    "Cobro superior al saldo deudor");

            ValidacionException excepcion = assertThrows(ValidacionException.class,
                    () -> entregaSvc.registrarParadaCompleta(null, cobranzaExcesiva, false),
                    () -> "S3: cobrar $50000.00 sobre un saldo deudor de $9000.00 debe rechazarse "
                            + "con ValidacionException. Estado real observado en la base -> "
                            + estadoPersistidoDelCliente(idCliente));

            assertEquals("El monto cobrado no puede superar el saldo deudor del cliente.",
                    excepcion.getMessage(),
                    "El mensaje de rechazo de S3 debe ser el texto exacto de la spec, pero fue: "
                            + excepcion.getMessage());

            assertMonto("9000.00", IntegrationTestSupport.leerSaldoCliente(idCliente),
                    "El saldo deudor debe seguir intacto en $9000.00 cuando la cobranza es rechazada");
            assertEquals(0, IntegrationTestSupport.contarFilas("cobranzas", "id_cliente", idCliente),
                    "Una cobranza rechazada no debe quedar registrada en la base");
        } finally {
            IntegrationTestSupport.eliminarDatosDeCliente(idCliente);
        }
    }

    // =========================================================================
    // Comportamiento vigente: la cobranza parcial deja el saldo consistente
    // =========================================================================

    @Test
    @DisplayName("Una cobranza parcial descuenta exactamente el importe cobrado del saldo deudor")
    void remitoYCobranzaActualizanSaldoDeFormaConsistente() throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
        Cliente cliente = IntegrationTestSupport.crearClienteDePrueba(
                "Cliente Test Cobranza Parcial", LIMITE_CREDITO_AMPLIO);
        int idCliente = cliente.getIdCliente();
        try {
            ParadaResult resultado = entregaSvc.registrarParadaCompleta(
                    entregaDeUnBultoMenuPollo(cliente),
                    cobranzaEnEfectivo(cliente, "3000.00", "Pago parcial en efectivo"),
                    false);

            assertTrue(resultado.getIdEntrega() > 0,
                    "La parada con entrega y cobranza debe emitir remito, pero getIdEntrega() = "
                            + resultado.getIdEntrega());
            assertTrue(resultado.getIdCobranza() > 0,
                    "La parada con entrega y cobranza debe emitir recibo, pero getIdCobranza() = "
                            + resultado.getIdCobranza());

            assertMonto("6000.00", IntegrationTestSupport.leerSaldoCliente(idCliente),
                    "Deuda $9000.00 menos cobro $3000.00 debe dejar un saldo deudor de $6000.00, "
                            + "pero el saldo leído de la base fue "
                            + IntegrationTestSupport.leerSaldoCliente(idCliente));

            assertEquals(1, IntegrationTestSupport.contarFilas("cobranzas", "id_cliente", idCliente),
                    "Debe quedar exactamente 1 cobranza registrada para el cliente del test");

            assertMonto("3000.00",
                    IntegrationTestSupport.sumarDetallesCobranzaPorMedio(idCliente, "EFECTIVO"),
                    "La suma de los renglones EFECTIVO del cliente debe ser $3000.00");
        } finally {
            IntegrationTestSupport.eliminarDatosDeCliente(idCliente);
        }
    }

    // =========================================================================
    // Comportamiento vigente: la cobranza queda fechada el mismo día de la entrega
    // =========================================================================

    @Test
    @DisplayName("La cobranza persiste la fecha del mismo día que la entrega")
    void cobranzaPersisteFechaEnElMismoDiaQueLaEntrega() throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
        Cliente cliente = IntegrationTestSupport.crearClienteDePrueba(
                "Cliente Test Fecha Cobranza", LIMITE_CREDITO_AMPLIO);
        int idCliente = cliente.getIdCliente();
        try {
            ParadaResult resultado = entregaSvc.registrarParadaCompleta(
                    entregaDeUnBultoMenuPollo(cliente),
                    cobranzaEnEfectivo(cliente, "3000.00", "Pago en efectivo en la parada"),
                    false);

            int idEntrega = resultado.getIdEntrega();
            int idCobranza = resultado.getIdCobranza();
            assertTrue(idEntrega > 0 && idCobranza > 0,
                    "La parada debe generar remito y recibo, pero se obtuvo idEntrega = " + idEntrega
                            + " e idCobranza = " + idCobranza);

            LocalDate hoy = LocalDate.now();

            LocalDate fechaEntrega = IntegrationTestSupport.leerFecha(
                    "entregas", "fecha_entrega", "id_entrega", idEntrega);
            assertEquals(hoy, fechaEntrega,
                    "La fecha_entrega del remito debe ser la fecha de hoy (" + hoy
                            + "), pero la base tiene " + fechaEntrega);

            LocalDate fechaCobranza = IntegrationTestSupport.leerFecha(
                    "cobranzas", "fecha_cobranza", "id_cobranza", idCobranza);
            assertEquals(hoy, fechaCobranza,
                    "La fecha_cobranza del recibo debe ser la fecha de hoy (" + hoy
                            + "), pero la base tiene " + fechaCobranza);
        } finally {
            IntegrationTestSupport.eliminarDatosDeCliente(idCliente);
        }
    }

    // =========================================================================
    // Helpers del test
    // =========================================================================

    private static Entrega entregaDeUnBultoMenuPollo(Cliente cliente) {
        Entrega entrega = new Entrega();
        entrega.setIdCliente(cliente.getIdCliente());
        entrega.setIdUsuario(ID_USUARIO);
        entrega.setEstado("EMITIDA");
        entrega.agregarDetalle(new DetalleEntrega(
                ID_ARTICULO_MENU_POL_10,
                1,
                PRECIO_MENU_POL_10,
                COSTO_MENU_POL_10,
                BigDecimal.ZERO));
        return entrega;
    }

    private static Cobranza cobranzaEnEfectivo(Cliente cliente, String monto, String referencia) {
        BigDecimal importe = new BigDecimal(monto);
        Cobranza cobranza = new Cobranza();
        cobranza.setIdCliente(cliente.getIdCliente());
        cobranza.setIdUsuario(ID_USUARIO);
        cobranza.setMontoTotal(importe);
        cobranza.setObservaciones("Cobranza de test de integración");
        cobranza.agregarDetalle(new DetalleCobranza("EFECTIVO", importe, referencia));
        return cobranza;
    }

    private static void assertMonto(String esperado, BigDecimal real, String contexto) {
        BigDecimal esperadoNumerico = new BigDecimal(esperado);
        assertEquals(0, esperadoNumerico.compareTo(real),
                contexto + " (esperado " + esperadoNumerico.toPlainString()
                        + ", real " + (real != null ? real.toPlainString() : "null") + ")");
    }

    /**
     * Estado real persistido del cliente, incluido en el mensaje del assert RED para
     * dejar evidencia observable de lo que el sistema hizo cuando aceptó el cobro excesivo.
     */
    private static String estadoPersistidoDelCliente(int idCliente) {
        try {
            BigDecimal saldo = IntegrationTestSupport.leerSaldoCliente(idCliente);
            int cobranzas = IntegrationTestSupport.contarFilas("cobranzas", "id_cliente", idCliente);
            return "saldo_final=" + (saldo != null ? saldo.toPlainString() : "null")
                    + ", cobranzas_registradas=" + cobranzas;
        } catch (SQLException e) {
            return "no se pudo leer el estado real: " + e.getMessage();
        }
    }
}
