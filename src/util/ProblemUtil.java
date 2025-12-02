package util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import model.Answer;
import model.NavDAOException;
import model.Navigation;
import model.Problem;
import util.SessionManager;

public class ProblemUtil {
    // Referencia global a la instancia actual
    private static ProblemUtil instance;
    
    // Nodos de la UI
    private final Label enunciadoProblema;
    private final RadioButton tBAlternativaA;
    private final RadioButton tBAlternativaB;
    private final RadioButton tBAlternativaC;
    private final RadioButton tBAlternativaD;
    private final Label textErrorCompResp;
    private final ToggleGroup questionGroup;
    private final Label contadorProblemas;
    private final Label tituloProbActual;

    // Estado interno (lo mismo que tienes ahora en MainController)
    private Problem currentProblem;
    private Answer ansAlternativaA;
    private Answer ansAlternativaB;
    private Answer ansAlternativaC;
    private Answer ansAlternativaD;
    private boolean alreadyAnswered = false;
    
    // Lista de problemas ya respondidos en esta sesión
    private final List<Problem> answeredProblems = new ArrayList<>();

    public ProblemUtil (
            Label enunciadoProblema,
            RadioButton tBAlternativaA,
            RadioButton tBAlternativaB,
            RadioButton tBAlternativaC,
            RadioButton tBAlternativaD,
            Label textErrorCompResp,
            ToggleGroup questionGroup,
            Label contadorProblemas,
            Label tituloProbActual
    ) {
        // guardar la instancia global
        instance = this;
        
        this.enunciadoProblema = enunciadoProblema;
        this.tBAlternativaA = tBAlternativaA;
        this.tBAlternativaB = tBAlternativaB;
        this.tBAlternativaC = tBAlternativaC;
        this.tBAlternativaD = tBAlternativaD;
        this.textErrorCompResp = textErrorCompResp;
        this.questionGroup = questionGroup;
        this.contadorProblemas = contadorProblemas;
        this.tituloProbActual = tituloProbActual;
        try {
            generateRandomProblem();
        } catch (NavDAOException ex) {
            System.getLogger(ProblemUtil.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
        updateSessionCounters();
        updateProblemTitle();
    }
    
    
    public static ProblemUtil getInstance() {
        return instance;
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
            enunciadoProblema.setText("Ya has respondido todos los problemas disponibles.");
            // opcional: desactivar el botón de comprobar o el de random si quieres
            // btnComprobarRespuesta.setDisable(true);
            // randomProblem.setDisable(true);
            return;
        }

        // Elegimos aleatoriamente SOLO entre los no respondidos
        Random random = new Random();
        int index = random.nextInt(remaining.size());
        Problem problem = remaining.get(index);
        loadProblem(problem);

        updateProblemTitle();
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

    /** Equivale a tu comprobarRespuesta(ActionEvent event) */
    public void comprobarRespuesta() {

        if (alreadyAnswered) return;

        // mismo patrón que ya tienes:
        List<Answer> answers = currentProblem.getAnswers();

        Toggle selectedToggle = questionGroup.getSelectedToggle();

        if (selectedToggle == null) {
            textErrorCompResp.setVisible(true);
            return;
        }

        textErrorCompResp.setVisible(false);

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

        // desactivar toggles y dejar opacidad normal
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

        updateSessionCounters();
        
        // Registrar que este problema ya ha sido respondido
        if (currentProblem != null && !answeredProblems.contains(currentProblem)) {
            answeredProblems.add(currentProblem);
        }
    }

    /** Igual que tu correctAnswer() */
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

    /** Igual que tu wrongAnswer() */
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

    /** Igual que tu updateSessionCounters() */
    public void updateSessionCounters() {
        int total = SessionManager.getProblemsSolved();
        int correct = SessionManager.getProblemsCorrect();
        int incorrect = SessionManager.getProblemsIncorrect();

        contadorProblemas.setText(
                "Aciertos: " + correct +
                "  |  Fallos: " + incorrect +
                "  |  Total: " + total
        );
    }

    /** Igual que tu updateProblemTitle() */
    public void updateProblemTitle() {
        int nextProblemNumber = SessionManager.getProblemsSolved() + 1;
        tituloProbActual.setText("Problema #" + nextProblemNumber);
    }
    
    /**
     * Resetea la lista de problemas ya respondidos para el generador
     * aleatorio. Después de llamar a este método, generateRandomProblem()
     * volverá a considerar todos los problemas como "no usados".
     */
    public void resetAnsweredProblems() {
        answeredProblems.clear();
        try {
            generateRandomProblem();
        } catch (NavDAOException ex) {
            System.getLogger(ProblemUtil.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
        }
    }   
}
