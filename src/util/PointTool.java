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

    private final Group zoomGroup;               // Contenedor donde está el mapa (y se hace el zoom)
    private final ListView<Poi> poiListView;     // Lista de POIs
    private final ObjectProperty<Color> currentColor;  // Color actual elegido por el usuario

    private int unnamedPoiCounter = 1;
    
    public PointTool(Group zoomGroup,
                     ListView<Poi> poiListView,
                     ObjectProperty<Color> currentColor) {
        this.zoomGroup   = zoomGroup;
        this.poiListView = poiListView;
        this.currentColor = currentColor;
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        // Solo reaccionamos al botón principal del ratón
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        // Convertimos las coordenadas de escena a coordenadas del Group (carta)
        Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        Color poiColor = currentColor.get();  // Capturamos el color actual en este momento

        // Creamos y configuramos el diálogo
        Dialog<Poi> poiDialog = new Dialog<>();
        poiDialog.setTitle("Nuevo POI");
        poiDialog.setHeaderText("Introduce un nuevo POI");

        // Icono del diálogo (opcional, pero queda bonito)
        // OJO: si tienes problemas aquí, puedes envolver esto en un try/catch
        Stage dialogStage = (Stage) poiDialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        // Botones del diálogo
        ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Controles del formulario
        TextField nameField = new TextField();
        nameField.setPromptText("Nombre del POI");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Descripción...");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(5);

        // Layout del contenido del diálogo
        VBox vbox = new VBox(
                10,
                new Label("Nombre:"),      nameField,
                new Label("Descripción:"), descArea
        );
        poiDialog.getDialogPane().setContent(vbox);

        // Conversor de resultado:
        // si el usuario pulsa Aceptar, devolvemos un Poi; si no, null.
        poiDialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                String name = nameField.getText();
                String desc = descArea.getText();

                // AQUÍ metemos la lógica del nombre por defecto
                if (name != null) {
                    name = name.trim();
                }
                if (desc != null) {
                    desc = desc.trim();
                }

                // Si no hay nombre, ponemos "Punto i"
                if (name == null || name.isEmpty()) {
                    name = "Punto " + unnamedPoiCounter++;
                }

                return new Poi(
                        name,
                        desc,
                        localPoint.getX(),
                        localPoint.getY(),
                        poiColor
                );
            }
            return null;
        });


        // Mostramos el diálogo y esperamos la respuesta del usuario
        Optional<Poi> result = poiDialog.showAndWait();

        // Si el usuario aceptó y se creó un Poi, lo añadimos a la lista
        result.ifPresent(poi -> poiListView.getItems().add(poi));
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        // Para POIs no hacemos nada en el drag
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // Tampoco necesitamos nada especial en el release
    }
}
