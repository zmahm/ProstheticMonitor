# Android Robotic Arm Monitor - ESP8266 WebSocket Controller

An Android app that monitors and controls a four-servo robotic arm driven by an ESP8266.  
It connects to the ESP8266’s **WebSocket** server (port 81) hosted on the device’s Wi-Fi Access Point and provides three tabs:

- Threshold Management
- Live Controls (log viewer)
- Metrics (status & thresholds snapshot)

This app is intended to support bench testing, calibration, and monitoring for the EEG → Flask → ESP8266 pipeline, but it also works standalone.

---

## Features

- Connect to `ws://<ESP_AP_IP>:81` and maintain a persistent WebSocket session.
- **Threshold Management Tab**
  - Per-servo slider for safe manual positioning between temporary min/max.
  - Set **min** / **max** thresholds interactively (non-persistent; reset on ESP reboot).
  - Console-style log view showing ESP8266 broadcast messages and command acks.
- **Live Controls Tab**
  - Real-time stream of all broadcast WebSocket messages (passive viewer for now).
  - Useful for observing simulated live control and EEG-driven sessions.
- **Metrics Tab**
  - Periodic status checks (`get_status`) to verify connectivity and uptime.
  - Snapshot of current thresholds for all servos (`get_thresholds`) without user input.
- Robust JSON encoding/decoding for the control protocol.

---

## App Architecture (high level)

- **WebSocketClient**: single source of truth for the WS connection & message routing.
- **Repository / Manager**: exposes methods like `setThresholds()`, `moveToPosition()`, `toggleServo()`, `getStatus()`, `getThresholds()`.
- **UI**: three fragments/tabs consuming a shared ViewModel (MVVM pattern recommended).
- **Logging**: append-only in-memory buffer per session; displayed in Thresholds & Live tabs.

---

## ESP8266 WebSocket API (used by the app)

- `{"action":"get_status"}`  
  Response: `{"status":"running","uptime":<secs>,"ip":"<ap_ip>"}`

- `{"action":"get_thresholds"}` or `{"action":"get_thresholds","servoId":<0..3>}`  
  Response (all): `{"thresholds":[{"servoId":0,"min":0,"max":180}, ...]}`  
  Response (one): `{"servoId":<id>,"min":<deg>,"max":<deg>}`

- `{"action":"set_thresholds","servoId":<id>,"min":<deg>,"max":<deg>}`  
  Response: `{"status":"thresholds updated","servoId":<id>,"min":<deg>,"max":<deg>}`

- `{"action":"move_to_position","servoId":<id>,"position":<deg>,"speed":"slow|medium|fast|instant"}`  
  Response: ack or movement-started message.

- `{"action":"toggle_servo","servoId":<id>,"speed":"slow|medium|fast|instant","direction":"min|max"}` (direction optional)  
  Response: instant move / toggle started / movement stopped.

Error examples: `{"error":"Invalid JSON"}`, `{"error":"Invalid servo ID"}`, etc.

---

## Requirements

- **Android Studio** (Giraffe or newer recommended).
- **Android**: minSdk 23+ (Android 6.0), targetSdk 34 (or current stable).
- **Permissions**:
  - `INTERNET`
  - `ACCESS_NETWORK_STATE`

Manifest example (add to `AndroidManifest.xml`):

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

---

## Configuration

- **WebSocket URL**: set the ESP8266 AP IP (usually `192.168.4.1`) and port `81`.
  - Recommended: keep it in a single source (e.g., `BuildConfig`/`local.properties`/`Constants.kt`).
  - Example value: `ws://192.168.4.1:81`

- **Reconnect policy**: enable automatic retry with backoff (e.g., 1s → 2s → 5s).

- **JSON**: use a single data class for outgoing requests with a generic `action` string and optional fields (`servoId`, `min`, `max`, `position`, `speed`, `direction`). Parse incoming frames into a sealed class hierarchy or a map for logs.

---

## UI Details

### Threshold Management Tab
- Purpose: **safe range discovery** to avoid mechanical strain or damage.
- For each servo (0–3):
  - **Slider**: manually position within the **temporary** min/max (sent via `move_to_position`).
  - **Set Min / Set Max** buttons: send `set_thresholds` with the currently displayed position.
- **Console log** (bottom): shows all broadcast acks/errors and helps verify that commands are received.
- **Persistence**: thresholds are **not persistent** on the ESP8266; they reset on power-off. Once validated, hardcode safe limits in firmware if desired.

### Live Controls Tab
- Passive log viewer for all broadcast messages.
- Will become the main pane when simulated/EEG live control is active.
- Useful for observing toggles, target changes, and motion events in real time.

### Metrics Tab
- Periodically calls `get_status` for **uptime** / connectivity.
- Fetches `get_thresholds` (all) to display current min/max for all servos.
- Intended for quick health checks and debugging without manual commands.

---

## Typical Workflows

### A) Calibrate Safe Ranges
1. Connect phone to the ESP8266 AP.
2. Open the app and connect (WS shows “connected”).
3. On **Threshold Management**, use sliders to confirm mechanical limits.
4. Tap **Set Min / Set Max** to save temporary thresholds on the ESP.
5. Verify acks in the console log.
6. Once satisfied, copy those limits into firmware as hardcoded defaults.

### B) Observe a Live Session
1. Open **Live Controls** to watch all broadcast messages.
2. Run your control client (e.g., desktop/browser) that sends `toggle_servo` or `move_to_position`.
3. Confirm movements and states are reflected in the log.

### C) Check System Health
1. Open **Metrics**.
2. Confirm `get_status` is updating (uptime increments).
3. Confirm thresholds snapshot matches expectations.

---

## Build & Run

1. Open the project in Android Studio.
2. Set the WebSocket endpoint in the app configuration (see **Configuration**).
3. Build and run on a device joined to the ESP8266 AP.

Notes:
- Emulators typically cannot reach `192.168.4.1` AP networks; use a **physical device**.
- Keep the screen awake during long tests (developer options or an in-app “keep awake” flag).

---

## Safety Notes

- Ensure servo power is adequate and grounds are common with the ESP8266.
- Always begin with conservative ranges and slow speed.
- Watch for over-current, brown-outs, or thermal issues during extended tests.



