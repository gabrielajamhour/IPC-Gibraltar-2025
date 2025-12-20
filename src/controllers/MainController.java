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
import javafx.application.Platform;
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
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Arc;
import javafx.scene.text.Text;
import javafx.stage.Modality;
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
    @FXML    private Slider zoom_slider;
    @FXML    private MenuButton map_pin;
    @FXML    private MenuItem profileButton;
    @FXML    private Button problemsButton;
    @FXML    private Button btnPoint;
    @FXML    private Button btnLine;
    @FXML    private ToggleGroup questionGroup;
    @FXML    private Button btnBorrar;
    @FXML    private Slider sliderGrosor;
    @FXML    private ColorPicker colorPicker;
    @FXML    private Button btnArco;
    @FXML    private Button btnSeleccionar;
    @FXML    private Button randomProblem;
    @FXML    private Label enunciadoProblema;
    @FXML    private Button btnComprobarRespuesta;
    @FXML    private RadioButton tBAlternativaA;
    @FXML    private RadioButton tBAlternativaB;
    @FXML    private RadioButton tBAlternativaC;
    @FXML    private RadioButton tBAlternativaD;
    @FXML    private Button btnTexto;
    @FXML    private MenuButton profileMain;
    @FXML    private Button btnTransportador;
    @FXML    private Button btnRegla;
    @FXML    private Label labelIntrucciones;
    @FXML    private Button btnDistancia;
    @FXML    private Label tituloPuntosMapa;
    @FXML    private Button btnExtremos;
    @FXML    private MenuItem sessionsButton;
    @FXML    private BorderPane pane;
    @FXML    private Button btnBorrarTodo;
    @FXML    private Label tituloProbActual;
    @FXML    private ImageView avatarMain;
    @FXML    private MenuItem resultsButton;
    

    // Tools
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
    
    // la variable zoomGroup se utiliza para dar soporte al zoom
    // el escalado se realiza sobre este nodo, al escalar el Group no mueve sus nodos
    private Group zoomGroup;
    private ZoomManager zoomManager;
    
    private ProblemUtil problemUtil;
    private SettingsUtil settings;
    
    @FXML    private Button toggleDrawerButton;
    @FXML    private VBox panelProblemas;
    @FXML    private ImageView imageView;
    @FXML    private ScrollPane scrollPane;
    @FXML    private VBox problemContainer;
    

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        data = sharedPoiData;
        
        zoomManager = new ZoomManager(scrollPane, zoom_slider);
        zoomGroup   = zoomManager.getZoomGroup();


        // --- HUD overlay para herramientas (NO se escala con el zoom) ---
        // Lo metemos en el StackPane del FXML (scrollPane + controles de zoom) para no perder el slider.
        javafx.scene.layout.Pane toolOverlay = new javafx.scene.layout.Pane();
        toolOverlay.setPickOnBounds(false); // clic en vacío pasa al ScrollPane
        toolOverlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        javafx.scene.Parent parent = scrollPane.getParent();
        if (parent instanceof javafx.scene.layout.StackPane centerStack) {
            int scrollIdx = centerStack.getChildren().indexOf(scrollPane);
            int insertIdx = (scrollIdx >= 0) ? (scrollIdx + 1) : centerStack.getChildren().size();
            centerStack.getChildren().add(insertIdx, toolOverlay);

            toolOverlay.prefWidthProperty().bind(centerStack.widthProperty());
            toolOverlay.prefHeightProperty().bind(centerStack.heightProperty());
        } else if (pane != null) {
            // Fallback por si cambia el FXML
            javafx.scene.layout.StackPane mapStack = new javafx.scene.layout.StackPane(scrollPane, toolOverlay);
            pane.setCenter(mapStack);
        }
        
        // Cada vez que cambie el slider de zoom, avisamos a PointTool
        zoom_slider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (pointTool instanceof util.PointTool pt) {
                pt.onZoomChanged(newVal.doubleValue());
            }
        });
        
        // Color actual = valor del ColorPicker
        colorPicker.setValue(Color.BLACK);
        currentColor.bind(colorPicker.valueProperty());

        // Grosor actual = valor del slider
        sliderGrosor.setValue((sliderGrosor.getMin() + sliderGrosor.getMax()) / 2);
        currentLineWidth.bind(sliderGrosor.valueProperty());
        
        // Crear herramientas
        pointTool = new PointTool(
            zoomGroup,
            data,
            currentColor,
            (Poi poi) -> {
                // SOLO centrar si no hay herramienta activa
                if (currentTool == null) {
                    centerOnPoi(poi);
                }

                // Si extremos está activo, también lo actualizamos
                if (extremosOverlay != null && extremosOverlay.isEnabled()) {
                    extremosOverlay.showFor(poi);
                }
            }
        );
        lineTool  = new LineTool(zoomGroup, lineData, currentLineWidth, currentColor);
        eraserTool = new EraserTool(zoomGroup, data, lineData, arcData, sharedTextData, map_pin);
        selectTool = new SelectTool(zoomGroup, currentColor, currentLineWidth);
        arcTool    = new ArcTool(zoomGroup, arcData, currentLineWidth, currentColor); 
        textTool   = new TextTool(zoomGroup, currentColor, currentLineWidth, sharedTextData);
        distanceTool = new DistanceTool(zoomGroup, currentLineWidth, currentColor);
        extremosOverlay = new ExtremosOverlay(zoomGroup, data);
        
        // Transportador (overlay auxiliar)
        protractorTool = new ProtractorTool(toolOverlay, scrollPane);
        reglaTool = new ReglaTool(toolOverlay, scrollPane);
        
        zoomManager.setBeforeZoomHook((oldS, newS) -> {
            if (reglaTool != null && reglaTool.isVisible()) {
                reglaTool.beforeMapZoomChange();
            }
        });

        zoomManager.setAfterZoomHook((oldS, newS) -> {
            if (reglaTool != null && reglaTool.isVisible()) {
                reglaTool.afterMapZoomChange(newS);
            }
        });
        
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
        
        problemUtil = new ProblemUtil(
            enunciadoProblema,
            tBAlternativaA,
            tBAlternativaB,
            tBAlternativaC,
            tBAlternativaD,
            questionGroup,
            tituloProbActual
        );
        
        btnComprobarRespuesta.disableProperty().bind(questionGroup.selectedToggleProperty().isNull());
        
        settings = SettingsUtil.getInstance();
        updateBackground();
        
        extremosOverlay.setOnPoiPicked(poi -> {
            if (poi == null) return;

            if (currentTool == null) {
                centerOnPoi(poi);
            }
            extremosOverlay.showFor(poi);
        });
        
        panelProblemas.setVisible(false);
        panelProblemas.setManaged(false);
        panelProblemas.setPrefWidth(0);
        
        try {
            problemUtil.loadFirstProblem();
        } catch (NavDAOException ex) {
            System.getLogger(MainController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        
        imageView.setPreserveRatio(true);

        if (imageView.getImage() != null) {
            imageView.setFitWidth(imageView.getImage().getWidth());
            imageView.setFitHeight(imageView.getImage().getHeight());
        }
        
        Platform.runLater(() -> {
            double anchoMapa = imageView.getImage().getWidth();
            double anchoVisible = scrollPane.getViewportBounds().getWidth();

            if (anchoVisible > 0 && anchoMapa > 0) {
                double escalaInicial = anchoVisible / anchoMapa;
                zoom_slider.setValue(escalaInicial); 
            }
        });
        
        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (newBounds.getWidth() > 0) {
                double anchoMapa = imageView.getImage().getWidth();
                double nuevaEscala = newBounds.getWidth() / anchoMapa;
            }
        });
    }
    
    public void setUser(User u) {
        profileMain.setText(" " + u.getNickName());
        avatarMain.setImage(u.getAvatar());
    }
    
    @FXML
    void zoomIn(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() + 0.1);
    }

    @FXML
    void zoomOut(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() - 0.1);
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
    
    
    // MainController.java
    private Poi pickPoiFromEvent(MouseEvent event) {
        Object t = event.getTarget();
        if (!(t instanceof Node node)) return null;

        // Subimos por la jerarquía hasta zoomGroup
        while (node != null && node != zoomGroup) {
            Object ud = node.getUserData();
            if (ud instanceof Poi poi) return poi;
            node = node.getParent();
        }
        return null;
    }

    private void centerOnPoi(Poi poi) {
        if (poi == null || poi.getPosition() == null) return;

        double scale = zoomGroup.getScaleX();
        double contentWLocal = zoomGroup.getBoundsInLocal().getWidth();
        double contentHLocal = zoomGroup.getBoundsInLocal().getHeight();

        double contentW = contentWLocal * scale;
        double contentH = contentHLocal * scale;

        Bounds viewport = scrollPane.getViewportBounds();
        double viewportW = viewport.getWidth();
        double viewportH = viewport.getHeight();

        double x = poi.getPosition().getX() * scale;
        double y = poi.getPosition().getY() * scale;

        double denomW = (contentW - viewportW);
        double denomH = (contentH - viewportH);
        if (denomW <= 0 || denomH <= 0) return;

        double targetH = (x - viewportW / 2) / denomW;
        double targetV = (y - viewportH / 2) / denomH;

        targetH = Math.max(0, Math.min(1, targetH));
        targetV = Math.max(0, Math.min(1, targetV));

        Timeline timeline = new Timeline(
            new KeyFrame(Duration.millis(500),
                new KeyValue(scrollPane.hvalueProperty(), targetH),
                new KeyValue(scrollPane.vvalueProperty(), targetV)
            )
        );
        timeline.play();
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
        openPage("/views/profile.fxml", currentUser);
    }

    @FXML
    private void openProblems(ActionEvent event) {
        User currentUser = SessionManager.getActiveUser();
        openModal("/views/problems.fxml", currentUser);
    }

    @FXML
    private void openSessions(ActionEvent event) {
        User currentUser = SessionManager.getActiveUser();
        openPage("/views/session-history.fxml", currentUser);
    }
    
    @FXML
    private void logout(ActionEvent event) {
        SessionManager.finalizeAndSaveSession();

        // Resetear el “canvas” al cerrar sesión (sin diálogo)
        ClearAll.clearAll(zoomGroup, data, lineData, arcData, sharedTextData, map_pin);

        // opcional: dejar todo consistente
        setCurrentTool(null);

        Stage stage = (Stage) zoom_slider.getScene().getWindow();
        SessionManager.goToLogIn(stage);
    }

    @FXML
    private void openConfig(ActionEvent event) {
        User currentUser = SessionManager.getActiveUser();
        openPage("/views/config.fxml", currentUser);
    }

    private void openPage(String fxmlPath, User userToInject) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Object controller = loader.getController();
        
            if (controller instanceof SessionsController) {
                SessionsController sc = (SessionsController) controller;
                sc.setUser(userToInject);
                sc.setCurrentSession(SessionManager.getCurrentSession());
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
    
    private void openModal(String fxmlPath, User userToInject) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            ProblemsController modalController = loader.getController();
            modalController.setMainController(this);
            modalController.setUser(userToInject);

            // Crear nuevo stage para la ventana modal
            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL);
            modalStage.setScene(new Scene(root));
            modalStage.setResizable(false);

            modalStage.showAndWait();
        } catch (IOException e) {
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

        // Si lo acabamos de activar, redibujar usando el último POI (si existe)
        if (extremosOverlay.isEnabled()) {
            extremosOverlay.redraw();
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
        if (scrollPane != null) {
            scrollPane.setPannable(currentTool == null);
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
        // RMB: desactiva tool y deja que siga funcionando el contexto
        if (event.getButton() == MouseButton.SECONDARY) {
            if (currentTool != null) setCurrentTool(null);
            return;
        }

        // ✅ Si NO hay herramienta activa, y has clicado un POI -> centrar
        if (currentTool == null && event.getButton() == MouseButton.PRIMARY) {
            Poi poi = pickPoiFromEvent(event);
            if (poi != null) {
                centerOnPoi(poi);
                // Si quieres, aquí también podrías refrescar extremos si están activos
                // if (extremosOverlay != null && extremosOverlay.isEnabled()) extremosOverlay.showFor(poi);
                event.consume();
                return;
            }
            // Si no has clicado un POI, no hacemos nada (y el ScrollPane puede panear)
            return;
        }

        // Delegación normal a la herramienta activa
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
        showProblemArea();
    }
    
    public void loadProblem(Problem selected){
        problemUtil.loadProblem(selected);
        problemUtil.updateProblemTitle(selected);
        showProblemArea();
    }
    
    public void showProblemArea() {
        problemContainer.setVisible(true);
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
        } else {
            pane.setStyle("-fx-background-image: url('/styles/background-image.png');");
        }
    }

    @FXML
    private void toggleDrawer(ActionEvent event) {
        boolean isVisible = panelProblemas.isVisible();

        if (isVisible) {
            toggleDrawerButton.setText("▼ Problemas");
            Timeline timeline = new Timeline();
            KeyValue kv = new KeyValue(panelProblemas.prefWidthProperty(), 0.0);
            KeyFrame kf = new KeyFrame(Duration.millis(300), kv);
            timeline.getKeyFrames().add(kf);

            timeline.setOnFinished(e -> {
                panelProblemas.setVisible(false);
                panelProblemas.setManaged(false); // Esto hace que deje de ocupar espacio
            });

            timeline.play();
        } else {
            toggleDrawerButton.setText("▲ Problemas");

            panelProblemas.setManaged(true);
            panelProblemas.setVisible(true);

            Timeline timeline = new Timeline();
            KeyValue kv = new KeyValue(panelProblemas.prefWidthProperty(), 290.0);
            KeyFrame kf = new KeyFrame(Duration.millis(300), kv);
            timeline.getKeyFrames().add(kf);
            timeline.play();
        }
    }
}
