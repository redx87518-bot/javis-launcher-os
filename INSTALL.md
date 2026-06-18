# JAVIS Launcher OS — Installation Guide

## Method 1: Build from Source (Android Studio)

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17 (bundled with Android Studio)
- Android SDK 34
- Git

### Steps
1. **Clone the repository**
   ```bash
   git clone https://github.com/redx87518-bot/javis-launcher-os.git
   cd javis-launcher-os
   ```

2. **Open in Android Studio**
   - File → Open → select the `javis-android` folder
   - Wait for Gradle sync to complete

3. **Build the APK**
   - Menu: Build → Build Bundle(s)/APK(s) → Build APK(s)
   - Or run: `./gradlew assembleDebug`

4. **Locate the APK**
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

## Method 2: Download from GitHub Actions

1. Go to the repository on GitHub
2. Click **Actions** tab
3. Select the latest successful **Android Debug Build** workflow
4. Download the `JAVIS-debug-apk-*` artifact

## Installing on Your Device

### Enable Unknown Sources
- **Android 8+**: Settings → Apps → Special app access → Install unknown apps → Select your file manager → Allow
- **MIUI (Redmi A1)**: Settings → Privacy → Install unknown apps → Enable for your file manager

### Install Steps
1. Transfer the APK to your Redmi A1 (USB, Google Drive, Telegram, etc.)
2. Open your file manager and find the APK
3. Tap it and select **Install**
4. If prompted about Play Protect, tap **Install anyway**

### Set as Default Launcher
1. Press the **Home button**
2. A prompt will appear: "Select home app"
3. Choose **JAVIS** and tap **Always**

---

## First Launch Setup

1. **Welcome screen** — tap Continue
2. **Enter your name** — JAVIS will use this to greet you
3. **Add AI Provider key** — at minimum, add one OpenRouter or Groq API key
4. **Voice setup** — optionally add ElevenLabs key for premium voice
5. **Activate JAVIS** — your AI companion is now live

---

## Permissions Setup (Important)

JAVIS needs several special permissions that Android doesn't grant automatically:

### Notification Access
- Settings → Apps → Special app access → Notification access → Enable **JAVIS**

### Accessibility Service
- Settings → Accessibility → JAVIS Accessibility Service → Enable

### Other Permissions
JAVIS will request these on first use:
- Microphone (voice input)
- Contacts (contact search & calling)
- Phone (make calls)
- Notifications (Android 13+)

---

## Performance Tips for Redmi A1

- Keep **Background App Limit** set to standard
- Add JAVIS to battery optimization **whitelist**
- Settings → Battery → Battery optimization → JAVIS → Don't optimize
- This ensures JAVIS stays running and responds immediately

---

## Getting API Keys

| Service | URL | Free Tier |
|---------|-----|-----------|
| OpenRouter | https://openrouter.ai/keys | Yes |
| Groq | https://console.groq.com/keys | Yes (fast) |
| DeepSeek | https://platform.deepseek.com | Yes |
| Together AI | https://api.together.xyz | $1 free |
| Fireworks AI | https://fireworks.ai | Free tier |
| ElevenLabs | https://elevenlabs.io | 10k chars/month free |

---

## Troubleshooting

**JAVIS doesn't respond to voice**
- Check microphone permission is granted
- Ensure JAVIS foreground service is running (notification in status bar)

**App doesn't show in launcher**
- Force-stop JAVIS and reopen it
- Re-set as default launcher from Settings

**AI responses are slow**
- Check internet connection
- Try switching to Groq (fastest provider)

**Notifications not showing**
- Enable Notification Access in special permissions
- See permissions section above
