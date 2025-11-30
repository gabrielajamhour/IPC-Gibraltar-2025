package util;

import controllers.MainController;
import java.io.IOException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import model.Problem;
import model.User;

public class SessionManager {

    private static User activeUser;

    public static void setActiveUser(User user) {
        activeUser = user;
    }

    public static User getActiveUser() {
        return activeUser;
    }
    
    // Trocar a cena para main.fxml
    public static void goToMain(Stage stage) {
        try {
            Parent root = FXMLLoader.load(SessionManager.class.getResource("/views/main.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {}
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
            Parent root = FXMLLoader.load(SessionManager.class.getResource("/views/auth.fxml"));
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {}
    }
    
    public static void goToMainAndLoadProblem(Stage stage, Problem problemToLoad) {
        try {
            FXMLLoader loader = new FXMLLoader(SessionManager.class.getResource("/views/main.fxml"));
            Parent root = loader.load();
            
            MainController mainController = loader.getController();

            if (problemToLoad != null) {
                mainController.loadProblem(problemToLoad); 
            }

            stage.setScene(new Scene(root));
            stage.show();
            
        } catch (IOException e) {
            e.printStackTrace();
            // Manejar error
        }
    }
}