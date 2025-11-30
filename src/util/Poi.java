package util;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;


public class Poi {
    
    private String code;
    private String description;
    private Point2D position;
    private Color color;

    public Poi(String code, String description, double x, double y, Color color) {
        this.code = code;
        this.description = description;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription() {
        this.description = description;
    }


    @Override
    public String toString() {
        return code + ", " + description;
    }
}

    

