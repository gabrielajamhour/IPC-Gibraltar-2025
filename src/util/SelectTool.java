package util;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Arc;   // <- NUEVO

/**
 *
 * @author Rafael Alonso
 */
public class SelectTool implements MapTool {

    private static final double HIT_TOLERANCE = 10.0; // píxeles

    private final Group zoomGroup;
    private final ObjectProperty<Color> currentColor;
    private final DoubleProperty currentLineWidth;

    public SelectTool(Group zoomGroup,
                      ObjectProperty<Color> currentColor,
                      DoubleProperty currentLineWidth) {
        this.zoomGroup = zoomGroup;
        this.currentColor = currentColor;
        this.currentLineWidth = currentLineWidth;
    }

    @Override
    public void onEnter() {
        if (zoomGroup != null) {
            zoomGroup.setCursor(Cursor.HAND);
        }
    }

    @Override
    public void onExit() {
        if (zoomGroup != null) {
            zoomGroup.setCursor(Cursor.DEFAULT);
        }
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        // Convertimos la posición del ratón a coordenadas del zoomGroup
        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        double px = p.getX();
        double py = p.getY();

        Node nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (Node child : zoomGroup.getChildren()) {
            // 1) Líneas
            if (child instanceof Line line) {
                double d = distancePointToSegment(
                        px, py,
                        line.getStartX(), line.getStartY(),
                        line.getEndX(), line.getEndY()
                );
                if (d < bestDist && d <= HIT_TOLERANCE) {
                    bestDist = d;
                    nearest = line;
                }
            }
            // 2) Arcos / círculos (Arc)
            else if (child instanceof Arc arc) {
                double d = distancePointToArc(px, py, arc);
                if (d < bestDist && d <= HIT_TOLERANCE) {
                    bestDist = d;
                    nearest = arc;
                }
            }
            // 3) Pins de POI
            else if (child instanceof Region region && region.getUserData() instanceof Poi) {
                Bounds b = region.getBoundsInParent();
                double cx = (b.getMinX() + b.getMaxX()) / 2.0;
                double cy = (b.getMinY() + b.getMaxY()) / 2.0;

                double d = Math.hypot(px - cx, py - cy);
                if (d < bestDist && d <= HIT_TOLERANCE) {
                    bestDist = d;
                    nearest = region;
                }
            }
        }

        if (nearest != null) {
            applyStyleToNode(nearest);
            event.consume();
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        // Nada por ahora
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // Nada por ahora
    }

    // Distancia de un punto (px,py) a un segmento (x1,y1)-(x2,y2)
    private double distancePointToSegment(double px, double py,
                                          double x1, double y1,
                                          double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;

        if (dx == 0 && dy == 0) {
            // el segmento es en realidad un punto
            return Math.hypot(px - x1, py - y1);
        }

        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.hypot(px - projX, py - projY);
    }

    // ==== NUEVO: distancia de un punto a un Arc ====

    private double distancePointToArc(double px, double py, Arc arc) {
        double cx = arc.getCenterX();
        double cy = arc.getCenterY();

        // radio aproximado (en tu ArcTool usas radioX = radioY)
        double r = (arc.getRadiusX() + arc.getRadiusY()) / 2.0;
        if (r <= 0) return Double.MAX_VALUE;

        // ¿Está el punto dentro del rango angular del arco?
        if (!isPointOnArcAngle(px, py, arc)) {
            return Double.MAX_VALUE;
        }

        double distCenter = Math.hypot(px - cx, py - cy);
        return Math.abs(distCenter - r); // distancia a la circunferencia
    }

    private boolean isPointOnArcAngle(double px, double py, Arc arc) {
        double cx = arc.getCenterX();
        double cy = arc.getCenterY();

        double anglePoint = pointToAngleDeg(cx, cy, px, py);

        double start = normalizeAngle(arc.getStartAngle());
        double length = arc.getLength();      // negativo = horario, positivo = antihorario
        double sweep = Math.abs(length);

        // Pequeño margen angular para que no sea demasiado estricto
        final double ANGLE_TOL = 2.0;

        if (sweep < 1e-3) {
            // arco degenerado, comprobamos solo que estamos cerca del ángulo de inicio
            double diff = angleDiff(start, anglePoint);
            return diff <= ANGLE_TOL;
        }

        if (length >= 0) {
            // Barrido antihorario desde start → anglePoint
            double diffCCW = positiveAngleDiff(start, anglePoint);
            return diffCCW <= sweep + ANGLE_TOL;
        } else {
            // Barrido horario desde start → anglePoint
            // equivale a barrer antihorario desde anglePoint → start
            double diffCW = positiveAngleDiff(anglePoint, start);
            return diffCW <= sweep + ANGLE_TOL;
        }
    }

    // ==== Utilidades de ángulos (copiadas de ArcTool) ====

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

    /**
     * Módulo de la diferencia angular mínima entre dos ángulos.
     */
    private double angleDiff(double a, double b) {
        double na = normalizeAngle(a);
        double nb = normalizeAngle(b);
        double diff = Math.abs(na - nb);
        return (diff > 180.0) ? 360.0 - diff : diff;
    }

    // ==== Aplicar estilo a la marca seleccionada ====

    private void applyStyleToNode(Node n) {
        Color c = currentColor.get();
        if (c == null) c = Color.RED;

        if (n instanceof Line line) {
            line.setStroke(c);
            double width = currentLineWidth.get();
            if (width > 0) {
                line.setStrokeWidth(width);
            }

        } else if (n instanceof Arc arc) {
            arc.setStroke(c);
            double width = currentLineWidth.get();
            if (width > 0) {
                arc.setStrokeWidth(width);
            }

        } else if (n instanceof Region marker) {
            Object ud = marker.getUserData();
            if (ud instanceof Poi poi) {
                poi.setColor(c);
            }

            int r = (int) Math.round(c.getRed() * 255);
            int g = (int) Math.round(c.getGreen() * 255);
            int b = (int) Math.round(c.getBlue() * 255);
            String webColor = String.format("#%02X%02X%02X", r, g, b);

            marker.setStyle("-fx-background-color: " + webColor + ";");
        }
    }
}
