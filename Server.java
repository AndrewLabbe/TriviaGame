import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    public static final String RESET = "\u001B[0m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    private static serverQuestion[] questionList = {
        new serverQuestion("Question 1", new String[]{"Answer1", "Answer2", "Answer3"}, 0),
        new serverQuestion("Question 2", new String[]{"Answer1", "Answer2", "Answer3"}, 0),
        new serverQuestion("Question 3", new String[]{"Answer1", "Answer2", "Answer3"}, 0),
        new serverQuestion("Question 4", new String[]{"Answer1", "Answer2", "Answer3"}, 0),
    };

    public int currentIDIteration = 0;

    public enum GameState {
        WAITING_FOR_PLAYERS, POLLING, CLIENT_ANSWERING, SHOWING_ANSWERS,
    }

    // set to -1 because game first increments value to move onto "first" question at index 0
    private int currentQuestion = -1;
    private GameState gameState = GameState.WAITING_FOR_PLAYERS;

    private int portTCP;
    private int portUDP;

    private Map<String, ClientInfo> clientSockets = new HashMap<>();

    private ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<String>();

    private ServerSocket serverTCPSocket;
    private DatagramSocket UDPDatagramSocket;

    public Server(int portTCP, int portUDP) {
        this.portTCP = portTCP;
        this.portUDP = portUDP;

        // Create tcp socket to accept incoming connections over tcp
        try {
            this.serverTCPSocket = new ServerSocket(this.portTCP);
            this.UDPDatagramSocket = new DatagramSocket(this.portUDP);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void createUDPPollingThread() {
        try {
            byte[] incomingData = new byte[1024];
            System.out.println(YELLOW + "Starting a UDP Thread..." + RESET);

            while (true) {
                try {
                    // TODO if not polling it still will block on incoming packet then quit
                    // System.out.println("What is the game state: " + gameState);
                    // if (gameState != GameState.POLLING) {
                    // System.out.println("Game state is not polling, closing UDP socket");
                    // UDPDatagramSocket.close();
                    // return;
                    // }

                    // create datagram packet using incoming data as paramater
                    System.out.println(YELLOW + "Listening for a new UDP packet..." + RESET);
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    UDPDatagramSocket.receive(incomingPacket);
                    // TODO see above TODO
                    if (gameState != GameState.POLLING) {
                        UDPDatagramSocket.receive(incomingPacket);

                        byte[] data = incomingPacket.getData();
                        System.out.println("Data: " + data);
                        System.out.println("Game state is not polling, ignoring packet that came in");
                        // System.out.println("Game state is not polling, closing UDP socket");
                        // UDPDatagramSocket.close();
                        continue;
                    }
                    // accept packet
                    UDPDatagramSocket.receive(incomingPacket);

                    byte[] data = incomingPacket.getData();
                    // try to convert to long
                    String message = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    String altMessage = "";

                    // bytes include hidden characters, manual parsing needed
                    for (int i = 0; i < message.length(); i++) {
                        char currentChar = message.charAt(i);
                        
                        // Check if the character is '$' or a digit
                        if (currentChar == '$' || Character.isDigit(currentChar)) {
                            // Process the character (here we print it)
                            altMessage += currentChar;
                        }
                    }

                    message = altMessage;

                    // when add to queue, use clientID and timestamp
                    messageQueue.add(message);
                    System.out.println(CYAN + "Client buzzed: " + message + RESET);
                    Thread.sleep(5);
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Listening process was interupted", e);
        }
    }

    /*
     * Always accepting new TCP connections
     * -------------------------------------
     * Loops forever:
     * blocks until client connects over TCP
     * Asks createClientTCPThread to setup TCP thread
     * restarts loop blocks until client connects over TCP...
     */
    public void createTCPConnectionThread() throws IOException, InterruptedException {
        try {
            while (true) {
                // wait for a client to request to connect
                System.out.println("Waiting new TCP connection...");

                Socket clientSocket = this.serverTCPSocket.accept();
                System.out.println("Client connected!");

                createClientTCPThread(clientSocket);
            }
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    /*
     * Takes in a connected TCP socket:
     * Creates in and out streams for TCP connection
     * Assigns client ID: <Client IP>:<PORT>
     * Creates a ClientInfo to track connected clients
     * Throws client into a thread loop to manage TCP communication, every client
     * gets its own TCP thread
     */
    private void createClientTCPThread(Socket clientSocket) throws InterruptedException, IOException {
        // Setup input and output streams for communication with the client
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

        // String clientIP = clientSocket.getInetAddress().getHostAddress();
        // int clientPort = clientSocket.getPort();
        // String clientID = clientIP + ":" + clientPort;

        String clientID = "" + currentIDIteration;
        currentIDIteration++;

        ClientInfo info = new ClientInfo(clientID, clientSocket, in, out);
        clientSockets.put(clientID, info);
        // assign client ID
        System.out.println("Client says: " + in.readLine());
        out.println(clientID);

        // create new thread
        new Thread(() -> {
            try {
                clientThread(info);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    /*
     * The loop/thread that each client will individually run
     */
    // TODO implement client game logic
    private void clientThread(ClientInfo info) throws IOException, InterruptedException {
        while (true) {
            if (info.in.ready())
                System.out.println("Client says: " + info.in.readLine());
            Thread.sleep(10);
        }
    }

    public void runGameLoop() throws InterruptedException, IOException {
        // packets constantly and do nothing with them if not polling gamestate
        new Thread(() -> {
            createUDPPollingThread();
        }).start();

        // TODO process for managing when to stop waiting for clients
        gameState = GameState.WAITING_FOR_PLAYERS;
        int secondForJoin = 10;
        System.out.println(GREEN + "Waiting for clients to join for " + secondForJoin + " seconds..." + RESET);
        Thread.sleep(secondForJoin * 1000);

        // Thread for polling, stays constantly open because it will just receive

        while (true) {
            gameState = GameState.POLLING;

            // Send "next question" message out to clients
            sendNext();

            // sending question
            sendQuestion();

            // switch game state to polling waiting for clients to buzz
            gameState = GameState.POLLING;

            // waiting for 15 seconds
            System.out.println(GREEN + "Polling for 15 seconds..." + RESET);
            long buzzTime = 15000;
            Thread.sleep(buzzTime);
            gameState = GameState.CLIENT_ANSWERING;
            // poll.join();
            System.out.println(GREEN + "Polling done, moving to client answering..." + RESET);

            // parse queue for who answered first
            if (messageQueue.isEmpty()) {
                System.out.println(RED + "No clients buzzed, not showing answers next question..." + RESET);
                // TODO what to do when no clients answered; send "next" move to next question
                sendNext();
                continue;
            } else {
                // split on '$'
                System.out.println("Splitting " + messageQueue.peek());
                String[] parts = messageQueue.poll().split("\\$");
                System.out.println("Parts: " + Arrays.toString(parts));

                String firstClientID = parts[0]; // first id in queue
                long minTime = Long.valueOf(parts[1]); // first timestamp in queue

                // find first client who buzzed
                while (!messageQueue.isEmpty()) {
                    parts = messageQueue.poll().split("\\$");
                    String clientID = parts[0];
                    long timeStamp = Long.parseLong(parts[1]);
                    if (timeStamp < minTime) {
                        minTime = timeStamp;
                        firstClientID = clientID;
                    }
                }
                // response with "ack" for first client and "negative-ack" for others
                for (String clientID : clientSockets.keySet()) {
                    ClientInfo info = clientSockets.get(clientID);
                    if (clientID.equals(firstClientID)) {
                        info.out.println("ack");
                    } else {
                        info.out.println("negative-ack");
                    }
                }
                ClientInfo firstClient = clientSockets.get(firstClientID);
                // wait for 10 seconds to get answer
                int waitTime = 10000;
                Thread.sleep(waitTime);

                // TODO When we recieve a UDP packet the client port is different for UPD and TCP
                /*
                 * When we get tcp connection we give the id as <ip>:<TCP port>
                 * When the client buzzes the incoming packet is from <ip>:<UDP port>
                 * So if we try to extract the client id from the incoming packet we will not
                 * find it in the map
                 */
                if (firstClient.in.ready()) {
                    String response = firstClient.in.readLine();
                    System.out.println("Client answered: " + response);
                    // TODO if correct +10 points and send "correct"
                    firstClient.out.println("Correct! +10");
                    // TODO if wrong -10 points and send "wrong"
                    firstClient.out.println("Wrong! -10");
                } else {
                    System.out.println("Client did not answer");
                    firstClient.out.println("No answer! -20");
                    // TODO -20 points
                }
            }
            gameState = GameState.SHOWING_ANSWERS;
            System.out.println("Showing answers...");
            int timeToShowAnswers = 5000;
            Thread.sleep(timeToShowAnswers);

            // TODO show answers

        }
    }

    private void sendNext() {
        for (String clientID : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientID);
            info.out.println("next");
        }
    }

    private void sendQuestion() {
        currentQuestion++;
        for (String clientID : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientID);
            // TODO send question
            info.out.println(new clientQuestion(questionList[currentQuestion].getQuestionText(), questionList[currentQuestion].getAnswers()));
        }
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        Server server = new Server(9080, 9090);

        new Thread(() -> {
            try {
                server.createTCPConnectionThread();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        server.runGameLoop();
    }
}
