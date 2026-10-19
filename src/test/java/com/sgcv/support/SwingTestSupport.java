package com.sgcv.support;

import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Utilidades mínimas para tests de UI Swing sin AssertJ Swing ni {@code Robot}.
 *
 * <p>Todo el trabajo sobre componentes se ejecuta en el Event Dispatch Thread (EDT)
 * mediante {@link SwingUtilities#invokeAndWait(Runnable)}: construir un {@code JFrame}
 * fuera del EDT produce resultados no deterministas y puede dejar la UI en un estado
 * inconsistente.</p>
 */
public final class SwingTestSupport {

    /**
     * Acción que puede lanzar excepciones comprobadas, para ejecutarla en el EDT.
     */
    @FunctionalInterface
    public interface AccionEnEdt {
        void ejecutar() throws Exception;
    }

    private SwingTestSupport() {
        // Clase utilitaria
    }

    /**
     * Ejecuta la acción en el EDT y propaga cualquier falla a la hebra del test,
     * de modo que JUnit la reporte como error del test.
     */
    public static void enEdt(AccionEnEdt accion) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            accion.ejecutar();
            return;
        }
        Throwable[] falla = new Throwable[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    accion.ejecutar();
                } catch (Throwable t) {
                    falla[0] = t;
                }
            });
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Fallo inesperado al despachar la acción en el EDT", e);
        }
        if (falla[0] instanceof Exception excepcion) {
            throw excepcion;
        }
        if (falla[0] != null) {
            throw new IllegalStateException("Fallo inesperado al ejecutar la acción en el EDT", falla[0]);
        }
    }

    /**
     * Busca recursivamente dentro de {@code raiz} el primer componente con el nombre
     * asignado por {@code setName}. Recorre todos los contenedores, incluidos los que no
     * son {@code JPanel}, para no depender del layout interno del panel.
     *
     * @return el componente encontrado, o {@code null} si ningún descendiente tiene ese nombre
     */
    public static Component buscarPorNombre(Container raiz, String nombre) {
        if (raiz == null) return null;
        Deque<Component> pendientes = new ArrayDeque<>();
        pendientes.add(raiz);
        while (!pendientes.isEmpty()) {
            Component actual = pendientes.removeFirst();
            if (nombre.equals(actual.getName())) {
                return actual;
            }
            if (actual instanceof Container contenedor) {
                for (Component hijo : contenedor.getComponents()) {
                    pendientes.addLast(hijo);
                }
            }
        }
        return null;
    }
}
