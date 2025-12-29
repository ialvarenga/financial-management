#!/bin/bash

# Set up Android SDK paths
export ANDROID_SDK_ROOT=/home/ialvarenga/Android/Sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools:$ANDROID_SDK_ROOT/emulator:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin

echo "========================================="
echo "Android App Runner Script"
echo "========================================="
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ ADB not found. Please check your Android SDK installation."
    exit 1
fi

echo "✓ ADB found: $(adb version 2>&1 | head -1)"
echo ""

# Start ADB server
echo "Starting ADB server..."
adb start-server 2>&1
echo ""

# Check for connected devices
echo "Checking for connected devices..."
DEVICES=$(adb devices | grep -v "List" | grep "device$" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ No devices connected."
    echo ""
    echo "Available emulators:"
    emulator -list-avds 2>&1
    echo ""
    echo "To run the app, you need to either:"
    echo "  1. Connect a physical Android device via USB (with USB debugging enabled)"
    echo "  2. Start an Android emulator"
    echo ""
    echo "To start an emulator (if you have one configured):"
    echo "  emulator -avd <emulator_name> &"
    echo ""
    echo "Or use Android Studio to create and start an emulator."
    exit 1
fi

echo "✓ Found $DEVICES device(s):"
adb devices -l
echo ""

# Install the app
echo "Installing app..."
cd /home/ialvarenga/AndroidStudioProjects/GerenciadorFinanceiro
./gradlew installDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ App installed successfully!"
    echo ""
    echo "Starting app..."
    # Start the main activity
    adb shell am start -n com.example.gerenciadorfinanceiro/.MainActivity
    echo ""
    echo "✓ App launched!"
else
    echo ""
    echo "❌ Installation failed. Check the error messages above."
    exit 1
fi

