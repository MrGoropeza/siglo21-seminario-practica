package com.sgcv.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de dominio puro para las reglas de crédito de {@link Cliente}.
 * No requieren base de datos ni dobles de JDBC.
 */
class ClienteTest {

    private static Cliente cliente(BigDecimal saldo, BigDecimal limite) {
        return new Cliente(
                1,
                "Distribuidora Avícola del Norte S.A.",
                "30-12345678-9",
                "Av. Siempre Viva 123",
                "351-555-0100",
                "Zona Norte",
                saldo,
                limite,
                true);
    }

    @Test
    @DisplayName("estaExcedido devuelve false cuando el saldo proyectado queda por debajo del límite")
    void estaExcedidoDevuelveFalsePorDebajoDelLimite() {
        Cliente cliente = cliente(new BigDecimal("1000"), new BigDecimal("1500"));

        boolean excedido = cliente.estaExcedido(new BigDecimal("400"));

        assertFalse(excedido,
                "1000 + 400 = 1400 no supera el límite de 1500, por lo que no debe considerarse excedido");
    }

    @Test
    @DisplayName("estaExcedido devuelve false en el límite exacto porque la regla compara con '>'")
    void estaExcedidoDevuelveFalseEnElLimiteExacto() {
        Cliente cliente = cliente(new BigDecimal("1000"), new BigDecimal("1500"));

        boolean excedido = cliente.estaExcedido(new BigDecimal("500"));

        assertFalse(excedido,
                "1000 + 500 = 1500 es exactamente el límite; la regla es estricta (>) y no debe excederse");
    }

    @Test
    @DisplayName("estaExcedido devuelve true cuando el saldo proyectado supera el límite")
    void estaExcedidoDevuelveTruePorEncimaDelLimite() {
        Cliente cliente = cliente(new BigDecimal("1000"), new BigDecimal("1500"));

        boolean excedido = cliente.estaExcedido(new BigDecimal("501"));

        assertTrue(excedido,
                "1000 + 501 = 1501 supera el límite de 1500, por lo que debe considerarse excedido");
    }

    @Test
    @DisplayName("estaExcedido(null) se comporta como un monto adicional cero")
    void estaExcedidoConMontoNuloSeComportaComoCero() {
        Cliente dentroDelLimite = cliente(new BigDecimal("1000"), new BigDecimal("1500"));
        Cliente porEncimaDelLimite = cliente(new BigDecimal("1501"), new BigDecimal("1500"));

        assertFalse(dentroDelLimite.estaExcedido(null),
                "Con saldo 1000 y límite 1500, un monto nulo (cero) no debe exceder el límite");
        assertTrue(porEncimaDelLimite.estaExcedido(null),
                "Con saldo 1501 y límite 1500, un monto nulo (cero) debe seguir marcando excedido");
    }

    @Test
    @DisplayName("getCreditoDisponible devuelve la diferencia entre límite y saldo")
    void getCreditoDisponibleDevuelveElMargenRestante() {
        Cliente cliente = cliente(new BigDecimal("1000"), new BigDecimal("1500"));

        BigDecimal disponible = cliente.getCreditoDisponible();

        assertEquals(0, disponible.compareTo(new BigDecimal("500")),
                "Con saldo 1000 y límite 1500 el crédito disponible debe ser 500, pero fue " + disponible);
    }

    @Test
    @DisplayName("getCreditoDisponible nunca devuelve un valor negativo")
    void getCreditoDisponibleNoEsNegativoSiElSaldoExcedeElLimite() {
        Cliente cliente = cliente(new BigDecimal("2000"), new BigDecimal("1500"));

        BigDecimal disponible = cliente.getCreditoDisponible();

        assertEquals(0, disponible.compareTo(BigDecimal.ZERO),
                "Con saldo 2000 y límite 1500 el crédito disponible debe ser 0, pero fue " + disponible);
    }
}
