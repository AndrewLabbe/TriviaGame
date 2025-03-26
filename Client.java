import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private int port;
    private String serverIP;
    private int serverPort;

    private String clientID;

    public Client(int port, String serverIP, int serverPort) {
        this.port = port;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void establishConnectToServer() throws UnknownHostException, IOException, InterruptedException {
        // 1. create the connection, in and out
        Socket socket = new Socket("localhost", 9090);

        // Setup output stream to send data to the server
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        // Setup input stream to receive data from the server
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // handshake
        out.println("Hello I would like to connect");
        // assign client id
        this.clientID = in.readLine();
        System.out.println("Server assigned Client ID: " + this.clientID);

        System.out.println("-- Starting client loop --");

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

    public static void main(String args[]) throws IOException, InterruptedException {
        Client client = new Client(9000, "localhost", 9090);
        client.establishConnectToServer();
    }
}
