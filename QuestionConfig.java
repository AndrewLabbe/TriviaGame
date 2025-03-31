import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class QuestionConfig {
    public String configPath;

    public QuestionConfig(String configPath) {
        this.configPath = configPath;
    }

    public ServerQuestion[] parseConfig() {
        ArrayList<ServerQuestion> questions = new  ArrayList<ServerQuestion>();

        String currQ = null;
        ArrayList<String> currAnswers = new ArrayList<String>();
        int correctIndex = -1;
        try {
            // Read all lines from the file and store them in a List of Strings
            List<String> lines = Files.readAllLines(Paths.get(configPath));
            
            // Convert List to an Array
            String[] linesArray = lines.toArray(new String[0]);
            
            // Print the lines
            for (String line : linesArray) {
                // skip empty lines
                if(line.trim().length() == 0)
                    continue;
                char firstChar = line.charAt(0);
                if(currQ == null) {
                    currQ = line;
                    continue;
                }
                // '-' is marker for a answer choice
                if(firstChar == '-') {
                    line = line.replace("-", "");
                    currAnswers.add(line.trim()); // trim begining and end spaces if any
                // '=' is marker for correct answer
                } else if(firstChar == '=') {
                    line = line.replace("=", "");
                    correctIndex = Integer.parseInt(line.trim()); // trim begining and end spaces if any
                // ';' is marker for end of question info
                } else if (firstChar == ';') {
                    if(currAnswers.size() == 0) {
                        System.err.println("Improper config file format, no answers found for question. Exiting...");
                        System.exit(-1);
                    } else if(correctIndex == -1) {
                        System.err.println("Improper config file format, no correct answer found for question. Exiting...");
                        System.exit(-1);
                    } else if(correctIndex >= currAnswers.size()) {
                        System.err.println("Improper config file format, correct answer index out of bounds. Exiting...");
                        System.exit(-1);
                    }
                    questions.add(new ServerQuestion(currQ, currAnswers.toArray(new String[currAnswers.size()]), correctIndex));

                    currQ = null;
                    currAnswers.clear();
                    correctIndex = -1;
                // if anything else then config is improper
                } else {
                    System.err.println("Improper config file format question not terminated with ';'. Exiting...");
                    System.exit(-1);
                }
            }
            if(currQ != null) {
                System.err.println("Improper config file format, final question not terminated with ';'. Exiting...");
                System.exit(-1);
            }
            return questions.toArray(new ServerQuestion[questions.size()]);
        } catch (IOException e) {
            // Handle any IO exceptions
            e.printStackTrace();
            System.err.println("Exiting...");
            System.exit(-1);
        }
        return null; // will never reach this but must be provided else error
    }

    public static void main(String[] args) {
        QuestionConfig questionConfig = new QuestionConfig("questions.txt");
        ServerQuestion[] questions = questionConfig.parseConfig();
        for(ServerQuestion q : questions) {
            System.out.println(q.getQuestionText());
            for(String a : q.getAnswers()) {
                System.out.println(a);
            }
            System.out.println(q.getCorrectQuestionIndex());
        }
    }
}
