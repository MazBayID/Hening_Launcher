@file:OptIn(ExperimentalMaterial3Api::class)

package com.hening

import android.Manifest
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

/** Layar pengaturan, dibuka dengan menekan lama beranda atau perintah :set. */
@Composable
fun LayarPengaturan(apps: List<App>, p: Pengaturan, tutup: () -> Unit) {
    val ctx = LocalContext.current
    val t = p.tema
    val petaApp = remember(apps) { apps.associateBy { it.pkg } }
    val peta = remember(p.str("nama")) { p.petaNama() }
    var pilihKiri by remember { mutableStateOf(false) }
    var pilihKanan by remember { mutableStateOf(false) }
    val versi = remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName } catch (e: Exception) { "?" }
    }
    val izinKalender = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        p.taruh("agenda", ok)
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
            Pilihan("Tema warna", daftarTema.map { it.nama }, p.int("tema", 0)) { p.taruh("tema", it) }
            Sakelar("Font monospace", p.mono) { p.taruh("mono", it) }
            Pilihan("Ukuran teks favorit", listOf("Kecil", "Sedang", "Besar"), p.ukuran) { p.taruh("ukuran", it) }
            Pilihan("Rata teks", listOf("Kiri", "Tengah", "Kanan"), p.rata) { p.taruh("rata", it) }
            Pilihan("Jumlah favorit", (3..8).map { "$it" }, p.jumlahFav - 3) { p.taruh("jumlah", it + 3) }
            Sakelar("Tampilkan jam", p.tampilJam) { p.taruh("jam", it) }
            Sakelar("Tampilkan tanggal", p.tampilTanggal) { p.taruh("tanggal", it) }
            Sakelar("Format 24 jam", p.jam24) { p.taruh("jam24", it) }
            Sakelar("Sembunyikan status bar", p.bool("hidestatus", false)) { p.taruh("hidestatus", it) }

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
            Sakelar("Tampilkan penggeser alfabet", p.bool("alfabet", true)) { p.taruh("alfabet", it) }
            Sakelar("Getaran halus pada penggeser", p.bool("haptik", true)) { p.taruh("haptik", it) }
            Sakelar("Buka otomatis bila hanya satu hasil", p.autoBuka) { p.taruh("autobuka", it) }
            Sakelar("Munculkan keyboard saat membuka daftar", p.autoKeyboard) { p.taruh("autokeyboard", it) }
            Teks("Perintah di kolom cari: g kata, 2+3*4, :baru, :sering, :ruang, :tema, :kunci, :set, :help")

            Judul("Pintasan geser")
            Teks("Geser kanan membuka pintasan kiri, geser kiri membuka pintasan kanan. Ketuk tulisan di bawah beranda untuk membukanya langsung.")
            BarisApp("Pintasan kiri", petaApp[p.str("pintasan_kiri")]?.let { peta[it.pkg] ?: it.label } ?: "Telepon") { pilihKiri = true }
            BarisApp("Pintasan kanan", petaApp[p.str("pintasan_kanan")]?.let { peta[it.pkg] ?: it.label } ?: "Kamera") { pilihKanan = true }

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
            Teks("Ketuk dua kali beranda untuk mengunci layar. Perlu mengaktifkan layanan aksesibilitas Hening, yang hanya dipakai untuk mengunci layar. Layanan ini ditampilkan sebagai Hening: kunci layar.")
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

            Judul("Lainnya")
            OutlinedButton(onClick = { Peluncur.pengaturanHome(ctx) }) { Text("Jadikan Hening launcher default") }
            Teks("Hening versi $versi")
            Spacer(Modifier.height(24.dp))
        }
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
        Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 15.sp)
        Text(nilai, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), fontSize = 15.sp)
    }
}

/** Dialog pilih aplikasi dengan pencarian. Tombol Bawaan mengembalikan ke pilihan awal. */
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
