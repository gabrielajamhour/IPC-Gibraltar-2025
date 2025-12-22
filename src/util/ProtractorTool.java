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

public class ProtractorTool {

    private final Pane overlayPane;
    private final ScrollPane scrollPane;

    private final Group protractorGroup;
    private final Region protractorNode;

    private final Circle handleL;
    private final Circle handleR;

    private boolean visible = false;

    // mover cuerpo
    private boolean draggingBody = false;
    private double lastParentX;
    private double lastParentY;

    // handles
    private boolean draggingHandle = false;
    private Circle activeHandle = null;

    // ancla (pivot) y referencias para drag
    private Point2D anchorLocal = null;          // en coords locales del grupo
    private Point2D anchorParentFixed = null;    // en coords del "mapa HUD" (viewport)
    private double pressDistParentFixed = 0.0;   // distancia fija en parent (para bloquear escala)
    private double pressAngleDeg = 0.0;          // angulo inicial

    // estado transformaciones
    private double userScale = 1.0;
    private double rotationDeg = DEFAULT_ROTATION;

    // “compensación” por zoom del mapa (en HUD la dejamos a 1.0)
    private double mapCompScale = 1.0;

    private static final double BASE_SIZE = 350.0;

    private static final double HANDLE_RADIUS = 9.0;
    private static final double HANDLE_OFFSET = 16.0;

    private static final double MIN_USER_SCALE = 0.25;
    private static final double MAX_USER_SCALE = 8.0;
    private static final double HARD_MIN_USER_SCALE = 0.05; // para garantizar que siempre pueda “caber”

    // Tu transportador ya arrancaba con 180º
    private static final double DEFAULT_ROTATION = 180.0;

    private final Scale scaleTx;
    private final Rotate rotateTx;

    // Centro VISUAL real (detectado por snapshot)
    private Point2D visualCenterLocal = null;
    private boolean anchorsComputed = false;

    // bloqueo pannable del ScrollPane al interactuar con el tool
    private boolean pannableLocked = false;
    private boolean prevPannable = true;

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

    public ProtractorTool(Pane overlayPane, ScrollPane scrollPane) {
        this.overlayPane = overlayPane;
        this.scrollPane = scrollPane;

        // aplicar el valor global por defecto
        this.handleScalingEnabled = defaultHandleScalingEnabled;

        protractorGroup = new Group();
        protractorNode = new Region();

        protractorNode.setPrefSize(BASE_SIZE, BASE_SIZE);
        protractorNode.setMinSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
        protractorNode.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        protractorNode.getStyleClass().add("transportador");
        String css = getClass().getResource("protractor.css").toExternalForm();
        protractorNode.getStylesheets().add(css);

        protractorNode.setScaleShape(true);
        protractorNode.setCenterShape(true);

        // Mantener la misma orientación que tu versión anterior del transportador
        // (el SVG/imagen del CSS suele venir "al revés")
        protractorNode.setScaleX(-1.0);
        protractorNode.setScaleY(1.0);

        handleL = new Circle(HANDLE_RADIUS);
        handleR = new Circle(HANDLE_RADIUS);
        styleHandle(handleL);
        styleHandle(handleR);

        // posición provisional (luego se recalcula con snapshot)
        handleL.setCenterX(-HANDLE_OFFSET);
        handleL.setCenterY(BASE_SIZE / 2.0);

        handleR.setCenterX(BASE_SIZE + HANDLE_OFFSET);
        handleR.setCenterY(BASE_SIZE / 2.0);

        protractorGroup.getChildren().addAll(protractorNode, handleL, handleR);

        double pivotX = BASE_SIZE / 2.0;
        double pivotY = BASE_SIZE / 2.0;

        scaleTx = new Scale(1.0, 1.0, pivotX, pivotY);
        rotateTx = new Rotate(DEFAULT_ROTATION, pivotX, pivotY);
        protractorGroup.getTransforms().addAll(scaleTx, rotateTx);

        protractorGroup.setVisible(false);
        protractorGroup.setMouseTransparent(false);

        protractorGroup.setTranslateX(300);
        protractorGroup.setTranslateY(200);
        overlayPane.getChildren().add(protractorGroup);

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

    private void applyTransforms() {
        double total = mapCompScale * userScale;
        scaleTx.setX(total);
        scaleTx.setY(total);
        rotateTx.setAngle(rotationDeg);
    }

    private Point2D regionCenterLocal() {
        return new Point2D(BASE_SIZE / 2.0, BASE_SIZE / 2.0);
    }

    // Centro para centrar/anclar: VISUAL si existe, si no fallback al centro del Region
    private Point2D centerForAnchoringLocal() {
        return (visualCenterLocal != null) ? visualCenterLocal : regionCenterLocal();
    }

    private Point2D handleLocal(Circle h) {
        return new Point2D(h.getCenterX(), h.getCenterY());
    }

    private void moveLocalPointToParent(Point2D localPoint, Point2D desiredParentPoint) {
        Point2D current = protractorGroup.localToParent(localPoint);
        double dx = desiredParentPoint.getX() - current.getX();
        double dy = desiredParentPoint.getY() - current.getY();
        protractorGroup.setTranslateX(protractorGroup.getTranslateX() + dx);
        protractorGroup.setTranslateY(protractorGroup.getTranslateY() + dy);
    }

    /**
     * Cambia pivote de Scale/Rotate sin que se mueva en pantalla el punto fixedLocal
     */
    private void setPivotKeepingLocalPointFixed(Point2D newPivotLocal, Point2D fixedLocal) {
        Point2D before = protractorGroup.localToParent(fixedLocal);

        scaleTx.setPivotX(newPivotLocal.getX());
        scaleTx.setPivotY(newPivotLocal.getY());
        rotateTx.setPivotX(newPivotLocal.getX());
        rotateTx.setPivotY(newPivotLocal.getY());

        applyTransforms();

        Point2D after = protractorGroup.localToParent(fixedLocal);

        double dx = before.getX() - after.getX();
        double dy = before.getY() - after.getY();
        protractorGroup.setTranslateX(protractorGroup.getTranslateX() + dx);
        protractorGroup.setTranslateY(protractorGroup.getTranslateY() + dy);
    }

    private Bounds mapBoundsLocal() {
        // En modo HUD, el "mapa" para el tool es el viewport visible del ScrollPane (coordenadas de pantalla).
        if (scrollPane != null) {
            Bounds vb = scrollPane.getViewportBounds();
            return new BoundingBox(0, 0, Math.max(0, vb.getWidth()), Math.max(0, vb.getHeight()));
        }
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

        Bounds b = protractorGroup.getBoundsInParent();

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
            else if (b.getMaxX() > maxX) dx = maxX - b.getMaxX();
        } else {
            dx = (minX + availableW / 2.0) - (b.getMinX() + b.getWidth() / 2.0);
        }

        if (b.getHeight() <= availableH) {
            if (b.getMinY() < minY) dy = minY - b.getMinY();
            else if (b.getMaxY() > maxY) dy = maxY - b.getMaxY();
        } else {
            dy = (minY + availableH / 2.0) - (b.getMinY() + b.getHeight() / 2.0);
        }

        if (dx != 0.0 || dy != 0.0) {
            protractorGroup.setTranslateX(protractorGroup.getTranslateX() + dx);
            protractorGroup.setTranslateY(protractorGroup.getTranslateY() + dy);
        }
    }

    private void enforceScaleToFitMap() {
        Bounds map = mapBoundsLocal();
        double margin = HANDLE_RADIUS + 2.0;

        double availW = Math.max(1.0, map.getWidth() - 2.0 * margin);
        double availH = Math.max(1.0, map.getHeight() - 2.0 * margin);

        Bounds b = protractorGroup.getBoundsInParent();

        if (b.getWidth() <= availW && b.getHeight() <= availH) return;

        double factorW = availW / Math.max(1.0, b.getWidth());
        double factorH = availH / Math.max(1.0, b.getHeight());
        double factor = Math.min(factorW, factorH);

        if (factor >= 1.0) return;

        // reducimos userScale lo justo para que quepa (sin depender de MIN_USER_SCALE)
        userScale = clamp(userScale * factor, HARD_MIN_USER_SCALE, MAX_USER_SCALE);
        applyTransforms();
    }

    /**
     * Detecta el bbox "visual" real (dibujada por CSS) y coloca los handles
     * en los extremos reales, no en el centro del Region
     */
    private void computeHandlesFromSnapshot() {
        try {
            protractorNode.applyCss();
            protractorNode.layout();

            SnapshotParameters sp = new SnapshotParameters();
            sp.setFill(Color.TRANSPARENT);

            WritableImage img = protractorNode.snapshot(sp, null);
            PixelReader pr = img.getPixelReader();
            if (pr == null) return;

            int w = (int) img.getWidth();
            int h = (int) img.getHeight();

            int minX = w, minY = h, maxX = -1, maxY = -1;

            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int a = (pr.getArgb(x, y) >>> 24) & 0xff;
                    if (a > 10) {
                        if (x < minX) minX = x;
                        if (y < minY) minY = y;
                        if (x > maxX) maxX = x;
                        if (y > maxY) maxY = y;
                    }
                }
            }

            if (maxX < 0 || maxY < 0) {
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

            visualCenterLocal = new Point2D((minX + maxX) / 2.0, (minY + maxY) / 2.0);
            anchorsComputed = true;
        } catch (Exception ignored) {
        }
    }

    private boolean targetIsInsideProtractor(Object target) {
        if (!(target instanceof Node n)) return false;
        Node cur = n;
        while (cur != null) {
            if (cur == protractorGroup) return true;
            cur = cur.getParent();
        }
        return false;
    }

    private void installScrollPanePanningLock() {
        if (scrollPane == null) return;

        scrollPane.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!visible) return;
            if (!targetIsInsideProtractor(e.getTarget())) return;

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
        protractorNode.setCursor(Cursor.MOVE);

        protractorNode.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (draggingHandle) return;

            draggingBody = true;
            Point2D p = mouseInZoomGroup(e);
            lastParentX = p.getX();
            lastParentY = p.getY();
            e.consume();
        });

        protractorNode.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!draggingBody) return;

            Point2D p = mouseInZoomGroup(e);
            double dx = p.getX() - lastParentX;
            double dy = p.getY() - lastParentY;

            protractorGroup.setTranslateX(protractorGroup.getTranslateX() + dx);
            protractorGroup.setTranslateY(protractorGroup.getTranslateY() + dy);

            clampGroupToMap();

            lastParentX = p.getX();
            lastParentY = p.getY();
            e.consume();
        });

        protractorNode.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> draggingBody = false);

        // ambos handles: rotación + tamaño
        handleL.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> beginHandleDrag(handleL, e));
        handleR.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> beginHandleDrag(handleR, e));

        handleL.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::dragHandle);
        handleR.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::dragHandle);

        handleL.addEventHandler(MouseEvent.MOUSE_RELEASED, this::endHandleDrag);
        handleR.addEventHandler(MouseEvent.MOUSE_RELEASED, this::endHandleDrag);
    }

    private void beginHandleDrag(Circle h, MouseEvent e) {
        if (!visible) return;
        if (e.getButton() != MouseButton.PRIMARY) return;

        draggingHandle = true;
        activeHandle = h;

        // el otro handle será el ANCLA
        Circle other = (h == handleL) ? handleR : handleL;
        anchorLocal = handleLocal(other);

        // pivote en el ancla, sin saltos
        setPivotKeepingLocalPointFixed(anchorLocal, anchorLocal);

        // fijamos dónde está el ancla en pantalla (coords del HUD/viewport)
        anchorParentFixed = protractorGroup.localToParent(anchorLocal);

        // distancia fija inicial (para bloqueo de escala)
        Point2D mouseParent = clampPointToMap(mouseInZoomGroup(e), HANDLE_RADIUS + 2.0);
        pressDistParentFixed = distance(anchorParentFixed, mouseParent);

        // ángulo inicial relativo
        pressAngleDeg = angleDeg(anchorParentFixed, mouseParent);

        e.consume();
    }

    private void dragHandle(MouseEvent e) {
        if (!draggingHandle) return;

        // CLAMP: el handle activo nunca puede salir del perímetro del viewport
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

        // 1) ROTACIÓN: actualizar rotationDeg para que la recta ancla->handle tenga ese ángulo
        double deltaRot = normalizeAngle(ang - pressAngleDeg);
        rotationDeg = normalizeAngleDegrees(rotationDeg + deltaRot);
        pressAngleDeg = ang; // “acumular” sin saltos
        applyTransforms();

        // 2) ESCALA (si procede): mantener el handle activo en effectivePoint
        if (handleScalingEnabled) {
            // Queremos que: protractorGroup.localToParent(activeHandleLocal) == effectivePoint
            Point2D activeLocal = handleLocal(activeHandle);
            Point2D currentParent = protractorGroup.localToParent(activeLocal);
            double currentDist = distance(anchorParentFixed, currentParent);
            double desiredDist = distance(anchorParentFixed, effectivePoint);

            double ratio = (currentDist <= 1e-6) ? 1.0 : (desiredDist / currentDist);
            userScale = clamp(userScale * ratio, MIN_USER_SCALE, MAX_USER_SCALE);

            applyTransforms();

            // tras escalar, volvemos a fijar el handle activo exactamente en effectivePoint
            Point2D afterParent = protractorGroup.localToParent(activeLocal);
            Point2D fix = new Point2D(
                    protractorGroup.getTranslateX() + (effectivePoint.getX() - afterParent.getX()),
                    protractorGroup.getTranslateY() + (effectivePoint.getY() - afterParent.getY())
            );
            protractorGroup.setTranslateX(fix.getX());
            protractorGroup.setTranslateY(fix.getY());
        } else {
            // Solo rotación: reposicionar el handle activo al punto proyectado
            Point2D activeLocal = handleLocal(activeHandle);
            moveLocalPointToParent(activeLocal, effectivePoint);
        }

        // evitar que quede más grande que el viewport y evitar que se "pierda"
        enforceScaleToFitMap();
        clampGroupToMap();

        e.consume();
    }

    private void endHandleDrag(MouseEvent e) {
        draggingHandle = false;
        activeHandle = null;
        anchorLocal = null;
        anchorParentFixed = null;
        e.consume();
    }

    public void centerOnViewport() {
        if (overlayPane == null) return;

        Bounds vb = (scrollPane != null) ? scrollPane.getViewportBounds() : overlayPane.getLayoutBounds();
        double viewportW = vb.getWidth();
        double viewportH = vb.getHeight();
        if (viewportW <= 0 || viewportH <= 0) return;

        double centerX = viewportW / 2.0;
        double centerY = viewportH / 2.0;

        Point2D pivot = centerForAnchoringLocal();
        protractorGroup.setTranslateX(centerX - pivot.getX());
        protractorGroup.setTranslateY(centerY - pivot.getY());
        clampGroupToMap();
    }

    public boolean toggleVisible() {
        setVisible(!visible);
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
        protractorGroup.setVisible(visible);

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

        // HUD: no depende del zoom del mapa
        mapCompScale = 1.0;
        applyTransforms();

        Platform.runLater(() -> {
            if (!anchorsComputed) computeHandlesFromSnapshot();

            // pivot en centro VISUAL (evita “anclaje raro”)
            Point2D center = centerForAnchoringLocal();
            setPivotKeepingLocalPointFixed(center, center);

            // centrar SIEMPRE al activar (ahora con pivot correcto)
            centerOnViewport();
            protractorGroup.toFront();
        });
    }

    public boolean isVisible() {
        return visible;
    }

    public void setHandleScalingEnabled(boolean enabled) {
        this.handleScalingEnabled = enabled;
    }

    public boolean isHandleScalingEnabled() {
        return handleScalingEnabled;
    }

    // ====== helpers ======

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    private static double distance(Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return Math.hypot(dx, dy);
    }

    private static double angleDeg(Point2D a, Point2D b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return Math.toDegrees(Math.atan2(dy, dx));
    }

    private static double normalizeAngle(double deg) {
        // devuelve delta en [-180, 180]
        double d = deg % 360.0;
        if (d > 180.0) d -= 360.0;
        if (d < -180.0) d += 360.0;
        return d;
    }

    private static double normalizeAngleDegrees(double deg) {
        double d = deg % 360.0;
        if (d < 0) d += 360.0;
        return d;
    }
}
