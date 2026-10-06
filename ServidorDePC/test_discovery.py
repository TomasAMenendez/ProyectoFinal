"""Beacon UDP y GET /discover, sin GPU y sin red."""
from __future__ import annotations

import inspect
import sys
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))

from discovery import beacon_log, beacon_payload, transmit


class _BadSocket:
    def __init__(self) -> None:
        self.closed = False

    def sendto(self, data: bytes, addr: tuple[str, int]) -> None:
        del data, addr
        raise OSError("red")

    def close(self) -> None:
        self.closed = True


class TestBeaconPayload(unittest.TestCase):
    def test_json_lleva_id_puerto_y_token(self) -> None:
        body = beacon_payload(8090, "tok")
        self.assertEqual(body["device_id"], "teo_pc_server")
        self.assertEqual(body["device_name"], "TEO ServidorDePC")
        self.assertEqual(body["api_port"], 8090)
        self.assertEqual(body["token"], "tok")
        self.assertNotIn("local_ip", body)

    def test_log_no_imprime_el_token_entero(self) -> None:
        token = "abcdefghijklmnopqrstuvwxyz"
        line = beacon_log(5556, token)
        self.assertIn("5556", line)
        self.assertIn(token[:8], line)
        self.assertNotIn(token, line)


class TestTransmit(unittest.TestCase):
    def test_fallo_udp_reabre_socket_sin_propagar(self) -> None:
        opened: list[object] = []

        def opener() -> object:
            fresh = object()
            opened.append(fresh)
            return fresh

        bad = _BadSocket()
        got = transmit(bad, b"{}", opener)  # type: ignore[arg-type]
        self.assertIs(got, opened[0])
        self.assertTrue(bad.closed)


class TestDiscoverHttp(unittest.TestCase):
    def test_discover_sin_auth_con_modelos_frios(self) -> None:
        import server as srv
        from fastapi.testclient import TestClient

        stt, llm, tts = srv.stt_engine.ready, srv.llm_engine.ready, srv.tts_engine.ready
        srv.stt_engine.ready = False
        srv.llm_engine.ready = False
        srv.tts_engine.ready = False
        try:
            client = TestClient(srv.create_app())
            resp = client.get("/discover")
            self.assertEqual(resp.status_code, 200)
            body = resp.json()
            self.assertEqual(body["device_id"], "teo_pc_server")
            self.assertEqual(body["api_port"], srv.PORT)
            self.assertEqual(body["token"], srv.TOKEN)
            health = client.get("/health")
            self.assertEqual(health.status_code, 401)
        finally:
            srv.stt_engine.ready = stt
            srv.llm_engine.ready = llm
            srv.tts_engine.ready = tts

    def test_main_arranca_el_beacon_antes_de_cargar_modelos(self) -> None:
        import server as srv

        src = inspect.getsource(srv.main)
        self.assertLess(src.index("beacon.start()"), src.index("stt_engine.load()"))
