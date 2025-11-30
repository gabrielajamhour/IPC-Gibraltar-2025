package util;

import controllers.AuthController;
import controllers.MainController;
import java.io.IOException;
import java.time.LocalDateTime;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Problem;
import model.Session;
import model.User;

public class SessionManager {

    private static User activeUser;
    
    private static int problemsSolved = 0; 
    private static int problemsCorrect = 0;   
    private static int problemsIncorrect = 0; 
    
    private static LocalDateTime sessionStartTime;

    public static void startNewSession(User user) {
        problemsSolved = 0;
        problemsCorrect = 0;
        problemsIncorrect = 0;
        sessionStartTime = LocalDateTime.now();
        activeUser = user;
    }

    public static User getActiveUser() {
        return activeUser;
    }
    
    // Trocar a cena para main.fxml
    public static void goToMain(Stage stage) throws IOException {
        try {
            FXMLLoader loader = new FXMLLoader(SessionManager.class.getResource("/views/main.fxml"));
            Parent root = loader.load();

            MainController mainController = loader.getController();
            mainController.setUser(activeUser);

            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void goToRegister(Stage stage) {
        try {
            Parent root = FXMLLoader.load(SessionManager.class.getResource("/views/register.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {}
    }
    
    public static void goToLogIn(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(SessionManager.class.getResource("/views/auth.fxml"));
            Parent root = loader.load();            
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {}
    }
    
    public static void goToMainAndLoadProblem(Stage stage, Problem problemToLoad) {
        try {
            FXMLLoader loader = new FXMLLoader(SessionManager.class.getResource("/views/main.fxml"));
            Parent root = loader.load();
            
            MainController mainController = loader.getController();
            mainController.setUser(activeUser);

            if (problemToLoad != null) {
                mainController.loadProblem(problemToLoad); 
            }

            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static int getProblemsSolved() { return problemsSolved; }
    public static int getProblemsCorrect() { return problemsCorrect; }
    public static int getProblemsIncorrect() { return problemsIncorrect; }
    
    public static void registerCorrectAttempt() {
        problemsSolved++;
        problemsCorrect++;
    }
    
    public static void registerIncorrectAttempt() {
        problemsSolved++;
        problemsIncorrect++;
    }
    
    public static void clearSessionCounters() {
        problemsSolved = 0;
        problemsCorrect = 0;
        problemsIncorrect = 0;
    }
    
    public static void finalizeAndSaveSession() {
        activeUser.addSession(problemsCorrect, problemsIncorrect);
        
        activeUser = null;
        clearSessionCounters();
        sessionStartTime = null;
    }
}