package com.mask0fdark.zapret2ui.data

import android.content.SharedPreferences

private const val DISCORD_HOSTS =
    "dis.gd discord.com discord.gg discord.media discord.app discord.co discord.dev discord.design " +
    "discord.gift discord.gifts discord.new discord.store discord.status discordapp.com discordapp.net " +
    "discordcdn.com discordstatus.com discordmerch.com discord-activities.com discordactivities.com " +
    "discordsays.com discordsez.com discordpartygames.com gateway.discord.gg api.discord.com " +
    "cdn.discordapp.com media.discordapp.net images-ext-1.discordapp.net images-ext-2.discordapp.net " +
    "images.discordapp.net stable.dl2.discordapp.net " +
    "discord-attachments-uploads-prd.storage.googleapis.com challenges.cloudflare.com " +
    "cloudflare-ech.com encryptedsni.com"

private const val YOUTUBE_HOSTS =
    "youtube.com youtu.be googlevideo.com ytimg.com youtubei.googleapis.com youtube.googleapis.com"

private const val SAFE_FALLBACK =
    "--auto=torst,ssl_err --timeout 3 --proto=t,h --disorder 1 --tlsrec 1+s"

/*
 * Discord is intentionally stronger than the YouTube profile.
 *
 * The old disorder+tlsrec pair is enough on some providers, but current Discord blocking can also
 * hit the gateway / API / Cloudflare ECH path. This sequence is a ByeDPI-compatible subset of the
 * community Discord strategies: a short-lived fake plus OOB/splits around SNI and reordered data.
 * It is host-scoped, so unrelated HTTPS traffic does not get the aggressive treatment.
 */
private const val DISCORD_TLS =
    "--proto=t,h --hosts \":$DISCORD_HOSTS\" --fake -1 --ttl 5 --oob 1 " +
    "--split 1+s --split 2+s --split 5+s --disorder 3+s " +
    "--split 7+s --split 10+s --split 15+s"

private const val YOUTUBE_TLS =
    "--auto=none --proto=t,h --hosts \":$YOUTUBE_HOSTS\" --disorder 1 --tlsrec 1+s"

private const val DISCORD_VOICE =
    "--auto=none --proto=u --pf 19294-19344 --udp-fake 6 " +
    "--auto=none --proto=u --pf 50000-65535 --udp-fake 6"

enum class StrategyPreset(
    val title: String,
    val description: String,
    val command: String,
) {
    COMBO(
        "Combo (recommended)",
        "Always-on mode: stronger Discord bypass, YouTube, voice traffic and a safe fallback for other blocked HTTPS connections.",
        DISCORD_TLS + " " +
            YOUTUBE_TLS + " " +
            DISCORD_VOICE + " " +
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
        "Stronger Discord TLS bypass plus the common UDP voice ranges.",
        DISCORD_TLS + " " +
            DISCORD_VOICE
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
