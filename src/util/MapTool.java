package util;

import javafx.scene.input.MouseEvent;

/**
 * @author Rafael Alonso
 */
public interface MapTool {
    void onMousePressed(MouseEvent event);
    void onMouseDragged(MouseEvent event);
    void onMouseReleased(MouseEvent event);

    // Opcionalmente:
    default void onEnter() {}   // cuando activas la herramienta
    default void onExit() {}    // cuando cambias a otra herramienta
}
