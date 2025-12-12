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
public class ReglaTool {

    private final Group zoomGroup;
    private final ScrollPane scrollPane;
    private final Region reglaNode;

    private boolean visible = false;
    private boolean dragging = false;
    private double lastParentX;
    private double lastParentY;
    
    // escala base para corregir orientación (flip, etc.)
    private double baseScaleX = 1.0;
    private double baseScaleY = 1.0;

    // escala de zoom que se modifica con la rueda
    private double zoomScale = 1.0;
    
    // rotación acumulada (grados)
    private double rotationDeg = DEFAULT_ROTATION;
    private static final double DEFAULT_ROTATION = 0.0;

    
    public ReglaTool(Group zoomGroup, ScrollPane scrollPane) {
        this.zoomGroup = zoomGroup;
        this.scrollPane = scrollPane;

        reglaNode = new Region();

        // Tamaño base (el CSS también marca pref-width/height)
        reglaNode.setPrefSize(350, 350);
        reglaNode.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        reglaNode.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        // Clase CSS -> coincide con ".transportador" en protractor.css
        reglaNode.getStyleClass().add("regla");
        
        // aplicar la combinación base + zoom
        reglaNode.setScaleX(baseScaleX * zoomScale);
        reglaNode.setScaleY(baseScaleY * zoomScale);

        // IMPORTANTE: cargar el CSS del transportador
        // (como protractor.css está en el mismo paquete "util" que esta clase)
        String css = getClass().getResource("protractor.css").toExternalForm();
        reglaNode.getStylesheets().add(css);

        // Para que el path del CSS se adapte al tamaño del Region
        reglaNode.setScaleShape(true);
        reglaNode.setCenterShape(true);

        reglaNode.setVisible(false);
        reglaNode.setMouseTransparent(false);

        // Posición inicial (dentro del mapa, ajusta si quieres)
        reglaNode.setTranslateX(300);
        reglaNode.setTranslateY(200);

        zoomGroup.getChildren().add(reglaNode);

        installHandlers();
    }

    private void installHandlers() {
        // Empezar arrastre
        reglaNode.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;

            dragging = true;

            // Pasar de coords locales del nodo a coords del padre
            var parentPoint = reglaNode.localToParent(e.getX(), e.getY());
            lastParentX = parentPoint.getX();
            lastParentY = parentPoint.getY();

            e.consume(); // que no vaya a la herramienta de dibujo
        });

        // Arrastrar siguiendo al ratón (sin "ir lento" con el zoom)
        reglaNode.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!dragging) return;

            var parentPoint = reglaNode.localToParent(e.getX(), e.getY());
            double parentX = parentPoint.getX();
            double parentY = parentPoint.getY();

            double dx = parentX - lastParentX;
            double dy = parentY - lastParentY;

            reglaNode.setTranslateX(reglaNode.getTranslateX() + dx);
            reglaNode.setTranslateY(reglaNode.getTranslateY() + dy);

            lastParentX = parentX;
            lastParentY = parentY;

            e.consume();
        });

        reglaNode.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            dragging = false;
        });

        // Scroll sobre la regla:
        //  - sin modificadores -> cambiar tamaño
        //  - con SHIFT -> rotar
        reglaNode.addEventFilter(ScrollEvent.SCROLL, e -> {
            // con SHIFT, muchas veces deltaY = 0 y el scroll viene en deltaX
            double delta = e.getDeltaY();
            if (delta == 0) delta = e.getDeltaX();

            if (e.isShiftDown()) {
                // aunque delta sea 0, consumimos para evitar que el ScrollPane haga scroll horizontal
                if (delta != 0) {
                    double rotStep = (delta > 0) ? 5.0 : -5.0;
                    rotationDeg = (rotationDeg + rotStep) % 360.0;
                    reglaNode.setRotate(rotationDeg);
                }
                e.consume();
                return;
            }

            // scroll normal -> escala
            if (delta != 0) {
                double step = (delta > 0) ? 0.15 : -0.15;
                zoomScale = Math.max(0.1, Math.min(8.0, zoomScale + step));
                reglaNode.setScaleX(baseScaleX * zoomScale);
                reglaNode.setScaleY(baseScaleY * zoomScale);
            }
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

            Bounds nodeBounds = reglaNode.getBoundsInLocal();
            double nodeW = nodeBounds.getWidth();
            double nodeH = nodeBounds.getHeight();

            reglaNode.setTranslateX(centerX - nodeW / 2.0);
            reglaNode.setTranslateY(centerY - nodeH / 2.0);
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

        Bounds nodeBounds = reglaNode.getBoundsInLocal();
        double nodeW = nodeBounds.getWidth();
        double nodeH = nodeBounds.getHeight();

        // Colocamos la regla centrada en ese punto local
        reglaNode.setTranslateX(centerXLocal - nodeW / 2.0);
        reglaNode.setTranslateY(centerYLocal - nodeH / 2.0);
    }

    
    /**
     * Ajusta la escala de la regla en función del zoom actual del mapa,
     * para que el tamaño en pantalla sea razonable al activarla.
     */
    private void adjustScaleForCurrentZoom() {
        // Escala que está aplicando el ZoomManager al mapa
        double mapScale = zoomGroup.getScaleX();

        if (mapScale <= 0) {
            mapScale = 1.0; // por seguridad
        }

        // Queremos que la regla salga con un tamaño "fijo" en pantalla:
        // ancho en píxeles ≈ prefWidth (350). Como la regla está dentro
        // de zoomGroup, en pantalla mide:
        //   prefWidth * mapScale * zoomScale
        // Si ponemos zoomScale = 1 / mapScale, el tamaño en pantalla
        // queda ≈ prefWidth.
        zoomScale = 0.75 / mapScale;

        // Aplicamos orientación base * escala de zoom calculada
        reglaNode.setScaleX(baseScaleX * zoomScale);
        reglaNode.setScaleY(baseScaleY * zoomScale);
    }


    // --- API para MainController ---

    public boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    // MAGIA NEGRA - NO DOT TOUCH
    public void setVisible(boolean visible) {
        this.visible = visible;
        reglaNode.setVisible(visible);
        if (visible) {
            adjustScaleForCurrentZoom(); // primero tamaño acorde al zoom actual
            
            // Poner a la rotación default
            rotationDeg = DEFAULT_ROTATION;
            reglaNode.setRotate(rotationDeg);
            
            // Esperamos al siguiente pulso de JavaFX para que el ScrollPane tenga bien el viewport
            Platform.runLater(() -> {
                centerOnViewport();   // ahora sí, centramos
                reglaNode.toFront();
            });
        }
    }


    public boolean isVisible() {
        return visible;
    }
}
