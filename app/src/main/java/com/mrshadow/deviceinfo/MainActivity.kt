package com.mrshadow.deviceinfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.json.JSONArray

private enum class Page { HOME, APPS, DEVELOPER, SETTINGS }
private enum class Language { EN, BN }
private val accentColor = Color(0xFF9BEA45)
private const val developerPhoto = "https://raw.githubusercontent.com/sentinelemran/Virtual-Mic/main/developer.jpg"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ShadowApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShadowApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf(Page.HOME) }
    var dark by remember { mutableStateOf(prefs.getBoolean("dark", true)) }
    var language by remember { mutableStateOf(if (prefs.getBoolean("bengali", false)) Language.BN else Language.EN) }
    var showUnavailable by remember { mutableStateOf(prefs.getBoolean("show_unavailable", true)) }
    var notifications by remember { mutableStateOf(prefs.getBoolean("notifications", false)) }
    var refreshCount by remember { mutableStateOf(0) }
    var sections by remember { mutableStateOf(emptyList<InfoSection>()) }

    LaunchedEffect(refreshCount) {
        sections = runCatching { DeviceInfoService(context).collect() }.getOrDefault(emptyList())
    }

    val colors = if (dark) darkColorScheme(primary = accentColor, background = Color(0xFF080A08), surface = Color(0xFF151815))
    else lightColorScheme(primary = Color(0xFF426B13))

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("MR SHADOW DEVICE INFO", fontWeight = FontWeight.Bold) }) },
            bottomBar = {
                NavigationBar {
                    listOf(
                        Triple(Page.HOME, tr(language, "Home", "হোম"), Icons.Default.Home),
                        Triple(Page.APPS, tr(language, "Apps", "অ্যাপস"), Icons.Default.Apps),
                        Triple(Page.DEVELOPER, tr(language, "Developer", "ডেভেলপার"), Icons.Default.Person),
                        Triple(Page.SETTINGS, tr(language, "Settings", "সেটিংস"), Icons.Default.Settings)
                    ).forEach { (destination, title, icon) ->
                        NavigationBarItem(
                            selected = page == destination,
                            onClick = { page = destination },
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title) }
                        )
                    }
                }
            }
        ) { padding ->
            when (page) {
                Page.HOME -> HomeScreen(language, sections, showUnavailable, { refreshCount++ }, Modifier.padding(padding))
                Page.APPS -> AppsScreen(language, Modifier.padding(padding))
                Page.DEVELOPER -> DeveloperScreen(language, sections, Modifier.padding(padding))
                Page.SETTINGS -> SettingsScreen(
                    language, dark,
                    { dark = it; prefs.edit().putBoolean("dark", it).apply() },
                    { language = it; prefs.edit().putBoolean("bengali", it == Language.BN).apply() },
                    showUnavailable,
                    { showUnavailable = it; prefs.edit().putBoolean("show_unavailable", it).apply() },
                    notifications,
                    { notifications = it; prefs.edit().putBoolean("notifications", it).apply() },
                    Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(language: Language, sections: List<InfoSection>, showUnavailable: Boolean, onRefresh: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    var reportPreview by remember { mutableStateOf<String?>(null) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(sections.asReport()) }
        }
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(tr(language, "Your device", "আপনার ডিভাইস"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(tr(language, "Device details are read locally.", "ডিভাইসের তথ্য স্থানীয়ভাবে দেখা হয়।"))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Text(tr(language, "Refresh", "রিফ্রেশ")) }
                OutlinedButton(onClick = {
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                        ?.setPrimaryClip(ClipData.newPlainText("Device information", sections.asReport()))
                }) { Icon(Icons.Default.ContentCopy, null); Text(tr(language, "Copy", "কপি")) }
                OutlinedButton(onClick = { exporter.launch("device-info.txt") }) { Icon(Icons.Default.Download, null); Text("TXT") }
            }
            OutlinedButton(onClick = { reportPreview = sections.asReport() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Share, null); Text(tr(language, "Review & share report", "রিপোর্ট দেখে শেয়ার করুন"))
            }
        }
        items(sections) { section -> HomeInfoCard(section, showUnavailable) }
    }

    reportPreview?.let { report ->
        AlertDialog(
            onDismissRequest = { reportPreview = null },
            title = { Text(tr(language, "Review report", "রিপোর্ট যাচাই")) },
            text = { Text(report, Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { reportPreview = null; shareText(context, report, "Device report") }) { Text(tr(language, "Continue", "এগিয়ে যান")) } },
            dismissButton = { TextButton(onClick = { reportPreview = null }) { Text(tr(language, "Cancel", "বাতিল")) } }
        )
    }
}

@Composable
private fun HomeInfoCard(section: InfoSection, showUnavailable: Boolean) {
    var expanded by remember(section.title) { mutableStateOf(false) }
    val fields = if (showUnavailable) section.fields else section.fields.filterNot { it.value == "Not Available" }
    Card(Modifier.fillMaxWidth().animateContentSize().clickable { expanded = !expanded }) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(section.title, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (fields.isEmpty()) Text("Not Available")
                fields.forEach { field ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(field.label, Modifier.weight(1f))
                        Text(field.value, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private data class AppEntry(val id: String, val name: String, val category: String, val description: String, val url: String)

@Composable
private fun AppsScreen(language: Language, modifier: Modifier) {
    val context = LocalContext.current
    val apps = remember {
        runCatching {
            val json = JSONArray(context.assets.open("apps.json").bufferedReader().use { it.readText() })
            (0 until json.length()).mapNotNull { index ->
                val item = json.optJSONObject(index) ?: return@mapNotNull null
                AppEntry(item.optString("id", index.toString()), item.optString("name"), item.optString("category"), item.optString("description"), item.optString("websiteUrl"))
            }
        }.getOrDefault(emptyList())
    }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(tr(language, "Developer apps", "ডেভেলপারের অ্যাপ"), style = MaterialTheme.typography.headlineSmall) }
        items(apps, key = { it.id }) { app ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Apps, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(app.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(app.category, style = MaterialTheme.typography.labelMedium)
                        Text(app.description)
                        TextButton(onClick = { openHttps(context, app.url) }) {
                            Text(tr(language, "View app", "অ্যাপ দেখুন")); Icon(Icons.Default.OpenInNew, null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperScreen(language: Language, sections: List<InfoSection>, modifier: Modifier) {
    val context = LocalContext.current
    var reportPreview by remember { mutableStateOf<String?>(null) }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            AsyncImage(
                model = developerPhoto,
                contentDescription = "MR SHADOW developer photo",
                modifier = Modifier.size(132.dp).clip(CircleShape),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text("𝙼𝚁  𝚂𝙷𝙰𝙳𝙾𝚆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(tr(language, "Android app developer", "Android অ্যাপ ডেভেলপার"))
        }
        item { ContactCard("WhatsApp", "https://wa.me/message/AXTGUAHKBLIHL1", Icons.Default.Chat) { openHttps(context, it) } }
        item { ContactCard("Telegram", "https://t.me/mr_shadow23", Icons.Default.Send) { openHttps(context, it) } }
        item {
            Button(onClick = { reportPreview = sections.asReport() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Security, null); Text(tr(language, "Review & share device report", "ডিভাইস রিপোর্ট দেখে শেয়ার করুন"))
            }
            Text(tr(language, "Sharing is user initiated; nothing is sent automatically.", "শেয়ার ব্যবহারকারী নিজে শুরু করেন; কিছু স্বয়ংক্রিয়ভাবে পাঠানো হয় না।"))
        }
    }

    reportPreview?.let { report ->
        AlertDialog(
            onDismissRequest = { reportPreview = null },
            title = { Text(tr(language, "Review report", "রিপোর্ট যাচাই")) },
            text = { Text(report, Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { reportPreview = null; shareText(context, report, "Device report") }) { Text(tr(language, "Continue", "এগিয়ে যান")) } },
            dismissButton = { TextButton(onClick = { reportPreview = null }) { Text(tr(language, "Cancel", "বাতিল")) } }
        )
    }
}

@Composable
private fun ContactCard(name: String, url: String, icon: ImageVector, onOpen: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(url) }) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(name, Modifier.weight(1f))
            Icon(Icons.Default.OpenInNew, null)
        }
    }
}

@Composable
private fun SettingsScreen(
    language: Language,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    onLanguageChange: (Language) -> Unit,
    showUnavailable: Boolean,
    onShowUnavailableChange: (Boolean) -> Unit,
    notifications: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    var dialog by remember { mutableStateOf<String?>(null) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(tr(language, "Settings", "সেটিংস"), style = MaterialTheme.typography.headlineSmall) }
        item { ToggleRow(tr(language, "Dark theme", "ডার্ক থিম"), dark, onDarkChange) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { onLanguageChange(Language.EN) }) { Text(if (language == Language.EN) "✓ English" else "English") }
                    TextButton(onClick = { onLanguageChange(Language.BN) }) { Text(if (language == Language.BN) "✓ বাংলা" else "বাংলা") }
                }
            }
        }
        item { ToggleRow(tr(language, "Show unavailable fields", "অনুপলব্ধ তথ্য দেখান"), showUnavailable, onShowUnavailableChange) }
        item { ToggleRow(tr(language, "Notifications preference", "নোটিফিকেশন পছন্দ"), notifications, onNotificationsChange) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    TextButton(onClick = { dialog = "privacy" }) { Icon(Icons.Default.Security, null); Text(tr(language, "Privacy", "গোপনীয়তা")) }
                    TextButton(onClick = { dialog = "about" }) { Icon(Icons.Default.Info, null); Text(tr(language, "About", "পরিচিতি")) }
                }
            }
        }
        item { Text(tr(language, "Notifications are a saved preference only; no notifications are posted.", "নোটিফিকেশন শুধু পছন্দ হিসেবে থাকে; কোনো নোটিফিকেশন পাঠানো হয় না।")) }
    }

    dialog?.let { kind ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(if (kind == "privacy") tr(language, "Privacy", "গোপনীয়তা") else tr(language, "About", "পরিচিতি")) },
            text = {
                Text(if (kind == "privacy") tr(
                    language,
                    "Device information stays on this device unless you choose to share it. The developer picture is loaded from the public Virtual-Mic GitHub repository. No IMEI, serial, SIM, phone, location, accounts, credentials or personal files are collected.",
                    "আপনি শেয়ার না করলে ডিভাইসের তথ্য এই ডিভাইসেই থাকে। ডেভেলপারের ছবি public Virtual-Mic GitHub repository থেকে লোড হয়। IMEI, serial, SIM, phone, location, account, credential বা ব্যক্তিগত ফাইল সংগ্রহ করা হয় না।"
                ) else "MR SHADOW DEVICE INFO • Version 1.0.0")
            },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text(tr(language, "Close", "বন্ধ করুন")) } }
        )
    }
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(title, Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

private fun openHttps(context: Context, raw: String) {
    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return
    if (uri.scheme == "https" && !uri.host.isNullOrBlank()) runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}

private fun shareText(context: Context, text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(Intent.createChooser(intent, title)) }
}

private fun tr(language: Language, english: String, bengali: String): String =
    if (language == Language.BN) bengali else english
