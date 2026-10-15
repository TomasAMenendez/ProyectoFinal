package com.example.aplicacionparacelular.ui.character

import com.example.aplicacionparacelular.onboarding.ChildProfile
import com.example.aplicacionparacelular.ui.dashboard.TeoCharacterFrame
import org.junit.Assert.*
import org.junit.Test

class TeoCostumeCatalogTest {
    @Test fun everyApiCostumeHasTwoDistinctFramedImages() {
        assertEquals(ChildProfile.COSTUMES, TeoCostumeCatalog.entries.map { it.id })
        val images = TeoCostumeCatalog.entries.flatMap { listOf(it.front, it.wave) }
        assertEquals(18, images.map { it.drawableRes }.distinct().size)
        images.forEach { art ->
            assertTrue(art.bounds.left >= 0 && art.bounds.top >= 0)
            assertTrue(art.bounds.right <= art.width && art.bounds.bottom <= art.height)
            assertTrue(art.bounds.right > art.bounds.left && art.bounds.bottom > art.bounds.top)
            val placement = TeoCharacterFrame.placement(art.bounds, 80, 104)
            assertTrue(art.bounds.width * placement.scale <= 80.01f)
            assertTrue(art.bounds.height * placement.scale <= 104.01f)
        }
    }

    @Test fun unknownCostumeUsesTheBearAndKeepsTheApprovedWaveFraming() {
        val bear = TeoCostumeCatalog.find("oso")
        assertEquals(bear, TeoCostumeCatalog.find("unknown"))
        assertEquals(bear, TeoCostumeCatalog.find(null))
        assertEquals(371, bear.wave.bounds.left)
        assertEquals(738, bear.wave.bounds.bottom)
    }
}
