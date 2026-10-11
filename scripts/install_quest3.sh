#!/usr/bin/env bash
# ==============================================================================
# MK Live Home Circuit - Meta Quest 3 Sideload Installer
# ==============================================================================
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
QUEST_APK="$PROJECT_ROOT/MK-Live-Cockpit-v1.1.0-Quest3.apk"

# Fallback to general apk if specific one not found
if [ ! -f "$QUEST_APK" ]; then
    QUEST_APK="$PROJECT_ROOT/android/app/build/outputs/apk/quest/release/app-quest-release.apk"
fi

echo "=========================================================="
echo "  🏎️ Mario Kart Live: Meta Quest 3 Installer"
echo "=========================================================="

if [ ! -f "$QUEST_APK" ]; then
    echo "❌ Error: Quest APK not found at $QUEST_APK!"
    echo "   Please build it first: cd android && ./gradlew assembleQuestRelease"
    exit 1
fi

echo "📦 Found Quest APK: $(basename "$QUEST_APK") ($(du -h "$QUEST_APK" | cut -f1))"

# Check ADB
if ! command -v adb >/dev/null 2>&1; then
    echo "❌ Error: adb is not installed or not in PATH."
    echo "   Install it with: sudo apt install adb (or android-tools-adb)"
    exit 1
fi

echo "🔍 Searching for connected ADB devices..."
DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')

if [ -z "$DEVICES" ]; then
    echo "⚠️  No ADB devices currently detected via USB or Wi-Fi."
    echo ""
    echo "To connect your Meta Quest 3:"
    echo "  1. Turn on Developer Mode in the Meta Quest mobile app."
    echo "  2. Connect your Quest 3 via USB-C cable to this PC."
    echo "  3. Put on the headset and click 'Always allow from this computer' on the prompt."
    echo "  (Optional wireless ADB: Run 'adb tcpip 5555' and 'adb connect <QUEST_IP>:5555')"
    echo ""
    read -rp "Enter Quest 3 IP address to connect wirelessly (or press Enter to retry USB): " QUEST_IP
    if [ -n "$QUEST_IP" ]; then
        echo "Connecting to $QUEST_IP:5555..."
        adb connect "$QUEST_IP:5555" || true
    fi
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | awk '{print $1}')
fi

if [ -z "$DEVICES" ]; then
    echo "❌ Still no device found. Please connect your Quest 3 and run this script again."
    exit 1
fi

TARGET_DEVICE=""
DEVICE_COUNT=$(echo "$DEVICES" | wc -l)

if [ "$DEVICE_COUNT" -eq 1 ]; then
    TARGET_DEVICE="$DEVICES"
else
    echo "Multiple devices detected:"
    select DEV in $DEVICES; do
        TARGET_DEVICE="$DEV"
        break
    done
fi

MODEL=$(adb -s "$TARGET_DEVICE" shell getprop ro.product.model 2>/dev/null || echo "Unknown")
echo "📱 Target device: $TARGET_DEVICE ($MODEL)"

echo "🚀 Installing $QUEST_APK..."
adb -s "$TARGET_DEVICE" install -r -g "$QUEST_APK"

echo " Granting audio and notification permissions..."
adb -s "$TARGET_DEVICE" shell pm grant com.example.mklive android.permission.RECORD_AUDIO 2>/dev/null || true
adb -s "$TARGET_DEVICE" shell pm grant com.example.mklive android.permission.POST_NOTIFICATIONS 2>/dev/null || true

echo ""
echo "=========================================================="
echo "  ✅ INSTALLATION SUCCESSFUL!"
echo "=========================================================="
echo "To launch the app in your Meta Quest 3:"
echo "  1. Put on your Quest 3 headset."
echo "  2. Open the App Library -> select the Filter dropdown (top right) -> 'Unknown Sources'."
echo "  3. Click on 'MK Live' (featuring the VR Mario icon)."
echo "  4. Press [Y] on your Left Touch Controller to toggle immersive VR Mode!"
echo "=========================================================="

read -rp "Do you want to launch the app right now on the headset? (y/N) " LAUNCH
if [[ "$LAUNCH" =~ ^[Yy]$ ]]; then
    adb -s "$TARGET_DEVICE" shell am start -n com.example.mklive/.MainActivity
    echo "🚀 App launched!"
fi
