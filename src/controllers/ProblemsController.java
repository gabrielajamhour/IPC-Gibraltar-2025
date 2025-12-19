package controllers;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import model.*;
import java.util.List;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import util.SessionManager;

/**
 * @author Gabriela Rego
 */

public class ProblemsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ListView<Problem> problemsList;

    private List<Problem> allProblems;
    @FXML    private Button btnOpenProblem;
    
    private MainController mainController;
    private User user;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            Navigation nav = Navigation.getInstance();
            allProblems = nav.getProblems();
            problemsList.setItems(FXCollections.observableArrayList(allProblems));
            
            problemsList.setCellFactory(list -> new ProblemCell());

        } catch (NavDAOException ex) {} 
    }
    
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }

    public void setUser(User user) {
        this.user = user;
    }
    
    @FXML
    private void searchProblems() {
        String query = searchField.getText().toLowerCase();

        List<Problem> filtered = allProblems.stream()
                .filter(p -> p.getText().toLowerCase().contains(query))
                .toList();

        problemsList.setItems(FXCollections.observableArrayList(filtered));
    }
    
    @FXML
    private void clearSearch() {
        searchField.clear();

        problemsList.setItems(
                FXCollections.observableArrayList(allProblems)
        );
    }

    @FXML
    private void openProblem(ActionEvent event) {
        Problem selectedProblem = problemsList.getSelectionModel().getSelectedItem();

        if (selectedProblem == null) {
            showAlert("Debes seleccionar un problema");
            return;
        }
        
        if (selectedProblem != null && mainController != null) {
        mainController.loadProblem(selectedProblem);

        Stage modalStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        modalStage.close();
    }
    }

    @FXML
    private void goBack(ActionEvent event) {
        Stage modalStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        modalStage.close();
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    public class ProblemCell extends ListCell<Problem> {

        private final Label numberLabel = new Label();
        private final Label textLabel = new Label();
        private final HBox row = new HBox(10);

        public ProblemCell() {
            numberLabel.setMinWidth(40);
            numberLabel.setAlignment(Pos.CENTER);
            numberLabel.setStyle(
                "-fx-text-fill: white;" +    
                "-fx-font-weight: bold;" +
                "-fx-background-color: #2a8396;" +
                "-fx-background-radius: 6;" +
                "-fx-padding: 2 0;"
            );

            textLabel.setWrapText(true);
            textLabel.setMaxWidth(Double.MAX_VALUE);

            HBox.setHgrow(textLabel, Priority.ALWAYS);
            row.getChildren().addAll(numberLabel, textLabel);

            setPrefWidth(0);
        }

        @Override
        protected void updateItem(Problem problem, boolean empty) {
            super.updateItem(problem, empty);

            if (empty || problem == null) {
                setGraphic(null);
                setStyle("");
                return;
            }

            numberLabel.setText(String.valueOf(getIndex() + 1));
            textLabel.setText(problem.getText());

            setGraphic(row);

            Boolean result = SessionManager.getProblemResult(problem);
            if (result == null) {
                setStyle("");
            } else if (result) {
                setStyle("-fx-background-color: #c6f7c2;");
            } else {
                setStyle("-fx-background-color: #f6c1c1;");
            }
        }
    }

}