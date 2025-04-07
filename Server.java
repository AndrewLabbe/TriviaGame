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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

    private static ServerQuestion[] questionList;

    public int currentIDIteration = 0;

    public enum GameState {
        WAITING_FOR_PLAYERS, POLLING, CLIENT_ANSWERING, SHOWING_ANSWERS, FINAL_SCORES
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
                    if (questionIndex != currentQuestion) {
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

        // Check if client already has existed

        // assign client ID
        System.out.println("Client says: " + in.readLine());

        // Client will send username as second send
        String clientUsername = in.readLine();
        // out.println(info.getClientID());

        // Validate username/check if is a reconnect

        // is the username valid format
        if (clientUsername == null || clientUsername.equals("")) {
            out.println("REJECT: username empty/null please provide a username.");
            System.out.println(RED + "Client sent empty username, rejecting connection" + RESET);
            return;
        }
        // Only allow alphanumeric characters
        if (!clientUsername.matches("[a-zA-Z0-9]+")) {
            out.println("REJECT: username invalid, only letters and numbers allowed in username.");
            System.out.println(RED + "Client sent username has symbols/non alphanumeric characters, rejecting connection" + RESET);
            return;
        }

        if (clientSockets.containsKey(clientUsername)) {
            ClientInfo existingClient = clientSockets.get(clientUsername);
            if (existingClient.isAlive()) {
                out.println("REJECT: username actively already in use. Please use a different username.");
                System.out.println(RED + "Client sent username already in use, rejecting connection" + RESET);
                return;
            } else {
                // Rejoin with same username and that client is no longer alive
                // allow rejoin if IP matches as well
                if (existingClient.isUsernameIPMatch(clientUsername, IP)) {
                    // rejoin
                    System.out.println(GREEN + "Client has preivously connected, reinitializing client state..." + RESET);
                    existingClient.setActive(true);
                    // client socket changes if rejoin
                    startClientThread(existingClient, in, out, clientSocket);
                    return; // exit the method to avoid adding a new client
                } else {
                    // IP does not match cannot rejoin
                    out.println("REJECT: This username is in use but inactive. Only client on original IP can rejoin with this username.");
                    System.out.println(RED + "Client sent username already in use, rejecting connection" + RESET);
                    return;
                }
            }
        }

        // if reaches here then know its a new user as rejoins return on handled
        ClientInfo info = new ClientInfo(clientUsername, IP);
        clientSockets.put(clientUsername, info);
        System.out.println(GREEN + "New client connected with ID: " + clientUsername + RESET);
        startClientThread(info, in, out, clientSocket);
    }

    /*
     * The loop/thread that each client will individually run
     */

    private void startClientThread(ClientInfo info, BufferedReader in, PrintWriter out, Socket clientSocket) throws IOException, InterruptedException {
        // create new thread
        new Thread(() -> {
            // TODO There are 3 trys for socket exceptions
            try {
                out.println("ACCEPTED: clientUsername: " + info.getClientUsername());
                // if client just joined then clear any unresolved messages as they are only relevant while active
                info.recievedClientAnswersQueue.clear();
                info.sendToClientQueue.clear();
                clientSocket.setSoTimeout(1); // allows us to use readln without it blocking forever
                // Implemented to solve server knowing when client is dead

                // wipeQueuedAnswered(); this should not be called here as then *all clients would be wiped every time a client joined late or rejoined
                // answers will be wiped in game loop regardless of whether client is alive or dead before polling which covers this case
                if (gameState == GameState.POLLING) {
                    info.queueSendMessage("LATE QUESTION" + ClientQuestion.serialize(ClientQuestion.convertQuestion(questionList[currentQuestion], currentQuestion)));
                }

                while (true) {
                    // send queued messages to client
                    while (!info.sendToClientQueue.isEmpty())
                        out.println(info.sendToClientQueue.poll());

                    // check if client has sent a message
                    try {
                        try {
                            String message = in.readLine(); // read the message

                            if (message == null)
                                throw new SocketException();

                            info.recievedClientAnswersQueue.add(message); // put message into queue to be read by gameloop
                            System.out.println("Client says: " + message);

                            Thread.sleep(10);
                            continue;
                        } catch (SocketTimeoutException e) {
                            // wait one second to readline if timeout in 1 second just move on
                        }

                        Thread.sleep(10);
                    } catch (SocketException e) {
                        System.out.println(RED + "Socket Exception on client " + info.getClientUsername() + RESET);
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

        while (clientSockets.size() == 0) {
            Thread.sleep(1000);
        }

        int secondsForJoin = 10;
        System.out.println(GREEN + "At least one client has joined, waiting for additional clients to join for " + secondsForJoin + " seconds..." + RESET);
        Thread.sleep(secondsForJoin * 1000);

        // Thread for polling, stays constantly open because it will just receive

        while (true) {
            // switch game state to polling waiting for clients to buzz
            gameState = GameState.POLLING;

            // sending question
            wipeQueuedAnswered(); // in case some answer got lost and not processed ie arrived late clear b4 starting polling
            // a queued answer at this point could not be possibly valid at this point
            sendQuestion();

            System.out.println(GREEN + "Polling for 15 seconds..." + RESET);
            long buzzTime = 15000;
            Thread.sleep(buzzTime);

            gameState = GameState.CLIENT_ANSWERING;
            // poll.join();
            System.out.println(GREEN + "Polling done, moving to client answering..." + RESET);

            // No-one buzzed
            if (UDPMessageQueue.isEmpty()) {
                System.out.println(RED + "No clients buzzed, not showing answers next question..." + RESET);
                sendNext();
                // sends next when restarts loop will send new question then
                continue;
            } else {
                // loop thru queued messages until found first buzz of living clients
                ClientInfo firstClient = null; // fastest client buzz
                long minTime = Long.MAX_VALUE; // fastest buzz timestamp
                while (!UDPMessageQueue.isEmpty()) {
                    String message = UDPMessageQueue.poll();

                    String[] parts = message.split("\\$");
                    String clientUsername = parts[0];

                    ClientInfo client = clientSockets.get(clientUsername);
                    if (!client.isAlive())
                        continue; // if client is dead skip
                    // client is alive

                    // check if client buzzed sooner
                    long timeStamp = Long.parseLong(parts[1]);
                    if (timeStamp < minTime) {
                        minTime = timeStamp;
                        firstClient = client;
                    }
                }

                if (firstClient == null) {
                    System.out.println(RED + "No clients buzzed, next question (Not showing answer)..." + RESET);
                    sendNext();
                    // sends next when restarts loop will send new question then
                    continue;
                }

                // there is a first client
                // response with "ack" for first client and "negative-ack" for others
                for (String clientUsername : clientSockets.keySet()) {
                    ClientInfo info = clientSockets.get(clientUsername);
                    // if is first client
                    if (clientUsername.equals(firstClient.getClientUsername())) {
                        info.queueSendMessage("ack");
                        System.out.println(GREEN + "Client " + clientUsername + " buzzed first!" + RESET);
                    } else { // else not first client
                        info.queueSendMessage("negative-ack");
                    }
                }
                // wait for 10 seconds to get answer from first client
                System.out.println(CYAN + "Waiting for client " + firstClient.getClientUsername() + " to answer..." + RESET);
                int waitTime = 10000;
                Thread.sleep(waitTime);

                // if recieved an answer
                if (!firstClient.recievedClientAnswersQueue.isEmpty()) {
                    String response = firstClient.recievedClientAnswersQueue.poll();
                    System.out.println("Client answered: " + response);

                    try {
                        int index = Integer.parseInt(response);

                        // if correct
                        if (index == questionList[currentQuestion].getCorrectQuestionIndex()) {
                            firstClient.queueSendMessage("correct");
                            firstClient.score += 10;
                        } else { // wrong
                            firstClient.queueSendMessage("wrong");
                            firstClient.score -= 10;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println(RED + "Format or corruption error from Client answer; response ignored no points lost..." + RESET);
                    }

                } else { // client did not answer
                    System.out.println("Client did not answer");
                    firstClient.queueSendMessage("none");
                    firstClient.score -= 20;
                }
            }

            // give client answer and allow showing for 5 seconds
            gameState = GameState.SHOWING_ANSWERS;
            System.out.println("Showing answers...");
            sendAnswerIndex();
            int timeToShowAnswers = 5000;
            Thread.sleep(timeToShowAnswers);

            // Send "next question" message out to clients
            sendNext();
        }
    }

    private void sendAnswerIndex() {
        for (String clientUsername : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientUsername);
            info.queueSendMessage("correct answer" + questionList[currentQuestion].getCorrectQuestionIndex());
        }
        sendLeaderboardToClients();
    }

    /**
     * Sends the next question to all clients
     * ITERATES CURRENTQUESION INDEX ON METHOD CALL
     */
    private void sendNext() {
        for (String clientUsername : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientUsername);
            info.queueSendMessage("next");
        }
        currentQuestion++;
    }

    /**
     * Sends the current question to all clients
     * If there are no more questions, sends the final scores
     * 
     * @throws InterruptedException
     */
    private void sendQuestion() throws InterruptedException {
        if (currentQuestion >= questionList.length) {
            System.out.println("No more questions, onto the final scores");
            sendLeaderboardToClients();
            // moves to final gamestate
            gameState = GameState.FINAL_SCORES;
            int secToSleep = 20;
            Thread.sleep(secToSleep * 1000);
            // game is done kill all clients
            for (String clientUsername : clientSockets.keySet()) {
                ClientInfo info = clientSockets.get(clientUsername);
                info.queueSendMessage("kill");
            }
            return;
        }
        // there is available question
        System.out.println(questionList[currentQuestion].getQuestionText());
        System.out.println("Answers: " + Arrays.toString(questionList[currentQuestion].getAnswers()));
        for (String clientUsername : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientUsername);
            info.queueSendMessage("QUESTION" + ClientQuestion.serialize(ClientQuestion.convertQuestion(questionList[currentQuestion], currentQuestion)));
        }
    }

    /**
     * Sends the leaderboard to all clients, contains usernames of all clients + scores in order of most to least
     */
    public void sendLeaderboardToClients() {
        ArrayList<ClientInfo> leaderboard = createLeaderboard();
        StringBuilder leaderboardMessage = new StringBuilder("Leaderboard");

        for (int i = 0; i < leaderboard.size(); i++) {
            ClientInfo client = leaderboard.get(i);
            leaderboardMessage.append((i + 1) + ". " + client.getClientUsername() + ": " + client.getScore() + "$");
        }

        String message = leaderboardMessage.toString();

        // Send the leaderboard message to all clients
        for (String clientUsername : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientUsername);
            info.queueSendMessage(message);
        }
    }

    public ArrayList<ClientInfo> createLeaderboard() {
        ArrayList<ClientInfo> scores = new ArrayList<ClientInfo>();

        // Loop through all clients to create a list out of map
        for (String clientUsername : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientUsername);
            scores.add(info);
        }

        // Define comparator to sort by score descending
        Comparator<ClientInfo> c = new Comparator<ClientInfo>() {
            @Override
            public int compare(ClientInfo a, ClientInfo b) {
                return Integer.compare(b.getScore(), a.getScore()); // descending
            }
        };

        scores.sort(c);
        return scores; // return the sorted list
    }

    /**
     * Wipes the queued answered for all clients
     */
    public void wipeQueuedAnswered() {
        for (String clientUsername : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientUsername);
            info.recievedClientAnswersQueue.clear();
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
