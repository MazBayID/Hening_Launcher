package com.hening

import android.content.Context

/**
 * Gestur yang aksinya bisa diatur di Pengaturan.
 * Setiap slot menyimpan kode aksi di kunci "gest_<slot>". Aksi "Buka aplikasi" menyimpan paketnya di "gest_<slot>_app".
 */
object Gestur {
    val slot = listOf("atas", "bawah", "kiri", "kanan", "dobel", "tekan", "abjad")
    val namaSlot = listOf(
        "Geser atas", "Geser bawah", "Geser kiri", "Geser kanan",
        "Ketuk dua kali beranda", "Tekan lama beranda", "Ketuk dua kali abjad",
    )
    val aksi = listOf(
        "Tidak ada", "Daftar aplikasi", "Panel notifikasi", "Kunci layar", "Pengaturan Hening",
        "Pintasan kiri", "Pintasan kanan", "Buka aplikasi…", "Ganti Ruang",
    )

    fun bawaan(s: String): Int = when (s) {
        "atas" -> 1
        "bawah" -> 2
        "kiri" -> 6
        "kanan" -> 5
        "dobel" -> 3
        "tekan" -> 4
        else -> 3
    }

    fun kode(p: Pengaturan, s: String): Int = p.int("gest_$s", bawaan(s))

    fun jalankan(s: String, ctx: Context, p: Pengaturan, apps: List<App>, onLaci: () -> Unit, onPengaturan: () -> Unit) {
        when (kode(p, s)) {
            1 -> onLaci()
            2 -> Peluncur.bukaNotifikasi(ctx)
            3 -> Peluncur.kunciLayar(ctx)
            4 -> onPengaturan()
            5 -> Peluncur.pintasan(ctx, apps, p.str("pintasan_kiri"), true, p)
            6 -> Peluncur.pintasan(ctx, apps, p.str("pintasan_kanan"), false, p)
            7 -> apps.firstOrNull { it.pkg == p.str("gest_${s}_app") }?.let { Peluncur.buka(ctx, it, p) }
            8 -> p.taruh("ruang", 1 - p.ruang)
        }
    }
}
