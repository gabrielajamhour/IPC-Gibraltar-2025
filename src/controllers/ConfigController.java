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

/**
 * FXML Controller class
 *
 * @author Rafael Alonso
 */
public class ConfigController implements Initializable {

    @FXML    private Button btnResetProblem;
    @FXML    private CheckBox chkDynamicPoiSize;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        chkDynamicPoiSize.setSelected(PointTool.isDynamicPoiSizeEnabled());
    }
    
    @FXML
    private void activateResetProblem(ActionEvent event) {
        ProblemUtil util = ProblemUtil.getInstance();
        if (util != null) {
            util.resetAnsweredProblems();
        }
    }

    @FXML
    private void activateVovler(ActionEvent event) throws IOException {
        Stage stage = (Stage) btnResetProblem.getScene().getWindow();
        SessionManager.goToMain(stage);
    }

    @FXML
    private void alternateDinamicPOISize(ActionEvent event) {
        PointTool.setDynamicPoiSizeEnabled(chkDynamicPoiSize.isSelected());
    }

}
