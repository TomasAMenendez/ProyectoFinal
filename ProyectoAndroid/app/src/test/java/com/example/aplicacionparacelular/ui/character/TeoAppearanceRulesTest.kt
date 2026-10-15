package com.example.aplicacionparacelular.ui.character

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class TeoAppearanceRulesTest {
    @Test fun onlyACompleteProfileWithAKnownCostumeChangesAppearance() {
        assertEquals("tiburon", TeoAppearanceRules.confirmedCostume(JSONObject("""{"complete":true,"costume":"tiburon"}""")))
        assertNull(TeoAppearanceRules.confirmedCostume(JSONObject("""{"complete":false,"costume":"perro"}""")))
        assertNull(TeoAppearanceRules.confirmedCostume(JSONObject("""{"complete":true}""")))
        assertNull(TeoAppearanceRules.confirmedCostume(JSONObject("""{"complete":true,"costume":null}""")))
        assertNull(TeoAppearanceRules.confirmedCostume(JSONObject("""{"complete":true,"costume":"unknown"}""")))
    }
}
