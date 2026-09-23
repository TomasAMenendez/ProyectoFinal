package com.example.aplicacionparacelular.onboarding

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChildProfileTest {
    @Test fun parse_readsContractFields() {
        val profile = ChildProfile.parse(
            JSONObject(
                """
                {
                  "name": "Luna",
                  "gender": "nena",
                  "interests": "dinosaurios",
                  "costume": "tiburon",
                  "extra": "le cuesta el ruido fuerte",
                  "complete": true
                }
                """.trimIndent(),
            ),
        )

        assertEquals("Luna", profile.name)
        assertEquals("nena", profile.gender)
        assertEquals("dinosaurios", profile.interests)
        assertEquals("tiburon", profile.costume)
        assertEquals("le cuesta el ruido fuerte", profile.extra)
        assertTrue(profile.complete)
    }

    @Test fun canSubmit_requiresTrimmedNameAndValidGender() {
        assertTrue(ChildProfile(name = " Luna ", gender = "nena").canSubmit)
        assertFalse(ChildProfile(name = "   ", gender = "nena").canSubmit)
        assertFalse(ChildProfile(name = "Luna", gender = "").canSubmit)
        assertFalse(ChildProfile(name = "Luna", gender = "otro").canSubmit)
    }

    @Test fun sanitize_trimsAndClampsFieldsAndDefaultsCostume() {
        val profile = ChildProfile(
            name = " ${"n".repeat(45)} ",
            gender = "nene",
            interests = " ${"i".repeat(605)} ",
            costume = "invalido",
            extra = " ${"e".repeat(605)} ",
        ).sanitize()

        assertEquals(40, profile.name.length)
        assertEquals(600, profile.interests.length)
        assertEquals(600, profile.extra.length)
        assertEquals("oso", profile.costume)
    }

    @Test fun exposesGenderEnumAndCostumesInSpecOrder() {
        assertEquals("nene", ChildGender.NENE.value)
        assertEquals("nena", ChildGender.NENA.value)
        assertEquals("no_decir", ChildGender.NO_DECIR.value)
        assertEquals(
            listOf(
                "oso",
                "perro",
                "oveja",
                "dinosaurio",
                "chancho",
                "tiburon",
                "stitch",
                "spiderman",
                "chimuelo",
            ),
            ChildProfile.COSTUMES,
        )
    }
}
