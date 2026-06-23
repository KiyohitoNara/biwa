package io.github.kiyohitonara.biwa.di

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.loggerConfigInit
import co.touchlab.kermit.platformLogWriter
import org.koin.dsl.module

/**
 * Koin module that provides the application-wide [Logger].
 *
 * @param isDebug whether this is a debug build. Debug builds log everything from
 *   [Severity.Verbose]; release builds are limited to [Severity.Warn] and above.
 */
fun loggingModule(isDebug: Boolean) =
    module {
        single {
            Logger(
                config =
                    loggerConfigInit(
                        platformLogWriter(),
                        minSeverity = if (isDebug) Severity.Verbose else Severity.Warn,
                    ),
                tag = "Biwa",
            )
        }
    }
