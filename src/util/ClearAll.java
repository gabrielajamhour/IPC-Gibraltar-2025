package util;

import java.util.Optional;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;

/**
 * @author Rafael Alonso
 * Herramienta para borrar todos los elementos “de dibujo” del mapa
 * (POIs, líneas, arcos, textos), pero sin tocar:
 *  - la imagen de la carta
 *  - la regla
 *  - el transportador
 */
public class ClearAll {

    private ClearAll() {
        // clase de utilidades, no instanciable
    }

    /**
     * Borra todas las marcas del mapa (nodos Circle, Line, Arc, Text) y
     * limpia las listas de datos, pero NO elimina la regla ni el
     * transportador (que son Region) ni la imagen base.
     */
    public static void clearAll(Group zoomGroup,
                                ObservableList<?> poiList,
                                ObservableList<?> lineList,
                                ObservableList<Arc> arcList,
                                ObservableList<Text> textList,
                                Node map_pin) {

        if (zoomGroup != null) {
            // Elimina solo nodos de “marcas”: círculos, líneas, arcos y textos
            zoomGroup.getChildren().removeIf(node ->
                    (node instanceof Circle) ||
                    (node instanceof Line)   ||
                    (node instanceof Arc)    ||
                    (node instanceof Text)
            );
        }

        if (poiList != null) {
            poiList.clear();
        }
        if (lineList != null) {
            lineList.clear();
        }
        if (arcList != null) {
            arcList.clear();
        }
        if (textList != null) {
            textList.clear();
        }
        if (map_pin != null) {
            // ocultamos el pin del mapa
            map_pin.setVisible(false);
        }
    }

    /**
     * Muestra un diálogo de confirmación y, si el usuario acepta, llama a clearAll.
     */
    public static boolean clearAllWithConfirmation(Group zoomGroup,
                                                   ObservableList<?> poiList,
                                                   ObservableList<?> lineList,
                                                   ObservableList<Arc> arcList,
                                                   ObservableList<Text> textList,
                                                   Node map_pin) {

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmar borrado");
        alert.setHeaderText("¿Borrar todas las marcas del mapa?");
        alert.setContentText("Esta acción eliminará todos los puntos, líneas, arcos y textos. ¿Deseas continuar?");

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            clearAll(zoomGroup, poiList, lineList, arcList, textList, map_pin);
            return true;
        }

        return false;
    }
}
