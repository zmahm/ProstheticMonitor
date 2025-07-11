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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LiveControlFragment extends Fragment implements WebSocketManager.ESPMessageListener {

    private static final int NUM_SERVOS = 4;

    private LinearLayout liveServoContainer;
    private RadioGroup speedRadioGroup;
    private TextView liveWsConsole;

    private SeekBar[] servoSliders = new SeekBar[NUM_SERVOS];
    private int[] liveMin = new int[NUM_SERVOS];
    private int[] liveMax = new int[NUM_SERVOS];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_live_control, container, false);

        liveServoContainer = view.findViewById(R.id.liveServoContainer);
        speedRadioGroup = view.findViewById(R.id.speedRadioGroupLive);
        liveWsConsole = view.findViewById(R.id.liveWsConsole);

        liveWsConsole.setMovementMethod(new ScrollingMovementMethod());

        WebSocketManager.getInstance().addListener(this);

        requestLiveThresholds();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        WebSocketManager.getInstance().removeListener(this);
    }

    private void requestLiveThresholds() {
        WebSocketManager.getInstance().send("{\"action\":\"get_thresholds\"}");
    }

    private void buildSliders() {
        liveServoContainer.removeAllViews();

        for (int i = 0; i < NUM_SERVOS; i++) {
            final int servoId = i;

            TextView label = new TextView(getContext());
            label.setText("Servo " + servoId);
            label.setTextColor(Color.GREEN);
            label.setTextSize(18f);

            SeekBar slider = new SeekBar(getContext());
            slider.setMax(liveMax[i] - liveMin[i]);
            slider.setProgress(0);
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    int actualPosition = liveMin[servoId] + progress;
                    sendToggle(servoId, actualPosition);
                }

                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {}
            });

            LinearLayout group = new LinearLayout(getContext());
            group.setOrientation(LinearLayout.VERTICAL);
            group.setPadding(0, 24, 0, 24);
            group.addView(label);
            group.addView(slider);

            liveServoContainer.addView(group);
            servoSliders[servoId] = slider;
        }
    }

    private void sendToggle(int servoId, int position) {
        try {
            JSONObject json = new JSONObject();
            json.put("action", "toggle_servo");
            json.put("servoId", servoId);
            json.put("direction", ""); // Will toggle based on state
            json.put("speed", getSelectedSpeed());
            WebSocketManager.getInstance().send(json.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private String getSelectedSpeed() {
        int checked = speedRadioGroup.getCheckedRadioButtonId();
        if (checked == R.id.radioSlowLive) return "slow";
        if (checked == R.id.radioFastLive) return "fast";
        return "medium";
    }

    @Override
    public void onWebSocketMessage(String message) {
        requireActivity().runOnUiThread(() -> {
            appendToConsole(message);

            try {
                JSONObject obj = new JSONObject(message);

                if (obj.has("thresholds")) {
                    JSONArray array = obj.getJSONArray("thresholds");
                    for (int i = 0; i < array.length(); i++) {
                        JSONObject item = array.getJSONObject(i);
                        int id = item.getInt("servoId");
                        liveMin[id] = item.getInt("min");
                        liveMax[id] = item.getInt("max");
                    }
                    buildSliders(); // Build UI once all thresholds are received
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        });
    }

    private void appendToConsole(String message) {
        liveWsConsole.append("\n" + message);
        final int scrollAmount = liveWsConsole.getLayout().getLineTop(liveWsConsole.getLineCount()) - liveWsConsole.getHeight();
        if (scrollAmount > 0)
            liveWsConsole.scrollTo(0, scrollAmount);
        else
            liveWsConsole.scrollTo(0, 0);
    }
}
