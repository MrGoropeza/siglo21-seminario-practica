package com.sgcv.service;

import com.sgcv.dao.CobranzaDAO;
import com.sgcv.dao.ICobranzaDAO;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Cliente;
import com.sgcv.model.Cobranza;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class CobranzaService implements ICobranzaService {

    private final ICobranzaDAO cobranzaDAO;

    public CobranzaService() {
        this(new CobranzaDAO());
    }

    public CobranzaService(ICobranzaDAO cobranzaDAO) {
        this.cobranzaDAO = cobranzaDAO;
    }

    @Override
    public int registrarCobranza(Cobranza cobranza) throws SQLException {
        if (cobranza == null) {
            throw new ValidacionException("La cobranza no puede ser nula.");
        }
        if (cobranza.getIdCliente() <= 0) {
            throw new ValidacionException("Debe asociar un cliente válido a la cobranza.");
        }
        if (cobranza.getMontoTotal() == null || cobranza.getMontoTotal().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidacionException("El monto a cobrar debe ser mayor a cero.");
        }
        if (cobranza.getDetalles() == null || cobranza.getDetalles().isEmpty()) {
            throw new ValidacionException("Debe ingresar al menos un medio de pago.");
        }
        Cliente cliente = new ClienteService().buscarPorId(cobranza.getIdCliente());
        if (cliente == null) {
            throw new ValidacionException("Cliente inexistente con ID: " + cobranza.getIdCliente());
        }
        if (cobranza.getMontoTotal().compareTo(cliente.getSaldoCuentaCorriente()) > 0) {
            throw new ValidacionException("El monto cobrado no puede superar el saldo deudor del cliente.");
        }
        return cobranzaDAO.registrarCobranza(cobranza);
    }

    @Override
    public List<Cobranza> listarCobranzasHoy() throws SQLException {
        return cobranzaDAO.listarPorFecha(LocalDate.now());
    }

    @Override
    public BigDecimal obtenerTotalCobradoHoyPorMedio(String medioPago, LocalDate fecha) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            return cobranzaDAO.obtenerTotalCobradoHoyPorMedio(medioPago, fecha != null ? fecha : LocalDate.now(), conn);
        }
    }

    public ICobranzaDAO getCobranzaDAO() {
        return cobranzaDAO;
    }
}
