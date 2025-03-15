package com.example.esp8266apiintegration;

import android.util.Log;
import okhttp3.*;

import java.io.IOException;

public class ESP8266API {
    private static final String TAG = "ESP8266API";
    private static final String BASE_URL = "http://192.168.4.1";  // ESP8266 Access Point IP
    private static final OkHttpClient client = new OkHttpClient();

    // Method to fetch system info
    public static void getSystemInfo(Callback callback) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/data")
                .build();

        client.newCall(request).enqueue(callback);
    }

    // Method to toggle a servo
    public static void toggleServo(int servoId, Callback callback) {
        String jsonBody = "{\"servoId\":" + servoId + "}";

        RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json; charset=utf-8"));

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/servo/toggle")
                .post(body)
                .build();

        client.newCall(request).enqueue(callback);
    }
}