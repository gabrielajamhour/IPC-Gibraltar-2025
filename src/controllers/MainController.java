package controllers;

import util.Poi;
import util.PointTool;
import util.LineTool;
import util.EraserTool;
import util.MapTool;
import util.ZoomManager;
import util.ClearAll;

import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Bounds;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import java.io.IOException;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;

/**
 * MAIN CONTROLLER
 * =================
 * Este é o controlador principal do aplicativo, que gerencia:
 *  - Interface principal
 *  - Mapa com zoom e POIs
 *  - Ferramentas de desenho (ponto, linha, borracha)
 *  - Navegação entre páginas (Profile, Problems, Results)
 */
public class MainController implements Initializable {

    // ======================================
    // ZOOM & MAPA
    // O Group que será escalado para zoom
    private Group zoomGroup;
    private ZoomManager zoomManager;

    // Lista de POIs compartilhada
    private static final ObservableList<Poi> sharedPoiData = FXCollections.observableArrayList();
    private ObservableList<Poi> data;

    // Ferramentas de desenho
    private MapTool currentTool;
    private MapTool pointTool;
    private MapTool lineTool;
    private MapTool eraserTool;

    // Propriedades compartilhadas
    private final ObjectProperty<Color> currentColor = new SimpleObjectProperty<>(Color.RED);
    private final DoubleProperty currentLineWidth = new SimpleDoubleProperty(2.0);

    // HashMap para acessar rapidamente os POIs por chave
    private final HashMap<String, Poi> hm = new HashMap<>();

    // ======================================
    // FXML ELEMENTS
    @FXML private ListView<Poi> map_listview;
    @FXML private ScrollPane map_scrollpane;
    @FXML private Slider zoom_slider;
    @FXML private MenuButton map_pin;
    @FXML private MenuItem pin_info;
    @FXML private Label mousePosition;
    @FXML private ImageView mapImageView;
    @FXML private Pane mapCanvasPane;

    // Botões de navegação
    @FXML private Button profileButton;
    @FXML private Button problemsButton;
    @FXML private Button resultsButton;

    // Toolbar de ferramentas
    @FXML private Button btnPoint;
    @FXML private Button btnLine;
    @FXML private Button btnBorrar;
    @FXML private Button btnBorrarTodo;
    @FXML private Slider sliderGrosor;
    @FXML private ColorPicker colorPicker;

    // Toggle group das perguntas (exemplo de quiz)
    @FXML private ToggleGroup questionGroup;

    // ======================================
    // INICIALIZAÇÃO
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initData();                 // Inicializa lista de POIs
        initZoomManager();           // Configura scroll e zoom
        initTools();                 // Cria ferramentas de desenho
        setupMouseEvents();          // Registra eventos do mouse
        setupInitialZoom();
        mousePosition.setText("X: " + 0 + "    Y: " + 0);
    }

    /**
     * Inicializa os dados da lista de POIs
     */
    private void initData() {
        data = sharedPoiData;
        map_listview.setItems(data);

        // POI inicial (somente se a lista estiver vazia)
        if (data.isEmpty()) {
            Poi p1 = new Poi("Teste", "Test del POI", 1000, 1000);
            p1.setColor(Color.RED);
            data.add(p1);
        }
    }

    /**
     * Configura o ZoomManager e a Group que será escalada
     */
    private void initZoomManager() {
        zoomManager = new ZoomManager(map_scrollpane, zoom_slider);
        zoomGroup = zoomManager.getZoomGroup();
        
        zoomGroup.setScaleX(0.1);
        zoomGroup.setScaleY(0.1);
    }

    /**
     * Cria as ferramentas e liga às propriedades compartilhadas
     */
    private void initTools() {
        // Bind de propriedades
        currentColor.bind(colorPicker.valueProperty());
        currentLineWidth.bind(sliderGrosor.valueProperty());

        // Ferramentas de desenho
        pointTool  = new PointTool(zoomGroup, map_listview, currentColor);
        lineTool   = new LineTool(zoomGroup, currentLineWidth, currentColor);
        eraserTool = new EraserTool(zoomGroup, map_listview);

        // Ferramenta inicial (nenhuma ativa)
        setCurrentTool(null);
    }

    /**
     * Registra eventos de mouse no mapa
     */
    private void setupMouseEvents() {
        zoomGroup.addEventFilter(MouseEvent.MOUSE_PRESSED,  this::onMapPressed);
        zoomGroup.addEventFilter(MouseEvent.MOUSE_DRAGGED,  this::onMapDragged);
        zoomGroup.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMapReleased);
    }
    
    private void setupInitialZoom() {
        // Usa Platform.runLater para garantir que o layout do ScrollPane já tenha sido calculado.
        javafx.application.Platform.runLater(() -> {

            if (mapImageView == null || mapImageView.getImage() == null || zoomGroup == null) {
                System.err.println("Erro: Componentes de mapa não carregados corretamente.");
                return;
            }

            // 1. Obter dimensões reais da imagem
            double imageWidth = mapImageView.getImage().getWidth();
            double imageHeight = mapImageView.getImage().getHeight();

            // 2. Obter dimensões do Viewport (área visível do ScrollPane)
            Bounds viewport = map_scrollpane.getViewportBounds();
            double viewportWidth = viewport.getWidth();
            double viewportHeight = viewport.getHeight();

            if (viewportWidth == 0 || viewportHeight == 0) return;

            // 3. Calcular o fator de escala (o menor valor garante que toda a imagem caiba)
            double scaleX = viewportWidth / imageWidth;
            double scaleY = viewportHeight / imageHeight;
            double initialScale = Math.min(scaleX, scaleY);

            // 4. Aplicar o zoom ao zoomGroup (o alvo correto para a escala)
            zoomGroup.setScaleX(initialScale);
            zoomGroup.setScaleY(initialScale);

            // 5. Ajusta o slider para refletir o novo zoom
            zoom_slider.setValue(initialScale); 

            // Opcional: Centraliza a imagem no ScrollPane
            map_scrollpane.setHvalue(0.5); 
            map_scrollpane.setVvalue(0.5); 
        });
    }

    // ======================================
    // ZOOM
    @FXML
    void zoomIn(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() + 0.1);
    }

    @FXML
    void zoomOut(ActionEvent event) {
        zoom_slider.setValue(zoom_slider.getValue() - 0.1);
    }

    // ======================================
    // POSIÇÃO DO MOUSE
    @FXML
    private void showPosition(MouseEvent event) {
        mousePosition.setText("X: " + (int) event.getX() + "    Y: " + (int) event.getY());
    }

    // ======================================
    // LISTA DE POIs
    @FXML
    void listClicked(MouseEvent event) {
        Poi itemSelected = map_listview.getSelectionModel().getSelectedItem();
        if (itemSelected == null) return;

        // Centralizar POI no ScrollPane
        double scale = zoomGroup.getScaleX();
        double contentW = zoomGroup.getBoundsInLocal().getWidth() * scale;
        double contentH = zoomGroup.getBoundsInLocal().getHeight() * scale;
        Bounds viewport = map_scrollpane.getViewportBounds();
        double viewportW = viewport.getWidth();
        double viewportH = viewport.getHeight();

        double x = itemSelected.getPosition().getX() * scale;
        double y = itemSelected.getPosition().getY() * scale;

        double targetH = Math.max(0, Math.min(1, (x - viewportW / 2) / (contentW - viewportW)));
        double targetV = Math.max(0, Math.min(1, (y - viewportH / 2) / (contentH - viewportH)));

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.millis(500),
                        new KeyValue(map_scrollpane.hvalueProperty(), targetH),
                        new KeyValue(map_scrollpane.vvalueProperty(), targetV)
                )
        );
        timeline.play();

        // Atualiza o pin
        map_pin.setLayoutX(itemSelected.getPosition().getX());
        map_pin.setLayoutY(itemSelected.getPosition().getY());
        pin_info.setText(itemSelected.getDescription());
        map_pin.setVisible(true);
        updateMapPinStyle(itemSelected.getColor());
    }

    private void updateMapPinStyle(Color c) {
        if (c == null) c = Color.RED;
        String webColor = String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
        map_pin.setStyle("-fx-background-color: " + webColor + ";");
    }

    // ======================================
    // NAVEGAÇÃO ENTRE PÁGINAS
    @FXML
    private void openProfile(ActionEvent event) { openPage("/views/profile.fxml", event); }
    @FXML
    private void openProblems(ActionEvent event) { openPage("/views/problemSelection.fxml", event); }
    @FXML
    private void openResults(ActionEvent event) { openPage("/views/results.fxml", event); }

    private void openPage(String fxmlPath, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ======================================
    // FERRAMENTAS DE DESENHO
    @FXML private void activatePointTool() { toggleTool(pointTool); }
    @FXML private void activateLineTool()  { toggleTool(lineTool); }
    @FXML private void activateBorrar(ActionEvent event) { toggleTool(eraserTool); }

    private void toggleTool(MapTool tool) {
        if (currentTool == tool) setCurrentTool(null);
        else setCurrentTool(tool);
    }

    private void setCurrentTool(MapTool newTool) {
        if (currentTool != null) currentTool.onExit();
        currentTool = newTool;
        if (currentTool != null) currentTool.onEnter();
        updateToolButtons();
    }

    private void updateToolButtons() {
        String activeStyle   = "-fx-background-color: #4287f5; -fx-text-fill: white;";
        String inactiveStyle = "";
        if (btnPoint != null) btnPoint.setStyle(currentTool == pointTool ? activeStyle : inactiveStyle);
        if (btnLine  != null) btnLine.setStyle(currentTool == lineTool ? activeStyle : inactiveStyle);
        if (btnBorrar != null) btnBorrar.setStyle(currentTool == eraserTool ? activeStyle : inactiveStyle);

        if (map_scrollpane != null) map_scrollpane.setPannable(currentTool == null);
    }

    @FXML private void activateBorrarTodo(ActionEvent event) {
        boolean borrado = ClearAll.clearAllWithConfirmation(zoomGroup, data, map_pin);
        if (borrado) setCurrentTool(null);
    }

    // ======================================
    // EVENTOS DE MOUSE
    private void onMapPressed(MouseEvent event)  { if (currentTool != null) { currentTool.onMousePressed(event); event.consume(); } }
    private void onMapDragged(MouseEvent event)  { if (currentTool != null) { currentTool.onMouseDragged(event); event.consume(); } }
    private void onMapReleased(MouseEvent event) { if (currentTool != null) { currentTool.onMouseReleased(event); event.consume(); } }
}
