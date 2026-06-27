package com.sileo.island.service

import android.app.Notification
import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.service.notification.NotificationListenerService
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
import com.sileo.island.Sileo
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

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val n = sbn.notification ?: return
        // Skip noise: persistent/foreground-service notifications and group summaries.
        if (n.flags and Notification.FLAG_ONGOING_EVENT != 0) return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val text = (extras.getCharSequence(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT))?.toString()?.trim()
        if (title.isNullOrEmpty() && text.isNullOrEmpty()) return

        Sileo.notification(
            title = title ?: appLabel(sbn.packageName),
            description = text?.ifEmpty { null },
            icon = appIconBitmap(sbn.packageName),
        )
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
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            // Pass-through: never steals touches from the app underneath.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP }

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
