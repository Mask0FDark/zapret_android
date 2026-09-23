package com.mask0fdark.zapret2ui.services

import com.mask0fdark.zapret2ui.data.AppStatus
import com.mask0fdark.zapret2ui.data.Mode

var appStatus = AppStatus.Halted to Mode.VPN
    private set

fun setStatus(status: AppStatus, mode: Mode) {
    appStatus = status to mode
}
