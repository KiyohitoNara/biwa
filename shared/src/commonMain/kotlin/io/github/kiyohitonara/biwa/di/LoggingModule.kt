package io.github.kiyohitonara.biwa.di

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import org.koin.dsl.module

/**
 * Koin module that provides the application-wide [Logger].
 *
 * @param isDebug whether this is a debug build. Debug builds log everything from
 *   [Severity.Verbose]; release builds are limited to [Severity.Warn] and above.
 * @param logWriter the platform [LogWriter] to emit logs to (e.g. Logcat on Android,
 *   OSLog on iOS).
 */
fun loggingModule(
    isDebug: Boolean,
    logWriter: LogWriter,
) = module {
    single {
        Logger(
            config =
                loggerConfigInit(
                    logWriter,
                    minSeverity = if (isDebug) Severity.Verbose else Severity.Warn,
                ),
            tag = "Biwa",
        )
    }
}
