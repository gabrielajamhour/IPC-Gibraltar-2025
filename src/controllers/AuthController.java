package controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.NavDAOException;
import model.Navigation;
import model.User;
import util.SessionManager;

/**
 * FXML Controller class
 *
 * @author Rafael Alonso
 */
public class AuthController implements Initializable {

    // Set "true" para que el usuario tenga un feedback del requisito de la contrasena inmediato,
    // de manera que puede saber si la contrasena que digita cumple los requisitos de una cotnrasena, 
    // (no le va a decir si su contrasena es correcta, solo si cumple los requisitos)
    private final boolean feedback_password = false;
    
    //properties to control valid fieds values
    private BooleanProperty validPassword;
    private BooleanProperty validUsername;
    
    @FXML    private Label lUserInvalid;
    @FXML    private Label lPasswordWrong;
    @FXML    private TextField eUsername;
    @FXML    private TextField ePassword;
    @FXML    private Button bIniciar;
    
    
    // ===================== initialize =====================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ePassword.setOnAction(e -> bIniciar.fire());
        
        validPassword = new SimpleBooleanProperty(false);   
        validUsername = new SimpleBooleanProperty(false);
        
        addValidateOnFocusLost(eUsername, this::checkUsername);
        
        if (feedback_password){
            addValidate(ePassword, this::checkPassword);
            
            BooleanBinding validFields = validPassword.and(validUsername);
            bIniciar.disableProperty().bind(validFields.not());
        } else {
            bIniciar.disableProperty().bind(validUsername.not());
        }
    }  

    
    // ===================== Accept =====================
    @FXML
    private void pulsadoIniciar(ActionEvent event) throws IOException {
        String username = eUsername.getText();
        String password = ePassword.getText();
        
        try {
            Navigation nav = Navigation.getInstance();
            User u = nav.authenticate(username, password);
            
            
            if(u == null){
                lPasswordWrong.setText("Wrong Password");
                manageError(lPasswordWrong, ePassword, validPassword);
            } else {
                SessionManager.startNewSession(u);
                Stage stage = (Stage) ePassword.getScene().getWindow();
                SessionManager.goToMain(stage);
            }
            
        } catch (NavDAOException ex) {
            System.getLogger(AuthController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    @FXML
    private void pulsadoRegistrar(ActionEvent event) {
        Stage stage = (Stage) ePassword.getScene().getWindow();
        SessionManager.goToRegister(stage);
    }
    
    
    // ===================== Validaciones =====================

    private void checkPassword() {
        String password = ePassword.getText();
        if(!User.checkPassword(password)){
            lPasswordWrong.setText("Invalid Password");
            manageError(lPasswordWrong, ePassword, validPassword);
        } else {
            manageCorrect(lPasswordWrong, ePassword, validPassword);
        }
    }
    
    private void checkUsername() {
        try {
            String username = eUsername.getText();
            Navigation nav = Navigation.getInstance();
            if (!nav.exitsNickName(username)){
                manageError(lUserInvalid, eUsername, validUsername);
            } else {
                manageCorrect(lUserInvalid, eUsername, validUsername);
            }
        } catch (NavDAOException ex) {
            System.getLogger(AuthController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    
    // ===================== Helpers de error =====================
    
    private void manageError(Label errorLabel, TextField textField, BooleanProperty boolProp ){
        boolProp.setValue(false);
        textField.requestFocus();
        
        errorLabel.visibleProperty().set(true);
        textField.styleProperty().setValue("-fx-background-color: #FCE5E0"); 
    }
    
    private void manageCorrect(Label errorLabel, TextField textField, BooleanProperty boolProp ){
        boolProp.setValue(true);
        
        errorLabel.visibleProperty().set(false);
        textField.styleProperty().setValue("");
    }
    
    
    // ===================== Listeners auxiliares =====================
    
    private void addValidateOnFocusLost(TextField field, Runnable validator) {
        field.focusedProperty().addListener((obs, oldFocused, newFocused) -> {
            if (!newFocused) {    // cuando pierde el foco
                validator.run();
            }
        });
    }
    
    private void addValidate(TextField field, Runnable validator) {
        field.textProperty().addListener((obs, oldFocused, newFocused) -> {
            // Siempre
            validator.run();
        });
    }
    
}