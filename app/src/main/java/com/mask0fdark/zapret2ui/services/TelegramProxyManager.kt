package com.mask0fdark.zapret2ui.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.mask0fdark.zapret2ui.utility.getPreferences
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom

/**
 * Local Telegram MTProto -> WebSocket proxy.
 *
 * Native engine: amurcanov/tg-ws-proxy-android (GPLv3), based on Flowseal/tg-ws-proxy.
 * It is deliberately independent from the DPI SOCKS chain: Telegram connects to 127.0.0.1:1443,
 * while the proxy reaches Telegram DCs through WSS/Cloudflare fronts.
 */
class TelegramProxyManager(private val context: Context) {
    companion object {
        const val LISTEN_HOST = "127.0.0.1"
        const val LISTEN_PORT = 1443

        const val PREF_ENABLED = "telegram_ws_proxy_enabled"
        const val PREF_TOS_ACCEPTED = "telegram_ws_proxy_ack"
        const val PREF_LAST_ERROR = "telegram_ws_proxy_last_error"
        private const val PREF_SECRET = "telegram_ws_proxy_secret"

        private const val TAG = "TelegramWsProxy"

        fun proxyUri(context: Context): Uri {
            val prefs = context.getPreferences()
            val secret = ensureSecret(prefs.getString(PREF_SECRET, null)).also {
                prefs.edit().putString(PREF_SECRET, it).apply()
            }
            return Uri.parse(
                "tg://proxy?server=$LISTEN_HOST&port=$LISTEN_PORT&secret=dd$secret"
            )
        }

        private fun ensureSecret(current: String?): String {
            val clean = current.orEmpty().trim().lowercase()
            if (clean.length == 32 && clean.all { it in '0'..'9' || it in 'a'..'f' }) {
                return clean
            }
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }

    private interface ProxyLibrary : Library {
        fun StartProxy(host: String, port: Int, dcIps: String, secret: String, verbose: Int): Int
        fun StopProxy(): Int
        fun SetPoolSize(size: Int)
        fun SetCfProxyCacheDir(cacheDir: String)
        fun SetCfProxyConfig(enabled: Int, priority: Int, userDomain: String)
        fun GetSecretWithPrefix(): Pointer?
        fun FreeString(pointer: Pointer)
    }

    private val lib: ProxyLibrary by lazy {
        Native.load("tgwsproxy", ProxyLibrary::class.java) as ProxyLibrary
    }

    suspend fun start(@Suppress("UNUSED_PARAMETER") upstreamPort: Int): Boolean =
        withContext(Dispatchers.IO) {
            if (canConnect(LISTEN_HOST, LISTEN_PORT)) {
                return@withContext true
            }

            val prefs = context.getPreferences()
            val secret = ensureSecret(prefs.getString(PREF_SECRET, null))
            prefs.edit().putString(PREF_SECRET, secret).apply()

            try {
                lib.SetPoolSize(4)
                lib.SetCfProxyCacheDir(context.cacheDir.absolutePath)
                lib.SetCfProxyConfig(1, 1, "")

                val result = lib.StartProxy(
                    LISTEN_HOST,
                    LISTEN_PORT,
                    "",
                    secret,
                    0,
                )
                if (result != 0) {
                    val error = "Telegram WS proxy returned code $result"
                    Log.e(TAG, error)
                    prefs.edit().putString(PREF_LAST_ERROR, error).apply()
                    return@withContext false
                }

                if (!waitForPort(8_000)) {
                    val error = "Telegram WS proxy did not open $LISTEN_HOST:$LISTEN_PORT"
                    Log.e(TAG, error)
                    prefs.edit().putString(PREF_LAST_ERROR, error).apply()
                    try { lib.StopProxy() } catch (_: Throwable) {}
                    return@withContext false
                }

                prefs.edit().putString(PREF_LAST_ERROR, "").apply()
                Log.i(TAG, "Telegram MTProto/WS proxy ready on $LISTEN_HOST:$LISTEN_PORT")
                true
            } catch (t: Throwable) {
                val error = "${t.javaClass.simpleName}: ${t.message ?: "proxy start failed"}"
                Log.e(TAG, error, t)
                prefs.edit().putString(PREF_LAST_ERROR, error).apply()
                false
            }
        }

    fun stop() {
        try {
            lib.StopProxy()
        } catch (t: Throwable) {
            Log.w(TAG, "Telegram WS proxy stop failed", t)
        }
    }

    private suspend fun waitForPort(timeoutMs: Long): Boolean {
        val started = System.currentTimeMillis()
        while (System.currentTimeMillis() - started < timeoutMs) {
            if (canConnect(LISTEN_HOST, LISTEN_PORT)) return true
            delay(200)
        }
        return false
    }

    private fun canConnect(host: String, port: Int): Boolean =
        try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 300)
            }
            true
        } catch (_: Exception) {
            false
        }
}
