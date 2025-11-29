package util;

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
                applyZoom((Double) newVal)
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

        applyZoom(slider.getValue());
    }

    public Group getZoomGroup() {
        return zoomGroup;
    }

    private void applyZoom(double scaleValue) {
        double scrollH = scrollPane.getHvalue();
        double scrollV = scrollPane.getVvalue();

        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);

        scrollPane.setHvalue(scrollH);
        scrollPane.setVvalue(scrollV);
    }
}

