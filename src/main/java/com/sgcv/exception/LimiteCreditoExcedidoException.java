package com.sgcv.exception;

import java.math.BigDecimal;

/**
 * Excepción de negocio lanzada cuando una operación comercial proyectada
 * excede el límite de crédito acordado para la cuenta corriente del cliente.
 */
public class LimiteCreditoExcedidoException extends Exception {

    private final int idCliente;
    private final BigDecimal saldoActual;
    private final BigDecimal limiteCredito;
    private final BigDecimal montoOperacion;
    private final BigDecimal exceso;

    public LimiteCreditoExcedidoException(String mensaje, int idCliente, BigDecimal saldoActual,
                                          BigDecimal limiteCredito, BigDecimal montoOperacion) {
        super(mensaje);
        this.idCliente = idCliente;
        this.saldoActual = saldoActual != null ? saldoActual : BigDecimal.ZERO;
        this.limiteCredito = limiteCredito != null ? limiteCredito : BigDecimal.ZERO;
        this.montoOperacion = montoOperacion != null ? montoOperacion : BigDecimal.ZERO;
        BigDecimal proyectado = this.saldoActual.add(this.montoOperacion);
        this.exceso = proyectado.subtract(this.limiteCredito).max(BigDecimal.ZERO);
    }

    public LimiteCreditoExcedidoException(String mensaje) {
        super(mensaje);
        this.idCliente = 0;
        this.saldoActual = BigDecimal.ZERO;
        this.limiteCredito = BigDecimal.ZERO;
        this.montoOperacion = BigDecimal.ZERO;
        this.exceso = BigDecimal.ZERO;
    }

    public LimiteCreditoExcedidoException(String mensaje, Throwable cause) {
        super(mensaje, cause);
        this.idCliente = 0;
        this.saldoActual = BigDecimal.ZERO;
        this.limiteCredito = BigDecimal.ZERO;
        this.montoOperacion = BigDecimal.ZERO;
        this.exceso = BigDecimal.ZERO;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public BigDecimal getSaldoActual() {
        return saldoActual;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public BigDecimal getMontoOperacion() {
        return montoOperacion;
    }

    public BigDecimal getExceso() {
        return exceso;
    }
}
