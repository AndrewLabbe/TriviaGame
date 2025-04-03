
import com.sun.jdi.IntegerType;

public class ClientQuestion {
    
    private String questionText;
    private String[] answers;
    private int questionIndex;

    public ClientQuestion(String questionText, String[] answers, int questionIndex){
        this.questionText = questionText;
        this.answers = answers;
        this.questionIndex = questionIndex;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getAnswers() {
        return answers;
    }

    public static ClientQuestion convertQuestion(ServerQuestion sq, int questionIndex){
        return new ClientQuestion(sq.getQuestionText(), sq.getAnswers(), questionIndex);
    }

    // public static byte[] serialize(final ClientQuestion obj) {
    //     ByteArrayOutputStream bos = new ByteArrayOutputStream();

    //     try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
    //         out.writeObject(obj);
    //         out.flush();
    //         return bos.toByteArray();
    //     } catch (Exception ex) {
    //         throw new RuntimeException(ex);
    //     }
    // }

    // public static ClientQuestion deserialize(byte[] bytes)
    //         throws ClassNotFoundException, IOException {
    //     ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
    //     ObjectInput in = new ObjectInputStream(bis);
    //     return (ClientQuestion) in.readObject();
    // }


    /**
     * Serialize a ClientQuestion object to a string, DO NOT USE ';' IN POSSIBLE ANSWER STRIGNS
     * @param q
     * @return
     */
    public static String serialize(final ClientQuestion q) {
        return q.questionText + "$" + String.join(";", q.answers) + "$" + q.questionIndex;
    }

    public static ClientQuestion deserialize(String s) {
        String[] parts = s.split("\\$");
        return new ClientQuestion(parts[0], parts[1].split(";"), Integer.parseInt(parts[2]));
    }

    public int getQuestionIndex() {
        return questionIndex;
    }
}
