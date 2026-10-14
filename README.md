# MK Live Cockpit & Base Station

Drive and record **Mario Kart Live: Home Circuit** without a Nintendo Switch — now with **Meta Quest 3 VR Mode**! 🏎️🥽💨

Transform a Linux PC, Raspberry Pi, or Steam Deck with a Wi-Fi adapter into an autonomous Mario Kart base station. Drive with an ultra-low latency native Android cockpit app on your smartphone or immerse yourself inside the cockpit with the **Meta Quest 3** using Touch Plus controllers, live stereoscopic FPV video, and telemetry-reactive artificial horizon instrumentation.

---

## 🌟 Highlights & Key Features

* **🏎️💨 AR Combat Edition & Virtual Circuit Engine (New in v2.0.0!):**
  * **3D Floor Homography & Ground Projection:** Real-world camera geometry coupled with real-time IMU gyroscopic pitch and roll telemetry.
  * **Interactive Gold Coins:** Rotating 3D gold coins floating on the floor. Driving over them rewards the iconic chime, glitter starbursts, coin counter increments, and a +1.5% speed buff per coin (up to +15% top speed).
  * **Holographic ?-Item Boxes:** Pulsating 3D question-mark cubes that shatter into 16 crystalline shards and trigger the Item Roulette.
  * **Turbo Boost Dash Pads:** Flowing neon chevron arrows on the carpet delivering an instant 100% Nitro Boost with camera speed blur lines.
  * **Virtual Track Gates & Lap Timing:** Interactive checkpoint archways (`🚩 GATE 1..4` & `🏁 FINISH / LAP`) placeable on the floor via D-Pad UP or HUD button. Real-time lap timer, lap counting, and personal best records.
  * **⚔️ Full Combat Arsenal:**
    * **[L1 / LB] (Hold):** Minigun rapid fire (18 Hz) with bumper muzzle flashes, tracer bullets, screen shake, and rumble.
    * **[X]:** Missile Launcher with Lock-On HUD reticle, target tracking, comic explosions, and haptics.
    * **[Y]:** Bob-Omb EMP shockwave detonator.
    * **[B]:** Banana Peels dropped behind the kart (causes 360° spin-out and power cut).
    * **[A]:** Mushroom Boost & Active Item trigger.
    * **[R1 / RB]:** Weapon Select / Horn.
* **🔊 Low-Latency SoundPool SFX & Dynamic Engine RPM Synthesizer:**
  * Procedurally synthesized 16-bit PCM 44.1 kHz arcade audio engine (zero external dependencies).
  * Pitch-modulated kart engine sound scaling in real time with live throttle telemetry ($0.8\text{x} \dots 2.2\text{x}$).
* **🥽 Meta Quest 3 & VR Headset Stereoscopic 3D AR:**
  * Dual-viewport Side-by-Side (SBS) rendering with inter-pupillary distance (IPD) parallax: AR coins, gates, and boxes appear with tangible 3D depth directly in your room.
* **🛠️ Video Recorder Color Channel Inversion Bugfix:**
  * Hardware-accelerated Skia `ColorMatrix` RGB/BGR channel swapper fixing the inverted red/blue colors (cyan Mario hat) on Google Tensor (Pixel 7 Pro) and Mali/Exynos GPUs, with AR overlay burn-in.

---

## 🎮 Controller Combat Mapping

### Meta Quest Touch Plus & Gamepads (Xbox, PlayStation, Switch Pro)

| Input | Combat Function | Description |
| :--- | :--- | :--- |
| **Right Trigger (R2 / RT)** | **Throttle (0 – 70%)** | Sensitive analog acceleration for precision cornering |
| **Left Trigger (L2 / LT)** | **Brake & Reverse** | Decelerate or reverse |
| **Left Stick** | **Analog Steering** | Proportional steering Left / Right |
| **L1 / LB (Hold)** | **🔥 Minigun (Gatling)** | 18 Hz rapid-fire rotary cannon with tracers & screen shake |
| **R1 / RB** | **🎯 Weapon Select / Horn** | Cycle active item or honk car horn |
| **X Button / Square (□)** | **🚀 Lock-On Missile** | Guided rocket with radar target tracking & explosion |
| **Y Button / Triangle (△)** | **💣 Bob-Omb / EMP** | High-explosive shockwave clearing obstacles |
| **B Button / Circle (○)** | **🍌 Banana Peel Drop** | Deploys rear banana hazard (or dialog back in menus) |
| **A Button / Cross (✕)** | **🍄 Boost / Active Item** | 100% Nitro Boost or triggers current roulette weapon |
| **D-Pad Up** | **🚩 Set Checkpoint Gate** | Drops a virtual checkpoint / finish gate at current position |
| **D-Pad Down / Select** | **🥽 Toggle VR Mode** | Switch between 2D Cockpit and Immersive 3D SBS View |
| **Thumbstick Click (L3/R3)**| **🧭 Reset Orientation** | Zero out artificial horizon heading |

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
