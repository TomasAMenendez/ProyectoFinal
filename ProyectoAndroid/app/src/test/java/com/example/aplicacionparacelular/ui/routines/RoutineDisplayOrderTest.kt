package com.example.aplicacionparacelular.ui.routines

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineDisplayOrderTest {
    private fun routine(id: String, time: String, enabled: Boolean = true) = RoutineItem(
        id = id,
        name = id,
        time = time,
        reminderMessage = "Mensaje de $id",
        transitionFrom = "",
        transitionTo = "",
        successMessage = "",
        preReminderMinutes = 5,
        enabled = enabled,
        pictogramId = "comer",
    )

    @Test
    fun unaRutinaAgregadaALas17ApareceAntesDeLaDeLas2030() {
        val input = listOf(
            routine("dientes", "08:00"),
            routine("dormir", "20:30", enabled = false),
            routine("merendar", "17:00"),
        )

        val displayed = RoutineDisplayOrder.sorted(input)

        assertEquals(listOf("dientes", "merendar", "dormir"), displayed.map { it.id })
        assertEquals(listOf("dientes", "dormir", "merendar"), input.map { it.id })
        assertEquals(input[1], displayed[2])
    }

    @Test
    fun editarLaHoraReubicaLaRutinaYConservaElOrdenDeLosEmpates() {
        val input = listOf(
            routine("primera", "08:00"),
            routine("segunda", "08:00"),
            routine("editada", "07:30"),
        )

        assertEquals(
            listOf("editada", "primera", "segunda"),
            RoutineDisplayOrder.sorted(input).map { it.id },
        )
    }

    @Test
    fun lasHorasAntiguasSinCeroInicialSeOrdenanYLasInvalidasQuedanAlFinal() {
        val input = listOf(
            routine("invalida", "sin hora"),
            routine("tarde", "17:00"),
            routine("maniana", "9:05"),
            routine("fuera_de_rango", "25:00"),
        )

        assertEquals(
            listOf("maniana", "tarde", "invalida", "fuera_de_rango"),
            RoutineDisplayOrder.sorted(input).map { it.id },
        )
    }
}
