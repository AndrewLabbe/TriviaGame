
public class ServerQuestion {
    
    private String questionText;
    private String[] answers;
    private int correctQuestionIndex;

    public ServerQuestion(String questionText, String[] answers, int correctQuestionIndex){
        this.questionText = questionText;
        this.answers = answers;
        this.correctQuestionIndex = correctQuestionIndex;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getAnswers() {
        return answers;
    }

    public int getCorrectQuestionIndex() {
        return correctQuestionIndex;
    }
    
    

}
