package com.example.aplicacionparacelular.ui.preview

import com.example.aplicacionparacelular.ui.routines.RoutineItem

object RoutinePreviewSamples {
    fun items(): List<RoutineItem> = listOf(
        RoutineItem(
            id = "ejemplo_dientes",
            name = "Cepillarse los dientes",
            time = "08:00",
            reminderMessage = "¡Es hora de cepillarse los dientes!",
            transitionFrom = "",
            transitionTo = "",
            successMessage = "",
            preReminderMinutes = 5,
            enabled = true,
            reminded = true,
            completed = true,
            pictogramId = "cepillar_dientes",
        ),
        RoutineItem(
            id = "ejemplo_almuerzo",
            name = "Almorzar en casa con la familia",
            time = "12:30",
            reminderMessage = "¡A prepararse para almorzar!\nLavate las manos antes de sentarte.",
            transitionFrom = "",
            transitionTo = "",
            successMessage = "",
            preReminderMinutes = 5,
            enabled = true,
            reminded = true,
            completed = false,
            pictogramId = "comer",
        ),
        RoutineItem(
            id = "ejemplo_dormir",
            name = "Prepararse para dormir y apagar las luces",
            time = "20:30",
            reminderMessage = "Es hora de prepararse para dormir.\nCuando termines, apagamos la luz.",
            transitionFrom = "",
            transitionTo = "",
            successMessage = "",
            preReminderMinutes = 5,
            enabled = false,
        ),
    )
}
