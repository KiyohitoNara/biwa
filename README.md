This is a Kotlin Multiplatform project targeting Android, iOS.

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
    Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
    folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

## Architecture

Biwa follows Clean Architecture + MVVM. Dependencies point inward only:
the UI talks to ViewModels, ViewModels depend on use cases (never on repositories directly),
and use cases depend on repository interfaces whose implementations live in the data layer.
Platform-specific concerns (database, preferences, media extraction) are provided through
Kotlin Multiplatform's `expect`/`actual` mechanism.

```mermaid
flowchart TD
    subgraph Presentation["Presentation Layer"]
        Android["Android UI<br/>(Compose: MainActivity, Screens, NavHost)"]
        iOS["iOS UI<br/>(SwiftUI views, ViewModel reused via SKIE)"]
        VM["ViewModel<br/>StateFlow&lt;UiState&gt; / SharedFlow&lt;Event&gt;<br/>[shared/commonMain]"]
        Android --> VM
        iOS --> VM
    end

    subgraph Domain["Domain Layer  [shared/commonMain]"]
        UC["Use Case<br/>(business logic)"]
        RepoIf["Repository Interface"]
        UC --> RepoIf
    end

    subgraph Data["Data Layer  [shared/commonMain + platform main]"]
        RepoImpl["Repository Impl"]
        DB["SQLDelight<br/>(.sq files)"]
        Pref["Preferences Storage"]
        Media["Media Extraction<br/>+ Thumbnails"]
        RepoImpl --> DB
        RepoImpl --> Pref
        RepoImpl --> Media
    end

    subgraph Platform["Platform Implementations (expect/actual)"]
        AndroidImpl["Android:<br/>SQLite · SharedPreferences · MediaStore"]
        iOSImpl["iOS:<br/>Native SQLite · NSUserDefaults · Photos framework"]
    end

    VM --> UC
    RepoIf -.implemented by.-> RepoImpl
    DB --> AndroidImpl
    DB --> iOSImpl
    Pref --> AndroidImpl
    Pref --> iOSImpl
    Media --> AndroidImpl
    Media --> iOSImpl
```

**Cross-cutting concerns:** Koin for DI (`AppModule` and `ViewModelModule`, both in `shared`),
ExoPlayer for media playback, and Coil for image loading.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:
- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…