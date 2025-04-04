import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
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

    // private static ServerQuestion[] questionList = {
    //     new ServerQuestion("Question 1: What is a fuzzy animal", new String[]{"bear", "lizard", "turtle"}, 0),
    //     new ServerQuestion("Question 2: What is the name of a rock", new String[]{"dog", "cat", "rock"}, 0),
    //     new ServerQuestion("Question 3", new String[]{"Answer1", "Answer2", "Answer3"}, 0),
    //     new ServerQuestion("Question 4", new String[]{"Answer1", "Answer2", "Answer3"}, 0),
    // };

    private static ServerQuestion[] questionList;

    public int currentIDIteration = 0;

    public enum GameState {
        WAITING_FOR_PLAYERS, POLLING, CLIENT_ANSWERING, SHOWING_ANSWERS,
    }

    // set to -1 because game first increments value to move onto "first" question at index 0
    private int currentQuestion = 0;
    private GameState gameState = GameState.WAITING_FOR_PLAYERS;

    private int portTCP;
    private int portUDP;

    private Map<String, ClientInfo> clientSockets = new HashMap<>();

    private ConcurrentLinkedQueue<String> UDPMessageQueue = new ConcurrentLinkedQueue<String>();

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


        QuestionConfig qConfig = new QuestionConfig("questions.txt");
        questionList = qConfig.getQuestionsAsArray();
    }

    public void createUDPPollingThread() {
        try {
            byte[] incomingData = new byte[1024];
            System.out.println(YELLOW + "Starting a UDP Thread..." + RESET);

            while (true) {
                try {
                    // create datagram packet using incoming data as paramater
                    System.out.println(YELLOW + "Listening for a new UDP packet..." + RESET);
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    UDPDatagramSocket.receive(incomingPacket);

                    // if recieve while not polling skip
                    if (gameState != GameState.POLLING) {
                        System.out.println("Game state is not polling, ignoring packet that came in");
                        continue;
                    }

                    byte[] data = incomingPacket.getData();
                    // try to convert to long
                    String message = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    String validString = ""; 

                    // bytes include hidden characters, manual parsing needed
                    for (int i = 0; i < message.length(); i++) {
                        char currentChar = message.charAt(i);
                        
                        // Check if the character is '$' or a digit
                        if (currentChar == '$' || Character.isDigit(currentChar) || Character.isLetter(currentChar)) {
                            // Process the character (here we print it)
                            validString += currentChar;
                        }
                    }

                    String[] parts = validString.split("\\$");
                    int questionIndex = Integer.parseInt(parts[2]);
                    if(questionIndex != currentQuestion){
                        System.out.println(YELLOW + "Recieved buzz from earlier question, moving on and ignoring." + RESET);
                        continue;
                    }
                    message = parts[0] + "$" + parts[1];

                    // when add to queue, use clientUsername and timestamp
                    UDPMessageQueue.add(message);
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
        // String clientUsername = clientIP + ":" + clientPort;
        
        // String clientUsername = "" + currentIDIteration;
        // currentIDIteration++;

        String IP = clientSocket.getInetAddress().getHostAddress();
        int port = clientSocket.getPort();

        boolean hasConnectedB4 = false;
        // Check if client already has existed

        // assign client ID
        System.out.println("Client says: " + in.readLine());
        // Client will send username as second send
        String clientUsername = in.readLine();
        // out.println(info.getClientID());
        
        for (ClientInfo tmpInfo : clientSockets.values()) {
            if (tmpInfo.isARejoin(clientUsername, IP)) {
                if(tmpInfo.isAlive()) {
                    out.println("REJECT: user already active under username:IP combo");
                    return;
                }

                System.out.println(GREEN + "Client has preivously connected, reinitializing client state..." + RESET);
                hasConnectedB4 = true;
                tmpInfo.setActive(true);
                startClientThread(tmpInfo, in, out);
                break;
            }
        }

        if(!hasConnectedB4) {
            ClientInfo info = new ClientInfo(clientUsername, clientSocket, IP, port);
            clientSockets.put(clientUsername, info);
            System.out.println(GREEN + "New client connected with ID: " + clientUsername + RESET);
            startClientThread(info, in, out);
        }

    }

    /*
     * The loop/thread that each client will individually run
     */

    private void startClientThread(ClientInfo info, BufferedReader in, PrintWriter out) throws IOException, InterruptedException {
        // create new thread
        new Thread(() -> {
            try {
                out.println("ACCEPTED: clientUsername: " + info.getClientUsername());
                info.recievedFromClientsQueue.clear();
                info.sendToClientQueue.clear();
                info.clientSocket.setSoTimeout(1);

                if(gameState == GameState.POLLING){
                    info.queueSendMessage("QUESTION" + ClientQuestion.serialize(ClientQuestion.convertQuestion(questionList[currentQuestion], currentQuestion)));
                }
                while (true) {
                    // send message first
                    while(!info.sendToClientQueue.isEmpty()) {
                        // System.out.println("sending: " + info.sendToClientQueue.peek());
                        out.println(info.sendToClientQueue.poll());
                    }

                    // check if client has sent a message
                    try {
                        try {
                            String message = in.readLine(); // read the message
                        
                            if(message == null)
                                throw new SocketException();

                        info.recievedFromClientsQueue.add(message); // put message into queue to be read by gameloop
                        System.out.println("Client says: " + message);

                        Thread.sleep(10);
                        continue;
                    } catch(SocketTimeoutException e ) {
                        // wait one second to readline if timeout in 1 second just move on
                    }
                    
                    Thread.sleep(10);
                } catch(SocketException e) {
                    System.out.println(RED + "Socket Exception on client "+ info.getClientUsername() + RESET);
                    info.setActive(false);
                    break;
                }

                    // Thread.sleep(10);
                }
                
            } catch (IOException | InterruptedException e) { 
                info.setActive(false); // TODO logic is not throwing an error

                e.printStackTrace();
                // will end thread if any exception
                // TODO decide if all exceptions should quit or if say io exception should keep going
            }
        }).start();
        
    }

    public void runGameLoop() throws InterruptedException, IOException {
        // packets constantly and do nothing with them if not polling gamestate
        new Thread(() -> {
            createUDPPollingThread();
        }).start();

        // TODO process for managing when to stop waiting for clients
        gameState = GameState.WAITING_FOR_PLAYERS;
        System.out.println(MAGENTA + "Currently waiting for players to join..." + RESET);

        while(clientSockets.size() == 0){
            Thread.sleep(1000);
        }

        int secondsForJoin = 10;
        System.out.println(GREEN + "At least one client has joined, waiting for additional clients to join for " + secondsForJoin + " seconds..." + RESET);
        Thread.sleep(secondsForJoin * 1000);
        
        
        // Thread for polling, stays constantly open because it will just receive
 
        // TODO index out of bound cause we dont check when out of questions
        int numQuestion = questionList.length; // TODO implement
        while (true) {
            // switch game state to polling waiting for clients to buzz
            gameState = GameState.POLLING;
            
            // sending question
            sendQuestion();

            // waiting for 15 seconds
            System.out.println(GREEN + "Polling for 15 seconds..." + RESET);
            long buzzTime = 15000;
            Thread.sleep(buzzTime);
            gameState = GameState.CLIENT_ANSWERING;
            // poll.join();
            System.out.println(GREEN + "Polling done, moving to client answering..." + RESET);

            // parse queue for who answered first
            if (UDPMessageQueue.isEmpty()) {
                System.out.println(RED + "No clients buzzed, not showing answers next question..." + RESET);
                // TODO what to do when no clients answered; send "next" move to next question
                sendNext();
                continue;
            } else {
                // split on '$'
                System.out.println("Splitting " + UDPMessageQueue.peek());
                String[] parts = UDPMessageQueue.poll().split("\\$");
                System.out.println("Parts: " + Arrays.toString(parts));

                String firstClientID = parts[0]; // first id in queue
                long minTime = Long.valueOf(parts[1]); // first timestamp in queue

                // find first client who buzzed
                while (!UDPMessageQueue.isEmpty()) {
                    parts = UDPMessageQueue.poll().split("\\$");
                    String clientUsername = parts[0];
                    long timeStamp = Long.parseLong(parts[1]);
                    if (timeStamp < minTime && clientSockets.get(clientUsername).isAlive()) {
                        minTime = timeStamp;
                        firstClientID = clientUsername;
                    }
                }
                // response with "ack" for first client and "negative-ack" for others
                for (String clientUsername : clientSockets.keySet()) {
                    ClientInfo info = clientSockets.get(clientUsername);
                    if (clientUsername.equals(firstClientID)) {
                        info.queueSendMessage("ack");
                    } else {
                        info.queueSendMessage("negative-ack");
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
                if(firstClient == null) {
                    System.out.println("Null first client moving on");

                } else if (!firstClient.recievedFromClientsQueue.isEmpty()) {
                    String response = firstClient.recievedFromClientsQueue.poll();
                    System.out.println("Client answered: " + response);

                    int index = -1;
                    try{
                        index = Integer.parseInt(response);

                        if(index == questionList[currentQuestion].getCorrectQuestionIndex()){
                            firstClient.queueSendMessage("correct");
                            firstClient.score += 10;
                        }
                        else{
                            firstClient.queueSendMessage("wrong");
                            firstClient.score -= 10;
                        }
                    }
                    catch(NumberFormatException e){
                        System.out.println(RED + "Incorrect format of Client response; response ignored no points lost..." + RESET);
                    }

                } else {
                    System.out.println("Client did not answer");
                    firstClient.queueSendMessage("none");
                    firstClient.score -= 20;
                }
            }
            gameState = GameState.SHOWING_ANSWERS;
            System.out.println("Showing answers...");
            int timeToShowAnswers = 5000;
            Thread.sleep(timeToShowAnswers);

            // TODO show answers

            // Send "next question" message out to clients
            sendNext();

        }
    }

    private void sendNext() {
        for (String clientUsername : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientUsername);
            info.queueSendMessage("next");
        }
        currentQuestion++;
    }

    private void sendQuestion() {
        if(currentQuestion >= questionList.length){
            System.out.println("No more questions, onto the final scores");
            // TODO move to final gamestate
        }
        else{
            System.out.println(questionList[currentQuestion].getQuestionText());
            System.out.println("Answers: "+ Arrays.toString(questionList[currentQuestion].getAnswers()));
            for (String clientUsername : clientSockets.keySet()) {
                ClientInfo info = clientSockets.get(clientUsername);
                info.queueSendMessage("QUESTION" + ClientQuestion.serialize(ClientQuestion.convertQuestion(questionList[currentQuestion], currentQuestion)));
                // info.out.println(new ClientQuestion(questionList[currentQuestion].getQuestionText(), questionList[currentQuestion].getAnswers()));
            }
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
