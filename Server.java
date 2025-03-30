import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    public enum GameState {
        WAITING_FOR_PLAYERS,
        POLLING,
        CLIENT_ANSWERING,
        SHOWING_ANSWERS,
    }

    private int currentQuestion = 0;
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

            while (true) {
                if (gameState != GameState.POLLING) {
                    UDPDatagramSocket.close();
                    return;
                }
                try {
                    // create datagram packet using incoming data as paramater
                    DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
                    // accept packet
                    UDPDatagramSocket.receive(incomingPacket);

                    // TODO do something with incoming udp packts
                    byte[] data = incomingPacket.getData();
                    // try to convert to long
                    long timeStamp = ByteBuffer.wrap(data).getLong();
                    System.out.println("Received UDP packet: " + data);

                    // when add to queue, use clientID and timestamp
                    String clientID = incomingPacket.getAddress().getHostAddress() + ":" + incomingPacket.getPort();
                    messageQueue.add(clientID + "$" + timeStamp);
                    Thread.sleep(5);
                } catch (Exception e) {
                    System.out.println("Cannot access network");
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

        String clientIP = clientSocket.getInetAddress().getHostAddress();
        int clientPort = clientSocket.getPort();
        String clientID = clientIP + ":" + clientPort;

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
            // -- sending -- => out.println(...);
            // -- listening -- => String message = in.readLine();
            System.out.println("Client says: " + info.in.readLine());
            info.out.println("Hello from server!");
            Thread.sleep(10);
        }
    }

    public void runGameLoop() throws InterruptedException, IOException {
        // TODO process for managing when to stop waiting for clients
        gameState = GameState.WAITING_FOR_PLAYERS;
        while (true) {
            // TODO send the question

            // wait for clients to buzz
            gameState = GameState.POLLING;
            Thread poll = new Thread() {
                public void run() {
                    createUDPPollingThread();
                }
            };
            poll.start();
            // waiting for 15 seconds
            long buzzTime = 15000;
            Thread.sleep(buzzTime);
            gameState = GameState.CLIENT_ANSWERING;
            poll.join();

            // parse queue for who answered first
            if (messageQueue.isEmpty()) {
                System.out.println("No clients answered");
                // TODO what to do when no clients answered; send "next" move to next question
            } else {
                String[] parts = messageQueue.poll().split("$");
                String firstClientID = parts[0];
                long minTime = Long.parseLong(parts[1]);

                while (!messageQueue.isEmpty()) {
                    parts = messageQueue.poll().split("$");
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

                // new Thread(() -> {
                // try {
                // while (true) {
                // if (firstClient.in.ready()) {
                // String response = firstClient.in.readLine();
                // }
                // Thread.sleep(1000);
                // }
                // } catch (IOException | InterruptedException e) {
                // e.printStackTrace();
                // }
                // });
                Thread.sleep(waitTime);
                if (firstClient.in.ready()) {
                    String response = firstClient.in.readLine();
                    System.out.println("Client answered: " + response);
                    // TODO if correct +10 points
                    firstClient.out.println("Correct! +10");
                    // TODO if wrong -10 points
                    firstClient.out.println("Wrong! -10");

                } else {
                    System.out.println("Client did not answer");
                    firstClient.out.println("No answer! -20");
                    // TODO -20 points
                }
            }
            // TODO show answers

            // TODO move to next question; send "next" to all clients
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

        new Thread(() -> {
            server.createUDPPollingThread();
        }).start();

        server.runGameLoop();
    }
}
