
import java.io.Serializable;

public class clientQuestion implements Serializable{
    
    private String questionText;
    private String[] answers;

    public clientQuestion(String questionText, String[] answers){
        this.questionText = questionText;
        this.answers = answers;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getAnswers() {
        return answers;
    }

}
