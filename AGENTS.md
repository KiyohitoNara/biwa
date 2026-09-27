# AGENTS.md

## Project Overview

**Biwa** is a Kotlin Multiplatform (KMP) media library app for Android and iOS.

## Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose Multiplatform (Android), SwiftUI (iOS)
- **Architecture**: Clean Architecture + MVVM
- **Build System**: Gradle
- **Format**: ktlint (Android/shared), SwiftLint (iOS)
- **Lint**: ktlint (Android/shared), SwiftLint (iOS)
- **Test**: kotlin.test (multiplatform), JUnit4 (Android/JVM runner), kotlinx-coroutines-test

## Development Workflow

### Branch Strategy

This project follows GitLab Flow.

## Development Commands

**Build:**
```bash
# Android
./gradlew :composeApp:assembleDebug

# iOS
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

**Run:**
```bash
# Android 
./gradlew :composeApp:installDebug

# iOS
xcrun simctl boot "iPhone 17"
xcrun simctl install "iPhone 17" iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app
xcrun simctl launch "iPhone 17" io.github.kiyohitonara.biwa.Biwa
```

**Format:**
```bash
./gradlew ktlintFormat
```

**Lint:**
```bash
./gradlew ktlintCheck
```

**Test:**
```bash
# Android unit tests
./gradlew :composeApp:testDebugUnitTest
./gradlew :shared:testDebugUnitTest

# Android instrumented tests
./gradlew :composeApp:connectedDebugAndroidTest
./gradlew :shared:connectedDebugAndroidTest

# iOS unit tests
./gradlew :shared:iosSimulatorArm64Test
```

### Other

**Generate SQLDelight code:**
```bash
./gradlew generateCommonMainBiwaDatabase
```

## Project Structure

```
Biwa/
├── shared/     # Domain + data layers
├── composeApp/ # Android UI
└── iosApp/     # iOS UI
```
