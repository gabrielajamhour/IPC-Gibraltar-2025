package controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import util.SessionManager;
import util.SettingsUtil;

/**
 * @author Gabriela Rego & Rafael Alonso
 */

public class App extends Application {
    private SettingsUtil settings = SettingsUtil.getInstance();
    
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Parent root = loader.load();
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        stage.setTitle("Carta Náutica - Estrecho de Gibraltar");
        
        MainController controller = loader.getController();
        controller.setSettings(settings);
        
        stage.setScene(scene);
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());
        stage.show();
        
        SessionManager.goToLogIn(stage);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
    public SettingsUtil getSettings() { return settings; }
    
}
