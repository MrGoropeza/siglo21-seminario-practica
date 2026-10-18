package com.sgcv.service;

import com.sgcv.model.ArqueoCaja;
import com.sgcv.model.ResumenDiarioDTO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Contrato de servicio para el Arqueo, Cierre y Segregación de Fondos Diaria (CU-04).
 */
public interface IArqueoService {

    /**
     * Calcula en tiempo real las métricas operativas del día:
     * bultos entregados, facturación, cobranzas en efectivo/transferencia,
     * fondo de reposición segregado para frigoríficos y margen neto diario.
     */
    ResumenDiarioDTO calcularResumenDiario(LocalDate fecha) throws SQLException;

    /**
     * Registra el cierre formal de caja persistiendo el desglose por medios de pago.
     */
    int registrarArqueoCaja(ArqueoCaja arqueo) throws SQLException;

    /**
     * Obtiene el último arqueo registrado en la base de datos.
     */
    ArqueoCaja obtenerUltimoArqueo() throws SQLException;

    /**
     * Lista el historial cronológico de cierres de caja.
     */
    List<ArqueoCaja> listarHistorico() throws SQLException;
}
