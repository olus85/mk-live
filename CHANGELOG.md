# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [2.0.0] - 2026-10-14

### Added
* **🏎️💨 Augmented Reality (AR) Core Engine (Killer Feature):**
  * **3D Perspective & Ground Projection (Homography):** Physical camera geometry ($h = 4.5\text{ cm}$, fixed tilt $\theta = 8^\circ$, $82^\circ \text{H-FOV} \times 52^\circ \text{V-FOV}$) coupled with real-time IMU gyroscopic pitch and roll telemetry.
  * **Interactive Gold Coins:** 3D spinning and bobbing golden coins floating above the carpet/floor. Collecting gives the iconic Mario "Cha-Ching!" chime, particle star explosion, increments coin counter (`x07 / 10`), and applies a +1.5% speed buff per coin (up to +15% top speed).
  * **Holographic ?-Item Boxes:** Translucent pulsating yellow/orange 3D cubes hovering on the track. Driving through shatters the cube into 16 crystalline fragments and triggers the Item Roulette.
  * **Turbo Boost Dash Pads:** Flowing neon-cyan and red directional chevrons on the floor that ignite an instant 100% Nitro Boost with visual motion blur speed lines and jet turbine sound.
  * **Virtual Track Gates & Lap Timing:** Interactive checkpoint archways with banners (`🚩 GATE 1..4` and `🏁 FINISH / LAP`). Set virtual gates anywhere on the floor with D-Pad UP or the HUD button, complete with real-time lap timer, lap counting (3 Laps GP), and best lap records.
  * **Odometry & Dead Reckoning Tracker:** Integrates throttle, steering, and gyro yaw rate to continuously track relative positions and perform sub-10ms collision detection.
* **⚔️ Combat Arsenal & Weapons System:**
  * **Minigun / Gatling Gun on [L1 / LB] (Hold):** 18 Hz rapid-fire rotary cannon with alternating bumper muzzle flashes, tracer bullets racing into depth towards the HUD reticle, weapon screen shake, and rumble. Destroys obstacles, pops item boxes, and blows bananas off the track.
  * **Lock-On Missile Launcher on [X]:** HUD radar crosshair scans forward cone; locks onto track gates and targets with a high-pitched lock-on tone. Launches guided 3D rockets with fiery exhaust trails and detonates with comic explosion sparks, smoke, and rumble.
  * **Bob-Omb / EMP Shockwave on [Y]:** Deploys high-explosive area-clearing blast that obliterates track obstacles and shakes the viewport.
  * **Banana Peels on [B]:** Drops banana peels directly behind the kart. Driving over them triggers cartoon slip-and-spin tire squeal and cuts motor power for 1 second.
  * **Mushroom Boost on [A]:** Triggers 100% nitro acceleration.
  * **Weapon Select / Horn on [R1 / RB]:** Cycles active weapons or sounds the car horn.
* **🔊 Low-Latency SoundPool SFX Engine & Dynamic Engine RPM Synthesizer:**
  * Built-in procedural 16-bit PCM 44.1 kHz sound synthesizer for instant zero-latency playback without external asset dependencies.
  * Complete SFX matrix: coin chimes, crystalline shatter, roulette tick, victory jingle, minigun ratchet, rocket whoosh, explosion boom, banana drop, slip squeal, turbo roar, horn honk, and start light beeps.
  * Dynamic frequency-modulated engine sound: synthesizes real-time combustion harmonics, pitch-scaled continuously ($0.8\text{x} \dots 2.2\text{x}$) with kart throttle telemetry and boost state.
* **🥽 Stereoscopic 3D AR for Meta Quest 3:**
  * Side-by-Side (SBS) dual-viewport 3D AR rendering with accurate inter-pupillary distance (IPD) retinal disparity. Coins, boxes, and gates appear with tangible 3D depth floating directly in the user's living room.
* **🛠️ Video Recorder Color Channel Inversion Bugfix:**
  * Fixed red/blue color swapping (cyan Mario cap) on Google Tensor (Pixel 7 Pro) and Mali/Exynos GPUs using hardware-accelerated Skia `ColorMatrix` R/B channel swapping.
  * Added setting toggle in Settings Dialog: "Color Channel Fix (RGB/BGR)".
  * Augmented Reality overlays and combat effects burned directly into recorded MP4 video clips.

---

## [1.1.0] - 2026-10-11

### Added
* **Meta Quest 3 VR Mode & Stereoscopic 3D Cockpit (Killer Feature):**
  * **Immersive Stereoscopic 3D (SBS):** Dual-viewport Side-By-Side rendering engine with customizable IPD parallax for high-immersion FPV driving directly inside VR.
  * **Live Gyroscopic Artificial Horizon:** Dynamic attitude indicator and pitch ladder reacting instantly to real-time IMU telemetry (pitch, roll, yaw) streamed from the kart.
  * **Native Meta Touch Plus Controller Mapping:**
    * Smooth analog throttle on Right Trigger (0–70%).
    * Instant 100% Boost on **[A]** and **[X]** buttons.
    * Left Trigger / Grip for smooth braking and reverse.
    * Left Thumbstick for precise steering.
    * Button **[Y]** for one-click toggling between Flat HUD and Immersive VR mode.
    * Thumbstick click (**L3/R3**) to instantly reset horizon orientation.
  * **Haptic Rumble Feedback:** Tactile rumble pulses on full boost acceleration, heavy braking, and VR mode switching.
  * **Custom Quest 3 App Icon:** Distinct full-bleed 3D Mario icon wearing a Meta Quest 3 VR headset (`ic_launcher.png` and `ic_launcher_round.png`) exclusively for the Quest build.
  * **Oculus Platform Integration:** Quest manifest integration including `com.oculus.intent.category.VR`, targeting `quest2|questpro|quest3|quest3s`, 1920x1080 panel sizing, and hand tracking metadata.
  * **Automated Quest Sideload Installer:** Added `scripts/install_quest3.sh` and `Install_Quest3.desktop` for 1-click ADB sideloading onto connected headsets.
* **Bilingual Localization (German & English):**
  * German localization fully restored alongside English.
  * In-app language picker in Settings: seamlessly choose between **System Default**, **Deutsch**, or **English** with real-time UI switching.
* **Multi-Flavor Build System:**
  * `phone` flavor: Standard release with classic 3D racing Mario icon for Android smartphones.
  * `quest` flavor: VR-optimized release with Oculus VR manifest and VR Mario icon for Meta Quest headsets.

---

## [1.0.1] - 2026-10-11

### Added
* **Full-Bleed 3D App Icon & Round Launcher Support:**
  * Replaced letterboxed icon with dynamic, edge-to-edge 3D Mario racing driver icon.
  * Native circular icon (`ic_launcher_round.png`) filling the entire silhouette for Google Pixel Launcher and Android 14+.

### Changed
* **Complete English Localization:**
  * All Cockpit UI elements, settings dialog, pairing menus, and system notifications translated to English.
  * Standardized all project documentation and GitHub releases to English.

---

## [1.0.0] - 2026-10-11

### Added
* **Integrated Video Recording with Cockpit HUD Overlay (Killer Feature):**
  * Hardware-accelerated MP4 video recording via `MediaCodec` and `MediaMuxer` (1280x720 @ 30 FPS).
  * Real-time live telemetry burned directly into recorded video (speedometer, throttle gauge, boost indicator, steering angle, brake status, battery percentage, timestamp & REC duration timer).
  * Automatic MediaStore scanner: recorded clips appear instantly in `Movies/MK_Live` and Google Photos / Android Gallery.
  * Recording options in Settings: toggle Cockpit HUD overlay on/off, toggle phone microphone audio recording on/off.
* **PC Base Station & Wi-Fi Transmission Optimization:**
  * Desktop starter shortcut (`Start_Basis_Station.desktop`) and automated service management script (`start_basis_station.sh`).
  * Wi-Fi transmission power boosted to 20 dBm (100 mW EIRP, ETSI/DE) for maximum connection stability.
  * Desktop setup guide for Linux base station operation.
* **Enhanced Controller Support & Driving Physics:**
  * Analog R2 / RT trigger mapped to 0–70% throttle for precise cornering and handling.
  * A and X buttons trigger 100% full-throttle boost for instant acceleration.
  * Continuous 30 Hz keep-alive stream preventing the kart's 1.8s deadman emergency brake when holding the throttle.
  * Gamepad B button intercepted to prevent accidentally exiting the app while racing.

### Changed
* **Settings Dialog:**
  * Completely redesigned for landscape screens: compact 2-column layout that fits on any display without scrolling.
  * Base station IP configuration with fast reconnect.
  * Toggle between H.264 (low latency) and MJPEG (universal compatibility) streams.
* **Cockpit UI & Telemetry Instruments:**
  * Clean status bar with fast access to Pairing, Recording, and Settings.
  * Dynamic compass ribbon, G-force meter, and battery indicator.

### Removed
* Removed experimental direct smartphone Wi-Fi hotspot mode, as mobile Broadcom FullMAC Wi-Fi chips do not support Nintendo's proprietary 802.11 LP2P Vendor-IEs (`00:22:AA:06`) or static CCMP encryption.
