package com.sileo.island.service

import android.app.Notification
import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.NotificationListenerService.RankingMap
import android.service.notification.StatusBarNotification
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sileo.island.AppPrefs
import com.sileo.island.BuildConfig
import com.sileo.island.Sileo
import com.sileo.island.SileoVariant
import com.sileo.island.ui.SileoOverlay

/**
 * The bridge to the real OS. While the user has granted "Notification access",
 * Android keeps this service alive and delivers every notification to
 * [onNotificationPosted]. We map each one to a [Sileo] toast, and host the island
 * itself in a system overlay window added right here — so no separate foreground
 * service is needed (the listener's own lifecycle keeps everything alive).
 */
class SileoNotificationListenerService : NotificationListenerService() {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val overlayOwner = OverlayLifecycleOwner()

    override fun onListenerConnected() {
        super.onListenerConnected()
        com.sileo.island.SileoSettings.load(this)
        addOverlay()
    }

    override fun onListenerDisconnected() {
        removeOverlay()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    // Keys we've already turned into an island, so progress updates / snooze-returns
    // don't spam or loop.
    private val handledKeys = HashSet<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        // Self-heal: if overlay permission was granted *after* the listener connected,
        // onListenerConnected()'s addOverlay() already bailed. Retry here (idempotent)
        // so the window comes up as soon as any notification arrives.
        addOverlay()

        val n = sbn.notification ?: return
        // Diagnostic logging is DEBUG-only and never includes notification content
        // (titles/text can hold OTPs, messages, banking alerts). Capture in a debug
        // build with:  adb logcat -s SileoListener
        dbg(
            "posted pkg=${sbn.packageName} cat=${n.category} flags=0x${n.flags.toString(16)} " +
                "ongoing=${n.flags and Notification.FLAG_ONGOING_EVENT != 0} enabled=${AppPrefs.isEnabled(this, sbn.packageName)}",
        )
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            dbg("  drop: group summary")
            return
        }
        // Only intercept apps the user opted in (our own test notification always
        // passes); everything else keeps stock behavior.
        if (sbn.packageName != packageName && !AppPrefs.isEnabled(this, sbn.packageName)) {
            dbg("  drop: package not enabled in picker")
            return
        }
        if (sbn.key in handledKeys) return // already shown (update / snooze return)

        val progress = hasProgress(n)
        val ongoing = n.flags and Notification.FLAG_ONGOING_EVENT != 0
        // Skip foreground-service noise, but DO allow ongoing progress (downloads etc).
        if (ongoing && !progress) {
            dbg("  drop: ongoing & not progress (alarm/fgs)")
            return
        }

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()?.trim()
        if (title.isNullOrEmpty() && text.isNullOrEmpty()) {
            dbg("  drop: empty title & text")
            return
        }
        dbg("  -> showing island") // no title: never log notification content

        val (variant, loading) = classify(n, progress)
        val actionLabel = if (variant == SileoVariant.ACTION) {
            n.actions?.firstOrNull()?.title?.toString()
        } else null

        handledKeys += sbn.key
        Sileo.notification(
            title = title ?: appLabel(sbn.packageName),
            description = text?.ifEmpty { null },
            icon = appIconBitmap(sbn.packageName),
            variant = variant,
            actionLabel = actionLabel,
            loading = loading,
            // Tap launches the app; the action chip fires the first action button.
            contentIntent = n.contentIntent,
            actionIntent = n.actions?.firstOrNull()?.actionIntent,
        )

        // Suppress the system heads-up for chosen apps (one-shot transient notifs only;
        // progress/ongoing ones are left alone so they keep updating in the shade).
        if (!ongoing && !progress) {
            runCatching { snoozeNotification(sbn.key, 1500L) }
        }
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification?,
        rankingMap: RankingMap?,
        reason: Int,
    ) {
        // Crucial: when WE snooze a notification to hide its heads-up, the system
        // fires this with REASON_SNOOZED and later re-posts it. If we forgot the key
        // here, the re-post would re-show + re-snooze forever. Only forget on a real
        // dismissal, so the snooze-return is de-duped and the loop can't happen.
        if (sbn != null && reason != REASON_SNOOZED) {
            handledKeys.remove(sbn.key)
        }
        super.onNotificationRemoved(sbn, rankingMap, reason)
    }

    /** DEBUG-only logger. No-ops in release so nothing about notifications ships. */
    private fun dbg(msg: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("SileoListener", msg)
    }

    private fun hasProgress(n: Notification): Boolean {
        val e = n.extras
        return e.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false) ||
            e.getInt(Notification.EXTRA_PROGRESS_MAX, 0) > 0
    }

    /** Map an OS notification to the Sileo variant that best fits it. */
    private fun classify(n: Notification, progress: Boolean): Pair<SileoVariant, Boolean> {
        if (progress) return SileoVariant.PROMISE to true
        val hasActions = (n.actions?.size ?: 0) > 0
        return when (n.category) {
            Notification.CATEGORY_ERROR -> SileoVariant.ERROR to false
            Notification.CATEGORY_CALL,
            Notification.CATEGORY_ALARM,
            Notification.CATEGORY_REMINDER,
            Notification.CATEGORY_EVENT -> SileoVariant.ACTION to false
            Notification.CATEGORY_MESSAGE,
            Notification.CATEGORY_EMAIL,
            Notification.CATEGORY_SOCIAL -> SileoVariant.INFO to false
            Notification.CATEGORY_PROGRESS -> SileoVariant.PROMISE to true
            else -> if (hasActions) SileoVariant.ACTION to false else SileoVariant.INFO to false
        }
    }

    private fun appIconBitmap(pkg: String) = runCatching {
        packageManager.getApplicationIcon(pkg)
            .toBitmap(width = 96, height = 96)
            .asImageBitmap()
    }.getOrNull()

    // --- Overlay window ---

    private fun addOverlay() {
        if (overlayView != null) return
        if (!Settings.canDrawOverlays(this)) return // user hasn't granted "display over apps" yet

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager = wm
        overlayOwner.start()

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(overlayOwner)
            setViewTreeViewModelStoreOwner(overlayOwner)
            setViewTreeSavedStateRegistryOwner(overlayOwner)
            setContent { SileoOverlay() }
        }
        overlayView = view

        val params = WindowManager.LayoutParams(
            // MATCH_PARENT width so the window never repositions horizontally as the
            // island's width changes (expand→collapse). A WRAP_CONTENT width window
            // re-centers itself every frame, which on some launchers/MIUI glitches and
            // leaves the island stuck drifted to the side. The island centers itself in
            // Compose instead. Height is WRAP_CONTENT so the window is only a top strip
            // (as tall as the island), leaving the rest of the screen pass-through; when
            // no island is showing the content is empty, so the strip shrinks to nothing.
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // NOT_FOCUSABLE: don't steal the keyboard / back button. We intentionally do
            // NOT set NOT_TOUCHABLE anymore, so the island can receive taps.
            // FLAG_HARDWARE_ACCELERATED is required for the RenderEffect goo to draw:
            // windows added directly via WindowManager are NOT HW-accelerated by default
            // (unlike Activity windows), so without it the gooey layer renders blank on
            // real devices even though it works on some emulators.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }

        runCatching { wm.addView(view, params) }
    }

    private fun removeOverlay() {
        overlayView?.let { v -> runCatching { windowManager?.removeView(v) } }
        overlayView = null
        overlayOwner.stop()
    }

    private fun appLabel(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)
}

/**
 * Minimal owner so a [ComposeView] can run inside a window that isn't an Activity.
 * Compose needs Lifecycle / ViewModelStore / SavedStateRegistry from the view tree.
 */
private class OverlayLifecycleOwner :
    LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val vmStore = ViewModelStore()
    private val savedStateController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = vmStore
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry

    fun start() {
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun stop() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        vmStore.clear()
    }
}
