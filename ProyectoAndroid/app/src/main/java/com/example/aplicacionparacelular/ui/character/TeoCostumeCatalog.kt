package com.example.aplicacionparacelular.ui.character

import com.example.aplicacionparacelular.R
import com.example.aplicacionparacelular.ui.dashboard.OpaqueBounds

data class TeoCharacterArt(val drawableRes: Int, val width: Int, val height: Int, val bounds: OpaqueBounds)
data class TeoCostume(val id: String, val labelRes: Int, val front: TeoCharacterArt, val wave: TeoCharacterArt)

/** Coordenadas del contenido visible de los PNG originales, sin recortarlos. */
object TeoCostumeCatalog {
    val entries = listOf(
        TeoCostume("oso", R.string.child_profile_costume_oso,
            art(R.drawable.teo_skin_oso_front, 1671, 941, 549, 31, 1157, 915),
            art(R.drawable.teo_skin_oso_wave, 1365, 768, 371, 34, 927, 738)),
        TeoCostume("perro", R.string.child_profile_costume_perro,
            art(R.drawable.teo_skin_perro_front, 1672, 941, 458, 58, 1225, 919),
            art(R.drawable.teo_skin_perro_wave, 1365, 768, 356, 45, 987, 727)),
        TeoCostume("oveja", R.string.child_profile_costume_oveja,
            art(R.drawable.teo_skin_oveja_front, 1671, 941, 502, 46, 1189, 932),
            art(R.drawable.teo_skin_oveja_wave, 1365, 768, 357, 44, 958, 737)),
        TeoCostume("dinosaurio", R.string.child_profile_costume_dinosaurio,
            art(R.drawable.teo_skin_dinosaurio_front, 1672, 941, 529, 20, 1151, 899),
            art(R.drawable.teo_skin_dinosaurio_wave, 1365, 768, 357, 20, 937, 729)),
        TeoCostume("chancho", R.string.child_profile_costume_chancho,
            art(R.drawable.teo_skin_chancho_front, 2730, 1536, 839, 82, 1913, 1469),
            art(R.drawable.teo_skin_chancho_wave, 1365, 768, 360, 42, 956, 736)),
        TeoCostume("tiburon", R.string.child_profile_costume_tiburon,
            art(R.drawable.teo_skin_tiburon_front, 1672, 941, 538, 11, 1147, 896),
            art(R.drawable.teo_skin_tiburon_wave, 1365, 768, 354, 9, 938, 729)),
        TeoCostume("stitch", R.string.child_profile_costume_stitch,
            art(R.drawable.teo_skin_stitch_front, 1672, 941, 458, 54, 1213, 899),
            art(R.drawable.teo_skin_stitch_wave, 1365, 768, 355, 44, 988, 728)),
        TeoCostume("spiderman", R.string.child_profile_costume_spiderman,
            art(R.drawable.teo_skin_spiderman_front, 1365, 768, 453, 43, 926, 735),
            art(R.drawable.teo_skin_spiderman_wave, 1365, 768, 366, 44, 925, 736)),
        TeoCostume("chimuelo", R.string.child_profile_costume_chimuelo,
            art(R.drawable.teo_skin_chimuelo_front, 1672, 941, 510, 23, 1172, 897),
            art(R.drawable.teo_skin_chimuelo_wave, 1365, 768, 353, 20, 955, 730)),
    )

    fun find(id: String?): TeoCostume = entries.firstOrNull { it.id == id } ?: entries.first()

    private fun art(res: Int, width: Int, height: Int, left: Int, top: Int, right: Int, bottom: Int) =
        TeoCharacterArt(res, width, height, OpaqueBounds(left, top, right, bottom))
}
