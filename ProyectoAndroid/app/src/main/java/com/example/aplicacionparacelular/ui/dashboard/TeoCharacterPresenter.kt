package com.example.aplicacionparacelular.ui.dashboard

import android.graphics.Matrix
import android.widget.ImageView
import androidx.core.view.doOnLayout
import com.example.aplicacionparacelular.ui.character.TeoCharacterArt
import com.example.aplicacionparacelular.ui.character.TeoCostumeCatalog

object TeoCharacterPresenter {
    fun applyWave(image: ImageView, costume: String? = null) = apply(image, TeoCostumeCatalog.find(costume).wave)

    fun applyFront(image: ImageView, costume: String?) = apply(image, TeoCostumeCatalog.find(costume).front)

    private fun apply(image: ImageView, art: TeoCharacterArt) {
        image.setImageResource(art.drawableRes)
        image.scaleType = ImageView.ScaleType.MATRIX
        image.doOnLayout {
            val width = image.width - image.paddingLeft - image.paddingRight
            val height = image.height - image.paddingTop - image.paddingBottom
            val placement = TeoCharacterFrame.placement(art.bounds, width, height)
            image.imageMatrix = Matrix().apply {
                setScale(placement.scale, placement.scale)
                postTranslate(placement.translateX, placement.translateY)
            }
        }
    }
}
