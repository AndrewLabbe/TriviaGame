import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientInfo {
    public enum GameState {
        WAITING_FOR_PLAYERS,
        POLLING,
        CLIENT_ANSWERING,
        SHOWING_ANSWERS,
    }

    private String clientUsername;
    public Socket clientSocket;
    

    // public queue toClientQueue
    // public queue fromClientQueue

    public ConcurrentLinkedQueue<String> sendToClientQueue = new ConcurrentLinkedQueue<String>();
    public ConcurrentLinkedQueue<String> recievedFromClientsQueue = new ConcurrentLinkedQueue<String>();

    public int score = 0;


    private boolean isAlive = false;

    private String TCPIP;
    private int TCPPort;

    public ClientInfo(String clientUsername, Socket clientSocket, String ip, int TCPPort) {
        this.clientUsername = clientUsername;
        this.clientSocket = clientSocket;
        this.isAlive = true;

        this.TCPIP = ip;
        this.TCPPort = TCPPort;
    }

    public void queueSendMessage(String message) {
        sendToClientQueue.add(message);
    }

    public String getClientUsername() {
        return clientUsername;
    }

    public String getTCPIP() {
        return TCPIP;
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

    public boolean isARejoin(String clientID, String ip) {
        System.out.println("compared clientid " + clientID);
        return this.TCPIP.equals(TCPIP) && this.clientUsername.equals(clientID);
    }

}
