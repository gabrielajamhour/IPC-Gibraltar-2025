package cartaNautica;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ResourceBundle;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.NavDAOException;
import model.User;
import model.Navigation;
import util.SessionManager;

/**
 * FXML Controller class
 *
 * @author Rafael Alonso
 */
public class RegisterController implements Initializable {
 
    //properties to control valid fieds values
    private BooleanProperty validPassword;
    private BooleanProperty validEmail;
    private BooleanProperty equalPasswords;  
    private BooleanProperty validAge;
    private BooleanProperty validUsername;
    
    private final int EQUALS = 0;  
    private Image selectedAvatar;


    // Email
    @FXML    private TextField eemail;
    @FXML    private Label lIncorrectEmail;
    
    // Passwrod 1
    @FXML    private Label lIncorrectPassword;
    @FXML    private TextField epassword;
    
    // Password 2
    @FXML    private TextField epassword2;
    @FXML    private Label lPassDifferent;
    
    // Username
    @FXML    private TextField eUsername;
    @FXML    private Label lInvalidUsername;
    
    // Age
    @FXML    private DatePicker eAge;
    @FXML    private Label lageNotOldEnought;
    
    // Avatar
    @FXML    private Label lAvatarError;
    
    // Buttons
    @FXML    private Button bAccept;
    @FXML    private Button bCancel;
    

    
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
    
    
    // Para el DatePicker
    private void manageError(Label errorLabel, DatePicker textField, BooleanProperty boolProp ){
        boolProp.setValue(false);
        textField.requestFocus();
        
        errorLabel.visibleProperty().set(true);
        textField.styleProperty().setValue("-fx-background-color: #FCE5E0"); 
    }
    
    private void manageCorrect(Label errorLabel, DatePicker textField, BooleanProperty boolProp ){
        boolProp.setValue(true);
        
        errorLabel.visibleProperty().set(false);
        textField.styleProperty().setValue("");
    }
    

    // ===================== initialize =====================
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {
       
        // Inicializamos las properties en false
        validEmail = new SimpleBooleanProperty(false);
        validPassword = new SimpleBooleanProperty(false);   
        equalPasswords = new SimpleBooleanProperty(false);
        validAge = new SimpleBooleanProperty(false);
        validUsername = new SimpleBooleanProperty(false);
        
        
        // Listeners        
        addValidateOnFocusLost(eemail, this::checkEditMail);
        addValidateOnFocusLost(epassword, this::checkPassword);
        addValidateOnFocusLost(epassword2, this::checkEquals);
        addValidateOnFocusLost(eUsername, this::checkUsername);
        addValidateOnFocusLost(eAge, this::checkAge);
        
        // Listener para password1 == password2
        // Validación en “tiempo real” al escribir la segunda contraseña
        //epassword2.textProperty().addListener((obs, oldText, newText) -> {checkEquals();});
        
        
        // Habilitar o deshabilitar el boton ACCEPT
        BooleanBinding validFields = validEmail.and(validPassword).and(equalPasswords).and(validAge).and(validUsername);
        
        bAccept.disableProperty().bind(validFields.not());
        
        bCancel.setOnAction( (event)->{
            Stage stage = (Stage) epassword.getScene().getWindow();
            SessionManager.goToLogIn(stage);
                });
    } 

    
    // ===================== Validaciones =====================
    
    private void checkEditMail() {
        String email = eemail.getText();
        if(!User.checkEmail(email))
            // Incorrect email
            manageError(lIncorrectEmail, eemail, validEmail);
        else
            manageCorrect(lIncorrectEmail, eemail, validEmail);
    }

    private void checkPassword() {
        String password = epassword.getText();
        if(!User.checkPassword(password))
            // Incorrect password
            manageError(lIncorrectPassword, epassword, validPassword);
        else
            manageCorrect(lIncorrectPassword, epassword, validPassword);
    }
    
    private void checkEquals() {
        String pass1 = epassword.getText();
        String pass2 = epassword2.getText();
        
        if (pass1.isEmpty() || pass2.isEmpty()) {return;}
        
        if(pass1.compareTo(pass2) != EQUALS){
            manageError(lPassDifferent, epassword2, equalPasswords);
        } else
            manageCorrect(lPassDifferent, epassword2, equalPasswords);
    }
    
    private void checkUsername() {
        try {
            
            String username = eUsername.getText();
            Navigation nav = Navigation.getInstance();
            if (!User.checkNickName(username)){
                // Invalid Username
                manageError(lInvalidUsername, eUsername, validUsername);
            } else if (nav.exitsNickName(username)){
                lInvalidUsername.setText("Username already in use");
                manageError(lInvalidUsername, eUsername, validUsername);
            } else {
                manageCorrect(lInvalidUsername, eUsername, validUsername);
            }
            
        } catch (NavDAOException ex) {
            System.getLogger(RegisterController.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    private void checkAge() {
        LocalDate birthdate = eAge.getValue();

        // 1) No ha seleccionado fecha
        if (birthdate == null) {
            lageNotOldEnought.setText("Debes seleccionar tu fecha de nacimiento");
            manageError(lageNotOldEnought, eAge, validAge);
            return;
        }

        LocalDate today = LocalDate.now();

        // 2) Fecha en el futuro (error)
        if (birthdate.isAfter(today)) {
            lageNotOldEnought.setText("La fecha de nacimiento no puede ser futura");
            manageError(lageNotOldEnought, eAge, validAge);
            return;
        }

        // 3) Calcular edad
        int years = Period.between(birthdate, today).getYears();

        if (years < 16) {
            lageNotOldEnought.setText("Debes tener al menos 16 años");
            manageError(lageNotOldEnought, eAge, validAge);
        } else {
            // Correcto
            lageNotOldEnought.setVisible(false);
            manageCorrect(lageNotOldEnought, eAge, validAge);
        }
    }
    
    
    @FXML
    private void onChangeAvatar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecciona tu avatar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg"));

        File file = fc.showOpenDialog(bAccept.getScene().getWindow());
        if (file != null) {
            Image selectedAvatar = new Image(file.toURI().toString());
        }
    }
    
    // ===================== Listeners auxiliares =====================
    
    private void addValidateOnFocusLost(TextField field, Runnable validator) {
        field.focusedProperty().addListener((obs, oldFocused, newFocused) -> {
            if (!newFocused) {    // cuando pierde el foco
                validator.run();
            }
        });
    }
    
    // Para DatePicker
    private void addValidateOnFocusLost(DatePicker field, Runnable validator) {
        field.focusedProperty().addListener((obs, oldFocused, newFocused) -> {
            if (!newFocused) {    // cuando pierde el foco
                validator.run();
            }
        });
    }
    
    
    // ===================== Accept =====================
    
    @FXML
    private void handleBAcceptOnAction(ActionEvent event) {
        String email = eemail.getText();
        String password = epassword.getText();
        String username = eUsername.getText();
        LocalDate birthdate = eAge.getValue();
        
        try {
            Navigation nav = Navigation.getInstance();
            User u = nav.registerUser(username, email, password, selectedAvatar, birthdate);
            SessionManager.setActiveUser(u);

            Stage stage = (Stage) epassword.getScene().getWindow();
            SessionManager.goToMain(stage);

        } catch (NavDAOException ex) {}
    }

}
