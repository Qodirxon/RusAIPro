# RusPro AI — v2.0 FINAL

Final polished source project for an offline-first Russian learning app.

## Final feature set
- A1–C1 placement and adaptive level
- 520 scenario missions across 10 domains
- Work, Travel, Daily Life, Interview, Negotiation, Customer Service, Study, Social, Technology, Leadership
- Branching/adaptive conversation flow
- Voice recognition (`ru-RU`)
- Russian Text-to-Speech
- Adaptive local Coach
- Error DNA: grammar / vocabulary / relevance / fluency
- Spaced repetition vocabulary
- Daily plan
- XP, levels, streaks and achievements
- Boss simulations
- Local progress persistence
- Analytics dashboard
- Profile and learning preferences
- Offline-first architecture; no mandatory account/server
- One-click build scripts for Windows/macOS/Linux once Android SDK + Gradle are available

## Build the APK
This environment does not contain the Android SDK/Gradle distribution, so an APK cannot be honestly compiled here.

### Android Studio
1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Connect your Android phone (USB debugging) or start an emulator.
4. `Build` → `Build APK(s)`.
5. Debug APK will be under `app/build/outputs/apk/debug/`.

### Windows
Run `BUILD_APK.bat`.

### macOS/Linux
Run `./build_apk.sh`.

The scripts call the Gradle wrapper when it is available and otherwise explain what needs to be installed.


## v2.0.1 Stability / Performance Pass
- Responsive mission list with horizontal filters
- Scroll-safe mission screen for small phones
- `rememberSaveable` for important UI state
- Stable list keys for Compose
- Release minification + resource shrinking
- APK packaging cleanup
- Resizable activity support for different screen sizes
- Local-only permissions: microphone is requested only when voice mode is used
- No INTERNET permission

## GitHub APK build
1. Create a GitHub repository.
2. Upload the CONTENTS of this project to the repository root.
3. Commit to `main`.
4. Open **Actions** → **Build RusPro AI APK**.
5. Press **Run workflow**.
6. Open the completed run → **Artifacts** → `RusProAI-debug-APK`.
7. Download and extract the artifact; the APK is inside.

The workflow installs Android SDK 35, JDK 17 and Gradle 8.9 automatically.
