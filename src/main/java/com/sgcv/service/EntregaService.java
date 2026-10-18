package com.sgcv.service;

import com.sgcv.dao.ArticuloDAO;
import com.sgcv.dao.ClienteDAO;
import com.sgcv.dao.EntregaDAO;
import com.sgcv.dao.IArticuloDAO;
import com.sgcv.dao.IClienteDAO;
import com.sgcv.dao.IEntregaDAO;
import com.sgcv.exception.LimiteCreditoExcedidoException;
import com.sgcv.exception.StockInsuficienteException;
import com.sgcv.exception.ValidacionException;
import com.sgcv.model.Articulo;
import com.sgcv.model.Cliente;
import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Servicio transaccional que coordina las reglas de negocio de distribución avícola,
 * el snapshot inmutable de precios y la persistencia atómica ACID de entregas.
 */
public class EntregaService implements IEntregaService {

    @FunctionalInterface
    public interface ConnectionSupplier {
        Connection get() throws SQLException;
    }

    private final IClienteDAO clienteDAO;
    private final IArticuloDAO articuloDAO;
    private final IEntregaDAO entregaDAO;
    private final ConnectionSupplier connectionSupplier;

    public EntregaService() {
        this(new ClienteDAO(), new ArticuloDAO(), new EntregaDAO(), DatabaseConnection::getConnection);
    }

    public EntregaService(IClienteDAO clienteDAO, IArticuloDAO articuloDAO, IEntregaDAO entregaDAO) {
        this(clienteDAO, articuloDAO, entregaDAO, DatabaseConnection::getConnection);
    }

    public EntregaService(IClienteDAO clienteDAO, IArticuloDAO articuloDAO, IEntregaDAO entregaDAO,
                          ConnectionSupplier connectionSupplier) {
        this.clienteDAO = clienteDAO;
        this.articuloDAO = articuloDAO;
        this.entregaDAO = entregaDAO;
        this.connectionSupplier = connectionSupplier != null ? connectionSupplier : DatabaseConnection::getConnection;
    }

    @Override
    public boolean validarExcesoLimiteCredito(int idCliente, BigDecimal totalEstimado) throws SQLException {
        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            throw new ValidacionException("Cliente inexistente ID: " + idCliente);
        }
        return cliente.estaExcedido(totalEstimado);
    }

    @Override
    public boolean registrarEntrega(Entrega entrega)
            throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
        return registrarEntrega(entrega, false);
    }

    @Override
    public boolean registrarEntrega(Entrega entrega, boolean autorizarExcesoCredito)
            throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
        if (entrega == null || entrega.getDetalles().isEmpty()) {
            throw new ValidacionException("La entrega debe contener al menos un renglón de mercadería.");
        }

        // 1. Validar existencia del cliente
        Cliente cliente = clienteDAO.buscarPorId(entrega.getIdCliente());
        if (cliente == null) {
            throw new ValidacionException("Cliente no registrado con ID: " + entrega.getIdCliente());
        }

        // 2. Validar stock físico disponible y congelar precios con cálculo polimórfico
        for (DetalleEntrega det : entrega.getDetalles()) {
            Articulo art = articuloDAO.buscarPorId(det.getIdArticulo());
            if (art == null) {
                throw new ValidacionException("Artículo no encontrado con ID: " + det.getIdArticulo());
            }
            if (!art.tieneStock(det.getCantidadBultos())) {
                throw new StockInsuficienteException(art.getCodigo(), art.getStockDeposito(), det.getCantidadBultos());
            }

            // Snapshot inmutable: cálculo polimórfico de precio final según categoría (fresco vs congelado)
            BigDecimal precioFinal = art.calcularPrecioVentaFinal(det.getCantidadBultos());
            det.setPrecioUnitarioCongelado(precioFinal);
            det.setCostoUnitarioCongelado(art.getPrecioCostoReposicion());
        }

        entrega.recalcularTotal();

        // 3. Control de riesgo crediticio (CU-01 S10)
        if (cliente.estaExcedido(entrega.getTotalEntrega())) {
            boolean autorizadoPorObservaciones = entrega.getObservaciones() != null &&
                    entrega.getObservaciones().toLowerCase().contains("autorizad");
            if (!autorizarExcesoCredito && !autorizadoPorObservaciones) {
                BigDecimal saldoProyectado = cliente.getSaldoCuentaCorriente().add(entrega.getTotalEntrega());
                BigDecimal exceso = saldoProyectado.subtract(cliente.getLimiteCredito());
                throw new LimiteCreditoExcedidoException(
                        String.format("Límite de crédito excedido para cliente '%s' (Saldo actual: $%s, Límite: $%s, Total entrega: $%s, Exceso: $%s).",
                                cliente.getRazonSocial(), cliente.getSaldoCuentaCorriente(), cliente.getLimiteCredito(),
                                entrega.getTotalEntrega(), exceso),
                        cliente.getIdCliente(), cliente.getSaldoCuentaCorriente(), cliente.getLimiteCredito(),
                        entrega.getTotalEntrega()
                );
            }
        }

        if (entrega.getNumeroRemito() == null || entrega.getNumeroRemito().isBlank()) {
            entrega.setNumeroRemito(entregaDAO.obtenerSiguienteNumeroRemito());
        }

        // 4. Transacción Atómica ACID
        Connection conn = null;
        try {
            conn = connectionSupplier.get();
            conn.setAutoCommit(false);

            // A. Insertar cabecera de entrega
            int idEntrega = entregaDAO.insertar(entrega, conn);
            if (idEntrega <= 0) {
                throw new SQLException("No se pudo generar la cabecera de entrega.");
            }
            entrega.setIdEntrega(idEntrega);

            // B. Insertar renglones y decrementar stock físico
            for (DetalleEntrega det : entrega.getDetalles()) {
                det.setIdEntrega(idEntrega);
                boolean detOk = entregaDAO.insertarDetalle(idEntrega, det, conn);
                if (!detOk) {
                    throw new SQLException("Fallo al persistir detalle para artículo ID: " + det.getIdArticulo());
                }

                boolean stockOk = articuloDAO.decrementarStock(det.getIdArticulo(), det.getCantidadBultos(), conn);
                if (!stockOk) {
                    throw new SQLException("Fallo concurrente al descontar stock para artículo ID: " + det.getIdArticulo());
                }
            }

            // C. Incrementar saldo deudor en cuenta corriente del cliente
            boolean saldoOk = clienteDAO.actualizarSaldo(entrega.getIdCliente(), entrega.getTotalEntrega(), conn);
            if (!saldoOk) {
                throw new SQLException("Fallo al actualizar saldo deudor del cliente ID: " + entrega.getIdCliente());
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); } catch (SQLException ignored) {}
                DatabaseConnection.closeQuietly(conn);
            }
        }
    }

    @Override
    public IEntregaDAO getEntregaDAO() { return entregaDAO; }

    @Override
    public IClienteDAO getClienteDAO() { return clienteDAO; }

    @Override
    public IArticuloDAO getArticuloDAO() { return articuloDAO; }
}
