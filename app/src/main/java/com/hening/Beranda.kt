@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.hening

import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
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

private val TANGGAL = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))

/** "baru", "5m", "2j", "3h": umur notifikasi. */
private fun umurNotif(waktu: Long): String {
    val menit = ((System.currentTimeMillis() - waktu) / 60_000L).coerceAtLeast(0L)
    return when {
        menit < 1 -> "baru"
        menit < 60 -> "${menit}m"
        menit < 1440 -> "${menit / 60}j"
        else -> "${menit / 1440}h"
    }
}

/**
 * Beranda: Smart Bar kontekstual (jam, agenda, atau media), favorit sebagai teks dengan pratinjau notifikasi,
 * dan abjad bergelombang di sisi kanan. Daftar favorit bergerak dengan pegas.
 * Geser kanan pada favorit membuka popup pintasan, widget, dan grup.
 */
@Composable
fun Beranda(apps: List<App>, p: Pengaturan, onLaci: () -> Unit, onPengaturan: () -> Unit) {
    val ctx = LocalContext.current
    val t = p.tema
    val terminal = p.gaya == 0
    val smart = p.bool("smartbar", true)
    val barisNotif = p.int("notifbaris", 2)
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
    var popupPkg by remember { mutableStateOf<String?>(null) }

    val aksesNotif = Notif.aktif(ctx)
    val media by produceState<InfoMedia?>(null, aksesNotif) {
        while (true) {
            value = if (aksesNotif) withContext(Dispatchers.IO) { ambilMedia(ctx) } else null
            delay(2500)
        }
    }
    val agendaAktif = p.bool("agenda", false)
    val acara by produceState<Acara?>(null, agendaAktif) {
        while (true) {
            value = if (agendaAktif) withContext(Dispatchers.IO) { Agenda.berikut(ctx) } else null
            delay(60_000)
        }
    }

    val tampilWaktu = p.bool("tampilwaktu", false)
    val waktuLayar by produceState(0L, tampilWaktu, apps) {
        while (true) {
            value = if (tampilWaktu && Waktu.izin(ctx)) {
                withContext(Dispatchers.IO) { Waktu.hariIni(ctx).filterKeys { k -> k in petaApp }.values.sum() }
            } else {
                0L
            }
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

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .pointerInput(apps) {
                    detectTapGestures(
                        onDoubleTap = { Gestur.jalankan("dobel", ctx, p, apps, onLaci, onPengaturan) },
                        onLongPress = { Gestur.jalankan("tekan", ctx, p, apps, onLaci, onPengaturan) },
                    )
                }
                .pointerInput(apps) {
                    var total = Offset.Zero
                    detectDragGestures(
                        onDragStart = { total = Offset.Zero },
                        onDragEnd = {
                            val mendatar = abs(total.x) > abs(total.y) * 1.5f
                            when {
                                !mendatar && total.y < -size.height * 0.10f -> Gestur.jalankan("atas", ctx, p, apps, onLaci, onPengaturan)
                                !mendatar && total.y > size.height * 0.10f -> Gestur.jalankan("bawah", ctx, p, apps, onLaci, onPengaturan)
                                mendatar && total.x < -size.width * 0.25f -> Gestur.jalankan("kiri", ctx, p, apps, onLaci, onPengaturan)
                                mendatar && total.x > size.width * 0.25f -> Gestur.jalankan("kanan", ctx, p, apps, onLaci, onPengaturan)
                            }
                        },
                    ) { _, d -> total += d }
                }
                .padding(start = 24.dp, end = if (p.bool("alfabetberanda", true)) 44.dp else 24.dp, top = 20.dp, bottom = 20.dp),
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
            if (smart) {
                SmartBar(sekarang, p, t, terminal, rata, teksRata, acara, media)
            } else {
                if (p.tampilJam) JamBeranda(sekarang, p, t, terminal, rata)
                if (p.tampilTanggal) {
                    Text(
                        (if (terminal) "// " else "") + sekarang.format(TANGGAL),
                        color = if (terminal) t.redup else t.fg.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                    )
                }
            }
            val layar = waktuLayar
            if (layar > 0L) {
                Text("◷ layar hari ini: " + Waktu.format(layar), color = t.redup, fontSize = 12.sp)
            }
            val ac = acara
            if (!smart && ac != null) {
                Text("› " + Agenda.teks(ac), color = t.aksen2, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(Modifier.weight(1f))

            if (terminal && favorit.isNotEmpty()) {
                Text("\$ ls favorit/", color = t.redup, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
            }
            LazyColumn(
                Modifier.fillMaxWidth(),
                userScrollEnabled = false,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = rata,
            ) {
                itemsIndexed(favorit, key = { _, a -> a.pkg }) { i, a ->
                    Box(
                        Modifier.fillMaxWidth().animateItem(
                            fadeInSpec = spring<Float>(stiffness = Spring.StiffnessMediumLow),
                            placementSpec = spring<IntOffset>(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = spring<Float>(stiffness = Spring.StiffnessMedium),
                        ),
                    ) {
                        BarisFavorit(
                            a, i + 1, p, apps, peta, t, terminal, ukuran, rata, teksRata,
                            Notif.data[a.pkg], barisNotif,
                            menuTampil = menuPkg == a.pkg,
                            onMenu = { menuPkg = if (it) a.pkg else null },
                            onPopup = { popupPkg = a.pkg },
                        )
                    }
                }
                if (favorit.isEmpty() && apps.isNotEmpty()) {
                    item {
                        Text(
                            "Geser ke atas untuk membuka daftar aplikasi.\nTekan lama sebuah aplikasi untuk menjadikannya favorit.",
                            color = t.redup,
                            fontSize = 14.sp,
                            textAlign = teksRata,
                        )
                    }
                }
            }

            val m = media
            if (!smart && m != null) {
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (terminal) "[$labelKiri]" else labelKiri,
                    color = if (terminal) t.aksen2 else t.fg.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.clickable { Peluncur.pintasan(ctx, apps, pkgKiri, true, p) }.padding(vertical = 8.dp),
                )
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
                Text(
                    if (terminal) "[$labelKanan]" else labelKanan,
                    color = if (terminal) t.aksen2 else t.fg.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1,
                    modifier = Modifier.clickable { Peluncur.pintasan(ctx, apps, pkgKanan, false, p) }.padding(vertical = 8.dp),
                )
            }
        }
        if (p.bool("alfabetberanda", true)) {
            AlfabetBeranda(
                apps, p,
                Modifier.align(Alignment.CenterEnd).statusBarsPadding().navigationBarsPadding(),
                onGestur = { Gestur.jalankan(it, ctx, p, apps, onLaci, onPengaturan) },
            )
        }
    }

    popupPkg?.let { pkg ->
        val app = petaApp[pkg]
        if (app != null) PopupFavorit(app, p, apps) { popupPkg = null } else popupPkg = null
    }
}

/** Satu favorit: nomor, nama, lencana jumlah, lalu pratinjau notifikasi terbaru di bawahnya. */
@Composable
private fun BarisFavorit(
    a: App,
    nomor: Int,
    p: Pengaturan,
    apps: List<App>,
    peta: Map<String, String>,
    t: Tema,
    terminal: Boolean,
    ukuran: TextUnit,
    rata: Alignment.Horizontal,
    teksRata: TextAlign,
    ringkas: Ringkas?,
    barisNotif: Int,
    menuTampil: Boolean,
    onMenu: (Boolean) -> Unit,
    onPopup: () -> Unit,
) {
    val ctx = LocalContext.current
    val view = LocalView.current
    Column(Modifier.fillMaxWidth(), horizontalAlignment = rata) {
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
                                onDragEnd = {
                                    if (dx > 60f) {
                                        Getar.konfirmasi(view)
                                        onPopup()
                                    }
                                },
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
        }
        if (ringkas != null && barisNotif > 0) {
            ringkas.item.take(barisNotif).forEach { n ->
                PratinjauNotif(n, t, terminal, { Peluncur.buka(ctx, a, p) }, { PendengarNotifikasi.hapusKunci(n.kunci) })
            }
            val sisa = ringkas.jumlah - minOf(barisNotif, ringkas.item.size)
            if (sisa > 0) {
                Text(
                    "+$sisa lagi",
                    color = t.redup,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = if (terminal) 22.dp else 0.dp),
                )
            }
        }
    }
}

/** Satu baris pratinjau: pengirim (tebal) dan isi, ditambah umur notifikasi. Tekan lama untuk menghapusnya. */
@Composable
private fun PratinjauNotif(n: ItemNotif, t: Tema, terminal: Boolean, onBuka: () -> Unit, onHapus: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = if (terminal) 22.dp else 0.dp, top = 2.dp)
            .combinedClickable(onClick = onBuka, onLongClick = onHapus),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = t.aksen2, fontWeight = FontWeight.Medium)) { append(n.judul) }
                if (n.isi.isNotBlank()) withStyle(SpanStyle(color = t.redup)) { append("  " + n.isi) }
            },
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Text("  " + umurNotif(n.waktu), color = t.redup, fontSize = 10.sp)
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
    var dialogBatas by remember { mutableStateOf(false) }
    val fav = a.pkg in p.favorit()
    val pantau = p.bool("pantau:${a.pkg}", false)

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
        if (Waktu.izin(ctx)) {
            DropdownMenuItem(
                text = { Text("Hari ini: " + Waktu.format(Waktu.hariIni(ctx)[a.pkg] ?: 0L)) },
                onClick = {},
                enabled = false,
            )
        }
        Butir(if (pantau) "Jeda sadar: aktif (ketuk untuk matikan)" else "Aktifkan jeda sadar") { tutup(); p.taruh("pantau:${a.pkg}", !pantau) }
        Butir("Batas harian…") { tutup(); dialogBatas = true }
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
    if (dialogBatas) {
        var menit by remember { mutableStateOf(p.int("batas:${a.pkg}", 0).toString()) }
        AlertDialog(
            onDismissRequest = { dialogBatas = false },
            title = { Text("Batas harian") },
            text = {
                Column {
                    OutlinedTextField(
                        value = menit,
                        onValueChange = { menit = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Menit per hari (0 = tanpa batas)") },
                        singleLine = true,
                    )
                    Text(
                        "Bila batas tercapai, layar jeda sadar tampil lebih lama sebelum aplikasi terbuka.",
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { p.taruh("batas:${a.pkg}", menit.toIntOrNull() ?: 0); dialogBatas = false }) { Text("Simpan") }
            },
            dismissButton = { TextButton(onClick = { dialogBatas = false }) { Text("Batal") } },
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
