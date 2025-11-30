package controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import model.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javafx.stage.Stage;
import util.SessionManager;

public class ResultsController {

    @FXML private DatePicker dateFilter;
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colDate;
    @FXML private TableColumn<Session, Number> colHits;
    @FXML private TableColumn<Session, Number> colFaults;

    private User currentUser;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    public void initialize() {
        colDate.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getTimeStamp().format(DATE_FORMATTER)
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
        java.util.List<Session> sessions = currentUser.getSessions();
        if (sessions == null) {
            sessions = new java.util.ArrayList<>();
        }
        
        ObservableList<Session> list = FXCollections.observableArrayList(sessions);
        sessionsTable.setItems(list);
    }

    @FXML
    private void applyFilter() {
        LocalDate minDate = dateFilter.getValue();
        if (minDate == null) {
            loadSessions();
            return;
        }

        if (currentUser == null || currentUser.getSessions() == null) {
             sessionsTable.setItems(FXCollections.observableArrayList());
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
    private void goBack() throws IOException {
        Stage stage = (Stage) dateFilter.getScene().getWindow();
        SessionManager.goToMain(stage);
    }
}
