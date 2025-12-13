package util;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.event.EventHandler;

/*
 * @author Rafael Alonso 
 * Herramienta para trazar líneas sobre el mapa.
 *
 * Comportamiento híbrido:
 * - Si el usuario hace click y arrastra (distancia >= DRAG_THRESHOLD) → línea por drag.
 * - Si el usuario hace un click casi sin mover (distancia < DRAG_THRESHOLD) → se interpreta
 *   como primer punto de una línea "two-click". La línea sigue al ratón hasta el segundo click.
 *
 * Todas las líneas creadas se añaden a lineData para poder reconstruirlas
 * al volver a cargar el MainController.
 */
public class LineTool implements MapTool {

    // Umbral de píxeles para decidir si ha sido arrastre o clic
    private static final double DRAG_THRESHOLD = 5.0;

    private final Group zoomGroup;
    private final ObservableList<Line> lineData;
    private final DoubleProperty currentLineWidth;
    private final ObjectProperty<Color> currentColor;

    private Line currentLine;
    private boolean waitingSecondClick = false;

    private double pressX;
    private double pressY;

    // Handler reutilizable para MOUSE_MOVED (preview en modo two-click)
    private final EventHandler<MouseEvent> mouseMovedHandler = this::handleMouseMoved;

    public LineTool(Group zoomGroup,
                    ObservableList<Line> lineData,
                    DoubleProperty currentLineWidth,
                    ObjectProperty<Color> currentColor) {
        this.zoomGroup = zoomGroup;
        this.lineData = lineData;
        this.currentLineWidth = currentLineWidth;
        this.currentColor = currentColor;
    }

    @Override
    public void onEnter() {
        if (zoomGroup != null) {
            zoomGroup.addEventHandler(MouseEvent.MOUSE_MOVED, mouseMovedHandler);
        }
    }

    @Override
    public void onExit() {
        if (zoomGroup != null) {
            zoomGroup.removeEventHandler(MouseEvent.MOUSE_MOVED, mouseMovedHandler);
        }
        currentLine = null;
        waitingSecondClick = false;
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || zoomGroup == null) {
            return;
        }

        // Convertimos la posición del ratón a coordenadas de zoomGroup,
        // por si hay zoom/transformaciones
        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        pressX = p.getX();
        pressY = p.getY();

        // Si NO estamos esperando el segundo clic, creamos una nueva línea
        if (!waitingSecondClick) {
            Color c = currentColor.get();
            if (c == null) {
                c = Color.RED;
            }

            currentLine = new Line(pressX, pressY, pressX, pressY);
            currentLine.setStroke(c);
            currentLine.setStrokeWidth(currentLineWidth.get());

            // Bordes redondeados → efecto "cilindro"
            currentLine.setStrokeLineCap(StrokeLineCap.ROUND);

            zoomGroup.getChildren().add(currentLine);
        }
        // Si sí estamos esperando segundo click, usamos la currentLine ya existente.
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || zoomGroup == null) {
            return;
        }

        // En cualquier caso, si hay una currentLine, la actualizamos
        // Esto da soporte al modo "drag"
        if (currentLine != null) {
            Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
            currentLine.setEndX(p.getX());
            currentLine.setEndY(p.getY());
        }
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || zoomGroup == null) {
            return;
        }
        if (currentLine == null) {
            return;
        }

        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        double releaseX = p.getX();
        double releaseY = p.getY();

        double dist = Math.hypot(releaseX - pressX, releaseY - pressY);

        if (dist >= DRAG_THRESHOLD) {
            // ➜ Lo interpretamos como línea por arrastre
            currentLine.setEndX(releaseX);
            currentLine.setEndY(releaseY);

            if (!lineData.contains(currentLine)) {
                lineData.add(currentLine);
            }

            currentLine = null;
            waitingSecondClick = false;
        } else {
            // ➜ Lo interpretamos como un clic (sin arrastre significativo)
            if (!waitingSecondClick) {
                // Primer clic → fijamos punto inicial y esperamos segundo clic
                waitingSecondClick = true;
                // El extremo se seguirá moviendo con el ratón en handleMouseMoved
            } else {
                // Segundo clic → fijamos el punto final y guardamos la línea
                currentLine.setEndX(releaseX);
                currentLine.setEndY(releaseY);

                if (!lineData.contains(currentLine)) {
                    lineData.add(currentLine);
                }

                currentLine = null;
                waitingSecondClick = false;
            }
        }
    }

    /**
     * Mueve el extremo de la línea mientras esperamos el segundo clic.
     * Se llama en cada movimiento del ratón cuando la herramienta está activa.
     */
    private void handleMouseMoved(MouseEvent event) {
        if (!waitingSecondClick) return;
        if (currentLine == null) return;
        if (zoomGroup == null) return;

        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        currentLine.setEndX(p.getX());
        currentLine.setEndY(p.getY());
    }
}
