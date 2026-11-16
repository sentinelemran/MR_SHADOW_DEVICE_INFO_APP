package com.mrshadow.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import java.util.Locale

data class InfoField(val label: String, val value: String)
data class InfoSection(val title: String, val fields: List<InfoField>) {
    fun report() = fields.joinToString("\n") { "${it.label}: ${it.value}" }
}

class DeviceInfoService(private val context: Context) {
    fun collect(): List<InfoSection> {
        fun value(raw: String?): String = raw?.takeIf(String::isNotBlank) ?: "Not Available"
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memory = manager?.let { ActivityManager.MemoryInfo().also(it::getMemoryInfo) }
        val storage = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val battery = runCatching {
            context.registerReceiver(null, IntentFilterCompat.batteryChanged())
        }.getOrNull()
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val batteryStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> "Not Available"
        }
        val metrics = context.resources.displayMetrics
        return listOf(
            InfoSection("Basic device information", listOf(
                InfoField("Manufacturer", value(Build.MANUFACTURER)),
                InfoField("Brand", value(Build.BRAND)),
                InfoField("Model", value(Build.MODEL)),
                InfoField("Device", value(Build.DEVICE)),
                InfoField("Product", value(Build.PRODUCT)),
                InfoField("Board", value(Build.BOARD)),
                InfoField("Hardware", value(Build.HARDWARE)),
                InfoField("Device type", if (context.resources.configuration.smallestScreenWidthDp >= 600) "Tablet" else "Phone"),
            )),
            InfoSection("Android & operating system", listOf(
                InfoField("Android version", value(Build.VERSION.RELEASE)),
                InfoField("Android SDK", Build.VERSION.SDK_INT.toString()),
                InfoField("Build ID", value(Build.ID)),
                InfoField("Security patch", value(Build.VERSION.SECURITY_PATCH)),
                InfoField("System architecture", value(System.getProperty("os.arch"))),
                InfoField("Root status", "Not Available"),
                InfoField("Bootloader unlock status", "Not Available"),
            )),
            InfoSection("Processor", listOf(
                InfoField("Supported CPU ABI", Build.SUPPORTED_ABIS.joinToString().ifBlank { "Not Available" }),
                InfoField("Available cores", Runtime.getRuntime().availableProcessors().toString()),
                InfoField("Hardware / chipset", value(Build.HARDWARE)),
            )),
            InfoSection("Memory", listOf(
                InfoField("Total RAM", memory?.totalMem?.let(::formatBytes) ?: "Not Available"),
                InfoField("Available RAM", memory?.availMem?.let(::formatBytes) ?: "Not Available"),
            )),
            InfoSection("Storage", listOf(
                InfoField("Internal total", storage?.totalBytes?.let(::formatBytes) ?: "Not Available"),
                InfoField("Internal available", storage?.availableBytes?.let(::formatBytes) ?: "Not Available"),
            )),
            InfoSection("Battery", listOf(
                InfoField("Level", if (level >= 0 && scale > 0) "${level * 100 / scale}%" else "Not Available"),
                InfoField("Status", batteryStatus),
                InfoField("Temperature", battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)?.takeIf { it >= 0 }?.let { String.format(Locale.US, "%.1f °C", it / 10f) } ?: "Not Available"),
            )),
            InfoSection("Display", listOf(
                InfoField("Resolution", "${metrics.widthPixels} × ${metrics.heightPixels} px"),
                InfoField("Density DPI", "${metrics.densityDpi}"),
            )),
            InfoSection("Camera", listOf(InfoField("Camera metadata", "Not Available"))),
        )
    }

    private fun formatBytes(bytes: Long): String = if (bytes > 0) Formatter.formatFileSize(context, bytes) else "Not Available"
}

private object IntentFilterCompat {
    fun batteryChanged() = IntentFilterCompatInternal.create()
}

private object IntentFilterCompatInternal {
    fun create() = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
}

fun List<InfoSection>.asReport() = joinToString("\n\n") { "${it.title}\n${it.report()}" }
