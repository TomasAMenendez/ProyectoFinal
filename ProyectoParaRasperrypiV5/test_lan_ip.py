"""Tests de la IP que el beacon le manda al celular."""
from __future__ import annotations

import json
import unittest
from unittest import mock

from lan_ip import (
    beacon_fields,
    parse_ip_addr_show,
    pick_lan_ip,
    usable_lan_ip,
)


class TestUsableLanIp(unittest.TestCase):
    def test_rechaza_wildcard_y_loopback(self) -> None:
        self.assertIsNone(usable_lan_ip("0.0.0.0"))
        self.assertIsNone(usable_lan_ip("127.0.0.1"))
        self.assertIsNone(usable_lan_ip(""))
        self.assertIsNone(usable_lan_ip(None))

    def test_acepta_lan(self) -> None:
        self.assertEqual(usable_lan_ip("192.168.0.148"), "192.168.0.148")


class TestPickLanIp(unittest.TestCase):
    def test_sin_ruta_usa_wlan(self) -> None:
        self.assertEqual(
            pick_lan_ip("0.0.0.0", ["127.0.0.1", "192.168.0.148"]),
            "192.168.0.148",
        )

    def test_ruta_ganadora(self) -> None:
        self.assertEqual(
            pick_lan_ip("192.168.0.148", ["10.0.0.2"]),
            "192.168.0.148",
        )

    def test_sin_nada_es_none(self) -> None:
        self.assertIsNone(pick_lan_ip(None, ["0.0.0.0", "127.0.1.1"]))


class TestParseIpAddr(unittest.TestCase):
    def test_wlan0_global(self) -> None:
        text = (
            "2: wlan0    inet 192.168.0.148/24 brd 192.168.0.255 "
            "scope global dynamic noprefixroute wlan0\n"
            "1: lo    inet 127.0.0.1/8 scope host lo\n"
        )
        self.assertEqual(parse_ip_addr_show(text), ["192.168.0.148"])


class TestBeaconFields(unittest.TestCase):
    def test_omite_local_ip_si_no_hay_lan(self) -> None:
        data = beacon_fields(8080, "tok", None)
        self.assertNotIn("local_ip", data)
        raw = json.dumps(data)
        parsed = json.loads(raw)
        self.assertEqual(parsed["api_port"], 8080)
        self.assertNotIn("local_ip", parsed)

    def test_incluye_ip_usable(self) -> None:
        data = beacon_fields(8080, "tok", "192.168.0.148")
        self.assertEqual(data["local_ip"], "192.168.0.148")


class TestReadInterfaceIps(unittest.TestCase):
    def test_runner_inyectado(self) -> None:
        from lan_ip import read_interface_ips

        def runner(cmd, **kwargs):
            return mock.Mock(
                stdout="2: wlan0    inet 192.168.0.148/24 brd 192.168.0.255 scope global wlan0\n",
                stderr="",
            )

        self.assertEqual(read_interface_ips(runner=runner), ["192.168.0.148"])


if __name__ == "__main__":
    unittest.main()
