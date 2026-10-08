package com.hening

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import java.io.File
import androidx.compose.ui.text.font.Typeface as TypefaceCompose

/** Font kustom (.ttf atau .otf) pilihan pengguna, disalin ke penyimpanan internal aplikasi. */
object FontKustom {
    var family by mutableStateOf<FontFamily?>(null)

    private fun berkas(ctx: Context) = File(ctx.filesDir, "font_kustom.ttf")

    fun simpan(ctx: Context, uri: Uri): Boolean {
        return try {
            val f = berkas(ctx)
            val dibaca = ctx.contentResolver.openInputStream(uri)?.use { masuk ->
                f.outputStream().use { keluar -> masuk.copyTo(keluar) }
                true
            } ?: false
            dibaca && Typeface.createFromFile(f) != Typeface.DEFAULT
        } catch (e: Exception) {
            false
        }
    }

    fun muat(ctx: Context): FontFamily? {
        val f = berkas(ctx)
        if (!f.exists()) return null
        return try {
            FontFamily(TypefaceCompose(Typeface.createFromFile(f)))
        } catch (e: Exception) {
            null
        }
    }

    fun hapus(ctx: Context) {
        berkas(ctx).delete()
    }
}
