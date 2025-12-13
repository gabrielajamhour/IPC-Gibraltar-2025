package util;

import java.util.function.Consumer;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Line;
import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * @author Rafael Alonso
 */
public final class ExtremosOverlay {

    private final Group zoomGroup;
    private final Group overlay;

    private boolean enabled = false;
    private Poi lastPoi = null;

    // Visual settings
    private double dashA = 8.0;
    private double dashB = 6.0;
    // Make it clearly visible by default (user complained it was too thin)
    private double strokeWidth = 2.5;
    
    // Click-to-select POI sobre el mapa (sin depender de la lista)
    private static final double HIT_TOLERANCE = 40.0;

    private final EventHandler<MouseEvent> pickPoiHandler = this::onZoomGroupPressed;
    
    private Consumer<Poi> onPoiPicked;

    
    public ExtremosOverlay(Group zoomGroup) {
        if (zoomGroup == null) throw new IllegalArgumentException("zoomGroup cannot be null");
        this.zoomGroup = zoomGroup;
        this.overlay = new Group();
        this.overlay.setMouseTransparent(true);
        this.zoomGroup.getChildren().add(this.overlay);
    }

    /** Enables/disables the overlay. When disabled, it clears current marks. */
    public void setEnabled(boolean enabled) {
        if (this.enabled == enabled) return;
        this.enabled = enabled;

        if (enabled) {
            // Capturamos clicks en POIs sobre el mapa
            zoomGroup.addEventFilter(MouseEvent.MOUSE_PRESSED, pickPoiHandler);
            redraw();
        } else {
            zoomGroup.removeEventFilter(MouseEvent.MOUSE_PRESSED, pickPoiHandler);
            clear();
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    /** Clears current marks. */
    public void clear() {
        overlay.getChildren().clear();
    }

    /**
     * Shows (or updates) the extremes for the given POI.
     * If disabled, it remembers the POI but does not draw.
     */
    public void showFor(Poi poi) {
        this.lastPoi = poi;
        if (!enabled) return;
        draw(poi);
    }

    /** Redraw using the last POI. */
    public void redraw() {
        if (!enabled) return;
        draw(lastPoi);
    }

    /**
     * Returns the map bounds in zoomGroup coordinates.
     * Prefers an ImageView child; fallback to zoomGroup layout bounds.
     */
    private Bounds getMapBoundsInParent() {
        // Prefer the biggest ImageView (the map) in case there are other ImageViews (icons, avatars, ...)
        ImageView best = null;
        double bestArea = -1;

        for (Node n : zoomGroup.getChildren()) {
            if (n instanceof ImageView iv) {
                Bounds b = iv.getBoundsInParent();
                double area = b.getWidth() * b.getHeight();
                if (area > bestArea) {
                    bestArea = area;
                    best = iv;
                }
            }
        }

        if (best != null) {
            return best.getBoundsInParent();
        }

        return zoomGroup.getLayoutBounds();
    }

    private void draw(Poi poi) {
        clear();
        if (poi == null) return;

        Point2D p = poi.getPosition();
        if (p == null) return;

        Bounds b = getMapBoundsInParent();
        if (b == null) return;

        double x = p.getX();
        double y = p.getY();

        double left   = b.getMinX();
        double right  = b.getMaxX();
        double top    = b.getMinY();
        double bottom = b.getMaxY();

        // Use the same color as the POI
        Paint stroke = poi.getColor() != null ? poi.getColor() : Color.BLACK;

        // 4 guide lines (up, down, left, right)
        Line leftLine   = new Line(x, y, left,  y);
        Line rightLine  = new Line(x, y, right, y);
        Line topLine    = new Line(x, y, x, top);
        Line bottomLine = new Line(x, y, x, bottom);

        styleGuideLine(leftLine, stroke);
        styleGuideLine(rightLine, stroke);
        styleGuideLine(topLine, stroke);
        styleGuideLine(bottomLine, stroke);

        overlay.getChildren().addAll(leftLine, rightLine, topLine, bottomLine);
    }

    private void styleGuideLine(Line line, Paint stroke) {
        line.setStroke(stroke);
        line.setStrokeWidth(strokeWidth);
        line.getStrokeDashArray().setAll(dashA, dashB);
        line.setMouseTransparent(true);
    }

    // Optional setters
    public void setDash(double a, double b) { this.dashA = a; this.dashB = b; redraw(); }
    public void setStrokeWidth(double w) { this.strokeWidth = w; redraw(); }
    // Ticks removed: the user only wants the guide lines.
    
    private void onZoomGroupPressed(MouseEvent event) {
        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        Poi poi = findPoiAt(p.getX(), p.getY());

        if (poi != null) {
            if (onPoiPicked != null) onPoiPicked.accept(poi);
            else showFor(poi);

            event.consume(); // clave: evita el “pan raro”
        }

    }

    private Poi findPoiAt(double px, double py) {
        Poi bestPoi = null;
        double bestDist = Double.MAX_VALUE;

        for (Node child : zoomGroup.getChildren()) {
            if (!(child instanceof Region region)) continue;
            if (!(region.getUserData() instanceof Poi poi)) continue;

            // 1) Hit-test real: si el click cae dentro del marker (pin)
            Bounds bb = region.getBoundsInParent();
            if (bb != null && bb.contains(px, py)) {
                return poi; // hit exacto
            }

            // 2) Fallback por distancia a la posición lógica (punta)
            Point2D pos = poi.getPosition();
            if (pos == null) continue;

            double d = Math.hypot(px - pos.getX(), py - pos.getY());
            if (d <= HIT_TOLERANCE && d < bestDist) {
                bestDist = d;
                bestPoi = poi;
            }
        }
        return bestPoi;
    }

    public void setOnPoiPicked(Consumer<Poi> handler) {
        this.onPoiPicked = handler;
    }

}
