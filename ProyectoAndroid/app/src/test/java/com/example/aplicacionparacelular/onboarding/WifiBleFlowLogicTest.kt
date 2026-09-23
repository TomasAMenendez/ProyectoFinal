package com.example.aplicacionparacelular.onboarding

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lógica pura del provisionamiento WiFi por BLE: contrato de estados de la Pi,
 * garantía de respuesta única del scan de presencia, permiso denegado para
 * siempre y tolerancia de la respuesta de `POST /api/wifi/provisioning`.
 */
class WifiBleFlowLogicTest {

    // ------------------------------------------------------------------
    // Contrato de estados (los nueve que publica la Pi)
    // ------------------------------------------------------------------

    @Test fun contractHasExactlyTheNineStates() {
        assertEquals(
            listOf(
                "idle",
                "pin_ok",
                "scanning",
                "connecting",
                "connected",
                "failed:pin",
                "failed:session",
                "failed:auth",
                "failed:timeout",
            ),
            TeoBleStatus.CONTRACT,
        )
    }

    @Test fun everyContractStateMapsToItsOwnEffect() {
        assertEquals(TeoStatusEffect.WAIT_PIN, TeoBleStatus.effectOf("idle"))
        assertEquals(TeoStatusEffect.PIN_ACCEPTED, TeoBleStatus.effectOf("pin_ok"))
        assertEquals(TeoStatusEffect.SCANNING_NETWORKS, TeoBleStatus.effectOf("scanning"))
        assertEquals(TeoStatusEffect.CONNECTING_WIFI, TeoBleStatus.effectOf("connecting"))
        assertEquals(TeoStatusEffect.JOINED_WIFI, TeoBleStatus.effectOf("connected"))
        assertEquals(TeoStatusEffect.RETRY_PIN, TeoBleStatus.effectOf("failed:pin"))
        assertEquals(TeoStatusEffect.RESTART_SESSION, TeoBleStatus.effectOf("failed:session"))
        assertEquals(TeoStatusEffect.RETRY_PASSWORD, TeoBleStatus.effectOf("failed:auth"))
        assertEquals(TeoStatusEffect.SESSION_EXPIRED, TeoBleStatus.effectOf("failed:timeout"))
    }

    @Test fun noContractStateIsIgnored() {
        for (status in TeoBleStatus.CONTRACT) {
            assertFalse(status, TeoBleStatus.effectOf(status) == TeoStatusEffect.UNKNOWN)
        }
    }

    @Test fun statusIsNormalizedBeforeMapping() {
        assertEquals(TeoStatusEffect.PIN_ACCEPTED, TeoBleStatus.effectOf("  PIN_OK\n"))
        assertEquals(TeoStatusEffect.SESSION_EXPIRED, TeoBleStatus.effectOf("Failed:Timeout "))
    }

    @Test fun unknownFailureIsAnExplicitFailure() {
        // La Pi vieja mandaba estos; si alguno vuelve, tiene que ser error visible.
        assertEquals(TeoStatusEffect.UNKNOWN_FAILURE, TeoBleStatus.effectOf("failed:scan"))
        assertEquals(TeoStatusEffect.UNKNOWN_FAILURE, TeoBleStatus.effectOf("failed:payload"))
        assertEquals(TeoStatusEffect.UNKNOWN_FAILURE, TeoBleStatus.effectOf("failed:internal"))
        assertEquals(TeoStatusEffect.UNKNOWN_FAILURE, TeoBleStatus.effectOf("failed:cualquiera"))
    }

    @Test fun unknownNonFailureDegradesInsteadOfCrashing() {
        assertEquals(TeoStatusEffect.UNKNOWN, TeoBleStatus.effectOf("scan_ok"))
        assertEquals(TeoStatusEffect.UNKNOWN, TeoBleStatus.effectOf(""))
        assertEquals(TeoStatusEffect.UNKNOWN, TeoBleStatus.effectOf("¿?"))
    }

    // ------------------------------------------------------------------
    // Scan de presencia: onResult exactamente una vez
    // ------------------------------------------------------------------

    @Test fun busyPresenceScanAnswersFalseInsteadOfStayingSilent() {
        assertEquals(false, BlePresenceLogic.immediateAnswer(busy = true, scannerAvailable = true, permissionsGranted = true))
    }

    @Test fun presenceScanAnswersFalseWithoutAdapterOrPermission() {
        assertEquals(false, BlePresenceLogic.immediateAnswer(busy = false, scannerAvailable = false, permissionsGranted = true))
        assertEquals(false, BlePresenceLogic.immediateAnswer(busy = false, scannerAvailable = true, permissionsGranted = false))
    }

    @Test fun presenceScanRunsWhenEverythingIsReady() {
        assertNull(BlePresenceLogic.immediateAnswer(busy = false, scannerAvailable = true, permissionsGranted = true))
    }

    @Test fun singleShotDeliversOnlyTheFirstAnswer() {
        val seen = mutableListOf<Boolean>()
        val once = SingleShot<Boolean> { seen.add(it) }

        assertTrue(once.deliver(true))
        assertFalse(once.deliver(false))
        assertFalse(once.deliver(true))

        assertEquals(listOf(true), seen)
        assertTrue(once.isFired())
    }

    @Test fun singleShotIsUnfiredBeforeTheFirstDelivery() {
        val once = SingleShot<Boolean> { }
        assertFalse(once.isFired())
    }

    // ------------------------------------------------------------------
    // Permiso denegado para siempre
    // ------------------------------------------------------------------

    @Test fun permissionGrantedIsNotBlocked() {
        val grants = mapOf("BLUETOOTH_SCAN" to true, "BLUETOOTH_CONNECT" to true)
        assertFalse(BlePermissionUi.permanentlyDenied(grants) { true })
    }

    @Test fun deniedButStillExplainableIsNotBlocked() {
        val grants = mapOf("BLUETOOTH_SCAN" to false)
        assertFalse(BlePermissionUi.permanentlyDenied(grants) { true })
    }

    @Test fun deniedWithoutRationaleIsBlocked() {
        val grants = mapOf("BLUETOOTH_SCAN" to true, "BLUETOOTH_CONNECT" to false)
        assertTrue(BlePermissionUi.permanentlyDenied(grants) { false })
    }

    @Test fun emptyGrantsAreNotBlocked() {
        assertFalse(BlePermissionUi.permanentlyDenied(emptyMap()) { false })
    }

    // ------------------------------------------------------------------
    // Respuesta de POST /api/wifi/provisioning
    // ------------------------------------------------------------------

    @Test fun provisioningStartedWithTheHappyResponse() {
        assertTrue(WifiProvisioningResponse.startedOk(JSONObject("""{"status":"ok","enabled":true}""")))
    }

    @Test fun provisioningNotStartedWhenBleIsUnavailable() {
        assertFalse(WifiProvisioningResponse.startedOk(JSONObject("""{"status":"ok","enabled":false}""")))
        assertFalse(WifiProvisioningResponse.startedOk(JSONObject("""{"status":"error"}""")))
        assertFalse(WifiProvisioningResponse.startedOk(JSONObject("""{"error":"BLE no disponible"}""")))
        assertFalse(WifiProvisioningResponse.startedOk(JSONObject("""{"status":"ok","ble_available":false}""")))
        assertFalse(WifiProvisioningResponse.startedOk(JSONObject("""{"status":"ok","started":false}""")))
    }

    @Test fun provisioningToleratesStringsAndNumbers() {
        assertFalse(WifiProvisioningResponse.startedOk(JSONObject("""{"enabled":"false"}""")))
        assertFalse(WifiProvisioningResponse.startedOk(JSONObject("""{"enabled":0}""")))
        assertTrue(WifiProvisioningResponse.startedOk(JSONObject("""{"enabled":"true"}""")))
        assertTrue(WifiProvisioningResponse.startedOk(JSONObject("""{"enabled":1}""")))
    }

    @Test fun provisioningWithAnUnknownShapeIsAcceptedAsSuccess() {
        // Tolerancia: un cambio chico de contrato no tiene que romper la app.
        assertTrue(WifiProvisioningResponse.startedOk(JSONObject("{}")))
        assertTrue(WifiProvisioningResponse.startedOk(JSONObject("""{"algo":"nuevo"}""")))
    }

    @Test fun provisioningWithoutBodyIsAFailure() {
        assertFalse(WifiProvisioningResponse.startedOk(null))
    }

    // ------------------------------------------------------------------
    // Política de relectura de CHAR_SCAN (ping-pong con la Pi)
    // ------------------------------------------------------------------

    @Test fun firstPinOkWithoutListReadsTheNetworks() {
        assertEquals(
            ScanListAction.READ,
            WifiScanPolicy.onPinOk(hasNetworks = false, userChoosing = false, readInFlight = false, autoReads = 0),
        )
    }

    @Test fun pinOkWithAListNeverReadsAgain() {
        // El bug: cada `pin_ok` disparaba otro read, y cada read hacía que la Pi
        // volviera a escanear y a mandar `pin_ok`.
        assertEquals(
            ScanListAction.SETTLE,
            WifiScanPolicy.onPinOk(hasNetworks = true, userChoosing = false, readInFlight = false, autoReads = 1),
        )
    }

    @Test fun pinOkWhileTheParentChoosesDoesNotTouchTheScreen() {
        assertEquals(
            ScanListAction.KEEP,
            WifiScanPolicy.onPinOk(hasNetworks = true, userChoosing = true, readInFlight = false, autoReads = 1),
        )
        assertEquals(
            ScanListAction.KEEP,
            WifiScanPolicy.onPinOk(hasNetworks = false, userChoosing = true, readInFlight = false, autoReads = 0),
        )
    }

    @Test fun pinOkDoesNotPileUpReadsWhileOneIsInFlight() {
        assertEquals(
            ScanListAction.KEEP,
            WifiScanPolicy.onPinOk(hasNetworks = false, userChoosing = false, readInFlight = true, autoReads = 1),
        )
    }

    @Test fun emptyListStopsAutoReadingAfterTheBudget() {
        // La Pi puede quedarse con el cache vacío: reintentamos un par de veces
        // y después dejamos el botón "Buscar redes de nuevo" al padre.
        val budget = WifiScanPolicy.MAX_AUTO_READS
        assertEquals(
            ScanListAction.READ,
            WifiScanPolicy.onPinOk(hasNetworks = false, userChoosing = false, readInFlight = false, autoReads = budget - 1),
        )
        assertEquals(
            ScanListAction.SETTLE,
            WifiScanPolicy.onPinOk(hasNetworks = false, userChoosing = false, readInFlight = false, autoReads = budget),
        )
    }

    @Test fun scanningWithAUsableListOnlyHints() {
        assertEquals(
            ScanListAction.REFRESH_HINT,
            WifiScanPolicy.onScanning(hasNetworks = true, userChoosing = false),
        )
    }

    @Test fun scanningWithoutListShowsTheSpinner() {
        assertEquals(
            ScanListAction.WAIT,
            WifiScanPolicy.onScanning(hasNetworks = false, userChoosing = false),
        )
    }

    @Test fun scanningWhileTheParentChoosesDoesNotTouchTheScreen() {
        assertEquals(
            ScanListAction.KEEP,
            WifiScanPolicy.onScanning(hasNetworks = true, userChoosing = true),
        )
        assertEquals(
            ScanListAction.KEEP,
            WifiScanPolicy.onScanning(hasNetworks = false, userChoosing = true),
        )
    }

    @Test fun manualRefreshAlwaysReads() {
        assertTrue(WifiScanPolicy.allowsManualRead(readInFlight = false))
        assertFalse(WifiScanPolicy.allowsManualRead(readInFlight = true))
    }

    @Test fun anEmptyAnswerDoesNotErasetheListOnScreen() {
        val shown = listOf(WifiNetworkUi("Casa", 80, true))
        // Read automático con respuesta vacía: se conserva lo que ya se ve.
        assertEquals(shown, WifiScanPolicy.listToShow(shown, emptyList(), manual = false))
        // Refresh pedido a mano: manda la respuesta, aunque venga vacía.
        assertEquals(emptyList<WifiNetworkUi>(), WifiScanPolicy.listToShow(shown, emptyList(), manual = true))
        // Con redes nuevas siempre gana la respuesta.
        val fresh = listOf(WifiNetworkUi("Vecino", 40, true))
        assertEquals(fresh, WifiScanPolicy.listToShow(shown, fresh, manual = false))
    }
}
