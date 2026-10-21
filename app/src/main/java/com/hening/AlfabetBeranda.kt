package com.hening

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

private fun abjadDari(s: String): Char {
    val c = s.firstOrNull()?.uppercaseChar() ?: '#'
    return if (c in 'A'..'Z') c else '#'
}

/**
 * Abjad di tepi kanan beranda dengan gelombang melengkung. Sentuh: huruf di bawah jari menggelombang ke tengah
 * dengan pegas (ada sedikit pantulan), getaran halus terasa tiap pindah huruf, dan daftar aplikasi berawalan huruf
 * itu muncul di kiri. Geser jari ke kiri lalu lepas pada sebuah aplikasi untuk membukanya.
 */
@Composable
fun AlfabetBeranda(apps: List<App>, p: Pengaturan, modifier: Modifier = Modifier, onGestur: (String) -> Unit = {}) {
    val ctx = LocalContext.current
    val t = p.tema
    val view = LocalView.current
    val d = LocalDensity.current
    val peta = remember(p.str("nama")) { p.petaNama() }
    val sembunyi = p.tersembunyi
    val urut = remember(apps, peta, sembunyi) {
        apps.filter { it.pkg !in sembunyi }
            .map { it to (peta[it.pkg] ?: it.label) }
            .sortedBy { it.second.lowercase() }
    }
    val huruf = remember(urut) { urut.map { abjadDari(it.second) }.distinct() }
    val getarAktif = p.bool("haptik", true)

    var tinggi by remember { mutableFloatStateOf(0f) }
    var jariY by remember { mutableFloatStateOf(0f) }
    var aktif by remember { mutableIntStateOf(-1) }
    var pilih by remember { mutableIntStateOf(-1) }
    var jumlahPanel by remember { mutableIntStateOf(0) }
    var panelAtas by remember { mutableFloatStateOf(0f) }
    var menyentuh by remember { mutableStateOf(false) }
    var ketukTerakhir by remember { mutableLongStateOf(0L) }

    // Gelombang digerakkan pegas: amplitudo naik dengan sedikit pantulan, posisi puncak mengikuti jari dengan jeda halus.
    val amplitudo by animateFloatAsState(
        targetValue = if (menyentuh) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "amplitudo",
    )
    val puncakY by animateFloatAsState(
        targetValue = jariY,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium),
        label = "puncak",
    )

    val itemPx = with(d) { 44.dp.toPx() }
    val padPx = with(d) { 6.dp.toPx() }
    val geserMaks = with(d) { 72.dp.toPx() }
    val radius = with(d) { 110.dp.toPx() }
    val ambangPanel = with(d) { 28.dp.toPx() }
    val ambangGeser = with(d) { 20.dp.toPx() }
    val maksItem = (tinggi / itemPx).toInt().coerceIn(1, 12)

    val isiPanel = if (aktif in huruf.indices) {
        urut.filter { abjadDari(it.second) == huruf[aktif] }.take(maksItem)
    } else {
        emptyList()
    }

    Box(modifier.fillMaxWidth().fillMaxHeight(0.72f).onSizeChanged { tinggi = it.height.toFloat() }) {
        AnimatedVisibility(
            visible = menyentuh && isiPanel.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 64.dp)
                .offset { IntOffset(0, panelAtas.roundToInt()) },
            enter = fadeIn(spring()) + scaleIn(
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                initialScale = 0.8f,
                transformOrigin = TransformOrigin(1f, 0.5f),
            ),
            exit = fadeOut(spring()) + scaleOut(targetScale = 0.9f, transformOrigin = TransformOrigin(1f, 0.5f)),
        ) {
            Column(
                Modifier
                    .background(t.bg.copy(alpha = 0.9f), RoundedCornerShape(14.dp))
                    .padding(vertical = 6.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                isiPanel.forEachIndexed { i, (_, nama) ->
                    Box(Modifier.height(44.dp), contentAlignment = Alignment.CenterEnd) {
                        Text(nama, color = if (i == pilih) t.aksen else t.fg, fontSize = 22.sp, maxLines = 1)
                    }
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(40.dp)
                .pointerInput(huruf, urut) {
                    awaitEachGesture {
                        val turun = awaitFirstDown()
                        menyentuh = true
                        val mulai = System.currentTimeMillis()
                        val y0 = turun.position.y
                        jariY = y0
                        var bergeser = false
                        var terakhir = -1
                        var terakhirPilih = -1
                        var dalamPanel = false

                        fun proses(pos: Offset) {
                            jariY = pos.y
                            if (abs(pos.y - y0) > ambangGeser) bergeser = true
                            val n = huruf.size
                            if (n == 0) return
                            if (pos.x < -ambangPanel) dalamPanel = true
                            if (dalamPanel) bergeser = true
                            if (pos.x >= -ambangPanel * 0.5f) dalamPanel = false
                            if (!dalamPanel) {
                                val i = ((pos.y / size.height) * n).toInt().coerceIn(0, n - 1)
                                if (i != terakhir) {
                                    terakhir = i
                                    aktif = i
                                    pilih = -1
                                    terakhirPilih = -1
                                    if (getarAktif) Getar.tick(view)
                                    val jumlah = urut.count { abjadDari(it.second) == huruf[i] }
                                        .coerceAtMost((tinggi / itemPx).toInt().coerceIn(1, 12))
                                    jumlahPanel = jumlah
                                    val tinggiPanel = jumlah * itemPx + 2 * padPx
                                    panelAtas = (pos.y - tinggiPanel / 2f).coerceIn(0f, maxOf(0f, tinggi - tinggiPanel))
                                }
                            } else {
                                val idx = floor((pos.y - panelAtas - padPx) / itemPx).toInt()
                                pilih = if (idx in 0 until jumlahPanel) idx else -1
                                if (pilih != terakhirPilih) {
                                    terakhirPilih = pilih
                                    if (pilih >= 0 && getarAktif) Getar.tick(view)
                                }
                            }
                        }

                        proses(turun.position)
                        do {
                            val e = awaitPointerEvent()
                            e.changes.forEach { proses(it.position); it.consume() }
                        } while (e.changes.any { it.pressed })

                        val dipilih = if (dalamPanel && pilih >= 0 && aktif in huruf.indices) {
                            urut.filter { abjadDari(it.second) == huruf[aktif] }.getOrNull(pilih)
                        } else {
                            null
                        }
                        menyentuh = false
                        aktif = -1
                        pilih = -1
                        if (dipilih != null) {
                            if (getarAktif) Getar.konfirmasi(view)
                            Peluncur.buka(ctx, dipilih.first, p)
                        }
                        val sekarang = System.currentTimeMillis()
                        if (!bergeser && sekarang - mulai < 300L) {
                            if (sekarang - ketukTerakhir < 450L) {
                                ketukTerakhir = 0L
                                onGestur("abjad")
                            } else {
                                ketukTerakhir = sekarang
                            }
                        }
                    }
                },
        ) {
            huruf.forEachIndexed { i, h ->
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        h.toString(),
                        fontSize = 11.sp,
                        color = if (i == aktif) t.aksen else t.redup,
                        modifier = Modifier.graphicsLayer {
                            val pusat = (i + 0.5f) * (tinggi / huruf.size)
                            val jarak = abs(puncakY - pusat) / radius
                            // Profil gelombang kosinus: 1 di bawah jari, 0 pada jarak satu radius.
                            val f = if (jarak >= 1f) 0f else (0.5f * (1f + cos(PI * jarak))).toFloat()
                            val k = f * amplitudo.coerceIn(0f, 1.3f)
                            translationX = -geserMaks * k
                            scaleX = 1f + 0.9f * k
                            scaleY = 1f + 0.9f * k
                            alpha = (0.55f + 0.45f * k).coerceIn(0f, 1f)
                        },
                    )
                }
            }
        }
    }
}
