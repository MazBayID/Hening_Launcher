@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.hening

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class Baris(val app: App, val nama: String)

private fun hurufDari(s: String): Char {
    val c = s.firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

/**
 * Daftar aplikasi: pencarian instan, perintah gaya terminal (g, :set, :ruang, :tema, :baru, :sering, kalkulator),
 * dan penggeser alfabet dengan haptik di sisi kanan.
 */
@Composable
fun Laci(apps: List<App>, p: Pengaturan, bukaPengaturan: () -> Unit, tutup: () -> Unit) {
    val ctx = LocalContext.current
    val t = p.tema
    val terminal = p.gaya == 0
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
    val q = cari.trim()
    val aksi = remember(q) { perintah(q, ctx, p, apps, bukaPengaturan, tutup) }
    val hasil = remember(semua, q) {
        when {
            q == ":baru" -> semua.sortedByDescending { it.app.terpasang }.take(12)
            q == ":sering" -> semua.filter { p.hit(it.app.pkg) > 0 }.sortedByDescending { p.hit(it.app.pkg) }.take(12)
            q.startsWith(":") -> emptyList()
            q.isEmpty() -> semua
            else -> semua.filter { it.nama.contains(q, ignoreCase = true) }
                .sortedBy { !it.nama.startsWith(q, ignoreCase = true) }
        }
    }
    val tampil: List<Any> = remember(hasil, q) {
        if (q.isEmpty()) {
            buildList<Any> {
                var h: Char? = null
                hasil.forEach { b ->
                    val ch = hurufDari(b.nama)
                    if (ch != h) {
                        add(ch)
                        h = ch
                    }
                    add(b)
                }
            }
        } else {
            hasil
        }
    }
    val huruf = remember(semua) { semua.map { hurufDari(it.nama) }.distinct() }

    LaunchedEffect(Unit) { if (p.autoKeyboard) fokus.requestFocus() }
    LaunchedEffect(cari, hasil.size) {
        if (p.autoBuka && q.isNotEmpty() && !q.startsWith(":") && aksi.isEmpty() && hasil.size == 1) {
            delay(450)
            Peluncur.buka(ctx, hasil[0].app, p)
            tutup()
        }
    }
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }

    Box(Modifier.fillMaxSize().background(t.bg)) {
    LatarBelakang(p, t)
    Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = cari,
                onValueChange = { cari = it },
                singleLine = true,
                placeholder = { Text(if (terminal) "ketik untuk mencari  (:help)" else "Cari…") },
                leadingIcon = if (terminal) ({ Text("❯", color = t.aksen) }) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = t.fg,
                    unfocusedTextColor = t.fg,
                    cursorColor = t.aksen,
                    focusedBorderColor = t.aksen.copy(alpha = 0.6f),
                    unfocusedBorderColor = t.redup.copy(alpha = 0.5f),
                    focusedPlaceholderColor = t.redup,
                    unfocusedPlaceholderColor = t.redup,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).focusRequester(fokus),
            )
            LazyColumn(Modifier.weight(1f), state = daftar, contentPadding = PaddingValues(end = 40.dp)) {
                items(aksi, key = { "a" + it.teks }) { a ->
                    Text(
                        "❯ " + a.teks,
                        color = t.aksen2,
                        fontSize = 15.sp,
                        modifier = Modifier.fillMaxWidth().clickable { a.jalankan() }.padding(horizontal = 24.dp, vertical = 10.dp),
                    )
                }
                items(tampil, key = { if (it is Baris) it.app.pkg else "h$it" }) { x ->
                    if (x is Baris) {
                        Box {
                            Text(
                                x.nama,
                                color = t.fg,
                                fontSize = 22.sp,
                                maxLines = 1,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = { Peluncur.buka(ctx, x.app, p); tutup() },
                                        onLongClick = { menuPkg = x.app.pkg },
                                    )
                                    .padding(horizontal = 24.dp, vertical = 11.dp),
                            )
                            MenuApp(x.app, p, apps, menuPkg == x.app.pkg) { menuPkg = null }
                        }
                    } else {
                        Text(
                            if (terminal) "# $x" else "$x",
                            color = if (terminal) t.redup else t.aksen,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(start = 24.dp, top = 14.dp, bottom = 2.dp),
                        )
                    }
                }
            }
        }

        if (p.bool("alfabet", true) && q.isEmpty() && huruf.isNotEmpty()) {
            PenggeserAlfabet(
                huruf = huruf,
                warna = t.aksen,
                aktif = hurufAktif,
                haptik = p.bool("haptik", true),
                onAktif = { hurufAktif = it },
                onPilih = { ch ->
                    val i = tampil.indexOfFirst { it == ch }
                    if (i >= 0) scope.launch { daftar.scrollToItem(i + aksi.size) }
                },
                modifier = Modifier.align(Alignment.CenterEnd).padding(top = 72.dp, bottom = 8.dp),
            )
        }
        hurufAktif?.let {
            Box(
                Modifier.align(Alignment.Center).size(88.dp).background(t.aksen.copy(alpha = 0.18f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(it.toString(), color = t.aksen, fontSize = 44.sp) }
        }
    }
    }
}

/** Kolom huruf A-Z di tepi kanan. Sentuh atau seret untuk melompat, dengan getaran halus tiap pindah huruf. */
@Composable
private fun PenggeserAlfabet(
    huruf: List<Char>,
    warna: Color,
    aktif: Char?,
    haptik: Boolean,
    onAktif: (Char?) -> Unit,
    onPilih: (Char) -> Unit,
    modifier: Modifier,
) {
    val getar = LocalHapticFeedback.current
    Column(
        modifier
            .fillMaxHeight()
            .width(34.dp)
            .pointerInput(huruf) {
                awaitEachGesture {
                    var terakhir: Char? = null
                    fun pilih(y: Float) {
                        val i = ((y / size.height) * huruf.size).toInt().coerceIn(0, huruf.lastIndex)
                        val ch = huruf[i]
                        if (ch != terakhir) {
                            terakhir = ch
                            if (haptik) getar.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAktif(ch)
                            onPilih(ch)
                        }
                    }
                    val turun = awaitFirstDown()
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
        huruf.forEach {
            Text(
                it.toString(),
                color = if (it == aktif) warna else warna.copy(alpha = 0.55f),
                fontSize = if (it == aktif) 15.sp else 11.sp,
            )
        }
    }
}
