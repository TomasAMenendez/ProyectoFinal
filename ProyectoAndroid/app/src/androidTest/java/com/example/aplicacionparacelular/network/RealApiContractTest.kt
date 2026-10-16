package com.example.aplicacionparacelular.network

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.aplicacionparacelular.ui.routines.RoutineItem
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Contratos Android ↔ Python reales; únicamente los efectos de hardware se simulan. */
@RunWith(AndroidJUnit4::class)
class RealApiContractTest {
    private lateinit var base: Context
    private lateinit var isolated: Context
    private lateinit var fixtureUrl: String
    private val preferenceNames = mutableSetOf<String>()
    private var fixtureActive = false
    private var pdfSupported = false

    @Before
    fun useExplicitFixtureWithIsolatedPreferences() {
        val argument = InstrumentationRegistry.getArguments().getString("teoApiPort")
        assumeTrue("Prueba opcional: requiere -e teoApiPort con la API temporal activa", argument != null)
        val port = argument?.toIntOrNull()
        require(port != null && port in 1..65535) { "teoApiPort debe ser un puerto válido" }
        fixtureUrl = "http://localhost:$port"
        val health = request("/__fixture/health").json
        assertEquals("teo-real-api-contract-v1", health.getString("fixture"))
        assertEquals("simulated_callbacks", health.getString("hardware"))
        assertEquals("temporary", health.getString("storage"))
        pdfSupported = health.getBoolean("pdf_supported")
        assertEquals(200, request("/__fixture/reset", "POST", JSONObject()).code)

        base = InstrumentationRegistry.getInstrumentation().targetContext
        val namespace = "real-api-test-${UUID.randomUUID()}"
        isolated = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                val isolatedName = "$namespace-$name"
                preferenceNames.add(isolatedName)
                return base.getSharedPreferences(isolatedName, mode)
            }
        }
        fixtureActive = true
        RobotApiClient.init(isolated)
        // El cliente descarta 127.* como IP de beacon; localhost alcanza adb reverse.
        RobotApiClient.setRobotAddress(isolated, "localhost", port)
        RobotApiClient.setPairingToken(isolated, TEST_TOKEN)
    }

    @After
    fun restoreTheRealClientWithoutChangingItsPreferences() {
        if (!fixtureActive) return
        try {
            RobotApiClient.init(base)
        } finally {
            fixtureActive = false
            preferenceNames.forEach { base.deleteSharedPreferences(it) }
            preferenceNames.clear()
        }
    }

    @Test
    fun statusIsPublicButPrivateEndpointsRequireThePairingToken() {
        val status = success(RobotApiClient.getStatus())
        assertTrue(status.getBoolean("models_ready"))
        assertTrue(status.getBoolean("power_on"))
        assertNotNull(ParentInbox.parseEventMillis(status.getString("timestamp")))
        RobotApiClient.setPairingToken(isolated, "incorrect-test-token")
        try {
            success(RobotApiClient.getStatus())
            assertTrue(RobotApiClient.getRoutines() is ApiResult.Error)
            assertEquals(401, request("/api/routines", token = "incorrect-test-token").code)
        } finally {
            RobotApiClient.setPairingToken(isolated, TEST_TOKEN)
        }
        success(RobotApiClient.getRoutines())
    }

    @Test
    fun theNineCostumesRoundTripThroughTheRealProfileValidationAndStorage() {
        val costumes = listOf("oso", "perro", "oveja", "dinosaurio", "chancho", "tiburon", "stitch", "spiderman", "chimuelo")
        costumes.forEach { costume ->
            val body = JSONObject()
                .put("name", "Perfil temporal")
                .put("gender", "no_decir")
                .put("interests", "Cuentos y música")
                .put("costume", costume)
                .put("extra", "Datos ficticios de integración")
            val saved = success(RobotApiClient.putChildProfile(body))
            assertTrue(saved.getBoolean("complete"))
            assertEquals(costume, saved.getString("costume"))
            val loaded = success(RobotApiClient.getChildProfile())
            assertEquals("Perfil temporal", loaded.getString("name"))
            assertEquals(costume, loaded.getString("costume"))
            assertNotNull(ParentInbox.parseEventMillis(loaded.getString("updated_at")))
        }
        assertTrue(RobotApiClient.putChildProfile(JSONObject().put("name", "").put("gender", "nene")) is ApiResult.Error)
        assertEquals("chimuelo", success(RobotApiClient.getChildProfile()).getString("costume"))
    }

    @Test
    fun routinesKeepTheirOptionalPictogramWhenTheRealSchedulerReloads() {
        val withPicture = routine("with-picture", "Cepillar los dientes", "08:15", "cepillar_dientes")
        val withoutPicture = routine("without-picture", "Descansar", "20:30", null)
        success(RobotApiClient.postRoutines(JSONObject().put("routines", JSONArray().put(withPicture.toJson()).put(withoutPicture.toJson()))))
        val returned = success(RobotApiClient.getRoutines()).getJSONArray("routines")
        assertEquals(2, returned.length())
        assertEquals("cepillar_dientes", RoutineItem.fromJson(returned.getJSONObject(0)).pictogramId)
        assertNull(RoutineItem.fromJson(returned.getJSONObject(1)).pictogramId)
        assertEquals("08:15", returned.getJSONObject(0).getString("time"))
        assertFalse(returned.getJSONObject(1).has("pictogram_id"))
        assertTrue(returned.getJSONObject(0).has("reminded"))
        assertTrue(returned.getJSONObject(0).has("completed"))
    }

    @Test
    fun sensoryChangesPersistWithoutOverwritingTimeLimitOrBellyWake() {
        success(RobotApiClient.postConfig(85, 0.85f, 7))
        success(RobotApiClient.postBellyWake(false))
        success(RobotApiClient.postSensory(35, 45))
        assertEquals(200, request("/__fixture/reload-state", "POST", JSONObject()).code)
        val status = success(RobotApiClient.getStatus())
        assertEquals(35, status.getInt("volume_limit"))
        assertEquals(0.45, status.getDouble("brightness"), 0.001)
        assertEquals(7, status.getInt("playtime_limit_minutes"))
        assertFalse(status.getBoolean("belly_wake_enabled"))
        success(RobotApiClient.postNightMode(true))
        assertTrue(success(RobotApiClient.getStatus()).getBoolean("night_mode"))
    }

    @Test
    fun aValidWaveWithSpacesAccentsPlusAndPercentCanBeUploadedListedAndDeleted() {
        val filename = "canción + % prueba #1.wav"
        val audio = smallWave()
        val uploaded = success(RobotApiClient.uploadMusic(filename, audio))
        assertEquals(filename, uploaded.getString("filename"))
        assertEquals(audio.size, uploaded.getInt("size_bytes"))
        val songs = success(RobotApiClient.getMusic()).getJSONArray("songs")
        assertEquals(1, songs.length())
        assertEquals(filename, songs.getJSONObject(0).getString("filename"))
        success(RobotApiClient.playMusic(filename))
        awaitSimulatedEvent("music_play", filename)
        success(RobotApiClient.stopMusic())
        success(RobotApiClient.deleteMusic(filename))
        assertEquals(0, success(RobotApiClient.getMusic()).getJSONArray("songs").length())
    }

    @Test
    fun sleepingDeclinesPlaybackAndCelebrationUntilTheRealStateWakes() {
        assertFalse(success(RobotApiClient.postPower(false)).getBoolean("power_on"))
        val music = success(RobotApiClient.playMusic("no-audio.wav"))
        assertFalse(music.getBoolean("power_on"))
        assertFalse(success(RobotApiClient.postCelebrate()).getBoolean("power_on"))
        assertFalse(success(RobotApiClient.playStory("no-story")).getBoolean("power_on"))
        assertFalse(eventKinds().contains("music_play"))
        assertFalse(eventKinds().contains("story_play"))
        assertFalse(eventKinds().contains("celebrate"))
        val awake = success(RobotApiClient.postPower(true))
        assertTrue(awake.getBoolean("power_on"))
        assertEquals("woke", awake.getString("wake_result"))
        success(RobotApiClient.postCelebrate())
        assertTrue(eventKinds().contains("celebrate"))
    }

    @Test
    fun parentNoticesHaveRealTimestampsAndAreConsumedOnce() {
        val incoming = JSONArray()
        listOf("crisis", "pedido", "logro", "aviso", "rutina", "info").forEach { type ->
            incoming.put(JSONObject().put("type", type).put("message", "Aviso ficticio: $type"))
        }
        assertEquals(4, request("/__fixture/notifications", "POST", JSONObject().put("notifications", incoming)).json.getInt("count"))
        val response = success(RobotApiClient.getNotifications())
        assertEquals(4, response.getInt("count"))
        val notices = response.getJSONArray("notifications")
        val types = mutableSetOf<String>()
        for (i in 0 until notices.length()) {
            val notice = notices.getJSONObject(i)
            val parsed = ParentInbox.notice(notice.getString("type"), notice.getString("message"), notice.getString("timestamp"), 1L)
            assertNotNull(parsed)
            assertNotNull(parsed!!.eventAtMillis)
            types.add(parsed.type)
        }
        assertEquals(setOf("crisis", "pedido", "logro", "aviso"), types)
        assertEquals(0, success(RobotApiClient.getNotifications()).getInt("count"))
    }

    @Test
    fun aRealPdfIsParsedPublishedUpdatedAndDeleted() {
        assumeTrue("El runtime Python necesita pypdf para verificar la extracción real de PDF", pdfSupported)
        val uploaded = success(RobotApiClient.uploadStory("cuento + % niñez.pdf", smallStoryPdf(), "La aventura + del jardín"))
        assertEquals("PDF rechazado: ${uploaded.optString("reason")}", "ready", uploaded.optString("status"))
        val id = uploaded.getString("id")
        try {
            assertEquals("La aventura + del jardín", uploaded.getString("title"))
            assertTrue(uploaded.getInt("word_count") >= 80)
            val story = success(RobotApiClient.getStory(id))
            assertTrue(story.getString("text").contains("Habia una vez"))
            assertEquals(1, success(RobotApiClient.getStories()).getJSONArray("stories").length())
            success(RobotApiClient.updateStory(id, title = "Nuevo título + %"))
            assertEquals("Nuevo título + %", success(RobotApiClient.getStory(id)).getString("title"))
            success(RobotApiClient.playStory(id))
            assertTrue(eventKinds().contains("story_play"))
            success(RobotApiClient.stopStory())
        } finally {
            success(RobotApiClient.deleteStory(id))
        }
        assertEquals(0, success(RobotApiClient.getStories()).getJSONArray("stories").length())
    }

    private fun routine(id: String, name: String, time: String, pictogram: String?) = RoutineItem(
        id, name, time, "Recordatorio de prueba", "jugar", name, "Muy bien", 5, true, pictogramId = pictogram,
    )

    private fun success(result: ApiResult<JSONObject>): JSONObject = when (result) {
        is ApiResult.Success -> result.data
        is ApiResult.Error -> throw AssertionError("La API real de prueba devolvió: ${result.message}")
    }

    private data class Reply(val code: Int, val json: JSONObject)

    private fun request(path: String, method: String = "GET", body: JSONObject? = null, token: String = TEST_TOKEN): Reply {
        val connection = URL("$fixtureUrl$path").openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = 3000
            connection.readTimeout = 5000
            connection.setRequestProperty("X-Robot-Token", token)
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val code = connection.responseCode
            val stream = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?: throw AssertionError("La API temporal devolvió HTTP $code sin cuerpo")
            val json = stream.bufferedReader(Charsets.UTF_8).use { JSONObject(it.readText()) }
            return Reply(code, json)
        } finally {
            connection.disconnect()
        }
    }

    private fun eventKinds(): List<String> {
        val events = request("/__fixture/events").json.getJSONArray("events")
        return (0 until events.length()).map { events.getJSONObject(it).getString("kind") }
    }

    private fun awaitSimulatedEvent(kind: String, value: String) {
        val deadline = System.nanoTime() + 2_000_000_000L
        do {
            val events = request("/__fixture/events").json.getJSONArray("events")
            for (i in 0 until events.length()) {
                val event = events.getJSONObject(i)
                if (event.optString("kind") == kind && event.optString("value") == value) {
                    assertEquals("simulated", event.getString("hardware"))
                    return
                }
            }
            Thread.sleep(25)
        } while (System.nanoTime() < deadline)
        fail("No se observó el callback simulado $kind")
    }

    private fun smallWave(): ByteArray {
        val samples = 80
        return ByteBuffer.allocate(44 + samples * 2).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII)); putInt(36 + samples * 2)
            put("WAVEfmt ".toByteArray(Charsets.US_ASCII)); putInt(16)
            putShort(1); putShort(1); putInt(8000); putInt(16000); putShort(2); putShort(16)
            put("data".toByteArray(Charsets.US_ASCII)); putInt(samples * 2)
            repeat(samples) { putShort(0) }
        }.array()
    }

    private fun smallStoryPdf(): ByteArray {
        val lines = listOf(
            "Habia una vez un conejo que vivia junto a un bosque lleno de flores.",
            "Cada manana caminaba despacio para saludar a sus amigos del jardin.",
            "Un dia encontro una semilla pequena y decidio cuidarla con paciencia.",
            "Entonces el conejo preparo la tierra mientras una tortuga traia agua.",
            "Sus amigos cantaron una cancion suave para acompanar aquel trabajo.",
            "Al pasar los dias aparecio una planta verde con hojas muy bonitas.",
            "El conejo dijo que compartir los cuidados hacia mas alegre la espera.",
            "Todos sonrieron y descansaron bajo la sombra al terminar la tarde.",
            "Desde aquel dia volvieron juntos al jardin para cuidar nuevas flores.",
        )
        val stream = "BT /F1 12 Tf 50 750 Td 16 TL\n" + lines.joinToString("\n") { "($it) Tj T*" } + "\nET\n"
        val objects = listOf(
            "<< /Type /Catalog /Pages 2 0 R >>",
            "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
            "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
            "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
            "<< /Length ${stream.toByteArray(Charsets.US_ASCII).size} >>\nstream\n${stream}endstream",
        )
        val output = ByteArrayOutputStream()
        fun write(text: String) { output.write(text.toByteArray(Charsets.US_ASCII)) }
        write("%PDF-1.4\n")
        val offsets = mutableListOf<Int>()
        objects.forEachIndexed { index, content ->
            offsets.add(output.size())
            write("${index + 1} 0 obj\n$content\nendobj\n")
        }
        val xref = output.size()
        write("xref\n0 6\n0000000000 65535 f \n")
        offsets.forEach { write(it.toString().padStart(10, '0') + " 00000 n \n") }
        write("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        return output.toByteArray()
    }

    companion object {
        private const val TEST_TOKEN = "teo-integration-only-not-a-device-token"
    }
}
