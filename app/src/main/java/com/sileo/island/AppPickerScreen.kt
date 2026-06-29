package com.sileo.island

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.sileo.island.ui.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppEntry(val pkg: String, val label: String, val icon: ImageBitmap)

@Composable
fun AppPickerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val c = appColors()

    val appsState = produceState<List<AppEntry>?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { loadLaunchableApps(context) }
    }
    val apps = appsState.value

    // Local mirror of the enabled set so toggles update instantly.
    val enabled: SnapshotStateMap<String, Boolean> = remember {
        mutableStateMapOf<String, Boolean>().apply {
            AppPrefs.enabled(context).forEach { put(it, true) }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.pageBg)
            .padding(top = 32.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "‹ Back",
                color = c.accent,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Spacer(Modifier.size(16.dp))
            Text("Choose apps", color = c.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Notifications from selected apps show as the Sileo island (and skip the default popup). Others stay untouched.",
            color = c.textSecondary, fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        if (apps == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.textPrimary)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(apps, key = { it.pkg }) { app ->
                    val on = enabled[app.pkg] == true
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                val next = !on
                                enabled[app.pkg] = next
                                AppPrefs.setEnabled(context, app.pkg, next)
                            }
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Image(
                            bitmap = app.icon,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                        Text(
                            app.label,
                            color = c.textPrimary,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = on,
                            onCheckedChange = { next ->
                                enabled[app.pkg] = next
                                AppPrefs.setEnabled(context, app.pkg, next)
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun loadLaunchableApps(ctx: Context): List<AppEntry> {
    val pm = ctx.packageManager
    val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(launcher, 0)
        .asSequence()
        .map { it.activityInfo.applicationInfo }
        .distinctBy { it.packageName }
        .filter { it.packageName != ctx.packageName }
        .map { info: ApplicationInfo ->
            AppEntry(
                pkg = info.packageName,
                label = pm.getApplicationLabel(info).toString(),
                icon = pm.getApplicationIcon(info).toBitmap(96, 96).asImageBitmap(),
            )
        }
        .sortedBy { it.label.lowercase() }
        .toList()
}
