package com.junkfood.seal.util

import android.content.Context
import android.os.Build
import android.util.Log
import com.junkfood.seal.BuildConfig
import java.io.File

object JsRuntimeUtil {
    private const val TAG = "JsRuntimeUtil"

    data class RuntimeDiagnostics(
        val isJsRuntimeAvailable: Boolean,
        val jsRuntimePath: String?,
        val runtimeName: String,
        val primaryAbi: String,
        val supportedAbis: List<String>,
        val isEjsSupported: Boolean,
    )

    fun getDiagnostics(context: Context): RuntimeDiagnostics {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val qjsFile = File(nativeDir, "libqjs.so")
        val isQjsPresent = qjsFile.exists()

        // Also check if any external Deno or other JS engine is present in native dir or internal dir
        val denoFile = File(context.filesDir, "deno")
        val isDenoPresent = denoFile.exists()

        val isAvailable = isQjsPresent || isDenoPresent
        val runtimeName = when {
            isQjsPresent -> "QuickJS (native libqjs.so)"
            isDenoPresent -> "Deno"
            else -> "None"
        }

        val runtimePath = when {
            isQjsPresent -> qjsFile.absolutePath
            isDenoPresent -> denoFile.absolutePath
            else -> null
        }

        val primaryAbi = Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
        val supportedAbis = Build.SUPPORTED_ABIS.toList()

        return RuntimeDiagnostics(
            isJsRuntimeAvailable = isAvailable,
            jsRuntimePath = runtimePath,
            runtimeName = runtimeName,
            primaryAbi = primaryAbi,
            supportedAbis = supportedAbis,
            isEjsSupported = isAvailable,
        )
    }

    fun logDiagnostics(context: Context) {
        if (!BuildConfig.DEBUG) return

        val diag = getDiagnostics(context)
        Log.d(TAG, "=== JS Runtime & yt-dlp Diagnostics ===")
        Log.d(TAG, "JS Runtime Available: ${diag.isJsRuntimeAvailable}")
        Log.d(TAG, "JS Runtime Engine: ${diag.runtimeName}")
        Log.d(TAG, "JS Runtime Path: ${diag.jsRuntimePath ?: "Not Found"}")
        Log.d(TAG, "Primary Device ABI: ${diag.primaryAbi}")
        Log.d(TAG, "All Supported ABIs: ${diag.supportedAbis.joinToString()}")
        Log.d(TAG, "YouTube EJS Challenge Solver Supported: ${diag.isEjsSupported}")
        Log.d(TAG, "yt-dlp Cached Version: ${PreferenceUtil.getString(YT_DLP_VERSION, "unknown")}")
        Log.d(TAG, "========================================")
    }

    /**
     * Sanitizes command line arguments to strip/mask cookies, authorization headers,
     * passwords, and other sensitive tokens before logging to Logcat.
     */
    fun sanitizeCommand(args: List<String>): List<String> {
        val sanitized = mutableListOf<String>()
        var skipNext = false

        for (i in args.indices) {
            if (skipNext) {
                skipNext = false
                continue
            }

            val arg = args[i]

            when {
                arg == "--cookies" || arg == "--cookies-from-browser" -> {
                    sanitized.add(arg)
                    if (i + 1 < args.size) {
                        sanitized.add("[REDACTED_COOKIE_PATH]")
                        skipNext = true
                    }
                }

                arg == "--password" || arg == "-p" || arg == "--video-password" -> {
                    sanitized.add(arg)
                    if (i + 1 < args.size) {
                        sanitized.add("[REDACTED_PASSWORD]")
                        skipNext = true
                    }
                }

                arg.startsWith("--add-header") -> {
                    sanitized.add(arg)
                    if (i + 1 < args.size) {
                        val header = args[i + 1]
                        if (header.contains("cookie", ignoreCase = true) ||
                            header.contains("authorization", ignoreCase = true) ||
                            header.contains("token", ignoreCase = true)
                        ) {
                            val headerKey = header.substringBefore(":")
                            sanitized.add("$headerKey:[REDACTED]")
                        } else {
                            sanitized.add(header)
                        }
                        skipNext = true
                    }
                }

                arg.startsWith("Cookie:", ignoreCase = true) || arg.startsWith("Authorization:", ignoreCase = true) -> {
                    val headerKey = arg.substringBefore(":")
                    sanitized.add("$headerKey:[REDACTED]")
                }

                else -> {
                    sanitized.add(arg)
                }
            }
        }

        return sanitized
    }

    fun logCommand(args: List<String>) {
        if (!BuildConfig.DEBUG) return
        val sanitized = sanitizeCommand(args)
        Log.d(TAG, "yt-dlp execution command: ${sanitized.joinToString(" ")}")
    }
}
