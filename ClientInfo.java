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
    PrintWriter out;
    private int score = 0;

    public ClientInfo(String clientID, Socket clientSocket, BufferedReader in, PrintWriter out) {
        this.clientID = clientID;
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

}
