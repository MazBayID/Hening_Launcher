@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.hening

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import androidx.compose.animation.core.*


private val TANGGAL = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))

/**
 * Beranda: jam, favorit sebagai teks, ringkasan notifikasi di bawah favorit, widget media, dan agenda.
 * Geser atas = daftar aplikasi, bawah = notifikasi, kiri/kanan = pintasan,
 * ketuk dua kali = kunci layar, tekan lama = pengaturan. Geser kanan pada favorit = grup.
 */
@Composable
fun Beranda(apps: List<App>, p: Pengaturan, onLaci: () -> Unit, onPengaturan: () -> Unit) {
    val ctx = LocalContext.current
    val t = p.tema
    val terminal = p.gaya == 0
    val peta = remember(p.str("nama")) { p.petaNama() }
    val petaApp = remember(apps) { apps.associateBy { it.pkg } }
    val favorit = p.favoritUrut().mapNotNull { petaApp[it] }.take(p.jumlahFav)
    val ukuran = when (p.ukuran) { 0 -> 22.sp; 2 -> 32.sp; else -> 27.sp }
    val rata = when (p.rata) { 1 -> Alignment.CenterHorizontally; 2 -> Alignment.End; else -> Alignment.Start }
    val teksRata = when (p.rata) { 1 -> TextAlign.Center; 2 -> TextAlign.End; else -> TextAlign.Start }
    val pkgKiri = p.str("pintasan_kiri")
    val pkgKanan = p.str("pintasan_kanan")
    val labelKiri = petaApp[pkgKiri]?.let { peta[it.pkg] ?: it.label } ?: "telepon"
    val labelKanan = petaApp[pkgKanan]?.let { peta[it.pkg] ?: it.label } ?: "kamera"
    var menuPkg by remember { mutableStateOf<String?>(null) }
    var grupPkg by remember { mutableStateOf<String?>(null) }

    val aksesNotif = Notif.aktif(ctx)
    val media by produceState<InfoMedia?>(null, aksesNotif) {
        while (true) {
            value = if (aksesNotif) withContext(Dispatchers.IO) { ambilMedia(ctx) } else null
            delay(2500)
        }
    }
    val agendaAktif = p.bool("agenda", false)
    val agenda by produceState<String?>(null, agendaAktif) {
        while (true) {
            value = if (agendaAktif) withContext(Dispatchers.IO) { Agenda.berikut(ctx) } else null
            delay(60_000)
        }
    }

    var sekarang by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            sekarang = LocalDateTime.now()
            delay(10_000)
        }
    }
    val kursor = rememberInfiniteTransition(label = "kursor").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "alfa",
    )

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { Peluncur.kunciLayar(ctx) },
                    onLongPress = { onPengaturan() },
                )
            }
            .pointerInput(Unit) {
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDragEnd = {
                        val mendatar = abs(total.x) > abs(total.y) * 1.5f
                        when {
                            !mendatar && total.y < -size.height * 0.10f -> onLaci()
                            !mendatar && total.y > size.height * 0.10f -> Peluncur.bukaNotifikasi(ctx)
                            // Jika digeser mendatar, ganti ruang/mode santai dan kerja
                            mendatar && total.x < -size.width * 0.25f -> p.taruh("ruang", if (p.ruang == 0) 1 else 0)
                            mendatar && total.x > size.width * 0.25f -> p.taruh("ruang", if (p.ruang == 0) 1 else 0)
                        }
                    },
                ) { _, d -> total += d }
                
            }
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = rata,
    ) {
        if (terminal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = t.aksen)) { append("hening") }
                        withStyle(SpanStyle(color = t.redup)) { append("@") }
                        withStyle(SpanStyle(color = t.aksen2)) { append(p.namaRuang(p.ruang).lowercase()) }
                        withStyle(SpanStyle(color = t.redup)) { append(":~\$ ") }
                        withStyle(SpanStyle(color = t.fg)) { append("date") }
                    },
                    fontSize = 13.sp,
                )
                Text("█", color = t.aksen, fontSize = 13.sp, modifier = Modifier.graphicsLayer { alpha = kursor.value })
            }
        }
        if (p.tampilJam) {
            Text(
                sekarang.format(DateTimeFormatter.ofPattern(if (p.jam24) "HH:mm" else "h:mm")),
                color = if (terminal) t.aksen else t.fg,
                fontSize = 58.sp,
                fontWeight = FontWeight.Light,
            )
        }
        if (p.tampilTanggal) {
            Text(
                (if (terminal) "// " else "") + sekarang.format(TANGGAL),
                color = if (terminal) t.redup else t.fg.copy(alpha = 0.7f),
                fontSize = 14.sp,
            )
        }
        val ag = agenda
        if (ag != null) {
            Text("› $ag", color = t.aksen2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(Modifier.weight(1f))

        if (terminal && favorit.isNotEmpty()) {
            Text("\$ ls favorit/", color = t.redup, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = rata) {
            favorit.forEachIndexed { i, a ->
                key(a.pkg) {
                    BarisFavorit(
                        a, i + 1, p, apps, petaApp, peta, t, terminal, ukuran, rata, teksRata,
                        Notif.data[a.pkg],
                        menuTampil = menuPkg == a.pkg,
                        grupTampil = grupPkg == a.pkg,
                        onMenu = { menuPkg = if (it) a.pkg else null },
                        onGrup = { grupPkg = if (it) a.pkg else null },
                    )
                }
            }
            if (favorit.isEmpty() && apps.isNotEmpty()) {
                Text(
                    "Geser ke atas untuk membuka daftar aplikasi.\nTekan lama sebuah aplikasi untuk menjadikannya favorit.",
                    color = t.redup,
                    fontSize = 14.sp,
                    textAlign = teksRata,
                )
            }
        }

        val m = media
        if (m != null) {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "♪ " + m.judul + (if (m.artis.isNotBlank()) " — " + m.artis else ""),
                    color = t.aksen2,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text("⏮", color = t.fg, modifier = Modifier.clickable { m.kontrol.transportControls.skipToPrevious() }.padding(8.dp))
                Text(
                    if (m.main) "⏸" else "▶",
                    color = t.fg,
                    modifier = Modifier.clickable {
                        if (m.main) m.kontrol.transportControls.pause() else m.kontrol.transportControls.play()
                    }.padding(8.dp),
                )
                Text("⏭", color = t.fg, modifier = Modifier.clickable { m.kontrol.transportControls.skipToNext() }.padding(8.dp))
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(0, 1).forEach { r ->
                    val nama = p.namaRuang(r).lowercase()
                    Text(
                        if (terminal) "~/$nama" else nama,
                        color = if (r == p.ruang) (if (terminal) t.aksen else t.fg) else t.redup,
                        fontSize = 13.sp,
                        fontWeight = if (r == p.ruang) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.clickable { p.taruh("ruang", r) }.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }
        } 
    }
}

@Composable
private fun BarisFavorit(
    a: App,
    nomor: Int,
    p: Pengaturan,
    apps: List<App>,
    petaApp: Map<String, App>,
    peta: Map<String, String>,
    t: Tema,
    terminal: Boolean,
    ukuran: TextUnit,
    rata: Alignment.Horizontal,
    teksRata: TextAlign,
    ringkas: Ringkas?,
    menuTampil: Boolean,
    grupTampil: Boolean,
    onMenu: (Boolean) -> Unit,
    onGrup: (Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    Column(horizontalAlignment = rata) {
        Box {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (terminal) {
                    Text("$nomor", color = t.aksen, fontSize = 13.sp, modifier = Modifier.width(22.dp))
                }
                Text(
                    peta[a.pkg] ?: a.label,
                    color = t.fg,
                    fontSize = ukuran,
                    textAlign = teksRata,
                    modifier = Modifier
                        .pointerInput(Unit) {
                            var dx = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dx = 0f },
                                onDragEnd = { if (dx > 60f) onGrup(true) },
                            ) { _, d -> dx += d }
                        }
                        .combinedClickable(
                            onClick = { Peluncur.buka(ctx, a, p) },
                            onLongClick = { onMenu(true) },
                        ),
                )
                if (ringkas != null) {
                    Text("  ●${ringkas.jumlah}", color = t.aksen2, fontSize = 13.sp)
                }
            }
            MenuApp(a, p, apps, menuTampil) { onMenu(false) }
            DropdownMenu(expanded = grupTampil, onDismissRequest = { onGrup(false) }) {
                val anggota = p.grup(a.pkg).mapNotNull { petaApp[it] }
                if (anggota.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Grup kosong. Tekan lama aplikasi lain, pilih Masukkan ke grup.", fontSize = 12.sp) },
                        onClick = { onGrup(false) },
                    )
                }
                anggota.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(peta[g.pkg] ?: g.label) },
                        onClick = { onGrup(false); Peluncur.buka(ctx, g, p) },
                        trailingIcon = { Text("✕", modifier = Modifier.clickable { p.toggleGrup(a.pkg, g.pkg) }.padding(8.dp)) },
                    )
                }
            }
        }
        if (ringkas != null && ringkas.teks.isNotBlank()) {
            Text(
                ringkas.teks,
                color = t.redup,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = teksRata,
                modifier = Modifier
                    .padding(start = if (terminal) 22.dp else 0.dp)
                    .combinedClickable(
                        onClick = { Peluncur.buka(ctx, a, p) },
                        onLongClick = { PendengarNotifikasi.hapus(a.pkg) },
                    ),
            )
        }
    }
}

@Composable
private fun Butir(teks: String, aksi: () -> Unit) {
    DropdownMenuItem(text = { Text(teks) }, onClick = aksi)
}

/** Menu tekan lama untuk satu aplikasi. Dipakai di beranda dan daftar aplikasi. */
@Composable
fun MenuApp(a: App, p: Pengaturan, apps: List<App>, tampil: Boolean, tutup: () -> Unit) {
    val ctx = LocalContext.current
    var ganti by remember { mutableStateOf(false) }
    var pilihGrup by remember { mutableStateOf(false) }
    val fav = a.pkg in p.favorit()

    DropdownMenu(expanded = tampil, onDismissRequest = tutup) {
        Butir(if (fav) "Hapus dari favorit" else "Jadikan favorit (${p.namaRuang(p.ruang)})") {
            tutup()
            if (!p.toggleFav(a.pkg)) Toast.makeText(ctx, "Favorit di Ruang ini sudah penuh", Toast.LENGTH_SHORT).show()
        }
        if (fav) {
            Butir("Pindah ke atas") { tutup(); p.naikFav(a.pkg) }
            Butir("Pindah ke bawah") { tutup(); p.turunFav(a.pkg) }
        }
        Butir("Masukkan ke grup favorit…") { tutup(); pilihGrup = true }
        Butir("Ganti nama") { tutup(); ganti = true }
        Butir("Sembunyikan") { tutup(); p.toggleSembunyi(a.pkg) }
        Butir("Info aplikasi") { tutup(); Peluncur.info(ctx, a) }
        Butir("Copot pemasangan") { tutup(); Peluncur.copot(ctx, a) }
    }
    if (ganti) {
        var teks by remember { mutableStateOf(p.petaNama()[a.pkg] ?: a.label) }
        AlertDialog(
            onDismissRequest = { ganti = false },
            title = { Text("Ganti nama") },
            text = { OutlinedTextField(value = teks, onValueChange = { teks = it }, singleLine = true) },
            confirmButton = { TextButton(onClick = { p.setNama(a.pkg, teks); ganti = false }) { Text("Simpan") } },
            dismissButton = { TextButton(onClick = { p.setNama(a.pkg, ""); ganti = false }) { Text("Reset") } },
        )
    }
    if (pilihGrup) {
        val petaApp = apps.associateBy { it.pkg }
        val induk = p.favorit().filter { it != a.pkg }.mapNotNull { petaApp[it] }
        AlertDialog(
            onDismissRequest = { pilihGrup = false },
            title = { Text("Masukkan ke grup") },
            text = {
                Column {
                    if (induk.isEmpty()) Text("Belum ada favorit lain di Ruang ini.")
                    induk.forEach { f ->
                        val di = a.pkg in p.grup(f.pkg)
                        Text(
                            (if (di) "✓ " else "") + (p.petaNama()[f.pkg] ?: f.label),
                            modifier = Modifier.fillMaxWidth().clickable { p.toggleGrup(f.pkg, a.pkg) }.padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pilihGrup = false }) { Text("Selesai") } },
        )
    }
}
