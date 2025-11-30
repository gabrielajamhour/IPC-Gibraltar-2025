/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package controllers;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import util.SessionManager;

/**
 *
 * @author jose
 */
public class App extends Application {
    
    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/views/main.fxml"));
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/resources/main.css").toExternalForm());
        
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/resources/logo.png")));
        stage.setTitle("Carta Náutica - Estrecho de Gibraltar");
        stage.setScene(scene);
        stage.show();
        
        SessionManager.goToLogIn(stage);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}
