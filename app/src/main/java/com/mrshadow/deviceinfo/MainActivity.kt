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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray

private enum class Page { HOME, APPS, DEVELOPER, SETTINGS }
private enum class Language { EN, BN }

private val accentColor = Color(0xFF9BEA45)

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
    val preferences = remember {
        context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

    var page by remember { mutableStateOf(Page.HOME) }
    var darkTheme by remember { mutableStateOf(preferences.getBoolean("dark", true)) }
    var language by remember {
        mutableStateOf(
            if (preferences.getBoolean("bengali", false)) Language.BN else Language.EN
        )
    }
    var showUnavailable by remember {
        mutableStateOf(preferences.getBoolean("show_unavailable", true))
    }
    var notifications by remember {
        mutableStateOf(preferences.getBoolean("notifications", false))
    }
    var refreshCount by remember { mutableStateOf(0) }
    var sections by remember { mutableStateOf(emptyList<InfoSection>()) }

    LaunchedEffect(refreshCount) {
        sections = runCatching {
            DeviceInfoService(context).collect()
        }.getOrDefault(emptyList())
    }

    val colors = if (darkTheme) {
        darkColorScheme(
            primary = accentColor,
            background = Color(0xFF080A08),
            surface = Color(0xFF151815)
        )
    } else {
        lightColorScheme(primary = Color(0xFF426B13))
    }

    MaterialTheme(colorScheme = colors) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "MR SHADOW DEVICE INFO",
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    val destinations = listOf(
                        Triple(Page.HOME, tr(language, "Home", "হোম"), Icons.Default.Home),
                        Triple(Page.APPS, tr(language, "Apps", "অ্যাপস"), Icons.Default.Apps),
                        Triple(Page.DEVELOPER, tr(language, "Developer", "ডেভেলপার"), Icons.Default.Person),
                        Triple(Page.SETTINGS, tr(language, "Settings", "সেটিংস"), Icons.Default.Settings)
                    )

                    destinations.forEach { (destination, label, icon) ->
                        NavigationBarItem(
                            selected = page == destination,
                            onClick = { page = destination },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { padding ->
            when (page) {
                Page.HOME -> HomeScreen(
                    language = language,
                    sections = sections,
                    showUnavailable = showUnavailable,
                    onRefresh = { refreshCount++ },
                    modifier = Modifier.padding(padding)
                )

                Page.APPS -> AppsScreen(
                    language = language,
                    modifier = Modifier.padding(padding)
                )

                Page.DEVELOPER -> DeveloperScreen(
                    language = language,
                    sections = sections,
                    modifier = Modifier.padding(padding)
                )

                Page.SETTINGS -> SettingsScreen(
                    language = language,
                    darkTheme = darkTheme,
                    onDarkThemeChange = {
                        darkTheme = it
                        preferences.edit().putBoolean("dark", it).apply()
                    },
                    onLanguageChange = {
                        language = it
                        preferences.edit()
                            .putBoolean("bengali", it == Language.BN)
                            .apply()
                    },
                    showUnavailable = showUnavailable,
                    onShowUnavailableChange = {
                        showUnavailable = it
                        preferences.edit().putBoolean("show_unavailable", it).apply()
                    },
                    notifications = notifications,
                    onNotificationsChange = {
                        notifications = it
                        preferences.edit().putBoolean("notifications", it).apply()
                    },
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    language: Language,
    sections: List<InfoSection>,
    showUnavailable: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)
                    ?.bufferedWriter()
                    ?.use { it.write(sections.asReport()) }
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                tr(language, "Your device", "আপনার ডিভাইস"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                tr(
                    language,
                    "Device information is read locally using Android APIs.",
                    "Android API ব্যবহার করে ডিভাইসের তথ্য স্থানীয়ভাবে দেখা হয়।"
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Text(tr(language, "Refresh", "রিফ্রেশ"))
                }

                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                        as? ClipboardManager
                    clipboard?.setPrimaryClip(
                        ClipData.newPlainText("Device information", sections.asReport())
                    )
                }) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Text(tr(language, "Copy", "কপি"))
                }

                OutlinedButton(onClick = { exporter.launch("device-info.txt") }) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text("TXT")
                }
            }

            OutlinedButton(
                onClick = {
                    showShareConfirmation(context, sections.asReport(), language)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Text(tr(language, "Share report", "রিপোর্ট শেয়ার"))
            }
        }

        if (sections.isEmpty()) {
            item {
                Text(tr(language, "Loading device information…", "ডিভাইসের তথ্য লোড হচ্ছে…"))
            }
        }

        items(sections) { section ->
            InfoSectionCard(section, showUnavailable)
        }
    }
}

@Composable
private fun InfoSectionCard(
    section: InfoSection,
    showUnavailable: Boolean
) {
    var expanded by remember(section.title) { mutableStateOf(false) }
    val fields = if (showUnavailable) {
        section.fields
    } else {
        section.fields.filterNot { it.value == "Not Available" }
    }

    Card(
        Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = section.title,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                if (fields.isEmpty()) {
                    Text("Not Available")
                }
                fields.forEach { field ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(field.label, Modifier.weight(1f))
                        Text(
                            field.value,
                            Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private data class AppEntry(
    val id: String,
    val name: String,
    val category: String,
    val description: String,
    val url: String
)

@Composable
private fun AppsScreen(language: Language, modifier: Modifier) {
    val context = LocalContext.current
    val apps = remember {
        runCatching {
            val json = JSONArray(
                context.assets.open("apps.json").bufferedReader().use { it.readText() }
            )
            (0 until json.length()).mapNotNull { index ->
                val item = json.optJSONObject(index) ?: return@mapNotNull null
                AppEntry(
                    id = item.optString("id", index.toString()),
                    name = item.optString("name"),
                    category = item.optString("category"),
                    description = item.optString("description"),
                    url = item.optString("downloadUrl")
                        .ifBlank { item.optString("websiteUrl") }
                )
            }
        }.getOrDefault(emptyList())
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                tr(language, "Developer-managed apps", "ডেভেলপার পরিচালিত অ্যাপ"),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        if (apps.isEmpty()) {
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        tr(
                            language,
                            "No apps listed yet. Add genuine app information and HTTPS URLs to apps.json.",
                            "এখনো কোনো অ্যাপ তালিকাভুক্ত নেই। apps.json-এ আসল অ্যাপের তথ্য ও HTTPS URL যোগ করুন।"
                        ),
                        Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(apps, key = { it.id }) { app ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        app.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(app.category)
                    Text(app.description)

                    if (app.url.isNotBlank()) {
                        TextButton(onClick = { openHttpsUrl(context, app.url) }) {
                            Text(tr(language, "Open link", "লিংক খুলুন"))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeveloperScreen(
    language: Language,
    sections: List<InfoSection>,
    modifier: Modifier
) {
    val context = LocalContext.current
    var reportPreview by remember { mutableStateOf<String?>(null) }

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(96.dp)
        ) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(
            "𝙼𝚁  𝚂𝙷𝙰𝙳𝙾𝚆",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        ContactCard(
            name = "WhatsApp",
            url = "https://wa.me/message/AXTGUAHKBLIHL1",
            icon = Icons.Default.Chat
        ) { openHttpsUrl(context, it) }

        ContactCard(
            name = "Telegram",
            url = "https://t.me/mr_shadow23",
            icon = Icons.Default.Send
        ) { openHttpsUrl(context, it) }

        Button(
            onClick = { reportPreview = sections.asReport() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Security, contentDescription = null)
            Text(tr(language, "Review & share report", "রিপোর্ট দেখে শেয়ার করুন"))
        }

        Text(
            tr(
                language,
                "Nothing is sent automatically. Continuing opens Android Sharesheet; no backend or bot is configured.",
                "কিছু স্বয়ংক্রিয়ভাবে পাঠানো হয় না। এগোলে Android Sharesheet খুলবে; backend বা bot নেই।"
            ),
            style = MaterialTheme.typography.bodySmall
        )
    }

    reportPreview?.let { report ->
        AlertDialog(
            onDismissRequest = { reportPreview = null },
            title = { Text(tr(language, "Review report", "রিপোর্ট যাচাই")) },
            text = {
                Column {
                    Text(
                        tr(
                            language,
                            "Review the report before sharing. The app does not claim delivery.",
                            "শেয়ার করার আগে রিপোর্টটি দেখুন। অ্যাপটি পাঠানো হয়েছে বলে দাবি করে না।"
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        report,
                        Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reportPreview = null
                    shareText(context, report, "Device report")
                }) {
                    Text(tr(language, "Continue", "এগিয়ে যান"))
                }
            },
            dismissButton = {
                TextButton(onClick = { reportPreview = null }) {
                    Text(tr(language, "Cancel", "বাতিল"))
                }
            }
        )
    }
}

@Composable
private fun ContactCard(
    name: String,
    url: String,
    icon: ImageVector,
    onOpen: (String) -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable { onOpen(url) }
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(name, Modifier.weight(1f))
            Icon(Icons.Default.OpenInNew, contentDescription = null)
        }
    }
}

@Composable
private fun SettingsScreen(
    language: Language,
    darkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
    onLanguageChange: (Language) -> Unit,
    showUnavailable: Boolean,
    onShowUnavailableChange: (Boolean) -> Unit,
    notifications: Boolean,
    onNotificationsChange: (Boolean) -> Unit,
    modifier: Modifier
) {
    var dialog by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                tr(language, "Settings", "সেটিংস"),
                style = MaterialTheme.typography.headlineSmall
            )
        }

        item {
            ToggleRow(
                title = tr(language, "Dark theme", "ডার্ক থিম"),
                checked = darkTheme,
                onChange = onDarkThemeChange
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { onLanguageChange(Language.EN) }) {
                        Text(if (language == Language.EN) "✓ English" else "English")
                    }
                    TextButton(onClick = { onLanguageChange(Language.BN) }) {
                        Text(if (language == Language.BN) "✓ বাংলা" else "বাংলা")
                    }
                }
            }
        }

        item {
            ToggleRow(
                title = tr(language, "Show unavailable fields", "অনুপলব্ধ তথ্য দেখান"),
                checked = showUnavailable,
                onChange = onShowUnavailableChange
            )
        }

        item {
            ToggleRow(
                title = tr(language, "Notifications preference", "নোটিফিকেশন পছন্দ"),
                checked = notifications,
                onChange = onNotificationsChange
            )
        }

        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    TextButton(onClick = { dialog = "privacy" }) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Text(tr(language, "Privacy", "গোপনীয়তা"))
                    }
                    TextButton(onClick = { dialog = "about" }) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Text(tr(language, "About", "পরিচিতি"))
                    }
                }
            }
        }

        item {
            Text(
                tr(
                    language,
                    "Notifications are a saved preference only; this app does not post notifications.",
                    "নোটিফিকেশন শুধু পছন্দ হিসেবে সংরক্ষিত হয়; অ্যাপ নোটিফিকেশন পাঠায় না।"
                ),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    dialog?.let { kind ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = {
                Text(
                    if (kind == "privacy") {
                        tr(language, "Privacy", "গোপনীয়তা")
                    } else {
                        tr(language, "About", "পরিচিতি")
                    }
                )
            },
            text = {
                Text(
                    if (kind == "privacy") {
                        tr(
                            language,
                            "Device information is read locally. No internet permission, backend or bot is included. This app does not collect IMEI, serial number, phone/SIM data, location, accounts, credentials or personal files.",
                            "ডিভাইসের তথ্য স্থানীয়ভাবে দেখা হয়। ইন্টারনেট permission, backend বা bot নেই। IMEI, serial, phone/SIM, location, account, credential বা ব্যক্তিগত ফাইল সংগ্রহ হয় না।"
                        )
                    } else {
                        "MR SHADOW DEVICE INFO\nVersion 1.0.0\nRooting, unlocking, firmware flashing and APK installation are not features."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { dialog = null }) {
                    Text(tr(language, "Close", "বন্ধ করুন"))
                }
            }
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

private fun openHttpsUrl(context: Context, rawUrl: String) {
    val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return
    if (uri.scheme == "https" && !uri.host.isNullOrBlank()) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        }
    }
}

private fun showShareConfirmation(
    context: Context,
    report: String,
    language: Language
) {
    // Home screen shares through Android Sharesheet after the user taps the button.
    shareText(context, report, tr(language, "Device report", "ডিভাইস রিপোর্ট"))
}

private fun shareText(context: Context, text: String, title: String) {
    val intent = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, text)

    runCatching {
        context.startActivity(Intent.createChooser(intent, title))
    }
}

private fun tr(language: Language, english: String, bengali: String): String =
    if (language == Language.BN) bengali else english
