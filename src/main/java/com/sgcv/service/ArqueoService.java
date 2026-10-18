package com.sgcv.service;

import com.sgcv.dao.ArqueoDAO;
import com.sgcv.dao.IArqueoDAO;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.ArqueoCaja;
import com.sgcv.model.ResumenDiarioDTO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ArqueoService implements IArqueoService {

    private final IArqueoDAO arqueoDAO;

    public ArqueoService() {
        this(new ArqueoDAO());
    }

    public ArqueoService(IArqueoDAO arqueoDAO) {
        this.arqueoDAO = arqueoDAO;
    }

    @Override
    public ResumenDiarioDTO calcularResumenDiario(LocalDate fecha) throws SQLException {
        return arqueoDAO.calcularResumenDiario(fecha != null ? fecha : LocalDate.now());
    }

    @Override
    public int registrarArqueoCaja(ArqueoCaja arqueo) throws SQLException {
        if (arqueo == null) {
            throw new ValidacionException("El arqueo no puede ser nulo.");
        }
        if (arqueo.getEfectivoRealContado() == null) {
            throw new ValidacionException("Debe ingresar el monto de efectivo real contado.");
        }
        return arqueoDAO.registrarArqueo(arqueo);
    }

    @Override
    public ArqueoCaja obtenerUltimoArqueo() throws SQLException {
        return arqueoDAO.obtenerUltimoArqueo();
    }

    @Override
    public List<ArqueoCaja> listarHistorico() throws SQLException {
        return arqueoDAO.listarHistorico();
    }

    public IArqueoDAO getArqueoDAO() {
        return arqueoDAO;
    }
}
