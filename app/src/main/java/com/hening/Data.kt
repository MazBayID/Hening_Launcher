package com.hening

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.net.Uri
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import org.json.JSONObject

data class App(val label: String, val pkg: String, val komponen: ComponentName, val terpasang: Long)

data class Tema(
    val nama: String,
    val bg: Color,
    val fg: Color,
    val aksen: Color,
    val aksen2: Color,
    val redup: Color,
    val gelap: Boolean,
)

val daftarTema = listOf(
    Tema("Terminal", Color(0xFF0C0C0C), Color(0xFFD0D0D0), Color(0xFF00E676), Color(0xFF40C4FF), Color(0xFF6B7280), true),
    Tema("Matrix", Color(0xFF000000), Color(0xFF33FF66), Color(0xFF00FF41), Color(0xFF00B32C), Color(0xFF1B7A2F), true),
    Tema("Dracula", Color(0xFF282A36), Color(0xFFF8F8F2), Color(0xFFBD93F9), Color(0xFF50FA7B), Color(0xFF6272A4), true),
    Tema("Monokai", Color(0xFF272822), Color(0xFFF8F8F2), Color(0xFFA6E22E), Color(0xFFF92672), Color(0xFF75715E), true),
    Tema("Nord", Color(0xFF2E3440), Color(0xFFECEFF4), Color(0xFF88C0D0), Color(0xFFA3BE8C), Color(0xFF616E88), true),
    Tema("Gruvbox", Color(0xFF282828), Color(0xFFEBDBB2), Color(0xFFFABD2F), Color(0xFFB8BB26), Color(0xFF928374), true),
    Tema("Solarized", Color(0xFF002B36), Color(0xFF93A1A1), Color(0xFF2AA198), Color(0xFFB58900), Color(0xFF586E75), true),
    Tema("One Dark", Color(0xFF282C34), Color(0xFFABB2BF), Color(0xFF61AFEF), Color(0xFF98C379), Color(0xFF5C6370), true),
    Tema("Hitam", Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFB0BEC5), Color(0xFF90CAF9), Color(0xFF757575), true),
    Tema("Putih", Color(0xFFF7F7F5), Color(0xFF111111), Color(0xFF1565C0), Color(0xFF2E7D32), Color(0xFF757575), false),
)

/** Nama semua tema: bawaan, lalu Kustom (indeks 10) dan Material You (indeks 11). */
fun namaTemaSemua(): List<String> = daftarTema.map { it.nama } + listOf("Kustom", "Material You")

fun hexDari(c: Color): String = "#%06X".format(c.toArgb() and 0xFFFFFF)

/** Membaca daftar aplikasi. Tanpa ikon, jadi ringan dan cepat. */
object Aplikasi {
    fun muat(ctx: Context): List<App> {
        val pm = ctx.packageManager
        val q = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(q, 0).mapNotNull { ri ->
            val ai = ri.activityInfo ?: return@mapNotNull null
            if (ai.packageName == ctx.packageName) return@mapNotNull null
            val waktu = try { pm.getPackageInfo(ai.packageName, 0).firstInstallTime } catch (e: Exception) { 0L }
            App(ri.loadLabel(pm).toString(), ai.packageName, ComponentName(ai.packageName, ai.name), waktu)
        }.distinctBy { it.pkg }.sortedBy { it.label.lowercase() }
    }
}

/**
 * Pengaturan tersimpan di SharedPreferences dan dibaca lewat state Compose,
 * jadi layar otomatis diperbarui saat nilainya berubah.
 */
class Pengaturan(context: Context) {
    private val ctxApp = context.applicationContext
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

    val tema: Tema
        get() {
            val i = int("tema", 0)
            return when (i) {
                10 -> temaKustom()
                11 -> materialYou()
                else -> daftarTema[i.coerceIn(0, daftarTema.lastIndex)]
            }
        }

    /** Warna tema kustom. Kosong berarti memakai warna tema Terminal. */
    fun warnaKustom(k: String): Color {
        val d = daftarTema[0]
        val bawaan = when (k) {
            "bg" -> d.bg
            "fg" -> d.fg
            "aksen" -> d.aksen
            "aksen2" -> d.aksen2
            else -> d.redup
        }
        val s = str("tk_$k")
        return if (s.isBlank()) bawaan else try { Color(android.graphics.Color.parseColor(s)) } catch (e: Exception) { bawaan }
    }

    fun temaKustom(): Tema {
        val bg = warnaKustom("bg")
        return Tema("Kustom", bg, warnaKustom("fg"), warnaKustom("aksen"), warnaKustom("aksen2"), warnaKustom("redup"), bg.luminance() < 0.5f)
    }

    /** Menyalin warna sebuah tema ke slot Kustom lalu memilihnya. */
    fun salinKeKustom(t: Tema) {
        taruh("tk_bg", hexDari(t.bg))
        taruh("tk_fg", hexDari(t.fg))
        taruh("tk_aksen", hexDari(t.aksen))
        taruh("tk_aksen2", hexDari(t.aksen2))
        taruh("tk_redup", hexDari(t.redup))
        taruh("tema", 10)
    }

    private val myGelap by lazy { buatMaterialYou(true) }
    private val myTerang by lazy { buatMaterialYou(false) }

    private fun buatMaterialYou(gelap: Boolean): Tema {
        if (Build.VERSION.SDK_INT < 31) return daftarTema[4]
        val c = if (gelap) dynamicDarkColorScheme(ctxApp) else dynamicLightColorScheme(ctxApp)
        return Tema("Material You", c.background, c.onBackground, c.primary, c.tertiary, c.outline, gelap)
    }

    /** Warna dari wallpaper sistem (Android 12 ke atas). Mengikuti mode gelap atau terang sistem. */
    fun materialYou(): Tema {
        val gelap = (ctxApp.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        return if (gelap) myGelap else myTerang
    }

    fun eksporTema(): String {
        val t = tema
        return JSONObject()
            .put("hening_tema", 1)
            .put("bg", hexDari(t.bg))
            .put("fg", hexDari(t.fg))
            .put("aksen", hexDari(t.aksen))
            .put("aksen2", hexDari(t.aksen2))
            .put("redup", hexDari(t.redup))
            .toString(2)
    }

    fun imporTema(teks: String): Boolean {
        return try {
            val o = JSONObject(teks)
            val kunci = listOf("bg", "fg", "aksen", "aksen2", "redup")
            val nilaiBaru = kunci.map { k ->
                val h = o.getString(k)
                android.graphics.Color.parseColor(h)
                k to h.uppercase()
            }
            nilaiBaru.forEach { (k, h) -> taruh("tk_$k", h) }
            taruh("tema", 10)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Seluruh pengaturan sebagai JSON untuk cadangan. Foto wallpaper dan font kustom tidak ikut. */
    fun eksporSemua(): String {
        val data = JSONObject()
        nilai.forEach { (k, v) ->
            val e = JSONObject()
            when (v) {
                is Int -> e.put("t", "i").put("v", v)
                is Boolean -> e.put("t", "b").put("v", v)
                is String -> e.put("t", "s").put("v", v)
                else -> null
            }?.let { data.put(k, it) }
        }
        return JSONObject().put("hening", 1).put("data", data).toString(2)
    }

    fun imporSemua(teks: String): Boolean {
        return try {
            val data = JSONObject(teks).getJSONObject("data")
            val kunci = data.keys().asSequence().toList()
            kunci.forEach { k ->
                val e = data.getJSONObject(k)
                when (e.getString("t")) {
                    "i" -> taruh(k, e.getInt("v"))
                    "b" -> taruh(k, e.getBoolean("v"))
                    "s" -> taruh(k, e.getString("v"))
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    val gaya: Int get() = int("gaya", 0)
    val mono: Boolean get() = bool("mono", true)
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

    /** Favorit untuk ditampilkan: urut manual, atau urut pemakaian bila diaktifkan. */
    fun favoritUrut(r: Int = ruang): List<String> {
        val l = favorit(r)
        return if (bool("urutauto", false)) l.sortedByDescending { hit(it) } else l
    }

    fun hit(pkg: String): Int = int("hit:$pkg", 0)
    fun catatBuka(pkg: String) = taruh("hit:$pkg", hit(pkg) + 1)

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

    fun turunFav(pkg: String) {
        val l = favorit().toMutableList()
        val i = l.indexOf(pkg)
        if (i >= 0 && i < l.lastIndex) {
            l.removeAt(i)
            l.add(i + 1, pkg)
            taruhDaftar("fav$ruang", l)
        }
    }

    /** Grup: aplikasi yang bersembunyi di balik sebuah favorit (geser kanan pada favorit untuk membukanya). */
    fun grup(induk: String): List<String> = daftar("grup:$induk")

    fun toggleGrup(induk: String, pkg: String) {
        val l = grup(induk).toMutableList()
        if (!l.remove(pkg)) l.add(pkg)
        taruhDaftar("grup:$induk", l)
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
