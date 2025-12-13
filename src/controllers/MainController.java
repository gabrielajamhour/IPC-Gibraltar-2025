package controllers;

import util.Poi;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ToggleGroup;
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
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.RadioButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Arc;
import javafx.scene.text.Text;
import model.NavDAOException;
import model.Problem;
import model.User;
import util.ArcTool;
import util.PointTool;
import util.ZoomManager;
import util.ClearAll;
import util.DistanceTool;
import util.ExtremosOverlay;
import util.ProblemUtil;
import util.ProtractorTool;
import util.ReglaTool;
import util.SelectTool;
import util.SessionManager;
import util.SettingsUtil;
import util.TextTool;

/**
 * @author Gabriela Rego & Rafael Alonso
 */

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
    @FXML    private Button btnPoint;
    @FXML    private Button btnLine;
    @FXML    private ToggleGroup questionGroup;
    private Button btnBorrar;
    @FXML    private Slider sliderGrosor;
    @FXML    private ColorPicker colorPicker;
    @FXML    private Button btnArco;
    private Button btnSeleccionar;
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
    private Button btnTransportador;
    private Button btnRegla;
    @FXML    private Label labelIntrucciones;
    private Button btnDistancia;
    @FXML    private Label tituloPuntosMapa;
    @FXML    private MenuItem resultsButton1;    
    private Button btnExtremos;
    @FXML    private MenuItem sessionsButton;
    @FXML    private BorderPane pane;

    // En vez de enum Tool, tendremos objetos:
    private MapTool currentTool;
    private MapTool pointTool;
    private MapTool lineTool;
    private MapTool panTool;
    private MapTool eraserTool;
    private MapTool selectTool;
    private MapTool arcTool;
    private MapTool textTool;
    private MapTool distanceTool;
    
    // Transportador de águlos y regla
    private ProtractorTool protractorTool;
    private ReglaTool reglaTool;
    
    // Marcación de extremos (overlay independiente)
    private ExtremosOverlay extremosOverlay;
    
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
    
    @FXML    private Label contadorProblemas;
    @FXML    private Label tituloProbActual;
    private ProblemUtil problemUtil;
    @FXML    private ImageView avatarMain;
    
    private SettingsUtil settings;
    @FXML
    private Label tituloHerramientasDibujo;
    @FXML
    private Label tituloHerramientasMedicion;
    @FXML
    private Button btnDistancia1;
    @FXML
    private Button btnRegla1;
    @FXML
    private Button btnTransportador1;
    @FXML
    private Button btnExtremos1;
    @FXML
    private Label tituloEdicion;
    @FXML
    private Button btnSeleccionar11;
    @FXML
    private Button btnBorrar11;
    @FXML
    private Button btnBorrarTodo11;
    
    

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initData();
        
        zoomManager = new ZoomManager(map_scrollpane, zoom_slider);
        zoomGroup   = zoomManager.getZoomGroup();
        
        // Cada vez que cambie el slider de zoom, avisamos a PointTool
        zoom_slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (pointTool instanceof util.PointTool pt) {
                pt.onZoomChanged(newVal.doubleValue()); // o zoomManager.getCurrentScale()
            }
        });
        
        // Overlay de marcación de extremos (queda dentro del zoomGroup)
        extremosOverlay = new ExtremosOverlay(zoomGroup);
        
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
        distanceTool = new DistanceTool(zoomGroup, currentLineWidth, currentColor);
        
        // Transportador (overlay auxiliar)
        protractorTool = new ProtractorTool(zoomGroup, map_scrollpane);
        reglaTool = new ReglaTool(zoomGroup, map_scrollpane);
        
        // Dibujar todos los elementos
        dibujar();
        
        // Iniciar las intrucciones dinamicas
        iniInstrLabel();
        
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
        
        settings = SettingsUtil.getInstance();
        updateBackground();
    }
    
    public void setUser(User u) {
        profileMain.setText(" " + u.getNickName());
        avatarMain.setImage(u.getAvatar());
    }
    
    private void initData() {        
        // Usamos la lista compartida
        data = sharedPoiData;
        map_listview.setItems(data);
        
        /*
        // Solo creamos el POI por defecto la primera vez
        if (data.isEmpty()) {
            Poi p1 = new Poi("Teste", "Test del POI", 1000, 1000, Color.RED);
            data.add(p1);
        }*/
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
        if (zoomGroup == null) return;

        // Convertimos la posición del ratón (en coordenadas de escena)
        // al sistema de coordenadas del zoomGroup (el mapa)
        Point2D mapPoint = zoomGroup.sceneToLocal(event.getSceneX(), event.getSceneY());

        mousePosition.setText(
            "X: " + (int) mapPoint.getX() + ",   Y: " + (int) mapPoint.getY()
        );
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
        
        // Si están activos los extremos, actualizarlos al POI seleccionado
        if (extremosOverlay != null && extremosOverlay.isEnabled()) {
            extremosOverlay.showFor(itemSelected);
        }
    }
    
    private void dibujar(){
        // Inicializar el dibujado de POIs (esta llamada sustituye a tu antiguo dibujar())
        if (pointTool instanceof PointTool pt) {
            pt.initPoiDrawing(data);
        }
        
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
    
    // Label de instrucciones vacío al inicio
    private void iniInstrLabel(){  
        if (labelIntrucciones != null) {
            labelIntrucciones.setText("");
        }
        ((ArcTool) arcTool).setInstructionUpdater(this::updateInstructionLabel);
        ((SelectTool) selectTool).setInstructionUpdater(this::updateInstructionLabel);
    }
    
    // Método genérico para actualizar el texto de instrucciones
    private void updateInstructionLabel(String text) {
        if (labelIntrucciones == null) return;

        labelIntrucciones.setText(text);

        // Si no hay texto → NO aplicar CSS
        if (text == null || text.trim().isEmpty()) {
            labelIntrucciones.getStyleClass().remove("text-box");
        } 
        // Si hay texto → aplicar CSS (solo una vez)
        else {
            if (!labelIntrucciones.getStyleClass().contains("text-box")) {
                labelIntrucciones.getStyleClass().add("text-box");
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
    private void openSessions(ActionEvent event) {
        User currentUser = SessionManager.getActiveUser();
        openPage("/views/session-history.fxml", event, currentUser);
    }
    
        @FXML
    private void logout(ActionEvent event) {
        SessionManager.finalizeAndSaveSession();
        Stage stage = (Stage) zoom_slider.getScene().getWindow();
        SessionManager.goToLogIn(stage);
    }

    @FXML
    private void openConfig(ActionEvent event) {
        User currentUser = SessionManager.getActiveUser();
        openPage("/views/config.fxml", event, currentUser);
    }

    private void openPage(String fxmlPath, ActionEvent event, User userToInject) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Object controller = loader.getController();
        
            if (controller instanceof SessionsController) {
                ((SessionsController) controller).setUser(userToInject);
            }
            
            if (controller instanceof ConfigController) {
                ((ConfigController) controller).setSettings(SettingsUtil.getInstance());
            }
            
            Stage stage = (Stage) zoom_slider.getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
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
    
    
    @FXML
    private void activateDistanceTool(ActionEvent event) {
        if (currentTool == distanceTool) {
            // Si ya estaba activa, la desactivamos
            setCurrentTool(null);
        } else {
            setCurrentTool(distanceTool);
        }
    }
    
    @FXML
    private void activateExtremos(ActionEvent event) {
        if (extremosOverlay == null) return;

        extremosOverlay.toggle();

        // Si lo acabamos de activar, dibujamos para el POI seleccionado (si lo hay)
        if (extremosOverlay.isEnabled()) {
            Poi selected = map_listview.getSelectionModel().getSelectedItem();
            extremosOverlay.showFor(selected);
        }

        updateToolButtons();
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
        
        // Botón de Distancia
        if (btnDistancia != null) {
            btnDistancia.setStyle(currentTool == distanceTool ? activeStyle : inactiveStyle);
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
        
        // Botón de Extremos: depende de si está habilitado el overlay
        if (btnExtremos != null && extremosOverlay != null) {
            btnExtremos.setStyle(extremosOverlay.isEnabled() ? activeStyle : inactiveStyle);
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
        
        // Limpiar instrucciones al cambiar de herramienta
        updateInstructionLabel("");
        
        if (currentTool != null) {
            currentTool.onEnter();
        }
        
        // Actualizar visualmente los botones
        updateToolButtons();
    }
    

    
    private void onMapPressed(MouseEvent event) {
        // Si es RMB, desactivar herramienta actual y no delegar nada
        if (event.getButton() == MouseButton.SECONDARY) {
            if (currentTool != null) {
                setCurrentTool(null);   // desactiva la herramienta
            }
            // No hacemos consume(), así el contexto (menús de líneas, etc.) sigue funcionando
            return;
        }

        // Solo delegamos el LMB (u otros botones que no sean RMB)
        if (currentTool != null) {
            currentTool.onMousePressed(event);
            event.consume();
        }
    }

    private void onMapDragged(MouseEvent event) {
        // Ignoramos el arrastre con RMB
        if (event.getButton() == MouseButton.SECONDARY) {
            return;
        }

        if (currentTool != null) {
            currentTool.onMouseDragged(event);
            event.consume();
        }
    }

    private void onMapReleased(MouseEvent event) {
        // Ignoramos la suelta con RMB
        if (event.getButton() == MouseButton.SECONDARY) {
            return;
        }

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

    public void setSettings(SettingsUtil settings) {
        this.settings = settings;

        settings.usarColorSolidoProperty()
                .addListener((obs, oldV, newV) -> updateBackground());

        updateBackground();
    }

    private void updateBackground() {
        if (settings.usarColorSolidoProperty().get()) {
            pane.setStyle("-fx-background-color: #dbdbdb;");
            tituloProbActual.setStyle("-fx-text-fill: #246f80;");
            tituloPuntosMapa.setStyle("-fx-text-fill: #246f80;");
            tituloHerramientasDibujo.setStyle("-fx-text-fill: #246f80;");
            tituloHerramientasMedicion.setStyle("-fx-text-fill: #246f80;");
            tituloEdicion.setStyle("-fx-text-fill: #246f80;");
        } else {
            pane.setStyle("-fx-background-image: url('/styles/background-image.png');");
        }
    }
}
