package com.hening

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File

data class Gradien(val nama: String, val warna: List<Color>)

val daftarGradien = listOf(
    Gradien("Aurora", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C7A7B))),
    Gradien("Senja", listOf(Color(0xFF1A0B2E), Color(0xFF6D214F), Color(0xFFFF6F61))),
    Gradien("Laut", listOf(Color(0xFF000428), Color(0xFF004E92))),
    Gradien("Hutan", listOf(Color(0xFF0B3D2E), Color(0xFF1B5E20), Color(0xFF6B8E23))),
    Gradien("Ungu", listOf(Color(0xFF1D1135), Color(0xFF5B2A86))),
    Gradien("Grafit", listOf(Color(0xFF0D0D0D), Color(0xFF2D2D2D), Color(0xFF454545))),
)

/** Foto wallpaper yang sudah dimuat, dipakai bersama oleh beranda dan daftar aplikasi. */
object LatarCache {
    var bitmap by mutableStateOf<ImageBitmap?>(null)
}

/** Foto wallpaper pilihan pengguna, disimpan di penyimpanan internal aplikasi. Tidak butuh izin. */
object Foto {
    private fun berkas(ctx: Context) = File(ctx.filesDir, "wallpaper.jpg")

    fun simpan(ctx: Context, uri: Uri): Boolean {
        return try {
            val dm = ctx.resources.displayMetrics
            val maks = maxOf(dm.widthPixels, dm.heightPixels)
            val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
            var s = 1
            while (batas.outWidth / (s * 2) >= maks && batas.outHeight / (s * 2) >= maks) s *= 2
            val opsi = BitmapFactory.Options().apply { inSampleSize = s }
            val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opsi) }
            if (bmp == null) {
                false
            } else {
                val orientasi = ctx.contentResolver.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
                val derajat = when (orientasi) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
                val hasil = if (derajat == 0f) bmp else Bitmap.createBitmap(
                    bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(derajat) }, true
                )
                berkas(ctx).outputStream().use { hasil.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun muat(ctx: Context): ImageBitmap? {
        val f = berkas(ctx)
        return if (f.exists()) BitmapFactory.decodeFile(f.path)?.asImageBitmap() else null
    }

    fun hapus(ctx: Context) {
        berkas(ctx).delete()
    }
}

/**
 * Latar belakang: warna tema (polos), gradien, atau foto.
 * Gradien dan foto diredupkan dengan warna tema supaya teks tetap terbaca.
 */
@Composable
fun LatarBelakang(p: Pengaturan, t: Tema) {
    val mode = p.int("wall", 0)
    val redup = p.int("scrim", 55) / 100f
    Box(Modifier.fillMaxSize()) {
        when (mode) {
            1 -> Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(daftarGradien[p.int("gradien", 0).coerceIn(0, daftarGradien.lastIndex)].warna)
                )
            )
            2 -> {
                val b = LatarCache.bitmap
                if (b != null) Image(b, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        if (mode != 0) Box(Modifier.fillMaxSize().background(t.bg.copy(alpha = redup)))
    }
}
