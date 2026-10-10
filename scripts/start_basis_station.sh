#!/usr/bin/env bash
# ==============================================================================
# MK Live Home Circuit - Basis-Station Starter & Status Script
# ==============================================================================
set -e

echo "=== Mario Kart Live: Basis-Station wird gestartet ==="

# 1. Funk-Regulierung & Sendeleistung optimieren
if command -v iw >/dev/null 2>&1; then
    sudo iw reg set DE 2>/dev/null || true
fi

# 2. Systemd Services starten
echo "Starte Systemd-Dienste (openkart, video, web)..."
sudo systemctl start mk-live-openkart.service mk-live-video.service mk-live-web.service

# 3. Status prüfen
sleep 1
IP_ADDR=$(hostname -I | awk '{print $1}')
STATUS_OK=true

for s in mk-live-openkart mk-live-video mk-live-web; do
    if systemctl is-active --quiet "$s"; then
        echo "[OK] $s läuft."
    else
        echo "[FEHLER] $s konnte nicht gestartet werden."
        STATUS_OK=false
    fi
done

if [ "$STATUS_OK" = true ]; then
    echo ""
    echo "=========================================================="
    echo "  BASIS-STATION ERFOLGREICH GESTARTET!"
    echo "  Cockpit URL:    http://${IP_ADDR}:9000"
    echo "  Video Stream:   http://${IP_ADDR}:9001/video.mjpeg"
    echo "=========================================================="
    if command -v notify-send >/dev/null 2>&1; then
        notify-send -i input-gaming "MK Live Basis-Station" "Aktiv auf http://${IP_ADDR}:9000" || true
    fi
else
    echo "Mindestens ein Dienst hat einen Fehler. Überprüfe 'journalctl -xeu mk-live-openkart'."
fi
