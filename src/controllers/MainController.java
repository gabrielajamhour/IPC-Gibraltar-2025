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
import javafx.scene.shape.Circle;
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
import javafx.scene.control.Toggle;
import javafx.scene.text.Text;
import model.Answer;
import model.NavDAOException;
import model.Navigation;
import model.Problem;
import util.PointTool;
import util.ZoomManager;
import util.ClearAll;
import util.SessionManager;


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
    
    // En vez de enum Tool, tendremos objetos:
    private MapTool currentTool;
    private MapTool pointTool;
    private MapTool lineTool;
    private MapTool panTool;
    private MapTool eraserTool;
    
    // Estados compartidos (color actual, grosor, etc)
    private final ObjectProperty<Color> currentColor = new SimpleObjectProperty<>(Color.RED);
    private final DoubleProperty currentLineWidth = new SimpleDoubleProperty(2.0);

    // hashmap para guardar los puntos de interes POI
    private final HashMap<String, Poi> hm = new HashMap<>();

    // Lista compartida entre instancias del controlador
    private static final ObservableList<Poi> sharedPoiData =
            FXCollections.observableArrayList();

    private ObservableList<Poi> data;
    
    private ZoomManager zoomManager;
    @FXML
    private Button randomProblem;
    @FXML
    private Text tituloProblema;
    @FXML
    private Label enunciadoProblema;
    @FXML
    private Button btnComprobarRespuesta;
    
    private Problem currentProblem;
    private Answer ansAlternativaA;
    private Answer ansAlternativaB;
    private Answer ansAlternativaC;
    private Answer ansAlternativaD;
    @FXML
    private RadioButton tBAlternativaA;
    @FXML
    private RadioButton tBAlternativaB;
    @FXML
    private RadioButton tBAlternativaC;
    @FXML
    private RadioButton tBAlternativaD;
    
    private boolean alreadyAnswered = false;
    @FXML
    private Label textErrorCompResp;

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
        lineTool  = new LineTool(zoomGroup, currentLineWidth, currentColor);
        eraserTool = new EraserTool(zoomGroup, map_listview);
        
        // Herramienta por defecto
        setCurrentTool(null); // o panTool si lo tienes

        // Eventos de ratón
        zoomGroup.addEventFilter(MouseEvent.MOUSE_PRESSED,  this::onMapPressed);
        zoomGroup.addEventFilter(MouseEvent.MOUSE_DRAGGED,  this::onMapDragged);
        zoomGroup.addEventFilter(MouseEvent.MOUSE_RELEASED, this::onMapReleased);
        
        mousePosition.setText("X: " + 0 + ",   Y: " + 0);
        try {
            generateRandomProblem(null);
        } catch (NavDAOException e) {
            e.printStackTrace();
        }
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

        // 6) Mover el pin (en coordenadas locales del zoomGroup, sin escala)
        map_pin.setLayoutX(itemSelected.getPosition().getX());
        map_pin.setLayoutY(itemSelected.getPosition().getY());

        pin_info.setText(itemSelected.getDescription());
        map_pin.setVisible(true);
        updateMapPinStyle(itemSelected.getColor());
    }

    
    // Open pages
    @FXML
    private void openProfile(ActionEvent event) {
        openPage("/views/profile.fxml", event);
    }

    @FXML
    private void openProblems(ActionEvent event) {
        openPage("/views/problemSelection.fxml", event);
    }

    @FXML
    private void openResults(ActionEvent event) {
        openPage("/views/results.fxml", event);
    }

    private void openPage(String fxmlPath, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace(); // mejor que dejar el catch vacío
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
    
    
    @FXML
    private void activateBorrarTodo(ActionEvent event) {
        boolean borrado = ClearAll.clearAllWithConfirmation(zoomGroup, data, map_pin);

        if (borrado) {
            setCurrentTool(null);   // solo si el usuario aceptó
        }
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
        List<Problem> allProblems = Navigation.getInstance().getProblems();

        if (!allProblems.isEmpty()) {
            Random random = new Random();
            int index = random.nextInt(allProblems.size());
            Problem problem = allProblems.get(index);
            loadProblem(problem);
        }
    }
    
    public void loadProblem(Problem selected){
        alreadyAnswered = false;

        questionGroup.getToggles().forEach(t -> {
            RadioButton rb = (RadioButton)t;
            rb.setDisable(false);
            rb.setStyle("");
        });
        
        questionGroup.selectToggle(null);
        
        currentProblem = selected;
        enunciadoProblema.setText(selected.getText());
        
        List<Answer> answers = selected.getAnswers();
        
        int[] numeros = {0, 1, 2, 3};

        for (int i = numeros.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            int temp = numeros[i];
            numeros[i] = numeros[j];
            numeros[j] = temp;
        }
        
        ansAlternativaA = answers.get(numeros[0]);
        ansAlternativaB = answers.get(numeros[1]);
        ansAlternativaC = answers.get(numeros[2]);
        ansAlternativaD = answers.get(numeros[3]);

        tBAlternativaA.setText("A. " + ansAlternativaA.getText());
        tBAlternativaB.setText("B. " + ansAlternativaB.getText());
        tBAlternativaC.setText("C. " + ansAlternativaC.getText());
        tBAlternativaD.setText("D. " + ansAlternativaD.getText());        
    }

    @FXML
    private void comprobarRespuesta(ActionEvent event) {
        
        if (alreadyAnswered) return;

        List<Answer> answers = currentProblem.getAnswers();

        Toggle selectedToggle = questionGroup.getSelectedToggle();
        
        if (selectedToggle == null) {
            textErrorCompResp.setVisible(true);
            return;
        }
        
        textErrorCompResp.setVisible(false);

        // agora converte para RadioButton depois de garantir que não é null
        RadioButton selected = (RadioButton) selectedToggle;

        Boolean isCorrect;
        
        if (selected == tBAlternativaA) { isCorrect = ansAlternativaA.getValidity(); }
        else if (selected == tBAlternativaB) { isCorrect = ansAlternativaB.getValidity(); }
        else if (selected == tBAlternativaC) { isCorrect = ansAlternativaC.getValidity(); }
        else { isCorrect = ansAlternativaD.getValidity(); }
        
        alreadyAnswered = true;
        
        questionGroup.getToggles().forEach(t -> {
            RadioButton rb = (RadioButton)t;
            rb.setDisable(true);
            rb.setStyle("-fx-opacity: 1;");
        });
        
        if (isCorrect) { correctAnswer(); }
        else { wrongAnswer(); }
    }

    private void correctAnswer() {        
        // Descobrir qual é a alternativa correta
        Answer correct = currentProblem.getAnswers()
                                       .stream()
                                       .filter(Answer::getValidity)
                                       .findFirst()
                                       .orElse(null);

        // Match Answer -> RadioButton
        RadioButton correctButton = getRadioButtonFromAnswer(correct);

        // Pintar o fundo de verde suave
        marcarAlternativa(correctButton, "#b6ffb3"); // verde claro

        // Adicionar símbolo ✓ no texto
        correctButton.setText(correctButton.getText() + "  ✓");

        // Registrar acerto (exemplo)
        //session.incrementCorrect(currentProblem);
    }

    private void wrongAnswer() {        
        // Descobrir a alternativa correta
        Answer correct = currentProblem.getAnswers()
                                       .stream()
                                       .filter(Answer::getValidity)
                                       .findFirst()
                                       .orElse(null);
        
        // Descobrir alternativa escolhida
        RadioButton selected = (RadioButton) questionGroup.getSelectedToggle();

        RadioButton correctButton = getRadioButtonFromAnswer(correct);

        // Pintar a errada em vermelho claro
        marcarAlternativa(selected, "#ffb3b3"); // vermelho claro
        selected.setText(selected.getText() + "  ✗");

        // Pintar a certa em verde
        marcarAlternativa(correctButton, "#b6ffb3");
        correctButton.setText(correctButton.getText() + "  ✓");

        // Registrar erro
        //session.incrementWrong(currentProblem);
    }
    
    private void marcarAlternativa(RadioButton rb, String color) {
        rb.setStyle("-fx-background-color: " + color + "; -fx-padding: 5px; -fx-opacity: 1;");
    }
    
    private RadioButton getRadioButtonFromAnswer(Answer ans) {
        if (ans == ansAlternativaA) return tBAlternativaA;
        if (ans == ansAlternativaB) return tBAlternativaB;
        if (ans == ansAlternativaC) return tBAlternativaC;
        return tBAlternativaD;
    }
}
