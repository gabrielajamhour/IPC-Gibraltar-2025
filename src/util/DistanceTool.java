package util;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.event.EventHandler;

/**
 * @author Rafael Alonso
 * Herramienta para medir distancias sobre el mapa.
 * - Primer clic: fija el punto inicial.
 * - Segundo clic: fija el punto final.
 * - Entre clic y clic, la línea sigue al ratón.
 * - Solo hay una medida visible a la vez.
 */
public class DistanceTool implements MapTool {

    private final Group zoomGroup;
    private final DoubleProperty currentLineWidth;
    private final ObjectProperty<Color> currentColor;

    private boolean waitingSecondClick = false;

    private Group currentMeasure;
    private Line mainLine;
    private Line tickStart;
    private Line tickEnd;
    private Text label;

    private double startX;
    private double startY;

    private static final double BASE_TICK_SIZE = 8.0;
    private static final double MIN_DISTANCE   = 3.0;

    private final EventHandler<MouseEvent> mouseMovedHandler = this::handleMouseMoved;

    public DistanceTool(Group zoomGroup,
                        DoubleProperty currentLineWidth,
                        ObjectProperty<Color> currentColor) {
        this.zoomGroup = zoomGroup;
        this.currentLineWidth = currentLineWidth;
        this.currentColor = currentColor;
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || zoomGroup == null) {
            return;
        }

        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());

        // PRIMER CLIC
        if (!waitingSecondClick) {

            // Si ya había una medida, la quitamos
            if (currentMeasure != null) {
                zoomGroup.getChildren().remove(currentMeasure);
                clearCurrentMeasure();
            }

            startX = p.getX();
            startY = p.getY();

            Color c = currentColor.get();
            if (c == null) c = Color.RED;

            double width = currentLineWidth.get();

            mainLine = new Line(startX, startY, startX, startY);
            mainLine.setStroke(c);
            mainLine.setStrokeWidth(width);
            mainLine.setStrokeLineCap(StrokeLineCap.ROUND);

            tickStart = new Line();
            tickStart.setStroke(c);
            tickStart.setStrokeWidth(width);

            tickEnd = new Line();
            tickEnd.setStroke(c);
            tickEnd.setStrokeWidth(width);

            label = new Text();
            label.setFill(c);
            // el tamaño de la fuente se ajusta en updateGeometry()

            currentMeasure = new Group(mainLine, tickStart, tickEnd, label);
            zoomGroup.getChildren().add(currentMeasure);

            waitingSecondClick = true;
            return;
        }

        // SEGUNDO CLIC
        if (waitingSecondClick && mainLine != null) {
            double endX = p.getX();
            double endY = p.getY();

            mainLine.setEndX(endX);
            mainLine.setEndY(endY);

            updateGeometry(endX, endY);

            double dx = endX - startX;
            double dy = endY - startY;
            double dist = Math.hypot(dx, dy);

            if (dist < MIN_DISTANCE) {
                zoomGroup.getChildren().remove(currentMeasure);
                clearCurrentMeasure();
            }

            waitingSecondClick = false;
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        if (!waitingSecondClick || mainLine == null || zoomGroup == null) return;

        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        updateGeometry(p.getX(), p.getY());
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // Nada especial aquí con el modo dos clics
    }

    private void handleMouseMoved(MouseEvent event) {
        if (!waitingSecondClick) return;
        if (currentMeasure == null || mainLine == null) return;
        if (zoomGroup == null) return;

        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        updateGeometry(p.getX(), p.getY());
    }

    /**
     * Recalcula línea, ticks y texto para el extremo final (endX, endY).
     * - El tamaño de la fuente se vincula al grosor actual.
     * - El texto se coloca siempre "por arriba" de la línea (y menor).
     */
    private void updateGeometry(double endX, double endY) {
        mainLine.setEndX(endX);
        mainLine.setEndY(endY);

        double dx = endX - startX;
        double dy = endY - startY;
        double length = Math.hypot(dx, dy);
        if (length == 0) return;

        // Vector normal unitario
        double nx = -dy / length;
        double ny =  dx / length;

        double tickSize = BASE_TICK_SIZE + currentLineWidth.get();

        // --- tamaño del texto en función del grosor ---
        double fontSize = 8.0 + currentLineWidth.get() * 2.0; // ajusta estos valores a tu gusto
        label.setFont(Font.font("System", FontWeight.BOLD, fontSize));

        // Marca en el inicio
        double sx1 = startX + nx * (tickSize / 2.0);
        double sy1 = startY + ny * (tickSize / 2.0);
        double sx2 = startX - nx * (tickSize / 2.0);
        double sy2 = startY - ny * (tickSize / 2.0);
        tickStart.setStartX(sx1);
        tickStart.setStartY(sy1);
        tickStart.setEndX(sx2);
        tickStart.setEndY(sy2);

        // Marca en el final
        double ex1 = endX + nx * (tickSize / 2.0);
        double ey1 = endY + ny * (tickSize / 2.0);
        double ex2 = endX - nx * (tickSize / 2.0);
        double ey2 = endY - ny * (tickSize / 2.0);
        tickEnd.setStartX(ex1);
        tickEnd.setStartY(ey1);
        tickEnd.setEndX(ex2);
        tickEnd.setEndY(ey2);

        // --- Texto en el centro, siempre por encima de la línea ---
        double midX = (startX + endX) / 2.0;
        double midY = (startY + endY) / 2.0;
        double offset = tickSize + 4.0;

        // Vector de desplazamiento inicial a lo largo de la normal
        double ox = nx * offset;
        double oy = ny * offset;

        // Como en JavaFX Y crece hacia abajo, "por arriba" = Y menor que midY.
        // Si el desplazamiento actual baja el texto (oy > 0), invertimos la dirección.
        if (oy > 0) {
            ox = -ox;
            oy = -oy;
        }

        label.setX(midX + ox);
        label.setY(midY + oy);
        label.setText(String.format("%.1f px", length));
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
            if (currentMeasure != null) {
                zoomGroup.getChildren().remove(currentMeasure);
            }
        }
        clearCurrentMeasure();
        waitingSecondClick = false;
    }

    private void clearCurrentMeasure() {
        currentMeasure = null;
        mainLine = null;
        tickStart = null;
        tickEnd = null;
        label = null;
    }
}
