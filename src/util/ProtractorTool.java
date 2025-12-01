package util;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Region;

/**
 * @author Rafael Alonso
 * Overlay para el transportador: se muestra/oculta,
 * se mueve con el ratón y se puede escalar.
 */
public class ProtractorTool {

    private final Group zoomGroup;
    private final ScrollPane scrollPane;
    private final Region protractorNode;

    private boolean visible = false;
    private boolean dragging = false;
    private double lastParentX;
    private double lastParentY;
    
    // escala base para corregir orientación (flip, etc.)
    private double baseScaleX = -1.0;
    private double baseScaleY = 1.0;

    // escala de zoom que se modifica con la rueda
    private double zoomScale = 1.0;


    public ProtractorTool(Group zoomGroup, ScrollPane scrollPane) {
        this.zoomGroup = zoomGroup;
        this.scrollPane = scrollPane;

        protractorNode = new Region();

        // Tamaño base (el CSS también marca pref-width/height)
        protractorNode.setPrefSize(350, 350);
        protractorNode.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        protractorNode.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // Clase CSS -> coincide con ".transportador" en protractor.css
        protractorNode.getStyleClass().add("transportador");
        
        protractorNode.setRotate(180);
        
        // aplicar la combinación base + zoom
        protractorNode.setScaleX(baseScaleX * zoomScale);
        protractorNode.setScaleY(baseScaleY * zoomScale);

        // IMPORTANTE: cargar el CSS del transportador
        // (como protractor.css está en el mismo paquete "util" que esta clase)
        String css = getClass().getResource("protractor.css").toExternalForm();
        protractorNode.getStylesheets().add(css);

        // Para que el path del CSS se adapte al tamaño del Region
        protractorNode.setScaleShape(true);
        protractorNode.setCenterShape(true);

        protractorNode.setVisible(false);
        protractorNode.setMouseTransparent(false);

        // Posición inicial (dentro del mapa, ajusta si quieres)
        protractorNode.setTranslateX(300);
        protractorNode.setTranslateY(200);

        zoomGroup.getChildren().add(protractorNode);

        installHandlers();
    }

    private void installHandlers() {
        // Empezar arrastre
        protractorNode.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;

            dragging = true;

            // Pasar de coords locales del nodo a coords del padre
            var parentPoint = protractorNode.localToParent(e.getX(), e.getY());
            lastParentX = parentPoint.getX();
            lastParentY = parentPoint.getY();

            e.consume(); // que no vaya a la herramienta de dibujo
        });

        // Arrastrar siguiendo al ratón (sin "ir lento" con el zoom)
        protractorNode.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!dragging) return;

            var parentPoint = protractorNode.localToParent(e.getX(), e.getY());
            double parentX = parentPoint.getX();
            double parentY = parentPoint.getY();

            double dx = parentX - lastParentX;
            double dy = parentY - lastParentY;

            protractorNode.setTranslateX(protractorNode.getTranslateX() + dx);
            protractorNode.setTranslateY(protractorNode.getTranslateY() + dy);

            lastParentX = parentX;
            lastParentY = parentY;

            e.consume();
        });

        protractorNode.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            dragging = false;
        });

        // Scroll sobre el transportador = cambiar tamaño
        protractorNode.addEventHandler(ScrollEvent.SCROLL, e -> {
            double delta = e.getDeltaY();
            double step = (delta > 0) ? 0.15 : -0.15;

            zoomScale = Math.max(0.1, Math.min(8.0, zoomScale + step));

            // aplicamos orientación base * escala de zoom
            protractorNode.setScaleX(baseScaleX * zoomScale);
            protractorNode.setScaleY(baseScaleY * zoomScale);

            e.consume();
        });
    }
    
    /**
     * Coloca el transportador en el centro de la parte visible del mapa,
     * teniendo en cuenta zoom y transformaciones.
     */
    public void centerOnViewport() {
        if (scrollPane == null) return;

        // Tamaño del viewport del ScrollPane
        Bounds viewportBounds = scrollPane.getViewportBounds();
        double viewportCenterX = viewportBounds.getWidth() / 2.0;
        double viewportCenterY = viewportBounds.getHeight() / 2.0;

        // Centro del viewport en coordenadas de escena
        Point2D centerInScene = scrollPane.localToScene(viewportCenterX, viewportCenterY);

        // Centro del viewport convertido a coordenadas del zoomGroup (contenido del mapa)
        Point2D centerInZoom = zoomGroup.sceneToLocal(centerInScene);

        double nodeW = protractorNode.getPrefWidth();
        double nodeH = protractorNode.getPrefHeight();

        // Colocamos el transportador con su centro en ese punto
        protractorNode.setTranslateX(centerInZoom.getX() - nodeW / 2.0);
        protractorNode.setTranslateY(centerInZoom.getY() - nodeH / 2.0);
    }


    // --- API para MainController ---

    public boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        protractorNode.setVisible(visible);
        if (visible) {
            centerOnViewport();
            protractorNode.toFront();
        }
    }

    public boolean isVisible() {
        return visible;
    }
}
