# JAVIS Launcher OS

> **Just A Very Intelligent System** — A futuristic AI-powered Android launcher that combines conversation, memory, voice, task planning, app control, contacts, notifications, online AI, and offline intelligence into one cohesive experience.

---

## Screenshots

> Futuristic dark UI with animated AI Orb, glassmorphism cards, and electric cyan accents.

---

## Features

### AI Brain Manager
- **Multi-provider fallback chain**: OpenRouter → Groq → DeepSeek → Together AI → Fireworks AI → Offline
- Automatic provider switching on failure — invisible to the user
- Full conversation context maintained across sessions
- Offline mode for core tasks without internet

### AI Orb
- Animated center-piece of the launcher
- States: Idle (breathing pulse), Listening (audio wave), Thinking (rotating rings), Speaking (voice visualization), Complete (glow)
- Tap to speak, hold for text input

### Voice System
- **Input**: Android SpeechRecognizer — fast, native, reliable
- **Output**: ElevenLabs (primary) with Android TTS fallback
- Continuous context across conversations

### Memory System
- Automatically extracts: name, nickname, preferences, interests, habits
- Persistent Room database storage
- Retrievable in natural conversation

### Task Planner & Agent Router
| Agent | Handles |
|-------|---------|
| App Agent | App launching, discovery |
| Contact Agent | Search, lookup |
| Call Agent | Phone calls |
| Message Agent | Draft & confirm messages |
| Alarm/Reminder Agent | Alarms, timers, reminders |
| Notification Agent | Summary, grouping |
| Memory Agent | Store & retrieve facts |
| Search Agent | Web search |

### Home Screen
- Personalized greeting (Morning/Afternoon/Evening)
- AI Orb as primary interaction
- Favorite & recent apps
- Notification summary
- Upcoming reminders
- Quick action buttons

### App Drawer
- All installed apps with category filtering
- Fast search
- Favorite pinning (long press)

### Mission Control Dashboard
- AI provider status and response speed
- Unread notifications
- Upcoming reminders
- System status

### Settings
- All 5 AI provider keys + model selection
- ElevenLabs voice configuration
- Greeting preferences
- Memory management
- Permissions checklist

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt |
| Database | Room |
| Preferences | DataStore |
| Networking | Retrofit + OkHttp |
| Concurrency | Coroutines + Flow |
| Navigation | Compose Navigation |

---

## AI Providers Setup

| Provider | Get Key |
|----------|---------|
| OpenRouter | https://openrouter.ai |
| Groq | https://console.groq.com |
| DeepSeek | https://platform.deepseek.com |
| Together AI | https://api.together.xyz |
| Fireworks AI | https://fireworks.ai |
| ElevenLabs | https://elevenlabs.io |

---

## Build

### Requirements
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 34
- Min SDK 26 (Android 8.0)

### Debug Build
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Release Build
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release-unsigned.apk
```

### GitHub Actions
Push to `main` branch → automatic debug APK build → uploaded as artifact

---

## Installation

See [INSTALL.md](INSTALL.md) for full installation guide.

---

## Architecture

```
app/
├── ai/                     # AI Brain Manager + 5 provider clients + Offline engine
├── agents/                 # AgentRouter + 6 specialized agents
├── data/
│   ├── db/                 # Room database: 7 entities + DAOs
│   └── preferences/        # DataStore preferences
├── di/                     # Hilt dependency injection
├── service/                # Foreground service, Notification listener, Accessibility
├── ui/
│   ├── components/         # JavisOrb, GlassCard, NotificationBadge
│   ├── screens/            # Home, AppDrawer, Conversation, Dashboard, Settings, Setup
│   ├── navigation/         # NavHost + routes
│   └── theme/              # JavisTheme, colors, typography
└── voice/                  # VoiceManager + ElevenLabsClient
```

---

## Permissions Required

| Permission | Purpose |
|-----------|---------|
| RECORD_AUDIO | Voice input |
| READ_CONTACTS, CALL_PHONE | Contact search & calling |
| FOREGROUND_SERVICE | Background AI service |
| POST_NOTIFICATIONS | Notification access |
| BIND_NOTIFICATION_LISTENER_SERVICE | Read notifications |
| BIND_ACCESSIBILITY_SERVICE | App detection |
| INTERNET | AI API calls |

---

## License

MIT License — feel free to fork and build upon JAVIS.

---

*Built with Kotlin + Jetpack Compose. Inspired by Jarvis, Nothing OS, and Pixel Launcher.*
