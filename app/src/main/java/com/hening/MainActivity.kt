package com.hening

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var versi by mutableIntStateOf(0)
    private var sinyalHome by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transparan = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = transparan, navigationBarStyle = transparan)
        val pengaturan = Pengaturan(applicationContext)
        setContent { Root(pengaturan, versi, sinyalHome) }
    }

    override fun onResume() {
        super.onResume()
        versi++ // muat ulang daftar aplikasi
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        sinyalHome++ // tombol Home ditekan lagi: kembali ke beranda
    }
}

@Composable
fun Root(p: Pengaturan, versi: Int, sinyalHome: Int) {
    val ctx = LocalContext.current
    var apps by remember { mutableStateOf<List<App>>(emptyList()) }
    var laci by remember { mutableStateOf(false) }
    var pengaturan by remember { mutableStateOf(false) }

    LaunchedEffect(versi) {
        apps = withContext(Dispatchers.IO) { Aplikasi.muat(ctx) }
        p.isiAwal(apps.map { it.pkg }.toSet())
    }
    LaunchedEffect(sinyalHome) { laci = false; pengaturan = false }
    LaunchedEffect(p.int("foto_versi", 0), p.int("wall", 0)) {
        LatarCache.bitmap = if (p.int("wall", 0) == 2) withContext(Dispatchers.IO) { Foto.muat(ctx) } else null
    }

    // Ganti Ruang otomatis: Kerja pada hari kerja 08.00-17.00, selain itu Santai.
    val autoRuang = p.bool("autoruang", false)
    LaunchedEffect(autoRuang) {
        var terakhir = -1
        while (autoRuang) {
            val n = LocalDateTime.now()
            val r = if (n.dayOfWeek.value in 1..5 && n.hour in 8..16) 1 else 0
            if (r != terakhir) {
                p.taruh("ruang", r)
                terakhir = r
            }
            delay(60_000)
        }
    }
    BackHandler {
        if (pengaturan) pengaturan = false else laci = false
    }

    val tema = p.tema
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val kontrol = WindowCompat.getInsetsController(window, view)
            kontrol.isAppearanceLightStatusBars = !tema.gelap
            kontrol.isAppearanceLightNavigationBars = !tema.gelap
            if (p.bool("hidestatus", false)) {
                kontrol.hide(WindowInsetsCompat.Type.statusBars())
                kontrol.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                kontrol.show(WindowInsetsCompat.Type.statusBars())
            }
        }
    }

    val fam = if (p.mono) FontFamily.Monospace else FontFamily.Default
    val skala = if (tema.gelap) {
        darkColorScheme(primary = tema.fg, secondary = tema.aksen, tertiary = tema.aksen2)
    } else {
        lightColorScheme(primary = tema.fg, secondary = tema.aksen, tertiary = tema.aksen2)
    }
    MaterialTheme(colorScheme = skala, typography = tipografi(fam)) {
        CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = fam)) {
            Box(Modifier.fillMaxSize().background(tema.bg)) {
                LatarBelakang(p, tema)
                Beranda(apps, p, onLaci = { laci = true }, onPengaturan = { pengaturan = true })
                AnimatedVisibility(
                    visible = laci,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                ) { Laci(apps, p, { pengaturan = true }) { laci = false } }
                if (pengaturan) LayarPengaturan(apps, p) { pengaturan = false }
            }
        }
    }
}

/** Semua gaya teks Material memakai satu keluarga font (monospace atau bawaan). */
private fun tipografi(f: FontFamily): Typography {
    val d = Typography()
    return Typography(
        displayLarge = d.displayLarge.copy(fontFamily = f),
        displayMedium = d.displayMedium.copy(fontFamily = f),
        displaySmall = d.displaySmall.copy(fontFamily = f),
        headlineLarge = d.headlineLarge.copy(fontFamily = f),
        headlineMedium = d.headlineMedium.copy(fontFamily = f),
        headlineSmall = d.headlineSmall.copy(fontFamily = f),
        titleLarge = d.titleLarge.copy(fontFamily = f),
        titleMedium = d.titleMedium.copy(fontFamily = f),
        titleSmall = d.titleSmall.copy(fontFamily = f),
        bodyLarge = d.bodyLarge.copy(fontFamily = f),
        bodyMedium = d.bodyMedium.copy(fontFamily = f),
        bodySmall = d.bodySmall.copy(fontFamily = f),
        labelLarge = d.labelLarge.copy(fontFamily = f),
        labelMedium = d.labelMedium.copy(fontFamily = f),
        labelSmall = d.labelSmall.copy(fontFamily = f),
    )
}
