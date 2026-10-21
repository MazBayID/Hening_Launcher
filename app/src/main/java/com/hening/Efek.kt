package com.hening

import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.unit.IntOffset
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Pegas untuk perpindahan item daftar: sedikit memantul, lalu tenang. Setara SpringForce di View. */
val PegasItem: FiniteAnimationSpec<IntOffset> =
    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)

/** Getaran halus untuk geseran antar item. Memakai getaran segmen bila tersedia (Android 14 ke atas). */
object Getar {
    fun tick(view: View) {
        val kode = when {
            Build.VERSION.SDK_INT >= 34 -> HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
            else -> HapticFeedbackConstants.CLOCK_TICK
        }
        view.performHapticFeedback(kode)
    }

    fun konfirmasi(view: View) {
        val kode = if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        view.performHapticFeedback(kode)
    }
}

/** Status transisi buka aplikasi. Beranda mengecil dengan pegas saat aplikasi dibuka, lalu kembali saat resume. */
object Transisi {
    var membuka by mutableStateOf(false)
}
