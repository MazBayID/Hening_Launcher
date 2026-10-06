package com.hening

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

object Peluncur {

    fun buka(ctx: Context, app: App, p: Pengaturan? = null) {
        p?.catatBuka(app.pkg)
        mulai(
            ctx,
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.komponen)
                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
    }

    /** Pintasan geser: aplikasi pilihan, atau telepon dan kamera bila belum dipilih. */
    fun pintasan(ctx: Context, apps: List<App>, pkg: String, kiri: Boolean, p: Pengaturan? = null) {
        val app = apps.firstOrNull { it.pkg == pkg }
        if (app != null) {
            buka(ctx, app, p)
        } else {
            mulai(ctx, if (kiri) Intent(Intent.ACTION_DIAL) else Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        }
    }

    fun info(ctx: Context, app: App) =
        mulai(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")))

    fun infoHening(ctx: Context) =
        mulai(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}")))

    fun copot(ctx: Context, app: App) =
        mulai(ctx, Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")))

    fun pengaturanHome(ctx: Context) = mulai(ctx, Intent(Settings.ACTION_HOME_SETTINGS))
    fun aksesNotifikasi(ctx: Context) = mulai(ctx, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    fun aksesibilitas(ctx: Context) = mulai(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    fun bukaUrl(ctx: Context, url: String) = mulai(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

    fun salin(ctx: Context, teks: String) {
        val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("hening", teks))
        Toast.makeText(ctx, "Disalin", Toast.LENGTH_SHORT).show()
    }

    /** Mengunci layar lewat layanan aksesibilitas. Perlu diaktifkan di pengaturan. */
    fun kunciLayar(ctx: Context) {
        val s = ServisKunci.instance
        if (s == null || Build.VERSION.SDK_INT < 28) {
            Toast.makeText(ctx, "Aktifkan layanan kunci layar Hening di pengaturan", Toast.LENGTH_SHORT).show()
        } else {
            s.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
    }

    /** Membuka panel notifikasi lewat StatusBarManager. Diam saja bila ROM tidak mengizinkan. */
    fun bukaNotifikasi(ctx: Context) {
        try {
            val sb = ctx.getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sb)
        } catch (e: Exception) {
        }
    }

    private fun mulai(ctx: Context, intent: Intent) {
        try {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(ctx, "Tidak bisa dibuka", Toast.LENGTH_SHORT).show()
        }
    }
}
