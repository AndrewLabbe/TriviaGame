import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArgHandler {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";

    private List<String> args;
    private String helpText;
    private String[] requiredArgs;

    public HashMap<String, String> argMap = new HashMap<>();

    private Map<String, String> optionalArgsWithDef;

    /**
     * Constructor for ArgHandler, Helps to parse args
     *
     * @param args
     * @param helpText
     * @param requiredArgs
     * @param optionalArgsWithDef
     */
    public ArgHandler(String args[], String helpText, String[] requiredArgs, Map<String, String> optionalArgsWithDef) {
        // convert args to List
        this.args = List.of(args);
        this.helpText = helpText;
        this.requiredArgs = requiredArgs;
        this.optionalArgsWithDef = optionalArgsWithDef;

        // replace null args with empty
        if (requiredArgs == null) {
            this.requiredArgs = new String[0];
        }

        if (optionalArgsWithDef == null) {
            this.optionalArgsWithDef = new HashMap<>();
        }
        this.argMap = generateArgMap();

    }

    private HashMap<String, String> generateArgMap() {
        HashMap<String, String> argMap = new HashMap<>();
        // Add default values for optional arguments
        for (Map.Entry<String, String> entry : optionalArgsWithDef.entrySet()) {
            argMap.put(entry.getKey(), entry.getValue());
        }
        try {
            for (int i = 0; i < args.size(); i += 2) {
                String key = args.get(i);
                String value = args.get(i + 1);
                argMap.put(key, value);
            }
        } catch (IndexOutOfBoundsException e) {
            System.out.println(RED + "Invalid argument format, expected <arg> <value>; i.e. --username Tom" + RESET);
            printHelp();
            System.exit(-1);
        } catch (Exception e) {
            System.out.println(RED + "An error occurred while processing arguments: \n" + RESET + e.getMessage());
            printHelp();
            System.exit(-1);
        }

        boolean valid = true;
        for (String requiredArg : requiredArgs) {
            if (!argMap.containsKey(requiredArg)) {
                System.out.println(RED + "Missing required argument: " + requiredArg + RESET);
                valid = false;
            }
        }
        if (!valid) {
            printHelp();
            System.exit(-1);
        }
        return argMap;
    }

    public void printHelp() {
        System.out.println();
        System.out.println(helpText);
        System.out.println("Format: <arg> <value>; i.e. --username Tom");
        System.out.println();
        if (requiredArgs.length > 0) {
            System.out.println("Required arguments:");
            for (String arg : requiredArgs) {
                System.out.println("  " + arg);
            }
        }
        if (optionalArgsWithDef.size() > 0) {
            System.out.println("Optional arguments:");
            for (String arg : optionalArgsWithDef.keySet()) {
                System.out.println("  " + arg + " <default: " + optionalArgsWithDef.get(arg) + ">");
            }
        }

    }

    /**
     * Get the value of an argument, returns null if not in map
     * Will try arg in format arg -arg and --arg, compatible with all 3 as arg type
     *
     * @param arg
     * @return
     */
    public String get(String arg) {
        // get num dashes that arg input starts with

        for (int i = 0; i <= 2; i++) {
            String dashes = "-".repeat(i);
            if (argMap.containsKey(dashes + arg)) {
                return argMap.get(dashes + arg);
            }
        }
        System.out.println(RED + "Argument not found: " + arg + RESET);
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String arg : requiredArgs) {
            sb.append(arg).append(": ").append(argMap.get(arg)).append("\n");
        }
        for (String arg : optionalArgsWithDef.keySet()) {
            sb.append(arg).append(": ").append(argMap.get(arg)).append("\n");
        }
        return sb.toString();
    }

    public void put(String key, String value) {
        argMap.put(key, value);
    }
}