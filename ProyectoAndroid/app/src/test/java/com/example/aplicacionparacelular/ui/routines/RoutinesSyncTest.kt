package com.example.aplicacionparacelular.ui.routines

import androidx.arch.core.executor.ArchTaskExecutor
import androidx.arch.core.executor.TaskExecutor
import com.example.aplicacionparacelular.network.ApiResult
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoutinesSyncTest {
    private val reads = mutableListOf<(ApiResult<JSONObject>) -> Unit>()
    private val writes = mutableListOf<Pair<JSONObject, (ApiResult<JSONObject>) -> Unit>>()
    private lateinit var model: RoutinesViewModel
    private val first = RoutineItem("comer", "Comer", "12:00", "A comer", "", "", "Listo", 5, true)
    private val second = first.copy(id = "banio", name = "Baño", pictogramId = "banio")

    @Before
    fun prepare() {
        ArchTaskExecutor.getInstance().setDelegate(object : TaskExecutor() {
            override fun executeOnDiskIO(runnable: Runnable) = runnable.run()
            override fun postToMainThread(runnable: Runnable) = runnable.run()
            override fun isMainThread() = true
        })
        model = RoutinesViewModel(
            fetchRoutines = { reads.add(it) },
            updateRoutines = { body, callback -> writes.add(body to callback) },
        )
    }

    @After
    fun cleanup() {
        ArchTaskExecutor.getInstance().setDelegate(null)
    }

    private fun loaded() {
        model.loadRoutines()
        reads.removeAt(0)(ApiResult.Success(JSONObject().put("routines",
            org.json.JSONArray().put(first.toJson()).put(second.toJson()),
        )))
    }

    @Test
    fun failedFirstReadDoesNotAllowReplacingUnknownRobotRoutines() {
        model.loadRoutines()
        reads.removeAt(0)(ApiResult.Error("Sin conexión"))
        var accepted = true
        model.save(first, isNew = true) { ok, _ -> accepted = ok }
        assertFalse(accepted)
        assertTrue(writes.isEmpty())
        assertTrue(model.routines.value.orEmpty().isEmpty())
    }

    @Test
    fun aPendingReadDoesNotAllowWritingAnEmptySnapshot() {
        model.loadRoutines()
        model.save(first, isNew = true) { _, _ -> }
        assertTrue(writes.isEmpty())
    }

    @Test
    fun writesWaitForConfirmationAndCannotOverlap() {
        loaded()
        model.toggleRoutine(first.id)
        model.toggleRoutine(second.id)
        assertEquals(1, writes.size)
        assertTrue(model.routines.value!!.first().enabled)
        writes[0].second(ApiResult.Success(JSONObject().put("status", "ok")))
        assertFalse(model.routines.value!!.first().enabled)
        assertEquals("banio", model.routines.value!![1].pictogramId)
        model.toggleRoutine(second.id)
        assertEquals(2, writes.size)
        val sent = writes[1].first.getJSONArray("routines")
        assertFalse(sent.getJSONObject(0).getBoolean("enabled"))
        assertFalse(sent.getJSONObject(1).getBoolean("enabled"))
    }

    @Test
    fun aFailedWritePreservesConfirmedDataAndRequiresRefreshBeforeAnotherWrite() {
        loaded()
        model.toggleRoutine(first.id)
        writes[0].second(ApiResult.Error("Respuesta perdida"))
        model.toggleRoutine(second.id)
        assertEquals(1, writes.size)
        assertTrue(model.routines.value!!.all { it.enabled })
    }

    @Test
    fun aRefreshCannotRaceWithAnUnconfirmedWrite() {
        loaded()
        model.removeRoutine(first.id)
        model.loadRoutines()
        assertTrue(reads.isEmpty())
        writes[0].second(ApiResult.Success(JSONObject().put("status", "ok")))
        assertEquals(listOf(second.id), model.routines.value!!.map { it.id })
    }

    @Test
    fun aSuccessfulHttpResponseWithoutAcceptanceDoesNotCommitTheDraft() {
        loaded()
        model.removeRoutine(first.id)
        writes[0].second(ApiResult.Success(JSONObject().put("status", "rejected").put("reason", "No guardado")))
        assertEquals(listOf(first.id, second.id), model.routines.value!!.map { it.id })
        model.toggleRoutine(second.id)
        assertEquals(1, writes.size)
    }

    @Test
    fun invalidReadBodyDoesNotEnableDestructiveWrites() {
        model.loadRoutines()
        reads.removeAt(0)(ApiResult.Success(JSONObject().put("error", "No disponible")))
        model.save(first, isNew = true) { _, _ -> }
        assertTrue(writes.isEmpty())
    }

    @Test
    fun aSuccessfulRefreshAllowsAddingWithoutDroppingPreviouslyConfirmedItems() {
        model.loadRoutines()
        reads.removeAt(0)(ApiResult.Error("Sin conexión"))
        loaded()
        model.save(first.copy(id = "jardin", pictogramId = "jardin"), isNew = true) { _, _ -> }
        assertEquals(3, writes.single().first.getJSONArray("routines").length())
    }
}
