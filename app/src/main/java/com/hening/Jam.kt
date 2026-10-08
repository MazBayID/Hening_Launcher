package com.hening

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

/** Jam di beranda dengan empat gaya: 0 digital, 1 analog, 2 flip, 3 bertumpuk. */
@Composable
fun JamBeranda(waktu: LocalDateTime, p: Pengaturan, t: Tema, terminal: Boolean, rata: Alignment.Horizontal) {
    val utama = if (terminal) t.aksen else t.fg
    when (p.int("gayajam", 0)) {
        1 -> JamAnalog(waktu, t)
        2 -> JamFlip(waktu, p, t)
        3 -> Column(horizontalAlignment = rata) {
            Text(
                waktu.format(DateTimeFormatter.ofPattern(if (p.jam24) "HH" else "hh")),
                color = t.fg, fontSize = 72.sp, fontWeight = FontWeight.Light, lineHeight = 64.sp,
            )
            Text(
                waktu.format(DateTimeFormatter.ofPattern("mm")),
                color = utama, fontSize = 72.sp, fontWeight = FontWeight.Light, lineHeight = 64.sp,
            )
        }
        else -> Text(
            waktu.format(DateTimeFormatter.ofPattern(if (p.jam24) "HH:mm" else "h:mm")),
            color = utama,
            fontSize = 58.sp,
            fontWeight = FontWeight.Light,
        )
    }
}

@Composable
private fun JamAnalog(w: LocalDateTime, t: Tema) {
    Canvas(Modifier.size(140.dp)) {
        val c = center
        val r = size.minDimension / 2f - 4f
        drawCircle(t.redup, radius = r, style = Stroke(width = 3f))
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90.0)
            val dalam = if (i % 3 == 0) r * 0.80f else r * 0.88f
            drawLine(
                t.redup,
                Offset(c.x + (cos(a) * dalam).toFloat(), c.y + (sin(a) * dalam).toFloat()),
                Offset(c.x + (cos(a) * r).toFloat(), c.y + (sin(a) * r).toFloat()),
                strokeWidth = if (i % 3 == 0) 4f else 2f,
            )
        }
        val menit = w.minute + w.second / 60f
        val jam = (w.hour % 12) + menit / 60f

        fun jarum(derajat: Float, panjang: Float, tebal: Float, warna: Color) {
            val a = Math.toRadians((derajat - 90f).toDouble())
            drawLine(
                warna, c,
                Offset(c.x + (cos(a) * panjang).toFloat(), c.y + (sin(a) * panjang).toFloat()),
                strokeWidth = tebal, cap = StrokeCap.Round,
            )
        }
        jarum(jam * 30f, r * 0.5f, 7f, t.fg)
        jarum(menit * 6f, r * 0.78f, 5f, t.aksen)
        drawCircle(t.aksen, radius = 6f, center = c)
    }
}

@Composable
private fun JamFlip(w: LocalDateTime, p: Pengaturan, t: Tema) {
    val jam = w.format(DateTimeFormatter.ofPattern(if (p.jam24) "HH" else "hh"))
    val menit = w.format(DateTimeFormatter.ofPattern("mm"))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        KartuFlip(jam, t)
        Text(":", color = t.redup, fontSize = 36.sp)
        KartuFlip(menit, t)
    }
}

@Composable
private fun KartuFlip(teks: String, t: Tema) {
    Box(
        Modifier
            .background(t.fg.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(teks, color = t.aksen, fontSize = 50.sp, fontWeight = FontWeight.Medium)
        Canvas(Modifier.matchParentSize()) {
            drawLine(t.bg, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = 3f)
        }
    }
}
