@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.hening

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Baris(val app: App, val nama: String)

private fun hurufDari(s: String): Char {
    val c = s.firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

/**
 * Daftar aplikasi: pencarian instan (buka otomatis bila hanya satu hasil)
 * dan penggeser alfabet di sisi kanan.
 */
@Composable
fun Laci(apps: List<App>, p: Pengaturan, tutup: () -> Unit) {
    val ctx = LocalContext.current
    val tema = p.tema
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    val peta = remember(p.str("nama")) { p.petaNama() }
    val sembunyi = p.tersembunyi
    var cari by remember { mutableStateOf("") }
    var hurufAktif by remember { mutableStateOf<Char?>(null) }
    var menuPkg by remember { mutableStateOf<String?>(null) }
    val fokus = remember { FocusRequester() }
    val daftar = rememberLazyListState()

    val semua = remember(apps, peta, sembunyi) {
        apps.filter { it.pkg !in sembunyi }
            .map { Baris(it, peta[it.pkg] ?: it.label) }
            .sortedBy { it.nama.lowercase() }
    }
    val hasil = remember(semua, cari) {
        val q = cari.trim()
        if (q.isEmpty()) semua
        else semua.filter { it.nama.contains(q, ignoreCase = true) }
            .sortedBy { !it.nama.startsWith(q, ignoreCase = true) }
    }
    val huruf = remember(semua) { semua.map { hurufDari(it.nama) }.distinct() }

    LaunchedEffect(Unit) { if (p.autoKeyboard) fokus.requestFocus() }
    LaunchedEffect(cari, hasil.size) {
        if (p.autoBuka && cari.isNotBlank() && hasil.size == 1) {
            delay(450)
            Peluncur.buka(ctx, hasil[0].app)
            tutup()
        }
    }
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }

    Box(
        Modifier
            .fillMaxSize()
            .background(tema.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = cari,
                onValueChange = { cari = it },
                singleLine = true,
                placeholder = { Text("Cari…") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = tema.fg,
                    unfocusedTextColor = tema.fg,
                    cursorColor = tema.fg,
                    focusedBorderColor = tema.fg.copy(alpha = 0.5f),
                    unfocusedBorderColor = tema.fg.copy(alpha = 0.2f),
                    focusedPlaceholderColor = tema.fg.copy(alpha = 0.5f),
                    unfocusedPlaceholderColor = tema.fg.copy(alpha = 0.5f),
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp).focusRequester(fokus),
            )
            LazyColumn(Modifier.weight(1f), state = daftar, contentPadding = PaddingValues(end = 40.dp)) {
                items(hasil, key = { it.app.pkg }) { b ->
                    Box {
                        Text(
                            b.nama,
                            color = tema.fg,
                            fontSize = 22.sp,
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = { Peluncur.buka(ctx, b.app); tutup() },
                                    onLongClick = { menuPkg = b.app.pkg },
                                )
                                .padding(horizontal = 28.dp, vertical = 12.dp),
                        )
                        MenuApp(b.app, p, menuPkg == b.app.pkg) { menuPkg = null }
                    }
                }
            }
        }

        if (cari.isBlank() && huruf.isNotEmpty()) {
            PenggeserAlfabet(
                huruf = huruf,
                warna = tema.fg,
                onAktif = { hurufAktif = it },
                onPilih = { ch ->
                    val i = semua.indexOfFirst { hurufDari(it.nama) == ch }
                    if (i >= 0) scope.launch { daftar.scrollToItem(i) }
                },
                modifier = Modifier.align(Alignment.CenterEnd).padding(top = 72.dp, bottom = 8.dp),
            )
        }
        hurufAktif?.let {
            Box(
                Modifier.align(Alignment.Center).size(88.dp).background(tema.fg.copy(alpha = 0.15f), androidx.compose.foundation.shape.RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(it.toString(), color = tema.fg, fontSize = 44.sp) }
        }
    }
}

/** Kolom huruf A-Z di tepi kanan. Sentuh atau seret untuk melompat ke huruf itu. */
@Composable
private fun PenggeserAlfabet(
    huruf: List<Char>,
    warna: Color,
    onAktif: (Char?) -> Unit,
    onPilih: (Char) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxHeight()
            .width(34.dp)
            .pointerInput(huruf) {
                awaitEachGesture {
                    val turun = awaitFirstDown()
                    fun pilih(y: Float) {
                        val i = ((y / size.height) * huruf.size).toInt().coerceIn(0, huruf.lastIndex)
                        onAktif(huruf[i])
                        onPilih(huruf[i])
                    }
                    pilih(turun.position.y)
                    do {
                        val e = awaitPointerEvent()
                        e.changes.forEach { pilih(it.position.y); it.consume() }
                    } while (e.changes.any { it.pressed })
                    onAktif(null)
                }
            },
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        huruf.forEach { Text(it.toString(), color = warna.copy(alpha = 0.7f), fontSize = 11.sp) }
    }
}
