package com.example.aplicacionparacelular.ui.routines

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.aplicacionparacelular.network.ApiResult
import com.example.aplicacionparacelular.network.RobotConnectionManager
import org.json.JSONArray
import org.json.JSONObject

data class RoutineItem(
    val id: String,
    val name: String,
    val time: String,
    val reminderMessage: String,
    val transitionFrom: String,
    val transitionTo: String,
    val successMessage: String,
    val preReminderMinutes: Int,
    val enabled: Boolean,
    val reminded: Boolean = false,
    val completed: Boolean = false,
    val pictogramId: String? = null,
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("time", time)
            put("reminder_message", reminderMessage)
            put("transition_from", transitionFrom)
            put("transition_to", transitionTo)
            put("success_message", successMessage)
            put("pre_reminder_minutes", preReminderMinutes)
            put("enabled", enabled)
            val pictogram = pictogramId?.trim().orEmpty()
            if (pictogram.isNotEmpty()) put("pictogram_id", pictogram)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RoutineItem {
            return RoutineItem(
                id = json.optString("id", ""),
                name = json.optString("name", ""),
                time = json.optString("time", "00:00"),
                reminderMessage = json.optString("reminder_message", ""),
                transitionFrom = json.optString("transition_from", ""),
                transitionTo = json.optString("transition_to", ""),
                successMessage = json.optString("success_message", ""),
                preReminderMinutes = json.optInt("pre_reminder_minutes", 5),
                enabled = json.optBoolean("enabled", true),
                reminded = json.optBoolean("reminded", false),
                completed = json.optBoolean("completed", false),
                pictogramId = if (!json.has("pictogram_id") || json.isNull("pictogram_id")) {
                    null
                } else {
                    json.optString("pictogram_id").trim().ifEmpty { null }
                },
            )
        }
    }
}

class RoutinesViewModel(
    private val fetchRoutines: ((ApiResult<JSONObject>) -> Unit) -> Unit = {
        RobotConnectionManager.fetchRoutines(it)
    },
    private val updateRoutines: (JSONObject, (ApiResult<JSONObject>) -> Unit) -> Unit = { body, callback ->
        RobotConnectionManager.updateRoutines(body, callback)
    },
) : ViewModel() {

    private val _routines = MutableLiveData<List<RoutineItem>>(emptyList())
    val routines: LiveData<List<RoutineItem>> = _routines

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _syncStatus = MutableLiveData<String?>()
    val syncStatus: LiveData<String?> = _syncStatus

    private var hasLoaded = false
    private val _canEdit = MutableLiveData(false)
    val canEdit: LiveData<Boolean> = _canEdit

    fun loadRoutines() {
        if (_isLoading.value == true) return
        hasLoaded = false
        _isLoading.value = true
        publishEditable()
        fetchRoutines { result ->
            _isLoading.value = false
            when (result) {
                is ApiResult.Success -> {
                    val routinesArray = result.data.optJSONArray("routines")
                    if (routinesArray == null) {
                        _syncStatus.value = "No se pudieron leer las rutinas"
                    } else {
                        val list = mutableListOf<RoutineItem>()
                        for (i in 0 until routinesArray.length()) {
                            val json = routinesArray.optJSONObject(i) ?: continue
                            list.add(RoutineItem.fromJson(json))
                        }
                        hasLoaded = true
                        _routines.value = list
                        scheduleLocalReminders()
                    }
                }
                is ApiResult.Error -> {
                    _syncStatus.value = result.message.ifBlank { "No se pudieron leer las rutinas" }
                }
            }
            publishEditable()
        }
    }

    fun consumeStatus() {
        _syncStatus.value = null
    }

    fun save(item: RoutineItem, isNew: Boolean, onResult: (Boolean, String?) -> Unit) {
        val previous = _routines.value.orEmpty()
        val next = if (isNew) {
            previous + item
        } else if (previous.none { it.id == item.id }) {
            onResult(false, "No se encontró la rutina")
            return
        } else {
            previous.map { if (it.id == item.id) item else it }
        }
        persist(next, onResult)
    }

    fun removeRoutine(id: String) {
        val previous = _routines.value.orEmpty()
        persist(previous.filterNot { it.id == id }) { _, _ -> }
    }

    fun toggleRoutine(id: String) {
        val previous = _routines.value.orEmpty()
        val next = previous.map { routine ->
            if (routine.id == id) routine.copy(enabled = !routine.enabled) else routine
        }
        persist(next) { _, _ -> }
    }

    private fun persist(
        next: List<RoutineItem>,
        onResult: (Boolean, String?) -> Unit,
    ) {
        if (_canEdit.value != true) {
            val message = if (_isLoading.value == true) {
                "Esperá a que termine la operación de rutinas"
            } else {
                "Primero hay que leer las rutinas de TEO. Reintentá la carga"
            }
            _syncStatus.value = message
            onResult(false, message)
            return
        }
        _isLoading.value = true
        publishEditable()
        val jsonArray = JSONArray()
        for (routine in next) {
            jsonArray.put(routine.toJson())
        }
        val body = JSONObject().apply { put("routines", jsonArray) }
        updateRoutines(body) { result ->
            _isLoading.value = false
            if (result is ApiResult.Success && result.data.optString("status") == "ok") {
                _routines.value = next
                scheduleLocalReminders()
                publishEditable()
                onResult(true, null)
            } else {
                hasLoaded = false
                val message = when (result) {
                    is ApiResult.Error -> result.message
                    is ApiResult.Success -> result.data.optString("reason").ifBlank {
                        result.data.optString("message").ifBlank { result.data.optString("error") }
                    }
                }.ifBlank { "No se pudieron guardar las rutinas" }
                publishEditable()
                _syncStatus.value = message
                onResult(false, message)
            }
        }
    }

    fun attachContext(context: android.content.Context) {
        appContext = context.applicationContext
        scheduleLocalReminders()
    }

    private var appContext: android.content.Context? = null

    private fun publishEditable() {
        _canEdit.value = hasLoaded && _isLoading.value != true
    }

    private fun scheduleLocalReminders() {
        if (!hasLoaded) return
        val ctx = appContext ?: return
        val items = _routines.value.orEmpty()
            .filter { it.enabled }
            .mapNotNull { routine ->
                val parts = routine.time.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
                val minute = parts.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                val total = hour * 60 + minute
                val pre = (total - routine.preReminderMinutes).mod(24 * 60)
                listOf(
                    Triple(routine.id, routine.name, total),
                    Triple(routine.id + "_pre", "Pronto: ${routine.name}", pre),
                )
            }
            .flatten()
        com.example.aplicacionparacelular.notifications.ReminderScheduler.saveAndScheduleRoutines(ctx, items)
    }
}
