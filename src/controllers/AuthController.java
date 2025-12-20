package controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import model.NavDAOException;
import model.Navigation;
import model.User;
import util.SessionManager;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TextFormatter;

/**
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
    @FXML    private ToggleButton btnViewPassword;
    
    // Guarda la contraseña REAL (el TextField mostrará ●●● si está oculto)
    private final StringProperty realPassword = new SimpleStringProperty("");
    private boolean internalPasswordUpdate = false;

    
    // para evitar bucles cuando actualizamos el textfield “desde dentro”
    private boolean internalUpdate = false;
    
    // ===================== initialize =====================
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Enter -> iniciar (lo mantienes)
        ePassword.setOnAction(e -> bIniciar.fire());

        // Intercepta TODO lo que el usuario hace en el TextField (teclear, borrar, pegar...)
        ePassword.setTextFormatter(new TextFormatter<String>(change -> {
            if (internalPasswordUpdate) return change;
            if (!change.isContentChange()) return change;

            String old = realPassword.get();
            int start = change.getRangeStart();
            int end = change.getRangeEnd();
            String inserted = change.getText() == null ? "" : change.getText();

            // Construye el nuevo texto REAL aplicando el cambio
            String newReal = old.substring(0, start) + inserted + old.substring(end);
            realPassword.set(newReal);

            // caret lógico
            int newCaret = start + inserted.length();

            // Cancela el cambio normal y repinta nosotros
            Platform.runLater(() -> {
                internalPasswordUpdate = true;
                refreshPasswordDisplay();
                ePassword.positionCaret(Math.min(newCaret, ePassword.getText().length()));
                internalPasswordUpdate = false;
            });

            return null; // cancela el cambio original
        }));

        // Toggle: ver/ocultar
        if (btnViewPassword != null) {
            btnViewPassword.selectedProperty().addListener((obs, was, is) -> {
                internalPasswordUpdate = true;
                refreshPasswordDisplay();
                ePassword.positionCaret(ePassword.getText().length());
                internalPasswordUpdate = false;
            });
        }

        // Estado inicial
        internalPasswordUpdate = true;
        refreshPasswordDisplay();
        internalPasswordUpdate = false;
        
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
        
        initErrorLabel(lUserInvalid);
        initErrorLabel(lPasswordWrong);
    }  
    
    private void initErrorLabel(Label lbl) {
        lbl.setVisible(false);
        lbl.setManaged(false);
    }

    
    // ===================== Accept =====================
    @FXML
    private void pulsadoIniciar(ActionEvent event) throws IOException {
        String username = eUsername.getText();
        String password = getPasswordReal();
        
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
    private void pulsadoRegistrar(MouseEvent event) {
        Stage stage = (Stage) ePassword.getScene().getWindow();
        SessionManager.goToRegister(stage);
    }    
    
    // ===================== Validaciones =====================

    private void checkPassword() {
        String password = getPasswordReal();
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
        
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        textField.styleProperty().setValue("-fx-background-color: #FCE5E0"); 
    }
    
    private void manageCorrect(Label errorLabel, TextField textField, BooleanProperty boolProp ){
        boolProp.setValue(true);
        
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
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

    private void refreshPasswordDisplay() {
        String real = realPassword.get();
        boolean reveal = (btnViewPassword != null && btnViewPassword.isSelected());

        if (reveal) {
            ePassword.setText(real);
        } else {
            ePassword.setText("●".repeat(real.length()));
        }
    }

    private String getPasswordReal() {
        return realPassword.get();
    }
}