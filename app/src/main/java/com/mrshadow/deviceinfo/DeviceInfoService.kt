package com.mrshadow.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
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

    private fun value(raw: String?): String = raw?.takeIf { it.isNotBlank() } ?: unavailable

    fun collect(): List<InfoSection> {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memory = activityManager?.let { manager ->
            ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
        }
        val storage = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val battery = runCatching {
            context.registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val batteryLevel = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val batteryScale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryStatus = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val display = context.resources.displayMetrics

        fun bytes(count: Long?): String =
            if (count != null && count > 0) Formatter.formatFileSize(context, count) else unavailable

        val batteryStatusText = when (batteryStatus) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
            else -> unavailable
        }
        val batteryTemperature = battery
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
                    InfoField("Hardware version", value(Build.HARDWARE)),
                    InfoField("Device type", if (context.resources.configuration.smallestScreenWidthDp >= 600) "Tablet" else "Phone")
                )
            ),
            InfoSection(
                "Android & operating system",
                listOf(
                    InfoField("Android version", value(Build.VERSION.RELEASE)),
                    InfoField("Android SDK", Build.VERSION.SDK_INT.toString()),
                    InfoField("Version number", value(Build.DISPLAY)),
                    InfoField("Build incremental", value(Build.VERSION.INCREMENTAL)),
                    InfoField("Build ID", value(Build.ID)),
                    InfoField("Security patch", value(Build.VERSION.SECURITY_PATCH)),
                    InfoField("Kernel version", value(System.getProperty("os.version"))),
                    InfoField("System architecture", value(System.getProperty("os.arch"))),
                    InfoField("Root status", unavailable),
                    InfoField("Bootloader unlock status", unavailable)
                )
            ),
            InfoSection(
                "Radio & processor",
                listOf(
                    InfoField("Baseband version", value(Build.getRadioVersion())),
                    InfoField("Supported CPU ABI", Build.SUPPORTED_ABIS.joinToString().ifBlank { unavailable }),
                    InfoField("Available processor cores", Runtime.getRuntime().availableProcessors().toString()),
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
                    InfoField("Battery level", if (batteryLevel >= 0 && batteryScale > 0) "${batteryLevel * 100 / batteryScale}%" else unavailable),
                    InfoField("Charging status", batteryStatusText),
                    InfoField("Temperature", batteryTemperature),
                    InfoField("Technology", value(battery?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)))
                )
            ),
            InfoSection(
                "Display",
                listOf(
                    InfoField("Resolution", "${display.widthPixels} × ${display.heightPixels} px"),
                    InfoField("Density DPI", "${display.densityDpi} dpi")
                )
            ),
            collectCameraInfo()
        )
    }

    /** Camera characteristics are metadata-only; reading them does not open a camera or require CAMERA permission. */
    private fun collectCameraInfo(): InfoSection {
        val cameras = runCatching {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return@runCatching emptyList()
            manager.cameraIdList.mapNotNull { id ->
                runCatching {
                    val characteristics = manager.getCameraCharacteristics(id)
                    val facing = when (characteristics.get(CameraCharacteristics.LENS_FACING)) {
                        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
                        CameraCharacteristics.LENS_FACING_BACK -> "Back"
                        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
                        else -> "Unknown"
                    }
                    val orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                    val hardwareLevel = when (characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
                        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "External"
                        else -> "Unknown"
                    }
                    "Camera $id: $facing, orientation ${orientation ?: unavailable}°, $hardwareLevel"
                }.getOrNull()
            }
        }.getOrDefault(emptyList())

        val fields = mutableListOf(InfoField("Camera count", cameras.size.toString()))
        if (cameras.isEmpty()) {
            fields += InfoField("Camera details", unavailable)
        } else {
            cameras.forEachIndexed { index, details ->
                fields += InfoField("Camera ${index + 1}", details)
            }
        }
        return InfoSection("Camera", fields)
    }
}

fun List<InfoSection>.asReport(): String =
    joinToString("\n\n") { section ->
        "${section.title}\n" + section.fields.joinToString("\n") { "${it.label}: ${it.value}" }
    }
