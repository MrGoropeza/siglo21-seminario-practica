package com.sgcv.model;

import java.math.BigDecimal;

/**
 * Consolidación diaria por canal/medio de pago en el libro mayor del día.
 */
public class DetalleArqueo {

    private int idDetalleArqueo;
    private int idArqueo;
    private String medioPago; // 'EFECTIVO' o 'TRANSFERENCIA'
    private BigDecimal totalCobranzas;
    private BigDecimal totalPagosProveedor;
    private BigDecimal saldoNeto;

    public DetalleArqueo() {
        this.medioPago = "EFECTIVO";
        this.totalCobranzas = BigDecimal.ZERO;
        this.totalPagosProveedor = BigDecimal.ZERO;
        this.saldoNeto = BigDecimal.ZERO;
    }

    public DetalleArqueo(int idDetalleArqueo, int idArqueo, String medioPago,
                         BigDecimal totalCobranzas, BigDecimal totalPagosProveedor, BigDecimal saldoNeto) {
        this.idDetalleArqueo = idDetalleArqueo;
        this.idArqueo = idArqueo;
        this.medioPago = medioPago != null ? medioPago.toUpperCase() : "EFECTIVO";
        this.totalCobranzas = totalCobranzas != null ? totalCobranzas : BigDecimal.ZERO;
        this.totalPagosProveedor = totalPagosProveedor != null ? totalPagosProveedor : BigDecimal.ZERO;
        this.saldoNeto = saldoNeto != null ? saldoNeto : this.totalCobranzas.subtract(this.totalPagosProveedor);
    }

    public DetalleArqueo(String medioPago, BigDecimal totalCobranzas, BigDecimal totalPagosProveedor, BigDecimal saldoNeto) {
        this(0, 0, medioPago, totalCobranzas, totalPagosProveedor, saldoNeto);
    }


    public int getIdDetalleArqueo() { return idDetalleArqueo; }
    public void setIdDetalleArqueo(int idDetalleArqueo) { this.idDetalleArqueo = idDetalleArqueo; }

    public int getIdArqueo() { return idArqueo; }
    public void setIdArqueo(int idArqueo) { this.idArqueo = idArqueo; }

    public String getMedioPago() { return medioPago; }
    public void setMedioPago(String medioPago) { this.medioPago = medioPago != null ? medioPago.toUpperCase() : "EFECTIVO"; }

    public BigDecimal getTotalCobranzas() { return totalCobranzas; }
    public void setTotalCobranzas(BigDecimal totalCobranzas) { this.totalCobranzas = totalCobranzas; }

    public BigDecimal getTotalPagosProveedor() { return totalPagosProveedor; }
    public void setTotalPagosProveedor(BigDecimal totalPagosProveedor) { this.totalPagosProveedor = totalPagosProveedor; }

    public BigDecimal getSaldoNeto() { return saldoNeto; }
    public void setSaldoNeto(BigDecimal saldoNeto) { this.saldoNeto = saldoNeto; }
}
