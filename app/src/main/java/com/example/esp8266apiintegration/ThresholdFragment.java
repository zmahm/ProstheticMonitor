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

    private SeekBar[] servoSliders = new SeekBar[NUM_SERVOS];
    private Button[] minButtons = new Button[NUM_SERVOS];
    private Button[] maxButtons = new Button[NUM_SERVOS];

    private int[] currentPositions = new int[NUM_SERVOS];
    private int[] defaultMin = new int[NUM_SERVOS];
    private int[] defaultMax = new int[NUM_SERVOS];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_threshold, container, false);

        servoGroupContainer = view.findViewById(R.id.servoGroupContainer);
        speedRadioGroup = view.findViewById(R.id.speedRadioGroup);
        wsConsole = view.findViewById(R.id.wsConsole);
        wsConsole.setMovementMethod(new ScrollingMovementMethod());

        WebSocketManager.getInstance().addListener(this);
        setupSliders();
        requestDefaultThresholds();

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
            label.setTextColor(Color.GREEN);
            label.setTextSize(18f);
            label.setText(String.format("Servo %d - Current Position: 0", servoId)); // Initial display


            SeekBar slider = new SeekBar(getContext());
            slider.setMax(180); // Placeholder, updated later
            slider.setProgress(0);
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    currentPositions[servoId] = progress;
                    label.setText(String.format("Servo %d - Current Position: %d", servoId, progress));
                    sendMoveToPosition(servoId, progress);
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            // Styled buttons using utility method
            Button setMin = UIUtils.createStyledButton(
                    getContext(),
                    "Set Min",
                    v -> sendSetThreshold(servoId, currentPositions[servoId], true, false)
            );

            Button setMax = UIUtils.createStyledButton(
                    getContext(),
                    "Set Max",
                    v -> sendSetThreshold(servoId, currentPositions[servoId], false, true)
            );

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

    private void sendMoveToPosition(int servoId, int position) {
        try {
            JSONObject json = new JSONObject();
            json.put("action", "move_to_position");
            json.put("servoId", servoId);
            json.put("position", position);
            json.put("speed", getSelectedSpeed());
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

    private String getSelectedSpeed() {
        int checked = speedRadioGroup.getCheckedRadioButtonId();
        if (checked == R.id.radioSlow) return "slow";
        if (checked == R.id.radioMedium) return "medium";
        if (checked == R.id.radioFast) return "fast";
        return "instant";
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
