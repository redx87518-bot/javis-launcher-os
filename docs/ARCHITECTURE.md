# JAVIS Launcher OS — Architecture

## Overview

JAVIS follows MVVM + Clean Architecture principles optimized for Android launchers.

```
┌─────────────────────────────────────────────────────────┐
│                    JAVIS Launcher OS                     │
├─────────────────────────────────────────────────────────┤
│  UI Layer (Jetpack Compose)                             │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │  Home    │ │Settings  │ │Mission   │ │Onboarding│  │
│  │Screen   │ │Screen    │ │Control   │ │Wizard    │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
├─────────────────────────────────────────────────────────┤
│  ViewModel Layer (StateFlow + Coroutines)                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │HomeViewModel │  │SettingsVM    │  │MissionVM     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│  Domain Layer                                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │BrainManager  │  │TaskPlanner   │  │VoiceManager  │  │
│  │(AI Failover) │  │(Intent→Exec) │  │(TTS/STT)     │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
├─────────────────────────────────────────────────────────┤
│  Data Layer                                             │
│  ┌─────────────────────┐  ┌─────────────────────────┐  │
│  │  Room Database       │  │  Retrofit API Services  │  │
│  │  - Conversations     │  │  - OpenRouter           │  │
│  │  - Memory            │  │  - Groq                 │  │
│  │  - Installed Apps    │  │  - DeepSeek             │  │
│  │  - Tasks             │  │  - Together AI          │  │
│  │  - Notifications     │  │  - Fireworks AI         │  │
│  │  - Contacts          │  │  - ElevenLabs TTS       │  │
│  └─────────────────────┘  └─────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│  System Services                                        │
│  ┌─────────────────┐  ┌─────────────┐  ┌────────────┐  │
│  │ ForegroundSvc   │  │NotifListener│  │Accessibility│ │
│  │ (always alive)  │  │(read notifs)│  │(app control)│ │
│  └─────────────────┘  └─────────────┘  └────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## AI Failover Chain

```
User Request
     │
     ▼
BrainManager
     │
     ▼ (priority order)
1. OpenRouter ──→ Success? ──→ Return response
     │ Fail
     ▼
2. Groq ──────→ Success? ──→ Return response
     │ Fail
     ▼
3. DeepSeek ──→ Success? ──→ Return response
     │ Fail
     ▼
4. Together ──→ Success? ──→ Return response
     │ Fail
     ▼
5. Fireworks → Success? ──→ Return response
     │ Fail
     ▼
6. Offline ───→ Pattern-based response
```

## Task Execution Pipeline

```
User Input (voice/text)
        │
        ▼
   TaskPlanner
        │
        ▼
  BrainManager.chat()
        │ (AI response with embedded action JSON)
        ▼
  extractAndExecuteAction()
        │
   ┌────┴────┐
   │  JSON   │
   │ Action  │
   └────┬────┘
        │
   ┌────▼────────────────────────────────┐
   │          Action Router              │
   │  OPEN_APP → PackageManager.launch   │
   │  CALL_CONTACT → Intent.ACTION_CALL  │
   │  SET_ALARM → AlarmClock intent      │
   │  SEARCH_WEB → Browser intent        │
   │  SEND_SMS → (with confirmation)     │
   └────────────────────────────────────┘
```

## Memory Architecture

JAVIS stores memories in 4 categories:
- **profile** — name, nickname, preferences
- **habit** — recurring behaviors learned over time
- **preference** — user-stated preferences
- **routine** — daily schedule patterns

The BrainManager injects relevant memories into the system prompt on every request.

## Voice Pipeline

```
Text → VoiceManager
            │
            ▼ (if ElevenLabs key set)
       ElevenLabs API
            │ Success → Play MP3
            │ Fail
            ▼
       Android TTS fallback
```
