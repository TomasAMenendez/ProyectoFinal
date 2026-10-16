package com.example.aplicacionparacelular.ui.routines

import com.example.aplicacionparacelular.R

object PictogramCatalog {
    data class Entry(val id: String, val labelRes: Int, val drawableRes: Int)

    val entries: List<Entry> = listOf(
        Entry("banio", R.string.routines_picto_banio, R.drawable.teo_picto_banio),
        Entry("cepillar_dientes", R.string.routines_picto_cepillar_dientes, R.drawable.teo_picto_cepillar_dientes),
        Entry("comer", R.string.routines_picto_comer, R.drawable.teo_picto_comer),
        Entry("jardin", R.string.routines_picto_jardin, R.drawable.teo_picto_jardin),
        Entry("medico", R.string.routines_picto_medico, R.drawable.teo_picto_medico),
    )

    fun find(id: String?): Entry? {
        val key = id?.trim().orEmpty()
        if (key.isEmpty()) return null
        return entries.firstOrNull { it.id == key }
    }
}
