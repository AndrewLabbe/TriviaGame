import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientInfo {
    public enum GameState {
        WAITING_FOR_PLAYERS,
        POLLING,
        CLIENT_ANSWERING,
        SHOWING_ANSWERS,
    }

    private String clientID;
    private Socket clientSocket;
    BufferedReader in;

    // TCP Connection print/read
    public PrintWriter out;
    public int score = 0;

    // public queue toClientQueue
    // public queue fromClientQueue

    private boolean isAlive = false;

    private String TCPIP;
    private int TCPPort;

    public ClientInfo(String clientID, Socket clientSocket, BufferedReader in, PrintWriter out, String ip, int port) {
        this.clientID = clientID;
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
        this.isAlive = true;

        this.TCPIP = ip;
        this.TCPPort = port;
    }

    public String getClientID() {
        return clientID;
    }

    public int getScore() {
        return score;
    }

    public void setActive(boolean b){
        this.isAlive = b;
    }

    public boolean isAlive(){
        return this.isAlive;
    }

    public boolean isSameIPPort(String ip, int port) {
        return this.TCPIP.equals(ip) && this.TCPPort == port;
    }

    public boolean isSameIPPort(Socket socket) {
        return isSameIPPort(socket.getInetAddress().getHostAddress(), socket.getPort());
    }

}
