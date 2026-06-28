package com.sileo.island

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

/**
 * The Sileo notification variants, mirroring the web component.
 * Accent colors are the OKLCH values from the original, converted to sRGB.
 */
enum class SileoVariant(val accent: Color) {
    SUCCESS(Color(0xFF34C759)),
    ERROR(Color(0xFFFF453A)),
    WARNING(Color(0xFFFFB020)),
    INFO(Color(0xFF0A84FF)),
    ACTION(Color(0xFF5E5CE6)),
    PROMISE(Color(0xFF8E8E93)), // loading / neutral grey until it resolves
}

/** A single live toast. */
data class ToastData(
    val id: Long,
    val variant: SileoVariant,
    val title: String,
    val description: String? = null,
    val actionLabel: String? = null,
    val durationMs: Long = 6000L,
    val loading: Boolean = false,
    // When set (real notifications), shown in the badge instead of the variant glyph.
    val appIcon: ImageBitmap? = null,
    // Real-notification intents. contentIntent launches the source app on tap;
    // actionIntent fires the first action button (e.g. "Done"). Null for in-app demos.
    val contentIntent: android.app.PendingIntent? = null,
    val actionIntent: android.app.PendingIntent? = null,
)

/**
 * Tiny in-process controller — the equivalent of `sileo.success(...)` on the web.
 * Compose observes [toasts] directly because it is a snapshot-backed list.
 */
object Sileo {
    private val ids = AtomicLong(0L)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val toasts = mutableStateListOf<ToastData>()

    /**
     * True while Sileo's own activity is visible. The system overlay hides its
     * island then, so only the in-app host draws — otherwise both hosts render
     * the same toast at the same spot and it looks doubled (one collapses on tap,
     * the other keeps running its own timeline).
     */
    var appInForeground by mutableStateOf(false)

    fun show(data: ToastData) {
        toasts.add(data)
    }

    fun dismiss(id: Long) {
        toasts.removeAll { it.id == id }
    }

    /** Replace a live toast in place (same id) — used to resolve a promise. */
    fun update(id: Long, transform: (ToastData) -> ToastData) {
        val i = toasts.indexOfFirst { it.id == id }
        if (i >= 0) toasts[i] = transform(toasts[i])
    }

    private fun fire(
        variant: SileoVariant,
        title: String,
        description: String?,
        actionLabel: String? = null,
        loading: Boolean = false,
        durationMs: Long = 6000L,
        appIcon: ImageBitmap? = null,
        contentIntent: android.app.PendingIntent? = null,
        actionIntent: android.app.PendingIntent? = null,
    ): Long {
        val id = ids.incrementAndGet()
        show(
            ToastData(
                id = id,
                variant = variant,
                title = title,
                description = description,
                actionLabel = actionLabel,
                loading = loading,
                durationMs = durationMs,
                appIcon = appIcon,
                contentIntent = contentIntent,
                actionIntent = actionIntent,
            )
        )
        return id
    }

    /** A real OS notification: shows the posting app's icon in the badge. */
    fun notification(
        title: String,
        description: String?,
        icon: ImageBitmap?,
        variant: SileoVariant = SileoVariant.INFO,
        actionLabel: String? = null,
        loading: Boolean = false,
        contentIntent: android.app.PendingIntent? = null,
        actionIntent: android.app.PendingIntent? = null,
    ) = fire(
        variant, title, description, actionLabel = actionLabel, loading = loading,
        appIcon = icon, contentIntent = contentIntent, actionIntent = actionIntent,
    )

    fun success(title: String, description: String? = null) =
        fire(SileoVariant.SUCCESS, title, description)

    fun error(title: String, description: String? = null) =
        fire(SileoVariant.ERROR, title, description)

    fun warning(title: String, description: String? = null) =
        fire(SileoVariant.WARNING, title, description)

    fun info(title: String, description: String? = null) =
        fire(SileoVariant.INFO, title, description)

    fun action(title: String, description: String? = null, actionLabel: String = "Open") =
        fire(SileoVariant.ACTION, title, description, actionLabel = actionLabel)

    /**
     * The promise flow: shows a loading island, then resolves to success after a
     * beat — title/badge/description morph, it settles back to a pill, then leaves.
     */
    fun promise(
        title: String,
        description: String? = null,
        resolvedTitle: String = "Saved",
        resolvedDescription: String = "Your changes are synced to the cloud.",
        resolveAfterMs: Long = 1900L,
    ): Long {
        val id = fire(
            SileoVariant.PROMISE, title, description,
            loading = true, durationMs = resolveAfterMs + 2900L,
        )
        scope.launch {
            delay(resolveAfterMs)
            update(id) {
                it.copy(
                    variant = SileoVariant.SUCCESS,
                    loading = false,
                    title = resolvedTitle,
                    description = resolvedDescription,
                )
            }
        }
        return id
    }
}
