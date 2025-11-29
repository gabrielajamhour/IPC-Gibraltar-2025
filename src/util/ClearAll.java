package util;

import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
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
    
    public static boolean clearAllWithConfirmation(Group zoomGroup, ObservableList<?> poiList, Node map_pin) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar borrado");
        alert.setHeaderText("¿Borrar todas las marcas del mapa?");
        alert.setContentText("Esta acción eliminará todos los puntos y líneas. ¿Deseas continuar?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            clearAll(zoomGroup, poiList, map_pin);
            return true;
        }

        return false;
    }
}


