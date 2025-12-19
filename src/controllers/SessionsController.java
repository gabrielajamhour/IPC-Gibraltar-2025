package controllers;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.*;
import model.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import javafx.stage.Stage;
import util.SessionManager;

/**
 * @author Gabriela Rego
 */

public class SessionsController {

    @FXML private DatePicker dateFilter;
    @FXML private TableView<Session> sessionsTable;
    @FXML private TableColumn<Session, String> colDate;
    @FXML private TableColumn<Session, Number> colHits;
    @FXML private TableColumn<Session, Number> colFaults;

    private User currentUser;
    private Session currentSession;
    private ObservableList<Session> tableData;
    
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
        
        SessionManager.setSessionsController(this);
    }

    public void setUser(User user) {
        this.currentUser = user;
        loadSessions();
    }
    
    public void setCurrentSession(Session session) {
        this.currentSession = session;
        loadSessions();
    }

    private void loadSessions() {
        // Creamos la lista observable desde las sesiones del usuario
        java.util.List<Session> sessions = currentUser != null && currentUser.getSessions() != null
                ? new ArrayList<>(currentUser.getSessions())
                : new ArrayList<>();
        
        sessions.removeIf(s -> s.getHits() == 0 && s.getFaults() == 0);

        // Si hay una sesión en curso y aún no está en la lista, la añadimos al inicio
        if (currentSession != null && !sessions.contains(currentSession)) {
            sessions.add(0, currentSession);
        }

        // Convertimos a ObservableList para la tabla
        tableData = FXCollections.observableArrayList(sessions);
        tableData.sort((s1, s2) -> s2.getTimeStamp().compareTo(s1.getTimeStamp()));
        sessionsTable.setItems(tableData);
    }

    
    public void updateCurrentSession(Session updatedSession) {
        if (tableData == null) return;

        int index = tableData.indexOf(currentSession);

        if (index >= 0) {
            tableData.set(index, updatedSession);
        }

        currentSession = updatedSession;
    }

    @FXML
    private void applyFilter() {
        LocalDate minDate = dateFilter.getValue();
        
        if (minDate == null) {
            sessionsTable.setItems(tableData);
            return;
        }
        
        ObservableList<Session> filtered = FXCollections.observableArrayList(
            tableData.stream()
                .filter(s ->
                    s.getTimeStamp()
                     .toLocalDate()
                     .isAfter(minDate.minusDays(1))
                )
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