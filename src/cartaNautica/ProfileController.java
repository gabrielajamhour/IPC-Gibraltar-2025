package cartaNautica;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import model.User;
import util.SessionManager;

public class ProfileController implements Initializable{

    @FXML private ImageView avatarImage;
    @FXML private TextField nicknameField;
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
        if (!user.getBirthdate().equals(newBirth)) {
            errorLabel.setText("Debes tener al menos 16 años.");
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
}
