package util;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.event.EventHandler;

/**
 * @author Rafael Alonso
 * 
 * Herramienta de compás:
 *
 *  Clic 1: centro del compás
 *  Clic 2: punto de inicio del arco (define radio y ángulo inicial)
 *  Mover ratón: se ve el arco "en vivo"
 *  Clic 3: punto final del arco
 *
 *  Si el ángulo final está casi encima del inicial (por ejemplo < 5º de diferencia),
 *  se interpreta como circunferencia completa.
 */
public class ArcTool implements MapTool {

    private static final double MIN_RADIUS = 3.0;
    private static final double FULL_CIRCLE_MARGIN_DEG = 5.0;

    private final Group zoomGroup;
    private final ObservableList<Circle> circleData;
    private final ObservableList<Arc> arcData;
    private final DoubleProperty currentLineWidth;
    private final ObjectProperty<Color> currentColor;

    private enum Step {
        WAIT_CENTER,
        WAIT_START,
        WAIT_END
    }

    private Step step = Step.WAIT_CENTER;

    private double centerX;
    private double centerY;
    private double radius;
    private double startAngleDeg;

    private Arc currentArc;

    private final EventHandler<MouseEvent> mouseMovedHandler = this::handleMouseMoved;

    public ArcTool(Group zoomGroup,
                   ObservableList<Circle> circleData,
                   ObservableList<Arc> arcData,
                   DoubleProperty currentLineWidth,
                   ObjectProperty<Color> currentColor) {
        this.zoomGroup = zoomGroup;
        this.circleData = circleData;
        this.arcData = arcData;
        this.currentLineWidth = currentLineWidth;
        this.currentColor = currentColor;
    }

    @Override
    public void onEnter() {
        if (zoomGroup != null) {
            zoomGroup.addEventFilter(MouseEvent.MOUSE_MOVED, mouseMovedHandler);
        }
    }

    @Override
    public void onExit() {
        if (zoomGroup != null) {
            zoomGroup.removeEventFilter(MouseEvent.MOUSE_MOVED, mouseMovedHandler);
        }
        if (currentArc != null && zoomGroup != null) {
            zoomGroup.getChildren().remove(currentArc);
        }
        currentArc = null;
        step = Step.WAIT_CENTER;
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || zoomGroup == null) {
            return;
        }

        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());

        switch (step) {
            case WAIT_CENTER:
                // Primer clic: fijamos el centro del compás
                centerX = p.getX();
                centerY = p.getY();
                step = Step.WAIT_START;
                break;

            case WAIT_START:
                // Segundo clic: definimos radio y ángulo de inicio
                radius = p.distance(centerX, centerY);
                if (radius < MIN_RADIUS) {
                    // Demasiado cerca del centro: ignoramos y seguimos esperando buen punto
                    return;
                }

                startAngleDeg = pointToAngleDeg(centerX, centerY, p.getX(), p.getY());

                currentArc = new Arc();
                currentArc.setCenterX(centerX);
                currentArc.setCenterY(centerY);
                currentArc.setRadiusX(radius);
                currentArc.setRadiusY(radius);
                currentArc.setStartAngle(startAngleDeg);
                currentArc.setLength(0); // todavía no hay barrido

                currentArc.setStroke(currentColor.get());
                currentArc.setStrokeWidth(currentLineWidth.get());
                currentArc.setFill(Color.TRANSPARENT);
                currentArc.setType(ArcType.OPEN);

                zoomGroup.getChildren().add(currentArc);

                step = Step.WAIT_END;
                break;

            case WAIT_END:
                // Tercer clic: fijamos el punto final del arco
                finalizeArc(p);
                break;
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        if (zoomGroup == null) return;
        // Permitimos también actualizar el arco mientras se arrastra
        handleMouseMoved(event);
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // No hacemos nada: el arco se fija con el tercer clic
    }

    // ===================== LÓGICA INTERNA =====================

    /**
     * Actualiza el "preview" del arco mientras esperamos el tercer clic.
     */
    private void handleMouseMoved(MouseEvent event) {
        if (step != Step.WAIT_END) return;
        if (currentArc == null || zoomGroup == null) return;

        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        double endAngleDeg = pointToAngleDeg(centerX, centerY, p.getX(), p.getY());

        // Barrido en sentido HORARIO:
        // magnitud = giro antihorario desde end → start
        double sweepClockwise = -positiveAngleDiff(endAngleDeg, startAngleDeg);

        currentArc.setStartAngle(startAngleDeg);
        currentArc.setLength(sweepClockwise);
    }


    /**
     * Termina el arco en la posición indicada. Decide si es arco o círculo completo.
     */
    private void finalizeArc(Point2D endPoint) {
        if (currentArc == null || zoomGroup == null) {
            step = Step.WAIT_CENTER;
            return;
        }

        double endAngleDeg = pointToAngleDeg(centerX, centerY, endPoint.getX(), endPoint.getY());

        // Magnitud del barrido EN SENTIDO HORARIO:
        // "lo que hay que girar desde end hasta start en sentido antihorario",
        // que equivale a ir de start a end en horario.
        double sweepClockwiseMag = positiveAngleDiff(endAngleDeg, startAngleDeg);

        // ¿Es prácticamente un círculo completo?
        if (sweepClockwiseMag < FULL_CIRCLE_MARGIN_DEG ||
            sweepClockwiseMag > 360.0 - FULL_CIRCLE_MARGIN_DEG) {

            // ➜ Interpretamos como circunferencia completa
            zoomGroup.getChildren().remove(currentArc);

            Circle circle = new Circle(centerX, centerY, radius);
            circle.setStroke(currentColor.get());
            circle.setStrokeWidth(currentLineWidth.get());
            circle.setFill(Color.TRANSPARENT);

            zoomGroup.getChildren().add(circle);
            if (circleData != null && !circleData.contains(circle)) {
                circleData.add(circle);
            }
        } else {
            // ➜ Arco parcial en sentido HORARIO
            double sweepClockwise = -sweepClockwiseMag;

            currentArc.setStartAngle(startAngleDeg);
            currentArc.setLength(sweepClockwise);

            if (arcData != null && !arcData.contains(currentArc)) {
                arcData.add(currentArc);
            }
        }

        currentArc = null;
        step = Step.WAIT_CENTER;
    }


    // ===================== UTILIDADES DE ÁNGULOS =====================

    private double pointToAngleDeg(double cx, double cy, double x, double y) {
        double dx = x - cx;
        double dy = y - cy;

        // Invertimos Y porque en pantalla crece hacia abajo
        double angleRad = Math.atan2(-dy, dx); // 0° en la derecha, sentido antihorario
        double angleDeg = Math.toDegrees(angleRad);
        return normalizeAngle(angleDeg);
    }

    private double normalizeAngle(double angleDeg) {
        double a = angleDeg % 360.0;
        if (a < 0) {
            a += 360.0;
        }
        return a;
    }

    /**
     * Diferencia angular positiva en [0, 360).
     * Devuelve cuánto hay que girar desde startDeg hasta endDeg en sentido antihorario.
     */
    private double positiveAngleDiff(double startDeg, double endDeg) {
        double s = normalizeAngle(startDeg);
        double e = normalizeAngle(endDeg);
        double diff = e - s;
        if (diff < 0) {
            diff += 360.0;
        }
        return diff;
    }
}
