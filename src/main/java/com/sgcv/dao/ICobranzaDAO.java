package com.sgcv.dao;

import com.sgcv.model.Cobranza;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface ICobranzaDAO {

    int registrarCobranza(Cobranza cobranza) throws SQLException;

    int registrarCobranza(Cobranza cobranza, Connection conn) throws SQLException;

    List<Cobranza> listarPorFecha(LocalDate fecha) throws SQLException;

    BigDecimal obtenerTotalCobradoHoyPorMedio(String medioPago, LocalDate fecha, Connection conn) throws SQLException;
}
