import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

public class Client {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    private String serverIP;
    private int serverPortTCP;
    private int serverPortUDP;

    private ClientWindow window;

    private DatagramSocket clientUDPSocket;
    private Socket clientTCPSocket;

    private BufferedReader in;
    private PrintWriter out;

    public ClientQuestion currQuestion;

    private String username;

    public Client(String serverIP, int serverPortTCP, int serverPortUDP, String username) {
        this.serverIP = serverIP;
        this.serverPortTCP = serverPortTCP;
        this.serverPortUDP = serverPortUDP;

        this.username = username;

        this.window = new ClientWindow(this);

        // create udp socket
        try {
            this.clientUDPSocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getUsername() {
        return this.username;
    }

    /*
     * Establish connection to server over TCP
     * creates a socket using provided serverIP and the port for server over TCP
     * create in and out communication streams
     * Get client ID from server
     * Loop, starts TCP loop
     */
    public void establishConnectToServer() throws UnknownHostException, IOException, InterruptedException {
        // create the connection, in and out
        // Socket socket = new Socket("localhost", 9090);
        try {
            clientTCPSocket = new Socket(this.serverIP, this.serverPortTCP);

            System.out.println(BLUE + "Starting tcp thread on port: " + clientTCPSocket.getLocalPort() + RESET);

            // Setup output stream to send data to the server
            this.out = new PrintWriter(clientTCPSocket.getOutputStream(), true);

            // Setup input stream to receive data from the server
            this.in = new BufferedReader(new InputStreamReader(clientTCPSocket.getInputStream()));

        } catch (ConnectException e) {
            System.out.println(RED + "Failed to connect to server, make sure the server is running on correct port and IP." + RESET);
            System.out.println(RED + "Exiting..." + RESET);
            System.exit(-1);
        }
        // handshake
        out.println(this.username); // username = clientID
        String message = in.readLine();
        if (message == null) {
            System.out.println(RED + "Server may have disconnected exiting..." + RESET);
            System.exit(-1);
        } else if (message.toLowerCase().strip().startsWith("reject")) {
            System.out.println(RED + "Server rejected connection. '" + message + "'" + RESET);
            System.exit(-1);
        } else {
            System.out.println(GREEN + "Server accepted connection. " + message + RESET);
        }

        // listening/sending thread
        clientGameLoop();
    }

    public void sendAnswer(int answerChoice) {
        // send answer to server over TCP
        out.println(answerChoice);
    }

    private void clientGameLoop() {
        try {
            // first step is waiting for client
            processResponse();
        } catch (Exception e) {
            System.out.println(RED + "error from client game loop, exiting..." + RESET);
            System.exit(-1);
        }
    }

    public void buzz() throws UnknownHostException {
        long timeStamp = System.currentTimeMillis();
        String message = username + "$" + timeStamp;
        System.out.println("BUZZING: " + message + " on question " + currQuestion.getQuestionText());
        byte[] buffer = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // send pack with content of timestamp
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverIP),
                this.serverPortUDP);
        // send packet
        try {
            System.out.println(CYAN + "Sending Buzz Packet " + message + RESET);
            this.clientUDPSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO Possible input {"ack", "negative-ack", "correct", "wrong", "next"}
    private void processResponse() throws IOException, InterruptedException, ClassNotFoundException {
        System.out.println(BLUE + "READY TO RECIEVE FROM SERVER" + RESET);

        clientTCPSocket.setSoTimeout(5000);

        new Thread(() -> {
            try {
                while (true) {
                    out.println("ping");
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        while (true) {
            String serverMessage = "";
            try {
                try {
                    serverMessage = in.readLine();
                } catch (SocketTimeoutException e) {
                    System.out.println(RED + "No message in last 5 seconds" + RESET);
                    if (!clientTCPSocket.isConnected() || clientTCPSocket.isClosed()) {
                        System.out.println(RED + "Server may have disconnected, exiting..." + RESET);
                        System.exit(-1);
                    }
                    continue;
                }
                if (serverMessage == null) {
                    throw new SocketException();
                } else if (serverMessage == "ping")
                    continue;
            } catch (Exception e) {
                System.out.println(RED + "Lost connection to the server. Exiting..." + RESET);
                System.exit(-1);
            }

            if (serverMessage.equalsIgnoreCase("kill")) {
                System.out.println(RED + "You have been removed from the game by the server." + RESET);
                System.exit(0);
            } else if (serverMessage.equals("ack")) {
                System.out.println(GREEN + "ACK: You buzzed first." + RESET);
                window.updateAnswerFeedback("You buzzed first");
                window.answeringClientButtons();
                window.updateGameStateLabel("Answering...");
                window.startTimer(10);
            } else if (serverMessage.equals("negative-ack")) {
                System.out.println(YELLOW + "You were not the first to buzz." + RESET);
                window.updateAnswerFeedback("You did not buzz first");
                window.disableAllButtons();
                window.updateGameStateLabel("Waiting for next question...");
                window.startTimer(10);
            } else if (serverMessage.equals("correct")) {
                System.out.println(GREEN + "Correct! +10 points." + RESET);
            } else if (serverMessage.equals("wrong")) {
                System.out.println(YELLOW + "Incorrect. -10 points." + RESET);
            } else if (serverMessage.equals("none")) {
                System.out.println(YELLOW + "No answer. -20 points." + RESET);
            } else if (serverMessage.equals("next")) {
                System.out.println(BLUE + "Next question..." + RESET);
            } else if (serverMessage.toLowerCase().startsWith("question")) {
                serverMessage = serverMessage.substring("question".length());
                currQuestion = ClientQuestion.deserialize(serverMessage);
                window.startTimer(15);
                window.updateQuestionText(currQuestion);
                window.pollingButtons();
                window.updateGameStateLabel("Polling... BUZZ BUZZ BUZZ!");
                printServerQuestion(currQuestion);
            } else if (serverMessage.toLowerCase().startsWith("leaderboard")) {
                serverMessage = serverMessage.substring("leaderboard".length());
                window.reportScores(serverMessage);
                System.out.println("SCORE REPORT METHOD CALLED, message = " + serverMessage);
            } else if (serverMessage.toLowerCase().startsWith("finished")) {
                window.disableAllButtons();
                window.updateGameStateLabel("Quiz Finished, closing in...");
                window.startTimer(20);
            } else if (serverMessage.toLowerCase().startsWith("late")) {
                serverMessage = serverMessage.substring("late question".length());
                currQuestion = ClientQuestion.deserialize(serverMessage);
                System.out.println(MAGENTA + "LATE QUESTION: " + currQuestion.getQuestionText() + "(" + currQuestion.getQuestionIndex() + ")" + RESET);
                window.lateTimer();
                window.updateQuestionText(currQuestion);
                window.pollingButtons();
                window.updateGameStateLabel("Polling... BUZZ BUZZ BUZZ!");
                // make the labels make sense
            } else if (serverMessage.toLowerCase().startsWith("answering")) {
                serverMessage = serverMessage.substring("ANSWERING".length());
                currQuestion = ClientQuestion.deserialize(serverMessage);
                System.out.println(MAGENTA + "Joined mid answering, Question: " + currQuestion.getQuestionText() + "(" + currQuestion.getQuestionIndex() + ")" + RESET);
                window.disableAllButtons();
                window.updateQuestionText(currQuestion);
                window.updateGameStateLabel("Joined late on answering, waiting for next question...");
                // window.lateTimer();
            } else if (serverMessage.toLowerCase().startsWith("correct answer")) {
                serverMessage = serverMessage.substring("correct answer".length());
                int correctIndex = Integer.parseInt(serverMessage.strip());
                window.updateAnswerFeedback("Correct answer: " + currQuestion.getAnswers()[correctIndex]);
                window.disableAllButtons();
                window.updateGameStateLabel("Waiting for next question...");
            } else {
                System.out.println(YELLOW + "UNKNOWN MESSAGE: " + serverMessage + RESET);
            }

            Thread.sleep(10);
        }
    }

    public void printServerQuestion(ClientQuestion cq) {
        System.out.println(MAGENTA + cq.getQuestionText() + RESET);
        for (String choice : cq.getAnswers()) {
            System.out.println(choice);
        }
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        // server ip arg - REQUIRED
        // username – REQUIRED

        // serverTCPPort
        // serverUDPPort

        // TODO Add args for serverIP, (optional) serverPortTCP, serverPortUDP
        String username = "TestUser";

        if (args.length > 0) {
            username = args[0];
        }

        String serverIP = "10.115.110.178";
        System.out.println(MAGENTA + "Username " + username + ": Connecting to server at " + serverIP + RESET);
        Client client = new Client(serverIP, 9080, 9090, username);
        client.establishConnectToServer();
    }

}
