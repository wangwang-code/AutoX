package com.stardust.autojs.runtime.api

import android.Manifest
import com.stardust.util.ScreenMetrics
import android.os.Build
import android.annotation.SuppressLint
import com.stardust.pio.PFiles
import android.telephony.TelephonyManager
import android.provider.Settings.SettingNotFoundException
import android.media.AudioManager
import android.content.IntentFilter
import android.os.BatteryManager
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.os.Vibrator
import ezy.assist.compat.SettingsCompat
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.net.wifi.WifiInfo
import android.provider.Settings
import android.view.WindowManager
import android.util.DisplayMetrics
import androidx.annotation.RequiresApi
import com.stardust.autojs.R
import com.stardust.autojs.runtime.exception.ScriptException
import com.stardust.pio.UncheckedIOException
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.net.NetworkInterface
import java.net.SocketException
import java.util.*

/**
 * Created by Stardust on 2017/12/2.
 */
class Device(private val mContext: Context) {
    companion object {
        val width = ScreenMetrics.getDeviceScreenWidth()
        val height = ScreenMetrics.getDeviceScreenHeight()
        val buildId = Build.ID
        val buildDisplay = Build.DISPLAY
        val product = Build.PRODUCT
        val board = Build.BOARD
        val brand = Build.BRAND
        val device = Build.DEVICE
        val model = Build.MODEL
        val bootloader = Build.BOOTLOADER
        val hardware = Build.HARDWARE
        val fingerprint = Build.FINGERPRINT
        val sdkInt = Build.VERSION.SDK_INT
        val incremental = Build.VERSION.INCREMENTAL
        val release = Build.VERSION.RELEASE
        var baseOS: String? = null
        var securityPatch: String? = null
        val codename = Build.VERSION.CODENAME

        @SuppressLint("HardwareIds")
        val serial = Build.SERIAL
        private const val FAKE_MAC_ADDRESS = "02:00:00:00:00:00"

        @get:Throws(SocketException::class)
        private val macByInterface: String?
            private get() {
                val networkInterfaces: List<NetworkInterface> =
                    Collections.list(NetworkInterface.getNetworkInterfaces())
                for (networkInterface in networkInterfaces) {
                    if (networkInterface.name.equals("wlan0", ignoreCase = true)) {
                        val macBytes = networkInterface.hardwareAddress ?: return null
                        val mac = StringBuilder()
                        for (b in macBytes) {
                            mac.append(String.format("%02X:", b))
                        }
                        if (mac.length > 0) {
                            mac.deleteCharAt(mac.length - 1)
                        }
                        return mac.toString()
                    }
                }
                return null
            }

        @get:Throws(Exception::class)
        private val macByFile: String?
            private get() = try {
                PFiles.read("/sys/class/net/wlan0/address")
            } catch (e: UncheckedIOException) {
                null
            }

        init {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                baseOS = Build.VERSION.BASE_OS
                securityPatch = Build.VERSION.SECURITY_PATCH
            } else {
                baseOS = null
                securityPatch = null
            }
        }
    }

    private var mWakeLock: PowerManager.WakeLock? = null
    private val mWakeLockFlag = 0

    @get:SuppressLint("HardwareIds")
    val iMEI: String?
        get() {
            checkReadPhoneStatePermission()
            return try {
                (mContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).deviceId
            } catch (e: SecurityException) {
                null
            }
        }

    @get:SuppressLint("HardwareIds")
    val androidId: String
        get() = Settings.Secure.getString(mContext.contentResolver, Settings.Secure.ANDROID_ID)

    @get:Throws(SettingNotFoundException::class)
    @set:Throws(SettingNotFoundException::class)
    var brightness: Int
        get() = Settings.System.getInt(mContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        set(b) {
            checkWriteSettingsPermission()
            Settings.System.putInt(mContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, b)
        }

    @get:Throws(SettingNotFoundException::class)
    @set:Throws(SettingNotFoundException::class)
    var brightnessMode: Int
        get() = Settings.System.getInt(
            mContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE
        )
        set(b) {
            checkWriteSettingsPermission()
            Settings.System.putInt(
                mContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                b
            )
        }
    var musicVolume: Int
        get() = (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
            .getStreamVolume(AudioManager.STREAM_MUSIC)
        set(i) {
            checkWriteSettingsPermission()
            (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
                .setStreamVolume(AudioManager.STREAM_MUSIC, i, 0)
        }
    var notificationVolume: Int
        get() = (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
            .getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        set(i) {
            checkWriteSettingsPermission()
            (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
                .setStreamVolume(AudioManager.STREAM_NOTIFICATION, i, 0)
        }
    var alarmVolume: Int
        get() = (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
            .getStreamVolume(AudioManager.STREAM_ALARM)
        set(i) {
            checkWriteSettingsPermission()
            (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
                .setStreamVolume(AudioManager.STREAM_ALARM, i, 0)
        }
    val musicMaxVolume: Int
        get() = (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
            .getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val notificationMaxVolume: Int
        get() = (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
            .getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
    val alarmMaxVolume: Int
        get() = (getSystemService<Any>(Context.AUDIO_SERVICE) as AudioManager)
            .getStreamMaxVolume(AudioManager.STREAM_ALARM)
    val battery: Float
        get() {
            val batteryIntent =
                mContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?: return (-1).toFloat()
            val level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val battery = level.toFloat() / scale * 100.0f
            return (Math.round(battery * 10) / 10).toFloat()
        }
    val totalMem: Long
        get() {
            val activityManager = getSystemService<ActivityManager>(Context.ACTIVITY_SERVICE)
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            return info.totalMem
        }
    val availMem: Long
        get() {
            val activityManager = getSystemService<ActivityManager>(Context.ACTIVITY_SERVICE)
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            return info.availMem
        }
    val isCharging: Boolean
        get() {
            val intent =
                mContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    ?: throw ScriptException("Cannot retrieve the battery state")
            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            return plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB
        }

    fun keepAwake(flags: Int, timeout: Long) {
        checkWakeLock(flags)
        mWakeLock!!.acquire(timeout)
    }

    @SuppressLint("WakelockTimeout")
    fun keepAwake(flags: Int) {
        checkWakeLock(flags)
        mWakeLock!!.acquire()
    }

    //按照API文档来说不应该使用PowerManager.isScreenOn()，但是，isScreenOn()和实际不一致的情况通常只会出现在安卓智能手表的类似设备上
    //因此这里仍然使用PowerManager.isScreenOn()
    // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
    //   return ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getState() == Display.STATE_ON;
    //} else {
    //}
    val isScreenOn: Boolean
        get() =//按照API文档来说不应该使用PowerManager.isScreenOn()，但是，isScreenOn()和实际不一致的情况通常只会出现在安卓智能手表的类似设备上
        //因此这里仍然使用PowerManager.isScreenOn()
        // if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
        //   return ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getState() == Display.STATE_ON;
            //} else {
            (getSystemService<Any>(Context.POWER_SERVICE) as PowerManager).isScreenOn

    //}
    fun wakeUpIfNeeded() {
        if (!isScreenOn) {
            wakeUp()
        }
    }

    fun wakeUp() {
        keepScreenOn(200)
    }

    fun keepScreenOn() {
        keepAwake(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP)
    }

    fun keepScreenOn(timeout: Long) {
        keepAwake(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            timeout
        )
    }

    fun keepScreenDim() {
        keepAwake(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP)
    }

    fun keepScreenDim(timeout: Long) {
        keepAwake(PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, timeout)
    }

    private fun checkWakeLock(flags: Int) {
        if (mWakeLock == null || flags != mWakeLockFlag) {
            cancelKeepingAwake()
            mWakeLock = (getSystemService<Any>(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                flags,
                Device::class.java.name
            )
        }
    }

    fun cancelKeepingAwake() {
        if (mWakeLock != null && mWakeLock!!.isHeld) mWakeLock!!.release()
    }

    fun vibrate(millis: Long) {
        (getSystemService<Any>(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(millis)
    }

    fun cancelVibration() {
        (getSystemService<Any>(Context.VIBRATOR_SERVICE) as Vibrator).cancel()
    }

    private fun checkWriteSettingsPermission() {
        if (SettingsCompat.canWriteSettings(mContext)) {
            return
        }
        SettingsCompat.manageWriteSettings(mContext)
        throw SecurityException(mContext.getString(R.string.no_write_settings_permissin))
    }

    private fun checkReadPhoneStatePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (mContext.checkSelfPermission(Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                throw SecurityException(mContext.getString(R.string.no_read_phone_state_permissin))
            }
        }
    }

    // just to avoid warning of null pointer to make android studio happy..
    private fun <T> getSystemService(service: String): T {
        val systemService = mContext.getSystemService(service)
            ?: throw RuntimeException("should never happen...$service")
        return systemService as T
    }

    @get:Throws(Exception::class)
    @get:SuppressLint("HardwareIds")
    val macAddress: String?
        get() {
            val wifiMan =
                mContext.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    ?: return null
            val wifiInf = wifiMan.connectionInfo
                ?: return macByFile
            var mac = wifiInf.macAddress
            if (FAKE_MAC_ADDRESS == mac) {
                mac = null
            }
            if (mac == null) {
                mac = macByInterface
                if (mac == null) {
                    mac = macByFile
                }
            }
            return mac
        }

    /**
     * 获取是否存在NavigationBar
     * @return
     */
    fun checkDeviceHasNavigationBar(): Boolean {
        var hasNavigationBar = false
        val rs = mContext.resources
        val id = rs.getIdentifier("config_showNavigationBar", "bool", "android")
        if (id > 0) {
            hasNavigationBar = rs.getBoolean(id)
        }
        try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val m = systemPropertiesClass.getMethod("get", String::class.java)
            val navBarOverride = m.invoke(systemPropertiesClass, "qemu.hw.mainkeys") as String
            if ("1" == navBarOverride) {
                hasNavigationBar = false
            } else if ("0" == navBarOverride) {
                hasNavigationBar = true
            }
        } catch (e: Exception) {
        }
        return hasNavigationBar
    }

    /**
     * 获取虚拟功能键高度
     * @return
     */
    val virtualBarHeigh: Int
        get() {
            var vh = 0
            val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val dm = DisplayMetrics()
            try {
                val c = Class.forName("android.view.Display")
                val method = c.getMethod("getRealMetrics", DisplayMetrics::class.java)
                method.invoke(display, dm)
                vh = dm.heightPixels - windowManager.defaultDisplay.height
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return vh
        }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isIgnoringBatteryOptimizations(): Boolean {
        return (mContext.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
            mContext.packageName
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun isIgnoringBatteryOptimizations(pkgName: String?): Boolean {
        return (mContext.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
            pkgName
        )
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestIgnoreBatteryOptimizations() {
        try {
            val intent0 = Intent()
            val isIgnoring =
                (mContext.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                    mContext.packageName
                )
            if (isIgnoring) {
                intent0.action =
                    android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            } else {
                intent0.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).data =
                    Uri.parse("package:" + mContext.packageName)
            }
            mContext.startActivity(intent0)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun requestIgnoreBatteryOptimizations(pkgName: String?) {
        try {
            val intent0 = Intent()
            val isIgnoring =
                (mContext.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
                    pkgName
                )
            if (isIgnoring) {
                intent0.action =
                    android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
            } else {
                intent0.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).data =
                    Uri.parse("package:$pkgName")
            }
            mContext.startActivity(intent0)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun toString(): String {
        return "Device{" +
                "width=" + width +
                ", height=" + height +
                ", buildId='" + buildId + '\'' +
                ", buildDisplay='" + buildDisplay + '\'' +
                ", product='" + product + '\'' +
                ", board='" + board + '\'' +
                ", brand='" + brand + '\'' +
                ", device='" + device + '\'' +
                ", model='" + model + '\'' +
                ", bootloader='" + bootloader + '\'' +
                ", hardware='" + hardware + '\'' +
                ", fingerprint='" + fingerprint + '\'' +
                ", sdkInt=" + sdkInt +
                ", incremental='" + incremental + '\'' +
                ", release='" + release + '\'' +
                ", baseOS='" + baseOS + '\'' +
                ", securityPatch='" + securityPatch + '\'' +
                ", serial='" + serial + '\'' +
                '}'
    }
}