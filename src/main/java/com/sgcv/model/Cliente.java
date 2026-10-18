package com.sgcv.model;

import com.sgcv.exception.ValidacionException;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad de dominio que representa a un cliente minorista (B2B) del canal de distribución avícola.
 * Incorpora reglas de negocio para el control de exposición y riesgo crediticio en cuenta corriente.
 */
public class Cliente {

    private int idCliente;
    private String razonSocial;
    private String cuit;
    private String direccion;
    private String telefono;
    private String zonaReparto;
    private BigDecimal saldoCuentaCorriente;
    private BigDecimal limiteCredito;
    private boolean activo;

    public Cliente() {
        this.saldoCuentaCorriente = BigDecimal.ZERO;
        this.limiteCredito = BigDecimal.ZERO;
        this.activo = true;
    }

    public Cliente(int idCliente, String razonSocial, String cuit, String direccion, 
                   String telefono, String zonaReparto, BigDecimal saldoCuentaCorriente, 
                   BigDecimal limiteCredito, boolean activo) {
        if (razonSocial == null || razonSocial.trim().isEmpty()) {
            throw new ValidacionException("La razón social del cliente es obligatoria.");
        }
        this.idCliente = idCliente;
        this.razonSocial = razonSocial.trim();
        this.cuit = cuit != null ? cuit.trim() : "";
        this.direccion = direccion != null ? direccion.trim() : "";
        this.telefono = telefono != null ? telefono.trim() : "";
        this.zonaReparto = zonaReparto != null ? zonaReparto.trim() : "";
        this.saldoCuentaCorriente = saldoCuentaCorriente != null ? saldoCuentaCorriente : BigDecimal.ZERO;
        this.limiteCredito = limiteCredito != null ? limiteCredito : BigDecimal.ZERO;
        this.activo = activo;
    }

    /**
     * Regla de Negocio (CU-01 S10):
     * Evalúa si un importe adicional proyectado excede el límite de crédito acordado.
     * @param montoAdicional importe de la nueva entrega o pedido
     * @return true si saldoActual + montoAdicional > limiteCredito
     */
    public boolean estaExcedido(BigDecimal montoAdicional) {
        BigDecimal adicional = montoAdicional != null ? montoAdicional : BigDecimal.ZERO;
        BigDecimal saldoProyectado = this.saldoCuentaCorriente.add(adicional);
        return saldoProyectado.compareTo(this.limiteCredito) > 0;
    }

    /**
     * Calcula el margen disponible de crédito antes de superar el límite.
     * @return límite menos saldo actual (o 0 si ya está excedido)
     */
    public BigDecimal getCreditoDisponible() {
        BigDecimal disponible = this.limiteCredito.subtract(this.saldoCuentaCorriente);
        return disponible.max(BigDecimal.ZERO);
    }

    public BigDecimal calcularCreditoDisponible() {
        return getCreditoDisponible();
    }


    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) {
        if (razonSocial == null || razonSocial.trim().isEmpty()) {
            throw new ValidacionException("La razón social del cliente es obligatoria.");
        }
        this.razonSocial = razonSocial.trim();
    }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit != null ? cuit.trim() : ""; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion != null ? direccion.trim() : ""; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono != null ? telefono.trim() : ""; }

    public String getZonaReparto() { return zonaReparto; }
    public void setZonaReparto(String zonaReparto) { this.zonaReparto = zonaReparto != null ? zonaReparto.trim() : ""; }

    public BigDecimal getSaldoCuentaCorriente() { return saldoCuentaCorriente; }
    public void setSaldoCuentaCorriente(BigDecimal saldoCuentaCorriente) {
        this.saldoCuentaCorriente = saldoCuentaCorriente != null ? saldoCuentaCorriente : BigDecimal.ZERO;
    }

    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public void setLimiteCredito(BigDecimal limiteCredito) {
        if (limiteCredito != null && limiteCredito.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El límite de crédito no puede ser negativo.");
        }
        this.limiteCredito = limiteCredito != null ? limiteCredito : BigDecimal.ZERO;
    }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return idCliente == cliente.idCliente;
    }

    @Override
    public int hashCode() {
        return Objects.hash(idCliente);
    }

    @Override
    public String toString() {
        return razonSocial + " (Saldo: $" + saldoCuentaCorriente + " / Límite: $" + limiteCredito + ")";
    }
}
