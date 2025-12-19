package util;

import controllers.MainController;
import controllers.SessionsController;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Problem;
import model.Session;
import model.User;

/**
 * @author Gabriela Rego & Rafael Alonso
 */

public class SessionManager {

    private static User activeUser;
    private static Session currentSession;
    private static SessionsController sessionsController;
    
    private static int problemsSolved = 0; 
    private static int problemsCorrect = 0;   
    private static int problemsIncorrect = 0; 
    
    private static LocalDateTime sessionStartTime;
    
    private static final Map<Integer, Boolean> problemResults = new HashMap<>();

    public static void setSessionsController(SessionsController sc) {
        sessionsController = sc;
    }

    public static SessionsController getSessionsController() {
        return sessionsController;
    }
    
    public static void startNewSession(User user) {
        problemsSolved = 0;
        problemsCorrect = 0;
        problemsIncorrect = 0;
        
        sessionStartTime = LocalDateTime.now();
        activeUser = user;
        
        currentSession = new Session(sessionStartTime, 0, 0);
    }

    public static User getActiveUser() {
        return activeUser;
    }
    
    public static Session getCurrentSession() {
        return currentSession;
    }
    
    // Trocar a cena para main.fxml
    public static void goToMain(Stage stage) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(SessionManager.class.getResource("/views/main.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.setUser(activeUser);
            
            mainController.setSettings(SettingsUtil.getInstance());

            stage.setScene(new Scene(root));
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void goToRegister(Stage stage) {
        try {
            Parent root = FXMLLoader.load(SessionManager.class.getResource("/views/register.fxml"));
            stage.setScene(new Scene(root));
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
            stage.show();
        } catch (IOException e) {}
    }
    
    public static void goToLogIn(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(SessionManager.class.getResource("/views/auth.fxml"));
            Parent root = loader.load();            
            stage.setScene(new Scene(root));
            stage.setMinWidth(stage.getWidth());
            stage.setMinHeight(stage.getHeight());
            stage.show();
        } catch (IOException e) {}
    }
    
    public static int getProblemsSolved() { return problemsSolved; }
    public static int getProblemsCorrect() { return problemsCorrect; }
    public static int getProblemsIncorrect() { return problemsIncorrect; }
    
    public static void registerCorrectAttempt() {
        problemsSolved++;
        problemsCorrect++;
        rebuildCurrentSession();
    }
    
    public static void registerIncorrectAttempt() {
        problemsSolved++;
        problemsIncorrect++;
        rebuildCurrentSession();
    }
    
    private static void rebuildCurrentSession() {
        if (sessionStartTime != null) {
            currentSession = new Session(
                sessionStartTime,
                problemsCorrect,
                problemsIncorrect
            );
        }
    }

    public static void clearSessionCounters() {
        problemsSolved = 0;
        problemsCorrect = 0;
        problemsIncorrect = 0;
    }
    
    public static void finalizeAndSaveSession() {
        if (activeUser != null && currentSession != null) {
            activeUser.addSession(
                currentSession.getHits(),
                currentSession.getFaults()
            );
        }

        currentSession = null;
        activeUser = null;

        clearSessionCounters();
        sessionStartTime = null;
        problemResults.clear();
    }
    
    public static void clearProblemsResults(){
        problemResults.clear();
    }
    
    public static void registerProblemResult(Problem p, boolean correct) {
        problemResults.put(p.getText().hashCode(), correct);
    }

    public static Boolean getProblemResult(Problem p) {
        return problemResults.get(p.getText().hashCode());
    }
}