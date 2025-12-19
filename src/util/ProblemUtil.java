package util;

import controllers.SessionsController;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import model.Answer;
import model.NavDAOException;
import model.Navigation;
import model.Problem;
import model.Session;

/**
 * @author Gabriela Rego
 */

public class ProblemUtil {
    // Referencia global a la instancia actual
    private static ProblemUtil instance;
    
    // Nodos de la UI
    private final Label enunciadoProblema;
    private final RadioButton tBAlternativaA;
    private final RadioButton tBAlternativaB;
    private final RadioButton tBAlternativaC;
    private final RadioButton tBAlternativaD;
    private final ToggleGroup questionGroup;
    private final Label tituloProbActual;

    // Estado interno (lo mismo que tienes ahora en MainController)
    private Problem currentProblem;
    private Answer ansAlternativaA;
    private Answer ansAlternativaB;
    private Answer ansAlternativaC;
    private Answer ansAlternativaD;
    private boolean alreadyAnswered = false;
    
    // Lista de problemas ya respondidos en esta sesión
    private static final List<Problem> answeredProblems = new ArrayList<>();

    public ProblemUtil (
            Label enunciadoProblema,
            RadioButton tBAlternativaA,
            RadioButton tBAlternativaB,
            RadioButton tBAlternativaC,
            RadioButton tBAlternativaD,
            ToggleGroup questionGroup,
            Label tituloProbActual
    ) {
        // guardar la instancia global
        instance = this;
        
        this.enunciadoProblema = enunciadoProblema;
        this.tBAlternativaA = tBAlternativaA;
        this.tBAlternativaB = tBAlternativaB;
        this.tBAlternativaC = tBAlternativaC;
        this.tBAlternativaD = tBAlternativaD;
        this.questionGroup = questionGroup;
        this.tituloProbActual = tituloProbActual;
        try {
            loadFirstProblem();
        } catch (NavDAOException ex) {
            System.getLogger(ProblemUtil.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }
    
    private int getProblemNumber(Problem problem) {
        try {
            List<Problem> allProblems = Navigation.getInstance().getProblems();
            int index = allProblems.indexOf(problem);
            return index >= 0 ? index + 1 : -1;
        } catch (NavDAOException e) {
            return -1;
        }
    }

    
    
    public static ProblemUtil getInstance() {
        return instance;
    }
    
    public void loadFirstProblem() throws NavDAOException {
        List<Problem> allProblems = Navigation.getInstance().getProblems();

        if (allProblems == null || allProblems.isEmpty()) return;

        Problem first = allProblems.get(0);
        
        loadProblem(first);
        updateProblemTitle(first);
    }
    
    
    public void generateRandomProblem() throws NavDAOException {
        List<Problem> allProblems = Navigation.getInstance().getProblems();

        if (allProblems.isEmpty()) {
            return;
        }

        // Filtramos solo los que aún no se han respondido
        List<Problem> remaining = new ArrayList<>();
        for (Problem p : allProblems) {
            if (!answeredProblems.contains(p)) {
                remaining.add(p);
            }
        }

        // Si no quedan problemas sin responder, mostramos mensaje y salimos
        if (remaining.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Problemas completados");
            alert.setHeaderText("Ya has respondido todos los problemas disponibles.");
            alert.setContentText(
                "Si quieres volver a empezar, ve a la pantalla de Configuración " +
                "y usa la opción de resetear problemas."
            );
            alert.showAndWait();

            // NO tocamos el enunciado actual, simplemente no cargamos uno nuevo.
            return;
        }

        // Elegimos aleatoriamente SOLO entre los no respondidos
        Random random = new Random();
        int index = random.nextInt(remaining.size());
        Problem problem = remaining.get(index);
        loadProblem(problem);

        updateProblemTitle(problem);
    }

    /** Equivale a tu loadProblem(Problem selected) */
    public void loadProblem(Problem selected) {
        alreadyAnswered = false;

        // mismo código que tienes:
        questionGroup.getToggles().forEach(t -> {
            RadioButton rb = (RadioButton) t;
            rb.setDisable(false);
            rb.setStyle("");
        });

        questionGroup.selectToggle(null);

        currentProblem = selected;
        enunciadoProblema.setText(selected.getText());

        List<Answer> answers = selected.getAnswers();

        int[] numeros = {0, 1, 2, 3};

        for (int i = numeros.length - 1; i > 0; i--) {
            int j = (int) (Math.random() * (i + 1));
            int temp = numeros[i];
            numeros[i] = numeros[j];
            numeros[j] = temp;
        }

        ansAlternativaA = answers.get(numeros[0]);
        ansAlternativaB = answers.get(numeros[1]);
        ansAlternativaC = answers.get(numeros[2]);
        ansAlternativaD = answers.get(numeros[3]);

        tBAlternativaA.setText("A. " + ansAlternativaA.getText());
        tBAlternativaB.setText("B. " + ansAlternativaB.getText());
        tBAlternativaC.setText("C. " + ansAlternativaC.getText());
        tBAlternativaD.setText("D. " + ansAlternativaD.getText());
    }
    
    public void updateProblemTitle(Problem problem) {
        int number = getProblemNumber(problem);

        if (number > 0) {
            tituloProbActual.setText("Problema #" + number);
        } else {
            tituloProbActual.setText("Problema");
        }
    }

    public void comprobarRespuesta() {

        if (alreadyAnswered) return;

        List<Answer> answers = currentProblem.getAnswers();

        Toggle selectedToggle = questionGroup.getSelectedToggle();

        RadioButton selected = (RadioButton) selectedToggle;

        Boolean isCorrect;

        if (selected == tBAlternativaA) {
            isCorrect = ansAlternativaA.getValidity();
        } else if (selected == tBAlternativaB) {
            isCorrect = ansAlternativaB.getValidity();
        } else if (selected == tBAlternativaC) {
            isCorrect = ansAlternativaC.getValidity();
        } else {
            isCorrect = ansAlternativaD.getValidity();
        }

        alreadyAnswered = true;

        questionGroup.getToggles().forEach(t -> {
            RadioButton rb = (RadioButton) t;
            rb.setDisable(true);
            rb.setStyle("-fx-opacity: 1;");
        });

        if (isCorrect) {
            SessionManager.registerCorrectAttempt();
            correctAnswer();
        } else {
            SessionManager.registerIncorrectAttempt();
            wrongAnswer();
        }
        
        SessionManager.registerProblemResult(currentProblem, isCorrect);
        
        if (currentProblem != null && !answeredProblems.contains(currentProblem)) {
            answeredProblems.add(currentProblem);
        }
        
        Session updated = SessionManager.getCurrentSession();

        SessionsController sc = SessionManager.getSessionsController();
        if (sc != null) {
            sc.updateCurrentSession(updated);
}

    }

    private void correctAnswer() {
        Answer correct = currentProblem.getAnswers()
                .stream()
                .filter(Answer::getValidity)
                .findFirst()
                .orElse(null);

        RadioButton correctButton = getRadioButtonFromAnswer(correct);

        marcarAlternativa(correctButton, "#b6ffb3"); // verde claro
        correctButton.setText(correctButton.getText() + "  ✓");
    }

    private void wrongAnswer() {
        Answer correct = currentProblem.getAnswers()
                .stream()
                .filter(Answer::getValidity)
                .findFirst()
                .orElse(null);

        RadioButton selected = (RadioButton) questionGroup.getSelectedToggle();
        RadioButton correctButton = getRadioButtonFromAnswer(correct);

        marcarAlternativa(selected, "#e88b8b"); // rojo claro
        selected.setText(selected.getText() + "  ✗");

        marcarAlternativa(correctButton, "#81c97f"); // verde claro
        if (!correctButton.getText().contains("✓")) {
            correctButton.setText(correctButton.getText() + "  ✓");
        }
    }

    private void marcarAlternativa(RadioButton rb, String color) {
        rb.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 14px; -fx-padding: 2 5 2 5; -fx-opacity: 1;");
    }

    private RadioButton getRadioButtonFromAnswer(Answer ans) {
        if (ans == ansAlternativaA) return tBAlternativaA;
        if (ans == ansAlternativaB) return tBAlternativaB;
        if (ans == ansAlternativaC) return tBAlternativaC;
        return tBAlternativaD;
    }
    
    public void resetAnsweredProblems() {
        answeredProblems.clear();
        SessionManager.clearProblemsResults();
        try {
            generateRandomProblem();
        } catch (NavDAOException ex) {
            System.getLogger(ProblemUtil.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }   
}
