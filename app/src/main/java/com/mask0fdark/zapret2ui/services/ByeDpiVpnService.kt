package com.mask0fdark.zapret2ui.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.mask0fdark.zapret2ui.R
import com.mask0fdark.zapret2ui.activities.MainActivity
import com.mask0fdark.zapret2ui.core.ByeDpiProxy
import com.mask0fdark.zapret2ui.core.ByeDpiProxyPreferences
import com.mask0fdark.zapret2ui.core.TProxyService
import com.mask0fdark.zapret2ui.data.*
import com.mask0fdark.zapret2ui.utility.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class ByeDpiVpnService : LifecycleVpnService() {
    private val byeDpiProxy = ByeDpiProxy()
    private var proxyJob: Job? = null
    private var tunFd: ParcelFileDescriptor? = null
    private var telegramProxyManager: TelegramProxyManager? = null
    private val mutex = Mutex()
    private var starting: Boolean = false
    private var stopping: Boolean = false
    private var tun2SocksStarted: Boolean = false

    companion object {
        private val TAG: String = ByeDpiVpnService::class.java.simpleName
        private const val FOREGROUND_SERVICE_ID: Int = 1
        private const val NOTIFICATION_CHANNEL_ID: String = "ByeDPIVpn"
        const val PREF_LAST_ERROR: String = "vpn_last_error"

        private var status: ServiceStatus = ServiceStatus.Disconnected
    }

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.vpn_channel_name,
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return when (val action = intent?.action) {
            START_ACTION -> {
                startForeground()
                lifecycleScope.launch { start() }
                START_STICKY
            }

            STOP_ACTION -> {
                lifecycleScope.launch { stop() }
                START_NOT_STICKY
            }

            else -> {
                Log.w(TAG, "Unknown action: $action")
                START_NOT_STICKY
            }
        }
    }

    override fun onRevoke() {
        Log.i(TAG, "VPN revoked")
        lifecycleScope.launch { stop() }
    }

    private suspend fun start() {
        Log.i(TAG, "Starting")
        getPreferences().edit().putString(PREF_LAST_ERROR, "").apply()

        mutex.withLock {
            if (status == ServiceStatus.Connected || starting || stopping) {
                Log.w(TAG, "Ignoring duplicate VPN start: status=$status starting=$starting stopping=$stopping")
                return@withLock
            }

            starting = true
            try {
                startProxy()
                startTelegramProxyIfEnabled()
                startTun2Socks()
                updateStatus(ServiceStatus.Connected)
            } catch (t: Throwable) {
                val message = "${t.javaClass.simpleName}: ${t.message ?: "unknown error"}"
                Log.e(TAG, "Failed to start VPN: $message", t)
                getPreferences().edit().putString(PREF_LAST_ERROR, message).apply()
                updateStatus(ServiceStatus.Failed)
                stopLocked()
            } finally {
                starting = false
            }
        }
    }

    private fun startForeground() {
        val notification: Notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_SERVICE_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }

    private suspend fun stop() {
        Log.i(TAG, "Stopping")
        mutex.withLock {
            stopLocked()
        }
    }

    private suspend fun stopLocked() {
        if (stopping) {
            Log.w(TAG, "VPN stop already in progress")
            return
        }

        stopping = true
        try {
            stopTun2Socks()
            stopTelegramProxy()
            stopProxy()
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to stop VPN cleanly", t)
        } finally {
            stopping = false
            updateStatus(ServiceStatus.Disconnected)
            stopSelf()
        }
    }

    private suspend fun startProxy() {
        Log.i(TAG, "Starting proxy")

        if (proxyJob != null) {
            Log.w(TAG, "Proxy fields not null")
            throw IllegalStateException("Proxy fields not null")
        }

        val preferences = getByeDpiPreferences()

        proxyJob = lifecycleScope.launch(Dispatchers.IO) {
            val code = byeDpiProxy.startProxy(preferences)

            withContext(Dispatchers.Main) {
                if (!stopping) {
                    if (code != 0) {
                        val message = "ByeDPI engine exited with code $code"
                        Log.e(TAG, message)
                        getPreferences().edit().putString(PREF_LAST_ERROR, message).apply()
                        updateStatus(ServiceStatus.Failed)
                    } else {
                        Log.w(TAG, "ByeDPI engine stopped unexpectedly")
                    }

                    // Schedule cleanup from a different coroutine. Calling stop() from proxyJob
                    // itself would make stopProxy() wait for the current job and deadlock.
                    lifecycleScope.launch { stop() }
                }
            }
        }

        Log.i(TAG, "Proxy started")
    }

    private suspend fun stopProxy() {
        Log.i(TAG, "Stopping proxy")

        val job = proxyJob
        if (job == null) {
            Log.w(TAG, "Proxy already stopped")
            return
        }

        try {
            byeDpiProxy.stopProxy()
        } catch (e: IllegalStateException) {
            Log.w(TAG, "ByeDPI proxy socket was already closed", e)
        }

        job.join()
        proxyJob = null
        Log.i(TAG, "Proxy stopped")
    }

    private suspend fun startTelegramProxyIfEnabled() {
        val preferences = getPreferences()
        val enabled = preferences.getBoolean(TelegramProxyManager.PREF_ENABLED, false)
        if (!enabled) {
            return
        }

        val upstreamPort = preferences.getString("byedpi_proxy_port", null)?.toIntOrNull() ?: 1080
        val manager = TelegramProxyManager(this)
        telegramProxyManager = manager

        val started = manager.start(upstreamPort)
        preferences.edit()
            .putString(
                TelegramProxyManager.PREF_LAST_ERROR,
                if (started) "" else "Could not start Telegram MTProto/WS proxy"
            )
            .apply()

        if (!started) {
            manager.stop()
            telegramProxyManager = null
        }
    }

    private fun stopTelegramProxy() {
        telegramProxyManager?.stop()
        telegramProxyManager = null
    }

    private fun startTun2Socks() {
        Log.i(TAG, "Starting tun2socks")

        if (tunFd != null) {
            throw IllegalStateException("VPN field not null")
        }

        val sharedPreferences = getPreferences()
        val port = sharedPreferences.getString("byedpi_proxy_port", null)?.toInt() ?: 1080
        val dns = sharedPreferences.getStringNotNull("dns_ip", "1.1.1.1")
        val ipv6 = sharedPreferences.getBoolean("ipv6_enable", false)

        val tun2socksConfig = """
        | misc:
        |   task-stack-size: 81920
        | socks5:
        |   mtu: 8500
        |   address: 127.0.0.1
        |   port: $port
        |   udp: udp
        """.trimMargin("| ")

        val configPath = try {
            File.createTempFile("config", "tmp", cacheDir).apply {
                writeText(tun2socksConfig)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create config file", e)
            throw e
        }

        val fd = createBuilder(dns, ipv6).establish()
            ?: throw IllegalStateException("VPN connection failed")

        this.tunFd = fd

        try {
            TProxyService.TProxyStartService(configPath.absolutePath, fd.fd)
            tun2SocksStarted = true
            Log.i(TAG, "Tun2Socks started")
        } catch (t: Throwable) {
            tunFd?.close()
            tunFd = null
            tun2SocksStarted = false
            throw t
        }
    }

    private fun stopTun2Socks() {
        Log.i(TAG, "Stopping tun2socks")

        if (tun2SocksStarted) {
            try {
                TProxyService.TProxyStopService()
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to stop tun2socks", t)
            } finally {
                tun2SocksStarted = false
            }
        }

        try {
            cacheDir.listFiles { file -> file.name.startsWith("config") && file.name.endsWith("tmp") }
                ?.forEach { it.delete() }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to delete temporary tun2socks config", e)
        }

        try {
            tunFd?.close()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to close VPN file descriptor", t)
        } finally {
            tunFd = null
        }

        Log.i(TAG, "Tun2socks stopped")
    }

    private fun getByeDpiPreferences(): ByeDpiProxyPreferences =
        ByeDpiProxyPreferences.fromSharedPreferences(getPreferences())

    private fun updateStatus(newStatus: ServiceStatus) {
        Log.d(TAG, "VPN status changed from $status to $newStatus")

        status = newStatus

        setStatus(
            when (newStatus) {
                ServiceStatus.Connected -> AppStatus.Running

                ServiceStatus.Disconnected,
                ServiceStatus.Failed -> AppStatus.Halted
            },
            Mode.VPN
        )

        val intent = Intent(
            when (newStatus) {
                ServiceStatus.Connected -> STARTED_BROADCAST
                ServiceStatus.Disconnected -> STOPPED_BROADCAST
                ServiceStatus.Failed -> FAILED_BROADCAST
            }
        )
        intent.putExtra(SENDER, Sender.VPN.ordinal)
        sendBroadcast(intent)
    }

    private fun createNotification(): Notification =
        createConnectionNotification(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_title,
            R.string.vpn_notification_content,
            ByeDpiVpnService::class.java,
        )

    private fun createBuilder(dns: String, ipv6: Boolean): Builder {
        Log.d(TAG, "DNS: $dns")
        val builder = Builder()
        builder.setSession("ByeDPI")
        builder.setConfigureIntent(
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        )

        builder.addAddress("10.10.10.10", 32)
            .addRoute("0.0.0.0", 0)

        if (ipv6) {
            builder.addAddress("fd00::1", 128)
                .addRoute("::", 0)
        }

        if (dns.isNotBlank()) {
            builder.addDnsServer(dns)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        builder.addDisallowedApplication(applicationContext.packageName)

        return builder
    }
}
