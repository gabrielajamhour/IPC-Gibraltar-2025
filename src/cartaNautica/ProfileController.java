package cartaNautica;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import model.Navigation;
import java.io.File;
import java.time.LocalDate;
import model.NavDAOException;
import model.User;

public class ProfileController {

    @FXML private ImageView avatarImage;
    @FXML private TextField nicknameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker birthdatePicker;
    @FXML private Label errorLabel;

    private User currentUser;

    public void initialize() throws NavDAOException {
        // Obtener instancia única de Navegacion (según PDF)
        Navigation nav = Navigation.getInstance();
        currentUser = nav.getLoggedUser();

        if (currentUser == null) {
            errorLabel.setText("No hay usuario logueado.");
            return;
        }

        // Rellenar datos
        nicknameField.setText(currentUser.getNickName());
        emailField.setText(currentUser.getEmail());
        passwordField.setText(currentUser.getPassword());
        birthdatePicker.setValue(currentUser.getBirthdate());

        // Cargar avatar
        if (currentUser.getAvatar() != null) {
            avatarImage.setImage(new Image(currentUser.getAvatar()));
        }
    }

    @FXML
    private void onChangeAvatar() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Seleccionar avatar");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes", "*.png", "*.jpg"));

        File file = fc.showOpenDialog(null);
        if (file != null) {
            avatarImage.setImage(new Image(file.toURI().toString()));
            currentUser.setAvatar(file.toURI().toString());
        }
    }

    @FXML
    private void onSave() {
        errorLabel.setText("");

        String newEmail = emailField.getText();
        String newPass = passwordField.getText();
        LocalDate newBirth = birthdatePicker.getValue();

        // Validaciones según el PDF
        if (!User.checkEmail(newEmail)) {
            errorLabel.setText("Email inválido.");
            return;
        }
        if (!User.checkPassword(newPass)) {
            errorLabel.setText("La contraseña no cumple los requisitos.");
            return;
        }
        if (User.getBirthdate() == newBirth) { // este método existe según PDF
            errorLabel.setText("Debes tener al menos 16 años.");
            return;
        }

        // Guardar cambios
        currentUser.setEmail(newEmail);
        currentUser.setPassword(newPass);
        currentUser.setBirthdate(newBirth);

        errorLabel.setText("Cambios guardados correctamente.");
    }

    @FXML
    private void onCancel() {
        // volver a main.fxml
        Navigation.UtilsViews.openMainView(); // ejemplo: depende de cómo gestiones navegación
    }
}
