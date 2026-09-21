package com.sgcv.model;

import java.math.BigDecimal;

/**
 * Entidad de dominio que representa a un cliente minorista (B2B).
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
        this.idCliente = idCliente;
        this.razonSocial = razonSocial;
        this.cuit = cuit;
        this.direccion = direccion;
        this.telefono = telefono;
        this.zonaReparto = zonaReparto;
        this.saldoCuentaCorriente = saldoCuentaCorriente != null ? saldoCuentaCorriente : BigDecimal.ZERO;
        this.limiteCredito = limiteCredito != null ? limiteCredito : BigDecimal.ZERO;
        this.activo = activo;
    }

    public boolean estaExcedido(BigDecimal montoAdicional) {
        if (montoAdicional == null) montoAdicional = BigDecimal.ZERO;
        BigDecimal saldoProyectado = this.saldoCuentaCorriente.add(montoAdicional);
        return saldoProyectado.compareTo(this.limiteCredito) > 0;
    }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String razonSocial) { this.razonSocial = razonSocial; }

    public String getCuit() { return cuit; }
    public void setCuit(String cuit) { this.cuit = cuit; }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getZonaReparto() { return zonaReparto; }
    public void setZonaReparto(String zonaReparto) { this.zonaReparto = zonaReparto; }

    public BigDecimal getSaldoCuentaCorriente() { return saldoCuentaCorriente; }
    public void setSaldoCuentaCorriente(BigDecimal saldoCuentaCorriente) { this.saldoCuentaCorriente = saldoCuentaCorriente; }

    public BigDecimal getLimiteCredito() { return limiteCredito; }
    public void setLimiteCredito(BigDecimal limiteCredito) { this.limiteCredito = limiteCredito; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }

    @Override
    public String toString() {
        return razonSocial + " (Saldo: $" + saldoCuentaCorriente + " / Límite: $" + limiteCredito + ")";
    }
}
