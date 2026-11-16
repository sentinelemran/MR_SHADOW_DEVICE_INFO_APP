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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray

private enum class Page { HOME, APPS, DEVELOPER, SETTINGS }
private enum class Language { EN, BN }
private val accent = Color(0xFF9BEA45)

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
    var sections by remember { mutableStateOf(emptyList<InfoSection>()) }
    fun refresh() { sections = runCatching { DeviceInfoService(context).collect() }.getOrDefault(emptyList()) }
    LaunchedEffect(Unit) { refresh() }
    val colors = if (dark) darkColorScheme(primary = accent, background = Color(0xFF080A08), surface = Color(0xFF151815)) else lightColorScheme(primary = Color(0xFF426B13))
    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = { CenterAlignedTopAppBar(title = { Text("MR SHADOW DEVICE INFO", fontWeight = FontWeight.Bold) }) },
            bottomBar = {
                NavigationBar {
                    val pages = listOf(
                        Page.HOME to tr(language, "Home", "হোম"),
                        Page.APPS to tr(language, "Apps", "অ্যাপস"),
                        Page.DEVELOPER to tr(language, "Developer", "ডেভেলপার"),
                        Page.SETTINGS to tr(language, "Settings", "সেটিংস"),
                    )
                    pages.forEach { (entry, title) ->
                        val icon = when (entry) {
                            Page.HOME -> Icons.Default.Home
                            Page.APPS -> Icons.Default.Apps
                            Page.DEVELOPER -> Icons.Default.Person
                            Page.SETTINGS -> Icons.Default.Settings
                        }
                        NavigationBarItem(selected = page == entry, onClick = { page = entry }, icon = { Icon(icon, title) }, label = { Text(title) })
                    }
                }
            },
        ) { padding ->
            when (page) {
                Page.HOME -> HomeScreen(language, sections, showUnavailable, ::refresh, Modifier.padding(padding))
                Page.APPS -> AppsScreen(language, Modifier.padding(padding))
                Page.DEVELOPER -> DeveloperScreen(language, sections, Modifier.padding(padding))
                Page.SETTINGS -> SettingsScreen(language, dark, { dark = it; prefs.edit().putBoolean("dark", it).apply() },
                    { language = it; prefs.edit().putBoolean("bengali", it == Language.BN).apply() },
                    showUnavailable, { showUnavailable = it; prefs.edit().putBoolean("show_unavailable", it).apply() },
                    notifications, { notifications = it; prefs.edit().putBoolean("notifications", it).apply() }, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(language: Language, sections: List<InfoSection>, showUnavailable: Boolean, onRefresh: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) runCatching { context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(sections.asReport()) } }
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(tr(language, "Your device", "আপনার ডিভাইস"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(tr(language, "Information is read locally using standard Android APIs.", "স্ট্যান্ডার্ড Android API দিয়ে তথ্য শুধু ডিভাইসেই দেখা হয়।"))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onRefresh) { Icon(Icons.Default.Refresh, null); Text(tr(language, "Refresh", "রিফ্রেশ")) }
                OutlinedButton(onClick = { (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.setPrimaryClip(ClipData.newPlainText("Device information", sections.asReport())) }) { Icon(Icons.Default.ContentCopy, null); Text(tr(language, "Copy", "কপি")) }
                OutlinedButton(onClick = { exporter.launch("device-info.txt") }) { Icon(Icons.Default.Download, null); Text("TXT") }
            }
            OutlinedButton(onClick = { shareText(context, sections.asReport(), "Device report") }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Share, null); Text(tr(language, "Share report", "রিপোর্ট শেয়ার")) }
        }
        items(sections) { InfoCard(it, showUnavailable) }
    }
}

@Composable
private fun InfoCard(section: InfoSection, showUnavailable: Boolean) {
    val fields = if (showUnavailable) section.fields else section.fields.filterNot { it.value == "Not Available" }
    Card(Modifier.fillMaxWidth().animateContentSize()) {
        androidx.compose.material3.ListItem(
            headlineContent = { Text(section.title) },
            supportingContent = { Column { fields.forEach { Text("${it.label}: ${it.value}") } } },
        )
    }
}

@Composable
private fun AppsScreen(language: Language, modifier: Modifier) {
    val context = LocalContext.current
    val apps = remember { runCatching {
        val json = JSONArray(context.assets.open("apps.json").bufferedReader().use { it.readText() })
        (0 until json.length()).mapNotNull { json.optJSONObject(it) }
    }.getOrDefault(emptyList()) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(tr(language, "Developer-managed apps", "ডেভেলপার পরিচালিত অ্যাপ"), style = MaterialTheme.typography.headlineSmall) }
        if (apps.isEmpty()) item { Card(Modifier.fillMaxWidth()) { Text(tr(language, "No apps listed. Add real information and HTTPS URLs to apps.json.", "এখনও অ্যাপ যোগ করা হয়নি। apps.json-এ আসল তথ্য ও HTTPS URL দিন।"), Modifier.padding(16.dp)) } }
        items(apps, key = { it.optString("id", it.optString("name")) }) { app ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
                Text(app.optString("name"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(app.optString("category"))
                Text(app.optString("description"))
                val url = app.optString("downloadUrl").ifBlank { app.optString("websiteUrl") }
                if (url.isNotBlank()) TextButton(onClick = { openHttps(context, url) }) { Text(tr(language, "Open link", "লিংক খুলুন")) }
            } }
        }
    }
}

@Composable
private fun DeveloperScreen(language: Language, sections: List<InfoSection>, modifier: Modifier) {
    val context = LocalContext.current
    var preview by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(100.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp)) } }
        Spacer(Modifier.height(12.dp))
        Text("𝙼𝚁  𝚂𝙷𝙰𝙳𝙾𝚆", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        ContactCard("WhatsApp", "https://wa.me/message/AXTGUAHKBLIHL1", Icons.Default.Chat) { openHttps(context, it) }
        ContactCard("Telegram", "https://t.me/mr_shadow23", Icons.Default.Send) { openHttps(context, it) }
        Button(onClick = { preview = sections.asReport() }) { Icon(Icons.Default.Security, null); Text(tr(language, "Review report", "রিপোর্ট দেখুন")) }
        Text(tr(language, "No data is sent automatically. Continuing opens Android Sharesheet, not a bot/backend.", "কোনো তথ্য নিজে থেকে পাঠানো হয় না। এগোলে Android Sharesheet খুলবে; bot/backend নয়।"))
    }
    preview?.let { report -> AlertDialog(
        onDismissRequest = { preview = null },
        title = { Text(tr(language, "Review device report", "ডিভাইস রিপোর্ট যাচাই")) },
        text = { Column { Text(tr(language, "Review all details before sharing.", "শেয়ার করার আগে সব তথ্য দেখুন।")); Spacer(Modifier.height(8.dp)); Text(report, Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) } },
        confirmButton = { TextButton(onClick = { preview = null; shareText(context, report, "Device report") }) { Text(tr(language, "Continue", "এগিয়ে যান")) } },
        dismissButton = { TextButton(onClick = { preview = null }) { Text(tr(language, "Cancel", "বাতিল")) } },
    ) }
}

@Composable
private fun ContactCard(String name, String url, ImageVector icon, (String) onOpen) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(url) }) { androidx.compose.material3.ListItem(leadingContent = { Icon(icon, null) }, headlineContent = { Text(name) }, trailingContent = { Icon(Icons.Default.OpenInNew, null) }) }
}

@Composable
private fun SettingsScreen(language: Language, dark: Boolean, setDark: (Boolean) -> Unit, setLanguage: (Language) -> Unit, unavailable: Boolean, setUnavailable: (Boolean) -> Unit, notifications: Boolean, setNotifications: (Boolean) -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text(tr(language, "Settings", "সেটিংস"), style = MaterialTheme.typography.headlineSmall) }
        item { SwitchLine("Appearance", tr(language, "Dark theme", "ডার্ক থিম"), dark, setDark) }
        item { Card(Modifier.fillMaxWidth()) { Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = { setLanguage(Language.EN) }) { Text(if (language == Language.EN) "✓ English" else "English") }; TextButton(onClick = { setLanguage(Language.BN) }) { Text(if (language == Language.BN) "✓ বাংলা" else "বাংলা") } } } }
        item { SwitchLine("Device information", tr(language, "Show unavailable fields", "অনুপলব্ধ তথ্য দেখান"), unavailable, setUnavailable) }
        item { SwitchLine("Notifications", tr(language, "Enable notifications", "নোটিফিকেশন চালু করুন"), notifications, setNotifications) }
        item { Card(Modifier.fillMaxWidth()) { Column {
            TextButton(onClick = { showAboutDialog(context, "Privacy", "No internet permission, backend or bot. No IMEI, serial, SIM, phone, location, accounts, credentials or personal files are collected.") }) { Icon(Icons.Default.Security, null); Text("Privacy") }
            TextButton(onClick = { showAboutDialog(context, "About", "MR SHADOW DEVICE INFO • 1.0.0") }) { Icon(Icons.Default.Info, null); Text("About") }
            TextButton(onClick = { shareText(context, "MR SHADOW DEVICE INFO support: https://t.me/mr_shadow23", "Support") }) { Icon(Icons.Default.Share, null); Text("Support") }
        } } }
        item { Text(tr(language, "Notifications are a saved preference only; this app does not post them.", "নোটিফিকেশন শুধু পছন্দ হিসেবে রাখা হয়; অ্যাপ নোটিফিকেশন পাঠায় না।")) }
    }
}

private fun showAboutDialog(context: Context, title: String, message: String) {
    androidx.appcompat.app.AlertDialog.Builder(context).setTitle(title).setMessage(message).setPositiveButton("OK", null).show()
}

@Composable
private fun SwitchLine(group: String, title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(group, color = MaterialTheme.colorScheme.primary); Text(title) }
        Switch(checked = checked, onCheckedChange = onChange)
    } }
}

private fun openHttps(context: Context, raw: String) {
    val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return
    if (uri.scheme == "https" && !uri.host.isNullOrBlank()) runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }
}

private fun shareText(context: Context, text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    runCatching { context.startActivity(Intent.createChooser(send, title)) }
}

private fun tr(language: Language, en: String, bn: String) = if (language == Language.BN) bn else en
