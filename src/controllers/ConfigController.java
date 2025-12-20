package controllers;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Toggle;
import javafx.stage.Stage;
import util.PointTool;
import util.ReglaTool;
import util.ProblemUtil;
import util.ProtractorTool;
import util.SessionManager;
import util.SettingsUtil;

/**
 * @author Gabriela Rego & Rafael Alonso
 */

public class ConfigController implements Initializable {

    private SettingsUtil settings;
    private Boolean valorOriginalColorFondo;
    private boolean valorOriginalDynamicPoiSize;
    private boolean valorOriginalReglaHandleScaling;
    
    @FXML    private CheckBox chkDynamicPoiSize;    
    @FXML    private CheckBox chkColorSolido;
    @FXML    private Button btnResetProblem;
    @FXML    private Button btnBack;
    @FXML    private Button btnSave;
    @FXML    private CheckBox chkSizeTool;


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chkDynamicPoiSize.setSelected(PointTool.isDynamicPoiSizeEnabled());
        if (chkSizeTool != null) {
            chkSizeTool.setSelected(ReglaTool.isDefaultHandleScalingEnabled());
        }
        if (settings != null) {
            valorOriginalColorFondo = settings.isUsarColorSolido();
        }
        chkDynamicPoiSize.setSelected(valorOriginalDynamicPoiSize);
        if (chkSizeTool != null) chkSizeTool.setSelected(valorOriginalReglaHandleScaling);
        
        chkDynamicPoiSize.setSelected(valorOriginalDynamicPoiSize);
        if (chkSizeTool != null) chkSizeTool.setSelected(valorOriginalReglaHandleScaling);
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
        valorOriginalReglaHandleScaling = ReglaTool.isDefaultHandleScalingEnabled();
        
        chkColorSolido.selectedProperty().bindBidirectional(settings.usarColorSolidoProperty());
        chkDynamicPoiSize.setSelected(valorOriginalDynamicPoiSize);
        if (chkSizeTool != null) chkSizeTool.setSelected(valorOriginalReglaHandleScaling);
    }

    @FXML
    private void activateVolver(ActionEvent event) throws IOException {
        settings.setUsarColorSolido(valorOriginalColorFondo);
        PointTool.setDynamicPoiSizeEnabled(valorOriginalDynamicPoiSize);
        ReglaTool.setDefaultHandleScalingEnabled(valorOriginalReglaHandleScaling);
        ProtractorTool.setDefaultHandleScalingEnabled(valorOriginalReglaHandleScaling);
        
        Stage stage = (Stage) btnResetProblem.getScene().getWindow();
        SessionManager.goToMain(stage);
    }

    @FXML
    private void activateGuardar(ActionEvent event) throws IOException {
        PointTool.setDynamicPoiSizeEnabled(chkDynamicPoiSize.isSelected());
        
        if (chkSizeTool != null) {
            boolean enabled = chkSizeTool.isSelected();
            ReglaTool.setDefaultHandleScalingEnabled(enabled);
            ProtractorTool.setDefaultHandleScalingEnabled(enabled);
        }
          
        Stage stage = (Stage) btnSave.getScene().getWindow();
        SessionManager.goToMain(stage);
    }
}
