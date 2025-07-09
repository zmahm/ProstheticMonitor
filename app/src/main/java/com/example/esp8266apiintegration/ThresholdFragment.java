package com.example.esp8266apiintegration;

import android.graphics.Color;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONException;
import org.json.JSONObject;

public class ThresholdFragment extends Fragment implements WebSocketManager.ESPMessageListener {

    private static final int NUM_SERVOS = 4;

    private LinearLayout servoGroupContainer;
    private RadioGroup speedRadioGroup;
    private TextView wsConsole;

    // One view group per servo
    private SeekBar[] servoSliders = new SeekBar[NUM_SERVOS];
    private Button[] minButtons = new Button[NUM_SERVOS];
    private Button[] maxButtons = new Button[NUM_SERVOS];
    private int[] currentPositions = new int[NUM_SERVOS];
    private int[] defaultMin = new int[NUM_SERVOS];
    private int[] defaultMax = new int[NUM_SERVOS];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_threshold, container, false);

        servoGroupContainer = view.findViewById(R.id.servoGroupContainer);
        speedRadioGroup = view.findViewById(R.id.speedRadioGroup);
        wsConsole = view.findViewById(R.id.wsConsole);

        wsConsole.setMovementMethod(new ScrollingMovementMethod());

        // Register this fragment as a WebSocket listener
        WebSocketManager.getInstance().addListener(this);

        setupSliders(); // Create UI elements
        requestDefaultThresholds(); // Ask ESP for default min/max

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        WebSocketManager.getInstance().removeListener(this);
    }

    private void setupSliders() {
        for (int i = 0; i < NUM_SERVOS; i++) {
            final int servoId = i;

            TextView label = new TextView(getContext());
            label.setText("Servo " + servoId);
            label.setTextColor(Color.GREEN);
            label.setTextSize(18f);

            SeekBar slider = new SeekBar(getContext());
            slider.setMax(45); // Temp, will update after receiving defaults
            slider.setProgress(0);
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    currentPositions[servoId] = progress;
                    sendMoveInstant(servoId, progress);
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            Button setMin = new Button(getContext());
            setMin.setText("Set Min");
            setMin.setOnClickListener(v -> sendSetThreshold(servoId, progressOf(servoId), true, false));

            Button setMax = new Button(getContext());
            setMax.setText("Set Max");
            setMax.setOnClickListener(v -> sendSetThreshold(servoId, progressOf(servoId), false, true));

            LinearLayout group = new LinearLayout(getContext());
            group.setOrientation(LinearLayout.VERTICAL);
            group.setPadding(0, 24, 0, 24);
            group.addView(label);
            group.addView(slider);
            group.addView(setMin);
            group.addView(setMax);

            servoGroupContainer.addView(group);

            // Save references
            servoSliders[servoId] = slider;
            minButtons[servoId] = setMin;
            maxButtons[servoId] = setMax;
        }
    }

    private int progressOf(int servoId) {
        return servoSliders[servoId].getProgress();
    }

    private String getSelectedSpeed() {
        int checked = speedRadioGroup.getCheckedRadioButtonId();
        if (checked == R.id.radioSlow) return "slow";
        if (checked == R.id.radioFast) return "fast";
        return "medium";
    }

    private void sendMoveInstant(int servoId, int position) {
        try {
            JSONObject json = new JSONObject();
            json.put("action", "move_instant");
            json.put("servoId", servoId);
            json.put("position", position);
            WebSocketManager.getInstance().send(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendSetThreshold(int servoId, int value, boolean updateMin, boolean updateMax) {
        try {
            JSONObject json = new JSONObject();
            json.put("action", "set_thresholds");
            json.put("servoId", servoId);
            if (updateMin) json.put("min", value);
            if (updateMax) json.put("max", value);
            WebSocketManager.getInstance().send(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void requestDefaultThresholds() {
        WebSocketManager.getInstance().send("{\"action\": \"get_default_thresholds\"}");
    }

    @Override
    public void onWebSocketMessage(String message) {
        requireActivity().runOnUiThread(() -> {
            appendToConsole(message);

            try {
                JSONObject obj = new JSONObject(message);
                if (obj.has("defaultMin") && obj.has("defaultMax")) {
                    int min = obj.getInt("defaultMin");
                    int max = obj.getInt("defaultMax");

                    for (int i = 0; i < NUM_SERVOS; i++) {
                        defaultMin[i] = min;
                        defaultMax[i] = max;
                        servoSliders[i].setMax(max);
                        servoSliders[i].setProgress(min);
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void appendToConsole(String message) {
        wsConsole.append("\n" + message);
        final int scrollAmount = wsConsole.getLayout().getLineTop(wsConsole.getLineCount()) - wsConsole.getHeight();
        if (scrollAmount > 0)
            wsConsole.scrollTo(0, scrollAmount);
        else
            wsConsole.scrollTo(0, 0);
    }
}
