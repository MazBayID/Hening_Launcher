@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.hening

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val TANGGAL = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))

/**
 * Beranda polos: jam, daftar favorit sebagai teks, dan pergantian Ruang.
 * Geser atas = daftar aplikasi, geser bawah = notifikasi,
 * geser kiri dan kanan = pintasan, tekan lama = pengaturan.
 */
@Composable
fun Beranda(apps: List<App>, p: Pengaturan, onLaci: () -> Unit, onPengaturan: () -> Unit) {
    val ctx = LocalContext.current
    val tema = p.tema
    val peta = remember(p.str("nama")) { p.petaNama() }
    val petaApp = remember(apps) { apps.associateBy { it.pkg } }
    val favorit = p.favorit().mapNotNull { petaApp[it] }.take(p.jumlahFav)
    val ukuran = when (p.ukuran) { 0 -> 24.sp; 2 -> 36.sp; else -> 30.sp }
    val rata = when (p.rata) { 1 -> Alignment.CenterHorizontally; 2 -> Alignment.End; else -> Alignment.Start }
    val teksRata = when (p.rata) { 1 -> TextAlign.Center; 2 -> TextAlign.End; else -> TextAlign.Start }
    val pkgKiri = p.str("pintasan_kiri")
    val pkgKanan = p.str("pintasan_kanan")
    val labelKiri = petaApp[pkgKiri]?.let { peta[it.pkg] ?: it.label } ?: "Telepon"
    val labelKanan = petaApp[pkgKanan]?.let { peta[it.pkg] ?: it.label } ?: "Kamera"
    var menuPkg by remember { mutableStateOf<String?>(null) }

    var sekarang by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            sekarang = LocalDateTime.now()
            delay(10_000)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onPengaturan() }) }
            .pointerInput(Unit) {
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDragEnd = {
                        val mendatar = abs(total.x) > abs(total.y) * 1.5f
                        when {
                            !mendatar && total.y < -size.height * 0.10f -> onLaci()
                            !mendatar && total.y > size.height * 0.10f -> Peluncur.bukaNotifikasi(ctx)
                            mendatar && total.x < -size.width * 0.25f -> Peluncur.pintasan(ctx, apps, pkgKanan, false)
                            mendatar && total.x > size.width * 0.25f -> Peluncur.pintasan(ctx, apps, pkgKiri, true)
                        }
                    },
                ) { _, d -> total += d }
            }
            .padding(horizontal = 28.dp, vertical = 20.dp),
        horizontalAlignment = rata,
    ) {
        if (p.tampilJam) {
            Text(
                sekarang.format(DateTimeFormatter.ofPattern(if (p.jam24) "HH:mm" else "h:mm")),
                color = tema.fg,
                fontSize = 64.sp,
                fontWeight = FontWeight.Light,
            )
        }
        if (p.tampilTanggal) {
            Text(sekarang.format(TANGGAL), color = tema.fg.copy(alpha = 0.7f), fontSize = 14.sp)
        }

        Spacer(Modifier.weight(1f))

        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = rata) {
            favorit.forEach { a ->
                key(a.pkg) {
                    Box {
                        Text(
                            peta[a.pkg] ?: a.label,
                            color = tema.fg,
                            fontSize = ukuran,
                            textAlign = teksRata,
                            modifier = Modifier.combinedClickable(
                                onClick = { Peluncur.buka(ctx, a) },
                                onLongClick = { menuPkg = a.pkg },
                            ),
                        )
                        MenuApp(a, p, menuPkg == a.pkg) { menuPkg = null }
                    }
                }
            }
            if (favorit.isEmpty() && apps.isNotEmpty()) {
                Text(
                    "Geser ke atas untuk membuka daftar aplikasi.\nTekan lama sebuah aplikasi untuk menjadikannya favorit.",
                    color = tema.fg.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = teksRata,
                )
            }
        }

        Spacer(Modifier.weight(1f))

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                labelKiri,
                color = tema.fg.copy(alpha = 0.8f),
                fontSize = 15.sp,
                maxLines = 1,
                modifier = Modifier.clickable { Peluncur.pintasan(ctx, apps, pkgKiri, true) }.padding(vertical = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(0, 1).forEach { r ->
                    Text(
                        p.namaRuang(r),
                        color = if (r == p.ruang) tema.fg else tema.fg.copy(alpha = 0.35f),
                        fontSize = 14.sp,
                        fontWeight = if (r == p.ruang) FontWeight.Medium else FontWeight.Normal,
                        modifier = Modifier.clickable { p.taruh("ruang", r) }.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            Text(
                labelKanan,
                color = tema.fg.copy(alpha = 0.8f),
                fontSize = 15.sp,
                maxLines = 1,
                modifier = Modifier.clickable { Peluncur.pintasan(ctx, apps, pkgKanan, false) }.padding(vertical = 8.dp),
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
fun MenuApp(a: App, p: Pengaturan, tampil: Boolean, tutup: () -> Unit) {
    val ctx = LocalContext.current
    var ganti by remember { mutableStateOf(false) }
    val fav = a.pkg in p.favorit()

    DropdownMenu(expanded = tampil, onDismissRequest = tutup) {
        Butir(if (fav) "Hapus dari favorit" else "Jadikan favorit (${p.namaRuang(p.ruang)})") {
            tutup()
            if (!p.toggleFav(a.pkg)) Toast.makeText(ctx, "Favorit di Ruang ini sudah penuh", Toast.LENGTH_SHORT).show()
        }
        if (fav) Butir("Pindah ke atas") { tutup(); p.naikFav(a.pkg) }
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
}
