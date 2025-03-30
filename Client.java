import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Client {
    private int clientPort;
    private String serverIP;
    private int serverPortTCP;
    private int serverPortUDP;

    private DatagramSocket clientSocket;

    private BufferedReader in;
    private PrintWriter out;

    private String clientID;
    private int score = 0;

    public Client(int clientPort, String serverIP, int serverPortTCP, int serverPortUDP) {
        this.clientPort = clientPort;
        this.serverIP = serverIP;
        this.serverPortTCP = serverPortTCP;
        this.serverPortUDP = serverPortUDP;

        // create udp socket
        try {
            this.clientSocket = new DatagramSocket();
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

        // Setup output stream to send data to the server
        this.out = new PrintWriter(socket.getOutputStream(), true);

        // Setup input stream to receive data from the server
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // handshake
        out.println("Hello I would like to connect");
        // assign client id
        this.clientID = in.readLine();
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
        byte[] buffer = ByteBuffer.allocate(Long.BYTES).putLong(timeStamp).array();
        // send pack with content of timestamp
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverIP),
                this.serverPortUDP);
        // send packet
        try {
            System.out.println("Sending Buzz Packet");
            this.clientSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO Possible input {"ack", "negative-ack", "correct", "wrong", "next"}
    private void processResponse() throws IOException, InterruptedException {
        while (true) {
            if (this.in.ready()) {
                String serverMessage = this.in.readLine();
                if (serverMessage.equals("ack")) {
                    System.out.println("Server says: " + serverMessage);
                } else if (serverMessage.equals("negative-ack")) {
                    System.out.println("Server says: " + serverMessage);
                } else if (serverMessage.equals("correct")) {
                    System.out.println("Got question correct +10");
                } else if (serverMessage.equals("wrong")) {
                    System.out.println("Got question wrong -10");
                } else if (serverMessage.equals("next")) {
                    System.out.println("Moving to next question...");
                    new Thread() {
                        public void run() {
                            try {
                                Thread.sleep(2000);
                                System.out.println("Buzzing in...");
                                buzz();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }.start();
                    buzz();
                } else {
                    // assume its a question
                    System.out.println("Question: " + serverMessage);
                }
            }
            Thread.sleep(10);
        }
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        Client client = new Client(9000, "localhost", 9080, 9090);
        client.establishConnectToServer();
    }

}
