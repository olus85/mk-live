# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
