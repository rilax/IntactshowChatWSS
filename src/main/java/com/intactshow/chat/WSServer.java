package com.intactshow.chat;

import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

public class WSServer extends WebSocketServer {

    public static ArrayList<WSClient> client = new ArrayList<>();
    private ApiClient apiClient = new ApiClient();

    public WSServer(InetSocketAddress address){
        super(address);
    }
    /*
    public WSServer(int port) throws UnknownHostException { super(new InetSocketAddress(port)); }
    public WSServer(int port, Draft_6455 draft) {
        super(new InetSocketAddress(port), Collections.<Draft>singletonList(draft));
    }
*/
    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        System.out.println("Client connect");
        client.add(new WSClient(webSocket));
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        System.out.println("client Disconnect");
        ApiClient.exitUser(getClassClient(webSocket));
        client.remove(getIndexClient(webSocket));
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        apiClient.apiMessage(getClassClient(webSocket), s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        webSocket.close();
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("Run server");
    }

    public static WSClient getClient(int id){
        for (int i = 0; i < client.size(); i++){
            if(client.get(i).userID == id){
                return client.get(i);
            }
        }
        return null;
    }

    public static ArrayList<WSClient> getClients(int id){
        ArrayList<WSClient> list = new ArrayList<>();
        for (int i = 0; i < client.size(); i++){
            if(client.get(i).userID == id){
                list.add(client.get(i));
            }
        }
        return list;
    }

    public static int getClientID(WebSocket ws){
        for (int i = 0; i < client.size(); i++){
            if(client.get(i).client.equals(ws)){
                return client.get(i).userID;
            }
        }
        return 0;
    }


    public static boolean getUserStatus(int id){
        for (int i = 0; i < client.size(); i++){
            if(client.get(i).userID == id){
                return true;
            }
        }
        return false;
    }



    public static int getIndexClient(WebSocket ws){
        int index = 0;
        for (int i = 0; i < client.size(); i++){
            if(client.get(i).client.equals(ws)){
                index = i;
                break;
            }
        }
        return index;
    }

    public static WSClient getClassClient(WebSocket ws){
        for (int i = 0; i < client.size(); i++){
            if(client.get(i).client.equals(ws)){
                return client.get(i);
            }
        }
        return null;
    }
}
