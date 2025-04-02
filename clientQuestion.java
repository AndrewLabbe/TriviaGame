public class ClientQuestion {
    
    private String questionText;
    private String[] answers;

    public ClientQuestion(String questionText, String[] answers){
        this.questionText = questionText;
        this.answers = answers;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String[] getAnswers() {
        return answers;
    }

    public static ClientQuestion convertQuestion(ServerQuestion sq){
        return new ClientQuestion(sq.getQuestionText(), sq.getAnswers());
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
        return q.questionText + "$" + String.join(";", q.answers);
    }


    public static ClientQuestion deserialize(String s) {
        String[] parts = s.split("\\$");
        return new ClientQuestion(parts[0], parts[1].split(";"));
    }
}
