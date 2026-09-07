# NotifyRelay

Forward notifications from selected apps on a Samsung / Android phone to Telegram on an iPhone.

The iPhone does not need a custom app. Telegram is the inbox.

## What it does

- Reads Android notifications through Notification Access
- Forwards **only** the apps you allow
- Skips ongoing / persistent notifications (music, maps, charging)
- Skips group summaries so you do not get duplicates
- Optional include / exclude keyword filters
- Sends each alert as a Telegram message

Do **not** allowlist banking, authenticator, or SMS OTP apps. Telegram bots are not end-to-end encrypted.

## Setup

### 1. Install on the Samsung phone

Open this folder in Android Studio, connect the Samsung phone with USB debugging, and run the `app` configuration. Or build a debug APK and sideload it:

```bash
./gradlew assembleDebug
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`.

### 2. Create a Telegram bot (on the iPhone)

1. Open Telegram and chat with [@BotFather](https://t.me/BotFather)
2. Send `/newbot` and follow the prompts
3. Copy the bot token
4. Open your new bot and tap **Start**

### 3. Connect NotifyRelay

1. Open NotifyRelay on the Samsung
2. Enable **Notification access** for NotifyRelay
3. Open **Telegram**, paste the bot token, tap **Detect chat ID**
4. Tap **Send test message** and confirm it arrives on the iPhone
5. Choose the apps to forward
6. Turn **Forwarding** on

### 4. Stop Samsung from killing the app

One UI is aggressive about background apps. Do all of these:

- Battery usage for NotifyRelay → **Unrestricted**
- Allow background activity
- Lock NotifyRelay in Recents (tap the app icon → Lock)
- Keep the Samsung on Wi‑Fi or mobile data, and charged enough to stay awake

## Filters

| Setting | Default | Purpose |
|---|---|---|
| Ignore ongoing notifications | On | Drops persistent status notifications |
| Ignore group summaries | On | Drops bundled “3 new messages” wrappers |
| Only if text contains | Empty | If set, the title or body must contain this text |
| Skip if text contains | Empty | If set, matching notifications are dropped |

An empty allowlist forwards nothing.

## Privacy

The bot token and chat ID stay in app private storage on the Samsung. Notification text is sent to Telegram’s servers so the iPhone can receive it. Treat this like forwarding those alerts through a third party.
