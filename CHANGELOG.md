# Changelog

Alle nennenswerten Änderungen an diesem Projekt werden in dieser Datei dokumentiert.

Das Format basiert auf [Keep a Changelog](https://keepachangelog.com/de/1.0.0/),
und dieses Projekt folgt [Semantic Versioning](https://semver.org/lang/de/).

---

## [1.0.0] - 2026-10-11

### Hinzugefügt
* **Integriertes Video-Recording mit Cockpit-HUD (Killer-Feature):**
  * Hardwarebeschleunigte MP4-Videoaufnahme über `MediaCodec` & `MediaMuxer` (1280x720 @ 30 FPS).
  * Direktes Einbrennen der Live-Telemetrie ins Video (Tacho, Gasbalken, Boost-Indikator, Lenkwinkel, Bremse, Akkustand, Zeitstempel & REC-Timer).
  * Automatischer MediaStore-Scan: Aufnahmen landen sofort in `Filme/MK_Live` und sind in der Android-Galerie / Google Fotos verfügbar.
  * Aufnahme-Optionen in den Settings: Cockpit-HUD-Overlay an/aus, Audioaufnahme über Smartphone-Mikrofon an/aus.
* **Neues stylisches App-Icon:**
  * Eigenes 3D-Vektor-Icon mit Mario im Rennkart, montierter FPV-Dachkamera, leuchtenden Speedtrails und Carbon-Badge.
  * Vollständige Unterstützung für runde App-Icons (`ic_launcher_round.png`) auf Google Pixel und Android 14+.
* **PC Basis-Station & Funk-Optimierung:**
  * Schneller Desktop-Starter (`Start_Basis_Station.desktop`) und Automatisierungsskript (`start_basis_station.sh`).
  * Sendeleistung der WLAN-Karte auf 20 dBm (100 mW EIRP, ETSI/DE) maximiert.
  * Ausführliche Schritt-für-Schritt-Anleitung auf dem Desktop (`ANLEITUNG_BASIS_STATION.txt`).
* **Erweiterte Controller-Unterstützung & Fahrphysik:**
  * R2-Trigger dosiert Gas feinfühlig bis maximal 70 % für kontrollierte Kurvenfahrten.
  * A- und X-Taste aktivieren den 100 % Vollgas-Boost für maximale Beschleunigung.
  * Automatischer 30 Hz Keep-Alive Stream: Verhindert das lästige 1,8s-Notbremsen des Karts beim Gedrückthalten des Gases.
  * B-Taste fängt Zurück-Navigation ab, sodass die App während der Fahrt nicht versehentlich schließt.

### Verbessert
* **Settings-Menü:**
  * Vollständig für Querformat (Landscape) überarbeitet: Passt ohne Scrollen perfekt auf den Bildschirm.
  * IP-Eingabe für Basis-Station mit Schnellverbindung.
  * Umschaltung zwischen H.264 (Low-Latency) und MJPEG (Universal).
* **Cockpit UI & Instrumente:**
  * Entfernung aller störenden Root- und Magisk-Hinweise.
  * Schneller Zugriff auf Pairing, Aufnahme und Einstellungen direkt aus der oberen Statusleiste.
  * Integriertes Kompassband, G-Meter und Akku-Statusanzeige.

### Entfernt
* Experimentelle Standalone-Funktionen für den internen Broadcom-Handychip entfernt, da Broadcom FullMAC-Firmwares keine proprietären Nintendo LP2P Vendor-IEs und statische CCMP-Verschlüsselung unterstützen.
