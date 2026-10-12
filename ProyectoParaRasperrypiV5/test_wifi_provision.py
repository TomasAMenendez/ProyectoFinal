"""Tests TDD de wifi_provision (parseo nmcli, sin radio real)."""
from __future__ import annotations

import json
import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from wifi_provision import (
    WifiCommandError,
    WifiCommandTimeout,
    WifiNetwork,
    WifiProvisioner,
    _default_runner,
    classify_nmcli_connect,
    load_known_networks,
    parse_nmcli_active_wifi_name,
    parse_nmcli_added_uuid,
    parse_nmcli_connection_rows,
    parse_nmcli_device_wifi_connected,
    parse_nmcli_wifi_list,
)

SECRET = "clave-super-secreta"


NMCLI_WIFI_FIXTURE = """\
Casa:80:WPA2
Casa:40:WPA2
Directa:70:
--:10:WPA2
"""


class TestParseNmcliWifiList(unittest.TestCase):
    def test_dedupe_by_ssid_max_signal_and_security(self) -> None:
        nets = parse_nmcli_wifi_list(NMCLI_WIFI_FIXTURE)
        self.assertEqual(len(nets), 2)
        by_ssid = {n.ssid: n for n in nets}
        self.assertEqual(by_ssid["Casa"].signal, 80)
        self.assertTrue(by_ssid["Casa"].secure)
        self.assertEqual(by_ssid["Directa"].signal, 70)
        self.assertFalse(by_ssid["Directa"].secure)

    def test_ssid_con_dos_puntos_escapados(self) -> None:
        nets = parse_nmcli_wifi_list("My\\:Net:80:WPA2\n")
        self.assertEqual(len(nets), 1)
        self.assertEqual(nets[0].ssid, "My:Net")
        self.assertEqual(nets[0].signal, 80)
        self.assertTrue(nets[0].secure)

    def test_ssid_con_backslash_escapado(self) -> None:
        nets = parse_nmcli_wifi_list("Casa\\\\Wifi:55:\n")
        self.assertEqual(len(nets), 1)
        self.assertEqual(nets[0].ssid, "Casa\\Wifi")
        self.assertFalse(nets[0].secure)

    def test_skip_empty_ssid(self) -> None:
        text = ":50:WPA2\nValid:60:WPA2\n"
        nets = parse_nmcli_wifi_list(text)
        self.assertEqual(len(nets), 1)
        self.assertEqual(nets[0].ssid, "Valid")


class TestParseNmcliDeviceWifiConnected(unittest.TestCase):
    def test_connected(self) -> None:
        self.assertTrue(parse_nmcli_device_wifi_connected("wlan0:wifi:connected\n"))

    def test_disconnected(self) -> None:
        self.assertFalse(parse_nmcli_device_wifi_connected("wlan0:wifi:disconnected\n"))


class TestClassifyNmcliConnect(unittest.TestCase):
    def test_ok(self) -> None:
        self.assertEqual(classify_nmcli_connect(0, "", 2.0), "ok")

    def test_auth_secrets_required(self) -> None:
        stderr = "Error: Connection activation failed: Secrets were required.\n"
        self.assertEqual(classify_nmcli_connect(1, stderr, 2.0), "auth")

    def test_espera_de_nmcli_es_clave_rechazada(self) -> None:
        stderr = "Error: Timeout expired\n"
        self.assertEqual(classify_nmcli_connect(1, stderr, 20.0), "auth")

    def test_key_mgmt_missing_no_es_clave_mala(self) -> None:
        stderr = "802-11-wireless-security.key-mgmt: property is missing"
        self.assertEqual(classify_nmcli_connect(1, stderr, 2.0), "error")

    def test_timeout(self) -> None:
        self.assertEqual(classify_nmcli_connect(1, "timeout", 30.0, timeout_s=30.0), "timeout")
        self.assertEqual(classify_nmcli_connect(1, "timeout", 31.0, timeout_s=30.0), "timeout")


class TestWifiProvisioner(unittest.TestCase):
    def test_is_connected_uses_device_list(self) -> None:
        calls: list[list[str]] = []

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            calls.append(argv)
            return 0, "wlan0:wifi:connected\n", ""

        prov = WifiProvisioner(runner=runner)
        self.assertTrue(prov.is_connected())
        self.assertEqual(calls, [["nmcli", "-t", "-f", "DEVICE,TYPE,STATE", "device"]])

    def test_scan_rescan_then_list(self) -> None:
        calls: list[list[str]] = []

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            calls.append(argv)
            if "--rescan" in argv:
                return 1, "", "rescan failed"
            return 0, NMCLI_WIFI_FIXTURE, ""

        prov = WifiProvisioner(runner=runner)
        nets = prov.scan()
        self.assertEqual(len(nets), 2)
        self.assertEqual(
            calls[0],
            [
                "nmcli",
                "-t",
                "-f",
                "SSID,SIGNAL,SECURITY",
                "dev",
                "wifi",
                "list",
                "--rescan",
                "yes",
            ],
        )
        self.assertEqual(
            calls[1],
            ["nmcli", "-t", "-f", "SSID,SIGNAL,SECURITY", "dev", "wifi"],
        )

    def test_connect_with_password(self) -> None:
        calls: list[list[str]] = []

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            calls.append(argv)
            return 0, "", ""

        prov = WifiProvisioner(runner=runner)
        result = prov.connect("Casa", "secret123")
        self.assertEqual(result, "ok")
        add = next(argv for argv in calls if argv[1:3] == ["connection", "add"])
        self.assertEqual(
            add[add.index("wifi-sec.psk") + 1],
            "secret123",
        )
        self.assertIn("wpa-psk", add)
        self.assertEqual(calls[-1][:5], ["nmcli", "-w", "20", "connection", "up"])
    def test_connect_open_network(self) -> None:
        calls: list[list[str]] = []

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            calls.append(argv)
            return 0, "", ""

        prov = WifiProvisioner(runner=runner)
        result = prov.connect("Directa", "")
        self.assertEqual(result, "ok")
        self.assertFalse(any("wifi-sec.psk" in argv for argv in calls))
        self.assertEqual(calls[-1][:5], ["nmcli", "-w", "20", "connection", "up"])

    def test_parse_uuid_de_connection_add(self) -> None:
        text = "Connection 'teo-probe-delete' (d6371a80-9aa5-4b7d-b77f-a5126ca599bb) successfully added.\n"
        self.assertEqual(parse_nmcli_added_uuid(text), "d6371a80-9aa5-4b7d-b77f-a5126ca599bb")
        self.assertEqual(
            parse_nmcli_connection_rows(
                "Casa:11111111-1111-1111-1111-111111111111:802-11-wireless\n"
            ),
            [("Casa", "11111111-1111-1111-1111-111111111111", "802-11-wireless")],
        )

    def test_connect_perfil_existente_incluye_key_mgmt(self) -> None:
        calls: list[list[str]] = []

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            calls.append(argv)
            if argv[:6] == ["nmcli", "-t", "-f", "NAME,UUID,TYPE", "connection", "show"]:
                return 0, "MOVISTARWIFI8165:b9f751ad-9e8a-4266-89c2-0b268b01e2f4:802-11-wireless\n", ""
            if argv[:3] == ["nmcli", "-g", "802-11-wireless.ssid"]:
                return 0, "MOVISTARWIFI8165\n", ""
            return 0, "", ""

        prov = WifiProvisioner(runner=runner)
        self.assertEqual(prov.connect("MOVISTARWIFI8165", "secret123"), "ok")
        modify = next(argv for argv in calls if argv[1:3] == ["connection", "modify"])
        self.assertEqual(
            modify[4:],
            ["wifi-sec.key-mgmt", "wpa-psk", "wifi-sec.psk", "secret123"],
        )
        self.assertFalse(any(argv[1:4] == ["dev", "wifi", "connect"] for argv in calls))

    def test_connect_auth_failure(self) -> None:
        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            return 1, "", "Secrets were required"

        prov = WifiProvisioner(runner=runner)
        self.assertEqual(prov.connect("Casa", "wrong"), "auth")


class TestNoLeakDePassword(unittest.TestCase):
    """C2: ninguna excepción puede arrastrar el argv con `password <clave>`."""

    def test_default_runner_timeout_no_expone_el_argv(self) -> None:
        argv = ["nmcli", "dev", "wifi", "connect", "Casa", "password", SECRET]

        def fake_run(*args, **kwargs):
            raise subprocess.TimeoutExpired(cmd=argv, timeout=30.0)

        with mock.patch.object(subprocess, "run", fake_run):
            with self.assertRaises(WifiCommandTimeout) as ctx:
                _default_runner(argv, 30.0)
        self.assertNotIn(SECRET, str(ctx.exception))
        self.assertNotIn("password", str(ctx.exception))
        self.assertNotIn(SECRET, repr(ctx.exception))

    def test_default_runner_error_generico_no_expone_el_argv(self) -> None:
        argv = ["nmcli", "dev", "wifi", "connect", "Casa", "password", SECRET]

        def fake_run(*args, **kwargs):
            raise subprocess.CalledProcessError(returncode=1, cmd=argv)

        with mock.patch.object(subprocess, "run", fake_run):
            with self.assertRaises(WifiCommandError) as ctx:
                _default_runner(argv, 30.0)
        self.assertNotIn(SECRET, str(ctx.exception))

    def test_connect_timeout_devuelve_timeout(self) -> None:
        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            raise subprocess.TimeoutExpired(cmd=argv, timeout=timeout)

        prov = WifiProvisioner(runner=runner)
        self.assertEqual(prov.connect("Casa", SECRET), "timeout")

    def test_connect_error_de_comando_devuelve_error(self) -> None:
        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            raise OSError("nmcli no existe")

        prov = WifiProvisioner(runner=runner)
        self.assertEqual(prov.connect("Casa", SECRET), "error")

    def test_connect_nunca_propaga_excepcion_con_argv(self) -> None:
        """Ni una excepción con la clave dentro puede escapar de connect()."""

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            raise RuntimeError(f"fallo corriendo {argv}")

        prov = WifiProvisioner(runner=runner)
        try:
            result = prov.connect("Casa", SECRET)
        except Exception as exc:  # pragma: no cover - el test falla abajo
            self.fail(f"connect propagó {type(exc).__name__}: {exc}")
        self.assertEqual(result, "error")

    def test_is_connected_con_timeout_devuelve_false(self) -> None:
        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            raise subprocess.TimeoutExpired(cmd=argv, timeout=timeout)

        prov = WifiProvisioner(runner=runner)
        self.assertFalse(prov.is_connected())

    def test_scan_con_timeout_levanta_error_sanitizado(self) -> None:
        argv_visto: list[list[str]] = []

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            argv_visto.append(argv)
            raise subprocess.TimeoutExpired(cmd=argv, timeout=timeout)

        prov = WifiProvisioner(runner=runner)
        with self.assertRaises(WifiCommandTimeout) as ctx:
            prov.scan()
        self.assertNotIn("SSID,SIGNAL,SECURITY", str(ctx.exception))

    def test_scan_tolera_rescan_que_revienta(self) -> None:
        """Un rescan que falla no debe impedir listar el cache de nmcli."""

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            if "--rescan" in argv:
                raise subprocess.TimeoutExpired(cmd=argv, timeout=timeout)
            return 0, NMCLI_WIFI_FIXTURE, ""

        prov = WifiProvisioner(runner=runner)
        self.assertEqual(len(prov.scan()), 2)


class TestSwitchNetwork(unittest.TestCase):
    def test_parse_nombre_wifi_activo(self) -> None:
        text = "lo:loopback\nCasa:802-11-wireless\n"
        self.assertEqual(parse_nmcli_active_wifi_name(text), "Casa")
        self.assertIsNone(parse_nmcli_active_wifi_name("lo:loopback\n"))

    def test_switch_ok_olvida_la_red_anterior(self) -> None:
        calls: list[list[str]] = []
        active = {"name": "Casa"}

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            calls.append(argv)
            if argv[:4] == ["nmcli", "-t", "-f", "NAME,TYPE"]:
                return 0, f"{active['name']}:802-11-wireless\n", ""
            if argv[:3] == ["nmcli", "connection", "add"]:
                return 0, "Connection 'Oficina' (11111111-1111-1111-1111-111111111111) successfully added.\n", ""
            if "connection" in argv and "up" in argv:
                active["name"] = "Oficina"
                return 0, "", ""
            if argv[:4] == ["nmcli", "connection", "delete", "id"]:
                return 0, "", ""
            return 1, "", "no"

        prov = WifiProvisioner(runner=runner)
        self.assertEqual(prov.switch_to("Oficina", "clave"), "ok")
        self.assertEqual(
            calls[-1],
            ["nmcli", "connection", "delete", "id", "Casa"],
        )

    def test_switch_clave_mala_no_borra_la_red_actual(self) -> None:
        calls: list[list[str]] = []

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            calls.append(argv)
            if argv[:4] == ["nmcli", "-t", "-f", "NAME,UUID,TYPE"] and "--active" in argv:
                return 0, "Casa:22222222-2222-2222-2222-222222222222:802-11-wireless\n", ""
            if argv[:4] == ["nmcli", "-t", "-f", "NAME,TYPE"]:
                return 0, "Casa:802-11-wireless\n", ""
            if argv[:3] == ["nmcli", "connection", "add"]:
                return 0, "Connection 'Oficina' (11111111-1111-1111-1111-111111111111) successfully added.\n", ""
            if argv[:3] == ["nmcli", "-g", "802-11-wireless.ssid"]:
                return 0, "Oficina\n", ""
            if argv[:6] == ["nmcli", "-t", "-f", "NAME,UUID,TYPE", "connection", "show"]:
                return 0, "Oficina:11111111-1111-1111-1111-111111111111:802-11-wireless\n", ""
            if "connection" in argv and "up" in argv and "11111111-1111-1111-1111-111111111111" in argv:
                return 1, "", "Secrets were required, but not provided"
            if argv[:3] == ["nmcli", "connection", "modify"]:
                return 0, "", ""
            if argv[:3] == ["nmcli", "connection", "down"]:
                return 0, "", ""
            if "connection" in argv and "up" in argv and "22222222-2222-2222-2222-222222222222" in argv:
                return 0, "", ""
            return 1, "", "no"

        prov = WifiProvisioner(runner=runner)
        self.assertEqual(prov.switch_to("Oficina", "mala"), "auth")
        self.assertFalse(any(argv[:3] == ["nmcli", "connection", "delete"] for argv in calls))
        self.assertIn(
            ["nmcli", "-w", "20", "connection", "up", "uuid", "22222222-2222-2222-2222-222222222222"],
            calls,
        )
        self.assertTrue(
            any("connection.autoconnect" in argv and "no" in argv for argv in calls)
        )


class TestKnownNetworks(unittest.TestCase):
    def test_archivo_roto_o_ausente_devuelve_vacio(self) -> None:
        missing = Path(tempfile.gettempdir()) / "known-wifi-que-no-existe.json"
        self.assertEqual(load_known_networks(missing), [])
        with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8") as fh:
            fh.write("{")
            path = Path(fh.name)
        self.addCleanup(path.unlink, missing_ok=True)
        self.assertEqual(load_known_networks(path), [])

    def test_ignora_entradas_sin_ssid_o_sin_clave(self) -> None:
        with tempfile.NamedTemporaryFile("w", suffix=".json", delete=False, encoding="utf-8") as fh:
            json.dump(
                [
                    {"ssid": "Casa", "password": "secreta"},
                    {"ssid": "", "password": "x"},
                    {"ssid": "Abierta", "password": 1},
                    "basura",
                ],
                fh,
            )
            path = Path(fh.name)
        self.addCleanup(path.unlink, missing_ok=True)
        self.assertEqual(load_known_networks(path), [("Casa", "secreta")])

    def test_para_en_la_primera_que_conecta(self) -> None:
        tried: list[str] = []

        def switch(ssid: str, password: str) -> str:
            tried.append(ssid)
            return "ok" if ssid == "TeleCentro-22ed" else "auth"

        prov = WifiProvisioner(runner=lambda argv, timeout: (0, "", ""))
        prov.switch_to = switch  # type: ignore[method-assign]
        joined = prov.try_known_networks(
            [
                ("MOVISTARWIFI8165", "a"),
                ("TeleCentro-22ed", "b"),
                ("moto g72", "c"),
            ]
        )
        self.assertEqual(joined, "TeleCentro-22ed")
        self.assertEqual(tried, ["MOVISTARWIFI8165", "TeleCentro-22ed"])


class TestWifiProvisionSource(unittest.TestCase):
    def test_no_log_action_in_module(self) -> None:
        src = Path(__file__).with_name("wifi_provision.py").read_text(encoding="utf-8")
        self.assertNotIn("log_action", src)


if __name__ == "__main__":
    unittest.main()
