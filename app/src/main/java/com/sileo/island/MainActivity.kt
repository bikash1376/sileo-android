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
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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

private const val TEST_CHANNEL = "sileo_test"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        createTestChannel(this)
        setContent { App() }
    }
}

@Composable
private fun App() {
    var route by remember { mutableStateOf("home") }
    when (route) {
        "apps" -> AppPickerScreen(onBack = { route = "home" })
        else -> OnboardingScreen(onChooseApps = { route = "apps" })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingScreen(onChooseApps: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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
            .background(Color(0xFFF4F4F5)),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
                .padding(top = 32.dp),
        ) {
            Text("Sileo · Android", color = Color(0xFF18181B), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Turn your notifications into the island.",
                color = Color(0xFF71717A), fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp),
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
                color = if (ready) Color(0xFF15803D) else Color(0xFF71717A),
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
            Text("Quick look (renders in the overlay):", color = Color(0xFF71717A), fontSize = 13.sp)
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
        }
    }
}

@Composable
private fun PermissionRow(
    step: String,
    title: String,
    desc: String,
    granted: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(enabled = !granted, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(if (granted) Color(0xFF34C759) else Color(0xFFE4E4E7)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (granted) "✓" else step, color = if (granted) Color.White else Color(0xFF52525B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFF18181B), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, color = Color(0xFF71717A), fontSize = 12.5.sp)
        }
        if (!granted) {
            Text("Grant", color = Color(0xFF0A84FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ChooseAppsRow(appCount: Int, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(50))
                .background(if (appCount > 0) Color(0xFF34C759) else Color(0xFFE4E4E7)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (appCount > 0) "✓" else "3", color = if (appCount > 0) Color.White else Color(0xFF52525B), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Choose apps", color = Color(0xFF18181B), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (appCount > 0) "$appCount selected" else "None yet — pick apps to show as islands",
                color = Color(0xFF71717A), fontSize = 12.5.sp,
            )
        }
        Text("Edit", color = Color(0xFF0A84FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FilledButton(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF18181B))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Chip(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(label, color = Color(0xFF27272A), fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

// --- helpers ---

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
