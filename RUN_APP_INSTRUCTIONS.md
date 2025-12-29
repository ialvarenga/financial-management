# How to Run the GerenciadorFinanceiro App

## ✅ Build Status
The app has been **successfully built**! The debug APK is located at:
```
/home/ialvarenga/AndroidStudioProjects/GerenciadorFinanceiro/app/build/outputs/apk/debug/app-debug.apk
```

## 📱 Running the App

You have several options to run the app:

### Option 1: Using Android Studio (Recommended)
1. Open the project in Android Studio
2. Click the "Run" button (green triangle) or press Shift+F10
3. Select a target device (emulator or physical device)
4. The app will install and launch automatically

### Option 2: Using Gradle Command Line
If you have a device connected or emulator running:

```bash
cd /home/ialvarenga/AndroidStudioProjects/GerenciadorFinanceiro
./gradlew installDebug
```

Then launch the app:
```bash
# Set up environment
export ANDROID_SDK_ROOT=/home/ialvarenga/Android/Sdk
export PATH=$PATH:$ANDROID_SDK_ROOT/platform-tools

# Launch the app
adb shell am start -n com.example.gerenciadorfinanceiro/.MainActivity
```

### Option 3: Using the Script
I've created a helper script that automates everything:

```bash
cd /home/ialvarenga/AndroidStudioProjects/GerenciadorFinanceiro
./run-app.sh
```

### Option 4: Manual Installation
Install the APK directly on a connected device:

```bash
export ANDROID_SDK_ROOT=/home/ialvarenga/Android/Sdk
$ANDROID_SDK_ROOT/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
$ANDROID_SDK_ROOT/platform-tools/adb shell am start -n com.example.gerenciadorfinanceiro/.MainActivity
```

## 🔧 Prerequisites

Before running the app, ensure you have:

1. **A running Android emulator** OR **a physical device connected**
   
   To check for devices:
   ```bash
   $ANDROID_SDK_ROOT/platform-tools/adb devices
   ```

2. **To start an emulator** (if you have one configured):
   ```bash
   # List available emulators
   $ANDROID_SDK_ROOT/emulator/emulator -list-avds
   
   # Start an emulator
   $ANDROID_SDK_ROOT/emulator/emulator -avd <emulator_name> &
   ```

3. **To create an emulator** (if you don't have one):
   - Open Android Studio
   - Go to Tools → Device Manager
   - Click "Create Device"
   - Follow the wizard to create a virtual device

## 📋 Quick Start Checklist

- [x] App built successfully (app-debug.apk created)
- [ ] Android emulator is running OR physical device is connected
- [ ] USB debugging enabled (for physical devices)
- [ ] ADB can see the device (`adb devices` shows your device)

## 🎯 App Information

- **Package Name**: com.example.gerenciadorfinanceiro
- **Main Activity**: MainActivity
- **Build Type**: Debug
- **SDK Location**: /home/ialvarenga/Android/Sdk

## 🚨 Troubleshooting

### No devices found
- Make sure an emulator is running or a device is connected
- Enable USB debugging on physical devices (Settings → Developer Options)
- Run `adb devices` to verify the device is recognized

### ADB not found
- The SDK is located at: /home/ialvarenga/Android/Sdk
- Add to PATH: `export PATH=$PATH:/home/ialvarenga/Android/Sdk/platform-tools`

### App won't install
- Try uninstalling first: `adb uninstall com.example.gerenciadorfinanceiro`
- Then reinstall: `./gradlew installDebug`

## 🎉 Next Steps

Once you have a device/emulator ready, simply run:
```bash
cd /home/ialvarenga/AndroidStudioProjects/GerenciadorFinanceiro
./run-app.sh
```

This will automatically check for devices, install the app, and launch it!

