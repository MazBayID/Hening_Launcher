@file:OptIn(ExperimentalMaterial3Api::class)

package com.hening

import android.Manifest
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val DETIK_JEDA = listOf(3, 5, 8, 12)

private fun tulis(ctx: Context, uri: Uri?, teks: String) {
    if (uri == null) return
    try {
        ctx.contentResolver.openOutputStream(uri)?.use { it.write(teks.toByteArray()) }
        Toast.makeText(ctx, "Tersimpan", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(ctx, "Gagal menyimpan", Toast.LENGTH_SHORT).show()
    }
}

private fun baca(ctx: Context, uri: Uri?): String? {
    if (uri == null) return null
    return try {
        ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
    } catch (e: Exception) {
        null
    }
}

/** Layar pengaturan, dibuka dengan menekan lama beranda (bila gestur bawaan) atau perintah :set. */
@Composable
fun LayarPengaturan(apps: List<App>, p: Pengaturan, tutup: () -> Unit) {
    val ctx = LocalContext.current
    val t = p.tema
    val scope = rememberCoroutineScope()
    val petaApp = remember(apps) { apps.associateBy { it.pkg } }
    val peta = remember(p.str("nama")) { p.petaNama() }
    var pilihKiri by remember { mutableStateOf(false) }
    var pilihKanan by remember { mutableStateOf(false) }
    var dialogSlot by remember { mutableStateOf<Int?>(null) }
    val versi = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (e: Exception) { "?" }
    }
    val dataWaktu by produceState(emptyMap<String, Long>(), ctx) {
        value = withContext(Dispatchers.IO) { Waktu.hariIni(ctx) }
    }

    val izinKalender = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        p.taruh("agenda", ok)
    }
    val pilihFoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { Foto.simpan(ctx, uri) }
                if (ok) {
                    p.taruh("wall", 2)
                    p.taruh("foto_versi", p.int("foto_versi", 0) + 1)
                } else {
                    Toast.makeText(ctx, "Foto tidak bisa dipakai", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val pilihFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { FontKustom.simpan(ctx, uri) }
                if (ok) {
                    p.taruh("font", 2)
                    p.taruh("font_versi", p.int("font_versi", 0) + 1)
                } else {
                    Toast.makeText(ctx, "Berkas font tidak valid", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val simpanTema = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        tulis(ctx, uri, p.eksporTema())
    }
    val bukaTema = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val s = baca(ctx, uri)
        if (s != null) {
            Toast.makeText(ctx, if (p.imporTema(s)) "Tema diimpor" else "Berkas tema tidak valid", Toast.LENGTH_SHORT).show()
        }
    }
    val simpanCadangan = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        tulis(ctx, uri, p.eksporSemua())
    }
    val bukaCadangan = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        val s = baca(ctx, uri)
        if (s != null) {
            Toast.makeText(ctx, if (p.imporSemua(s)) "Cadangan dipulihkan" else "Berkas cadangan tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(t.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = tutup) { Text("← kembali", color = t.fg) }
            Text(if (p.gaya == 0) "~/pengaturan" else "Pengaturan", color = t.aksen, fontSize = 18.sp)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Judul("Tampilan")
            Pilihan("Gaya beranda", listOf("Terminal", "Minimal"), p.gaya) { p.taruh("gaya", it) }
            Pilihan("Tema warna", namaTemaSemua(), p.int("tema", 0)) { p.taruh("tema", it) }
            Teks("Material You mengikuti warna wallpaper sistem (Android 12 ke atas).")
            val modeFont = p.int("font", if (p.bool("mono", true)) 0 else 1)
            Pilihan("Font", listOf("Monospace", "Sans", "Kustom"), modeFont) { p.taruh("font", it) }
            if (modeFont == 2) {
                OutlinedButton(onClick = { pilihFont.launch(arrayOf("*/*")) }) { Text("Pilih berkas font (.ttf / .otf)") }
                OutlinedButton(onClick = {
                    FontKustom.hapus(ctx)
                    FontKustom.family = null
                    p.taruh("font", 0)
                }) { Text("Hapus font kustom") }
                Teks("Contoh: JetBrains Mono atau Fira Code, unduh dari situs resminya lalu pilih berkas .ttf-nya.")
            }
            Pilihan("Gaya jam", listOf("Digital", "Analog", "Flip", "Bertumpuk"), p.int("gayajam", 0)) { p.taruh("gayajam", it) }
            Pilihan("Ukuran teks favorit", listOf("Kecil", "Sedang", "Besar"), p.ukuran) { p.taruh("ukuran", it) }
            Pilihan("Rata teks", listOf("Kiri", "Tengah", "Kanan"), p.rata) { p.taruh("rata", it) }
            Pilihan("Jumlah favorit", (3..8).map { "$it" }, p.jumlahFav - 3) { p.taruh("jumlah", it + 3) }
            Sakelar("Tampilkan jam", p.tampilJam) { p.taruh("jam", it) }
            Sakelar("Tampilkan tanggal", p.tampilTanggal) { p.taruh("tanggal", it) }
            Sakelar("Format 24 jam", p.jam24) { p.taruh("jam24", it) }
            Sakelar("Sembunyikan status bar", p.bool("hidestatus", false)) { p.taruh("hidestatus", it) }

            Judul("Editor tema")
            Teks("Salin warna dari tema yang ada, lalu ubah sesukamu. Tema Kustom otomatis terpilih.")
            Pilihan("Salin warna dari", daftarTema.map { it.nama }, -1) { p.salinKeKustom(daftarTema[it]) }
            if (p.int("tema", 0) == 10) {
                EditorWarna("Latar (bg)", "bg", p)
                EditorWarna("Teks (fg)", "fg", p)
                EditorWarna("Aksen utama", "aksen", p)
                EditorWarna("Aksen kedua", "aksen2", p)
                EditorWarna("Teks redup", "redup", p)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { simpanTema.launch("hening-tema.json") }) { Text("Ekspor tema") }
                OutlinedButton(onClick = { bukaTema.launch(arrayOf("*/*")) }) { Text("Impor tema") }
            }

            Judul("Wallpaper")
            Pilihan("Latar", listOf("Polos", "Gradien", "Foto"), p.int("wall", 0)) { p.taruh("wall", it) }
            if (p.int("wall", 0) == 1) {
                Pilihan("Gradien", daftarGradien.map { it.nama }, p.int("gradien", 0)) { p.taruh("gradien", it) }
            }
            if (p.int("wall", 0) == 2) {
                OutlinedButton(onClick = { pilihFoto.launch("image/*") }) { Text("Pilih foto dari galeri") }
                OutlinedButton(onClick = {
                    Foto.hapus(ctx)
                    LatarCache.bitmap = null
                    p.taruh("wall", 0)
                }) { Text("Hapus foto") }
            }
            if (p.int("wall", 0) != 0) {
                Text("Peredup latar: ${p.int("scrim", 55)}%", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
                Slider(
                    value = p.int("scrim", 55) / 100f,
                    onValueChange = { p.taruh("scrim", (it * 100).roundToInt()) },
                    valueRange = 0f..0.9f,
                )
                Teks("Peredup memakai warna tema supaya teks tetap terbaca di atas gradien atau foto.")
            }

            Judul("Favorit dan Ruang")
            Sakelar("Urutkan favorit otomatis menurut pemakaian", p.bool("urutauto", false)) { p.taruh("urutauto", it) }
            Sakelar("Ganti Ruang otomatis (Kerja Sen-Jum 08.00-17.00)", p.bool("autoruang", false)) { p.taruh("autoruang", it) }
            listOf(0, 1).forEach { r ->
                OutlinedTextField(
                    value = p.namaRuang(r),
                    onValueChange = { p.taruh("ruang$r", it.take(12)) },
                    label = { Text("Nama ruang ${r + 1}") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Teks("Geser kanan pada sebuah favorit untuk membuka grupnya. Tekan lama aplikasi lalu pilih Masukkan ke grup favorit.")

            Judul("Daftar aplikasi")
            Sakelar("Abjad melengkung di sisi kanan beranda", p.bool("alfabetberanda", true)) { p.taruh("alfabetberanda", it) }
            Sakelar("Penggeser alfabet di daftar aplikasi", p.bool("alfabet", true)) { p.taruh("alfabet", it) }
            Sakelar("Getaran halus pada penggeser", p.bool("haptik", true)) { p.taruh("haptik", it) }
            Sakelar("Buka otomatis bila hanya satu hasil", p.autoBuka) { p.taruh("autobuka", it) }
            Sakelar("Munculkan keyboard saat membuka daftar", p.autoKeyboard) { p.taruh("autokeyboard", it) }
            Teks("Perintah di kolom cari: g kata, 2+3*4, :baru, :sering, :waktu, :ruang, :tema, :kunci, :set, :help")

            Judul("Gestur")
            Teks("Atur aksi untuk tiap gestur. Pintasan kiri dan kanan diatur di bagian berikutnya.")
            Gestur.slot.forEachIndexed { i, s ->
                val kode = Gestur.kode(p, s)
                val ket = if (kode == 7) {
                    "Buka " + (petaApp[p.str("gest_${s}_app")]?.let { peta[it.pkg] ?: it.label } ?: "(belum dipilih)")
                } else {
                    Gestur.aksi[kode.coerceIn(0, Gestur.aksi.lastIndex)]
                }
                BarisApp(Gestur.namaSlot[i], ket) { dialogSlot = i }
            }

            Judul("Pintasan geser")
            Teks("Aplikasi untuk aksi Pintasan kiri dan Pintasan kanan. Teks di bawah beranda juga membukanya langsung.")
            BarisApp("Pintasan kiri", petaApp[p.str("pintasan_kiri")]?.let { peta[it.pkg] ?: it.label } ?: "Telepon") { pilihKiri = true }
            BarisApp("Pintasan kanan", petaApp[p.str("pintasan_kanan")]?.let { peta[it.pkg] ?: it.label } ?: "Kamera") { pilihKanan = true }

            Judul("Waktu layar dan jeda sadar")
            val izinWaktu = Waktu.izin(ctx)
            Teks("Usage access: " + if (izinWaktu) "aktif" else "nonaktif")
            if (!izinWaktu) {
                Teks("Dibutuhkan untuk menampilkan waktu layar per aplikasi dan batas harian.")
                OutlinedButton(onClick = { Peluncur.aksesUsage(ctx) }) { Text("Buka pengaturan Usage access") }
            }
            Sakelar("Tampilkan waktu layar hari ini di beranda", p.bool("tampilwaktu", false)) { p.taruh("tampilwaktu", it) }
            Pilihan("Lama jeda sadar", DETIK_JEDA.map { "$it dtk" }, DETIK_JEDA.indexOf(p.int("jedadetik", 5)).coerceAtLeast(0)) {
                p.taruh("jedadetik", DETIK_JEDA[it])
            }
            Teks("Jeda sadar muncul sebelum aplikasi yang kamu tandai terbuka. Tekan lama sebuah aplikasi lalu pilih Aktifkan jeda sadar atau Batas harian.")
            val dipantau = apps.filter { p.bool("pantau:${it.pkg}", false) || p.int("batas:${it.pkg}", 0) > 0 }
            if (dipantau.isEmpty()) Teks("Belum ada aplikasi yang dipantau.")
            dipantau.forEach { a ->
                val batas = p.int("batas:${a.pkg}", 0)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        (peta[a.pkg] ?: a.label) + if (batas > 0) "  (batas $batas m)" else "",
                        color = t.fg,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { p.taruh("pantau:${a.pkg}", false); p.taruh("batas:${a.pkg}", 0) }) { Text("Lepas") }
                }
            }
            val labelApp = petaApp
            val teratas = dataWaktu.filterKeys { it in labelApp }.entries.sortedByDescending { it.value }.take(8)
            if (teratas.isNotEmpty()) {
                Teks("Hari ini (total " + Waktu.format(dataWaktu.filterKeys { it in labelApp }.values.sum()) + "):")
                teratas.forEach { e ->
                    Text(
                        Waktu.format(e.value).padEnd(7) + (peta[e.key] ?: labelApp[e.key]?.label ?: e.key),
                        color = t.fg,
                        fontSize = 13.sp,
                    )
                }
            }

            Judul("Notifikasi dan media")
            val notifAktif = Notif.aktif(ctx)
            Teks("Akses notifikasi: " + if (notifAktif) "aktif" else "nonaktif")
            Teks("Bila aktif, jumlah dan ringkasan notifikasi muncul di bawah favorit (tekan lama ringkasan untuk menghapus), dan widget media tampil saat ada lagu diputar.")
            OutlinedButton(onClick = { Peluncur.aksesNotifikasi(ctx) }) { Text("Buka pengaturan akses notifikasi") }

            Judul("Agenda")
            Sakelar("Tampilkan acara kalender berikutnya", p.bool("agenda", false)) { aktif ->
                if (aktif && !Agenda.izin(ctx)) izinKalender.launch(Manifest.permission.READ_CALENDAR) else p.taruh("agenda", aktif)
            }

            Judul("Kunci layar")
            Teks("Aksi Kunci layar perlu mengaktifkan layanan aksesibilitas Hening, yang hanya dipakai untuk mengunci layar. Layanan ini ditampilkan sebagai Hening: kunci layar.")
            Teks("Status: " + if (ServisKunci.instance != null) "aktif" else "nonaktif")
            OutlinedButton(onClick = { Peluncur.aksesibilitas(ctx) }) { Text("Buka pengaturan aksesibilitas") }

            Judul("Android 13 ke atas")
            Teks("Aplikasi yang dipasang dari luar Play Store sering diblokir untuk akses notifikasi dan aksesibilitas. Bila tombolnya abu-abu, buka Info aplikasi, ketuk menu titik tiga, lalu pilih Izinkan setelan terbatas.")
            OutlinedButton(onClick = { Peluncur.infoHening(ctx) }) { Text("Buka info aplikasi Hening") }

            Judul("Aplikasi tersembunyi")
            val tersembunyi = p.tersembunyi.mapNotNull { petaApp[it] }
            if (tersembunyi.isEmpty()) Teks("Belum ada. Tekan lama aplikasi di daftar lalu pilih Sembunyikan.")
            tersembunyi.forEach { a ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(peta[a.pkg] ?: a.label, color = t.fg, modifier = Modifier.weight(1f))
                    TextButton(onClick = { p.toggleSembunyi(a.pkg) }) { Text("Tampilkan") }
                }
            }

            Judul("Cadangan")
            Teks("Menyimpan seluruh pengaturan (tema, favorit, Ruang, gestur, jeda sadar) ke satu berkas. Foto wallpaper dan font kustom tidak ikut.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { simpanCadangan.launch("hening-cadangan.json") }) { Text("Buat cadangan") }
                OutlinedButton(onClick = { bukaCadangan.launch(arrayOf("*/*")) }) { Text("Pulihkan") }
            }

            Judul("Lainnya")
            OutlinedButton(onClick = { Peluncur.pengaturanHome(ctx) }) { Text("Jadikan Hening launcher default") }
            Teks("Hening versi $versi")
            Spacer(Modifier.height(24.dp))
        }
    }

    dialogSlot?.let { i ->
        DialogAksi(Gestur.slot[i], Gestur.namaSlot[i], p, apps) { dialogSlot = null }
    }
    if (pilihKiri) {
        DialogApp(apps, "Pintasan kiri", { a -> p.taruh("pintasan_kiri", a?.pkg ?: ""); pilihKiri = false }) { pilihKiri = false }
    }
    if (pilihKanan) {
        DialogApp(apps, "Pintasan kanan", { a -> p.taruh("pintasan_kanan", a?.pkg ?: ""); pilihKanan = false }) { pilihKanan = false }
    }
}

@Composable
private fun Judul(teks: String) {
    Text(teks, color = MaterialTheme.colorScheme.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun Teks(teks: String) {
    Text(teks, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f), fontSize = 12.sp)
}

@Composable
private fun Sakelar(label: String, nilai: Boolean, ubah: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(checked = nilai, onCheckedChange = ubah)
    }
}

@Composable
private fun Pilihan(label: String, opsi: List<String>, terpilih: Int, pilih: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            opsi.forEachIndexed { i, nama ->
                val aktif = i == terpilih
                Text(
                    nama,
                    fontSize = 13.sp,
                    color = if (aktif) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (aktif) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { pilih(i) }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
}

@Composable
private fun BarisApp(label: String, nilai: String, aksi: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { aksi() }.padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(nilai, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontSize = 15.sp)
    }
}

/** Satu warna tema kustom, diedit sebagai kode hex #RRGGBB. */
@Composable
private fun EditorWarna(label: String, kunci: String, p: Pengaturan) {
    val aktif = hexDari(p.warnaKustom(kunci))
    var teks by remember(aktif) { mutableStateOf(aktif) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(p.warnaKustom(kunci)))
        OutlinedTextField(
            value = teks,
            onValueChange = {
                teks = it.take(7)
                if (Regex("^#[0-9A-Fa-f]{6}\$").matches(teks)) p.taruh("tk_$kunci", teks.uppercase())
            },
            label = { Text(label) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

/** Dialog memilih aksi untuk satu gestur. */
@Composable
private fun DialogAksi(slot: String, nama: String, p: Pengaturan, apps: List<App>, tutup: () -> Unit) {
    var pilihApp by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = tutup,
        title = { Text(nama) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Gestur.aksi.forEachIndexed { i, n ->
                    val aktif = Gestur.kode(p, slot) == i
                    Text(
                        (if (aktif) "● " else "○ ") + n,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                p.taruh("gest_$slot", i)
                                if (i == 7) pilihApp = true else tutup()
                            }
                            .padding(vertical = 10.dp),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = tutup) { Text("Tutup") } },
    )
    if (pilihApp) {
        DialogApp(apps, nama, { a -> p.taruh("gest_${slot}_app", a?.pkg ?: ""); pilihApp = false; tutup() }) { pilihApp = false }
    }
}

/** Dialog pilih aplikasi dengan pencarian. Tombol Bawaan mengosongkan pilihan. */
@Composable
private fun DialogApp(apps: List<App>, judul: String, pilih: (App?) -> Unit, tutup: () -> Unit) {
    var cari by remember { mutableStateOf("") }
    val hasil = remember(apps, cari) { apps.filter { it.label.contains(cari.trim(), ignoreCase = true) } }
    AlertDialog(
        onDismissRequest = tutup,
        title = { Text(judul) },
        text = {
            Column {
                OutlinedTextField(value = cari, onValueChange = { cari = it }, singleLine = true, placeholder = { Text("Cari…") })
                LazyColumn(Modifier.heightIn(max = 300.dp)) {
                    items(hasil, key = { it.pkg }) { a ->
                        Text(a.label, modifier = Modifier.fillMaxWidth().clickable { pilih(a) }.padding(vertical = 10.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { pilih(null) }) { Text("Bawaan") } },
        dismissButton = { TextButton(onClick = tutup) { Text("Batal") } },
    )
}
