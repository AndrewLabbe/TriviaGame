import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server {
    private int port;

    private Map<String, ClientInfo> clientSockets = new HashMap<>();

    private ConcurrentLinkedQueue messageQueue = new ConcurrentLinkedQueue<>();

    public Server(int port) {
        this.port = port;
    }

    public void createConnectionThread() throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket(this.port);
        while (true) {
            // create a server socket on port number 9090
            System.out.println("Server is running and waiting for client connection...");

            // Accept incoming client connection
            Socket clientSocket = serverSocket.accept();
            System.out.println("Client connected!");

            createClientTCPThread(clientSocket);
        }
    }

    private void createClientTCPThread(Socket clientSocket) throws InterruptedException, IOException {
        // TODO establish new thread b4 listen/send while loop
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
        // out.println("Hello client, you are connected!");
        out.println(clientID);

        // create new thread
        new Thread(() -> {
            try {
                clientThread(info);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void clientThread(ClientInfo info) throws IOException, InterruptedException {
        while (true) {
            // -- sending -- => out.println(...);
            // -- listening -- => String message = in.readLine();
            System.out.println("Client says: " + info.in.readLine());
            info.out.println("Hello from server!");
            Thread.sleep(10);
        }
    }

    public static void main(String args[]) throws IOException, InterruptedException {
        Server server = new Server(9090);
        server.createConnectionThread();
    }
}
