package com.mask0fdark.zapret2ui.data

const val STARTED_BROADCAST = "com.mask0fdark.zapret2ui.STARTED"
const val STOPPED_BROADCAST = "com.mask0fdark.zapret2ui.STOPPED"
const val FAILED_BROADCAST = "com.mask0fdark.zapret2ui.FAILED"

const val SENDER = "sender"

enum class Sender(val senderName: String) {
    Proxy("Proxy"),
    VPN("VPN")
}
