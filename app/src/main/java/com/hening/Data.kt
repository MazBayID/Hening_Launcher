package com.hening

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color

data class App(val label: String, val pkg: String, val komponen: ComponentName)

data class Tema(val nama: String, val bg: Color, val fg: Color, val gelap: Boolean)

val daftarTema = listOf(
    Tema("Hitam", Color(0xFF000000), Color(0xFFFFFFFF), true),
    Tema("Abu gelap", Color(0xFF16181D), Color(0xFFECEFF4), true),
    Tema("Biru malam", Color(0xFF0B132B), Color(0xFFE0E6F5), true),
    Tema("Putih", Color(0xFFF7F7F5), Color(0xFF111111), false),
)

/** Membaca daftar aplikasi. Tanpa ikon, jadi ringan dan cepat. */
object Aplikasi {
    fun muat(ctx: Context): List<App> {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(q, 0).mapNotNull { ri ->
            val ai = ri.activityInfo ?: return@mapNotNull null
            if (ai.packageName == ctx.packageName) return@mapNotNull null
            App(ri.loadLabel(pm).toString(), ai.packageName, ComponentName(ai.packageName, ai.name))
        }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    }
}

/**
 * Pengaturan tersimpan di SharedPreferences dan dibaca lewat state Compose,
 * jadi layar otomatis diperbarui saat nilainya berubah.
 */
class Pengaturan(context: Context) {
    private val sp = context.getSharedPreferences("hening", Context.MODE_PRIVATE)
    private val nilai = mutableStateMapOf<String, Any>().apply {
        sp.all.forEach { (k, v) -> if (v != null) put(k, v) }
    }

    fun int(k: String, d: Int): Int = (nilai[k] as? Int) ?: d
    fun bool(k: String, d: Boolean): Boolean = (nilai[k] as? Boolean) ?: d
    fun str(k: String, d: String = ""): String = (nilai[k] as? String) ?: d

    fun taruh(k: String, v: Any) {
        nilai[k] = v
        val e = sp.edit()
        when (v) {
            is Int -> e.putInt(k, v)
            is Boolean -> e.putBoolean(k, v)
            is String -> e.putString(k, v)
        }
        e.apply()
    }

    private fun daftar(k: String): List<String> = str(k).split(",").filter { it.isNotBlank() }
    private fun taruhDaftar(k: String, l: List<String>) = taruh(k, l.joinToString(","))

    val tema: Tema get() = daftarTema[int("tema", 0).coerceIn(0, daftarTema.lastIndex)]
    val ruang: Int get() = int("ruang", 0)
    val jumlahFav: Int get() = int("jumlah", 6)
    val rata: Int get() = int("rata", 0)
    val ukuran: Int get() = int("ukuran", 1)
    val tampilJam: Boolean get() = bool("jam", true)
    val tampilTanggal: Boolean get() = bool("tanggal", true)
    val jam24: Boolean get() = bool("jam24", true)
    val autoBuka: Boolean get() = bool("autobuka", true)
    val autoKeyboard: Boolean get() = bool("autokeyboard", false)
    val tersembunyi: List<String> get() = daftar("hidden")

    fun namaRuang(r: Int): String = str("ruang$r", if (r == 0) "Santai" else "Kerja")
    fun favorit(r: Int = ruang): List<String> = daftar("fav$r")

    /** Menambah atau menghapus favorit di Ruang aktif. False bila daftar sudah penuh. */
    fun toggleFav(pkg: String): Boolean {
        val l = favorit().toMutableList()
        if (l.remove(pkg)) {
            taruhDaftar("fav$ruang", l)
            return true
        }
        if (l.size >= jumlahFav) return false
        l.add(pkg)
        taruhDaftar("fav$ruang", l)
        return true
    }

    fun naikFav(pkg: String) {
        val l = favorit().toMutableList()
        val i = l.indexOf(pkg)
        if (i > 0) {
            l.removeAt(i)
            l.add(i - 1, pkg)
            taruhDaftar("fav$ruang", l)
        }
    }

    fun toggleSembunyi(pkg: String) {
        val l = tersembunyi.toMutableList()
        if (!l.remove(pkg)) l.add(pkg)
        taruhDaftar("hidden", l)
    }

    fun petaNama(): Map<String, String> =
        str("nama").split(",").mapNotNull { t ->
            val b = t.split(":")
            if (b.size == 2 && b[0].isNotBlank()) b[0] to Uri.decode(b[1]) else null
        }.toMap()

    fun setNama(pkg: String, nama: String) {
        val m = petaNama().toMutableMap()
        if (nama.isBlank()) {
            m.remove(pkg)
        } else {
            m[pkg] = nama.trim()
        }
        taruh("nama", m.entries.joinToString(",") { "${it.key}:${Uri.encode(it.value)}" })
    }

    /** Pemakaian pertama: isi favorit Ruang pertama dengan aplikasi umum yang terpasang. */
    fun isiAwal(terpasang: Set<String>) {
        if (bool("awal", false) || terpasang.isEmpty()) return
        val kandidat = listOf(
            "com.google.android.dialer", "com.google.android.apps.messaging", "com.android.chrome",
            "com.google.android.youtube", "com.google.android.apps.photos", "com.android.settings",
        )
        taruhDaftar("fav0", kandidat.filter { it in terpasang })
        taruh("awal", true)
    }
}
