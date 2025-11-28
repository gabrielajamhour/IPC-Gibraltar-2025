package util;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

/**
 *
 * @author Rafael Alonso
 */
public class LineTool implements MapTool {

    private final Group zoomGroup;
    private final DoubleProperty currentLineWidth;
    private final ObjectProperty<Color> currentColor;

    private Line tempLine;
    private Point2D lineStart;

    public LineTool(Group zoomGroup,
                    DoubleProperty currentLineWidth,
                    ObjectProperty<Color> currentColor) {
        this.zoomGroup = zoomGroup;
        this.currentLineWidth = currentLineWidth;
        this.currentColor = currentColor;
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) return;

        Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());

        lineStart = localPoint;
        tempLine = new Line();
        tempLine.setStartX(lineStart.getX());
        tempLine.setStartY(lineStart.getY());
        tempLine.setEndX(lineStart.getX());
        tempLine.setEndY(lineStart.getY());

        tempLine.setStroke(currentColor.get());
        tempLine.setStrokeWidth(currentLineWidth.get());

        zoomGroup.getChildren().add(tempLine);
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        if (tempLine == null || !event.isPrimaryButtonDown()) return;

        Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        tempLine.setEndX(localPoint.getX());
        tempLine.setEndY(localPoint.getY());
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        if (tempLine == null) return;

        // Aquí puedes decidir si la línea es demasiado corta y eliminarla
        tempLine = null;
        lineStart = null;
    }
}
