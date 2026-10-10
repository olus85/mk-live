# MK Live Cockpit & Base Station

Drive and record **Mario Kart Live: Home Circuit** without a Nintendo Switch! 🏎️💨

Transform a Linux PC, Raspberry Pi, or Steam Deck with a Wi-Fi adapter into an autonomous Mario Kart base station. Drive with an ultra-low latency native Android cockpit app or browser, using Bluetooth gamepads, live FPV telemetry, and integrated HUD video recording directly on your smartphone.

---

## 🌟 Highlights & Key Features

* **📱 Native Android Cockpit App:**
  * Ultra-low latency FPV camera stream (H.264 hardware decoding & MJPEG fallback).
  * Modern, compact landscape HUD optimized for gaming smartphones and controllers.
  * Real-time telemetry: Speedometer, throttle indicator, boost gauge, steering angle, G-Meter, compass, and battery voltage.
  * Safe gamepad navigation (B-button won't accidentally exit the app).
  * Fine throttle control via R2 trigger (0–70% for precision driving) and 100% Boost via A / X button.
  * Continuous 30 Hz keep-alive stream preventing the kart's 1.8s deadman emergency brake.

* **🔴 Live HUD Video Recording (Killer Feature):**
  * Hardware-accelerated H.264 video encoding (`MediaCodec` & `MediaMuxer` at 1280x720 @ 30 FPS).
  * Telemetry overlay burned directly into the video: Speedometer, throttle/boost bars, steering angle, battery, timestamp, and recording timer.
  * Instant Android Gallery / Google Photos integration (`Movies/MK_Live`).
  * Configurable options: HUD overlay toggle and smartphone microphone audio recording.

* **🖥️ Robust PC Base Station:**
  * Automated systemd services for Wi-Fi access point, H.264 reassembly worker, and WebSocket teleoperation server.
  * Desktop starter shortcut (`Start_Basis_Station.desktop`) and one-click control script (`start_basis_station.sh`).
  * Wi-Fi transmission power optimized (20 dBm / 100 mW EIRP, ETSI/DE) for maximum range and stability.
  * QR code and one-click pairing support.

---

## 🎮 Gamepad Controls

| Input | Function | Description |
| :--- | :--- | :--- |
| **R2 / RT Trigger** | **Throttle (0 – 70%)** | Sensitive analog control for smooth cornering |
| **A / Cross (✕)** | **Full Throttle Boost (100%)** | Instant maximum acceleration |
| **X / Square (□)** | **Boost (100%)** | Alternative boost button |
| **L2 / LT Trigger** | **Brake / Reverse** | Slow down or drive backwards |
| **Left Stick (X-Axis)** | **Steering** | Analog steering (Left / Right) |
| **B / Circle (○)** | **Menu Back** | Intercepted in cockpit to prevent accidental exit |
| **REC Button (HUD)** | **Start/Stop Recording** | Save 720p FPV race video with live HUD overlay |

---

## 🏗️ Architecture & How It Works

```text
 ┌────────────────┐
 │ Mario Kart Live│
 └───────┬────────┘
         │ 802.11 LP2P (Nintendo Wi-Fi Direct)
         ▼
 ┌─────────────────────────────────────────────────────────┐
 │ PC / Raspberry Pi Base Station                          │
 │                                                         │
 │  ┌──────────────┐     UDP 19001 (H.264)   ┌───────────┐ │
 │  │  OpenKart    ├────────────────────────►│  Video    │ │
 │  │  hostapd AP  │                         │  Worker   │ │
 │  │              ├───────┐                 └─────┬─────┘ │
 │  └──────┬───────┘       │ UDP 19002 (IMU)       │ Stream│
 │         ▲               ▼                       ▼ :9001 │
 │         │          ┌──────────┐            ┌──────────┐ │
 │         └──────────┤  Drive   │◄───────────┤ Web/WS   │ │
 │          Control   │  Server  │  Commands  │ Server   │ │
 │                    └──────────┘            └────▲─────┘ │
 └─────────────────────────────────────────────────┼───────┘
                                                   │
                  LAN / Wi-Fi (WebSocket + HTTP)   │
                                                   ▼
                                       ┌───────────────────────┐
                                       │ Android Cockpit App   │
                                       │ (Jetpack Compose,     │
                                       │  Hardware Decoder,    │
                                       │  HUD Video Recorder)  │
                                       └───────────────────────┘
```

1. **OpenKart SDK:** Runs an 802.11 access point tailored to Nintendo's LP2P protocol and handles the initial pairing handshake and encrypted communication.
2. **Video Worker (`video_worker.py`):** Reassembles UDP RTP H.264 packets into a clean stream for both native decoders and MJPEG fallback on port `9001`.
3. **Drive Server (`drive_web.py`):** Translates WebSocket control inputs into Nintendo control packets and streams IMU telemetry on port `9000`.
4. **Android App:** Connects to the base station via WebSocket and HTTP, receives video and telemetry, handles gamepad input at 30 Hz, and encodes recorded video with live HUD graphics.

---

## 🚀 Quick Start Guide

### 1. Base Station Setup (Linux PC / Raspberry Pi)

**Prerequisites:**
- Linux (Ubuntu 22.04+, Debian 12+, Fedora, or Arch Linux)
- Wi-Fi adapter supporting AP mode (SoftMAC / mac80211, e.g. Intel Wi-Fi or Atheros AR9271)
- Python 3.10+

**Starting the Services:**
Execute the automated starter script:
```bash
./scripts/start_basis_station.sh
```
Or use the desktop shortcut `Start_Basis_Station.desktop`.

To check service status:
```bash
systemctl --user status mk-live-openkart mk-live-video mk-live-web
```

### 2. Android App Installation

1. Download the latest `MK-Live-Cockpit-v1.0.0.apk` from the [Releases](https://github.com/olus85/mk-live/releases) page.
2. Install the APK on your Android device:
   ```bash
   adb install -r MK-Live-Cockpit-v1.0.0.apk
   ```
3. Open the **MK Live** app on your phone.
4. Tap **Einstellungen (Settings)** and enter the IP address of your Base Station PC (e.g. `192.168.0.105`).
5. Pair a Bluetooth controller (e.g. Xbox, PlayStation, 8BitDo) with your smartphone and start driving!

### 3. Pairing the Kart

1. Turn on your Mario Kart Live kart (hold the power button until the front LEDs blink).
2. In the Cockpit app, tap **Pairing** (or open `http://<BASE_STATION_IP>:9000` in a browser).
3. Point the kart's camera at the displayed QR code on your phone screen until it beeps and connects.

---

## 🛠️ Building from Source

### Android Cockpit App
```bash
cd android
./gradlew assembleRelease
```
The output APK will be located at: `android/app/build/outputs/apk/release/app-release.apk`.

---

## 📜 Credits & Acknowledgments

* **[OpenKart SDK](https://github.com/OpenKart-SDK/openkart)** by Sam Edwards (CFSworks) – AP implementation and kart reverse engineering.
* **[SwitchBrew](https://switchbrew.org/wiki/Mario_Kart_Live:_Home_Circuit)** – Protocol documentation for LP2P and RCD packet structures.
* **[Malu05](https://www.youtube.com/@malu05a)** – Early inspiration for custom Mario Kart telemetry and control.
* **hostapd** by Jouni Malinen and contributors.

---

## ⚖️ Disclaimer

Not affiliated with, endorsed, or sponsored by Nintendo Co., Ltd. *Mario Kart* and *Nintendo Switch* are registered trademarks of Nintendo. This project is an independent interoperability tool developed for educational and hobbyist purposes. No copyrighted game assets or proprietary Nintendo firmware keys are included.
