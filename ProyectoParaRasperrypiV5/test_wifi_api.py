"""Tests del endpoint WiFi provisioning y flags de estado."""
from __future__ import annotations

import json
import tempfile
import threading
import unittest
from contextlib import contextmanager
from http.server import HTTPServer
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from api_server import RobotState
from child_profile import set_profile_path


@contextmanager
def _api_server():
    import api_server

    prev_token = api_server.robot_state.pairing_token
    prev_ble = api_server.robot_state.ble_setup_active
    prev_wifi = api_server.robot_state.wifi_connected
    prev_cb = api_server.robot_state.on_wifi_provisioning
    prev_scan = api_server.robot_state.on_wifi_scan
    prev_switch = api_server.robot_state.on_wifi_switch
    api_server.robot_state.pairing_token = ""
    httpd = HTTPServer(("127.0.0.1", 0), api_server.ApiRequestHandler)
    thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    thread.start()
    try:
        yield f"http://127.0.0.1:{httpd.server_address[1]}", api_server.robot_state
    finally:
        httpd.shutdown()
        httpd.server_close()
        thread.join(timeout=2)
        api_server.robot_state.pairing_token = prev_token
        api_server.robot_state.ble_setup_active = prev_ble
        api_server.robot_state.wifi_connected = prev_wifi
        api_server.robot_state.on_wifi_provisioning = prev_cb
        api_server.robot_state.on_wifi_scan = prev_scan
        api_server.robot_state.on_wifi_switch = prev_switch


def _http_json(method, url, body=None, *, raw_data: bytes | None = None):
    if raw_data is not None:
        data = raw_data
    elif body is not None:
        data = json.dumps(body).encode("utf-8")
    else:
        data = None
    hdrs = {"Content-Type": "application/json"} if data is not None else {}
    req = Request(url, data=data, headers=hdrs, method=method)
    try:
        with urlopen(req, timeout=5) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except HTTPError as exc:
        return exc.code, json.loads(exc.read().decode("utf-8") or "{}")


class TestWifiFlags(unittest.TestCase):
    def tearDown(self) -> None:
        set_profile_path(None)

    def test_to_dict_incluye_flags_wifi(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            rs = RobotState(prefs_path=Path(tmp) / "parental_prefs.json")
            data = rs.to_dict()
            self.assertIn("ble_setup_active", data)
            self.assertIn("wifi_connected", data)
            self.assertFalse(data["ble_setup_active"])
            self.assertFalse(data["wifi_connected"])


class TestWifiProvisioningApi(unittest.TestCase):
    def test_post_enabled_true_llama_callback(self) -> None:
        with _api_server() as (base, state):
            calls: list[bool] = []
            state.on_wifi_provisioning = calls.append
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {"enabled": True})
            self.assertEqual(code, 200, str(body))
            self.assertEqual(body, {"status": "ok", "enabled": True})
            self.assertTrue(state.ble_setup_active)
            self.assertEqual(calls, [True])

    def test_post_enabled_false_llama_callback(self) -> None:
        with _api_server() as (base, state):
            state.ble_setup_active = True
            calls: list[bool] = []
            state.on_wifi_provisioning = calls.append
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {"enabled": False})
            self.assertEqual(code, 200, str(body))
            self.assertEqual(body, {"status": "ok", "enabled": False})
            self.assertFalse(state.ble_setup_active)
            self.assertEqual(calls, [False])

    def test_post_callback_que_devuelve_true_200(self) -> None:
        with _api_server() as (base, state):
            state.on_wifi_provisioning = lambda enabled: True
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {"enabled": True})
            self.assertEqual(code, 200, str(body))
            self.assertEqual(body, {"status": "ok", "enabled": True})
            self.assertTrue(state.ble_setup_active)

    def test_post_sin_coordinator_devuelve_503(self) -> None:
        """I4: sin BLE no se puede prometer una pantalla que nunca progresa."""
        with _api_server() as (base, state):
            state.on_wifi_provisioning = None
            state.ble_setup_active = False
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {"enabled": True})
            self.assertEqual(code, 503)
            self.assertEqual(body.get("error"), "BLE no disponible")
            self.assertFalse(state.ble_setup_active)

    def test_post_callback_que_no_pudo_anunciar_devuelve_503(self) -> None:
        with _api_server() as (base, state):
            state.on_wifi_provisioning = lambda enabled: False
            state.ble_setup_active = False
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {"enabled": True})
            self.assertEqual(code, 503)
            self.assertEqual(body.get("error"), "BLE no disponible")
            self.assertFalse(state.ble_setup_active)

    def test_post_callback_que_revienta_devuelve_503(self) -> None:
        def boom(enabled: bool) -> bool:
            raise RuntimeError("bluetoothd caído")

        with _api_server() as (base, state):
            state.on_wifi_provisioning = boom
            state.ble_setup_active = False
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {"enabled": True})
            self.assertEqual(code, 503)
            self.assertFalse(state.ble_setup_active)

    def test_post_disable_sin_coordinator_devuelve_503(self) -> None:
        with _api_server() as (base, state):
            state.on_wifi_provisioning = None
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {"enabled": False})
            self.assertEqual(code, 503)
            self.assertEqual(body.get("error"), "BLE no disponible")

    def test_post_json_invalido_400(self) -> None:
        with _api_server() as (base, _state):
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", raw_data=b"{bad")
            self.assertEqual(code, 400)
            self.assertEqual(body.get("error"), "JSON inválido")

    def test_post_json_array_400(self) -> None:
        with _api_server() as (base, _state):
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", raw_data=b"[]")
            self.assertEqual(code, 400)
            self.assertEqual(body.get("error"), "JSON inválido")

    def test_post_json_null_400(self) -> None:
        with _api_server() as (base, _state):
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", raw_data=b"null")
            self.assertEqual(code, 400)
            self.assertEqual(body.get("error"), "JSON inválido")

    def test_post_json_string_400(self) -> None:
        with _api_server() as (base, _state):
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", raw_data=b'"x"')
            self.assertEqual(code, 400)
            self.assertEqual(body.get("error"), "JSON inválido")

    def test_post_sin_enabled_400(self) -> None:
        with _api_server() as (base, _state):
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {})
            self.assertEqual(code, 400)
            self.assertEqual(body.get("error"), "JSON inválido")

    def test_post_enabled_no_bool_400(self) -> None:
        with _api_server() as (base, _state):
            code, body = _http_json("POST", f"{base}/api/wifi/provisioning", {"enabled": "yes"})
            self.assertEqual(code, 400)
            self.assertEqual(body.get("error"), "JSON inválido")


class TestWifiChangeApi(unittest.TestCase):
    def test_get_networks_no_exige_cortar_wifi(self) -> None:
        with _api_server() as (base, state):
            seen: list[str] = []

            def scan() -> dict:
                seen.append("scan")
                return {
                    "current": "Casa",
                    "networks": [{"ssid": "Oficina", "signal": 70, "secure": True}],
                }

            state.on_wifi_scan = scan
            code, body = _http_json("GET", f"{base}/api/wifi/networks")
            self.assertEqual(code, 200, str(body))
            self.assertEqual(body["current"], "Casa")
            self.assertEqual(body["networks"][0]["ssid"], "Oficina")
            self.assertEqual(seen, ["scan"])
            self.assertFalse(state.ble_setup_active)

    def test_post_connect_ok_y_clave_mala(self) -> None:
        with _api_server() as (base, state):
            calls: list[tuple[str, str]] = []

            def switch(ssid: str, password: str) -> str:
                calls.append((ssid, password))
                return "auth" if password == "mala" else "ok"

            state.on_wifi_switch = switch
            code, body = _http_json(
                "POST",
                f"{base}/api/wifi/connect",
                {"ssid": "Oficina", "password": "mala"},
            )
            self.assertEqual(code, 400)
            self.assertEqual(body.get("error"), "clave incorrecta")
            code, body = _http_json(
                "POST",
                f"{base}/api/wifi/connect",
                {"ssid": "Oficina", "password": "bien"},
            )
            self.assertEqual(code, 200, str(body))
            self.assertEqual(calls, [("Oficina", "mala"), ("Oficina", "bien")])

    def test_sin_callback_503(self) -> None:
        with _api_server() as (base, state):
            state.on_wifi_scan = None
            state.on_wifi_switch = None
            code, _ = _http_json("GET", f"{base}/api/wifi/networks")
            self.assertEqual(code, 503)
            code, _ = _http_json(
                "POST",
                f"{base}/api/wifi/connect",
                {"ssid": "Oficina", "password": "x"},
            )
            self.assertEqual(code, 503)


if __name__ == "__main__":
    unittest.main()
