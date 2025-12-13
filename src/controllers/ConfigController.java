package controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.stage.Stage;
import util.PointTool;
import util.ProblemUtil;
import util.SessionManager;
import util.SettingsUtil;

/**
 * @author Gabriela Rego
 */

public class ConfigController implements Initializable {

    @FXML    private Button btnResetProblem;
    @FXML    private CheckBox chkDynamicPoiSize;
    
    private SettingsUtil settings;
    @FXML    private CheckBox chkColorSolido;
    @FXML    private Button btnBack;
    private Boolean valorOriginalColorFondo;
    private boolean valorOriginalDynamicPoiSize;
    @FXML    private Button btnSave;


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chkDynamicPoiSize.setSelected(PointTool.isDynamicPoiSizeEnabled());
        if (settings != null) {
            valorOriginalColorFondo = settings.isUsarColorSolido();
        }
        chkDynamicPoiSize.setSelected(valorOriginalDynamicPoiSize);
    }
    
    @FXML
    private void activateResetProblem(ActionEvent event) {
        ProblemUtil util = ProblemUtil.getInstance();
        if (util != null) {
            util.resetAnsweredProblems();
        }
    }

    @FXML
    private void alternateDinamicPOISize(ActionEvent event) {}
    
    public void setSettings(SettingsUtil settings) {
        this.settings = settings;
        valorOriginalColorFondo = settings.isUsarColorSolido();
        valorOriginalDynamicPoiSize = PointTool.isDynamicPoiSizeEnabled();
        
        chkColorSolido.selectedProperty().bindBidirectional(settings.usarColorSolidoProperty());
        chkDynamicPoiSize.setSelected(valorOriginalDynamicPoiSize);
    }

    @FXML
    private void activateVolver(ActionEvent event) throws IOException {
        settings.setUsarColorSolido(valorOriginalColorFondo);
        PointTool.setDynamicPoiSizeEnabled(valorOriginalDynamicPoiSize);
        
        Stage stage = (Stage) btnResetProblem.getScene().getWindow();
        SessionManager.goToMain(stage);
    }

    @FXML
    private void activateGuardar(ActionEvent event) throws IOException {
        PointTool.setDynamicPoiSizeEnabled(chkDynamicPoiSize.isSelected());
        Stage stage = (Stage) btnSave.getScene().getWindow();
        SessionManager.goToMain(stage);
    }

}
