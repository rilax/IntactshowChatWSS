package com.intactshow.chat;

import org.java_websocket.WebSocket;

public class WSClient {
    public WebSocket client;
    public int userID = 0;
    public boolean isAuth = false;

    public WSClient(WebSocket client){
        this.client = client;
    }

}
