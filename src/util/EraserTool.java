package util;

import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

/**
 * @author Rafael Alonso
 * 
 * Herramienta de borrado:
 * 1) Intenta borrar un POI cercano.
 * 2) Si no, una línea cercana.
 * 3) Si no, una circunferencia cercana.
 * 4) Si no, un arco cercano.
 *
 * Funciona tanto con clic como con click-and-drag.
 */
public class EraserTool implements MapTool {

    private final Group zoomGroup;
    private final ListView<Poi> poiListView;
    private final ObservableList<Line> lineData;
    private final ObservableList<Arc> arcData;
    private final ObservableList<Text> textList;
    private final Node mapPin;

    // Radio máximo de borrado en píxeles
    private static final double MAX_DISTANCE = 5.0;

    public EraserTool(Group zoomGroup,
                      ListView<Poi> poiListView,
                      ObservableList<Line> lineData,
                      ObservableList<Arc> arcData,
                      ObservableList<Text> textList,
                      Node mapPin) {
        this.zoomGroup = zoomGroup;
        this.poiListView = poiListView;
        this.lineData = lineData;
        this.arcData = arcData;
        this.textList = textList;
        this.mapPin = mapPin;
    }

    @Override
    public void onEnter() {
        // nada especial
    }

    @Override
    public void onExit() {
        // nada especial
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY || zoomGroup == null) return;
        eraseAt(event);
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        if (zoomGroup == null) return;
        eraseAt(event);
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // no-op
    }

    // ===================== LÓGICA DE BORRADO =====================

    private void eraseAt(MouseEvent event) {
        Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());

        // 1) Intentar borrar un POI cercano
        Poi poiToRemove = findPoiNear(localPoint);
        if (poiToRemove != null) {
            poiListView.getItems().remove(poiToRemove);
            removePoiMarkersFromMap(poiToRemove);

            if (mapPin != null && mapPin.isVisible()) {
                mapPin.setVisible(false);
            }
            return;
        }

        // 2) Intentar borrar una línea cercana
        Line lineToRemove = findLineNear(localPoint);
        if (lineToRemove != null) {
            zoomGroup.getChildren().remove(lineToRemove);
            if (lineData != null) {
                lineData.remove(lineToRemove);
            }
            return;
        }

        // 3) Intentar borrar un arco cercano (incluye círculo)
        Arc arcToRemove = findArcNear(localPoint);
        if (arcToRemove != null) {
            zoomGroup.getChildren().remove(arcToRemove);
            if (arcData != null) {
                arcData.remove(arcToRemove);
            }
            return;
        }
        
        // 4) Intenta borrar un texto cercano
        Text text = findTextNear(localPoint);
        if (text != null) {
            zoomGroup.getChildren().remove(text);   // lo quitas del mapa
            if (textList != null) {
                textList.remove(text);              // y de la lista compartida
            }
            return;
        }
    }

    // ---------------- POIs ----------------

    private Poi findPoiNear(Point2D point) {
        // AUMENTA este valor para que sea más fácil acertar
        final double MAX_DISTANCE_POI = 40;

        Poi closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Poi poi : poiListView.getItems()) {
            Point2D poiPos = poi.getPosition(); // mismas coords en las que lo dibujas

            double dx = poiPos.getX() - point.getX();
            double dy = poiPos.getY() - point.getY();
            double dist = Math.hypot(dx, dy);

            if (dist <= MAX_DISTANCE_POI && dist < closestDist) {
                closestDist = dist;
                closest = poi;
            }
        }
        return closest;
    }

    private void removePoiMarkersFromMap(Poi poi) {
        if (zoomGroup == null || poi == null) return;

        for (int i = zoomGroup.getChildren().size() - 1; i >= 0; i--) {
            Node n = zoomGroup.getChildren().get(i);
            Object ud = n.getUserData();
            if (ud == poi) {
                zoomGroup.getChildren().remove(i);
            }
        }
    }

    // ---------------- Líneas ----------------

    private Line findLineNear(Point2D point) {
        if (zoomGroup == null) return null;

        Line closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Node n : zoomGroup.getChildren()) {
            if (!(n instanceof Line)) continue;
            Line line = (Line) n;

            double dist = distancePointToSegment(point, line);
            if (dist <= MAX_DISTANCE && dist < closestDist) {
                closestDist = dist;
                closest = line;
            }
        }
        return closest;
    }

    private double distancePointToSegment(Point2D p, Line line) {
        Point2D a = new Point2D(line.getStartX(), line.getStartY());
        Point2D b = new Point2D(line.getEndX(),   line.getEndY());

        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();

        if (dx == 0 && dy == 0) {
            return p.distance(a);
        }

        double t = ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) /
                   (dx * dx + dy * dy);

        t = Math.max(0, Math.min(1, t));

        Point2D projection = new Point2D(a.getX() + t * dx, a.getY() + t * dy);
        return p.distance(projection);
    }


    // ---------------- Arcos ----------------
    private Arc findArcNear(Point2D point) {
        if (zoomGroup == null) return null;

        Arc closest = null;
        double closestDist = Double.MAX_VALUE;

        for (Node n : zoomGroup.getChildren()) {
            if (!(n instanceof Arc)) continue;
            Arc arc = (Arc) n;

            double cx = arc.getCenterX();
            double cy = arc.getCenterY();
            double rx = arc.getRadiusX();
            double ry = arc.getRadiusY();
            double radius = (rx + ry) / 2.0;

            Point2D center = new Point2D(cx, cy);
            double distCenter = center.distance(point);
            double radialDiff = Math.abs(distCenter - radius);
            
            if (radialDiff > MAX_DISTANCE) continue;

            double anglePoint = pointToAngleDeg(cx, cy, point.getX(), point.getY());
            if (!isAngleOnArc(anglePoint, arc.getStartAngle(), arc.getLength())) {
                continue;
            }

            if (radialDiff < closestDist) {
                closestDist = radialDiff;
                closest = arc;
            }
        }
        return closest;
    }
    
    private Text findTextNear(Point2D point) {
        Text nearest = null;
        double bestDist = Double.MAX_VALUE;
        
        double px = point.getX();
        double py = point.getY();

        for (Node child : zoomGroup.getChildren()) {
            if (child instanceof Text text) {
                Bounds b = text.getBoundsInParent();
                double cx = (b.getMinX() + b.getMaxX()) / 2.0;
                double cy = (b.getMinY() + b.getMaxY()) / 2.0;

                double d = Math.hypot(px - cx, py - cy);
                if (d < bestDist && d <= MAX_DISTANCE) {
                    bestDist = d;
                    nearest = text;
                }
            }
        }

        return nearest;
    }

    // ---------------- Utilidades de ángulos ----------------

    private double pointToAngleDeg(double cx, double cy, double x, double y) {
        double dx = x - cx;
        double dy = y - cy;
        double angleRad = Math.atan2(-dy, dx);
        double angleDeg = Math.toDegrees(angleRad);
        return normalizeAngle(angleDeg);
    }

    private double normalizeAngle(double angleDeg) {
        double a = angleDeg % 360.0;
        if (a < 0) a += 360.0;
        return a;
    }

    private boolean isAngleOnArc(double angle, double start, double length) {
        // Caso especial: círculos completos (también los que guardamos como ±360º)
        if (Math.abs(length) >= 359.9) {
            return true; // cualquier ángulo está "sobre" el arco
        }

        double a = normalizeAngle(angle);
        double s = normalizeAngle(start);
        double e = normalizeAngle(start + length);

        if (length >= 0) {
            // arco antihorario
            if (s <= e) {
                return a >= s && a <= e;
            } else {
                // envuelve 360
                return a >= s || a <= e;
            }
        } else {
            // arco horario
            if (e <= s) {
                return a >= e && a <= s;
            } else {
                // envuelve 360
                return a >= e || a <= s;
            }
        }
    }
}
