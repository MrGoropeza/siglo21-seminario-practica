package com.sgcv.dao;

import com.sgcv.model.ArqueoCaja;
import com.sgcv.model.ResumenDiarioDTO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public interface IArqueoDAO {

    int registrarArqueo(ArqueoCaja arqueo) throws SQLException;

    ArqueoCaja obtenerUltimoArqueo() throws SQLException;

    List<ArqueoCaja> listarHistorico() throws SQLException;

    ResumenDiarioDTO calcularResumenDiario(LocalDate fecha) throws SQLException;
}
