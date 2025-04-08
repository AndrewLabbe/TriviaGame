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
            System.out.println(MAGENTA + "Starting a UDP Thread..." + RESET);
            // set timeout for reveiving packets so it is not completely blocking and importantly will be able to exit when quiz complete
            int secondsTimeout = 3;
            UDPDatagramSocket.setSoTimeout(secondsTimeout * 1000);
            // quit UDP thread when quiz is complete
            while (gameState != GameState.FINAL_SCORES) {
                try {
                    // create datagram packet using incoming data as paramater
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    UDPDatagramSocket.receive(incomingPacket);

                    // if recieve while not polling skip
                    if (gameState != GameState.POLLING) {
                        System.out.println(YELLOW + "Game state is not polling, ignoring packet that came in" + RESET);
                        continue;
                    }

                    byte[] data = incomingPacket.getData();
                    // try to convert to long
                    String baseMessage = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                    String validString = "";

                    // bytes include hidden characters, manual parsing needed
                    for (int i = 0; i < baseMessage.length(); i++) {
                        char currentChar = baseMessage.charAt(i);

                        // Check if the character is '$' or a digit
                        if (currentChar == '$' || Character.isDigit(currentChar) || Character.isLetter(currentChar)) {
                            // Process the character (here we print it)
                            validString += currentChar;
                        }
                    }

                    String[] parts = validString.split("\\$");

                    String username = parts[0];
                    if (!clientSockets.containsKey(username) || clientSockets.get(username) == null || !clientSockets.get(username).isAlive())
                        return;
                    String item = username + "$" + parts[1];

                    // when add to queue, use clientUsername and timestamp
                    UDPMessageQueue.add(item);
                    System.out.println(CYAN + "Client buzzed: " + item + RESET);

                    Thread.sleep(5);
                } catch (SocketTimeoutException e) {
                    // Do nothing
                    // System.out.println("UDP Socket Timeout, no packets received in " + secondsTimeout + " seconds");
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                    Thread.sleep(1000);
                }

            }
        } catch (Exception e) {
            throw new RuntimeException("Listening process was interupted", e);
        }
        System.out.println(MAGENTA + "ending udp thread" + RESET);
    }

    /*
     * Always accepting new TCP connections
     * -------------------------------------
     * Loops forever:
     * blocks until client connects over TCP
     * Asks createClientTCPThread to setup TCP thread
     * restarts loop blocks until client connects over TCP...
     */
    public void createTCPConnectionCreationThread() throws IOException, InterruptedException {
        try {
            // set timeout .accept() is not blocking forever, and it can check in the while loop if quiz ended
            int secondsTimeout = 5;
            serverTCPSocket.setSoTimeout(secondsTimeout * 1000);
            System.out.println(MAGENTA + "Starting TCP connection connection listening thread..." + RESET);
            while (gameState != GameState.FINAL_SCORES) {
                try {
                    // wait for a client to request to connect

                    Socket clientSocket = this.serverTCPSocket.accept();
                    System.out.println("A client connected!");

                    createClientTCPThread(clientSocket);
                } catch (SocketTimeoutException e) {
                    // Do nothing, just unblocks loop for a moment to check if done
                    // System.out.println("TCP Socket Timeout, no clients connected refreshing blocking");
                    Thread.sleep(100);
                }
            }
        } catch (IOException io) {
            io.printStackTrace();
        }

        System.out.println(MAGENTA + "Ending TCP connection creation thread" + RESET);
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

        String IP = clientSocket.getInetAddress().getHostAddress();
        // Check if client already has existed

        // assign client ID

        // Client will send username as second send
        String clientUsername = in.readLine();

        // Validate username/check if is a reconnect

        // is the username valid format
        if (clientUsername == null || clientUsername.equals("")) {
            out.println("REJECT: username (" + clientUsername + ") empty/null please provide a username.");
            System.out.println(RED + "Client sent empty username (" + clientUsername + "), rejecting connection" + RESET);
            return;
        }
        // Only allow alphanumeric characters
        if (!clientUsername.matches("[a-zA-Z0-9]+")) {
            out.println("REJECT: username (" + clientUsername + ") invalid, only letters and numbers allowed in username.");
            System.out.println(RED + "Client sent username (" + clientUsername + ") has symbols/non alphanumeric characters, rejecting connection" + RESET);
            return;
        }

        if (clientSockets.containsKey(clientUsername)) {
            ClientInfo existingClient = clientSockets.get(clientUsername);
            if (existingClient.isAlive()) {
                out.println("REJECT: username (" + clientUsername + ") actively already in use. Please use a different username.");
                System.out.println(RED + "Client sent username (" + clientUsername + ") already in use, rejecting connection" + RESET);
                return;
            } else {
                // Rejoin with same username and that client is no longer alive
                // allow rejoin if IP matches as well
                if (existingClient.isUsernameIPMatch(clientUsername, IP)) {
                    // rejoin
                    System.out.println(GREEN + "Client (" + clientUsername + ") has preivously connected, reinitializing client state..." + RESET);
                    existingClient.setActive(true);
                    // client socket changes if rejoin
                    startClientThread(existingClient, in, out, clientSocket);
                    return; // exit the method to avoid adding a new client
                } else {
                    // IP does not match cannot rejoin
                    out.println("REJECT: This username (" + clientUsername + ") is in use but inactive. Only client on original IP can rejoin with this username.");
                    System.out.println(RED + "Client sent username (" + clientUsername + ") already in use, rejecting connection" + RESET);
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
            try {
                out.println("Starting thread for clientUsername: " + info.getClientUsername());
                // if client just joined then clear any unresolved messages as they are only relevant while active

                // info.recievedClientAnswersQueue.clear();

                // info.sendToClientQueue.clear();

                clientSocket.setSoTimeout(1); // allows us to use readln without it blocking forever
                // Implemented to solve server knowing when client is dead

                // answers will be wiped in game loop regardless of whether client is alive or dead before polling which covers this case
                if (gameState == GameState.POLLING) {
                    info.queueSendMessage("LATE QUESTION" + ClientQuestion.serialize(ClientQuestion.convertQuestion(questionList[currentQuestion], currentQuestion)));
                } else if (gameState == GameState.CLIENT_ANSWERING) {
                    System.out.println("Client joined during answering: " + info.getClientUsername());
                    info.queueSendMessage("ANSWERING" + ClientQuestion.serialize(ClientQuestion.convertQuestion(questionList[currentQuestion], currentQuestion)));
                } else if (gameState == GameState.SHOWING_ANSWERS) {
                    info.queueSendMessage("SHOWING");
                }

                sendLeaderboardTo(info);

                int secondsBetweenPings = 2;
                long lastPing = -1;
                while (true) {
                    // if time to ping then ping
                    long timeSinceLastPing = System.currentTimeMillis() - lastPing;
                    if (timeSinceLastPing > secondsBetweenPings * 1000) {
                        out.println("ping");
                        lastPing = System.currentTimeMillis();
                    }
                    // send queued messages to client
                    while (!info.sendToClientQueue.isEmpty()) {
                        String sendMessage = info.sendToClientQueue.poll();
                        out.println(sendMessage);
                        if (sendMessage.equalsIgnoreCase("kill")) {
                            break; // if client is being killed then also exit the thead
                        }
                    }

                    // check if client has sent a message
                    try {
                        try {
                            String message = in.readLine(); // read the message

                            if (message == null) {
                                throw new SocketException();
                            }

                            info.recievedClientAnswersQueue.add(message); // put message into queue to be read by gameloop
                            // System.out.println("Client says: " + message);

                            Thread.sleep(10);
                            continue;
                        } catch (SocketTimeoutException e) {
                            // wait one second to readline if timeout in 1 second just move on
                        }

                        Thread.sleep(10);
                    } catch (SocketException e) {
                        System.out.println(RED + "Socket Exception on client, so time to kill " + info.getClientUsername() + RESET);
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
            System.out.println(YELLOW + "Disconnecting client thread for client: " + info.getClientUsername() + RESET);
            try {
                System.out.println("Closing Streams");
                in.close();
                out.close();
                clientSocket.shutdownInput();
                clientSocket.shutdownOutput();
                clientSocket.close();
                System.out.println("Successfully Closed");
            } catch (IOException e) {
                System.out.println("Socket is closed");
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
        System.out.println(GREEN + "Currently waiting for players to join..." + RESET);

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

            // send question quit if out of questions
            if (sendQuestion()) {
                System.out.println(GREEN + "Question sent to clients, waiting for buzz..." + RESET);
            } else {
                System.out.println(RED + "No more questions, moving to final scores..." + RESET);
                break; // no more questions
            }

            long buzzTimeSeconds = 15;
            System.out.println(GREEN + "Polling for " + buzzTimeSeconds + " seconds..." + RESET);
            Thread.sleep(buzzTimeSeconds * 1000);

            gameState = GameState.CLIENT_ANSWERING;
            // poll.join();
            System.out.println(GREEN + "Polling done, moving to client answering..." + RESET);

            // No-one buzzed
            if (UDPMessageQueue.isEmpty()) {
                System.out.println(RED + "No clients buzzed, next question (Not showing answer)..." + RESET);
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

                if (firstClient == null)
                    continue;
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
                // THIS NULL CHECK IF FOR IF NO **ALIVE** CLIENTS BUZZED
                if (firstClient == null) {
                    System.out.println(RED + "No alive clients buzzed, next question (Not showing answer)..." + RESET);
                    sendNext();
                    continue;
                }
                int waitSecondsTimeAnswering = 10;
                // wait for 10 seconds to get answer from first client
                System.out.println(CYAN + "Waiting for client " + firstClient.getClientUsername() + " to answer (" + waitSecondsTimeAnswering + " seconds)..." + RESET);
                Thread.sleep(waitSecondsTimeAnswering * 1000);

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
            System.out.println(CYAN + "Showing answers (5 seconds)..." + RESET);
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
     * @return true if there are more questions, false if no more questions
     * @throws InterruptedException
     */
    private boolean sendQuestion() throws InterruptedException {
        if (currentQuestion >= questionList.length) {
            System.out.println("No more questions, onto the final scores");
            sendLeaderboardToClients();
            // moves to final gamestate
            gameState = GameState.FINAL_SCORES;
            for (String clientUsername : clientSockets.keySet()) {
                ClientInfo info = clientSockets.get(clientUsername);
                info.queueSendMessage("FINISHED");
            }
            int secToSleep = 20;
            Thread.sleep(secToSleep * 1000);
            // game is done kill all clients
            for (String clientUsername : clientSockets.keySet()) {
                ClientInfo info = clientSockets.get(clientUsername);
                System.out.println("Killing Client: " + clientUsername + " with socket exception");
                info.queueSendMessage("kill");
            }
            return false;
        }
        // there is available question
        System.out.println("=====");
        System.out.println("Question " + (currentQuestion + 1) + ": " + questionList[currentQuestion].getQuestionText());
        System.out.println("Answer options: " + Arrays.toString(questionList[currentQuestion].getAnswers()));
        for (String clientUsername : clientSockets.keySet()) {
            ClientInfo info = clientSockets.get(clientUsername);
            info.queueSendMessage("QUESTION" + ClientQuestion.serialize(ClientQuestion.convertQuestion(questionList[currentQuestion], currentQuestion)));
        }
        System.out.println("=====");
        return true;
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
        System.out.println("----");
        System.out.println(GREEN + "Sent Leaderboard" + RESET);
        System.out.println(message);
        System.out.println("----\n");
    }

    public void sendLeaderboardTo(ClientInfo client) {
        ArrayList<ClientInfo> leaderboard = createLeaderboard();
        StringBuilder leaderboardMessage = new StringBuilder("Leaderboard");

        for (int i = 0; i < leaderboard.size(); i++) {
            ClientInfo cl = leaderboard.get(i);
            leaderboardMessage.append((i + 1) + ". " + cl.getClientUsername() + ": " + client.getScore() + "$");
        }

        String message = leaderboardMessage.toString();
        client.sendToClientQueue.add(message);
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
        // OPTIONAL:
        // serverTCPPort
        // serverUDPPort

        Map<String, String> optionalArgs = new HashMap<>();
        int tcpPort = 9080;
        int udpPort = 9090;
        optionalArgs.put("--tcpport", tcpPort + "");
        optionalArgs.put("--udpport", udpPort + "");
        ArgHandler argHandler = new ArgHandler(args, "OPTIONAL! --tcpport <int:port> --udpport <int:port>", null, optionalArgs);
        int portTCP = 9080;
        int portUDP = 9090;
        try {
            portTCP = Integer.parseInt(argHandler.get("tcpport"));
            portUDP = Integer.parseInt(argHandler.get("udpport"));
        } catch (NumberFormatException e) {
            System.out.println(RED + "Invalid port arg, make sure to format arg as number" + RESET);
        }

        System.out.println(CYAN);
        System.out.println(argHandler.toString().replace("--", ""));
        System.out.println(RESET);

        Server server = new Server(portTCP, portUDP);

        new Thread(() -> {
            try {
                server.createTCPConnectionCreationThread();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        server.runGameLoop();
    }
}
