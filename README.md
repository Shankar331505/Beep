# Beep — hear what matters

**Beep** is an Android app that listens to the world around you and taps you on the shoulder when something important happens: a smoke alarm, a siren, the doorbell, a baby crying, a car horn, or **someone calling your name**. It is designed for people wearing headphones, people in deep focus, and Deaf and hard-of-hearing users.

When Beep recognises a sound it can **pause your music or video**, play a short chime in your headphones, vibrate a pattern that tells you *what* happened, strobe the flashlight, and show a full-screen, colour-coded alert — even on the lock screen.

Everything runs **on the phone**. Beep never records, stores or uploads audio, and it has **no internet permission at all**.

---

## Features

| | |
|---|---|
| **23 recognisable sounds** | Safety (smoke/fire alarm, siren, breaking glass, crash, scream, car alarm, sudden loud noise), People (your name, shouting, baby crying, talking), Home (doorbell, knock, phone, timers/appliances, running water, dog, cat), Outside (car horn, reversing vehicle, bicycle bell, train horn, whistle). |
| **Your name** | Record your name 3 times; Beep spots it in live audio — even inside "hey Shankar, come here" — without the speech recogniser and without stopping your music. |
| **Teach Beep a sound** | Record any sound 3 times (your microwave, intercom, a pet) and Beep learns to recognise it. |
| **Modes** | One tap presets: Everyday, Focus, Home, Outside, Night. Every sound can also be switched and tuned individually. |
| **Per-sound sensitivity with a live meter** | Slide sensitivity and watch a live bar cross the trigger line as you make the sound, so you can tune it in seconds. |
| **Multi-sensory alerts** | Media pause → chime → category vibration → torch strobe → heads-up or full-screen alert. Each category has its own colour, melody and vibration. |
| **"What Beep hears"** | The home screen shows a live spectrogram and the AI's top guesses in real time. |
| **Timeline** | A private log of every alert with a 7-day chart and statistics. |
| **Smart idle** | In a silent room the neural network is skipped entirely, which saves battery. |
| **Quick Settings tile** | Turn listening on or off from the notification shade. |
| **Snooze** | "Mute 10 min" for one sound, straight from the alert. |

---

## How it works

```
 Microphone 16 kHz mono
        │  50 ms chunks (no audio focus taken — your music keeps playing)
        ▼
 ┌──────────────────── DetectionEngine (pure Kotlin) ─────────────────────┐
 │                                                                         │
 │  every chunk ─┬─► Level meter (RMS → dB)                                │
 │               ├─► Spectrum analyser (FFT → 48 log bands) → spectrogram  │
 │               ├─► LoudnessWatcher (spike over a 10 s room baseline)     │
 │               └─► NameSpotter (VAD → MFCC → subsequence DTW)            │
 │                                                                         │
 │  every 0.5 s ─► ring buffer (0.975 s) ─► YAMNet (TFLite, 521 classes)   │
 │                     ├─► per-sound scoring + temporal smoothing          │
 │                     ├─► custom-sound fingerprints (cosine similarity)   │
 │                     └─► speech probability → gates the NameSpotter      │
 │                                                                         │
 │  cooldown + snooze ─► Detection                                         │
 └─────────────────────────────────────────────────────────────────────────┘
        ▼
 Alerter: pause media → chime → vibration → torch → notification / full screen
        ▼
 EventLog (timeline)
```

### 1. Recognising everyday sounds — YAMNet with temporal smoothing
Beep runs Google's **YAMNet** (MobileNet v1 trained on AudioSet) locally with TensorFlow Lite. Twice a second, the most recent 0.975 s of audio (15 600 samples) is classified into 521 AudioSet classes. Each Beep sound maps to one or more of those classes; for example *Siren* is classes 316–319, 390 and 391.

A single high score is not enough. Each sound has a **shape**:
- **Impulse** sounds (knock, glass, horn) fire on one confident frame.
- **Sustained** sounds (alarms, crying, sirens) must score above the threshold in **2 of the last 3** frames, which removes most false alarms.

The sensitivity slider maps to a threshold from 0.75 (strict) down to 0.15 (very sensitive). A per-sound **cooldown** makes sure one doorbell produces one alert.

### 2. Recognising your name — MFCC + Dynamic Time Warping
General speech recognisers grab audio focus, which pauses your music, and they time out in the background. Beep instead does classic **keyword spotting** from scratch:

1. **Enrolment:** each of your recordings is trimmed to the voiced part and turned into **MFCCs** (25 ms frames, 10 ms hop, 26 mel filters, 12 coefficients, cepstral mean normalisation).
2. **Self-calibration:** Beep measures how different your own samples are from each other. That number becomes the yardstick for "close enough", so the threshold adapts to each name and voice.
3. **Live:** a voice-activity detector (energy above an adaptive noise floor) cuts the stream into short utterances. Each one is compared with every sample using **subsequence DTW**, which finds the name *anywhere* inside a longer phrase and tolerates faster or slower speech.
4. **Gate:** the match only counts if YAMNet agreed someone was speaking in the last 2.5 s.

### 3. Teaching Beep a new sound — few-shot fingerprints
For each example recording, Beep finds the loudest second and builds a **fingerprint** from two views of it:
- **semantic:** the square root of YAMNet's 521 class scores (what kind of sound it is), and
- **acoustic:** the mean and spread of its MFCCs (its timbre).

Both views are L2-normalised and combined. At run time, live fingerprints are compared with the stored examples by cosine similarity. Using both views lets Beep tell apart two different beeps that YAMNet would label the same way.

### 4. Sudden loud noises — no AI needed
`LoudnessWatcher` keeps the **median** level of the last 10 seconds as the room's baseline. It fires when a burst is both loud in absolute terms (75–95 dB depending on sensitivity) *and* at least 18 dB above that baseline. A busy street therefore doesn't trigger constantly, but a crash in a quiet room does.

### 5. Alerting
- **Media pause:** Beep requests *transient* audio focus. Well-behaved players (Spotify, YouTube, podcasts) pause and **resume automatically** when focus is released, 4 s after the chime, or when you tap "Got it" if *Keep paused until I respond* is on.
- **Chimes** are synthesised in code (sine + overtones with an envelope): a different melody per category, so with practice you can tell sounds apart by ear.
- **Vibration patterns:** an urgent triple for Safety, a tap-tap for People, knock-knock for Home, a horn-like buzz for Outside.
- **Torch strobe** through `CameraManager.setTorchMode`. This needs no camera permission.
- **Full-screen alert** over the lock screen through a full-screen-intent notification. If the app is already open, the alert takes over the screen in-app instead.

---

## Architecture

```
com.shankar.beep
├── audio/        Dsp (FFT, RMS, dB), Mfcc, Dtw, SpectrumAnalyzer, MicStream (AudioRecord → Flow)
├── ml/           YamNet (TFLite interpreter wrapper) + SoundClassifier interface
├── detect/       SoundCatalog & Modes, DetectionEngine, NameSpotter, Fingerprint, LoudnessWatcher
├── alert/        Alerter, Chime (synth), Haptics, Torch, MediaPauser, Notifier, AlertActionReceiver
├── data/         SettingsRepository (DataStore), EventLog (JSON), VoiceStore (templates & fingerprints)
├── service/      ListeningService (foreground, type=microphone), ListeningHub (state), BeepTileService
├── ui/           Compose screens: home, sounds, record (name & teach), timeline, settings, onboarding, alert
├── BeepApp.kt    Application + hand-rolled dependency container
├── MainActivity  Navigation, permissions, in-app alert overlay
└── AlertActivity Full-screen lock-screen alert
```

- **Kotlin, Jetpack Compose and Material 3** with a custom "Beacon" design language: a calm near-black canvas, one signal colour, one hue per sound category, and dark and light themes.
- **MVVM with unidirectional data flow:** the service publishes to `ListeningHub` (StateFlows), and screens observe through ViewModels.
- **Testable core:** `DetectionEngine` and everything in `audio/` and `detect/` are pure Kotlin. YAMNet sits behind the `SoundClassifier` interface, so the engine is tested on the JVM with a fake classifier.
- **No internet permission and no analytics.** Only derived numbers (MFCC templates, fingerprints) and alert labels are stored, in app-private storage.

---

## Build and run

Requirements: Android Studio (or JDK 17 + Android SDK 35), and a device or emulator running Android 8.0 (API 26) or newer with a microphone.

```bash
./gradlew :app:assembleDebug        # build app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest    # run the unit tests
./gradlew :app:installDebug         # install on a connected device
```

### Tests
`./gradlew :app:connectedDebugAndroidTest` runs 5 on-device tests against the real YAMNet model: it loads with all 521 labels, recognises silence, reacts to sound, separates custom-sound fingerprints, and the full engine processes 5 s of audio in a few tens of milliseconds.

25 JVM unit tests cover the FFT, dB maths, MFCC normalisation, DTW and subsequence DTW, the streaming name spotter, custom-sound fingerprints, sustained-versus-impulse logic, cooldowns, snooze, sensitivity thresholds, smart idle, the adaptive loudness detector, catalogue validity, and storage round-trips.

---

## Limitations and honest notes
- Phone microphones are not calibrated, so dB values are **approximate** (dBFS + 90).
- Name spotting is template-based: it works best when the samples sound like the way people actually call you. Adding a sample recorded by a friend helps with other voices.
- Some phone makers aggressively kill background apps. Settings → *Keep Beep running* explains how to allow unrestricted battery use.
- On Android 14+, full-screen alerts need the user's permission (a shortcut is in Settings).

## Future work
- Direction of arrival using the phone's multiple microphones ("knock to your left").
- Wear OS companion for wrist haptics.
- Live captions of what was said right after your name.
- Embedding-based custom sounds using YAMNet's internal 1024-d embedding layer.
