package util;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;

/**
 * @author Jose Soler 
 */

public class Poi {
    
    private Point2D position;
    private Color color;

    public Poi(double x, double y, Color color) {
        this.position = new Point2D(x, y);
        this.color = color;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D p) {
        this.position = p;
    }

    // === nuevo: getters/setters de color ===
    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
    
    @Override
    public String toString() {
        // Por si alguien hace debug / imprime el POI
        return "POI(" + position.getX() + ", " + position.getY() + ")";
    }
}

    

