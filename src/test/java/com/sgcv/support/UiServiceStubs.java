package com.sgcv.support;

import com.sgcv.dao.IArticuloDAO;
import com.sgcv.dao.IClienteDAO;
import com.sgcv.dao.IEntregaDAO;
import com.sgcv.model.ArqueoCaja;
import com.sgcv.model.Articulo;
import com.sgcv.model.Cobranza;
import com.sgcv.model.Entrega;
import com.sgcv.model.ParadaResult;
import com.sgcv.model.ResumenDiarioDTO;
import com.sgcv.service.IArticuloService;
import com.sgcv.service.IClienteService;
import com.sgcv.service.ICobranzaService;
import com.sgcv.service.IEntregaService;
import com.sgcv.service.IArqueoService;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Dobles de servicio en memoria para los tests de UI.
 *
 * <p>Construir {@code MainFrame} o {@code ArqueoPanel} con los servicios reales abre
 * conexiones JDBC y, si la base no responde, muestra un {@code JOptionPane} modal que
 * colgaría la suite. Estos dobles devuelven datos vacíos o fijos para que el test de UI
 * sea determinista y no dependa de MySQL.</p>
 */
public final class UiServiceStubs {

    private UiServiceStubs() {
        // Clase utilitaria
    }

    /**
     * Servicio de clientes sin datos: ninguna pestaña recibe clientes para listar.
     */
    public static IClienteService clienteServiceVacio() {
        return new IClienteService() {
            @Override public List<com.sgcv.model.Cliente> listarActivos() { return List.of(); }
            @Override public com.sgcv.model.Cliente buscarPorId(int id) { return null; }
            @Override public boolean verificarRiesgoCrediticio(int idCliente, BigDecimal montoNuevo) { return false; }
            @Override public int crearCliente(com.sgcv.model.Cliente cliente) { return 0; }
            @Override public boolean actualizarCliente(com.sgcv.model.Cliente cliente) { return false; }
            @Override public List<com.sgcv.model.MovimientoCuentaCorriente> obtenerExtractoCuentaCorriente(int idCliente) {
                return List.of();
            }
        };
    }

    /**
     * Servicio de artículos sin datos, con los dos algoritmos pedidos por la UI
     * (ordenamiento por stock y búsqueda binaria por código) resueltos linealmente.
     */
    public static IArticuloService articuloServiceVacio() {
        return new IArticuloService() {
            @Override public List<Articulo> listarDisponibles() { return List.of(); }
            @Override public List<Articulo> listarTodos() { return List.of(); }
            @Override public boolean reactivarArticulo(int idArticulo) { return false; }
            @Override public Articulo buscarPorId(int id) { return null; }
            @Override public List<Articulo> ordenarPorStockAscendente(List<Articulo> lista) {
                return lista == null ? List.of() : List.copyOf(lista);
            }
            @Override public int busquedaBinariaPorCodigo(List<Articulo> listaOrdenadaPorCodigo, String codigoBuscado) {
                return -1;
            }
            @Override public int crearArticulo(Articulo articulo) { return 0; }
            @Override public boolean actualizarArticulo(Articulo articulo) { return false; }
            @Override public boolean eliminarArticulo(int idArticulo) { return false; }
            @Override public boolean existeCodigo(String codigo, int idArticuloExcluir) { return false; }
        };
    }

    /**
     * Servicio de entregas sin persistencia: la UI sólo lo usa al confirmar una parada.
     */
    public static IEntregaService entregaServiceVacio() {
        return new IEntregaService() {
            @Override public boolean validarExcesoLimiteCredito(int idCliente, BigDecimal totalEstimado) { return false; }
            @Override public boolean registrarEntrega(Entrega entrega) { return false; }
            @Override public boolean registrarEntrega(Entrega entrega, boolean autorizarExcesoCredito) { return false; }
            @Override public ParadaResult registrarParadaCompleta(Entrega entrega, Cobranza cobranza, boolean autorizarExcesoCredito) {
                return null;
            }
            @Override public IEntregaDAO getEntregaDAO() { return null; }
            @Override public IClienteDAO getClienteDAO() { return null; }
            @Override public IArticuloDAO getArticuloDAO() { return null; }
        };
    }

    /**
     * Servicio de cobranzas sin persistencia.
     */
    public static ICobranzaService cobranzaServiceVacio() {
        return new ICobranzaService() {
            @Override public int registrarCobranza(Cobranza cobranza) { return 0; }
            @Override public List<Cobranza> listarCobranzasHoy() { return List.of(); }
            @Override public BigDecimal obtenerTotalCobradoHoyPorMedio(String medioPago, LocalDate fecha) {
                return BigDecimal.ZERO;
            }
        };
    }

    /**
     * Servicio de arqueo que devuelve siempre el resumen indicado y un historial vacío,
     * de modo que {@code ArqueoPanel} calcule sus totales sin tocar la base.
     *
     * @param resumen resumen diario fijo con el que se alimenta el panel
     */
    public static IArqueoService arqueoServiceConResumen(ResumenDiarioDTO resumen) {
        return new IArqueoService() {
            @Override public ResumenDiarioDTO calcularResumenDiario(LocalDate fecha) { return resumen; }
            @Override public int registrarArqueoCaja(ArqueoCaja arqueo) throws SQLException {
                throw new SQLException("El test de UI no persiste arqueos");
            }
            @Override public ArqueoCaja obtenerUltimoArqueo() { return null; }
            @Override public List<ArqueoCaja> listarHistorico() { return List.of(); }
        };
    }
}
