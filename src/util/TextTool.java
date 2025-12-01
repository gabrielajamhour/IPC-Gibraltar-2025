package util;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Group;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 * @author Rafael Alonso
 * 
 * Herramienta para añadir texto al mapa.
 * - Click izquierdo: aparece un TextField para escribir.
 * - ENTER o perder el foco: se crea un Text definitivo.
 * El color se toma de currentColor y el tamaño del slider (currentLineWidth).
 */
public class TextTool implements MapTool {

    private final Group zoomGroup;
    private final ObjectProperty<Color> currentColor;
    private final DoubleProperty currentLineWidth;
    private final ObservableList<Text> sharedTextData;

    private TextField activeField = null;

    public TextTool(Group zoomGroup,
                    ObjectProperty<Color> currentColor,
                    DoubleProperty currentLineWidth,
                    ObservableList<Text> sharedTextData) {
        this.zoomGroup = zoomGroup;
        this.currentColor = currentColor;
        this.currentLineWidth = currentLineWidth;
        this.sharedTextData = sharedTextData;
    }

    @Override
    public void onEnter() {
        // Cuando se entra con esta herramienta activa, no hace falta nada especial
    }

    @Override
    public void onExit() {
        // Si hubiera un campo en edición, lo confirmamos o lo quitamos
        if (activeField != null) {
            zoomGroup.getChildren().remove(activeField);
            activeField = null;
        }
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        // Si ya hay un campo en edición, confírmalo antes
        if (activeField != null) {
            commitAndRemoveField();
        }

        activeField = new TextField();
        activeField.setPromptText("Escribe texto...");

        double fontSize = Math.max(8, currentLineWidth.get());
        activeField.setFont(Font.font(fontSize));

        activeField.setLayoutX(event.getX());
        activeField.setLayoutY(event.getY());

        zoomGroup.getChildren().add(activeField);
        activeField.requestFocus();

        activeField.setOnAction(e -> commitAndRemoveField());

        activeField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                commitAndRemoveField();
            }
        });
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        // No necesitamos nada aquí
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // Tampoco
    }

    private void commitAndRemoveField() {
        if (activeField == null) return;

        String textContent = activeField.getText();
        double x = activeField.getLayoutX();
        double y = activeField.getLayoutY();

        zoomGroup.getChildren().remove(activeField);
        activeField = null;

        if (textContent != null && !textContent.isBlank()) {
            createAndStoreText(textContent, x, y);
        }
    }

    private void createAndStoreText(String content, double x, double y) {
        double fontSize = Math.max(8, currentLineWidth.get());
        Color color = currentColor.get();
        if (color == null) color = Color.RED;

        Text text = new Text(content);
        text.setX(x);
        text.setY(y + fontSize);
        text.setFill(color);
        text.setFont(Font.font(fontSize));

        // Menú contextual para borrar
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Eliminar texto");
        deleteItem.setOnAction(ev -> {
            zoomGroup.getChildren().remove(text);
            sharedTextData.remove(text);
        });
        menu.getItems().add(deleteItem);

        text.setOnContextMenuRequested(ev ->
                menu.show(text, ev.getScreenX(), ev.getScreenY())
        );

        // Añadimos al mapa y a la lista compartida
        zoomGroup.getChildren().add(text);
        sharedTextData.add(text);
    }
}
