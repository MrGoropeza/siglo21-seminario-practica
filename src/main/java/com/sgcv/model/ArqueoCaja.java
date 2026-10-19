package com.sgcv.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad de dominio que representa el Cierre y Arqueo Diario de Caja (CU-04).
 * Reconcilia cobranzas, pagos y calcula la segregación de fondos:
 * Fondo de Reposición a Frigoríficos vs Margen Neto Real del Día.
 */
public class ArqueoCaja {

    private int idArqueo;
    private int idUsuario;
    private LocalDate fechaArqueo;
    private BigDecimal saldoInicialEfectivo;
    private BigDecimal efectivoEsperado;
    private BigDecimal efectivoRealContado;
    private BigDecimal diferenciaEfectivo;
    private BigDecimal fondoReposicionSegregado;
    private BigDecimal margenNetoDia;
    private String observaciones;
    private String estado; // 'CERRADO' o 'AJUSTADO'
    private LocalDateTime fechaCierre;
    private final List<DetalleArqueo> detalles;

    public ArqueoCaja() {
        this.fechaArqueo = LocalDate.now();
        this.saldoInicialEfectivo = BigDecimal.ZERO;
        this.efectivoEsperado = BigDecimal.ZERO;
        this.efectivoRealContado = BigDecimal.ZERO;
        this.diferenciaEfectivo = BigDecimal.ZERO;
        this.fondoReposicionSegregado = BigDecimal.ZERO;
        this.margenNetoDia = BigDecimal.ZERO;
        this.estado = "CERRADO";
        this.fechaCierre = LocalDateTime.now();
        this.detalles = new ArrayList<>();
    }

    public ArqueoCaja(int idArqueo, int idUsuario, LocalDate fechaArqueo,
                      BigDecimal saldoInicialEfectivo, BigDecimal efectivoEsperado,
                      BigDecimal efectivoRealContado, BigDecimal diferenciaEfectivo,
                      BigDecimal fondoReposicionSegregado, BigDecimal margenNetoDia,
                      String observaciones, String estado, LocalDateTime fechaCierre) {
        this.idArqueo = idArqueo;
        this.idUsuario = idUsuario > 0 ? idUsuario : 1;
        this.fechaArqueo = fechaArqueo != null ? fechaArqueo : LocalDate.now();
        this.saldoInicialEfectivo = saldoInicialEfectivo != null ? saldoInicialEfectivo : BigDecimal.ZERO;
        this.efectivoEsperado = efectivoEsperado != null ? efectivoEsperado : BigDecimal.ZERO;
        this.efectivoRealContado = efectivoRealContado != null ? efectivoRealContado : BigDecimal.ZERO;
        this.diferenciaEfectivo = diferenciaEfectivo != null ? diferenciaEfectivo : BigDecimal.ZERO;
        this.fondoReposicionSegregado = fondoReposicionSegregado != null ? fondoReposicionSegregado : BigDecimal.ZERO;
        this.margenNetoDia = margenNetoDia != null ? margenNetoDia : BigDecimal.ZERO;
        this.observaciones = observaciones;
        this.estado = estado != null ? estado : "CERRADO";
        this.fechaCierre = fechaCierre != null ? fechaCierre : LocalDateTime.now();
        this.detalles = new ArrayList<>();
    }

    public void recalcularDiferencia() {
        this.diferenciaEfectivo = this.efectivoRealContado.subtract(this.efectivoEsperado);
    }

    public boolean estaCuadrada() {
        return this.diferenciaEfectivo.compareTo(BigDecimal.ZERO) == 0;
    }

    public int getIdArqueo() { return idArqueo; }
    public void setIdArqueo(int idArqueo) { this.idArqueo = idArqueo; }

    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }

    public LocalDate getFechaArqueo() { return fechaArqueo; }
    public void setFechaArqueo(LocalDate fechaArqueo) { this.fechaArqueo = fechaArqueo; }

    public BigDecimal getSaldoInicialEfectivo() { return saldoInicialEfectivo; }
    public void setSaldoInicialEfectivo(BigDecimal saldoInicialEfectivo) { this.saldoInicialEfectivo = saldoInicialEfectivo; }

    public BigDecimal getEfectivoEsperado() { return efectivoEsperado; }
    public void setEfectivoEsperado(BigDecimal efectivoEsperado) { this.efectivoEsperado = efectivoEsperado; }

    public BigDecimal getEfectivoRealContado() { return efectivoRealContado; }
    public void setEfectivoRealContado(BigDecimal efectivoRealContado) { this.efectivoRealContado = efectivoRealContado; }

    public BigDecimal getDiferenciaEfectivo() { return diferenciaEfectivo; }
    public void setDiferenciaEfectivo(BigDecimal diferenciaEfectivo) { this.diferenciaEfectivo = diferenciaEfectivo; }

    public BigDecimal getFondoReposicionSegregado() { return fondoReposicionSegregado; }
    public void setFondoReposicionSegregado(BigDecimal fondoReposicionSegregado) { this.fondoReposicionSegregado = fondoReposicionSegregado; }

    public BigDecimal getMargenNetoDia() { return margenNetoDia; }
    public void setMargenNetoDia(BigDecimal margenNetoDia) { this.margenNetoDia = margenNetoDia; }

    public String getObservaciones() { return observaciones; }
    /**
     * Deriva el estado del arqueo a partir de la diferencia de caja.
     *
     * @param diferencia efectivo contado menos efectivo esperado
     * @return {@code "AJUSTADO"} si hay diferencia distinta de cero, {@code "CERRADO"} en caso contrario
     */
    public static String estadoSegunDiferencia(BigDecimal diferencia) {
        if (diferencia == null || diferencia.compareTo(BigDecimal.ZERO) == 0) {
            return "CERRADO";
        }
        return "AJUSTADO";
    }

    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }

    public List<DetalleArqueo> getDetalles() { return detalles; }

    public void agregarDetalle(DetalleArqueo detalle) {
        if (detalle != null) {
            this.detalles.add(detalle);
        }
    }
}

