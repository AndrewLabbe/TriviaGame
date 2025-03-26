import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientInfo {
    private String clientID;
    private Socket clientSocket;
    BufferedReader in;
    PrintWriter out;

    public ClientInfo(String clientID, Socket clientSocket, BufferedReader in, PrintWriter out) {
        this.clientID = clientID;
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
    }

}
