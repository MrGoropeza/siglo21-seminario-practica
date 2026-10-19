package com.sgcv.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.sgcv.model.ResumenDiarioDTO;
import com.sgcv.support.SwingTestSupport;
import com.sgcv.support.UiServiceStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import java.awt.Component;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de UI de {@link ArqueoPanel} con componentes Swing reales.
 *
 * <p>El panel se alimenta con un {@link ResumenDiarioDTO} fijo mediante un doble de
 * {@code IArqueoService}: así los cálculos son deterministas y no dependen de MySQL.
 * No se hace click en el botón de cierre porque abre un {@code JOptionPane} modal que
 * bloquearía el test; sólo se verifica que el botón existe, está habilitado y tiene nombre.</p>
 */
class ArqueoPanelUiTest {

    private ArqueoPanel panel;

    @BeforeEach
    void instalarLookAndFeel() throws Exception {
        SwingTestSupport.enEdt(() -> {
            if (!(UIManager.getLookAndFeel() instanceof FlatLightLaf)) {
                FlatLightLaf.setup();
            }
        });
    }

    @AfterEach
    void disposePanel() throws Exception {
        ArqueoPanel aCerrar = panel;
        panel = null;
        if (aCerrar != null) {
            SwingTestSupport.enEdt(() -> {
                aCerrar.removeAll();
                aCerrar.revalidate();
            });
        }
    }

    /**
     * Resumen diario fijo: $20.000 cobrados en efectivo y $5.000 de costo de reposición,
     * por lo que el panel precarga $5.000 para el sobre del frigorífico y $15.000 de margen.
     */
    private static ResumenDiarioDTO resumenDePrueba() {
        ResumenDiarioDTO resumen = new ResumenDiarioDTO();
        resumen.setBultosEntregadosTotal(12);
        resumen.setFacturacionEntregasTotal(new BigDecimal("30000.00"));
        resumen.setCostoReposicionTotal(new BigDecimal("5000.00"));
        resumen.setCobranzasEfectivoTotal(new BigDecimal("20000.00"));
        resumen.setCobranzasTransferenciaTotal(new BigDecimal("5000.00"));
        resumen.setTotalCobrado(new BigDecimal("25000.00"));
        return resumen;
    }

    private void construirPanel() throws Exception {
        SwingTestSupport.enEdt(() -> panel = new ArqueoPanel(
                UiServiceStubs.arqueoServiceConResumen(resumenDePrueba()), null));
    }

    private <T extends Component> T componente(String nombre, Class<T> tipo) throws Exception {
        Component[] encontrado = new Component[1];
        SwingTestSupport.enEdt(() -> encontrado[0] = SwingTestSupport.buscarPorNombre(panel, nombre));
        assertNotNull(encontrado[0], "Debe existir un componente con el nombre '" + nombre + "'");
        assertTrue(tipo.isInstance(encontrado[0]),
                "El componente '" + nombre + "' debe ser " + tipo.getSimpleName()
                        + " pero fue " + encontrado[0].getClass().getName());
        return tipo.cast(encontrado[0]);
    }

    @Test
    @DisplayName("El panel expone los campos y labels del arqueo con nombres estables")
    void exponeComponentesConNombresEstables() throws Exception {
        construirPanel();

        assertNotNull(componente("arqueo.saldoInicial", JTextField.class));
        assertNotNull(componente("arqueo.efectivoReal", JTextField.class));
        assertNotNull(componente("arqueo.gasto", JTextField.class));
        assertNotNull(componente("arqueo.fondo", JTextField.class));
        assertNotNull(componente("arqueo.esperado", JLabel.class));
        assertNotNull(componente("arqueo.diferencia", JLabel.class));
        assertNotNull(componente("arqueo.margen", JLabel.class));
        assertNotNull(componente("arqueo.cerrar", JButton.class));
    }

    @Test
    @DisplayName("Al cargar métricas el panel precarga el sobre del frigorífico y el margen neto")
    void precargaFondoYMargenDesdeElResumen() throws Exception {
        construirPanel();

        JTextField fondo = componente("arqueo.fondo", JTextField.class);
        JLabel margen = componente("arqueo.margen", JLabel.class);
        JLabel esperado = componente("arqueo.esperado", JLabel.class);

        String[] textos = new String[3];
        SwingTestSupport.enEdt(() -> {
            textos[0] = fondo.getText();
            textos[1] = margen.getText();
            textos[2] = esperado.getText();
        });

        assertEquals("5000.00", textos[0], "El sobre del frigorífico se precarga con el costo de reposición del día");
        assertEquals(String.format("$%,.2f", new BigDecimal("15000.00")), textos[1],
                "El margen neto debe ser lo cobrado en efectivo menos el fondo segregado");
        assertEquals(String.format("$%,.2f", new BigDecimal("20000.00")), textos[2],
                "El efectivo esperado arranca con las cobranzas en efectivo del día");
    }

    @Test
    @DisplayName("Ingresar el efectivo real contado actualiza el label de diferencia de caja")
    void ingresarEfectivoRealActualizaLaDiferencia() throws Exception {
        construirPanel();

        JTextField efectivoReal = componente("arqueo.efectivoReal", JTextField.class);
        JLabel diferencia = componente("arqueo.diferencia", JLabel.class);

        String[] textos = new String[2];
        SwingTestSupport.enEdt(() -> textos[0] = diferencia.getText());
        String diferenciaInicial = textos[0];
        assertFalse(diferenciaInicial.isBlank(), "El label de diferencia debe mostrar la indicación inicial de carga");

        SwingTestSupport.enEdt(() -> {
            efectivoReal.setText("48500.00");
            textos[1] = diferencia.getText();
        });

        assertFalse(textos[1].isBlank(), "Con efectivo contado el label de diferencia no puede quedar vacío");
        assertNotEquals(diferenciaInicial, textos[1],
                "El label de diferencia debe cambiar al ingresar el efectivo real contado");
        assertTrue(textos[1].contains("SOBRANTE EN CAJA"),
                "Con $48.500 contados sobre $20.000 esperados debe informarse un sobrante, pero fue: " + textos[1]);
    }

    @Test
    @DisplayName("Un gasto de ruta reduce el efectivo esperado y el label de diferencia se recalcula")
    void ingresarGastoRecalculaElEfectivoEsperado() throws Exception {
        construirPanel();

        JTextField gasto = componente("arqueo.gasto", JTextField.class);
        JTextField efectivoReal = componente("arqueo.efectivoReal", JTextField.class);
        JLabel esperado = componente("arqueo.esperado", JLabel.class);
        JLabel diferencia = componente("arqueo.diferencia", JLabel.class);

        SwingTestSupport.enEdt(() -> gasto.setText("1500"));
        String[] textos = new String[2];
        SwingTestSupport.enEdt(() -> textos[0] = esperado.getText());
        assertEquals(String.format("$%,.2f", new BigDecimal("18500.00")), textos[0],
                "El efectivo esperado debe descontar los gastos de ruta");

        SwingTestSupport.enEdt(() -> {
            efectivoReal.setText("18500.00");
            textos[1] = diferencia.getText();
        });
        assertTrue(textos[1].contains("CAJA CUADRADA"),
                "Contar exactamente el efectivo esperado debe informar la caja cuadrada, pero fue: " + textos[1]);
    }

    @Test
    @DisplayName("El botón de cierre queda habilitado con un fondo dentro del tope cobrado")
    void botonDeCierreHabilitadoSinExcederElTope() throws Exception {
        construirPanel();

        JButton cerrar = componente("arqueo.cerrar", JButton.class);
        boolean[] estado = new boolean[1];
        SwingTestSupport.enEdt(() -> estado[0] = cerrar.isEnabled());

        assertTrue(estado[0], "El botón de cierre debe estar habilitado cuando el fondo no supera lo cobrado en efectivo");
    }
}
