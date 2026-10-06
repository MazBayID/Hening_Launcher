package com.hening

import android.content.Context
import android.net.Uri
import java.math.BigDecimal
import java.math.RoundingMode

/** Satu baris hasil perintah di kolom pencarian. */
data class Aksi(val teks: String, val jalankan: () -> Unit)

/**
 * Perintah gaya terminal di kolom pencarian:
 * "g kata" cari di web, "2+3*4" kalkulator, ":set", ":ruang", ":tema", ":kunci", ":baru", ":sering", ":help".
 */
fun perintah(q: String, ctx: Context, p: Pengaturan, bukaPengaturan: () -> Unit, tutup: () -> Unit): List<Aksi> {
    val hasil = mutableListOf<Aksi>()
    val teks = q.trim()

    if (teks.startsWith("g ") || teks.startsWith("? ")) {
        val kata = teks.drop(2).trim()
        if (kata.isNotEmpty()) {
            hasil.add(Aksi("web › $kata") {
                Peluncur.bukaUrl(ctx, "https://duckduckgo.com/?q=" + Uri.encode(kata))
                tutup()
            })
        }
    }

    val bolehMatematika = teks.any { it in "+-*/^%" } && teks.any { it.isDigit() } &&
        teks.all { it in "0123456789+-*/^%().,  " }
    if (bolehMatematika) {
        val v = Matematika.hitung(teks)
        if (v != null) {
            val s = Matematika.format(v)
            hasil.add(Aksi("= $s   (ketuk untuk menyalin)") { Peluncur.salin(ctx, s) })
        }
    }

    if (teks.startsWith(":")) {
        val bagian = teks.drop(1).trim().split(" ", limit = 2)
        val cmd = bagian[0].lowercase()
        val arg = bagian.getOrElse(1) { "" }.trim()
        when (cmd) {
            "set", "pengaturan" -> hasil.add(Aksi("buka pengaturan") { tutup(); bukaPengaturan() })
            "ruang" -> (0..1).filter { r -> arg.isBlank() || p.namaRuang(r).startsWith(arg, ignoreCase = true) }
                .forEach { r -> hasil.add(Aksi("ruang › ${p.namaRuang(r)}") { p.taruh("ruang", r); tutup() }) }
            "tema" -> daftarTema.forEachIndexed { i, tm ->
                if (arg.isBlank() || tm.nama.startsWith(arg, ignoreCase = true)) {
                    hasil.add(Aksi("tema › ${tm.nama}") { p.taruh("tema", i) })
                }
            }
            "kunci" -> hasil.add(Aksi("kunci layar") { tutup(); Peluncur.kunciLayar(ctx) })
            "baru" -> hasil.add(Aksi("aplikasi yang baru dipasang:") {})
            "sering" -> hasil.add(Aksi("aplikasi yang paling sering dibuka:") {})
            "help", "?" -> listOf(
                "g <kata>     cari di web",
                "2+3*4        kalkulator",
                ":baru        aplikasi baru dipasang",
                ":sering      aplikasi paling sering",
                ":ruang <n>   pindah ruang",
                ":tema <n>    ganti tema",
                ":kunci       kunci layar",
                ":set         pengaturan",
            ).forEach { hasil.add(Aksi(it) {}) }
            else -> hasil.add(Aksi("perintah tidak dikenal. coba :help") {})
        }
    }
    return hasil
}

/** Kalkulator sederhana: + - * / % ^ dan tanda kurung. */
object Matematika {
    fun hitung(teks: String): Double? =
        try { Penguraian(teks.replace(',', '.').replace(" ", "")).urai() } catch (e: Exception) { null }

    fun format(v: Double): String =
        BigDecimal(v).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

    private class Penguraian(val s: String) {
        var i = 0

        fun urai(): Double {
            val v = ekspresi()
            if (i != s.length) throw IllegalStateException()
            return v
        }

        fun ekspresi(): Double {
            var v = suku()
            while (i < s.length && (s[i] == '+' || s[i] == '-')) {
                val op = s[i++]
                val r = suku()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        fun suku(): Double {
            var v = pangkat()
            while (i < s.length && (s[i] == '*' || s[i] == '/' || s[i] == '%')) {
                val op = s[i++]
                val r = pangkat()
                v = when (op) { '*' -> v * r; '/' -> v / r; else -> v % r }
            }
            return v
        }

        fun pangkat(): Double {
            val dasar = unari()
            if (i < s.length && s[i] == '^') {
                i++
                return Math.pow(dasar, pangkat())
            }
            return dasar
        }

        fun unari(): Double {
            if (i < s.length && s[i] == '-') { i++; return -unari() }
            if (i < s.length && s[i] == '+') { i++; return unari() }
            return dasar()
        }

        fun dasar(): Double {
            if (i < s.length && s[i] == '(') {
                i++
                val v = ekspresi()
                if (i >= s.length || s[i] != ')') throw IllegalStateException()
                i++
                return v
            }
            val awal = i
            while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
            if (awal == i) throw IllegalStateException()
            return s.substring(awal, i).toDouble()
        }
    }
}
