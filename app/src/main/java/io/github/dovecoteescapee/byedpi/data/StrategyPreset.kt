package com.mask0fdark.zapret2ui.data

import android.content.SharedPreferences

enum class StrategyPreset(
    val title: String,
    val description: String,
    val command: String,
) {
    AUTO(
        "Auto",
        "Safe default with fallback after a reset or TLS error.",
        "--disorder 1 --auto=torst --timeout 3 --tlsrec 1+s"
    ),
    UNIVERSAL(
        "Universal",
        "Simple TCP desync for most HTTPS blocks.",
        "--disorder 1"
    ),
    YOUTUBE(
        "YouTube",
        "TCP disorder plus TLS record split.",
        "--disorder 1 --tlsrec 1+s"
    ),
    DISCORD(
        "Discord",
        "TCP desync plus UDP fake packets for voice traffic.",
        "--disorder 1 --udp-fake 6"
    ),
    TELEGRAM(
        "Telegram",
        "TCP desync plus stronger UDP handling for calls.",
        "--disorder 1 --udp-fake 8"
    );

    fun applyTo(preferences: SharedPreferences) {
        preferences.edit()
            .putString("strategy_preset", name)
            .putBoolean("byedpi_enable_cmd_settings", true)
            .putString("byedpi_cmd_args", command)
            .putString("byedpi_proxy_ip", "127.0.0.1")
            .putString("byedpi_proxy_port", "1080")
            .apply()
    }

    companion object {
        fun fromPreferences(preferences: SharedPreferences): StrategyPreset {
            val saved = preferences.getString("strategy_preset", AUTO.name)
            return entries.firstOrNull { it.name == saved } ?: AUTO
        }
    }
}
