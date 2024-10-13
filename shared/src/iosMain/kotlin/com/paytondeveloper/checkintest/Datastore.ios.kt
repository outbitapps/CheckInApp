package com.paytondeveloper.checkintest

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

@OptIn(ExperimentalSettingsImplementation::class)
actual fun createSettings(): Settings {
    return KeychainSettings("com.paytondeveloper.checkin.keychain")
}