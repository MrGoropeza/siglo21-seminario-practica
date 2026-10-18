package com.sgcv.exception;

/**
 * Excepción de regla de negocio lanzada cuando la existencia física en depósito
 * es inferior a la cantidad solicitada en la orden o entrega.
 */
public class StockInsuficienteException extends Exception {

    private final String codigoArticulo;
    private final int stockDisponible;
    private final int cantidadSolicitada;

    public StockInsuficienteException(String codigoArticulo, int stockDisponible, int cantidadSolicitada) {
        super(String.format("Stock insuficiente para artículo '%s'. Disponible en depósito: %d, Solicitado: %d.",
                codigoArticulo, stockDisponible, cantidadSolicitada));
        this.codigoArticulo = codigoArticulo;
        this.stockDisponible = stockDisponible;
        this.cantidadSolicitada = cantidadSolicitada;
    }

    public StockInsuficienteException(String mensaje) {
        super(mensaje);
        this.codigoArticulo = "";
        this.stockDisponible = 0;
        this.cantidadSolicitada = 0;
    }

    public StockInsuficienteException(String mensaje, Throwable cause) {
        super(mensaje, cause);
        this.codigoArticulo = "";
        this.stockDisponible = 0;
        this.cantidadSolicitada = 0;
    }

    public String getCodigoArticulo() {
        return codigoArticulo;
    }

    public int getStockDisponible() {
        return stockDisponible;
    }

    public int getCantidadSolicitada() {
        return cantidadSolicitada;
    }
}
