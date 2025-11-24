package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import model.*;
import java.time.LocalDate;
import javafx.stage.Stage;
import util.SessionManager;

public class ResultsController {

    @FXML private DatePicker dateFilter;
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colDate;
    @FXML private TableColumn<Session, Number> colHits;
    @FXML private TableColumn<Session, Number> colFaults;

    private User currentUser;

    public void initialize() {
        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTimeStamp().toString()
                )
        );
        colHits.setCellValueFactory(data -> 
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getHits()));
        colFaults.setCellValueFactory(data ->
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getFaults()));
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadSessions();
    }

    private void loadSessions() {
        ObservableList<Session> list =
                FXCollections.observableArrayList(currentUser.getSessions());
        sessionsTable.setItems(list);
    }

    @FXML
    private void applyFilter() {
        LocalDate minDate = dateFilter.getValue();
        if (minDate == null) {
            loadSessions();
            return;
        }

        ObservableList<Session> filtered = FXCollections.observableArrayList(
                currentUser.getSessions().stream()
                        .filter(s -> s.getTimeStamp().toLocalDate().isAfter(minDate.minusDays(1)))
                        .toList()
        );

        sessionsTable.setItems(filtered);
    }

    @FXML
    private void goBack() {
        Stage stage = (Stage) dateFilter.getScene().getWindow();
        SessionManager.goToMain(stage);
    }
}
