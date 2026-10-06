package com.hening

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.ZoneId

/** Acara kalender berikutnya dalam 24 jam ke depan. Perlu izin baca kalender. */
object Agenda {
    fun izin(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun berikut(ctx: Context): String? {
        if (!izin(ctx)) return null
        return try {
            val mulai = System.currentTimeMillis()
            val akhir = mulai + 24L * 3600L * 1000L
            val b = CalendarContract.Instances.CONTENT_URI.buildUpon()
            ContentUris.appendId(b, mulai)
            ContentUris.appendId(b, akhir)
            val kolom = arrayOf(
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.ALL_DAY,
            )
            ctx.contentResolver.query(b.build(), kolom, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                var hasil: String? = null
                while (hasil == null && c.moveToNext()) {
                    if (c.getInt(2) == 0) {
                        val w = Instant.ofEpochMilli(c.getLong(1)).atZone(ZoneId.systemDefault())
                        hasil = "%02d:%02d %s".format(w.hour, w.minute, c.getString(0) ?: "(tanpa judul)")
                    }
                }
                hasil
            }
        } catch (e: Exception) {
            null
        }
    }
}
