import java.net.Socket;
import java.util.PriorityQueue;

public class ClientInfo {
    public enum GameState {
        WAITING_FOR_PLAYERS,
        POLLING,
        CLIENT_ANSWERING,
        SHOWING_ANSWERS,
    }

    private String clientID;
    public Socket clientSocket;
    

    // public queue toClientQueue
    // public queue fromClientQueue

    public PriorityQueue<String> sendToClientQueue = new PriorityQueue<String>();
    public PriorityQueue<String> recievedFromClientsQueue = new PriorityQueue<String>();

    public int score = 0;


    private boolean isAlive = false;

    private String TCPIP;
    private int TCPPort;

    public ClientInfo(String clientID, Socket clientSocket, String ip, int TCPPort) {
        this.clientID = clientID;
        this.clientSocket = clientSocket;
        this.isAlive = true;

        this.TCPIP = ip;
        this.TCPPort = TCPPort;
    }

    public void queueSendMessage(String message) {
        sendToClientQueue.add(message);
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

    public boolean isARejoin(String clientID, String ip) {
        System.out.println("compared clientid " + clientID);
        return this.TCPIP.equals(TCPIP) && this.clientID.equals(clientID);
    }

}
