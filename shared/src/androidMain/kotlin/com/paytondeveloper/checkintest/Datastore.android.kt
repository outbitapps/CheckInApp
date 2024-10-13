package com.paytondeveloper.checkintest
import android.content.Context
import android.content.SharedPreferences
import android.health.connect.datatypes.AppInfo
import android.preference.PreferenceManager
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

class PrefSingleton private constructor() {
    public var mContext: Context? = null

    //
    private var mMyPreferences: SharedPreferences? = null

    fun Initialize(ctxt: Context?) {
        mContext = ctxt
        //
        mMyPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
    }

    companion object {
        private var mInstance: PrefSingleton? = null
        val instance: PrefSingleton
            get() {
                if (mInstance == null) mInstance = PrefSingleton()
                return mInstance!!
            }
    }
}

actual fun createSettings(): Settings {
    val delegate = PrefSingleton.instance.mContext!!.getSharedPreferences("keys", Context.MODE_PRIVATE)
    return SharedPreferencesSettings(delegate)
}