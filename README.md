# MK Live Cockpit & Base Station

Drive and record **Mario Kart Live: Home Circuit** without a Nintendo Switch — now with **Meta Quest 3 VR Mode**! 🏎️🥽💨

Transform a Linux PC, Raspberry Pi, or Steam Deck with a Wi-Fi adapter into an autonomous Mario Kart base station. Drive with an ultra-low latency native Android cockpit app on your smartphone or immerse yourself inside the cockpit with the **Meta Quest 3** using Touch Plus controllers, live stereoscopic FPV video, and telemetry-reactive artificial horizon instrumentation.

---

## 🌟 Highlights & Key Features

* **🥽 Meta Quest 3 & VR Headset Mode (New!):**
  * **Stereoscopic 3D Cockpit (SBS):** Immersive dual-viewport rendering with interpupillary distance (IPD) parallax adjustment.
  * **Dynamic Artificial Horizon:** Gyroscopic attitude indicator responding in real-time to kart pitch and roll telemetry.
  * **Meta Touch Plus Controller Support:** Drive naturally with trigger throttle, analog steering, grip braking, boost buttons, and haptic feedback.
  * **Dedicated Quest App Flavor:** Features a custom 3D Mario icon wearing a Meta Quest 3 VR headset, Oculus manifest declarations (`com.oculus.intent.category.VR`, 1920x1080 panel), and hand tracking support.
  * **One-Click VR Toggle:** Seamlessly switch between flat cinema panel and immersive VR mode anytime via button **[Y]** or HUD toggle.

* **🌍 Bilingual Localization (English & German):**
  * Full in-app language switching between German (Deutsch) and English.
  * Automatic detection of system language with manual override in Settings.

* **📱 Native Android Cockpit App:**
  * Ultra-low latency FPV camera stream (H.264 hardware decoding & MJPEG fallback).
  * Modern, compact landscape HUD optimized for gaming smartphones and VR displays.
  * Real-time telemetry: Speedometer, throttle indicator, boost gauge, steering angle, G-Meter, compass ribbon, and battery voltage.
  * Safe gamepad navigation (B-button won't accidentally exit the app).
  * Fine throttle control via R2 trigger (0–70% for precision driving) and 100% Boost via A / X button.
  * Continuous 30 Hz keep-alive stream preventing the kart's 1.8s deadman emergency brake.

* **🔴 Live HUD Video Recording:**
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

## 🎮 Controller Mapping

### Meta Quest Touch Plus Controllers (Quest 3 / Pro / 2)

| Input | Action | Description |
| :--- | :--- | :--- |
| **Right Trigger** | **Throttle (0 – 70%)** | Smooth analog acceleration |
| **A Button** | **Full Throttle Boost (100%)** | Instant maximum boost (with haptic rumble) |
| **X Button** | **Boost (100%)** | Secondary full boost button |
| **Left Trigger / Grip** | **Brake / Reverse** | Decelerate or reverse |
| **Left Thumbstick (X-Axis)** | **Steering** | Analog steering (Left / Right) |
| **Y Button** | **Toggle VR Mode** | Switch between Flat Cockpit and Immersive 3D SBS View |
| **Thumbstick Click (L3 / R3)** | **Reset Orientation** | Zero out artificial horizon heading |
| **B Button** | **Back / Cancel** | Safe dialog navigation |

### Standard Bluetooth / USB Gamepads (Xbox, DualSense, 8BitDo)

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
                       ┌─────────────────────────────────────────┐
                       │ Android App (Phone or Meta Quest 3 VR)  │
                       │ • Flat Screen HUD / 3D SBS VR Cockpit   │
                       │ • Telemetry-driven Artificial Horizon   │
                       │ • Quest Touch & Bluetooth Gamepad Input │
                       │ • Hardware H.264 Video Dec / Recorder   │
                       └─────────────────────────────────────────┘
```

1. **OpenKart SDK:** Runs an 802.11 access point tailored to Nintendo's LP2P protocol and handles the initial pairing handshake and encrypted communication.
2. **Video Worker (`video_worker.py`):** Reassembles UDP RTP H.264 packets into a clean stream for both native decoders and MJPEG fallback on port `9001`.
3. **Drive Server (`drive_web.py`):** Translates WebSocket control inputs into Nintendo control packets and streams IMU telemetry on port `9000`.
4. **Android & Quest App:** Connects to the base station via WebSocket and HTTP, receives video and telemetry, handles input at 30 Hz, and renders both 2D HUD and immersive 3D VR stereoscopic views.

---

## 🚀 Installation & Setup Guide

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

---

### 2. Meta Quest 3 Sideloading (VR Mode)

1. Enable **Developer Mode** on your Meta Quest 3 using the Meta Quest mobile app (under Devices -> Headset Settings -> Developer Mode).
2. Connect the Quest 3 via USB-C or Wi-Fi ADB.
3. Run the automated installer:
   ```bash
   ./scripts/install_quest3.sh
   ```
   *(Or double-click `Install_Quest3.desktop` on Linux)*
4. Alternatively, sideload using SideQuest or standard ADB:
   ```bash
   adb install -r -g MK-Live-Cockpit-v1.1.0-Quest3.apk
   ```
5. Inside the Quest 3 headset, open **App Library** -> filter by **Unknown Sources** -> launch **MK Live** (look for the Mario icon wearing the VR headset!).
6. Configure the Base Station IP in **Settings**, grab your Touch Controllers, and press **[Y]** to drive in full VR!

---

### 3. Android Smartphone Installation

1. Download `MK-Live-Cockpit-v1.1.0-Phone.apk` from the [Releases](https://github.com/olus85/mk-live/releases) page.
2. Install via ADB or file manager:
   ```bash
   adb install -r MK-Live-Cockpit-v1.1.0-Phone.apk
   ```
3. Open the **MK Live** app on your phone.
4. Tap **⚙ Settings** and enter the IP address of your Base Station PC (e.g. `192.168.0.105`).
5. Choose your preferred language (Deutsch / English) in the settings dialog.
6. Connect a Bluetooth gamepad and enjoy racing!

---

### 4. Pairing the Kart

1. Turn on your Mario Kart Live kart (hold the power button until the front LEDs blink).
2. In the Cockpit app, tap **Pairing** (or open `http://<BASE_STATION_IP>:9000` in a browser).
3. Point the kart's camera at the displayed QR code on your phone or VR virtual screen until it beeps and connects.

---

## 🛠️ Building from Source

### Android Phone Flavor
```bash
cd android
./gradlew assemblePhoneRelease
```
Output: `android/app/build/outputs/apk/phone/release/app-phone-release.apk`

### Meta Quest 3 Flavor
```bash
cd android
./gradlew assembleQuestRelease
```
Output: `android/app/build/outputs/apk/quest/release/app-quest-release.apk`

### Build All Flavors
```bash
cd android
./gradlew assembleRelease
```

---

## 📜 Credits & Acknowledgments

* **[OpenKart SDK](https://github.com/OpenKart-SDK/openkart)** by Sam Edwards (CFSworks) – AP implementation and kart reverse engineering.
* **[SwitchBrew](https://switchbrew.org/wiki/Mario_Kart_Live:_Home_Circuit)** – Protocol documentation for LP2P and RCD packet structures.
* **[Malu05](https://www.youtube.com/@malu05a)** – Early inspiration for custom Mario Kart telemetry and control.
* **hostapd** by Jouni Malinen and contributors.

---

## ⚖️ Disclaimer

Not affiliated with, endorsed, or sponsored by Nintendo Co., Ltd. or Meta Platforms, Inc. *Mario Kart* and *Nintendo Switch* are registered trademarks of Nintendo. *Meta Quest* is a trademark of Meta Platforms, Inc. This project is an independent interoperability tool developed for educational and hobbyist purposes. No copyrighted game assets or proprietary firmware keys are included.
