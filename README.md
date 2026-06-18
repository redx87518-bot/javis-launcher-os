# JAVIS Launcher OS

<p align="center">
  <img src="docs/javis_banner.png" width="600" alt="JAVIS Launcher OS">
</p>

<p align="center">
  <strong>Your AI Companion. Your Mission Control.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-26%2B-green?logo=android" />
  <img src="https://img.shields.io/badge/Kotlin-2.0-purple?logo=kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-Latest-blue" />
  <img src="https://img.shields.io/badge/AI-Multi--Provider-red" />
  <img src="https://github.com/redx87518-bot/javis-launcher-os/actions/workflows/android-build.yml/badge.svg" />
</p>

---

## What is JAVIS Launcher OS?

JAVIS (Just A Very Intelligent System) is a production-quality Android AI Launcher that transforms your Android device into an intelligent command center — inspired by the J.A.R.V.I.S. AI from Iron Man.

JAVIS becomes your:
- 🏠 **Home Screen** — Futuristic AI dashboard
- 🤖 **AI Companion** — Conversational intelligence
- 🎙️ **Voice Assistant** — Hands-free control
- 📋 **Task Planner** — Intent → Plan → Execute
- 🧠 **Memory System** — Remembers your habits & preferences
- 🔔 **Notification Center** — Smart summaries
- 📞 **Contact Assistant** — Calling & messaging
- 🔍 **Search Engine** — Apps, contacts, web, files
- 🎯 **Mission Control** — Live system dashboard

## Features

### 🤖 AI Brain System
Multi-provider AI with automatic failover:
1. **OpenRouter** — 100+ models including free tiers
2. **Groq** — Ultra-fast LPU inference
3. **DeepSeek** — DeepSeek V2 & Coder
4. **Together AI** — Fast open-source models
5. **Fireworks AI** — Blazing fast inference

### 🎙️ Voice System
- **ElevenLabs** high-quality TTS (primary)
- **Android TTS** fallback
- Configurable voice ID, speed, and style

### 🎯 AI Core
- Futuristic mechanical design
- Animated states: Idle → Listening → Thinking → Speaking → Executing
- Red energy rings, particle effects, green status indicators

### 🧠 Memory System
- Stores habits, preferences, and routines via Room Database
- Conversation history with session context
- Favorite apps & contacts

### 📱 Optimized for Low-End Devices
- Target: Redmi A1 & budget Android devices
- Efficient coroutines for background processing
- Minimal memory footprint
- Battery-aware operations

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Database | Room + DataStore |
| Networking | Retrofit + OkHttp |
| Concurrency | Kotlin Coroutines + Flow |
| Build | Gradle 8.9 + AGP 8.5 |

## Installation

### Download APK
Get the latest APK from [GitHub Actions Artifacts](https://github.com/redx87518-bot/javis-launcher-os/actions).

### Build from Source
```bash
git clone https://github.com/redx87518-bot/javis-launcher-os.git
cd javis-launcher-os
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Setup

1. Install APK on your Android device (API 26+)
2. Set JAVIS as your default launcher
3. Complete the onboarding (name + nickname)
4. Add your API keys in **Settings → AI Providers**:
   - At least one AI provider key (OpenRouter has free models)
   - Optional: ElevenLabs for premium voice
5. Grant permissions when prompted

## AI Provider Setup

### OpenRouter (Recommended — Free Tier Available)
1. Sign up at [openrouter.ai](https://openrouter.ai)
2. Get API key from dashboard
3. In JAVIS Settings → OpenRouter → paste key
4. Default model: `meta-llama/llama-3.1-8b-instruct:free` (free)

### Groq (Fastest)
1. Sign up at [console.groq.com](https://console.groq.com)
2. Get free API key
3. Default model: `llama-3.1-8b-instant`

### ElevenLabs Voice
1. Sign up at [elevenlabs.io](https://elevenlabs.io)
2. Get API key (free tier: 10k chars/month)
3. Default voice: Adam (ID: `pNInz6obpgDQGcFmaJgB`)

## Voice Commands

| Command | Action |
|---------|--------|
| "Open YouTube" | Launch app |
| "Call Musa" | Search contacts & call |
| "Set alarm for 7 AM" | Create alarm |
| "Search football highlights" | Web search |
| "Tell John I'm on my way" | Compose SMS (with confirmation) |
| "How was my day?" | Companion conversation |
| "Open WhatsApp and tell..." | Multi-step task |

## Architecture

```
app/
├── brain/          # BrainManager — AI orchestration & failover
├── voice/          # VoiceManager — ElevenLabs + Android TTS
├── tasks/          # TaskPlanner — Intent → Plan → Execute
├── agents/         # Specialized task agents
├── data/
│   ├── model/      # Domain models & Room entities
│   ├── local/      # Room database & DAOs
│   └── network/    # Retrofit API services
├── di/             # Hilt dependency injection modules
├── services/       # Foreground service, notification listener
├── accessibility/  # Accessibility service for automation
├── receivers/      # Boot, unlock broadcast receivers
└── ui/
    ├── home/       # Main launcher home screen
    ├── settings/   # Full settings screen
    ├── mission/    # Mission Control dashboard
    └── onboarding/ # First-run setup wizard
```

## Permissions

| Permission | Purpose |
|-----------|---------|
| `RECORD_AUDIO` | Voice activation |
| `READ_CONTACTS` | Contact search & calling |
| `CALL_PHONE` | Making calls |
| `SEND_SMS` | Message assistance |
| `BIND_NOTIFICATION_LISTENER_SERVICE` | Notification summaries |
| `BIND_ACCESSIBILITY_SERVICE` | App automation |
| `SET_ALARM` | Alarm creation |
| `FOREGROUND_SERVICE` | Background AI context |

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

## Roadmap

See [ROADMAP.md](ROADMAP.md) for planned features.

## License

MIT License — see [LICENSE](LICENSE) for details.

---

<p align="center">
  Built with ❤️ for the Android community
</p>
