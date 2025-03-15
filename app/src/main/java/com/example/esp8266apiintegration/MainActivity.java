package com.example.esp8266apiintegration;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import okhttp3.*;
import androidx.appcompat.app.AppCompatActivity;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "ESP8266API";
    private static final String BASE_URL = "http://192.168.4.1";  // ESP8266 Access Point IP
    private static final OkHttpClient client = new OkHttpClient();
    private final Handler handler = new Handler();
    private TextView statusTextView;
    private TextView requestLogTextView;
    private Spinner spinnerServos;
    private Button btnManualRefresh;
    private Button btnToggleServo;

    private final Runnable refreshTask = new Runnable() {
        @Override
        public void run() {
            getSystemInfo();
            handler.postDelayed(this, 5 * 60 * 1000); // Refresh every 5 minutes
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnManualRefresh = findViewById(R.id.btnManualRefresh);
        btnToggleServo = findViewById(R.id.btnToggleServo);
        statusTextView = findViewById(R.id.statusTextView);
        requestLogTextView = findViewById(R.id.requestLogTextView);
        spinnerServos = findViewById(R.id.spinnerServos);

        btnManualRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getSystemInfo();
            }
        });

        btnToggleServo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedServo = spinnerServos.getSelectedItemPosition();
                toggleServo(selectedServo);
            }
        });

        // Start automatic refresh every 5 minutes
        handler.post(refreshTask);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshTask); // Stop refresh when activity is destroyed
    }

    private void getSystemInfo() {
        statusTextView.setText("Fetching data...");

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/data")
                .build();

        logRequestDetails(request);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to connect", e);
                runOnUiThread(() -> {
                    statusTextView.setText("Failed to connect");
                    logResponseDetails("Failed to connect: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "API Response: " + responseBody);
                runOnUiThread(() -> {
                    statusTextView.setText("Connected: API Active");
                    logResponseDetails("Response: " + responseBody);
                });
            }
        });
    }

    private void toggleServo(int servoId) {
        String jsonBody = "{\"servoId\":" + servoId + "}";

        RequestBody body = RequestBody.create(jsonBody, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(BASE_URL + "/api/servo/toggle")
                .post(body)
                .build();

        logRequestDetails(request);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to connect", e);
                runOnUiThread(() -> {
                    logResponseDetails("Failed to connect: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                Log.d(TAG, "API Response: " + responseBody);
                runOnUiThread(() -> {
                    logResponseDetails("Response: " + responseBody);
                });
            }
        });
    }

    private void logRequestDetails(Request request) {
        runOnUiThread(() -> requestLogTextView.setText("Request Sent: \n" + request.toString()));
    }

    private void logResponseDetails(String response) {
        runOnUiThread(() -> requestLogTextView.append("\n\n" + response));
    }
}
