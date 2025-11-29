package util;

import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.ImageView;

/**
 *
 * @author Rafael Alonso
 */
public class ClearAll {
    public static void clearAll(Group zoomGroup, ObservableList<?> poiList, Node map_pin) {
        if (zoomGroup != null && zoomGroup.getChildren().size() > 1) {
            zoomGroup.getChildren().remove(1, zoomGroup.getChildren().size());
        }
        if (poiList != null) {
            poiList.clear();
        }
        if (map_pin != null) {
            map_pin.setVisible(false);
        }
    }
}

