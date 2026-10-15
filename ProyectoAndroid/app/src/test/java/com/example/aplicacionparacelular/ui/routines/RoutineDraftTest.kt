package com.example.aplicacionparacelular.ui.routines

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineDraftTest {
    private val original = RoutineItem(
        id = "dormir",
        name = "Prepararse para dormir",
        time = "20:30",
        reminderMessage = "Es hora de prepararse para dormir",
        transitionFrom = "juego",
        transitionTo = "calma",
        successMessage = "Listo",
        preReminderMinutes = 5,
        enabled = false,
        reminded = true,
        completed = false,
    )

    @Test
    fun elAltaVacioUsaLaFraseAutomaticaYQuedaActivada() {
        val outcome = RoutineDraft.forCreate(
            RoutineFields("Merendar", "17:00", "  "),
            "merendar_1",
        ) as RoutineOutcome.Ready
        assertEquals("¡Es hora de merendar!", outcome.item.reminderMessage)
        assertTrue(outcome.item.enabled)
        assertEquals("17:00", outcome.item.time)
        assertEquals(5, outcome.item.preReminderMinutes)
        assertNull(outcome.item.pictogramId)
        assertFalse(outcome.item.toJson().has("pictogram_id"))
    }

    @Test
    fun elNombreVacioNoDescartaElResto() {
        val outcome = RoutineDraft.forCreate(
            RoutineFields("  ", "17:00", "Mensaje escrito"),
            "id",
        )
        assertTrue(outcome is RoutineOutcome.Invalid)
    }

    @Test
    fun editarConservaActivacionYCamposNoEscritos() {
        val outcome = RoutineDraft.forEdit(
            original,
            RoutineFields("Dormir", "20:30", ""),
        ) as RoutineOutcome.Ready
        assertEquals("Dormir", outcome.item.name)
        assertEquals(original.reminderMessage, outcome.item.reminderMessage)
        assertEquals(original.time, outcome.item.time)
        assertFalse(outcome.item.enabled)
        assertEquals(original.transitionFrom, outcome.item.transitionFrom)
        assertEquals(original.successMessage, outcome.item.successMessage)
        assertNull(outcome.item.pictogramId)
    }

    @Test
    fun elPictogramaSeGuardaSeConservaYSeQuita() {
        val created = RoutineDraft.forCreate(
            RoutineFields("Baño", "19:00", "", "banio"),
            "bano_1",
        ) as RoutineOutcome.Ready
        assertEquals("banio", created.item.pictogramId)
        assertEquals("banio", created.item.toJson().getString("pictogram_id"))
        val kept = RoutineDraft.forEdit(
            created.item.copy(enabled = false, transitionFrom = "juego"),
            RoutineFields("Baño", "19:00", "Mensaje", "banio"),
        ) as RoutineOutcome.Ready
        assertFalse(kept.item.enabled)
        assertEquals("juego", kept.item.transitionFrom)
        assertEquals("banio", kept.item.pictogramId)
        val cleared = RoutineDraft.forEdit(
            kept.item,
            RoutineFields("Baño", "19:00", "Mensaje", null),
        ) as RoutineOutcome.Ready
        assertNull(cleared.item.pictogramId)
        val legacy = RoutineItem.fromJson(JSONObject("""{"id":"vieja","name":"Juego","time":"10:00"}"""))
        assertNull(legacy.pictogramId)
        val roundTrip = RoutineItem.fromJson(created.item.toJson())
        assertEquals("banio", roundTrip.pictogramId)
    }

    @Test
    fun completadaPrevaleceSobreRecordada() {
        assertEquals(RoutineFollowUp.COMPLETED, RoutineDraft.followUp(completed = true, reminded = true))
        assertEquals(RoutineFollowUp.REMINDED, RoutineDraft.followUp(completed = false, reminded = true))
        assertEquals(RoutineFollowUp.NONE, RoutineDraft.followUp(completed = false, reminded = false))
    }

    @Test
    fun laPreguntaIncluyeElNombre() {
        assertEquals(
            "¿Querés eliminar la rutina «Prepararse para dormir»?",
            RoutineDraft.deleteQuestion(" Prepararse para dormir "),
        )
    }
}
