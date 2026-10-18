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
    private final com.sgcv.dao.ICobranzaDAO cobranzaDAO;
    private final ConnectionSupplier connectionSupplier;

    public EntregaService() {
        this(new ClienteDAO(), new ArticuloDAO(), new EntregaDAO(), new com.sgcv.dao.CobranzaDAO(), DatabaseConnection::getConnection);
    }

    public EntregaService(IClienteDAO clienteDAO, IArticuloDAO articuloDAO, IEntregaDAO entregaDAO) {
        this(clienteDAO, articuloDAO, entregaDAO, new com.sgcv.dao.CobranzaDAO(), DatabaseConnection::getConnection);
    }

    public EntregaService(IClienteDAO clienteDAO, IArticuloDAO articuloDAO, IEntregaDAO entregaDAO,
                          ConnectionSupplier connectionSupplier) {
        this(clienteDAO, articuloDAO, entregaDAO, new com.sgcv.dao.CobranzaDAO(), connectionSupplier);
    }

    public EntregaService(IClienteDAO clienteDAO, IArticuloDAO articuloDAO, IEntregaDAO entregaDAO,
                          com.sgcv.dao.ICobranzaDAO cobranzaDAO, ConnectionSupplier connectionSupplier) {
        this.clienteDAO = clienteDAO;
        this.articuloDAO = articuloDAO;
        this.entregaDAO = entregaDAO;
        this.cobranzaDAO = cobranzaDAO != null ? cobranzaDAO : new com.sgcv.dao.CobranzaDAO();
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
    public com.sgcv.model.ParadaResult registrarParadaCompleta(Entrega entrega, com.sgcv.model.Cobranza cobranza, boolean autorizarExcesoCredito)
            throws SQLException, StockInsuficienteException, LimiteCreditoExcedidoException {
        boolean tieneEntrega = (entrega != null && entrega.getDetalles() != null && !entrega.getDetalles().isEmpty());
        boolean tieneCobranza = (cobranza != null && cobranza.getMontoTotal() != null && cobranza.getMontoTotal().compareTo(BigDecimal.ZERO) > 0);

        if (!tieneEntrega && !tieneCobranza) {
            throw new ValidacionException("Debe seleccionar al menos un bulto para entregar o ingresar un monto de cobranza.");
        }

        int idCliente = tieneEntrega ? entrega.getIdCliente() : (cobranza != null ? cobranza.getIdCliente() : 0);
        if (idCliente <= 0) {
            throw new ValidacionException("Debe seleccionar un cliente válido.");
        }

        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            throw new ValidacionException("Cliente inexistente con ID: " + idCliente);
        }

        BigDecimal totalEntrega = BigDecimal.ZERO;
        if (tieneEntrega) {
            for (DetalleEntrega det : entrega.getDetalles()) {
                Articulo art = articuloDAO.buscarPorId(det.getIdArticulo());
                if (art == null) {
                    throw new ValidacionException("Artículo no encontrado ID: " + det.getIdArticulo());
                }
                if (!art.tieneStock(det.getCantidadBultos())) {
                    throw new StockInsuficienteException(art.getCodigo(), art.getStockDeposito(), det.getCantidadBultos());
                }
                BigDecimal precioFinal = art.calcularPrecioVentaFinal(det.getCantidadBultos());
                det.setPrecioUnitarioCongelado(precioFinal);
                det.setCostoUnitarioCongelado(art.getPrecioCostoReposicion());
            }
            entrega.recalcularTotal();
            totalEntrega = entrega.getTotalEntrega();
        }

        BigDecimal totalCobrado = tieneCobranza ? cobranza.getMontoTotal() : BigDecimal.ZERO;
        BigDecimal saldoProyectado = cliente.getSaldoCuentaCorriente().add(totalEntrega).subtract(totalCobrado);

        if (saldoProyectado.compareTo(cliente.getLimiteCredito()) > 0) {
            boolean autorizadoPorObservaciones = entrega != null && entrega.getObservaciones() != null &&
                    entrega.getObservaciones().toLowerCase().contains("autorizad");
            if (!autorizarExcesoCredito && !autorizadoPorObservaciones) {
                BigDecimal exceso = saldoProyectado.subtract(cliente.getLimiteCredito());
                throw new LimiteCreditoExcedidoException(
                        String.format("Límite de crédito excedido para cliente '%s' (Saldo actual: $%s, Límite: $%s, Operación neta: $%s, Exceso: $%s).",
                                cliente.getRazonSocial(), cliente.getSaldoCuentaCorriente(), cliente.getLimiteCredito(),
                                totalEntrega.subtract(totalCobrado), exceso),
                        cliente.getIdCliente(), cliente.getSaldoCuentaCorriente(), cliente.getLimiteCredito(),
                        totalEntrega
                );
            }
        }

        com.sgcv.model.ParadaResult result = new com.sgcv.model.ParadaResult();
        result.setTotalEntrega(totalEntrega);
        result.setTotalCobrado(totalCobrado);
        result.setNuevoSaldoCliente(saldoProyectado);

        Connection conn = null;
        try {
            conn = connectionSupplier.get();
            conn.setAutoCommit(false);

            if (tieneEntrega) {
                if (entrega.getNumeroRemito() == null || entrega.getNumeroRemito().isBlank()) {
                    entrega.setNumeroRemito(entregaDAO.obtenerSiguienteNumeroRemito());
                }
                int idEntrega = entregaDAO.insertar(entrega, conn);
                if (idEntrega <= 0) {
                    throw new SQLException("No se pudo generar la cabecera del remito de entrega.");
                }
                entrega.setIdEntrega(idEntrega);
                result.setIdEntrega(idEntrega);
                result.setNumeroRemito(entrega.getNumeroRemito());

                for (DetalleEntrega det : entrega.getDetalles()) {
                    det.setIdEntrega(idEntrega);
                    boolean detOk = entregaDAO.insertarDetalle(idEntrega, det, conn);
                    if (!detOk) {
                        throw new SQLException("Error al persistir renglón para artículo ID: " + det.getIdArticulo());
                    }
                    boolean stockOk = articuloDAO.decrementarStock(det.getIdArticulo(), det.getCantidadBultos(), conn);
                    if (!stockOk) {
                        throw new SQLException("Error concurrente al descontar stock físico del artículo ID: " + det.getIdArticulo());
                    }
                }

                clienteDAO.actualizarSaldo(idCliente, totalEntrega, conn);
            }

            if (tieneCobranza) {
                cobranza.setIdCliente(idCliente);
                int idCobranza = cobranzaDAO.registrarCobranza(cobranza, conn);
                result.setIdCobranza(idCobranza);
                result.setNumeroRecibo(cobranza.getNumeroRecibo());
            }

            conn.commit();

            if (tieneEntrega && tieneCobranza) {
                result.setMensaje(String.format("Operación completada: Remito %s emitido ($%s) y Recibo %s cobrado ($%s).",
                        result.getNumeroRemito(), totalEntrega, result.getNumeroRecibo(), totalCobrado));
            } else if (tieneEntrega) {
                result.setMensaje(String.format("Remito %s emitido exitosamente a cuenta corriente ($%s).",
                        result.getNumeroRemito(), totalEntrega));
            } else {
                result.setMensaje(String.format("Cobranza de deuda registrada exitosamente. Recibo %s ($%s).",
                        result.getNumeroRecibo(), totalCobrado));
            }

            return result;
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

