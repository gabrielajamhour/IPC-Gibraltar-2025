package util;

import java.util.function.Consumer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Arc;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/**
 *
 * @author Rafael Alonso
 */
public class SelectTool implements MapTool {

    private static final double HIT_TOLERANCE = 10.0; // píxeles
    private static final double HIT_TOLERANCE_POI = 40.0;  // similar a MAX_DISTANCE_POI en EraserTool

    private final Group zoomGroup;
    private final ObjectProperty<Color> currentColor;
    private final DoubleProperty currentLineWidth;
    
    // Callback para instrucciones dinámicas
    private Consumer<String> instructionUpdater;

    public SelectTool(Group zoomGroup,
                      ObjectProperty<Color> currentColor,
                      DoubleProperty currentLineWidth) {
        this.zoomGroup = zoomGroup;
        this.currentColor = currentColor;
        this.currentLineWidth = currentLineWidth;
    }
    
    // Setter para registrar el callback de instrucciones
    public void setInstructionUpdater(Consumer<String> instructionUpdater) {
        this.instructionUpdater = instructionUpdater;
    }

    @Override
    public void onEnter() {
        if (zoomGroup != null) {
            zoomGroup.setCursor(Cursor.HAND);
        }
        // Instrucción inicial al activar la herramienta de selección
        updateInstruction("Primero selecciona las nuevas\nconfiguraciones del elemento a seleccionar");
    }

    @Override
    public void onExit() {
        if (zoomGroup != null) {
            zoomGroup.setCursor(Cursor.DEFAULT);
        }
        // Al salir, limpiamos el texto de instrucciones
        updateInstruction("");
    }
    
    // ============ INSTRUCCIONES DINÁMICAS ============

    private void updateInstruction(String text) {
        if (instructionUpdater != null) {
            instructionUpdater.accept(text);
        }
    }

    @Override
    public void onMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        // Convertimos la posición del ratón a coordenadas del zoomGroup
        Point2D p = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        double px = p.getX();
        double py = p.getY();

        Node nearest = null;
        double bestDist = Double.MAX_VALUE;

        for (Node child : zoomGroup.getChildren()) {
            // 1) Líneas
            if (child instanceof Line line) {
                double d = distancePointToSegment(
                        px, py,
                        line.getStartX(), line.getStartY(),
                        line.getEndX(), line.getEndY()
                );

                // Tolerancia dinámica: base + mitad del grosor de la línea
                double stroke = line.getStrokeWidth();
                double tolerance = HIT_TOLERANCE + stroke / 2.0;

                if (d < bestDist && d <= tolerance) {
                    bestDist = d;
                    nearest = line;
                }
            }
            // 2) Arcos / círculos (Arc)
            else if (child instanceof Arc arc) {
                double d = distancePointToArc(px, py, arc);

                double stroke = arc.getStrokeWidth();
                double tolerance = HIT_TOLERANCE + stroke / 2.0;

                if (d < bestDist && d <= tolerance) {
                    bestDist = d;
                    nearest = arc;
                }
            }
            // 3) Texto (nodos javafx.scene.text.Text)
            else if (child instanceof Text textNode) {
                double d = distancePointToText(px, py, textNode);
                if (d < bestDist && d <= HIT_TOLERANCE) {
                    bestDist = d;
                    nearest = textNode;
                }
            }
            
            // 4) Pins de POI (usamos la misma lógica de distancia que el EraserTool)
            else if (child instanceof Region region && region.getUserData() instanceof Poi poi) {

                // La posición del POI está en las mismas coords que usamos para dibujarlo
                Point2D poiPos = poi.getPosition();
                double d = Math.hypot(px - poiPos.getX(), py - poiPos.getY());

                if (d < bestDist && d <= HIT_TOLERANCE_POI) {
                    bestDist = d;
                    nearest = region;
                }
            }
        }

        if (nearest != null) {
            applyStyleToNode(nearest);
            event.consume();
        }
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
        // Nada por ahora
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
        // Nada por ahora
    }

    // Distancia de un punto (px,py) a un segmento (x1,y1)-(x2,y2)
    private double distancePointToSegment(double px, double py,
                                          double x1, double y1,
                                          double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;

        if (dx == 0 && dy == 0) {
            // el segmento es en realidad un punto
            return Math.hypot(px - x1, py - y1);
        }

        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        t = Math.max(0, Math.min(1, t));

        double projX = x1 + t * dx;
        double projY = y1 + t * dy;

        return Math.hypot(px - projX, py - projY);
    }

    private double distancePointToArc(double px, double py, Arc arc) {
        double cx = arc.getCenterX();
        double cy = arc.getCenterY();

        // radio aproximado (en tu ArcTool usas radioX = radioY)
        double r = (arc.getRadiusX() + arc.getRadiusY()) / 2.0;
        if (r <= 0) return Double.MAX_VALUE;

        // ¿Está el punto dentro del rango angular del arco?
        if (!isPointOnArcAngle(px, py, arc)) {
            return Double.MAX_VALUE;
        }

        double distCenter = Math.hypot(px - cx, py - cy);
        return Math.abs(distCenter - r); // distancia a la circunferencia
    }

    private boolean isPointOnArcAngle(double px, double py, Arc arc) {
        double cx = arc.getCenterX();
        double cy = arc.getCenterY();

        double anglePoint = pointToAngleDeg(cx, cy, px, py);

        double start = normalizeAngle(arc.getStartAngle());
        double length = arc.getLength();      // negativo = horario, positivo = antihorario
        double sweep = Math.abs(length);

        // Pequeño margen angular para que no sea demasiado estricto
        final double ANGLE_TOL = 2.0;

        if (sweep < 1e-3) {
            // arco degenerado, comprobamos solo que estamos cerca del ángulo de inicio
            double diff = angleDiff(start, anglePoint);
            return diff <= ANGLE_TOL;
        }

        if (length >= 0) {
            // Barrido antihorario desde start → anglePoint
            double diffCCW = positiveAngleDiff(start, anglePoint);
            return diffCCW <= sweep + ANGLE_TOL;
        } else {
            // Barrido horario desde start → anglePoint
            // equivale a barrer antihorario desde anglePoint → start
            double diffCW = positiveAngleDiff(anglePoint, start);
            return diffCW <= sweep + ANGLE_TOL;
        }
    }

    // Distancia del punto (px, py) al rectángulo del texto,
    // teniendo en cuenta su tamaño (bounding box + margen según font-size).
    private double distancePointToText(double px, double py, Text text) {
        Bounds b = text.getBoundsInParent();

        // Margen adicional proporcional al tamaño de la fuente
        double fontSize = (text.getFont() != null) ? text.getFont().getSize() : 0.0;
        double margin = Math.max(3.0, fontSize * 0.25); // puedes ajustar este factor

        double minX = b.getMinX() - margin;
        double maxX = b.getMaxX() + margin;
        double minY = b.getMinY() - margin;
        double maxY = b.getMaxY() + margin;

        // Si el clic está dentro del rectángulo inflado, consideramos distancia 0
        if (px >= minX && px <= maxX && py >= minY && py <= maxY) {
            return 0.0;
        }

        // Si está fuera, calculamos la distancia mínima al borde del rectángulo
        double dx = 0.0;
        if (px < minX)      dx = minX - px;
        else if (px > maxX) dx = px - maxX;

        double dy = 0.0;
        if (py < minY)      dy = minY - py;
        else if (py > maxY) dy = py - maxY;

        return Math.hypot(dx, dy);
    }
    

    // ==== Utilidades de ángulos (copiadas de ArcTool) ====

    private double pointToAngleDeg(double cx, double cy, double x, double y) {
        double dx = x - cx;
        double dy = y - cy;

        // ejes de pantalla: Y hacia abajo
        double angleRad = Math.atan2(-dy, dx); // 0° = derecha, antihorario positivo
        double angleDeg = Math.toDegrees(angleRad);
        return normalizeAngle(angleDeg);
    }

    private double normalizeAngle(double angleDeg) {
        double a = angleDeg % 360.0;
        if (a < 0) a += 360.0;
        return a;
    }

    /**
     * Diferencia positiva [0,360) de giro antihorario desde start → end.
     */
    private double positiveAngleDiff(double startDeg, double endDeg) {
        double s = normalizeAngle(startDeg);
        double e = normalizeAngle(endDeg);
        double diff = e - s;
        if (diff < 0) diff += 360.0;
        return diff;
    }

    /**
     * Módulo de la diferencia angular mínima entre dos ángulos.
     */
    private double angleDiff(double a, double b) {
        double na = normalizeAngle(a);
        double nb = normalizeAngle(b);
        double diff = Math.abs(na - nb);
        return (diff > 180.0) ? 360.0 - diff : diff;
    }

    // ==== Aplicar estilo a la marca seleccionada ====

    private void applyStyleToNode(Node n) {
        Color c = currentColor.get();
        if (c == null) c = Color.RED;

        if (n instanceof Line line) {
            line.setStroke(c);
            double width = currentLineWidth.get();
            if (width > 0) {
                line.setStrokeWidth(width);
            }

        } else if (n instanceof Arc arc) {
            arc.setStroke(c);
            double width = currentLineWidth.get();
            if (width > 0) {
                arc.setStrokeWidth(width);
            }
            
        } else if (n instanceof Text text) {
            // Color del texto
            text.setFill(c);
            text.setStroke(c);

            // Usamos sliderGrosor como "tamaño de fuente"
            double fontSize = Math.max(8, currentLineWidth.get());
            text.setFont(Font.font(fontSize));

        } else if (n instanceof Region marker) {
            Object ud = marker.getUserData();
            if (ud instanceof Poi poi) {
                poi.setColor(c);
            }

            int r = (int) Math.round(c.getRed() * 255);
            int g = (int) Math.round(c.getGreen() * 255);
            int b = (int) Math.round(c.getBlue() * 255);
            String webColor = String.format("#%02X%02X%02X", r, g, b);

            marker.setStyle("-fx-background-color: " + webColor + ";");
        }
    }
}
