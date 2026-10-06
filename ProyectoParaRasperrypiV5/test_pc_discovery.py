"""Hallazgo de ServidorDePC: UDP, barrido y ciclo. Sin red."""
from __future__ import annotations

import json
import socket
import unittest
from pathlib import Path

from pc_discovery import (
    PcDiscovery,
    announce_from_packet,
    classify_probe,
    default_scan,
    guarded_tick,
    listen_for_pc,
    scan_subnet,
    should_start,
    start_discovery,
)
from pc_server_client import PcServerClient

ROOT = Path(__file__).resolve().parent
WORKERS = ROOT / "workers.py"


def _packet(port: int = 8090, token: str = "tok", device_id: str = "teo_pc_server") -> bytes:
    return json.dumps(
        {
            "device_id": device_id,
            "device_name": "TEO ServidorDePC",
            "api_port": port,
            "token": token,
            "local_ip": "1.2.3.4",
        }
    ).encode("utf-8")


class _FakeSock:
    def __init__(self, packets: list[tuple[bytes, tuple[str, int]]]) -> None:
        self.packets = list(packets)
        self.timeouts: list[float] = []

    def settimeout(self, value: float) -> None:
        self.timeouts.append(value)

    def recvfrom(self, size: int) -> tuple[bytes, tuple[str, int]]:
        del size
        if not self.packets:
            raise socket.timeout()
        return self.packets.pop(0)


class _BoomFile:
    def write_text(self, text: str, encoding: str = "utf-8") -> None:
        del text, encoding
        raise OSError("disco")


class TestAnnounce(unittest.TestCase):
    def test_usa_ip_de_origen(self) -> None:
        found = announce_from_packet(_packet(), "10.0.0.8")
        self.assertEqual(found, ("http://10.0.0.8:8090", "tok"))

    def test_descarta_ajeno_token_vacio_puerto_e_ip(self) -> None:
        self.assertIsNone(announce_from_packet(_packet(device_id="otro"), "10.0.0.8"))
        self.assertIsNone(announce_from_packet(_packet(token="  "), "10.0.0.8"))
        self.assertIsNone(announce_from_packet(_packet(port=0), "10.0.0.8"))
        self.assertIsNone(announce_from_packet(_packet(port=70000), "10.0.0.8"))
        self.assertIsNone(announce_from_packet(b"no-json", "10.0.0.8"))
        self.assertIsNone(announce_from_packet(_packet(), "0.0.0.0"))
        self.assertIsNone(announce_from_packet(_packet(), "127.0.0.1"))
        self.assertIsNone(announce_from_packet(_packet(), "255.255.255.255"))

    def test_listen_ignora_el_malo_y_toma_el_bueno(self) -> None:
        times = iter([0.0, 0.0, 1.0, 1.0])
        sock = _FakeSock(
            [
                (b'{"device_id":"otro"}', ("9.9.9.9", 5556)),
                (_packet(token="abc"), ("10.1.1.5", 5556)),
            ]
        )
        found = listen_for_pc(sock, window_s=3.0, now=lambda: next(times))
        self.assertEqual(found, ("http://10.1.1.5:8090", "abc"))


class TestScan(unittest.TestCase):
    def test_barrido_usa_la_ip_que_respondio_y_salta_la_propia(self) -> None:
        seen: list[str] = []

        def fetch(ip: str) -> tuple[int, bytes]:
            seen.append(ip)
            if ip == "192.168.1.9":
                return 200, _packet(token="scan-tok", port=8090)
            raise OSError("no")

        found = scan_subnet("192.168.1", "192.168.1.20", fetch)
        self.assertEqual(found, ("http://192.168.1.9:8090", "scan-tok"))
        self.assertNotIn("192.168.1.20", seen)

    def test_sin_ip_de_lan_no_barre(self) -> None:
        self.assertIsNone(default_scan(lan_ip_fn=lambda: None))

    def test_respuesta_ajena_no_cuenta(self) -> None:
        def fetch(ip: str) -> tuple[int, bytes]:
            if ip == "10.0.0.4":
                return 200, _packet(device_id="router")
            raise OSError("no")

        self.assertIsNone(scan_subnet("10.0.0", "10.0.0.2", fetch))


class TestEnv(unittest.TestCase):
    def test_env_completo_o_parcial_no_arranca(self) -> None:
        self.assertFalse(should_start({"PC_SERVER_URL": "http://10.0.0.2:8090", "PC_SERVER_TOKEN": "t"}))
        self.assertFalse(should_start({"PC_SERVER_URL": "http://10.0.0.2:8090"}))
        self.assertFalse(should_start({"PC_SERVER_TOKEN": "t"}))
        self.assertTrue(should_start({}))

    def test_start_discovery_no_pisa_el_cliente(self) -> None:
        client = PcServerClient("http://10.0.0.2:8090", "manual", cache_path=_BoomFile(), token_path=_BoomFile())
        thread = start_discovery(
            client,
            env={"PC_SERVER_URL": "http://10.0.0.2:8090", "PC_SERVER_TOKEN": "manual"},
        )
        self.assertIsNone(thread)
        self.assertEqual(client.base_url, "http://10.0.0.2:8090")
        self.assertEqual(client.token, "manual")


class TestCycle(unittest.TestCase):
    def _client(self, url: str = "http://10.0.0.2:8090", token: str = "viejo") -> PcServerClient:
        return PcServerClient(url, token, cache_path=_BoomFile(), token_path=_BoomFile())

    def _discovery(
        self,
        client: PcServerClient,
        kind: str,
        found: tuple[str, str] | None,
    ) -> tuple[PcDiscovery, dict[str, int]]:
        counts = {"probe": 0, "listen": 0, "scan": 0, "sleep": 0}

        def probe(_client: PcServerClient) -> str:
            counts["probe"] += 1
            return kind

        def listen() -> tuple[str, str] | None:
            counts["listen"] += 1
            return found

        def scan() -> tuple[str, str] | None:
            counts["scan"] += 1
            return None

        disc = PcDiscovery(
            client,
            listen_fn=listen,
            scan_fn=scan,
            probe_fn=probe,
            sleep_fn=lambda _s: counts.__setitem__("sleep", counts["sleep"] + 1),
            log_fn=lambda _m: None,
            retry_s=15.0,
        )
        return disc, counts

    def test_200_y_503_dejan_la_url(self) -> None:
        for kind in ("ok", "loading"):
            client = self._client()
            disc, counts = self._discovery(client, kind, ("http://10.9.9.9:8090", "nuevo"))
            self.assertEqual(disc.tick(), "keep")
            self.assertEqual(client.base_url, "http://10.0.0.2:8090")
            self.assertEqual(counts["listen"], 0)

    def test_un_timeout_no_busca_dos_si(self) -> None:
        client = self._client()
        disc, counts = self._discovery(client, "timeout", ("http://10.9.9.9:8090", "nuevo"))
        self.assertEqual(disc.tick(), "wait-timeout")
        self.assertEqual(counts["listen"], 0)
        self.assertEqual(disc.tick(), "search-hit")
        self.assertEqual(client.base_url, "http://10.9.9.9:8090")
        self.assertEqual(client.token, "nuevo")

    def test_401_y_rechazo_buscan_enseguida(self) -> None:
        for kind in ("unauthorized", "refused", "unreachable"):
            client = self._client()
            disc, counts = self._discovery(client, kind, None)
            self.assertEqual(disc.tick(), "search-miss")
            self.assertEqual(counts["probe"], 1)
            self.assertEqual(counts["listen"], 1)
            self.assertEqual(counts["scan"], 1)
            disc.tick()
            self.assertEqual(counts["probe"], 1)
            self.assertEqual(counts["listen"], 2)

    def test_sin_udp_corre_el_barrido(self) -> None:
        client = PcServerClient("", "", cache_path=_BoomFile(), token_path=_BoomFile())
        seen = {"scan": 0}

        def listen() -> tuple[str, str] | None:
            return None

        def scan() -> tuple[str, str] | None:
            seen["scan"] += 1
            return ("http://10.4.4.4:8090", "hallado")

        disc = PcDiscovery(
            client,
            listen_fn=listen,
            scan_fn=scan,
            probe_fn=lambda _c: "ok",
            sleep_fn=lambda _s: None,
            log_fn=lambda _m: None,
        )
        self.assertEqual(disc.tick(), "search-hit")
        self.assertEqual(seen["scan"], 1)
        self.assertEqual(client.base_url, "http://10.4.4.4:8090")

    def test_socket_udp_roto_pasa_al_barrido(self) -> None:
        client = PcServerClient("", "", cache_path=_BoomFile(), token_path=_BoomFile())

        def listen() -> tuple[str, str] | None:
            raise OSError("bind")

        def scan() -> tuple[str, str] | None:
            return ("http://10.4.4.4:8090", "hallado")

        disc = PcDiscovery(
            client,
            listen_fn=listen,
            scan_fn=scan,
            probe_fn=lambda _c: "ok",
            sleep_fn=lambda _s: None,
            log_fn=lambda _m: None,
        )
        self.assertEqual(disc.tick(), "search-hit")
        self.assertEqual(client.token, "hallado")

    def test_adopt_cierra_el_circuit_aunque_no_escriba(self) -> None:
        client = self._client()
        client.fail_count = 2
        client.open_until = 10_000.0
        client.fail_count_tts = 2
        client.open_until_tts = 10_000.0
        disc = PcDiscovery(client, log_fn=lambda _m: None, sleep_fn=lambda _s: None)
        disc.adopt("http://10.8.8.8:8090/", "  nuevo-token  ")
        self.assertEqual(client.base_url, "http://10.8.8.8:8090")
        self.assertEqual(client.token, "nuevo-token")
        self.assertEqual(client.fail_count, 0)
        self.assertEqual(client.open_until, 0.0)
        self.assertEqual(client.fail_count_tts, 0)
        self.assertEqual(client.open_until_tts, 0.0)

    def test_adopt_escribe_url_y_token(self) -> None:
        import tempfile

        folder = Path(tempfile.mkdtemp())
        url_path = folder / "pc_server_url.txt"
        token_path = folder / "pc_server_token.txt"
        client = PcServerClient("http://10.0.0.2:8090", "viejo", cache_path=url_path, token_path=token_path)
        disc = PcDiscovery(client, log_fn=lambda _m: None, sleep_fn=lambda _s: None)
        disc.adopt("http://10.8.8.8:8090", "nuevo-token")
        self.assertEqual(url_path.read_text(encoding="utf-8").strip(), "http://10.8.8.8:8090")
        self.assertEqual(token_path.read_text(encoding="utf-8").strip(), "nuevo-token")

    def test_guarded_tick_no_propaga(self) -> None:
        client = self._client()
        slept: list[float] = []

        def probe(_client: PcServerClient) -> str:
            raise RuntimeError("boom")

        disc = PcDiscovery(
            client,
            probe_fn=probe,
            sleep_fn=slept.append,
            log_fn=lambda _m: None,
            retry_s=15.0,
        )
        guarded_tick(disc)
        self.assertEqual(slept, [15.0])


class TestClassify(unittest.TestCase):
    def test_timeout_refused_y_status(self) -> None:
        self.assertEqual(classify_probe(None, TimeoutError()), "timeout")
        self.assertEqual(classify_probe(None, socket.timeout()), "timeout")
        self.assertEqual(classify_probe(None, ConnectionRefusedError()), "refused")
        self.assertEqual(classify_probe(200, None), "ok")
        self.assertEqual(classify_probe(503, None), "loading")
        self.assertEqual(classify_probe(401, None), "unauthorized")
        self.assertEqual(classify_probe(None, OSError("x")), "unreachable")


class TestVozNoBarre(unittest.TestCase):
    def test_workers_no_importa_el_hallazgo(self) -> None:
        text = WORKERS.read_text(encoding="utf-8")
        self.assertNotIn("pc_discovery", text)
        self.assertNotIn("scan_subnet", text)
