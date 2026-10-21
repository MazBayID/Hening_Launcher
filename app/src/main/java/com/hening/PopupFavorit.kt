package com.hening

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.Process
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

private const val ID_HOST = 4711

/** Pintasan aplikasi (App Shortcuts). Hanya bisa dibaca bila Hening menjadi launcher default. */
object Pintasan {
    private fun layanan(ctx: Context) = ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    fun izin(ctx: Context): Boolean = try { layanan(ctx).hasShortcutHostPermission() } catch (e: Exception) { false }

    fun ambil(ctx: Context, pkg: String): List<ShortcutInfo> {
        return try {
            val la = layanan(ctx)
            if (!la.hasShortcutHostPermission()) return emptyList()
            val q = LauncherApps.ShortcutQuery()
                .setPackage(pkg)
                .setQueryFlags(
                    LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
                )
            (la.getShortcuts(q, Process.myUserHandle()) ?: emptyList())
                .filter { it.isEnabled }
                .sortedBy { it.rank }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun jalankan(ctx: Context, s: ShortcutInfo) {
        try {
            layanan(ctx).startShortcut(s, null, null)
        } catch (e: Exception) {
        }
    }
}

/**
 * Popup yang terbuka saat sebuah favorit digeser ke kanan:
 * pintasan aplikasi, widget aplikasi itu, dan grup aplikasi di baliknya.
 */
@Composable
fun PopupFavorit(app: App, p: Pengaturan, apps: List<App>, tutup: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val dens = LocalDensity.current
    val t = p.tema
    val peta = remember(p.str("nama")) { p.petaNama() }
    val petaApp = remember(apps) { apps.associateBy { it.pkg } }
    val nama = peta[app.pkg] ?: app.label

    val pintasan = remember(app.pkg) { Pintasan.ambil(ctx, app.pkg) }
    val punyaIzin = remember { Pintasan.izin(ctx) }

    // ----- widget -----
    val mgr = remember { AppWidgetManager.getInstance(ctx) }
    val host = remember { AppWidgetHost(ctx.applicationContext, ID_HOST) }
    DisposableEffect(host) {
        try { host.startListening() } catch (e: Exception) { }
        onDispose { try { host.stopListening() } catch (e: Exception) { } }
    }
    val idTersimpan = p.int("widget:${app.pkg}", -1)
    val daftarProvider = remember(app.pkg) {
        try { mgr.getInstalledProvidersForPackage(app.pkg, null) } catch (e: Exception) { emptyList<AppWidgetProviderInfo>() }
    }
    var pilihProvider by remember { mutableStateOf(false) }
    var idBaru by remember { mutableIntStateOf(-1) }
    val prosesRef = remember { arrayOfNulls<(Int) -> Unit>(1) }

    val konfigurasi = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { hasil ->
        val id = idBaru
        if (id != -1) {
            if (hasil.resultCode == Activity.RESULT_OK) p.taruh("widget:${app.pkg}", id) else host.deleteAppWidgetId(id)
        }
        idBaru = -1
    }
    val izinBind = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { hasil ->
        val id = idBaru
        if (id != -1) {
            if (hasil.resultCode == Activity.RESULT_OK) {
                prosesRef[0]?.invoke(id)
            } else {
                host.deleteAppWidgetId(id)
                idBaru = -1
            }
        }
    }

    fun proses(id: Int) {
        val cfg = mgr.getAppWidgetInfo(id)?.configure
        if (cfg != null) {
            try {
                konfigurasi.launch(
                    Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE)
                        .setComponent(cfg)
                        .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                )
            } catch (e: Exception) {
                p.taruh("widget:${app.pkg}", id)
                idBaru = -1
            }
        } else {
            p.taruh("widget:${app.pkg}", id)
            idBaru = -1
        }
    }
    prosesRef[0] = ::proses

    fun tambah(info: AppWidgetProviderInfo) {
        val id = host.allocateAppWidgetId()
        idBaru = id
        if (mgr.bindAppWidgetIdIfAllowed(id, info.provider)) {
            proses(id)
        } else {
            izinBind.launch(
                Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            )
        }
    }

    // ----- animasi masuk dengan pegas -----
    var tampil by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { tampil = true }
    val skala by animateFloatAsState(
        if (tampil) 1f else 0.85f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "popup",
    )

    Dialog(onDismissRequest = tutup, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = t.bg.copy(alpha = 0.97f),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .padding(20.dp)
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = skala
                    scaleY = skala
                    alpha = ((skala - 0.85f) / 0.15f).coerceIn(0f, 1f)
                },
        ) {
            Column(
                Modifier.verticalScroll(rememberScrollState()).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(nama, color = t.aksen, fontSize = 20.sp, modifier = Modifier.weight(1f))
                    Text("✕", color = t.redup, fontSize = 18.sp, modifier = Modifier.clickable { tutup() }.padding(8.dp))
                }
                Text(
                    "Buka aplikasi ›",
                    color = t.fg,
                    fontSize = 16.sp,
                    modifier = Modifier.fillMaxWidth().clickable { Peluncur.buka(ctx, app, p); tutup() }.padding(vertical = 6.dp),
                )

                Judul("pintasan", t)
                if (!punyaIzin) {
                    Redup("Jadikan Hening launcher default untuk melihat pintasan aplikasi.", t)
                } else if (pintasan.isEmpty()) {
                    Redup("Aplikasi ini tidak punya pintasan.", t)
                } else {
                    pintasan.take(6).forEach { s ->
                        Text(
                            "› " + (s.shortLabel ?: s.id),
                            color = t.fg,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { Getar.konfirmasi(view); Pintasan.jalankan(ctx, s); tutup() }
                                .padding(vertical = 8.dp),
                        )
                    }
                }

                Judul("widget", t)
                if (idTersimpan != -1) {
                    val info = remember(idTersimpan) { mgr.getAppWidgetInfo(idTersimpan) }
                    if (info == null) {
                        Redup("Widget ini sudah tidak tersedia.", t)
                    } else {
                        val lebar = with(dens) { info.minWidth.toDp() }.coerceIn(160.dp, 340.dp)
                        val tinggi = with(dens) { info.minHeight.toDp() }.coerceIn(80.dp, 320.dp)
                        AndroidView(
                            factory = { c -> host.createView(c, idTersimpan, info) },
                            modifier = Modifier.size(lebar, tinggi),
                        )
                    }
                    TextButton(onClick = {
                        try { host.deleteAppWidgetId(idTersimpan) } catch (e: Exception) { }
                        p.taruh("widget:${app.pkg}", -1)
                    }) { Text("Hapus widget", color = t.aksen2) }
                } else if (daftarProvider.isEmpty()) {
                    Redup("Aplikasi ini tidak punya widget.", t)
                } else {
                    TextButton(onClick = { pilihProvider = true }) { Text("+ Tambah widget", color = t.aksen2) }
                }

                Judul("grup", t)
                val anggota = p.grup(app.pkg).mapNotNull { petaApp[it] }
                if (anggota.isEmpty()) {
                    Redup("Kosong. Tekan lama aplikasi lain lalu pilih Masukkan ke grup favorit.", t)
                }
                anggota.forEach { g ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "› " + (peta[g.pkg] ?: g.label),
                            color = t.fg,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { Peluncur.buka(ctx, g, p); tutup() }
                                .padding(vertical = 8.dp),
                        )
                        Text("✕", color = t.redup, modifier = Modifier.clickable { p.toggleGrup(app.pkg, g.pkg) }.padding(8.dp))
                    }
                }
            }
        }
    }

    if (pilihProvider) {
        AlertDialog(
            onDismissRequest = { pilihProvider = false },
            title = { Text("Pilih widget") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    daftarProvider.forEach { info ->
                        Text(
                            info.loadLabel(ctx.packageManager),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pilihProvider = false; tambah(info) }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pilihProvider = false }) { Text("Batal") } },
        )
    }
}

@Composable
private fun Judul(teks: String, t: Tema) {
    Text("# $teks", color = t.redup, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun Redup(teks: String, t: Tema) {
    Text(teks, color = t.redup, fontSize = 13.sp)
}
