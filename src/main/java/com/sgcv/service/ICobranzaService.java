package com.sgcv.service;

import com.sgcv.model.Cobranza;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface ICobranzaService {

    int registrarCobranza(Cobranza cobranza) throws SQLException;

    List<Cobranza> listarCobranzasHoy() throws SQLException;

    BigDecimal obtenerTotalCobradoHoyPorMedio(String medioPago, LocalDate fecha) throws SQLException;
}
