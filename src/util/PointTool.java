package util;

import java.util.Optional;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 *
 * @author Rafael Alonso
 */
public class PointTool implements MapTool {

    private final Group zoomGroup;
    private final ListView<Poi> poiListView;
    private final ObjectProperty<Color> currentColor;

    public PointTool(Group zoomGroup,
                     ListView<Poi> poiListView,
                     ObjectProperty<Color> currentColor) {
        this.zoomGroup = zoomGroup;
        this.poiListView = poiListView;
        this.currentColor = currentColor;
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        Dialog<Poi> poiDialog = new Dialog<>();
            poiDialog.setTitle("Nuevo POI");
            poiDialog.setHeaderText("Introduce un nuevo POI");
            // icono del diálogo
            Stage dialogStage = (Stage) poiDialog.getDialogPane().getScene().getWindow();
            dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));

            ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
            poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

            TextField nameField = new TextField();
            nameField.setPromptText("Nombre del POI");

            TextArea descArea = new TextArea();
            descArea.setPromptText("Descripción...");
            descArea.setWrapText(true);
            descArea.setPrefRowCount(5);

            VBox vbox = new VBox(10, new Label("Nombre:"), nameField,
                                       new Label("Descripción:"), descArea);
            poiDialog.getDialogPane().setContent(vbox);

            poiDialog.setResultConverter(dialogButton -> {
                if (dialogButton == okButton) {
                    return new Poi(nameField.getText().trim(),
                                   descArea.getText().trim(),
                                   0, 0);
                }
                return null;
            });

            Optional<Poi> result = poiDialog.showAndWait();

            if (result.isPresent()) {
                Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
                Poi poi = result.get();
                poi.setPosition(localPoint);
                poi.setColor(currentColor.get()); // color del POI = color actual elegido por el usuario
                poiListView.getItems().add(poi);
            }
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        // Normalmente no hace nada para POI
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // Tampoco hace nada
    }
}
