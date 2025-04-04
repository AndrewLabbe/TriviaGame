import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
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

    private BufferedReader in;
    private PrintWriter out;

    private ClientQuestion q;
    
    private int score = 0;

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

    public String getUsername(){
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

        Socket socket = new Socket(this.serverIP, this.serverPortTCP);
        System.out.println("Starting tcp thread on port: " + socket.getPort());

        // Setup output stream to send data to the server
        this.out = new PrintWriter(socket.getOutputStream(), true);

        // Setup input stream to receive data from the server
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // handshake
        out.println("Hello I would like to connect");
        out.println(this.username); // username = clientID
        String message = in.readLine();
        if(message == null) {
            System.out.println("Server may have disconnected exiting...");
            System.exit(-1);
        }else if(message.toLowerCase().startsWith("REJECT")) {
            System.out.println("Username exists/username active");
            System.exit(-1);
        }

        // listening/sending thread
        // Send message to the server
        System.out.println("Status: " + message + "!");
        clientGameLoop();
    }

    public void sendAnswer(int answerChoice){
        // send answer to server over TCP
        out.println(answerChoice);
    }

    private void clientGameLoop() {
        try {
            // first step is waiting for client
            processResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void buzz() throws UnknownHostException {
        long timeStamp = System.currentTimeMillis();
        System.out.println("My username: " + username);
        String message = username + "$" + timeStamp + "$" + q.getQuestionIndex();
        byte[] buffer = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // send pack with content of timestamp
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverIP),
                this.serverPortUDP);
        // send packet
        try {
            System.out.println("Sending Buzz Packet " + message);
            this.clientUDPSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO Possible input {"ack", "negative-ack", "correct", "wrong", "next"}
    private void processResponse() throws IOException, InterruptedException, ClassNotFoundException {
        System.out.println("READY TO RECIEVE FROM SERVER");
        String serverMessage = "";
        while (true) {
            try {
                serverMessage = in.readLine();
            } catch (Exception e) {
                System.out.println(RED + "Lost connection to the server. Exiting..." + RESET);
                System.exit(-1);
            }
        
            if (serverMessage.equalsIgnoreCase("kill")) {
                System.out.println(RED + "You have been removed from the game by the server." + RESET);
                System.exit(0);
            } else if (serverMessage.equals("ack")) {
                System.out.println("ACK: You buzzed first.");
                window.answeringClientButtons();
                window.updateGameStateLabel("Answering");
                window.startTimer(10);
            } else if (serverMessage.equals("negative-ack")) {
                System.out.println("You were not the first to buzz.");
                window.disableAllButtons();
                window.updateGameStateLabel("Waiting for first client to answer");
                window.startTimer(10);
            } else if (serverMessage.equals("correct")) {
                System.out.println("Correct! +10 points.");
                score += 10;
                window.updateScore(score);
            } else if (serverMessage.equals("wrong")) {
                System.out.println("Incorrect. -10 points.");
                score -= 10;
                window.updateScore(score);
            } else if (serverMessage.equals("none")) {
                System.out.println("No answer. -20 points.");
                score -= 20;
                window.updateScore(score);
            } else if (serverMessage.equals("next")) {
                System.out.println("Next question...");
            } else if (serverMessage.toLowerCase().startsWith("question")) {
                serverMessage = serverMessage.substring("question".length());
                q = ClientQuestion.deserialize(serverMessage);
                window.startTimer(15);
                window.updateText(q);
                window.pollingButtons();
                window.updateGameStateLabel("Polling");
                printServerQuestion(q);
            } else {
                System.out.println("UNKNOWN MESSAGE: " + serverMessage);
            }
        
            Thread.sleep(10);
        }        
    }

    public void printServerQuestion(ClientQuestion cq){
        System.out.println(cq.getQuestionText());
        for (String choice : cq.getAnswers()) {
            System.out.println(choice);
        }
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        // System.out.println(Arrays.toString(args));

        String username = "TestUser";
        if(args.length > 0) {
            username = args[0];
        }

        Client client = new Client("localhost", 9080, 9090, username);
        client.establishConnectToServer();
    }

}
