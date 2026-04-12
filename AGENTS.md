# AGENTS.md

## Project Overview

**Biwa** is a Kotlin Multiplatform (KMP) media library app for Android and iOS.
It manages photos, videos, and GIFs with tagging, playback tracking, and A/B point marking for video replay analysis.
The UI is built with Jetpack Compose Multiplatform.

## Technology Stack
- **Language**: Kotlin 2.3.0
- **UI Framework**: Jetpack Compose Multiplatform
- **Architecture**: Clean Architecture + MVVM
- **Navigation**: AndroidX Navigation Compose
- **Dependency Injection**: Koin
- **Database**: SQLDelight
- **Media Playback**: ExoPlayer
- **Image Loading**: Coil
- **Build System**: Gradle 9.1.0 (Kotlin DSL), version catalog at `gradle/libs.versions.toml`

## Project Structure

The project follows a two-module architecture:

```
Biwa/
├── shared/        # Domain + data layers (KMP: commonMain, androidMain, iosMain)
└── composeApp/    # Presentation layer (Compose Multiplatform: commonMain, androidMain)
```

### shared

```
shared/src/
├── commonMain/  # Domain models, use cases, repository interfaces, AppModule (Koin)
├── androidMain/ # Android impls: SQLite driver, SharedPreferences, MediaStore extraction
├── iosMain/     # iOS impls: Native SQLite, NSUserDefaults, Photos framework
├── commonTest/  # Use case unit tests with fake repositories
└── androidUnitTest/ # Repository integration tests with in-memory SQLite
```

### composeApp

```
composeApp/src/
├── commonMain/  # ViewModels, UiState models, ViewModelModule (Koin)
└── androidMain/ # MainActivity, BiwaApplication, Screen composables, NavHost
```

### Architecture Layers

```
Screen (Composable)
    ↓
ViewModel  [composeApp/commonMain]
    ↓
Use Case   [shared/commonMain/domain/usecase]
    ↓
Repository Interface  [shared/commonMain/domain/repository]
    ↓
Repository Impl + SQLDelight  [shared/commonMain+androidMain/data]
```

- ViewModels depend only on use cases — never on repositories directly.
- State is exposed as `StateFlow<UiState>`; one-shot events use `SharedFlow`.
- SQL queries are defined in `.sq` files under `shared/src/commonMain/sqldelight/`.

## Development Workflow

### Android

**Build:**
```bash
./gradlew :composeApp:assembleDebug
```

**Install on connected device:**
```bash
./gradlew :composeApp:installDebug
```

**Unit tests (JVM):**
```bash
./gradlew :composeApp:testDebugUnitTest
./gradlew :shared:testDebugUnitTest
```

**Instrumented tests (requires connected device or emulator):**
```bash
./gradlew :composeApp:connectedDebugAndroidTest
./gradlew :shared:connectedDebugAndroidTest
```

### iOS

iOS builds are driven by Xcode via the `iosApp/` project. Gradle is used only to build the shared KMP framework embedded into the Xcode project.

**Build shared framework for simulator:**
```bash
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

**Build shared framework for device:**
```bash
./gradlew :shared:linkDebugFrameworkIosArm64
```

**Run shared unit tests on iOS simulator:**
```bash
./gradlew :shared:iosSimulatorArm64Test
```

Full iOS app builds and device tests are performed from Xcode.

### Other

**Generate SQLDelight code:**
```bash
./gradlew generateCommonMainBiwaDatabase
```
