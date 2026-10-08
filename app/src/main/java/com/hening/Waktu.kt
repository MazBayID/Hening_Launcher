package com.hening

import android.app.AppOpsManager
import android.content.Context
import android.app.usage.UsageStatsManager
import android.os.Build
import android.os.Process
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

/** Waktu layar per aplikasi dari UsageStatsManager. Perlu izin Usage access. */
object Waktu {
    fun izin(ctx: Context): Boolean {
        return try {
            val ops = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= 29) {
                ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            } else {
                @Suppress("DEPRECATION")
                ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /** Milidetik di layar hari ini per paket. Kosong bila izin belum diberikan. */
    fun hariIni(ctx: Context): Map<String, Long> {
        if (!izin(ctx)) return emptyMap()
        return try {
            val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val awal = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            usm.queryAndAggregateUsageStats(awal, System.currentTimeMillis())
                .mapValues { it.value.totalTimeInForeground }
                .filter { it.value > 0L }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun format(ms: Long): String {
        val menit = ms / 60_000L
        return if (menit >= 60) "${menit / 60}j ${menit % 60}m" else "${menit}m"
    }
}

/**
 * Jeda sadar: aplikasi yang dipantau (atau punya batas harian) tidak langsung terbuka,
 * tetapi menampilkan layar jeda dulu. Peluncur.buka memeriksanya.
 */
object Jeda {
    var app by mutableStateOf<App?>(null)
    var lolos: String? = null

    fun perlu(p: Pengaturan, pkg: String): Boolean =
        lolos != pkg && (p.bool("pantau:$pkg", false) || p.int("batas:$pkg", 0) > 0)
}

@Composable
fun LayarJeda(app: App, p: Pengaturan) {
    val ctx = LocalContext.current
    val t = p.tema
    val nama = p.petaNama()[app.pkg] ?: app.label
    val pakai = remember(app.pkg) { if (Waktu.izin(ctx)) (Waktu.hariIni(ctx)[app.pkg] ?: 0L) else -1L }
    val batas = p.int("batas:${app.pkg}", 0)
    val lewat = batas > 0 && pakai >= batas * 60_000L
    val total = p.int("jedadetik", 5) * (if (lewat) 2 else 1)
    var sisa by remember(app.pkg) { mutableIntStateOf(total) }
    LaunchedEffect(app.pkg) {
        while (sisa > 0) {
            delay(1000)
            sisa--
        }
    }
    val napas = rememberInfiniteTransition(label = "napas").animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(2500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "skala",
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(t.bg)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Box(
                Modifier
                    .size(120.dp)
                    .graphicsLayer { scaleX = napas.value; scaleY = napas.value }
                    .background(t.aksen.copy(alpha = 0.25f), CircleShape)
            )
            Text("napas dulu…", color = t.aksen, fontSize = 22.sp)
            Text("Mau membuka $nama?", color = t.fg, fontSize = 18.sp, textAlign = TextAlign.Center)
            if (pakai >= 0) {
                val tambahan = if (batas > 0) " dari batas $batas menit" else ""
                Text(
                    "Hari ini kamu sudah memakainya ${Waktu.format(pakai)}$tambahan.",
                    color = if (lewat) t.aksen2 else t.redup,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { Jeda.app = null }) { Text("Tidak jadi") }
                Button(
                    onClick = {
                        Jeda.lolos = app.pkg
                        Jeda.app = null
                        Peluncur.buka(ctx, app, p)
                    },
                    enabled = sisa == 0,
                ) { Text(if (sisa > 0) "Tetap buka ($sisa)" else "Tetap buka") }
            }
        }
    }
}
