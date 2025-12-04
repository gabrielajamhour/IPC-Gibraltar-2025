package util;

import java.util.Optional;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 *
 * @author Rafael Alonso
 */
public class PointTool implements MapTool {

    private final Group zoomGroup;               // Contenedor donde está el mapa (y se hace el zoom)
    private final ListView<Poi> poiListView;     // Lista de POIs
    private final ObjectProperty<Color> currentColor;  // Color actual elegido por el usuario

    private int unnamedPoiCounter = 1;
    
    // Tamaño dinámico
    private static boolean dynamicPoiSizeEnabled = false;
    private double poiSizeBaseFactor = 2;
    private static final double DYNAMIC_EXTRA_SCALE = 1.5;
    
    private double lastZoomScale = 0.1;
    private static final double MIN_MAP_SCALE = 0.1; // igual que ZoomManager
    private static final double MAX_PIN_FACTOR = 3.0;
    private static final double MIN_PIN_FACTOR = 0.4;
    private static final double BASE_PIN_WIDTH  = 48;
    private static final double BASE_PIN_HEIGHT = 60;
    
    private static PointTool lastInstance;  // última instancia creada
    
    public PointTool(Group zoomGroup,
                    ListView<Poi> poiListView,
                    ObjectProperty<Color> currentColor) {
        this.zoomGroup   = zoomGroup;
        this.poiListView = poiListView;
        this.currentColor = currentColor;

        // Guarda la última instancia creada para poder forzar un repintado
        lastInstance = this;
        
        // 1) Cada vez que cambie la escala del mapa, recalculamos tamaños
        if (zoomGroup != null) {
            zoomGroup.scaleXProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(this::updatePoiMarkerSizes);
            });
            zoomGroup.scaleYProperty().addListener((obs, oldVal, newVal) -> {
                Platform.runLater(this::updatePoiMarkerSizes);
            });

            // 2) Y por seguridad, si cambia la transformación global (pans, layouts, etc.)
            zoomGroup.localToSceneTransformProperty().addListener((obs, oldT, newT) -> {
                Platform.runLater(this::updatePoiMarkerSizes);
            });
        }
    }

    
    
    // === Configuración global del tamaño dinámico de los POI ===
    public static boolean isDynamicPoiSizeEnabled() {
        return dynamicPoiSizeEnabled;
    }

    public static void setDynamicPoiSizeEnabled(boolean enabled) {
        dynamicPoiSizeEnabled = enabled;

        // Si ya existe una PointTool en el mapa, recalculamos los tamaños
        if (lastInstance != null) {
            lastInstance.updatePoiMarkerSizes();
        }
    }


    /** Llamado desde Main cuando cambia el zoom */
    public void onZoomChanged(double ignoredZoomScale) {
        // sincronizamos con el zoom real del mapa
        this.lastZoomScale = getCurrentMapScale(); // si quieres seguir guardándolo
        updatePoiMarkerSizes();
    }


    /** Inicializa el dibujado: dibuja los que ya existen y escucha la lista */
    public void initPoiDrawing(ObservableList<Poi> data) {
        // 1) Dibujar los que ya están
        for (Poi poi : data) {
            addPoiMarkerToMap(poi);
        }

        // 2) Listener: cada vez que se añada un nuevo POI a la lista, dibujarlo
        data.addListener((ListChangeListener<Poi>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Poi p : change.getAddedSubList()) {
                        addPoiMarkerToMap(p);
                    }
                }
                // POIs eliminados -> quitar marker del mapa
                if (change.wasRemoved()) {
                    for (Poi p : change.getRemoved()) {
                        removePoiMarkerFromMap(p);
                    }
                }
            }
        });
    }
    
    
    /** Crea el marker gráfico de un POI y lo añade al zoomGroup */
    public void addPoiMarkerToMap(Poi poi) {
        if (zoomGroup == null || poi == null || poi.getPosition() == null) return;

        // 1) Crear el nodo gráfico
        Region marker = new Region();
        marker.getStyleClass().add("map-pin");

        // 2) Color del pin según el color del POI
        Color c = poi.getColor();
        if (c == null) {
            c = Color.RED;
        }

        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        String webColor = String.format("#%02X%02X%02X", r, g, b);

        marker.setStyle("-fx-background-color: " + webColor + ";");

        // 3) Vincular el Node con el Poi, para que EraserTool/SelectTool lo puedan encontrar
        marker.setUserData(poi);

        // 5) Añadir al mapa
        zoomGroup.getChildren().add(marker);
        
        // 4) Aplicar tamaño dinámico
        Platform.runLater(() -> applyPoiMarkerScale(marker));
    }
    
    
    // === Tamaño dinámico de los pins ===
    
    private void applyPoiMarkerScale(Node marker) {
        if (!(marker instanceof Region region)) return;

        Object ud = marker.getUserData();
        if (!(ud instanceof Poi poi) || poi.getPosition() == null) return;

        // 1) Calcular factor según zoom / config
        double mapScale = getCurrentMapScale();
        double factor;

        if (dynamicPoiSizeEnabled) {
            // Queremos que en el zoom mínimo el factor sea MAX_PIN_FACTOR
            double base = MAX_PIN_FACTOR * MIN_MAP_SCALE; // 3.0 * 0.1 = 0.3
            factor = base / mapScale;
            factor *= DYNAMIC_EXTRA_SCALE;
        } else {
            factor = poiSizeBaseFactor;
        }

        // Solo límite inferior; el superior ya no hace falta porque nunca
        // superamos MAX_PIN_FACTOR si mapScale >= MIN_MAP_SCALE
        factor = Math.max(MIN_PIN_FACTOR, factor);
        // Si quieres, puedes mantener un clamp suave arriba:
        // factor = Math.max(MIN_PIN_FACTOR, Math.min(factor, MAX_PIN_FACTOR));

        // 2) Nuevo tamaño del pin
        double w = BASE_PIN_WIDTH  * factor;
        double h = BASE_PIN_HEIGHT * factor;

        region.setPrefWidth(w);
        region.setPrefHeight(h);
        region.setMinWidth(Region.USE_PREF_SIZE);
        region.setMinHeight(Region.USE_PREF_SIZE);
        region.setMaxWidth(Region.USE_PREF_SIZE);
        region.setMaxHeight(Region.USE_PREF_SIZE);

        // 3) Recolocar para que la punta siga en el POI
        double x = poi.getPosition().getX();
        double y = poi.getPosition().getY();

        region.setLayoutX(x - w / 2.0); // centrado horizontal
        region.setLayoutY(y - h);       // punta abajo
    }


    //factor = Math.max(0.1, Math.min(factor, 5.5));
    
    private void updatePoiMarkerSizes() {
        if (zoomGroup == null) return;

        for (Node n : zoomGroup.getChildren()) {
            if (n instanceof Region && n.getStyleClass().contains("map-pin")) {
                applyPoiMarkerScale(n);
            }
        }
    }
    
    /** Devuelve el zoom real actual del mapa (zoomGroup). */
    private double getCurrentMapScale() {
        if (zoomGroup == null) return 1.0;
        double sx = zoomGroup.getScaleX();
        if (sx <= 0) sx = 1.0;
        return sx;
    }
    
    /** Elimina del mapa el marker asociado a un POI */
    private void removePoiMarkerFromMap(Poi poi) {
        if (zoomGroup == null || poi == null) return;

        // Eliminamos cualquier Node cuyo userData sea exactamente ese Poi
        zoomGroup.getChildren().removeIf(node -> {
            Object ud = node.getUserData();
            return ud == poi;   // misma referencia de objeto
        });
    }


    @Override
    public void onMousePressed(MouseEvent event) {
        // Solo reaccionamos al botón principal del ratón
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }

        // Convertimos las coordenadas de escena a coordenadas del Group (carta)
        Point2D localPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());
        Color poiColor = currentColor.get();  // Capturamos el color actual en este momento

        // Creamos y configuramos el diálogo
        Dialog<Poi> poiDialog = new Dialog<>();
        poiDialog.setTitle("Nuevo POI");
        poiDialog.setHeaderText("Introduce un nuevo POI");

        // Icono del diálogo (opcional, pero queda bonito)
        // OJO: si tienes problemas aquí, puedes envolver esto en un try/catch
        Stage dialogStage = (Stage) poiDialog.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(
                new Image(getClass().getResourceAsStream("/resources/logo.png"))
        );

        // Botones del diálogo
        ButtonType okButton = new ButtonType("Aceptar", ButtonBar.ButtonData.OK_DONE);
        poiDialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        // Controles del formulario
        TextField nameField = new TextField();
        nameField.setPromptText("Nombre del POI");

        TextArea descArea = new TextArea();
        descArea.setPromptText("Descripción...");
        descArea.setWrapText(true);
        descArea.setPrefRowCount(5);

        // Layout del contenido del diálogo
        VBox vbox = new VBox(
                10,
                new Label("Nombre:"),      nameField,
                new Label("Descripción:"), descArea
        );
        poiDialog.getDialogPane().setContent(vbox);

        // Conversor de resultado:
        // si el usuario pulsa Aceptar, devolvemos un Poi; si no, null.
        poiDialog.setResultConverter(dialogButton -> {
            if (dialogButton == okButton) {
                String name = nameField.getText();
                String desc = descArea.getText();

                // La lógica del nombre por defecto
                if (name != null) {
                    name = name.trim();
                }
                if (desc != null) {
                    desc = desc.trim();
                }

                // Si no hay nombre, ponemos "Punto i"
                if (name == null || name.isEmpty()) {
                    name = "Punto " + unnamedPoiCounter++;
                }

                return new Poi(
                        name,
                        desc,
                        localPoint.getX(),
                        localPoint.getY(),
                        poiColor
                );
            }
            return null;
        });


        // Mostramos el diálogo y esperamos la respuesta del usuario
        Optional<Poi> result = poiDialog.showAndWait();

        // Si el usuario aceptó y se creó un Poi, lo añadimos a la lista
        result.ifPresent(poi -> {
            poiListView.getItems().add(poi); // se añade a la lista
            // NO hace falta llamar aquí a addPoiMarkerToMap(poi),
            // porque initPoiDrawing ya escucha los añadidos y dibuja los markers.
        });
    }

    @Override
    public void onMouseDragged(MouseEvent event) {
    }

    @Override
    public void onMouseReleased(MouseEvent event) {
    }
}
