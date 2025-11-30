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
import javafx.event.EventHandler;
import javafx.scene.shape.StrokeLineCap;

/**
 * @author Rafael Alonso
 * 
 * Herramienta de compás (arcos y círculos usando SOLO Arc):
 *
 *  Clic 1: centro del compás
 *  Clic 2: punto de inicio del arco (define radio y ángulo inicial)
 *  Mover ratón: se ve el arco "en vivo" (sentido horario)
 *  Clic 3: punto final del arco
 *
 *  Si la diferencia angular es casi 360º, se guarda un Arc de 360º (equivale a círculo).
 */
public class ArcTool implements MapTool {

    private static final double MIN_RADIUS = 3.0;
    private static final double FULL_CIRCLE_MARGIN_DEG = 5.0;

    private final Group zoomGroup;
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
                   ObservableList<Arc> arcData,
                   DoubleProperty currentLineWidth,
                   ObjectProperty<Color> currentColor) {
        this.zoomGroup = zoomGroup;
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
                // Clic 1: centro del compás
                centerX = p.getX();
                centerY = p.getY();
                step = Step.WAIT_START;
                break;

            case WAIT_START:
                // Clic 2: define el radio y el ángulo de inicio
                radius = p.distance(centerX, centerY);
                if (radius < MIN_RADIUS) {
                    return;
                }

                startAngleDeg = pointToAngleDeg(centerX, centerY, p.getX(), p.getY());

                currentArc = new Arc();
                currentArc.setCenterX(centerX);
                currentArc.setCenterY(centerY);
                currentArc.setRadiusX(radius);
                currentArc.setRadiusY(radius);
                currentArc.setStartAngle(startAngleDeg);
                currentArc.setLength(0);

                currentArc.setStroke(currentColor.get());
                currentArc.setStrokeWidth(currentLineWidth.get());
                currentArc.setStrokeLineCap(StrokeLineCap.ROUND);
                currentArc.setFill(Color.TRANSPARENT);
                currentArc.setType(ArcType.OPEN);

                zoomGroup.getChildren().add(currentArc);

                step = Step.WAIT_END;
                break;

            case WAIT_END:
                // Clic 3: fija el ángulo final (arco o círculo)
                finalizeArc(p);
                break;
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        if (zoomGroup == null) return;
        handleMouseMoved(event);
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // El arco se fija con el tercer clic, no aquí.
    }

    // ============ PREVIEW DEL ARCO ============

    private void handleMouseMoved(MouseEvent event) {
        if (step != Step.WAIT_END) return;
        if (currentArc == null || zoomGroup == null) return;

        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        double endAngleDeg = pointToAngleDeg(centerX, centerY, p.getX(), p.getY());

        // Magnitud del barrido EN SENTIDO HORARIO:
        // cuánto hay que girar antihorario de end -> start
        double sweepMag = positiveAngleDiff(endAngleDeg, startAngleDeg);

        // Si estamos muy cerca de cerrar el círculo, lo forzamos a 360º
        if (sweepMag > 360.0 - FULL_CIRCLE_MARGIN_DEG) {
            sweepMag = 360.0;
        }

        double sweepClockwise = -sweepMag; // negativo = horario en JavaFX

        currentArc.setCenterX(centerX);
        currentArc.setCenterY(centerY);
        currentArc.setRadiusX(radius);
        currentArc.setRadiusY(radius);
        currentArc.setStartAngle(startAngleDeg);
        currentArc.setLength(sweepClockwise);
    }


    // ============ FINALIZAR ARCO ============

    private void finalizeArc(Point2D endPoint) {
        if (currentArc == null || zoomGroup == null) {
            step = Step.WAIT_CENTER;
            return;
        }

        double endAngleDeg = pointToAngleDeg(centerX, centerY, endPoint.getX(), endPoint.getY());

        // Magnitud del barrido horario
        double sweepMag = positiveAngleDiff(endAngleDeg, startAngleDeg);

        // Igual que en el preview: si está muy cerca de 360º → lo fijamos en 360
        if (sweepMag > 360.0 - FULL_CIRCLE_MARGIN_DEG) {
            sweepMag = 360.0;
        }

        boolean isFullCircle = Math.abs(sweepMag - 360.0) < 0.0001;

        double sweepClockwise = -sweepMag;

        currentArc.setCenterX(centerX);
        currentArc.setCenterY(centerY);
        currentArc.setRadiusX(radius);
        currentArc.setRadiusY(radius);

        if (isFullCircle) {
            // Círculo completo (360º) en sentido horario
            currentArc.setStartAngle(0);      // da igual el inicio, es 360º
            currentArc.setLength(-360.0);
        } else {
            // Arco parcial horario
            currentArc.setStartAngle(startAngleDeg);
            currentArc.setLength(sweepClockwise);
        }

        if (arcData != null && !arcData.contains(currentArc)) {
            arcData.add(currentArc);
        }

        currentArc = null;
        step = Step.WAIT_CENTER;
    }


    // ============ UTILIDADES DE ÁNGULOS ============

    private double pointToAngleDeg(double cx, double cy, double x, double y) {
        double dx = x - cx;
        double dy = y - cy;

        // ejes de pantalla: Y hacia abajo
        double angleRad = Math.atan2(-dy, dx); // 0° = derecha, antihorario positivo
        double angleDeg = Math.toDegrees(angleRad);
        return normalizeAngle(angleDeg);
    }

    private double normalizeAngle(double angleDeg) {
        double a = angleDeg % 360.0;
        if (a < 0) a += 360.0;
        return a;
    }

    /**
     * Diferencia positiva [0,360) de giro antihorario desde start → end.
     */
    private double positiveAngleDiff(double startDeg, double endDeg) {
        double s = normalizeAngle(startDeg);
        double e = normalizeAngle(endDeg);
        double diff = e - s;
        if (diff < 0) diff += 360.0;
        return diff;
    }
}
