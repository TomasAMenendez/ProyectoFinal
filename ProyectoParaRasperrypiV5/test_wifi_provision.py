"""Tests TDD de wifi_provision (parseo nmcli, sin radio real)."""
from __future__ import annotations

import subprocess
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

    def test_auth_wireless_security(self) -> None:
        stderr = "802-11-wireless-security.key-mgmt: property is invalid"
        self.assertEqual(classify_nmcli_connect(1, stderr, 2.0), "auth")

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
            if argv[-1] == "rescan":
                return 1, "", "rescan failed"
            return 0, NMCLI_WIFI_FIXTURE, ""

        prov = WifiProvisioner(runner=runner)
        nets = prov.scan()
        self.assertEqual(len(nets), 2)
        self.assertEqual(calls[0], ["nmcli", "dev", "wifi", "rescan"])
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
        self.assertEqual(
            calls[-1],
            ["nmcli", "dev", "wifi", "connect", "Casa", "password", "secret123"],
        )
    def test_connect_open_network(self) -> None:
        calls: list[list[str]] = []

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            calls.append(argv)
            return 0, "", ""

        prov = WifiProvisioner(runner=runner)
        result = prov.connect("Directa", "")
        self.assertEqual(result, "ok")
        self.assertEqual(calls[-1], ["nmcli", "dev", "wifi", "connect", "Directa"])

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
            if argv[-1] == "rescan":
                return 0, "", ""
            raise subprocess.TimeoutExpired(cmd=argv, timeout=timeout)

        prov = WifiProvisioner(runner=runner)
        with self.assertRaises(WifiCommandTimeout) as ctx:
            prov.scan()
        self.assertNotIn("SSID,SIGNAL,SECURITY", str(ctx.exception))

    def test_scan_tolera_rescan_que_revienta(self) -> None:
        """Un rescan que falla no debe impedir listar el cache de nmcli."""

        def runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
            if argv[-1] == "rescan":
                raise subprocess.TimeoutExpired(cmd=argv, timeout=timeout)
            return 0, NMCLI_WIFI_FIXTURE, ""

        prov = WifiProvisioner(runner=runner)
        self.assertEqual(len(prov.scan()), 2)


class TestWifiProvisionSource(unittest.TestCase):
    def test_no_log_action_in_module(self) -> None:
        src = Path(__file__).with_name("wifi_provision.py").read_text(encoding="utf-8")
        self.assertNotIn("log_action", src)


if __name__ == "__main__":
    unittest.main()
