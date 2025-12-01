package util;

import javafx.application.Platform;
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
    // MAGIA NEGRA - NO DOT TOUCH
    public void centerOnViewport() {
        if (scrollPane == null || zoomGroup == null) return;

        Bounds viewportBounds = scrollPane.getViewportBounds();
        Bounds contentBounds  = zoomGroup.getBoundsInLocal();

        double viewportW = viewportBounds.getWidth();
        double viewportH = viewportBounds.getHeight();

        // Si todavía no hay tamaño de viewport, centramos en el contenido completo
        if (viewportW <= 0 || viewportH <= 0) {
            double centerX = (contentBounds.getMinX() + contentBounds.getMaxX()) / 2.0;
            double centerY = (contentBounds.getMinY() + contentBounds.getMaxY()) / 2.0;

            Bounds nodeBounds = protractorNode.getBoundsInLocal();
            double nodeW = nodeBounds.getWidth();
            double nodeH = nodeBounds.getHeight();

            protractorNode.setTranslateX(centerX - nodeW / 2.0);
            protractorNode.setTranslateY(centerY - nodeH / 2.0);
            return;
        }

        // Escala actual del mapa (la que pone el ZoomManager)
        double scale = zoomGroup.getScaleX();
        if (scale <= 0) scale = 1.0;

        // Tamaño del contenido ya escalado
        double contentWScaled = contentBounds.getWidth()  * scale;
        double contentHScaled = contentBounds.getHeight() * scale;

        double maxX = Math.max(contentWScaled - viewportW, 0);
        double maxY = Math.max(contentHScaled - viewportH, 0);

        // Esquina superior izquierda visible, en coordenadas ESCALADAS
        double visibleXScaled = scrollPane.getHvalue() * maxX;
        double visibleYScaled = scrollPane.getVvalue() * maxY;

        // Centro del viewport en coordenadas LOCALES (no escaladas)
        double centerXLocal = (visibleXScaled + viewportW / 2.0) / scale;
        double centerYLocal = (visibleYScaled + viewportH / 2.0) / scale;

        Bounds nodeBounds = protractorNode.getBoundsInLocal();
        double nodeW = nodeBounds.getWidth();
        double nodeH = nodeBounds.getHeight();

        // Colocamos la regla centrada en ese punto local
        protractorNode.setTranslateX(centerXLocal - nodeW / 2.0);
        protractorNode.setTranslateY(centerYLocal - nodeH / 2.0);
    }


    
    /**
     * Ajusta la escala de la regla en función del zoom actual del mapa,
     * para que el tamaño en pantalla sea razonable al activarla.
     */
        private void adjustScaleForCurrentZoom() {
        double mapScale = zoomGroup.getScaleX(); // o zoomManager.getCurrentScale()
        if (mapScale <= 0) {
            mapScale = 1.0;
        }

        Bounds localBounds = protractorNode.getBoundsInLocal();
        double width = localBounds.getWidth();
        if (width <= 0) {
            return;
        }

        double targetWidthOnScreen = 350; // igual que la regla, por consistencia

        double escalaInterna = targetWidthOnScreen / (width * mapScale);
        zoomScale = escalaInterna;

        protractorNode.setScaleX(baseScaleX * zoomScale);
        protractorNode.setScaleY(baseScaleY * zoomScale);
    }


    // --- API para MainController ---

    public boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    // MAGIA NEGRA - NO DOT TOUCH
    public void setVisible(boolean visible) {
        this.visible = visible;
        protractorNode.setVisible(visible);
        if (visible) {
            adjustScaleForCurrentZoom(); // primero tamaño acorde al zoom actual

            // Esperamos al siguiente pulso de JavaFX para que el ScrollPane tenga bien el viewport
            Platform.runLater(() -> {
                centerOnViewport();   // ahora sí, centramos
                protractorNode.toFront();
            });
        }
    }


    public boolean isVisible() {
        return visible;
    }
}
