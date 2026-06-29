package com.sileo.island

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sileo.island.service.SileoNotificationListenerService
import com.sileo.island.ui.AppColors
import com.sileo.island.ui.SettingsScreen
import com.sileo.island.ui.appColors

private const val TEST_CHANNEL = "sileo_test"

private const val REPO_URL = "https://github.com/bikash1376/sileo-android"
private const val ISSUES_URL = "https://github.com/bikash1376/sileo-android/issues/new"
private const val SILEO_WEB_URL = "https://sileo.aaryan.design/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Drop the splash window background before drawing the real UI.
        setTheme(R.style.Theme_SileoIsland)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createTestChannel(this)
        SileoSettings.load(this)
        setContent { App() }
    }

    // Tell the overlay to step aside while our own UI is on screen, so the island
    // isn't rendered twice (in-app host + overlay host both observe Sileo.toasts).
    override fun onStart() {
        super.onStart()
        Sileo.appInForeground = true
    }

    override fun onStop() {
        super.onStop()
        Sileo.appInForeground = false
    }
}

private enum class Tab { HOME, SETTINGS, HELP }

@Composable
private fun App() {
    var tab by remember { mutableStateOf(Tab.HOME) }
    // The app picker is a sub-screen of Home (push/back), not one of the tabs.
    var showApps by remember { mutableStateOf(false) }
    val c = appColors()

    Box(Modifier.fillMaxSize()) {
        // Inter is the app-UI typeface (provided here so every screen's Text inherits
        // it). The island/notification is intentionally left out — it keeps its own.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalTextStyle provides
                androidx.compose.material3.LocalTextStyle.current.copy(
                    fontFamily = com.sileo.island.ui.InterFamily,
                ),
        ) {
            if (showApps) {
                AppPickerScreen(onBack = { showApps = false })
            } else {
                Column(Modifier.fillMaxSize().background(c.pageBg)) {
                    Box(Modifier.weight(1f)) {
                        when (tab) {
                            Tab.HOME -> OnboardingScreen(onChooseApps = { showApps = true })
                            Tab.SETTINGS -> SettingsScreen()
                            Tab.HELP -> HelpScreen()
                        }
                    }
                    BottomBar(current = tab, c = c, onSelect = { tab = it })
                }
            }
        }
        // In-app preview host: the chips render here directly (hardware-accelerated
        // Activity window), so the gooey look can be tested without the system overlay
        // or any "display over other apps" / MIUI background-popup permission. Real
        // notifications still render in the service overlay over other apps.
        com.sileo.island.ui.SileoHost(Modifier.fillMaxSize())
    }
}

@Composable
private fun BottomBar(current: Tab, c: AppColors, onSelect: (Tab) -> Unit) {
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.track))
        Row(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem("⌂", "Home", current == Tab.HOME, c) { onSelect(Tab.HOME) }
            CenterTabItem(current == Tab.SETTINGS, c) { onSelect(Tab.SETTINGS) }
            TabItem("?", "Help", current == Tab.HELP, c) { onSelect(Tab.HELP) }
        }
    }
}

@Composable
private fun TabItem(glyph: String, label: String, selected: Boolean, c: AppColors, onClick: () -> Unit) {
    val color = if (selected) c.accent else c.textSecondary
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(glyph, color = color, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

/** The center tab (Settings) is visually raised — a filled gear in a circle. */
@Composable
private fun CenterTabItem(selected: Boolean, c: AppColors, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(if (selected) c.accent else c.track),
            contentAlignment = Alignment.Center,
        ) {
            // U+2699 + VS15 forces the monochrome (text) gear, not a colored emoji.
            Text("⚙︎", color = if (selected) Color.White else c.textPrimary, fontSize = 22.sp)
        }
        Spacer(Modifier.height(2.dp))
        Text("Settings", color = if (selected) c.accent else c.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingScreen(onChooseApps: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val c = appColors()

    var listenerOn by remember { mutableStateOf(isListenerEnabled(context)) }
    var overlayOn by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var appCount by remember { mutableStateOf(AppPrefs.enabled(context).size) }

    // Re-check permissions/selection every time we return to this screen.
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                listenerOn = isListenerEnabled(context)
                overlayOn = Settings.canDrawOverlays(context)
                appCount = AppPrefs.enabled(context).size
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val notifPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* result ignored; user can retry */ }

    Box(
        Modifier
            .fillMaxSize()
            .background(c.pageBg),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 32.dp),
        ) {
            Text("Sileo · Android", color = c.textPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Turn your notifications into the island.",
                color = c.textSecondary, fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
            )

            PermissionRow(
                step = "1",
                title = "Notification access",
                desc = "Lets Sileo read incoming notifications.",
                granted = listenerOn,
                onClick = {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                },
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow(
                step = "2",
                title = "Display over other apps",
                desc = "Lets Sileo draw the island on top of everything.",
                granted = overlayOn,
                onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}"),
                        ),
                    )
                },
            )

            Spacer(Modifier.height(12.dp))
            ChooseAppsRow(appCount = appCount, onClick = onChooseApps)

            Spacer(Modifier.height(28.dp))
            val ready = listenerOn && overlayOn && appCount > 0
            Text(
                if (ready) "Ready ✓  Notifications from your $appCount app(s) show as islands."
                else "Grant both permissions and pick at least one app.",
                color = if (ready) c.successText else c.textSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )

            Spacer(Modifier.height(16.dp))
            FilledButton("Send a real test notification") {
                if (Build.VERSION.SDK_INT >= 33 &&
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context, android.Manifest.permission.POST_NOTIFICATIONS,
                    ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    notifPermLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    postTestNotification(context)
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("Quick look (renders here in-app):", color = c.textSecondary, fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Chip("Success") { Sileo.success("Changes Saved", "Changes saved successfully to the database. Please refresh the page to see the changes.") }
                Chip("Error") { Sileo.error("Something Went Wrong", "We're having trouble saving your changes to the server. Please try again in a few minutes.") }
                Chip("Warning") { Sileo.warning("Storage Almost Full", "You've used 95% of your storage. Consider upgrading your plan.") }
                Chip("Info") { Sileo.info("New Update Available", "Version 0.2.0 is ready to install with performance improvements.") }
                Chip("Action") { Sileo.action("Invitation Sent", "Aaryan invited you to collaborate on Sileo.", actionLabel = "Accept") }
                Chip("Promise") { Sileo.promise("Saving changes…", "Syncing your edits to the cloud.") }
            }

            Spacer(Modifier.height(36.dp))
            Text("Open source", color = c.textPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Sileo is fully open — read every line, build it yourself, see that it sends nothing.",
                color = c.textSecondary, fontSize = 13.5.sp, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(12.dp))
            LinkText("View the code on GitHub  ↗") { openUrl(context, REPO_URL) }
            Spacer(Modifier.height(8.dp))
            LinkText("Got a bug or feedback? Raise it here  ↗") { openUrl(context, ISSUES_URL) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The Help "?" tab: privacy/security concerns first, then how to set it up. */
@Composable
private fun HelpScreen() {
    val context = LocalContext.current
    val c = appColors()
    Box(
        Modifier
            .fillMaxSize()
            .background(c.pageBg),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 32.dp),
        ) {
            Text("Help & FAQ", color = c.textPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Everything about Sileo — what it is, how to set it up, privacy, and how to turn it off.",
                color = c.textBody, fontSize = 14.sp, lineHeight = 20.sp,
            )
            Spacer(Modifier.height(20.dp))

            // One section open at a time (accordion). Default: the intro.
            var open by remember { mutableStateOf("what") }
            fun toggle(k: String) { open = if (open == k) "" else k }

            FaqItem("What is Sileo Island?", open == "what", { toggle("what") }) {
                Text(
                    "Sileo turns notifications from the apps you pick into a floating \"dynamic island\" at " +
                        "the top of your screen — a gooey pill that expands to show the notification, then " +
                        "tucks away. Every other app's notifications stay exactly as Android shows them.\n\n" +
                        "It's a native re-creation of Sileo, the physics-based notification toast for the web " +
                        "by Aaryan, rebuilt in Kotlin + Jetpack Compose as a real notification island.",
                    color = c.textBody, fontSize = 13.5.sp, lineHeight = 20.sp,
                )
            }
            Spacer(Modifier.height(10.dp))

            FaqItem("How to set it up", open == "setup", { toggle("setup") }) {
                SetupStep("1", "Allow notification access", "Enable Sileo Island in the notification-access list so it can read incoming notifications.")
                SetupStep("2", "Allow display over other apps", "Lets Sileo draw the island on top of whatever you're using.")
                SetupStep("3", "Choose your apps", "Pick which apps show as the island. Everything else stays the normal Android notification.")
                Spacer(Modifier.height(4.dp))
                Text("On Xiaomi / POCO / Redmi (MIUI / HyperOS)", color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "MIUI needs a couple more toggles. In Settings → Apps → Sileo Island, also turn on " +
                        "\"Display pop-up windows while running in the background\" and \"Autostart\", and set " +
                        "battery to No restrictions. Then toggle Notification access off and back on.",
                    color = c.textBody, fontSize = 13.5.sp, lineHeight = 19.sp,
                )
                Spacer(Modifier.height(12.dp))
                Text("Using it", color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap the gooey body to open the app it came from. Tap the pill to collapse it. Swipe up to dismiss.",
                    color = c.textBody, fontSize = 13.5.sp, lineHeight = 19.sp,
                )
            }
            Spacer(Modifier.height(10.dp))

            FaqItem("What about privacy?", open == "privacy", { toggle("privacy") }) {
                PrivacyPoint("Nothing leaves your phone", "Sileo has no internet permission at all — no servers, no analytics, no tracking. Your notifications are never uploaded or shared. They physically can't be.")
                PrivacyPoint("Nothing is stored", "Sileo keeps no history of your notifications. The only thing saved on-device is the short list of apps you picked.")
                PrivacyPoint("Your content is never logged", "Release builds never write notification titles or text to the device log, so OTPs, messages and banking alerts stay private.")
                PrivacyPoint("It can't see your taps", "The island only redraws a notification you already received. It can't read what you type, your passwords, or what's on screen underneath it.")
                PrivacyPoint("You choose what's included", "Only the apps you explicitly pick become islands. Everything else keeps stock Android behavior.")
            }
            Spacer(Modifier.height(10.dp))

            FaqItem("Does closing the app stop notifications?", open == "closing", { toggle("closing") }) {
                Text(
                    "No. Sileo's notification listener runs in the background on its own — it stays active as " +
                        "long as \"Notification access\" is granted, even after you swipe the app away from " +
                        "Recents. You don't need to keep the app open; it's only the setup screen.\n\n" +
                        "On Xiaomi / POCO / Redmi (MIUI / HyperOS) the system may aggressively kill background " +
                        "apps. If islands stop appearing, enable Autostart and set battery to \"No restrictions\" " +
                        "for Sileo, then toggle Notification access off and back on.",
                    color = c.textBody, fontSize = 13.5.sp, lineHeight = 20.sp,
                )
            }
            Spacer(Modifier.height(10.dp))

            FaqItem("How to disable Sileo completely", open == "disable", { toggle("disable") }) {
                Text(
                    "To fully stop Sileo, undo the access you granted. Turning off Notification access alone " +
                        "disables it — the rest is cleanup.",
                    color = c.textBody, fontSize = 13.5.sp, lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                SettingsActionButton("1.  Turn off Notification access") { openListenerSettings(context) }
                Spacer(Modifier.height(8.dp))
                SettingsActionButton("2.  Turn off Display over other apps") { openOverlaySettings(context) }
                Spacer(Modifier.height(8.dp))
                SettingsActionButton("3.  App info — battery, autostart, uninstall") { openAppInfo(context) }
                Spacer(Modifier.height(10.dp))
                Text(
                    "On MIUI, also disable Autostart and \"Display pop-up windows…\" from App info — you can " +
                        "uninstall from there too.",
                    color = c.textSecondary, fontSize = 12.5.sp, lineHeight = 18.sp,
                )
            }
            Spacer(Modifier.height(10.dp))

            FaqItem("Replies & actions", open == "actions", { toggle("actions") }) {
                Text(
                    "Sileo isn't a full notification replacement. Reply and action buttons don't work inline " +
                        "like native notifications — tapping one simply opens the app it came from.",
                    color = c.textBody, fontSize = 13.5.sp, lineHeight = 20.sp,
                )
            }

            Spacer(Modifier.height(28.dp))
            Text("About", color = c.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Sileo for Android is an independent, open-source project built on top of Sileo (web) by Aaryan.",
                color = c.textBody, fontSize = 13.5.sp, lineHeight = 19.sp,
            )
            Spacer(Modifier.height(12.dp))
            LinkText("The original Sileo (web) by Aaryan  ↗") { openUrl(context, SILEO_WEB_URL) }
            Spacer(Modifier.height(8.dp))
            LinkText("This app's source on GitHub  ↗") { openUrl(context, REPO_URL) }
            Spacer(Modifier.height(8.dp))
            LinkText("Report a bug / feedback  ↗") { openUrl(context, ISSUES_URL) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A collapsible FAQ card: tap the header to expand/collapse its body. */
@Composable
private fun FaqItem(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val c = appColors()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Spacer(Modifier.size(10.dp))
            Text(if (expanded) "–" else "+", color = c.textSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) { content() }
        }
    }
}

/** A full-width outlined button that deep-links into a system settings screen. */
@Composable
private fun SettingsActionButton(label: String, onClick: () -> Unit) {
    val c = appColors()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, c.track, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 14.dp),
    ) {
        Text(label, color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrivacyPoint(title: String, body: String) {
    val c = appColors()
    Column(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Text(title, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(3.dp))
        Text(body, color = c.textBody, fontSize = 13.5.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun SetupStep(num: String, title: String, body: String) {
    val c = appColors()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(50))
                .background(c.filledBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(num, color = c.filledOn, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(body, color = c.textBody, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun LinkText(label: String, onClick: () -> Unit) {
    val c = appColors()
    Text(
        label,
        color = c.accent, fontSize = 14.5.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 2.dp),
    )
}

@Composable
private fun PermissionRow(
    step: String,
    title: String,
    desc: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    val c = appColors()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .clickable(enabled = !granted, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(if (granted) c.success else c.track),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (granted) "✓" else step, color = if (granted) Color.White else c.textBody, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = c.textSecondary, fontSize = 12.5.sp)
        }
        if (!granted) {
            Text("Grant", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ChooseAppsRow(appCount: Int, onClick: () -> Unit) {
    val c = appColors()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(if (appCount > 0) c.success else c.track),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (appCount > 0) "✓" else "3", color = if (appCount > 0) Color.White else c.textBody, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Choose apps", color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (appCount > 0) "$appCount selected" else "None yet — pick apps to show as islands",
                color = c.textSecondary, fontSize = 12.5.sp,
            )
        }
        Text("Edit", color = c.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FilledButton(label: String, onClick: () -> Unit) {
    val c = appColors()
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.filledBg)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = c.filledOn, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    val c = appColors()
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(label, color = c.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// --- helpers ---

private fun openUrl(ctx: Context, url: String) {
    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

// --- "Disable Sileo" deep-links into the relevant system settings screens ---

private fun openListenerSettings(ctx: Context) {
    runCatching { ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
}

private fun openOverlaySettings(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")),
        )
    }
}

private fun openAppInfo(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")),
        )
    }
}

private fun isListenerEnabled(ctx: Context): Boolean {
    val flat = Settings.Secure.getString(ctx.contentResolver, "enabled_notification_listeners") ?: return false
    val target = ComponentName(ctx, SileoNotificationListenerService::class.java)
    return flat.split(":").any { ComponentName.unflattenFromString(it) == target }
}

private fun createTestChannel(ctx: Context) {
    val mgr = ctx.getSystemService(NotificationManager::class.java)
    val channel = NotificationChannel(TEST_CHANNEL, "Sileo Test", NotificationManager.IMPORTANCE_HIGH)
    mgr.createNotificationChannel(channel)
}

private fun postTestNotification(ctx: Context) {
    val n = NotificationCompat.Builder(ctx, TEST_CHANNEL)
        .setSmallIcon(R.drawable.ic_launcher_foreground)
        .setContentTitle("Test Notification")
        .setContentText("If this shows up as a Sileo island, the listener works end-to-end.")
        .setAutoCancel(true)
        .build()
    runCatching { NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), n) }
}
