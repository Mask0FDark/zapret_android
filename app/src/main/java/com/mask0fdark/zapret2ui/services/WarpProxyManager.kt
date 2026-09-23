package com.mask0fdark.zapret2ui.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale

class WarpProxyManager(private val context: Context) {
    companion object {
        const val LISTEN_HOST = "127.0.0.1"
        const val LISTEN_PORT = 1081
        const val PREF_ENABLED = "telegram_call_relay_enabled"
        const val PREF_TOS_ACCEPTED = "telegram_call_relay_tos_accepted"
        const val PREF_LAST_ERROR = "telegram_call_relay_last_error"

        private const val TAG = "WarpProxyManager"
    }

    private var process: Process? = null

    suspend fun start(upstreamPort: Int): Boolean = withContext(Dispatchers.IO) {
        if (isRunning(process)) {
            return@withContext true
        }

        if (!waitForPort("127.0.0.1", upstreamPort, 12_000)) {
            Log.e(TAG, "ByeDPI SOCKS is not listening on $upstreamPort")
            return@withContext false
        }

        val binary = File(context.applicationInfo.nativeLibraryDir, "libusque.so")
        if (!binary.exists()) {
            Log.e(TAG, "Bundled usque binary is missing: ${binary.absolutePath}")
            return@withContext false
        }

        val configDir = File(context.filesDir, "warp").apply { mkdirs() }
        val configFile = File(configDir, "config.json")
        val upstream = "socks5://127.0.0.1:$upstreamPort"

        if (!configFile.exists()) {
            val register = ProcessBuilder(
                binary.absolutePath,
                "-c",
                configFile.absolutePath,
                "register",
                "--accept-tos",
                "--model",
                "Android",
                "--locale",
                Locale.getDefault().toLanguageTag(),
                "--name",
                "Zapret Android",
            )
                .directory(configDir)
                .redirectErrorStream(true)
                .apply {
                    environment()["HTTPS_PROXY"] = upstream
                    environment()["HTTP_PROXY"] = upstream
                }
                .start()

            collectOutput(register, "warp-register")
            val exitCode = register.waitFor()
            if (exitCode != 0 || !configFile.exists()) {
                Log.e(TAG, "WARP registration failed with code $exitCode")
                return@withContext false
            }
        }

        process = ProcessBuilder(
            binary.absolutePath,
            "-c",
            configFile.absolutePath,
            "socks",
            "--http2",
            "--always-reconnect",
            "--bind",
            LISTEN_HOST,
            "--port",
            LISTEN_PORT.toString(),
            "--connect-port",
            "443",
        )
            .directory(configDir)
            .redirectErrorStream(true)
            .apply {
                environment()["HTTPS_PROXY"] = upstream
                environment()["HTTP_PROXY"] = upstream
            }
            .start()
            .also { collectOutput(it, "warp-socks") }

        if (!waitForPort(LISTEN_HOST, LISTEN_PORT, 15_000)) {
            Log.e(TAG, "WARP SOCKS did not open $LISTEN_HOST:$LISTEN_PORT")
            stop()
            return@withContext false
        }

        Log.i(TAG, "WARP SOCKS is listening on $LISTEN_HOST:$LISTEN_PORT")
        true
    }

    fun stop() {
        process?.destroy()
        process = null
    }

    private fun collectOutput(process: Process, prefix: String) {
        Thread {
            try {
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { Log.i(TAG, "[$prefix] $it") }
                }
            } catch (e: Exception) {
                Log.d(TAG, "[$prefix] output closed: ${e.message}")
            }
        }.apply {
            name = "zapret-$prefix-log"
            isDaemon = true
            start()
        }
    }

    private suspend fun waitForPort(host: String, port: Int, timeoutMs: Long): Boolean {
        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() - started < timeoutMs) {
            if (canConnect(host, port)) {
                return true
            }
            delay(250)
        }
        return false
    }

    private fun canConnect(host: String, port: Int): Boolean =
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 250)
            }
            true
        } catch (_: Exception) {
            false
        }

    private fun isRunning(process: Process?): Boolean {
        process ?: return false
        return try {
            process.exitValue()
            false
        } catch (_: IllegalThreadStateException) {
            true
        }
    }
}
