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

data class InfoSection(
    val title: String,
    val fields: List<InfoField>
)

class DeviceInfoService(private val context: Context) {
    private val unavailable = "Not Available"

    fun collect(): List<InfoSection> {
        fun value(raw: String?): String =
            raw?.takeIf { it.isNotBlank() } ?: unavailable

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager

        val memory = activityManager?.let { manager ->
            ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
        }

        val storage = runCatching {
            StatFs(Environment.getDataDirectory().path)
        }.getOrNull()

        val battery = runCatching {
            context.registerReceiver(
                null,
                android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
        }.getOrNull()

        val batteryLevel = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryStatus = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val metrics = context.resources.displayMetrics

        fun bytes(count: Long?): String =
            if (count != null && count > 0) {
                Formatter.formatFileSize(context, count)
            } else {
                unavailable
            }

        val statusText = when (batteryStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> unavailable
        }

        val temperature = battery
            ?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
            ?.takeIf { it >= 0 }
            ?.let { String.format(Locale.US, "%.1f °C", it / 10f) }
            ?: unavailable

        return listOf(
            InfoSection(
                "Basic device information",
                listOf(
                    InfoField("Manufacturer", value(Build.MANUFACTURER)),
                    InfoField("Brand", value(Build.BRAND)),
                    InfoField("Model", value(Build.MODEL)),
                    InfoField("Device", value(Build.DEVICE)),
                    InfoField("Product", value(Build.PRODUCT)),
                    InfoField("Board", value(Build.BOARD)),
                    InfoField("Hardware", value(Build.HARDWARE)),
                    InfoField(
                        "Device type",
                        if (context.resources.configuration.smallestScreenWidthDp >= 600) {
                            "Tablet"
                        } else {
                            "Phone"
                        }
                    )
                )
            ),
            InfoSection(
                "Android & operating system",
                listOf(
                    InfoField("Android version", value(Build.VERSION.RELEASE)),
                    InfoField("Android SDK", Build.VERSION.SDK_INT.toString()),
                    InfoField("Build ID", value(Build.ID)),
                    InfoField("Security patch", value(Build.VERSION.SECURITY_PATCH)),
                    InfoField("System architecture", value(System.getProperty("os.arch"))),
                    InfoField("Root status", unavailable),
                    InfoField("Bootloader unlock status", unavailable)
                )
            ),
            InfoSection(
                "Processor",
                listOf(
                    InfoField(
                        "Supported CPU ABI",
                        Build.SUPPORTED_ABIS.joinToString().ifBlank { unavailable }
                    ),
                    InfoField(
                        "Available processor cores",
                        Runtime.getRuntime().availableProcessors().toString()
                    ),
                    InfoField("Hardware / chipset", value(Build.HARDWARE))
                )
            ),
            InfoSection(
                "Memory",
                listOf(
                    InfoField("Total RAM", bytes(memory?.totalMem)),
                    InfoField("Available RAM", bytes(memory?.availMem))
                )
            ),
            InfoSection(
                "Storage",
                listOf(
                    InfoField("Internal storage total", bytes(storage?.totalBytes)),
                    InfoField("Internal storage available", bytes(storage?.availableBytes))
                )
            ),
            InfoSection(
                "Battery",
                listOf(
                    InfoField(
                        "Battery level",
                        if (batteryLevel >= 0 && batteryScale > 0) {
                            "${batteryLevel * 100 / batteryScale}%"
                        } else {
                            unavailable
                        }
                    ),
                    InfoField("Charging status", statusText),
                    InfoField("Temperature", temperature),
                    InfoField(
                        "Technology",
                        value(battery?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY))
                    )
                )
            ),
            InfoSection(
                "Display",
                listOf(
                    InfoField(
                        "Resolution",
                        "${metrics.widthPixels} × ${metrics.heightPixels} px"
                    ),
                    InfoField("Density DPI", "${metrics.densityDpi} dpi")
                )
            ),
            InfoSection(
                "Camera",
                listOf(InfoField("Camera metadata", unavailable))
            )
        )
    }
}

fun List<InfoSection>.asReport(): String =
    joinToString("\n\n") { section ->
        "${section.title}\n" +
            section.fields.joinToString("\n") { field ->
                "${field.label}: ${field.value}"
            }
    }
