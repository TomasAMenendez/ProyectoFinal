package com.example.aplicacionparacelular.ui.medical

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AgendaRulesTest {
    @Test
    fun titleAndWhenStayOnTheFormUntilBothExist() {
        assertEquals("El título es obligatorio.", AgendaRules.titleProblem("  "))
        assertNull(AgendaRules.titleProblem("Control"))
        assertEquals("Elegí la fecha.", AgendaRules.whenProblem(false, false))
        assertEquals("Elegí la hora.", AgendaRules.whenProblem(true, false))
        assertNull(AgendaRules.whenProblem(true, true))
    }

    @Test
    fun slashDateKeepsItsYearAndNamedDateWithoutYearDoesNotInventOne() {
        val slash = AgendaRules.millisOf("20/10/2026 10:30")
        val cal = Calendar.getInstance()
        cal.timeInMillis = slash
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, cal.get(Calendar.MONTH))
        assertEquals(20, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(10, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(0L, AgendaRules.millisOf("15 de septiembre, 10:00"))
        val named = AgendaRules.millisOf("15 de septiembre de 2026, 10:00")
        cal.timeInMillis = named
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0L, AgendaRules.millisOf("31/02/2026 10:00"))
    }

    @Test
    fun sortUsesRealTimeAndKeepsUndatedRecords() {
        val early = Appointment("a", "Control", "01/11/2026 09:00", atMillis = millis(2026, 11, 1, 9, 0))
        val later = Appointment("b", "Control", "20/10/2026 10:30", atMillis = millis(2026, 10, 20, 10, 30))
        val old = Appointment("c", "Fonoaudióloga", "15 de septiembre, 10:00", notes = "carnet")
        val sorted = AgendaRules.sort(listOf(early, old, later))
        assertEquals(listOf("b", "a", "c"), sorted.map { it.id })
        assertEquals("carnet", sorted.last().notes)
        assertEquals("20/10/2026 - 10:30 hs", AgendaRules.displayWhen(later))
        assertEquals("15 de septiembre, 10:00", AgendaRules.displayWhen(old))
    }

    @Test
    fun deleteUsesIdWhenTitlesMatch() {
        val first = Appointment("1", "Control", "20/10/2026 10:00", atMillis = 1)
        val second = Appointment("2", "Control", "21/10/2026 10:00", atMillis = 2)
        val left = AgendaRules.without(listOf(first, second), "1")
        assertEquals(listOf("2"), left.map { it.id })
    }

    @Test
    fun vaccineStatesFollowTheRecord() {
        val applied = AgendaRules.vaccine(Vaccine("v", "BCG", "Al nacer", true, dueDate = "2026-10-20"))
        assertEquals(VaccineStatus.APPLIED, applied.status)
        assertEquals(false, applied.showSchedule)
        val pending = AgendaRules.vaccine(Vaccine("v", "BCG", "Al nacer", false))
        assertNull(pending.dueText)
        assertTrue(pending.showSchedule)
        val dated = AgendaRules.vaccine(Vaccine("v", "BCG", "Al nacer", false, dueDate = "2026-11-03"))
        assertEquals("03/11/2026", dated.dueText)
        assertTrue(dated.showSchedule)
    }

    @Test
    fun freshCatalogDoesNotClaimApplicationsAndSavedRecordsRoundTrip() {
        assertTrue(AgendaRules.defaultVaccines().none { it.applied || it.dueDate.isNotBlank() || it.appliedDate.isNotBlank() })
        val saved = Appointment("id", "Control", "15 de septiembre, 10:00", notes = "traer carnet", atMillis = 0L)
        val json = MedicalRecords.appointmentsTo(listOf(saved))
        val loaded = MedicalRecords.appointmentsFrom(json)
        assertEquals(saved, loaded?.single())
        assertEquals(emptyList<Appointment>(), MedicalRecords.appointmentsFrom("[]"))
        assertEquals(null, MedicalRecords.appointmentsFrom("{"))
        val vaccine = Vaccine("v8", "Quíntuple", "4 meses", true, appliedDate = "01/04/2026", dueDate = "2026-04-01")
        assertEquals(vaccine, MedicalRecords.vaccinesFrom(MedicalRecords.vaccinesTo(listOf(vaccine)))?.single())
    }

    private fun millis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
