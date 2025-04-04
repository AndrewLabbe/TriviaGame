import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientInfo {

    private String clientUsername;
    

    // public queue toClientQueue
    // public queue fromClientQueue

    public ConcurrentLinkedQueue<String> sendToClientQueue = new ConcurrentLinkedQueue<String>();
    public ConcurrentLinkedQueue<String> recievedFromClientsQueue = new ConcurrentLinkedQueue<String>();

    public int score = 0;


    private boolean isAlive = false;

    private String TCPIP;

    public ClientInfo(String clientUsername, String TCPIP) {
        this.clientUsername = clientUsername;
        this.isAlive = true;

        this.TCPIP = TCPIP;
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

    public boolean isUsernameIPMatch(String clientID, String ip) {
        System.out.println("compared clientid " + clientID);
        return this.TCPIP.equals(TCPIP) && this.clientUsername.equals(clientID);
    }

}
