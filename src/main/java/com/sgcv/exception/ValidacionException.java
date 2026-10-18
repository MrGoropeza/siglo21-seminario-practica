package com.sgcv.exception;

/**
 * Excepción de dominio lanzada ante la violación de invariantes de negocio o
 * parámetros de entrada inválidos en el modelo u operaciones.
 */
public class ValidacionException extends RuntimeException {

    public ValidacionException(String mensaje) {
        super(mensaje);
    }

    public ValidacionException(String mensaje, Throwable cause) {
        super(mensaje, cause);
    }
}
