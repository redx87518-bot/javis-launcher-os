# JAVIS Launcher OS — Installation Guide

## Requirements

- Android 8.0 (API 26) or higher
- Minimum 1GB RAM (optimized for low-end devices)
- ~50MB storage
- Internet connection for AI features (offline mode available)

## Method 1: Download APK from GitHub Actions

1. Go to the [Actions tab](https://github.com/redx87518-bot/javis-launcher-os/actions)
2. Click the latest successful **Build Debug APK** workflow
3. Under **Artifacts**, download `javis-launcher-debug-*.zip`
4. Extract the APK
5. Transfer to your Android device
6. Install (enable "Unknown Sources" if prompted)

## Method 2: Build from Source

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK API 35

### Steps

```bash
# Clone the repository
git clone https://github.com/redx87518-bot/javis-launcher-os.git
cd javis-launcher-os

# Build debug APK
./gradlew assembleDebug

# The APK will be at:
# app/build/outputs/apk/debug/app-debug.apk
```

### Install via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Setting JAVIS as Default Launcher

1. Go to **Android Settings** → **Apps** → **Default Apps**
2. Select **Home App** or **Launcher**
3. Choose **JAVIS Launcher**
4. Press the Home button — JAVIS will launch

**Or:** When you press Home, Android will ask "Which launcher?" — choose JAVIS and tap "Always".

## First-Time Setup

1. **Welcome Screen** — Tap "INITIALIZE JAVIS"
2. **Identity** — Enter your name and how JAVIS should address you
3. **Permissions** — Grant the requested permissions:
   - Microphone (required for voice)
   - Contacts (for calling assistance)
   - Notifications (for smart summaries)
   - Phone (for call automation)
4. **Launch** — Tap "LAUNCH JAVIS"

## Granting Special Permissions

### Notification Access
1. Settings → Apps → Special App Access → Notification Access
2. Enable **JAVIS Launcher**

### Accessibility Service
1. Settings → Accessibility → Downloaded Apps
2. Enable **JAVIS Assistant**

## Adding API Keys (Required for AI)

1. Tap the **Settings** icon on the home screen
2. Go to **AI Providers**
3. Add at least one API key:
   - **OpenRouter** (recommended): [openrouter.ai](https://openrouter.ai) — has free models
   - **Groq**: [console.groq.com](https://console.groq.com) — free tier available

## Troubleshooting

**JAVIS won't respond to voice:**
- Check microphone permission in Android Settings
- Ensure Google Speech Recognition is installed

**AI responses not working:**
- Verify API key is correct in Settings
- Check internet connection
- Try "Test Connection" button

**App won't launch after tapping:**
- Ensure JAVIS is set as default launcher
- Check battery optimization — exclude JAVIS from optimization

**Notifications not showing:**
- Enable Notification Access (see above)
