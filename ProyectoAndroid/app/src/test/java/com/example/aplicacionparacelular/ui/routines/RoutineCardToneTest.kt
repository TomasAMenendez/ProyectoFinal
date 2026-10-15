package com.example.aplicacionparacelular.ui.routines

import com.example.aplicacionparacelular.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class RoutineCardToneTest {
    @Test
    fun activadaYDesactivadaUsanTonosDistintos() {
        assertNotEquals(RoutineCardTone.surface(true), RoutineCardTone.surface(false))
        assertEquals(R.color.teo_surface, RoutineCardTone.surface(true))
        assertEquals(R.color.teo_routine_inactive_surface, RoutineCardTone.surface(false))
        assertEquals(R.color.teo_text, RoutineCardTone.emphasis(true))
        assertEquals(R.color.teo_routine_inactive_text, RoutineCardTone.emphasis(false))
        assertEquals(R.color.teo_secondary, RoutineCardTone.message(true))
        assertEquals(R.color.teo_routine_inactive_message, RoutineCardTone.message(false))
    }

    @Test
    fun volverAActivarRecuperaElTonoDeLaTarjetaActiva() {
        assertEquals(R.color.teo_routine_inactive_text, RoutineCardTone.emphasis(false))
        assertEquals(R.color.teo_text, RoutineCardTone.emphasis(true))
        assertEquals(R.color.teo_surface, RoutineCardTone.surface(true))
        assertEquals(R.color.teo_secondary, RoutineCardTone.message(true))
    }
}
