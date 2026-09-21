package com.sgcv.service;

import com.sgcv.dao.ArticuloDAO;
import com.sgcv.dao.ClienteDAO;
import com.sgcv.dao.EntregaDAO;
import com.sgcv.model.Articulo;
import com.sgcv.model.Cliente;
import com.sgcv.model.DetalleEntrega;
import com.sgcv.model.Entrega;
import com.sgcv.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Servicio transaccional que implementa las reglas de negocio para el registro de entregas.
 */
public class EntregaService {

    private final ClienteDAO clienteDAO;
    private final ArticuloDAO articuloDAO;
    private final EntregaDAO entregaDAO;

    public EntregaService() {
        this.clienteDAO = new ClienteDAO();
        this.articuloDAO = new ArticuloDAO();
        this.entregaDAO = new EntregaDAO();
    }

    public EntregaService(ClienteDAO clienteDAO, ArticuloDAO articuloDAO, EntregaDAO entregaDAO) {
        this.clienteDAO = clienteDAO;
        this.articuloDAO = articuloDAO;
        this.entregaDAO = entregaDAO;
    }

    public boolean validarExcesoLimiteCredito(int idCliente, BigDecimal totalEstimado) throws SQLException {
        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            throw new IllegalArgumentException("Cliente inexistente ID: " + idCliente);
        }
        return cliente.estaExcedido(totalEstimado);
    }

    public boolean registrarEntrega(Entrega entrega) throws SQLException {
        if (entrega == null || entrega.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("La entrega debe contener al menos un renglón de mercadería.");
        }

        // 1. Validar stock disponible para todos los ítems antes de abrir transacción
        for (DetalleEntrega det : entrega.getDetalles()) {
            Articulo art = articuloDAO.buscarPorId(det.getIdArticulo());
            if (art == null || !art.tieneStock(det.getCantidadBultos())) {
                int disp = art != null ? art.getStockDeposito() : 0;
                throw new IllegalStateException("Stock insuficiente para artículo [" + 
                    (art != null ? art.getCodigo() : det.getIdArticulo()) + 
                    "]. Disponible: " + disp + ", Solicitado: " + det.getCantidadBultos());
            }
            // Snapshot inmutable: congelar precios y costos vigentes
            det.setPrecioUnitarioCongelado(art.getPrecioVentaBase());
            det.setCostoUnitarioCongelado(art.getPrecioCostoReposicion());
        }

        entrega.recalcularTotal();
        if (entrega.getNumeroRemito() == null || entrega.getNumeroRemito().isBlank()) {
            entrega.setNumeroRemito(entregaDAO.obtenerSiguienteNumeroRemito());
        }

        // 2. Transacción Atómica ACID
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
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

    public EntregaDAO getEntregaDAO() { return entregaDAO; }
    public ClienteDAO getClienteDAO() { return clienteDAO; }
    public ArticuloDAO getArticuloDAO() { return articuloDAO; }
}
