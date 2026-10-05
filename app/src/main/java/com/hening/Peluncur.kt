package com.hening

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast

object Peluncur {

    fun buka(ctx: Context, app: App) {
        mulai(
            ctx,
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(app.komponen)
                .addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED),
        )
    }

    /** Pintasan geser: aplikasi pilihan, atau telepon dan kamera bila belum dipilih. */
    fun pintasan(ctx: Context, apps: List<App>, pkg: String, kiri: Boolean) {
        val app = apps.firstOrNull { it.pkg == pkg }
        if (app != null) {
            buka(ctx, app)
        } else {
            mulai(ctx, if (kiri) Intent(Intent.ACTION_DIAL) else Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        }
    }

    fun info(ctx: Context, app: App) =
        mulai(ctx, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.pkg}")))

    fun copot(ctx: Context, app: App) =
        mulai(ctx, Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.pkg}")))

    fun pengaturanHome(ctx: Context) = mulai(ctx, Intent(Settings.ACTION_HOME_SETTINGS))

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
