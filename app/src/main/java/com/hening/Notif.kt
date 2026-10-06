package com.hening

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.app.NotificationManagerCompat

data class Ringkas(val jumlah: Int, val teks: String)

object Notif {
    /** Ringkasan notifikasi per paket, dibaca langsung oleh layar. */
    val data = mutableStateMapOf<String, Ringkas>()

    fun aktif(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
}

/** Membaca notifikasi untuk lencana di bawah favorit dan untuk widget media. Perlu izin akses notifikasi. */
class PendengarNotifikasi : NotificationListenerService() {

    override fun onListenerConnected() {
        instance = this
        segarkan()
    }

    override fun onListenerDisconnected() {
        if (instance === this) instance = null
        Notif.data.clear()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = segarkan()
    override fun onNotificationRemoved(sbn: StatusBarNotification?) = segarkan()

    private fun segarkan() {
        val baru = HashMap<String, Ringkas>()
        try {
            val daftar = activeNotifications ?: emptyArray()
            daftar
                .filter {
                    (it.notification.flags and Notification.FLAG_ONGOING_EVENT) == 0 &&
                        (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0
                }
                .groupBy { it.packageName }
                .forEach { (pkg, l) ->
                    val terbaru = l.maxByOrNull { it.postTime }
                    if (terbaru != null) {
                        val e = terbaru.notification.extras
                        val judul = e.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                        val isi = e.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                        baru[pkg] = Ringkas(l.size, listOf(judul, isi).filter { it.isNotBlank() }.joinToString(": "))
                    }
                }
        } catch (e: Exception) {
        }
        Notif.data.keys.retainAll(baru.keys)
        Notif.data.putAll(baru)
    }

    companion object {
        var instance: PendengarNotifikasi? = null

        /** Menghapus semua notifikasi yang bisa dihapus dari satu aplikasi. */
        fun hapus(pkg: String) {
            val s = instance ?: return
            try {
                s.activeNotifications
                    ?.filter { it.packageName == pkg && it.isClearable }
                    ?.forEach { s.cancelNotification(it.key) }
            } catch (e: Exception) {
            }
        }
    }
}

/** Layanan aksesibilitas yang hanya dipakai untuk mengunci layar. Tidak membaca isi layar. */
class ServisKunci : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        var instance: ServisKunci? = null
    }
}

class InfoMedia(val judul: String, val artis: String, val main: Boolean, val kontrol: MediaController)

/** Sesi media yang sedang aktif. Hanya bisa dibaca bila akses notifikasi diberikan. */
fun ambilMedia(ctx: Context): InfoMedia? {
    return try {
        val msm = ctx.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
        val sesi = msm.getActiveSessions(ComponentName(ctx, PendengarNotifikasi::class.java))
        val k = sesi.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: sesi.firstOrNull()
        val m = k?.metadata
        val judul = m?.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty()
        if (k == null || m == null || judul.isBlank()) {
            null
        } else {
            InfoMedia(
                judul,
                m.getString(MediaMetadata.METADATA_KEY_ARTIST).orEmpty(),
                k.playbackState?.state == PlaybackState.STATE_PLAYING,
                k,
            )
        }
    } catch (e: Exception) {
        null
    }
}
