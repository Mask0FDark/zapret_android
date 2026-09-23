package com.mask0fdark.zapret2ui.data

import android.content.SharedPreferences

private const val DISCORD_HOSTS =
    "discord.com discord.gg discordapp.com discordapp.net discordcdn.com discord.media discordstatus.com"
private const val YOUTUBE_HOSTS =
    "youtube.com youtu.be googlevideo.com ytimg.com youtubei.googleapis.com youtube.googleapis.com"
private const val TELEGRAM_IPS =
    "91.108.0.0/16 149.154.160.0/20 91.105.192.0/23 185.76.151.0/24"

private const val SAFE_FALLBACK =
    "--auto=torst,ssl_err --timeout 3 --proto=t,h --disorder 1 --tlsrec 1+s"

private const val DISCORD_VOICE =
    "--auto=none --proto=u --pf 19294-19344 --udp-fake 6 " +
    "--auto=none --proto=u --pf 50000-65535 --udp-fake 6"

private const val TELEGRAM_DIRECT =
    "--auto=none --ipset \":$TELEGRAM_IPS\" --disorder 1 --udp-fake 8"

enum class StrategyPreset(
    val title: String,
    val description: String,
    val command: String,
) {
    COMBO(
        "Combo (recommended)",
        "Always-on mode: Discord, YouTube and voice traffic, with a safe fallback for other blocked HTTPS connections.",
        "--proto=t,h --hosts \":$DISCORD_HOSTS $YOUTUBE_HOSTS\" --disorder 1 --tlsrec 1+s " +
            DISCORD_VOICE + " " +
            TELEGRAM_DIRECT + " " +
            SAFE_FALLBACK
    ),
    AUTO(
        "Auto",
        "Touches normal HTTPS only after a reset, timeout or broken TLS handshake is detected.",
        SAFE_FALLBACK
    ),
    UNIVERSAL(
        "Universal",
        "Simple TCP desync for most HTTPS blocks. More aggressive than Auto.",
        "--proto=t,h --disorder 1 --tlsrec 1+s"
    ),
    YOUTUBE(
        "YouTube",
        "Targets YouTube and Google video domains without touching unrelated HTTPS traffic.",
        "--proto=t,h --hosts \":$YOUTUBE_HOSTS\" --disorder 1 --tlsrec 1+s"
    ),
    DISCORD(
        "Discord",
        "Targets Discord TLS plus the common UDP voice ranges.",
        "--proto=t,h --hosts \":$DISCORD_HOSTS\" --disorder 1 --tlsrec 1+s " +
            DISCORD_VOICE
    ),
    TELEGRAM(
        "Telegram",
        "Attempts desync for known Telegram IPv4 ranges, including UDP. Full IP blocking may still require a relay.",
        "--ipset \":$TELEGRAM_IPS\" --disorder 1 --udp-fake 8"
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
            val saved = preferences.getString("strategy_preset", COMBO.name)
            return entries.firstOrNull { it.name == saved } ?: COMBO
        }
    }
}
