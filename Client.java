import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;

public class Client {
    private int clientPort;
    private String serverIP;
    private int serverPortTCP;
    private int serverPortUDP;

    private String clientID;
    private int score = 0;

    public Client(int clientPort, String serverIP, int serverPortTCP, int serverPortUDP) {
        this.clientPort = clientPort;
        this.serverIP = serverIP;
        this.serverPortTCP = serverPortTCP;
        this.serverPortUDP = serverPortUDP;
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
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Setup input stream to receive data from the server
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // handshake
        out.println("Hello I would like to connect");
        // assign client id
        this.clientID = in.readLine();
        System.out.println("Server assigned Client ID: " + this.clientID);

        // listening/sending thread
        while (true) {
            // Send message to the server
            out.println("Hello from client " + this.clientID + "!");

            // Receive response from the server
            String response = in.readLine();
            System.out.println("Server says: " + response);
            Thread.sleep(1000);
        }
    }

    private void buzz() throws UnknownHostException {
        long timeStamp = System.currentTimeMillis();
        byte[] buffer = ByteBuffer.allocate(Long.BYTES).putLong(timeStamp).array();
        // send pack with content of timestamp
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getByName(this.serverIP),
                this.serverPortTCP);
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        Client client = new Client(9000, "localhost", 9080, 9090);
        client.establishConnectToServer();
    }

    private void processResponse() {

    }
}
