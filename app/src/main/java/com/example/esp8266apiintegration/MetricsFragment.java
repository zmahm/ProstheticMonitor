package com.example.esp8266apiintegration;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MetricsFragment extends Fragment implements WebSocketManager.ESPMessageListener {

    private TextView metricsConsole;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_metrics, container, false);

        metricsConsole = view.findViewById(R.id.metricsConsole);
        metricsConsole.setMovementMethod(new ScrollingMovementMethod());

        WebSocketManager.getInstance().addListener(this);

        Button btnStatus = view.findViewById(R.id.btnGetStatus);
        Button btnThresholds = view.findViewById(R.id.btnGetThresholds);
        Button btnDefaultThresholds = view.findViewById(R.id.btnGetDefaultThresholds);

        btnStatus.setOnClickListener(v -> send("{\"action\": \"get_status\"}"));
        btnThresholds.setOnClickListener(v -> send("{\"action\": \"get_thresholds\"}"));
        btnDefaultThresholds.setOnClickListener(v -> send("{\"action\": \"get_default_thresholds\"}"));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        WebSocketManager.getInstance().removeListener(this);
    }

    private void send(String json) {
        WebSocketManager.getInstance().send(json);
        appendToConsole("Sent: " + json);
    }

    @Override
    public void onWebSocketMessage(String message) {
        requireActivity().runOnUiThread(() -> appendToConsole("Received: " + message));
    }

    private void appendToConsole(String message) {
        metricsConsole.append("\n" + message);
        final int scrollAmount = metricsConsole.getLayout().getLineTop(metricsConsole.getLineCount()) - metricsConsole.getHeight();
        if (scrollAmount > 0)
            metricsConsole.scrollTo(0, scrollAmount);
        else
            metricsConsole.scrollTo(0, 0);
    }
}
