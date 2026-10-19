package com.sgcv.ui;

import com.formdev.flatlaf.FlatLightLaf;
import com.sgcv.support.SwingTestSupport;
import com.sgcv.support.UiServiceStubs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import java.awt.Component;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test de UI de {@link MainFrame} usando componentes Swing reales.
 *
 * <p>No se usa AssertJ Swing (incompatible con el JDK de este entorno), {@code Robot}
 * ni interacción física: el frame se construye y se inspecciona dentro del EDT y se
 * cierra con {@code dispose()} en {@link #disposeFrame()}.</p>
 */
class MainFrameUiTest {

    private static final String[] TITULOS_ESPERADOS = {
            "🚚 Parada en Ruta",
            "📊 Cierre y Arqueo Diario",
            "💳 Cuentas Corrientes y Extracto",
            "📦 Catálogo de Bultos"
    };

    private MainFrame frame;

    @BeforeEach
    void instalarLookAndFeel() throws Exception {
        SwingTestSupport.enEdt(() -> {
            if (!(UIManager.getLookAndFeel() instanceof FlatLightLaf)) {
                FlatLightLaf.setup();
            }
        });
    }

    @AfterEach
    void disposeFrame() throws Exception {
        MainFrame aCerrar = frame;
        frame = null;
        if (aCerrar != null) {
            SwingTestSupport.enEdt(aCerrar::dispose);
        }
    }

    private JTabbedPane construirFrameYObtenerTabs() throws Exception {
        SwingTestSupport.enEdt(() -> frame = new MainFrame(
                UiServiceStubs.clienteServiceVacio(),
                UiServiceStubs.articuloServiceVacio(),
                UiServiceStubs.entregaServiceVacio(),
                UiServiceStubs.cobranzaServiceVacio(),
                UiServiceStubs.arqueoServiceConResumen(new com.sgcv.model.ResumenDiarioDTO())));

        JTabbedPane[] tabs = new JTabbedPane[1];
        SwingTestSupport.enEdt(() -> {
            Component encontrado = SwingTestSupport.buscarPorNombre(frame, "main.tabs");
            if (encontrado instanceof JTabbedPane tabbedPane) {
                tabs[0] = tabbedPane;
            }
        });
        assertNotNull(tabs[0], "El JTabbedPane debe poder localizarse por el nombre 'main.tabs'");
        return tabs[0];
    }

    @Test
    @DisplayName("MainFrame se construye en el EDT sin lanzar excepciones")
    void seConstruyeSinErroresEnElEdt() {
        assertDoesNotThrow(() -> SwingTestSupport.enEdt(() -> frame = new MainFrame(
                        UiServiceStubs.clienteServiceVacio(),
                        UiServiceStubs.articuloServiceVacio(),
                        UiServiceStubs.entregaServiceVacio(),
                        UiServiceStubs.cobranzaServiceVacio(),
                        UiServiceStubs.arqueoServiceConResumen(new com.sgcv.model.ResumenDiarioDTO()))),
                "La construcción del frame principal no debe fallar en el EDT");
        assertNotNull(frame, "El frame debe quedar creado");
    }

    @Test
    @DisplayName("El panel de pestañas principal tiene exactamente 4 pestañas")
    void tieneCuatroPestanas() throws Exception {
        JTabbedPane tabs = construirFrameYObtenerTabs();
        assertEquals(4, tabs.getTabCount(), "La ventana principal debe ofrecer exactamente las 4 pantallas del SGCV");
    }

    @Test
    @DisplayName("Los títulos de las 4 pestañas son los definidos en la UI")
    void losTitulosDeLasPestanasSonLosEsperados() throws Exception {
        JTabbedPane tabs = construirFrameYObtenerTabs();
        for (int i = 0; i < TITULOS_ESPERADOS.length; i++) {
            assertEquals(TITULOS_ESPERADOS[i], tabs.getTitleAt(i),
                    "El título de la pestaña " + (i + 1) + " no coincide con el definido en MainFrame");
        }
    }

    @Test
    @DisplayName("Cada pestaña tiene su panel asociado y la primera queda seleccionada")
    void cadaPestanaTieneSuPanelYArrancaEnLaPrimera() throws Exception {
        JTabbedPane tabs = construirFrameYObtenerTabs();
        for (int i = 0; i < tabs.getTabCount(); i++) {
            assertNotNull(tabs.getComponentAt(i), "La pestaña " + (i + 1) + " debe tener su panel asociado");
        }
        assertEquals(0, tabs.getSelectedIndex(), "La aplicación debe abrirse en la parada en ruta");
    }

    @Test
    @DisplayName("La navegación programática entre pestañas mantiene la selección pedida")
    void seleccionProgramaticaDePestanas() throws Exception {
        JTabbedPane tabs = construirFrameYObtenerTabs();
        SwingTestSupport.enEdt(() -> tabs.setSelectedIndex(1));
        assertEquals(1, tabs.getSelectedIndex(), "Seleccionar la pestaña de arqueo debe reflejarse en el índice seleccionado");
        assertTrue(tabs.getSelectedComponent() != null, "La pestaña seleccionada debe tener un componente visible");
    }
}
