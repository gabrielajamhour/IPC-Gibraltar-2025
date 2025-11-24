package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import model.User;
import util.SessionManager;

public class ProfileController implements Initializable{

    @FXML private ImageView avatarImage;
    @FXML private Label nicknameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker birthdatePicker;
    @FXML private Label errorLabel;

    private User user;

    public void initialize(URL url, ResourceBundle rb) {

        user = SessionManager.getActiveUser();

        if (user != null) {
            nicknameField.setText(user.getNickName());
            emailField.setText(user.getEmail());
            passwordField.setText(user.getPassword());
            birthdatePicker.setValue(user.getBirthdate());
            avatarImage.setImage(user.getAvatar());
        }
    }

    @FXML
    private void onChangeAvatar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar avatar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg"));

        File file = fc.showOpenDialog(null);
        if (file != null) {
            Image newImg = new Image(file.toURI().toString());
            avatarImage.setImage(newImg);
            user.setAvatar(newImg);
        }
    }

    @FXML
    private void onSave() {
        errorLabel.setText("");

        String newEmail = emailField.getText();
        String newPass = passwordField.getText();
        LocalDate newBirth = birthdatePicker.getValue();

        if (!User.checkEmail(newEmail)) {
            errorLabel.setText("Email inválido.");
            return;
        }
        if (!User.checkPassword(newPass)) {
            errorLabel.setText("La contraseña no cumple los requisitos.");
            return;
        }
        if (!checkAge(newBirth)) {
            return;
        }

        user.setEmail(newEmail);
        user.setPassword(newPass);
        user.setBirthdate(newBirth);

        // Volver al main automaticamente
        Stage stage = (Stage) avatarImage.getScene().getWindow();
        SessionManager.goToMain(stage);
    }

    @FXML
    private void onCancel() {
        Stage stage = (Stage) avatarImage.getScene().getWindow();
        SessionManager.goToMain(stage);
    }
    
    private boolean checkAge(LocalDate birthdate) {

        if (birthdate == null) {
            errorLabel.setText("Debes seleccionar tu fecha de nacimiento");
            return false;
        }

        LocalDate today = LocalDate.now();

        if (birthdate.isAfter(today)) {
            errorLabel.setText("La fecha de nacimiento no puede ser futura");
            return false;
        }

        int years = Period.between(birthdate, today).getYears();

        if (years < 16) {
            errorLabel.setText("Debes tener al menos 16 años");
            return false;
        }

        // Sin errores
        errorLabel.setText("");
        return true;
    }

}
