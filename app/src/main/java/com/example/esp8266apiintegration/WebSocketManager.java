package com.example.esp8266apiintegration;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.ArrayList;
import java.util.List;

public class WebSocketManager {
    private static final String WS_URL = "ws://192.168.4.1:81"; // ESP8266 WebSocket port
    private static WebSocketManager instance;

    private OkHttpClient client;
    private WebSocket webSocket;
    private final List<ESPMessageListener> listeners = new ArrayList<>();

    private WebSocketManager() {
        client = new OkHttpClient();
        connect();
    }

    public static WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    private void connect() {
        Request request = new Request.Builder().url(WS_URL).build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                broadcast("WebSocket Connected");
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                broadcast(text);
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                broadcast("WebSocket Error: " + t.getMessage());
            }

            @Override
            public void onClosing(WebSocket webSocket, int code, String reason) {
                broadcast("WebSocket Closing: " + reason);
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                broadcast("WebSocket Closed: " + reason);
            }
        });
    }

    public void send(String message) {
        if (webSocket != null) {
            webSocket.send(message);
        } else {
            broadcast("WebSocket not connected");
        }
    }

    public void addListener(ESPMessageListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(ESPMessageListener listener) {
        listeners.remove(listener);
    }

    private void broadcast(String message) {
        for (ESPMessageListener listener : listeners) {
            listener.onWebSocketMessage(message);
        }
    }

    // Custom listener interface for fragments or activities to implement
    public interface ESPMessageListener {
        void onWebSocketMessage(String message);
    }
}
