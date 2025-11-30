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
            // 2) Pins de POI
            else if (child instanceof Region region && region.getUserData() instanceof PoiTool) {
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

    private void applyStyleToNode(Node n) {
        Color c = currentColor.get();
        if (c == null) c = Color.RED;

        if (n instanceof Line line) {
            line.setStroke(c);
            double width = currentLineWidth.get();
            if (width > 0) {
                line.setStrokeWidth(width);
            }
        } else if (n instanceof Region marker) {
            Object ud = marker.getUserData();
            if (ud instanceof PoiTool poi) {
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
