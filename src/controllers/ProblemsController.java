package controllers;

import java.io.IOException;
import java.net.URL;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import model.*;
import java.util.List;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import util.SessionManager;

public class ProblemsController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ListView<Problem> problemsList;

    private List<Problem> allProblems;
    @FXML
    private Button btnOpenProblem;
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            Navigation nav = Navigation.getInstance();
            allProblems = nav.getProblems();
            problemsList.setItems(FXCollections.observableArrayList(allProblems));
            
            problemsList.setCellFactory(list -> new ProblemCell());

        } catch (NavDAOException ex) {} 
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
    private void openProblem() {
        Problem selected = problemsList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Debes seleccionar un problema");
            return;
        }
        
        Stage stage = (Stage) searchField.getScene().getWindow();
        SessionManager.goToMainAndLoadProblem(stage, selected);
    }

    @FXML
    private void goBack() throws IOException {
        Stage stage = (Stage) searchField.getScene().getWindow();
        SessionManager.goToMain(stage);
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Aviso");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    public class ProblemCell extends ListCell<Problem> {

        @Override
        protected void updateItem(Problem problem, boolean empty) {
            super.updateItem(problem, empty);

            if (empty || problem == null) {
                setText(null);
            } else {
                setText(problem.getText());
            }
        }
    }
}