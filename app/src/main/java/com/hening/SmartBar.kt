package com.hening

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class Konteks { JAM, AGENDA, MEDIA }

private val TANGGAL_BAR = DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("id", "ID"))

/**
 * Bar kontekstual di bagian atas beranda. Berganti otomatis:
 * media yang sedang diputar, lalu acara dalam 90 menit ke depan, selain itu jam.
 * Ketuk untuk berpindah manual (kembali otomatis setelah 20 detik).
 */
@Composable
fun SmartBar(
    waktu: LocalDateTime,
    p: Pengaturan,
    t: Tema,
    terminal: Boolean,
    rata: Alignment.Horizontal,
    teksRata: TextAlign,
    acara: Acara?,
    media: InfoMedia?,
) {
    val view = LocalView.current
    val sekarangMs = remember(waktu) { System.currentTimeMillis() }
    val agendaDekat = acara != null && (acara.mulai - sekarangMs) in (-15L * 60_000L)..(90L * 60_000L)
    val tersedia = buildList {
        add(Konteks.JAM)
        if (agendaDekat) add(Konteks.AGENDA)
        if (media != null) add(Konteks.MEDIA)
    }
    val otomatis = when {
        media != null && media.main -> Konteks.MEDIA
        agendaDekat -> Konteks.AGENDA
        else -> Konteks.JAM
    }
    var manual by remember { mutableStateOf<Konteks?>(null) }
    LaunchedEffect(manual) {
        if (manual != null) {
            delay(20_000)
            manual = null
        }
    }
    val aktif = manual?.takeIf { it in tersedia } ?: otomatis

    Column(
        Modifier
            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))
            .pointerInput(tersedia, aktif) {
                detectTapGestures(onTap = {
                    if (tersedia.size > 1) {
                        manual = tersedia[(tersedia.indexOf(aktif) + 1) % tersedia.size]
                        Getar.tick(view)
                    }
                })
            },
        horizontalAlignment = rata,
    ) {
        Crossfade(targetState = aktif, animationSpec = spring(stiffness = Spring.StiffnessLow), label = "smartbar") { k ->
            when (k) {
                Konteks.JAM -> Column(horizontalAlignment = rata) {
                    if (p.tampilJam) JamBeranda(waktu, p, t, terminal, rata)
                    if (p.tampilTanggal) {
                        Text(
                            (if (terminal) "// " else "") + waktu.format(TANGGAL_BAR),
                            color = if (terminal) t.redup else t.fg.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                        )
                    }
                }
                Konteks.AGENDA -> KonteksAgenda(acara, t, terminal, rata, teksRata)
                Konteks.MEDIA -> KonteksMedia(media, t, terminal, rata, teksRata)
            }
        }
        if (tersedia.size > 1) {
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tersedia.forEach { k ->
                    Box(Modifier.size(6.dp).background(if (k == aktif) t.aksen else t.redup, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun KonteksAgenda(a: Acara?, t: Tema, terminal: Boolean, rata: Alignment.Horizontal, teksRata: TextAlign) {
    if (a == null) return
    val menit = (a.mulai - System.currentTimeMillis()) / 60_000L
    val jam = Instant.ofEpochMilli(a.mulai).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    val ket = when {
        menit > 60 -> "dalam ${menit / 60}j ${menit % 60}m"
        menit > 0 -> "dalam $menit menit"
        menit > -1 -> "sekarang"
        else -> "berlangsung ${-menit} menit"
    }
    Column(horizontalAlignment = rata) {
        Text(if (terminal) "\$ next-event" else "Berikutnya", color = t.redup, fontSize = 12.sp)
        Text(a.judul, color = t.aksen, fontSize = 28.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = teksRata)
        Text("$jam · $ket", color = t.aksen2, fontSize = 14.sp)
    }
}

@Composable
private fun KonteksMedia(m: InfoMedia?, t: Tema, terminal: Boolean, rata: Alignment.Horizontal, teksRata: TextAlign) {
    if (m == null) return
    Column(horizontalAlignment = rata) {
        Text(if (terminal) "\$ now-playing" else "Sedang diputar", color = t.redup, fontSize = 12.sp)
        Text("♪ " + m.judul, color = t.aksen, fontSize = 24.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = teksRata)
        if (m.artis.isNotBlank()) Text(m.artis, color = t.redup, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("⏮", color = t.fg, fontSize = 24.sp, modifier = Modifier.clickable { m.kontrol.transportControls.skipToPrevious() }.padding(10.dp))
            Text(
                if (m.main) "⏸" else "▶",
                color = t.fg,
                fontSize = 24.sp,
                modifier = Modifier.clickable {
                    if (m.main) m.kontrol.transportControls.pause() else m.kontrol.transportControls.play()
                }.padding(10.dp),
            )
            Text("⏭", color = t.fg, fontSize = 24.sp, modifier = Modifier.clickable { m.kontrol.transportControls.skipToNext() }.padding(10.dp))
        }
    }
}
