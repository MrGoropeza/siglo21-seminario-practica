package com.sgcv.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de dominio puro para {@link ArqueoCaja#estadoSegunDiferencia(BigDecimal)}.
 *
 * <p>La regla de negocio del CU-04 es: un arqueo sólo queda {@code "CERRADO"} cuando el
 * efectivo contado coincide exactamente con el esperado; cualquier desvío (faltante o
 * sobrante) obliga a marcarlo como {@code "AJUSTADO"}. Un dato ausente se trata como
 * caja sin desvío para no bloquear el cierre.</p>
 */
class ArqueoCajaTest {

    @Test
    @DisplayName("Diferencia cero deja el arqueo en estado CERRADO")
    void diferenciaCeroProduceEstadoCerrado() {
        assertEquals("CERRADO", ArqueoCaja.estadoSegunDiferencia(BigDecimal.ZERO),
                "Sin desvío de caja el arqueo debe quedar CERRADO");
    }

    @Test
    @DisplayName("Sobrante de 1500 marca el arqueo como AJUSTADO")
    void diferenciaPositivaProduceEstadoAjustado() {
        assertEquals("AJUSTADO", ArqueoCaja.estadoSegunDiferencia(new BigDecimal("1500")),
                "Un sobrante de $1500 es un desvío y debe quedar AJUSTADO");
    }

    @Test
    @DisplayName("Faltante de -1500 marca el arqueo como AJUSTADO")
    void diferenciaNegativaProduceEstadoAjustado() {
        assertEquals("AJUSTADO", ArqueoCaja.estadoSegunDiferencia(new BigDecimal("-1500")),
                "Un faltante de $1500 es un desvío y debe quedar AJUSTADO");
    }

    @Test
    @DisplayName("Diferencia nula se interpreta como CERRADO")
    void diferenciaNulaProduceEstadoCerrado() {
        assertEquals("CERRADO", ArqueoCaja.estadoSegunDiferencia(null),
                "Sin diferencia informada no hay desvío y el arqueo debe quedar CERRADO");
    }
}
