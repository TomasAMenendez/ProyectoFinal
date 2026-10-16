package com.example.aplicacionparacelular.ui.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.imageio.ImageIO

class TeoCharacterFrameTest {
    @Test
    fun elRectanguloVisibleNoUsaElLienzoCompleto() {
        val width = 1365
        val height = 768
        val bounds = TeoCharacterFrame.bounds(width, height) { x, y ->
            x in 371 until 927 && y in 34 until 738
        }
        assertEquals(OpaqueBounds(371, 34, 927, 738), bounds)
        val slotWidth = 400
        val slotHeight = 520
        val contentScale = TeoCharacterFrame.uniformScale(bounds!!.width, bounds.height, slotWidth, slotHeight)
        val canvasScale = TeoCharacterFrame.uniformScale(width, height, slotWidth, slotHeight)
        assertTrue(contentScale > canvasScale)
        val placement = TeoCharacterFrame.placement(bounds, slotWidth, slotHeight)
        assertEquals(contentScale, placement.scale)
        assertEquals(placement.scale, placement.scale)
    }

    @Test
    fun laPoseSaludandoTieneEseContenidoOpaco() {
        val file = File("src/main/res/drawable/teo_character_wave.png")
        assertTrue(file.exists())
        val image = ImageIO.read(file)
        val bounds = TeoCharacterFrame.bounds(image.width, image.height) { x, y ->
            image.getRGB(x, y) ushr 24 != 0
        }
        assertEquals(1365, image.width)
        assertEquals(768, image.height)
        assertEquals(OpaqueBounds(371, 34, 927, 738), bounds)
    }
}
