package com.example.aplicacionparacelular.ui.dashboard

import com.example.aplicacionparacelular.R
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeChipsTest {
    @Test
    fun etiquetasDeEstadoUsanColorEIconoDistintos() {
        assertEquals(R.color.teo_success, HomeChips.connectionColors(true).first)
        assertEquals(R.color.teo_neutral, HomeChips.connectionColors(false).first)
        assertEquals(R.color.teo_gold, HomeChips.activityColors(TeoActivity.AWAKE).first)
        assertEquals(R.drawable.teo_ic_star, HomeChips.activityIcon(TeoActivity.AWAKE))
        assertEquals(R.color.teo_sleep, HomeChips.activityColors(TeoActivity.ASLEEP).first)
        assertEquals(R.drawable.teo_ic_moon, HomeChips.activityIcon(TeoActivity.ASLEEP))
        assertEquals(R.color.teo_gold, HomeChips.noticeColors("logro").first)
        assertEquals(R.color.teo_error_bg, HomeChips.noticeColors("crisis").first)
    }
}
