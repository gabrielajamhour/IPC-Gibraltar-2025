package controllers;

import util.Poi;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import util.LineTool;
import util.MapTool;
import util.PointTool;

/**
 *
 * @author jsoler
 */
public class MainController implements Initializable {

    // ======================================
    // la variable zoomGroup se utiliza para dar soporte al zoom
    // el escalado se realiza sobre este nodo, al escalar el Group no mueve sus nodos
    private Group zoomGroup;
    private Label mousePosistion;

    @FXML    private ListView<Poi> map_listview;
    @FXML    private ScrollPane map_scrollpane;
    @FXML    private Slider zoom_slider;
    @FXML    private MenuButton map_pin;
    @FXML    private MenuItem pin_info;
    @FXML    private SplitPane splitPane;
    @FXML    private Label mousePosition;
    @FXML    private Button profileButton;
    @FXML    private Button problemsButton;
    @FXML    private Button resultsButton;
    @FXML    private Button btnPoint;
    @FXML    private Button btnLine;
    @FXML    private ToggleGroup questionGroup;
    @FXML    private Button btnBorrar;
    @FXML    private Slider sliderGrosor;
    @FXML    private ColorPicker colorPicker;
    @FXML    private Button btnBorrarTodo;
    
    
    private Line tempLine = null;
    private Point2D lineStart = null;
    
    // En vez de enum Tool, tendremos objetos:
    private MapTool currentTool;
    private MapTool pointTool;
    private MapTool lineTool;
    private MapTool panTool;
    
    // Estados compartidos (color actual, grosor, etc)
    private final ObjectProperty<Color> currentColor = new SimpleObjectProperty<>(Color.RED);
    private final DoubleProperty currentLineWidth = new SimpleDoubleProperty(2.0);

    // hashmap para guardar los puntos de interes POI
    private final HashMap<String, Poi> hm = new HashMap<>();

    // Lista compartida entre instancias del controlador
    private static final ObservableList<Poi> sharedPoiData =
            FXCollections.observableArrayList();

    private ObservableList<Poi> data;

    

    @FXML
    void zoomIn(ActionEvent event) {
        //================================================
        // el incremento del zoom dependerá de los parametros del 
        // slider y del resultado esperado
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal += 0.1);
    }

    @FXML
    void zoomOut(ActionEvent event) {
        double sliderVal = zoom_slider.getValue();
        zoom_slider.setValue(sliderVal + -0.1);
    }
    
    // esta funcion es invocada al cambiar el value del slider zoom_slider
    private void zoom(double scaleValue) {
        //===================================================
        //guardamos los valores del scroll antes del escalado
        double scrollH = map_scrollpane.getHvalue();
        double scrollV = map_scrollpane.getVvalue();
        //===================================================
        // escalamos el zoomGroup en X e Y con el valor de entrada
        zoomGroup.setScaleX(scaleValue);
        zoomGroup.setScaleY(scaleValue);
        //===================================================
        // recuperamos el valor del scroll antes del escalado
        map_scrollpane.setHvalue(scrollH);
        map_scrollpane.setVvalue(scrollV);
    }

    @FXML
    void listClicked(MouseEvent event) {
        Poi itemSelected = map_listview.getSelectionModel().getSelectedItem();

        // Animación del scroll hasta la mousePosistion del item seleccionado
        double mapWidth = zoomGroup.getBoundsInLocal().getWidth();
        double mapHeight = zoomGroup.getBoundsInLocal().getHeight();
        double scrollH = itemSelected.getPosition().getX() / mapWidth;
        double scrollV = itemSelected.getPosition().getY() / mapHeight;
        final Timeline timeline = new Timeline();
        final KeyValue kv1 = new KeyValue(map_scrollpane.hvalueProperty(), scrollH);
        final KeyValue kv2 = new KeyValue(map_scrollpane.vvalueProperty(), scrollV);
        final KeyFrame kf = new KeyFrame(Duration.millis(500), kv1, kv2);
        timeline.getKeyFrames().add(kf);
        timeline.play();

        // movemos el objto map_pin hasta la mousePosistion del POI
//        double pinW = map_pin.getBoundsInLocal().getWidth();
//        double pinH = map_pin.getBoundsInLocal().getHeight();
        map_pin.setLayoutX(itemSelected.getPosition().getX());
        map_pin.setLayoutY(itemSelected.getPosition().getY());
        pin_info.setText(itemSelected.getDescription());
        map_pin.setVisible(true);
        updateMapPinStyle(itemSelected.getColor()); 
    }

    private void initData() {        
        // Usamos la lista compartida
        data = sharedPoiData;
        map_listview.setItems(data);

        // Solo creamos el POI por defecto la primera vez
        if (data.isEmpty()) {
            Poi p1 = new Poi("Teste", "Test del POI", 1000, 1000);
            p1.setColor(Color.RED);
            data.add(p1);
        }
        
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initData();
        //==========================================================
        // inicializamos el slider y enlazamos con el zoom
        zoom_slider.setMin(0.1);
        zoom_slider.setMax(1.5);
        zoom_slider.setValue(0.1);
        zoom_slider.valueProperty().addListener((o, oldVal, newVal) -> zoom((Double) newVal));

        //=========================================================================
        //Envuelva el contenido de scrollpane en un grupo para que 
        //ScrollPane vuelva a calcular las barras de desplazamiento tras el escalado
        Group contentGroup = new Group();
        zoomGroup = new Group();
        contentGroup.getChildren().add(zoomGroup);
        zoomGroup.getChildren().add(map_scrollpane.getContent());
        map_scrollpane.setContent(contentGroup);
        zoom(0.1);
        
        // Zoom con Ctrl + rueda
        map_scrollpane.addEventFilter(ScrollEvent.SCROLL, e -> {
            if (e.isControlDown()) {
                double delta = e.getDeltaY(); // positivo al subir rueda, negativo al bajar

                double step = 0.1; // cuanto cambia el zoom cada “tic”
                if (delta > 0) {
                    zoom_slider.setValue(Math.min(zoom_slider.getMax(), zoom_slider.getValue() + step));
                } else if (delta < 0) {
                    zoom_slider.setValue(Math.max(zoom_slider.getMin(), zoom_slider.getValue() - step));
                }

                e.consume(); // no dejes que el scrollpane se desplace
            }
        });
        
        // Color actual = valor del ColorPicker
        currentColor.bind(colorPicker.valueProperty());

        // Grosor actual = valor del slider
        currentLineWidth.bind(sliderGrosor.valueProperty());
        
        // Crear herramientas
        pointTool = new PointTool(zoomGroup, map_listview, currentColor);
        lineTool  = new LineTool(zoomGroup, currentLineWidth, currentColor);

        // Herramienta por defecto
        setCurrentTool(null); // o panTool si lo tienes

        // Eventos de ratón
        zoomGroup.addEventFilter(MouseEvent.MOUSE_PRESSED,  this::onMapPressed);
        zoomGroup.addEventFilter(MouseEvent.MOUSE_DRAGGED,  this::onMapDragged);
        zoomGroup.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMapReleased);
    }

    private void setCurrentTool(MapTool newTool) {
        if (currentTool != null) {
            currentTool.onExit();
        }
        currentTool = newTool;
        if (currentTool != null) {
            currentTool.onEnter();
        }
        
        // Actualizar visualmente los botones
        updateToolButtons();
    }
    
    
    @FXML
    private void showPosition(MouseEvent event) {
        mousePosistion.setText("sceneX: " + (int) event.getSceneX() + ", sceneY: " + (int) event.getSceneY() + "\n"
                + "         X: " + (int) event.getX() + ",          Y: " + (int) event.getY());
    }

    private void closeApp(ActionEvent event) {
        ((Stage) zoom_slider.getScene().getWindow()).close();
    }

    private void about(ActionEvent event) {
        Alert mensaje = new Alert(Alert.AlertType.INFORMATION);
        // Acceder al Stage del Dialog y cambiar el icono
        Stage dialogStage = (Stage) mensaje.getDialogPane().getScene().getWindow();
        dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        mensaje.setTitle("Acerca de");
        mensaje.setHeaderText("IPC - 2025");
        mensaje.showAndWait();
    }
    
    private void updateMapPinStyle(Color c) {
        if (c == null) {
            c = Color.RED; // por si acaso
        }

        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);

        String webColor = String.format("#%02X%02X%02X", r, g, b);

        // Color de fondo del botón-pin
        map_pin.setStyle("-fx-background-color: " + webColor + ";");
}
    
    @FXML
    private void openProfile(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/profile.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) zoom_slider.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {}
    }

    @FXML
    private void openProblems(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/problemSelection.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) zoom_slider.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {}
    }

    @FXML
    private void openResults(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/results.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) zoom_slider.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {}
    }

    
    // Botones de la toolbar:
    @FXML
    private void activatePointTool() {
        if (currentTool == pointTool) {
            // Si ya estaba activa, la apagamos
            setCurrentTool(null);
        } else {
            setCurrentTool(pointTool);
        }
    }

    @FXML
    private void activateLineTool() {
        if (currentTool == lineTool) {
            // Si ya estaba activa, la apagamos
            setCurrentTool(null);
        } else {
            setCurrentTool(lineTool);
        }
    }

    @FXML
    private void onNoneToolClicked() {
        setCurrentTool(null); // deja solo el pan del ScrollPane
    }
    
    @FXML
    private void activateBorrar(ActionEvent event) {
    }

    @FXML
    private void activateBorrarTodo(ActionEvent event) {
    }

    private void onMapPressed(MouseEvent event) {
        if (currentTool != null) {
            currentTool.onMousePressed(event);
            event.consume();
        }
    }

    private void onMapDragged(MouseEvent event) {
        if (currentTool != null) {
            currentTool.onMouseDragged(event);
            event.consume();
        }
    }

    private void onMapReleased(MouseEvent event) {
        if (currentTool != null) {
            currentTool.onMouseReleased(event);
            event.consume();
        }
    }
    
    private void updateToolButtons() {
        String activeStyle   = "-fx-background-color: #4287f5; -fx-text-fill: white;";
        String inactiveStyle = "";

        // Botón de puntos
        if (btnPoint != null) {
            btnPoint.setStyle(currentTool == pointTool ? activeStyle : inactiveStyle);
        }

        // Botón de líneas
        if (btnLine != null) {
            btnLine.setStyle(currentTool == lineTool ? activeStyle : inactiveStyle);
        }

        // importante: solo dejamos mover el mapa cuando no hay herramienta de dibujo
        if (map_scrollpane != null) {
            map_scrollpane.setPannable(currentTool == null);
        }
        
    }
}