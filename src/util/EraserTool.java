package util;

import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Line;

/**
 *
 * @author Rafael Alonso
 */
public class EraserTool implements MapTool {

    private final Group zoomGroup;
    private final ListView<Poi> poiListView;

    private final double HIT_RADIUS = 10.0;

    public EraserTool(Group zoomGroup, ListView<Poi> poiListView) {
        this.zoomGroup = zoomGroup;
        this.poiListView = poiListView;
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        eraseAt(event);
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;
        // mientras arrastras, sigue borrando todo lo que pasa por el camino
        eraseAt(event);
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // nada especial
    }

    // ----------------- LÓGICA DE BORRADO -----------------

    private void eraseAt(MouseEvent event) {
        // Convertir coordenadas de escena a coordenadas del zoomGroup
        Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());

        // 1) Intentar borrar un POI cercano
        Poi poiToRemove = findPoiNear(localPoint);
        if (poiToRemove != null) {
            poiListView.getItems().remove(poiToRemove);
            return; // si ya has borrado un POI, no hace falta seguir con líneas
        }

        // 2) Intentar borrar una línea cercana
        Line lineToRemove = findLineNear(localPoint);
        if (lineToRemove != null) {
            zoomGroup.getChildren().remove(lineToRemove);
        }
    }

    private Poi findPoiNear(Point2D p) {
        for (Poi poi : poiListView.getItems()) {
            if (poi.getPosition() != null &&
                poi.getPosition().distance(p) <= HIT_RADIUS) {
                return poi;
            }
        }
        return null;
    }

    private Line findLineNear(Point2D p) {
        for (Node n : zoomGroup.getChildren()) {
            if (n instanceof Line line) {
                if (distancePointToSegment(p, line) <= HIT_RADIUS) {
                    return line;
                }
            }
        }
        return null;
    }

    private double distancePointToSegment(Point2D p, Line line) {
        Point2D a = new Point2D(line.getStartX(), line.getStartY());
        Point2D b = new Point2D(line.getEndX(), line.getEndY());

        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();

        if (dx == 0 && dy == 0) {
            return p.distance(a);
        }

        double t = ((p.getX() - a.getX()) * dx + (p.getY() - a.getY()) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        Point2D proj = new Point2D(a.getX() + t * dx, a.getY() + t * dy);
        return p.distance(proj);
    }
}
