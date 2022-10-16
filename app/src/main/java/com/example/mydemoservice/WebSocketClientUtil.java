package com.example.mydemoservice;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class WebSocketClientUtil extends WebSocketClient {

    public WebSocketClientUtil(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.i("WebSocketClientUtil", "onOpen()");

    }

    @Override
    public void onMessage(String message) {
        Log.i("WebSocketClientUtil", "onMessage()");

    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i("WebSocketClientUtil", "onClose()");

    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
        Log.i("WebSocketClientUtil", "onError()");

    }
}
