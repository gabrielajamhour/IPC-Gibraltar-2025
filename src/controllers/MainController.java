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
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import javafx.animation.Timeline;
import javafx.beans.property.ObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import util.EraserTool;
import util.LineTool;
import util.MapTool;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.shape.Arc;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import model.Answer;
import model.NavDAOException;
import model.Navigation;
import model.Problem;
import model.User;
import util.ArcTool;
import util.PointTool;
import util.ZoomManager;
import util.ClearAll;
import util.ProblemUtil;
import util.ProtractorTool;
import util.ReglaTool;
import util.SelectTool;
import util.SessionManager;
import util.TextTool;


public class MainController implements Initializable {

    // ======================================
    // la variable zoomGroup se utiliza para dar soporte al zoom
    // el escalado se realiza sobre este nodo, al escalar el Group no mueve sus nodos
    private Group zoomGroup;
    
    @FXML    private ListView<Poi> map_listview;
    @FXML    private ScrollPane map_scrollpane;
    @FXML    private Slider zoom_slider;
    @FXML    private MenuButton map_pin;
    @FXML    private MenuItem pin_info;
    @FXML    private Label mousePosition;
    @FXML    private MenuItem profileButton;
    @FXML    private Button problemsButton;
    @FXML    private MenuItem resultsButton;
    @FXML    private Button btnPoint;
    @FXML    private Button btnLine;
    @FXML    private ToggleGroup questionGroup;
    @FXML    private Button btnBorrar;
    @FXML    private Slider sliderGrosor;
    @FXML    private ColorPicker colorPicker;
    @FXML    private Button btnBorrarTodo;
    @FXML    private Button btnArco;
    @FXML    private Button btnSeleccionar;
    @FXML    private Button randomProblem;
    @FXML    private Label enunciadoProblema;
    @FXML    private Button btnComprobarRespuesta;
    @FXML    private RadioButton tBAlternativaA;
    @FXML    private RadioButton tBAlternativaB;
    @FXML    private RadioButton tBAlternativaC;
    @FXML    private RadioButton tBAlternativaD;
    @FXML    private Label textErrorCompResp;
    @FXML    private Button btnTexto;
    @FXML    private MenuButton profileMain;
    @FXML    private Button btnTransportador;
    @FXML    private Button btnRegla;
    
    // En vez de enum Tool, tendremos objetos:
    private MapTool currentTool;
    private MapTool pointTool;
    private MapTool lineTool;
    private MapTool panTool;
    private MapTool eraserTool;
    private MapTool selectTool;
    private MapTool arcTool;
    private MapTool textTool;
    
    // Transportador de águlos y regla
    private ProtractorTool protractorTool;
    private ReglaTool reglaTool;
    
    // Estados compartidos (color actual, grosor, etc)
    private final ObjectProperty<Color> currentColor = new SimpleObjectProperty<>(Color.RED);
    private final DoubleProperty currentLineWidth = new SimpleDoubleProperty(2.0);

    
    // Lista compartida de líneas para TODA la app (sobrevive a cambiar de escena)
    private static final ObservableList<Line> lineData =
        FXCollections.observableArrayList();
    
    // Curvas (arcos y círculos, todo Arc)
    private static final ObservableList<Arc> arcData =
        FXCollections.observableArrayList();
    
    // Textos compartidos entre instancias del controlador
    private static final ObservableList<Text> sharedTextData =
        FXCollections.observableArrayList();

    // Lista compartida entre instancias del controlador
    private static final ObservableList<Poi> sharedPoiData =
        FXCollections.observableArrayList();

    private ObservableList<Poi> data;
    
    private ZoomManager zoomManager;
    
    private Problem currentProblem;
    private Answer ansAlternativaA;
    private Answer ansAlternativaB;
    private Answer ansAlternativaC;
    private Answer ansAlternativaD;
    private boolean alreadyAnswered = false;
    
    
    @FXML    private Label contadorProblemas;
    @FXML    private Label tituloProbActual;
    private Label problemaActual;
    private ProblemUtil problemUtil;
    
    // Nodo visual que usará el estilo del CSS (transportador)
    private Rectangle protractorNode;
    private boolean protractorVisible = false;
    private double protractorScale = 1.0;
    private double lastProtractorMouseX;
    private double lastProtractorMouseY;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initData();
        
        zoomManager = new ZoomManager(map_scrollpane, zoom_slider);
        zoomGroup   = zoomManager.getZoomGroup();
        
        // Color actual = valor del ColorPicker
        currentColor.bind(colorPicker.valueProperty());

        // Grosor actual = valor del slider
        currentLineWidth.bind(sliderGrosor.valueProperty());
        
        // Crear herramientas
        pointTool = new PointTool(zoomGroup, map_listview, currentColor);
        lineTool  = new LineTool(zoomGroup, lineData, currentLineWidth, currentColor);
        eraserTool = new EraserTool(zoomGroup, map_listview, lineData, arcData, sharedTextData, map_pin);
        selectTool = new SelectTool(zoomGroup, currentColor, currentLineWidth);
        arcTool    = new ArcTool(zoomGroup, arcData, currentLineWidth, currentColor);
        textTool   = new TextTool(zoomGroup, currentColor, currentLineWidth, sharedTextData);
        
        // Transportador (overlay auxiliar)
        protractorTool = new ProtractorTool(zoomGroup, map_scrollpane);
        reglaTool = new ReglaTool(zoomGroup, map_scrollpane);
        
        // Dibujar todos los elementos
        dibujar();
        
        // Herramienta por defecto
        setCurrentTool(null);

        // Eventos de ratón
        zoomGroup.addEventFilter(MouseEvent.MOUSE_PRESSED,  this::onMapPressed);
        zoomGroup.addEventFilter(MouseEvent.MOUSE_DRAGGED,  this::onMapDragged);
        zoomGroup.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMapReleased);
        
        mousePosition.setText("X: " + 0 + ",   Y: " + 0);
        
        problemUtil = new ProblemUtil(
            enunciadoProblema,
            tBAlternativaA,
            tBAlternativaB,
            tBAlternativaC,
            tBAlternativaD,
            textErrorCompResp,
            questionGroup,
            contadorProblemas,
            tituloProbActual
        );
    }
    
    public void setUser(User u) {
        profileMain.setText(" " + u.getNickName());
    }
    
    private void initData() {        
        // Usamos la lista compartida
        data = sharedPoiData;
        map_listview.setItems(data);

        // Solo creamos el POI por defecto la primera vez
        if (data.isEmpty()) {
            Poi p1 = new Poi("Teste", "Test del POI", 1000, 1000, Color.RED);
            data.add(p1);
        }
        
    }
    
    @FXML
    void zoomIn(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() + 0.1);
    }

    @FXML
    void zoomOut(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() - 0.1);
    }
    
    @FXML
    private void showPosition(MouseEvent event) {
        mousePosition.setText("X: " + (int) event.getX() + ",   Y: " + (int) event.getY());
    }

    private void closeApp(ActionEvent event) {
        ((Stage) zoom_slider.getScene().getWindow()).close();
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
    
     // Crea un marcador visual para un POI usando la clase CSS ".map-pin"
    private void addPoiMarkerToMap(Poi poi) {
        if (zoomGroup == null || poi == null || poi.getPosition() == null) return;

        // 1) Crear el nodo gráfico
        Region marker = new Region();
        marker.getStyleClass().add("map-pin"); // usa el estilo de main.css

        double x = poi.getPosition().getX();
        double y = poi.getPosition().getY();

        // Tamaño del pin según el CSS: 48x60
        double pinW = 48;
        double pinH = 60;

        // Colocamos el pin "apoyado" en la posición del POI:
        // centrado horizontalmente y con la punta abajo
        marker.setLayoutX(x - pinW / 2);
        marker.setLayoutY(y - pinH);

        // 2) Color del pin según el color del POI
        Color c = poi.getColor();
        if (c == null) {
            c = Color.RED;
        }

        int r = (int) Math.round(c.getRed() * 255);
        int g = (int) Math.round(c.getGreen() * 255);
        int b = (int) Math.round(c.getBlue() * 255);
        String webColor = String.format("#%02X%02X%02X", r, g, b);

        // Aplicar el color de fondo (puedes sofisticarlo más si quieres respetar el borde negro/blanco)
        marker.setStyle("-fx-background-color: " + webColor + ";");

        // 3) Vincular el Node con el Poi, para que el EraserTool pueda encontrarlo
        marker.setUserData(poi);

        // 4) Añadirlo al mapa
        zoomGroup.getChildren().add(marker);
    }
    
    
    @FXML
    void listClicked(MouseEvent event) {
        Poi itemSelected = map_listview.getSelectionModel().getSelectedItem();
        if (itemSelected == null) return;

        // 1) Datos básicos
        double scale = zoomGroup.getScaleX(); // asumimos zoom uniforme X = Y

        // tamaño del contenido SIN zoom (en coordenadas locales)
        double contentWLocal = zoomGroup.getBoundsInLocal().getWidth();
        double contentHLocal = zoomGroup.getBoundsInLocal().getHeight();

        // tamaño del contenido CON zoom (lo que ve realmente el ScrollPane)
        double contentW = contentWLocal * scale;
        double contentH = contentHLocal * scale;

        // tamaño del viewport (parte visible del ScrollPane)
        Bounds viewport = map_scrollpane.getViewportBounds();
        double viewportW = viewport.getWidth();
        double viewportH = viewport.getHeight();

        // 2) Posición del POI en coordenadas de contenido (con zoom)
        double x = itemSelected.getPosition().getX() * scale;
        double y = itemSelected.getPosition().getY() * scale;

        // 3) Queremos que el POI quede en el centro del viewport
        double targetH = (x - viewportW / 2) / (contentW - viewportW);
        double targetV = (y - viewportH / 2) / (contentH - viewportH);

        // 4) Limitar entre 0 y 1 para que no se salga
        targetH = Math.max(0, Math.min(1, targetH));
        targetV = Math.max(0, Math.min(1, targetV));

        // 5) Animación de scroll
        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(500),
                new KeyValue(map_scrollpane.hvalueProperty(), targetH),
                new KeyValue(map_scrollpane.vvalueProperty(), targetV)
            )
        );
        timeline.play();
    }
    
    private void dibujar(){
        // Dibujar los POIs
        for (Poi poi : data) {
            addPoiMarkerToMap(poi);
        }

        // 2) Cada vez que se añada un nuevo POI a la lista, dibujarlo también
        data.addListener((ListChangeListener<Poi>) change -> {
            while (change.next()) {
                if (change.wasAdded()) {
                    for (Poi p : change.getAddedSubList()) {
                        addPoiMarkerToMap(p);
                    }
                }
            }
        });
        
        // Dibujar las Lineas
        for (Line line : lineData) {
            if (!zoomGroup.getChildren().contains(line)) {
                zoomGroup.getChildren().add(line);
            }
        }
        
        // Dibujar los arcos
        for (Arc arc : arcData) {
            if (!zoomGroup.getChildren().contains(arc)) {
                zoomGroup.getChildren().add(arc);
            }
        }
        
        // Dibujar los textos
        for (Text t : sharedTextData) {
            if (!zoomGroup.getChildren().contains(t)) {
                zoomGroup.getChildren().add(t);
            }
        }
    }
    
    
    // Open pages
    @FXML
    private void openProfile(ActionEvent event) {
        User currentUser = SessionManager.getActiveUser();
        openPage("/views/profile.fxml", event, currentUser);
    }

    @FXML
    private void openProblems(ActionEvent event) {
        User currentUser = SessionManager.getActiveUser();
        openPage("/views/problems.fxml", event, currentUser);
    }

    @FXML
    private void openResults(ActionEvent event) {
        User currentUser = SessionManager.getActiveUser();
        openPage("/views/results.fxml", event, currentUser);
    }

    private void openPage(String fxmlPath, ActionEvent event, User userToInject) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Object controller = loader.getController();
        
            if (controller instanceof ResultsController) {
                ((ResultsController) controller).setUser(userToInject);
            }
            
            Stage stage = (Stage) zoom_slider.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
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
    private void activateBorrar(ActionEvent event) {
        if (currentTool == eraserTool) {
            // si ya está activa, la desactivamos
            setCurrentTool(null);
        } else {
            setCurrentTool(eraserTool);
        }
    }
    
    @FXML
    private void activateBorrarTodo(ActionEvent event) {
        boolean borrado = ClearAll.clearAllWithConfirmation(zoomGroup, data, lineData, arcData, sharedTextData, map_pin);

        if (borrado) {
            setCurrentTool(null);   // solo si el usuario aceptó
        }
    }
    
    @FXML
    private void activateArcoTool(ActionEvent event) {
        if (currentTool == arcTool) {
            // si ya está activa, la desactivamos
            setCurrentTool(null);
        } else {
            setCurrentTool(arcTool);
        }
    }

    @FXML
    private void activateSeleccionarTool(ActionEvent event) {
        if (currentTool == selectTool) {
            // si ya está activa, la desactivamos
            setCurrentTool(null);
        } else {
            setCurrentTool(selectTool);
        }
    }
    
    @FXML
    private void activateTextTool(ActionEvent event) {
        if (currentTool == textTool) {
            // Si ya estaba activa, la desactivamos
            setCurrentTool(null);
        } else {
            setCurrentTool(textTool);
        }
    }
    
    @FXML
    private void activateTransportadorTool(ActionEvent event) {
        if (protractorTool == null) return;

        boolean visible = protractorTool.toggleVisible();

        // Estilo del botón (igual que los demás)
        String activeStyle   = "-fx-background-color: #4287f5; -fx-text-fill: white;";
        String inactiveStyle = "";

        if (btnTransportador != null) {
            btnTransportador.setStyle(visible ? activeStyle : inactiveStyle);
        }

        // IMPORTANTE: NO tocamos currentTool
        // El usuario puede tener LineTool, PointTool, etc. activos y seguir dibujando.
    }
    
    @FXML
    private void activateReglaTool(ActionEvent event) {
        if (reglaTool == null) return;

        boolean visible = reglaTool.toggleVisible();

        // Estilo del botón (igual que los demás)
        String activeStyle   = "-fx-background-color: #4287f5; -fx-text-fill: white;";
        String inactiveStyle = "";

        if (btnRegla != null) {
            btnRegla.setStyle(visible ? activeStyle : inactiveStyle);
        }

        // IMPORTANTE: NO tocamos currentTool
        // El usuario puede tener LineTool, PointTool, etc. activos y seguir dibujando.
    }

    private void onNoneToolClicked() {
            setCurrentTool(null); // deja solo el pan del ScrollPane
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
        
            // Botón de borrar
        if (btnBorrar != null) {
            btnBorrar.setStyle(currentTool == eraserTool ? activeStyle : inactiveStyle);
        }
        
         // Botón de selección
        if (btnSeleccionar != null) {
            btnSeleccionar.setStyle(currentTool == selectTool ? activeStyle : inactiveStyle);
        }
        
        // Botón de arco / círculo
        if (btnArco != null) {
            btnArco.setStyle(currentTool == arcTool ? activeStyle : inactiveStyle);
        }
        
        // Botón de Texto
        if (btnTexto != null) {
            btnTexto.setStyle(currentTool == textTool ? activeStyle : inactiveStyle);
        }
        
        // Botón de Transportador: depende de si está visible el overlay
        if (btnTransportador != null && protractorTool != null) {
            btnTransportador.setStyle(
                protractorTool.isVisible() ? activeStyle : inactiveStyle
            );
        }
        
        // Botón de Regla: depende de si está visible el overlay
        if (btnRegla != null && reglaTool != null) {
            btnRegla.setStyle(
                reglaTool.isVisible() ? activeStyle : inactiveStyle
            );
        }

        // importante: solo dejamos mover el mapa cuando no hay herramienta de dibujo
        if (map_scrollpane != null) {
            map_scrollpane.setPannable(currentTool == null);
        }
        
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
    

    
    // Mouse manager
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

    @FXML
    private void generateRandomProblem(ActionEvent event) throws NavDAOException {
        problemUtil.generateRandomProblem();
    }
    
    public void loadProblem(Problem selected){
        problemUtil.loadProblem(selected);    
    }

    @FXML
    private void comprobarRespuesta(ActionEvent event) {
        problemUtil.comprobarRespuesta();
    }
    
    private void marcarAlternativa(RadioButton rb, String color) {
        rb.setStyle("-fx-background-color: " + color + "; -fx-padding: 5px; -fx-opacity: 1;");
    }
    

    @FXML
    private void logout(ActionEvent event) {
        SessionManager.finalizeAndSaveSession();
        Stage stage = (Stage) zoom_slider.getScene().getWindow();
        SessionManager.goToLogIn(stage);
    }
}
