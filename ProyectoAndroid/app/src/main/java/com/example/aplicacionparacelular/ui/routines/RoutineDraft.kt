package com.example.aplicacionparacelular.ui.routines

data class RoutineFields(
    val name: String,
    val time: String,
    val message: String,
    val pictogramId: String? = null,
)

sealed class RoutineOutcome {
    data class Ready(val item: RoutineItem) : RoutineOutcome()
    data class Invalid(val nameError: String? = null, val formError: String? = null) : RoutineOutcome()
}

enum class RoutineFollowUp {
    NONE,
    REMINDED,
    COMPLETED,
}

object RoutineDraft {
    private val timePattern = Regex("""([01]\d|2[0-3]):[0-5]\d""")

    fun automaticMessage(name: String): String = "¡Es hora de ${name.trim().lowercase()}!"

    fun deleteQuestion(name: String): String = "¿Querés eliminar la rutina «${name.trim()}»?"

    fun followUp(completed: Boolean, reminded: Boolean): RoutineFollowUp = when {
        completed -> RoutineFollowUp.COMPLETED
        reminded -> RoutineFollowUp.REMINDED
        else -> RoutineFollowUp.NONE
    }

    fun forCreate(fields: RoutineFields, id: String): RoutineOutcome {
        val name = fields.name.trim()
        if (name.isEmpty()) return RoutineOutcome.Invalid(nameError = "El nombre es obligatorio")
        val time = normalizedTime(fields.time)
            ?: return RoutineOutcome.Invalid(formError = "Elegí una hora válida")
        val message = fields.message.trim().ifEmpty { automaticMessage(name) }
        return RoutineOutcome.Ready(
            RoutineItem(
                id = id,
                name = name,
                time = time,
                reminderMessage = message,
                transitionFrom = "",
                transitionTo = "",
                successMessage = "¡Muy bien! ¡Lo lograste!",
                preReminderMinutes = 5,
                enabled = true,
                pictogramId = fields.pictogramId?.trim()?.ifEmpty { null },
            ),
        )
    }

    fun forEdit(original: RoutineItem, fields: RoutineFields): RoutineOutcome {
        val name = fields.name.trim()
        if (name.isEmpty()) return RoutineOutcome.Invalid(nameError = "El nombre es obligatorio")
        val time = normalizedTime(fields.time) ?: original.time
        val message = fields.message.trim().ifEmpty { original.reminderMessage }
        return RoutineOutcome.Ready(
            original.copy(
                name = name,
                time = time,
                reminderMessage = message,
                pictogramId = fields.pictogramId?.trim()?.ifEmpty { null },
            ),
        )
    }

    fun newId(name: String, now: Long = System.currentTimeMillis()): String {
        val slug = name.trim().lowercase().replace(" ", "_").ifEmpty { "rutina" }
        return "${slug}_$now"
    }

    private fun normalizedTime(raw: String): String? {
        val text = raw.trim()
        return if (timePattern.matches(text)) text else null
    }
}
