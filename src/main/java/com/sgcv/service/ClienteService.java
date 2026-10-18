package com.sgcv.service;

import com.sgcv.dao.ClienteDAO;
import com.sgcv.dao.IClienteDAO;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Cliente;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

/**
 * Servicio de negocio para la administración de clientes y evaluación de riesgo crediticio.
 */
public class ClienteService implements IClienteService {

    private final IClienteDAO clienteDAO;

    public ClienteService() {
        this(new ClienteDAO());
    }

    public ClienteService(IClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    @Override
    public List<Cliente> listarActivos() throws SQLException {
        return clienteDAO.listarActivos();
    }

    @Override
    public Cliente buscarPorId(int id) throws SQLException {
        return clienteDAO.buscarPorId(id);
    }

    @Override
    public boolean verificarRiesgoCrediticio(int idCliente, BigDecimal montoNuevo) throws SQLException {
        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            throw new ValidacionException("Cliente inexistente con ID: " + idCliente);
        }
        return cliente.estaExcedido(montoNuevo);
    }

    @Override
    public int crearCliente(Cliente cliente) throws SQLException {
        validarCliente(cliente);
        return clienteDAO.crearCliente(cliente);
    }

    @Override
    public boolean actualizarCliente(Cliente cliente) throws SQLException {
        if (cliente == null || cliente.getIdCliente() <= 0) {
            throw new ValidacionException("Debe especificar un cliente válido para actualizar.");
        }
        validarCliente(cliente);
        return clienteDAO.actualizarCliente(cliente);
    }

    private void validarCliente(Cliente cliente) {
        if (cliente == null) {
            throw new ValidacionException("Los datos del cliente no pueden ser nulos.");
        }
        if (cliente.getRazonSocial() == null || cliente.getRazonSocial().trim().isEmpty()) {
            throw new ValidacionException("La razón social del cliente es obligatoria.");
        }
        if (cliente.getLimiteCredito() != null && cliente.getLimiteCredito().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El límite de crédito no puede ser negativo.");
        }
    }

    @Override
    public List<com.sgcv.model.MovimientoCuentaCorriente> obtenerExtractoCuentaCorriente(int idCliente) throws SQLException {
        return clienteDAO.obtenerExtractoCuentaCorriente(idCliente);
    }

    public IClienteDAO getClienteDAO() {
        return clienteDAO;
    }
}

