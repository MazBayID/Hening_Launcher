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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import kotlinx.coroutines.Dispatchers
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
        }
    }

    MaterialTheme(
        colorScheme = if (tema.gelap) darkColorScheme(primary = tema.fg) else lightColorScheme(primary = tema.fg),
    ) {
        Box(Modifier.fillMaxSize().background(tema.bg)) {
            Beranda(apps, p, onLaci = { laci = true }, onPengaturan = { pengaturan = true })
            AnimatedVisibility(
                visible = laci,
                enter = slideInVertically { it },
                exit = slideOutVertically { it },
            ) { Laci(apps, p) { laci = false } }
            if (pengaturan) LayarPengaturan(apps, p) { pengaturan = false }
        }
    }
}
