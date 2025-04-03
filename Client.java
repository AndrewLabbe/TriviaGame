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
    private int clientPortTCP;
    private String serverIP;
    private int serverPortTCP;
    private int serverPortUDP;

    private DatagramSocket clientUDPSocket;

    private BufferedReader in;
    private PrintWriter out;

    private String clientID;
    private int score = 0;

    private String username;

    public Client(String serverIP, int serverPortTCP, int serverPortUDP, String username) {
        this.clientPortTCP = clientPortTCP;
        this.serverIP = serverIP;
        this.serverPortTCP = serverPortTCP;
        this.serverPortUDP = serverPortUDP;

        this.username = username;

        // create udp socket
        try {
            this.clientUDPSocket = new DatagramSocket();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
        out.println(this.username);
        String message = in.readLine();
        if(message == null) {
            System.out.println("Client may have disconnected during initialization, skipping.");
            return;
        }
        if(message.toLowerCase().startsWith("REJECT")) {
            System.out.println("Username exists/username active");
            System.exit(-1);
        }
        // assign client id
        this.clientID = message;
        System.out.println("Server assigned Client ID: " + this.clientID);

        // listening/sending thread
        // Send message to the server
        out.println("Hello from client " + this.clientID + "!");

        clientGameLoop();
    }

    private void clientGameLoop() {
        try {
            // first step is waiting for client
            processResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buzz() throws UnknownHostException {
        long timeStamp = System.currentTimeMillis();
        String message = clientID + "$" + timeStamp;
        byte[] buffer = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        // send pack with content of timestamp
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverIP),
                this.serverPortUDP);
        // send packet
        try {
            System.out.println("Sending Buzz Packet");
            this.clientUDPSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO Possible input {"ack", "negative-ack", "correct", "wrong", "next"}
    private void processResponse() throws IOException, InterruptedException, ClassNotFoundException {
        while (true) {
            if (this.in.ready()) {
                String serverMessage = this.in.readLine();
                if (serverMessage.equals("ack")) {
                    System.out.println(serverMessage + ": You buzzed first");
                    // TODO allowed to answer
                } else if (serverMessage.equals(serverMessage + ": negative-ack")) {
                    System.out.println("Server says: " + serverMessage);
                } else if (serverMessage.equals("correct")) {
                    System.out.println("Got question correct +10");
                    score += 10;
                } else if (serverMessage.equals("wrong")) {
                    System.out.println("Got question wrong -10");
                    score -= 10;
                }else if (serverMessage.equals("none")) {
                    System.out.println("Did not answer -20");
                    score -= 20;
                }
                 else if (serverMessage.equals("next")) {
                    System.out.println("Moving to next question...");
                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(2000);
                                System.out.println("Buzzing in...");
                                buzz();
                                // TODO Allow buzing in
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    buzz();
                } else { // TODO add a try catch to make sure its a question
                    // assume its a question
                    printServerQuestion(ClientQuestion.deserialize(serverMessage));
                }
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
        if(args.length < 1) {
            System.out.println("must provide username as arg, Client <username>");
            System.exit(-1);
        }

        String username =  args[0];

        Client client = new Client("localhost", 9080, 9090, username);
        client.establishConnectToServer();
    }

}
