package com.example.mydemoservice;

import android.util.Log;
import android.widget.Toast;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.handshake.ServerHandshake;

import java.io.Serializable;
import java.net.URI;

public class MeyDemoWebSocketClient extends WebSocketClient implements Serializable {

    public MeyDemoWebSocketClient(URI server_uri) {
        super(server_uri,new Draft_6455());
    }

    @Override
    public void onOpen(ServerHandshake hand_shake_data) {
        Log.i("MeyDemoWebSocketClient", "onOpen()");
    }

    @Override
    public void onMessage(String message) {
        Log.i("MeyDemoWebSocketClient", "onMessage()");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.i("MeyDemoWebSocketClient", "onClose()");
    }

    @Override
    public void onError(Exception ex) {
        Log.i("MeyDemoWebSocketClient", "onError()");
    }
}
