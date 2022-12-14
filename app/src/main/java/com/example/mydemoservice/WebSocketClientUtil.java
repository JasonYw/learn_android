package com.example.mydemoservice;

import android.content.Intent;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebSocketClientUtil extends WebSocketClient {

    public WebSocketClientUtil(URI serverUri) {
        super(serverUri,new Draft_6455());
    }


    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i("WebSocketClientUtil:onOpen", handshakedata.getHttpStatusMessage());

    }

    @Override
    public void onMessage(String message) {
        Log.i("WebSocketClientUtil:onMessage", "onMessage:" + message);

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i("WebSocketClientUtil:onClose",reason);
    }


    @Override
    public void onError(Exception ex) {
        Log.i("WebSocketClientUtil:onError", ex.toString());
    }

}
