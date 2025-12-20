package util;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

public class ReglaTool {

    private final Pane overlayPane;
    private final ScrollPane scrollPane;

    private final Group reglaGroup;
    private final Region reglaNode;

    private final Circle handleL;
    private final Circle handleR;

    private boolean visible = false;

    // mover regla
    private boolean draggingBody = false;
    private double lastParentX;
    private double lastParentY;

    // gesto "extremos"
    private boolean draggingHandle = false;
    private Circle activeHandle;
    private Circle anchorHandle;
    private Point2D anchorLocal;        // coords locales (reglaGroup)
    private Point2D anchorParentFixed;  // coords del zoomGroup (ancla fija en pantalla)
    private double baseDistLocal = 1.0;
    private double baseAngleLocalDeg = 0.0;

    // tamaño usuario + compensación zoom
    private double userScale = 1.0;
    private double mapCompScale = 1.0;

    private double rotationDeg = 0.0;

    private boolean anchorsComputed = false;

    // Centro VISUAL real (detectado por snapshot)
    private Point2D visualCenterLocal = null; // si null, fallback al centro del Region

    // bloquear panning ScrollPane mientras interactúas
    private boolean pannableLocked = false;
    private boolean prevPannable = true;

    private static final double BASE_SIZE = 350.0;

    private static final double HANDLE_RADIUS = 9.0;
    private static final double HANDLE_OFFSET = 16.0;

    private static final double MIN_USER_SCALE = 0.25;
    private static final double MAX_USER_SCALE = 8.0;

    private static final double DEFAULT_ROTATION = 0.0;

    private final Scale scaleTx;
    private final Rotate rotateTx;

    private Point2D windowAnchorScene = null;

    // true = los handles cambian tamaño+rotación
    // false = solo rotación (tamaño bloqueado)
    private boolean handleScalingEnabled = false;

    

    // Valor por defecto global (configurable desde ConfigController)
    private static boolean defaultHandleScalingEnabled = false;

    public static void setDefaultHandleScalingEnabled(boolean enabled) {
        defaultHandleScalingEnabled = enabled;
    }

    public static boolean isDefaultHandleScalingEnabled() {
        return defaultHandleScalingEnabled;
    }
// radio fijo (en coords del zoomGroup) cuando la escala está bloqueada
    private double pressDistParentFixed = 1.0;
    
    
    public ReglaTool(Pane overlayPane, ScrollPane scrollPane) {
        this.overlayPane = overlayPane;
        this.scrollPane = scrollPane;
        // aplicar el valor global por defecto
        this.handleScalingEnabled = defaultHandleScalingEnabled;


        reglaGroup = new Group();
        reglaNode = new Region();

        reglaNode.setPrefSize(BASE_SIZE, BASE_SIZE);
        reglaNode.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        reglaNode.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        reglaNode.getStyleClass().add("regla");
        String css = getClass().getResource("protractor.css").toExternalForm();
        reglaNode.getStylesheets().add(css);

        reglaNode.setScaleShape(true);
        reglaNode.setCenterShape(true);

        handleL = new Circle(HANDLE_RADIUS);
        handleR = new Circle(HANDLE_RADIUS);
        styleHandle(handleL);
        styleHandle(handleR);

        // posición provisional (luego se recalcula con snapshot)
        handleL.setCenterX(-HANDLE_OFFSET);
        handleL.setCenterY(BASE_SIZE / 2.0);

        handleR.setCenterX(BASE_SIZE + HANDLE_OFFSET);
        handleR.setCenterY(BASE_SIZE / 2.0);

        reglaGroup.getChildren().addAll(reglaNode, handleL, handleR);

        double pivotX = BASE_SIZE / 2.0;
        double pivotY = BASE_SIZE / 2.0;

        scaleTx = new Scale(1.0, 1.0, pivotX, pivotY);
        rotateTx = new Rotate(DEFAULT_ROTATION, pivotX, pivotY);
        reglaGroup.getTransforms().addAll(scaleTx, rotateTx);

        reglaGroup.setVisible(false);
        reglaGroup.setMouseTransparent(false);

        reglaGroup.setTranslateX(300);
        reglaGroup.setTranslateY(200);        overlayPane.getChildren().add(reglaGroup);
        installScrollPanePanningLock();
        installHandlers();
    }

    private void styleHandle(Circle c) {
        c.setFill(Color.rgb(255, 255, 255, 0.85));
        c.setStroke(Color.BLACK);
        c.setStrokeWidth(1.0);
        c.setCursor(Cursor.HAND);
    }

    private Point2D mouseInZoomGroup(MouseEvent e) {
        return overlayPane.sceneToLocal(e.getSceneX(), e.getSceneY());
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double angleDeg(Point2D from, Point2D to) {
        double ang = Math.toDegrees(Math.atan2(to.getY() - from.getY(), to.getX() - from.getX()));
        return (ang % 360.0 + 360.0) % 360.0;
    }

    private void applyTransforms() {
        double total = mapCompScale * userScale;
        scaleTx.setX(total);
        scaleTx.setY(total);
        rotateTx.setAngle(rotationDeg);
    }

    private Point2D regionCenterLocal() {
        return new Point2D(BASE_SIZE / 2.0, BASE_SIZE / 2.0);
    }

    // Centro para centrar/anclar zoom: VISUAL si existe, si no fallback al centro del Region
    private Point2D centerForAnchoringLocal() {
        return (visualCenterLocal != null) ? visualCenterLocal : regionCenterLocal();
    }

    private Point2D handleLocal(Circle h) {
        return new Point2D(h.getCenterX(), h.getCenterY());
    }

    private void moveLocalPointToParent(Point2D localPoint, Point2D desiredParentPoint) {
        Point2D current = reglaGroup.localToParent(localPoint);
        double dx = desiredParentPoint.getX() - current.getX();
        double dy = desiredParentPoint.getY() - current.getY();
        reglaGroup.setTranslateX(reglaGroup.getTranslateX() + dx);
        reglaGroup.setTranslateY(reglaGroup.getTranslateY() + dy);
    }

    /**
     * Cambia pivote de Scale/Rotate sin que se mueva en pantalla el punto fixedLocal.
     */
    private void setPivotKeepingLocalPointFixed(Point2D newPivotLocal, Point2D fixedLocal) {
        Point2D before = reglaGroup.localToParent(fixedLocal);

        scaleTx.setPivotX(newPivotLocal.getX());
        scaleTx.setPivotY(newPivotLocal.getY());
        rotateTx.setPivotX(newPivotLocal.getX());
        rotateTx.setPivotY(newPivotLocal.getY());

        applyTransforms();

        Point2D after = reglaGroup.localToParent(fixedLocal);

        double dx = before.getX() - after.getX();
        double dy = before.getY() - after.getY();
        reglaGroup.setTranslateX(reglaGroup.getTranslateX() + dx);
        reglaGroup.setTranslateY(reglaGroup.getTranslateY() + dy);
    }

    // ====== CLAMPS ======
    private Bounds mapBoundsLocal() {
        // En modo HUD, el "mapa" para la regla es el viewport visible del ScrollPane (coordenadas de pantalla).
        if (scrollPane != null) {
            Bounds vb = scrollPane.getViewportBounds();
            return new BoundingBox(0, 0, Math.max(0, vb.getWidth()), Math.max(0, vb.getHeight()));
        }
        // fallback (por si aún no hay layout)
        Bounds b = overlayPane.getLayoutBounds();
        return new BoundingBox(0, 0, Math.max(0, b.getWidth()), Math.max(0, b.getHeight()));
    }

    private Point2D clampPointToMap(Point2D p, double margin) {
        Bounds m = mapBoundsLocal();
        double minX = m.getMinX() + margin;
        double maxX = m.getMaxX() - margin;
        double minY = m.getMinY() + margin;
        double maxY = m.getMaxY() - margin;

        return new Point2D(
                clamp(p.getX(), minX, maxX),
                clamp(p.getY(), minY, maxY)
        );
    }

    private void clampGroupToMap() {
        Bounds m = mapBoundsLocal();
        double margin = HANDLE_RADIUS + 2.0;

        Bounds b = reglaGroup.getBoundsInParent(); // parent == zoomGroup

        double minX = m.getMinX() + margin;
        double maxX = m.getMaxX() - margin;
        double minY = m.getMinY() + margin;
        double maxY = m.getMaxY() - margin;

        double availableW = Math.max(1.0, maxX - minX);
        double availableH = Math.max(1.0, maxY - minY);

        double dx = 0.0;
        double dy = 0.0;

        if (b.getWidth() <= availableW) {
            if (b.getMinX() < minX) dx = minX - b.getMinX();
            if (b.getMaxX() > maxX) dx = maxX - b.getMaxX();
        } else {
            // si es más grande que el mapa, al menos centramos dentro
            double targetCenterX = minX + availableW / 2.0;
            double curCenterX = b.getMinX() + b.getWidth() / 2.0;
            dx = targetCenterX - curCenterX;
        }

        if (b.getHeight() <= availableH) {
            if (b.getMinY() < minY) dy = minY - b.getMinY();
            if (b.getMaxY() > maxY) dy = maxY - b.getMaxY();
        } else {
            double targetCenterY = minY + availableH / 2.0;
            double curCenterY = b.getMinY() + b.getHeight() / 2.0;
            dy = targetCenterY - curCenterY;
        }

        if (dx != 0.0 || dy != 0.0) {
            reglaGroup.setTranslateX(reglaGroup.getTranslateX() + dx);
            reglaGroup.setTranslateY(reglaGroup.getTranslateY() + dy);
        }
    }
    
    private static final double HARD_MIN_USER_SCALE = 0.05; // para garantizar que siempre pueda “caber”

    private void enforceScaleToFitMap() {
        Bounds map = mapBoundsLocal();
        double margin = HANDLE_RADIUS + 2.0;

        double availW = Math.max(1.0, map.getWidth()  - 2.0 * margin);
        double availH = Math.max(1.0, map.getHeight() - 2.0 * margin);

        Bounds b = reglaGroup.getBoundsInParent(); // coords del zoomGroup

        if (b.getWidth() <= availW && b.getHeight() <= availH) return;

        double factor = Math.min(availW / b.getWidth(), availH / b.getHeight());
        if (factor >= 1.0) return;

        // reducimos userScale lo justo para que quepa (sin depender de MIN_USER_SCALE)
        userScale = clamp(userScale * factor, HARD_MIN_USER_SCALE, MAX_USER_SCALE);
        applyTransforms();
    }

    // ====================

    /**
     * Detecta el bbox "visual" real de la regla (dibujada por CSS) y coloca los handles
     * en los extremos reales, no en el centro del Region.
     */
    private void computeHandlesFromSnapshot() {
        try {
            reglaNode.applyCss();
            reglaNode.layout();

            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);

            WritableImage img = reglaNode.snapshot(sp, null);
            PixelReader pr = img.getPixelReader();
            if (pr == null) return;

            int w = (int) img.getWidth();
            int h = (int) img.getHeight();

            int minX = w, minY = h, maxX = -1, maxY = -1;

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    Color c = pr.getColor(x, y);

                    // "contenido" = no transparente y no casi-blanco
                    if (c.getOpacity() > 0.02 && !(c.getRed() > 0.97 && c.getGreen() > 0.97 && c.getBlue() > 0.97)) {
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (maxX < 0 || maxY < 0) {
                // fallback
                visualCenterLocal = regionCenterLocal();
                handleL.setCenterX(-HANDLE_OFFSET);
                handleL.setCenterY(BASE_SIZE / 2.0);
                handleR.setCenterX(BASE_SIZE + HANDLE_OFFSET);
                handleR.setCenterY(BASE_SIZE / 2.0);
                return;
            }

            double cy = (minY + maxY) / 2.0;

            handleL.setCenterX(minX - HANDLE_OFFSET);
            handleL.setCenterY(cy);

            handleR.setCenterX(maxX + HANDLE_OFFSET);
            handleR.setCenterY(cy);

            handleL.toFront();
            handleR.toFront();

            // ESTE es el centro “real” que debes usar para centrar y para zoom
            visualCenterLocal = new Point2D((minX + maxX) / 2.0, (minY + maxY) / 2.0);

            anchorsComputed = true;
        } catch (Exception ignored) {
        }
    }

    private boolean targetIsInsideRegla(Object target) {
        if (!(target instanceof Node n)) return false;
        Node cur = n;
        while (cur != null) {
            if (cur == reglaGroup) return true;
            cur = cur.getParent();
        }
        return false;
    }

    /**
     * Evita que el ScrollPane "panee" cuando estás manipulando la regla.
     */
    private void installScrollPanePanningLock() {
        if (scrollPane == null) return;

        scrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!visible) return;
            if (!targetIsInsideRegla(e.getTarget())) return;

            if (!pannableLocked) {
                prevPannable = scrollPane.isPannable();
                scrollPane.setPannable(false);
                pannableLocked = true;
            }
        });

        scrollPane.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (!pannableLocked) return;
            scrollPane.setPannable(prevPannable);
            pannableLocked = false;
        });
    }

    private void installHandlers() {
        // mover cuerpo
        reglaNode.setCursor(Cursor.MOVE);

        reglaNode.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (draggingHandle) return;

            draggingBody = true;
            Point2D p = mouseInZoomGroup(e);
            lastParentX = p.getX();
            lastParentY = p.getY();
            e.consume();
        });

        reglaNode.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!draggingBody) return;

            Point2D p = mouseInZoomGroup(e);
            double dx = p.getX() - lastParentX;
            double dy = p.getY() - lastParentY;

            reglaGroup.setTranslateX(reglaGroup.getTranslateX() + dx);
            reglaGroup.setTranslateY(reglaGroup.getTranslateY() + dy);

            clampGroupToMap();

            lastParentX = p.getX();
            lastParentY = p.getY();
            e.consume();
        });

        reglaNode.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> draggingBody = false);

        // ambos handles: rotación + tamaño
        handleL.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> beginHandleDrag(handleL, e));
        handleR.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> beginHandleDrag(handleR, e));

        handleL.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::dragHandle);
        handleR.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::dragHandle);

        handleL.addEventHandler(MouseEvent.MOUSE_RELEASED, this::endHandleDrag);
        handleR.addEventHandler(MouseEvent.MOUSE_RELEASED, this::endHandleDrag);
    }

    private void beginHandleDrag(Circle pressed, MouseEvent e) {
        if (e.getButton() != MouseButton.PRIMARY) return;

        draggingHandle = true;

        activeHandle = pressed;
        anchorHandle = (pressed == handleL) ? handleR : handleL;

        Point2D activeLocal = handleLocal(activeHandle);
        anchorLocal = handleLocal(anchorHandle);

        baseDistLocal = Math.max(1.0, anchorLocal.distance(activeLocal));
        baseAngleLocalDeg = angleDeg(anchorLocal, activeLocal);

        // pivote en el ancla, sin saltos
        setPivotKeepingLocalPointFixed(anchorLocal, anchorLocal);

        // fijamos dónde está el ancla en pantalla (coords del zoomGroup)
        anchorParentFixed = reglaGroup.localToParent(anchorLocal);

        e.consume();
    }

    private void dragHandle(MouseEvent e) {
        if (!draggingHandle) return;

        // CLAMP: el handle activo nunca puede salir del perímetro del mapa
        Point2D mouseParentRaw = mouseInZoomGroup(e);
        Point2D mouseParent = clampPointToMap(mouseParentRaw, HANDLE_RADIUS + 2.0);

        // ángulo deseado (siempre)
        double ang = angleDeg(anchorParentFixed, mouseParent);

        // si NO permitimos escalar, proyectamos el punto del ratón al círculo de radio fijo
        Point2D effectivePoint = mouseParent;
        if (!handleScalingEnabled) {
            double rad = Math.toRadians(ang);
            effectivePoint = new Point2D(
                    anchorParentFixed.getX() + Math.cos(rad) * pressDistParentFixed,
                    anchorParentFixed.getY() + Math.sin(rad) * pressDistParentFixed
            );
            effectivePoint = clampPointToMap(effectivePoint, HANDLE_RADIUS + 2.0);
        }

        double targetAngleParent = angleDeg(anchorParentFixed, effectivePoint);
        double targetDistParent = Math.max(1.0, anchorParentFixed.distance(effectivePoint));

        // rotación siempre
        rotationDeg = targetAngleParent - baseAngleLocalDeg;
        rotationDeg = (rotationDeg % 360.0 + 360.0) % 360.0;

        // escala solo si está permitido
        if (handleScalingEnabled) {
            double desiredTotalScale = targetDistParent / baseDistLocal;
            userScale = desiredTotalScale / Math.max(mapCompScale, 1e-6);
            userScale = clamp(userScale, MIN_USER_SCALE, MAX_USER_SCALE);
        }

        applyTransforms();

        // si escaló, igual puede necesitar ajuste para que no agrande el ScrollPane
        enforceScaleToFitMap();

        // ancla fija
        moveLocalPointToParent(anchorLocal, anchorParentFixed);

        // clamp final
        clampGroupToMap();

        e.consume();
    }

    private void endHandleDrag(MouseEvent e) {
        if (!draggingHandle) return;

        draggingHandle = false;

        // IMPORTANTE: volver a pivote en el CENTRO VISUAL para que el zoom no “ancle” a un lado
        Point2D center = centerForAnchoringLocal();
        setPivotKeepingLocalPointFixed(center, center);

        clampGroupToMap();
        e.consume();
    }

    /**
     * Tu método "MAGIA NEGRA" (lo mantenemos), pero usando el CENTRO VISUAL real.
     */
    // MAGIA NEGRA - NO DOT TOUCH
    public void centerOnViewport() {
        if (overlayPane == null) return;

        Bounds vb = (scrollPane != null) ? scrollPane.getViewportBounds() : overlayPane.getLayoutBounds();
        double viewportW = vb.getWidth();
        double viewportH = vb.getHeight();
        if (viewportW <= 0 || viewportH <= 0) return;

        double centerX = viewportW / 2.0;
        double centerY = viewportH / 2.0;

        Point2D pivot = centerForAnchoringLocal();
        reglaGroup.setTranslateX(centerX - pivot.getX());
        reglaGroup.setTranslateY(centerY - pivot.getY());
        clampGroupToMap();
    }

    private void adjustScaleForCurrentZoom() {
        // Como la regla está en un overlay (no se escala con el mapa),
        // la compensación de zoom se vuelve constante.
        mapCompScale = 0.75;
    }

    // --- API ---

    public boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    // MAGIA NEGRA - NO DOT TOUCH
    public void setVisible(boolean visible) {
        this.visible = visible;
        reglaGroup.setVisible(visible);

        if (!visible) {
            if (pannableLocked && scrollPane != null) {
                scrollPane.setPannable(prevPannable);
                pannableLocked = false;
            }
            return;
        }

        // reset TOTAL al activar
        userScale = 1.0;
        rotationDeg = DEFAULT_ROTATION;

        // pivote inicial al centro (luego lo movemos al centro visual en runLater)
        Point2D c = regionCenterLocal();
        scaleTx.setPivotX(c.getX());
        scaleTx.setPivotY(c.getY());
        rotateTx.setPivotX(c.getX());
        rotateTx.setPivotY(c.getY());

        adjustScaleForCurrentZoom();
        applyTransforms();

        Platform.runLater(() -> {
            if (!anchorsComputed) computeHandlesFromSnapshot();

            // pivot en centro VISUAL (evita “anclaje raro” al hacer zoom)
            Point2D center = centerForAnchoringLocal();
            setPivotKeepingLocalPointFixed(center, center);

            // centrar SIEMPRE al activar (ahora con pivot correcto)
            centerOnViewport();

            reglaGroup.toFront();
        });
    }

    public boolean isVisible() {
        return visible;
    }

    /**
     * Mantener tamaño constante en pantalla al cambiar zoom SIN mover la regla.
     * Anclaje en el CENTRO VISUAL.
     */
    public void onMapZoomChanged(double newMapScale) {
        // Compatibilidad: ya no hace falta ajustar nada con el zoom.
    }
    
    public void setHandleScalingEnabled(boolean enabled) {
        this.handleScalingEnabled = enabled;
    }

    public boolean isHandleScalingEnabled() {
        return handleScalingEnabled;
    }
    
    public void beforeMapZoomChange() {
        // No-op: la regla está anclada a la pantalla, no al mapa.
    }

    public void afterMapZoomChange(double newScale) {
        // No-op: la regla está anclada a la pantalla, no al mapa.
    }

    private void restoreWindowAnchor() {
        // No-op (modo HUD).
    }
}