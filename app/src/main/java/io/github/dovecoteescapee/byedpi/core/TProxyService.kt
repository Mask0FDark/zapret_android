package com.mask0fdark.zapret2ui.core

object TProxyService {
    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    @JvmStatic
    external fun TProxyStartService(configPath: String, fd: Int)

    @JvmStatic
    external fun TProxyStopService()

    @JvmStatic
    @Suppress("unused")
    external fun TProxyGetStats(): LongArray
}
