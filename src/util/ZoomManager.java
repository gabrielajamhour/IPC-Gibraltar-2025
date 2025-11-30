package util;

import static java.lang.Math.clamp;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.ScrollEvent;

/**
 *
 * @author Rafael Alonso
 */
public class ZoomManager {

    private final ScrollPane scrollPane;
    private final Slider slider;
    private Group zoomGroup;
    
    private double currentScale = 1.0;
    private final double minScale = 0.5;
    private final double maxScale = 3.0;

    public ZoomManager(ScrollPane scrollPane, Slider slider) {
        this.scrollPane = scrollPane;
        this.slider = slider;
        init();
    }

    private void init() {
        // Envolver contenido original del ScrollPane
        Group contentGroup = new Group();
        zoomGroup = new Group();

        contentGroup.getChildren().add(zoomGroup);
        zoomGroup.getChildren().add(scrollPane.getContent());
        scrollPane.setContent(contentGroup);

        // Configuración del slider
        slider.setMin(0.1);
        slider.setMax(1.5);
        slider.setValue(0.1);
        slider.valueProperty().addListener((o, oldVal, newVal) ->
                applyZoomCentered((Double) newVal)
        );

        // Zoom con Ctrl + rueda
        scrollPane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                double delta = e.getDeltaY();
                double step = 0.1;

                if (delta > 0) {
                    slider.setValue(Math.min(slider.getMax(), slider.getValue() + step));
                } else if (delta < 0) {
                    slider.setValue(Math.max(slider.getMin(), slider.getValue() - step));
                }
                e.consume();
            }
        });

        applyZoomCentered(slider.getValue());
    }

    public Group getZoomGroup() {
        return zoomGroup;
    }


    /**
     * Aplica el zoom manteniendo fijo el punto que está en el centro
     * del viewport (pantalla), no el centro del mapa completo.
     */
    private void applyZoomCentered(double newScale) {
        if (zoomGroup == null) return;

        Bounds viewportBounds = scrollPane.getViewportBounds();
        Bounds contentBounds  = zoomGroup.getBoundsInLocal();

        double viewportW = viewportBounds.getWidth();
        double viewportH = viewportBounds.getHeight();

        // Si aún no se ha hecho layout, solo aplicamos escala y guardamos
        if (viewportW == 0 || viewportH == 0) {
            zoomGroup.setScaleX(newScale);
            zoomGroup.setScaleY(newScale);
            currentScale = newScale;
            return;
        }

        // --- 1) Punto actual en el centro del viewport (antes del zoom) ---

        double oldScale = currentScale; // escala anterior

        double contentWOld = contentBounds.getWidth()  * oldScale;
        double contentHOld = contentBounds.getHeight() * oldScale;

        double maxXOld = Math.max(contentWOld - viewportW, 0);
        double maxYOld = Math.max(contentHOld - viewportH, 0);

        double hValueOld = scrollPane.getHvalue();
        double vValueOld = scrollPane.getVvalue();

        // Coordenadas (en contenido escalado) de la esquina superior izquierda visible
        double visibleXOld = maxXOld * hValueOld;
        double visibleYOld = maxYOld * vValueOld;

        // Centro del viewport en coordenadas del contenido SIN escalar
        double centerXLocal = (visibleXOld + viewportW / 2.0) / oldScale;
        double centerYLocal = (visibleYOld + viewportH / 2.0) / oldScale;

        // --- 2) Aplicar nueva escala ---

        zoomGroup.setScaleX(newScale);
        zoomGroup.setScaleY(newScale);
        currentScale = newScale;

        double contentWNew = contentBounds.getWidth()  * newScale;
        double contentHNew = contentBounds.getHeight() * newScale;

        double maxXNew = Math.max(contentWNew - viewportW, 0);
        double maxYNew = Math.max(contentHNew - viewportH, 0);

        // Centro del viewport en coordenadas del contenido ESCALADO (nuevo zoom)
        double centerXNewScaled = centerXLocal * newScale;
        double centerYNewScaled = centerYLocal * newScale;

        // Queremos que ese punto siga en el centro del viewport:
        double visibleXNew = centerXNewScaled - viewportW / 2.0;
        double visibleYNew = centerYNewScaled - viewportH / 2.0;

        // Limitar para no salirnos del rango
        visibleXNew = clamp(visibleXNew, 0, maxXNew);
        visibleYNew = clamp(visibleYNew, 0, maxYNew);

        double hValueNew = (maxXNew == 0) ? 0.0 : visibleXNew / maxXNew;
        double vValueNew = (maxYNew == 0) ? 0.0 : visibleYNew / maxYNew;

        scrollPane.setHvalue(hValueNew);
        scrollPane.setVvalue(vValueNew);
    }

}
