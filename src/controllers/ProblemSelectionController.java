package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import model.*;
import java.util.List;
import javafx.stage.Stage;
import util.SessionManager;

public class ProblemSelectionController {

    @FXML private TextField searchField;
    @FXML private ListView<Problem> problemsList;

    private List<Problem> allProblems;

    public void setProblems(List<Problem> problems) {
        allProblems = problems;
        problemsList.setItems(FXCollections.observableArrayList(allProblems));
    }

    @FXML
    private void searchProblems() {
        String query = searchField.getText().toLowerCase();

//        var filtered = allProblems.stream()
//                .filter(p -> p.getText().get().toLowerCase().contains(query))
//                .toList();
//
//        problemsList.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML
    private void openProblem() {
        Problem selected = problemsList.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Debes seleccionar un problema");
            return;
        }

        // Aquí deberías navegar al Main y cargar este problema seleccionado
        // mainController.loadProblem(selected);
    }

    @FXML
    private void goBack() {
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
}
