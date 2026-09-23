"""Tests TDD de sesión BLE de setup WiFi y renderizado de PIN en LCD."""
from __future__ import annotations

import json
import threading
import time
import unittest
from unittest import mock

from PIL import ImageFont

from ble_setup import (
    STATUS_VALUES,
    BleSetupSession,
    WifiBleCoordinator,
    render_pin_image,
    run_wifi_ble_loop,
)
from wifi_provision import WifiNetwork


class TestSession(unittest.TestCase):
    def test_start_sets_pin_and_idle(self) -> None:
        pins = iter(["111111", "222222"])
        s = BleSetupSession(rng=lambda: next(pins), now=lambda: 0.0)
        self.assertEqual(s.start(), "111111")
        self.assertTrue(s.active)
        self.assertFalse(s.pin_ok)
        self.assertFalse(s.allow_scan_or_connect())
        self.assertEqual(s.status, "idle")

    def test_wrong_pin_five_times_rotates(self) -> None:
        pins = iter(["111111", "222222"])
        s = BleSetupSession(rng=lambda: next(pins), now=lambda: 0.0)
        s.start()
        for _ in range(5):
            self.assertEqual(s.verify_pin("000000"), "failed:pin")
        self.assertEqual(s.status, "idle")
        self.assertEqual(s.verify_pin("222222"), "pin_ok")
        self.assertTrue(s.allow_scan_or_connect())

    def test_connect_before_pin_is_session(self) -> None:
        s = BleSetupSession(rng=lambda: "111111", now=lambda: 0.0)
        s.start()
        self.assertFalse(s.allow_scan_or_connect())

    def test_timeout(self) -> None:
        t = {"n": 0.0}
        s = BleSetupSession(rng=lambda: "111111", now=lambda: t["n"])
        s.start()
        t["n"] = 601.0
        self.assertTrue(s.expired())


class TestPinImage(unittest.TestCase):
    def test_size_and_digits(self) -> None:
        img = render_pin_image("123456", 320, 240)
        self.assertEqual(img.size, (320, 240))

    def test_pillow_viejo_sin_size_ni_truetype(self) -> None:
        """Pillow < 10 no acepta load_default(size=...): debe degradar."""
        real_load_default = ImageFont.load_default

        def load_default_sin_size(*args, **kwargs):
            if args or kwargs:
                raise TypeError(
                    "load_default() got an unexpected keyword argument 'size'"
                )
            return real_load_default()

        real_truetype = ImageFont.truetype

        def truetype_sin_fuentes_de_disco(font=None, *args, **kwargs):
            if isinstance(font, str):
                raise OSError("cannot open resource")
            return real_truetype(font, *args, **kwargs)

        with mock.patch.object(ImageFont, "load_default", load_default_sin_size), \
                mock.patch.object(ImageFont, "truetype", truetype_sin_fuentes_de_disco):
            img = render_pin_image("123456", 320, 240)
        self.assertEqual(img.size, (320, 240))
        self.assertGreater(len(img.getcolors(maxcolors=4096) or []), 1)


class FakeTransport:
    """Transporte GATT falso: registra notificaciones y arranques."""

    def __init__(self) -> None:
        self.notified: list[str] = []
        self.starts = 0
        self.stops = 0
        self.session = None
        self.on_scan = None
        self.on_connect = None
        self.on_pin = None

    def start(self, session, on_scan, on_connect, on_pin=None) -> None:
        self.starts += 1
        self.session = session
        self.on_scan = on_scan
        self.on_connect = on_connect
        self.on_pin = on_pin

    def stop(self) -> None:
        self.stops += 1

    def notify_status(self, status: str) -> None:
        self.notified.append(status)


class FakeProvisioner:
    """Provisioner falso sin nmcli ni radio."""

    def __init__(
        self,
        connect_result: str = "ok",
        connected: bool = False,
        networks: list[WifiNetwork] | None = None,
    ) -> None:
        self.connect_result = connect_result
        self.connected = connected
        self.networks = (
            list(networks)
            if networks is not None
            else [WifiNetwork(ssid="CasaTeo", signal=77, secure=True)]
        )
        self.connect_calls: list[tuple[str, str]] = []
        self.scan_calls = 0

    def is_connected(self) -> bool:
        return self.connected

    def scan(self) -> list[WifiNetwork]:
        self.scan_calls += 1
        return list(self.networks)

    def connect(self, ssid: str, password: str) -> str:
        self.connect_calls.append((ssid, password))
        return self.connect_result


class FakeEyeDisplay:
    """LCD falsa que sólo guarda el override recibido."""

    def __init__(self) -> None:
        self.width = 320
        self.height = 240
        self.overrides: list = []

    def set_lcd_override(self, image) -> None:
        self.overrides.append(image)


class CoordinatorFixture:
    """Arma un coordinator con reloj controlado y colaboradores falsos."""

    def __init__(
        self,
        provisioner: FakeProvisioner | None = None,
        defer: bool = False,
    ) -> None:
        self.clock = {"n": 0.0}
        self.provisioner = provisioner or FakeProvisioner()
        self.transport = FakeTransport()
        self.eye = FakeEyeDisplay()
        self.states: list[tuple[str, bool]] = []
        self.defer = defer
        self.jobs: list = []
        self.coord = WifiBleCoordinator(
            self.provisioner,
            self.transport,
            self.eye,
            now=lambda: self.clock["n"],
            on_state=lambda key, value: self.states.append((key, value)),
            spawn=self._spawn,
        )

    def _spawn(self, work) -> None:
        """Corre el trabajo inline, o lo encola si el test lo pidió diferido."""
        if self.defer:
            self.jobs.append(work)
            return
        work()

    def run_jobs(self) -> None:
        jobs, self.jobs = self.jobs, []
        for job in jobs:
            job()

    def enter_with_pin(self) -> None:
        self.coord.enter_setup()
        self.coord.session.verify_pin(self.coord.session_pin)


class TestCoordinator(unittest.TestCase):
    def test_enter_setup_arranca_sesion_lcd_y_transporte(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        self.assertTrue(fx.coord.session.active)
        self.assertEqual(fx.transport.starts, 1)
        self.assertEqual(len(fx.eye.overrides), 1)
        self.assertIsNotNone(fx.eye.overrides[0])
        self.assertIn(("ble_setup_active", True), fx.states)

    def test_enter_setup_es_idempotente(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        pin = fx.coord.session_pin
        fx.coord.enter_setup()
        self.assertEqual(fx.transport.starts, 1)
        self.assertEqual(fx.coord.session_pin, pin)

    def test_handle_scan_sin_pin_falla_session(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        payload = fx.coord.handle_scan()
        self.assertEqual(json.loads(payload.decode("utf-8")), [])
        self.assertIn("failed:session", fx.transport.notified)
        self.assertEqual(fx.provisioner.scan_calls, 0)

    def test_handle_scan_con_pin_devuelve_redes(self) -> None:
        fx = CoordinatorFixture()
        fx.enter_with_pin()
        payload = fx.coord.handle_scan()
        data = json.loads(payload.decode("utf-8"))
        self.assertEqual(data, [{"ssid": "CasaTeo", "signal": 77, "secure": True}])

    def test_handle_connect_password_mala_sigue_activa(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connect_result="auth"))
        fx.enter_with_pin()
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "mala"}')
        self.assertIn("failed:auth", fx.transport.notified)
        self.assertTrue(fx.coord.session.active)
        self.assertEqual(fx.transport.stops, 0)

    def test_handle_connect_ok_notifica_y_sale_de_setup(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connect_result="ok"))
        fx.enter_with_pin()
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "secreta"}')
        self.assertIn("connected", fx.transport.notified)
        self.assertEqual(fx.transport.stops, 1)
        self.assertIsNone(fx.eye.overrides[-1])
        self.assertIn(("wifi_connected", True), fx.states)
        self.assertIn(("ble_setup_active", False), fx.states)
        self.assertFalse(fx.coord.session.active)

    def test_handle_connect_notifica_connecting_antes_de_llamar_nmcli(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connect_result="ok"), defer=True)
        fx.enter_with_pin()
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "secreta"}')
        self.assertEqual(fx.transport.notified[-1], "connecting")
        self.assertEqual(fx.coord.session.status, "connecting")
        self.assertEqual(fx.provisioner.connect_calls, [])
        self.assertEqual(fx.transport.stops, 0)
        fx.run_jobs()
        self.assertEqual(fx.provisioner.connect_calls, [("CasaTeo", "secreta")])
        self.assertEqual(fx.transport.notified[-1], "connected")
        self.assertEqual(fx.transport.stops, 1)

    def test_segunda_escritura_durante_connect_se_ignora(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connect_result="ok"), defer=True)
        fx.enter_with_pin()
        payload = b'{"ssid": "CasaTeo", "password": "secreta"}'
        fx.coord.handle_connect(payload)
        fx.coord.handle_connect(payload)
        self.assertEqual(len(fx.jobs), 1)
        fx.run_jobs()
        self.assertEqual(len(fx.provisioner.connect_calls), 1)

    def test_connect_fallido_permite_reintentar(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connect_result="auth"), defer=True)
        fx.enter_with_pin()
        payload = b'{"ssid": "CasaTeo", "password": "mala"}'
        fx.coord.handle_connect(payload)
        fx.run_jobs()
        self.assertEqual(fx.transport.notified[-1], "failed:auth")
        fx.coord.handle_connect(payload)
        fx.run_jobs()
        self.assertEqual(len(fx.provisioner.connect_calls), 2)

    def test_connect_por_defecto_corre_en_hilo_daemon_aparte(self) -> None:
        release = threading.Event()
        info: dict = {}

        class BlockingProvisioner(FakeProvisioner):
            def connect(self, ssid: str, password: str) -> str:
                info["thread"] = threading.current_thread()
                self.connect_calls.append((ssid, password))
                release.wait(5.0)
                return self.connect_result

        provisioner = BlockingProvisioner(connect_result="auth")
        transport = FakeTransport()
        coord = WifiBleCoordinator(provisioner, transport, FakeEyeDisplay(), now=lambda: 0.0)
        coord.enter_setup()
        coord.session.verify_pin(coord.session_pin)
        coord.handle_connect(b'{"ssid": "CasaTeo", "password": "mala"}')
        self.assertEqual(transport.notified[-1], "connecting")
        release.set()
        deadline = time.monotonic() + 5.0
        while time.monotonic() < deadline and transport.notified[-1] != "failed:auth":
            time.sleep(0.01)
        self.assertEqual(transport.notified[-1], "failed:auth")
        self.assertIsNot(info["thread"], threading.current_thread())
        self.assertTrue(info["thread"].daemon)

    def test_handle_connect_bytes_no_utf8_no_revienta(self) -> None:
        fx = CoordinatorFixture()
        fx.enter_with_pin()
        fx.coord.handle_connect(b"\xff\xfe\x00")
        self.assertTrue(fx.transport.notified[-1].startswith("failed:"))
        self.assertEqual(fx.provisioner.connect_calls, [])

    def test_handle_connect_json_invalido_no_revienta(self) -> None:
        fx = CoordinatorFixture()
        fx.enter_with_pin()
        fx.coord.handle_connect(b"{no json")
        self.assertTrue(fx.transport.notified[-1].startswith("failed:"))
        self.assertEqual(fx.provisioner.connect_calls, [])

    def test_handle_connect_sin_ssid_no_revienta(self) -> None:
        fx = CoordinatorFixture()
        fx.enter_with_pin()
        fx.coord.handle_connect(b'{"password": "x"}')
        self.assertTrue(fx.transport.notified[-1].startswith("failed:"))
        self.assertEqual(fx.provisioner.connect_calls, [])

    def test_handle_connect_sin_pin_falla_session(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "x"}')
        self.assertIn("failed:session", fx.transport.notified)
        self.assertEqual(fx.provisioner.connect_calls, [])

    def test_tick_a_los_601_s_sale_por_timeout(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.clock["n"] = 601.0
        fx.coord.tick()
        self.assertEqual(fx.transport.stops, 1)
        self.assertIsNone(fx.eye.overrides[-1])
        self.assertIn(("ble_setup_active", False), fx.states)

    def test_tick_antes_del_timeout_no_sale(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.clock["n"] = 120.0
        fx.coord.tick()
        self.assertEqual(fx.transport.stops, 0)
        self.assertTrue(fx.coord.session.active)

    def test_tick_con_wifi_ya_conectado_sale(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.provisioner.connected = True
        fx.coord.tick()
        self.assertEqual(fx.transport.stops, 1)
        self.assertIn(("wifi_connected", True), fx.states)

    def test_sin_eye_display_no_revienta(self) -> None:
        provisioner = FakeProvisioner()
        transport = FakeTransport()
        coord = WifiBleCoordinator(provisioner, transport, None, now=lambda: 0.0)
        coord.enter_setup()
        coord.leave_setup()
        self.assertEqual(transport.starts, 1)
        self.assertEqual(transport.stops, 1)

    def test_tick_sin_setup_activo_no_hace_nada(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.tick()
        self.assertEqual(fx.transport.stops, 0)
        self.assertEqual(fx.transport.starts, 0)


class TestBootLoop(unittest.TestCase):
    """C5: el loop de tick tiene que vivir lo que vive la app."""

    def _loop(self, fx, iterations: int = 3) -> list[float]:
        sleeps: list[float] = []
        run_wifi_ble_loop(
            fx.coord,
            fx.provisioner,
            sleep=sleeps.append,
            iterations=iterations,
        )
        return sleeps

    def test_espera_5s_y_despues_tickea_cada_2s(self) -> None:
        fx = CoordinatorFixture()
        sleeps = self._loop(fx, iterations=2)
        self.assertEqual(sleeps, [5.0, 2.0, 2.0])

    def test_sin_wifi_al_arrancar_entra_a_setup(self) -> None:
        fx = CoordinatorFixture()
        self._loop(fx, iterations=1)
        self.assertEqual(fx.transport.starts, 1)
        self.assertTrue(fx.coord.session.active)

    def test_con_wifi_al_arrancar_no_anuncia_pero_sigue_tickeando(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connected=True))
        ticks: list[int] = []
        fx.coord.tick = lambda: ticks.append(1)  # type: ignore[method-assign]
        self._loop(fx, iterations=3)
        self.assertEqual(fx.transport.starts, 0)
        self.assertEqual(len(ticks), 3)
        self.assertIn(("wifi_connected", True), fx.states)

    def test_un_tick_que_revienta_no_mata_el_loop(self) -> None:
        fx = CoordinatorFixture()
        ticks: list[int] = []

        def tick_roto() -> None:
            ticks.append(1)
            raise RuntimeError("nmcli murió")

        fx.coord.tick = tick_roto  # type: ignore[method-assign]
        self._loop(fx, iterations=3)
        self.assertEqual(len(ticks), 3)

    def test_provisioner_roto_al_arrancar_igual_entra_a_setup(self) -> None:
        class RotoProvisioner(FakeProvisioner):
            def is_connected(self) -> bool:
                raise RuntimeError("nmcli no está")

        fx = CoordinatorFixture(RotoProvisioner())
        self._loop(fx, iterations=1)
        self.assertEqual(fx.transport.starts, 1)

    def test_enter_setup_falla_si_el_transporte_no_puede_anunciar(self) -> None:
        fx = CoordinatorFixture()
        fx.transport.available = False
        self.assertFalse(fx.coord.enter_setup())
        self.assertEqual(fx.transport.starts, 0)
        self.assertFalse(fx.coord.session.active)
        self.assertEqual(fx.coord.session_pin, "")
        self.assertEqual(fx.eye.overrides, [], "no se pinta un PIN que nadie puede leer")

    def test_enter_setup_devuelve_true_cuando_anuncia(self) -> None:
        fx = CoordinatorFixture()
        self.assertTrue(fx.coord.enter_setup())
        self.assertTrue(fx.coord.enter_setup())

    def test_enter_setup_devuelve_false_si_el_transporte_revienta(self) -> None:
        fx = CoordinatorFixture()

        def start_roto(*args, **kwargs):
            raise RuntimeError("BlueZ rechazó el advertisement")

        fx.transport.start = start_roto  # type: ignore[method-assign]
        self.assertFalse(fx.coord.enter_setup())
        self.assertFalse(fx.coord.session.active)


class TestGattOffsets(unittest.TestCase):
    """C4: lecturas y escrituras largas de BlueZ llegan troceadas por offset.

    La clase `Characteristic` vive dentro de `_build_classes()` y necesita
    `dbus`, así que la lógica de offsets se probó extrayéndola a helpers puros
    del módulo, que sí importan en Windows.
    """

    def test_read_offset_lee_la_opcion_de_dbus(self) -> None:
        from ble_gatt_bluez import read_offset

        self.assertEqual(read_offset(None), 0)
        self.assertEqual(read_offset({}), 0)
        self.assertEqual(read_offset({"offset": 22}), 22)
        self.assertEqual(read_offset({"offset": "no-numero"}), 0)
        self.assertEqual(read_offset({"offset": -5}), 0)

    def test_format_dbus_import_error_incluye_el_python_del_venv(self) -> None:
        from ble_gatt_bluez import format_dbus_import_error

        msg = format_dbus_import_error(
            ModuleNotFoundError("No module named 'dbus'"),
            "/home/teo/Desktop/ProyectoParaRasperrypiV5/venv/bin/python",
        )
        self.assertIn("No module named 'dbus'", msg)
        self.assertIn("venv/bin/python", msg)

    def test_mgmt_output_failed_detecta_ansi_y_returncode_mentiroso(self) -> None:
        from ble_gatt_bluez import mgmt_output_failed

        denied = "\x1b[0;91mUnable to set local name with status 0x14 (Permission Denied)\x1b[0m"
        self.assertTrue(mgmt_output_failed(denied))
        self.assertTrue(mgmt_output_failed("Add Advertising failed with status 0x0d (Invalid Parameters)\n"))
        self.assertFalse(mgmt_output_failed("Instance added: 1\n"))

    def test_mgmt_advertise_argvs_son_connectable_con_nombre_en_scan_rsp(self) -> None:
        from ble_gatt_bluez import mgmt_advertise_argvs

        self.assertEqual(
            mgmt_advertise_argvs("TEO"),
            [
                ["clr-adv"],
                ["name", "TEO", "TEO"],
                ["add-adv", "-c", "-g", "-n", "1"],
            ],
        )

    def test_enable_mgmt_advertising_exige_instance_added(self) -> None:
        from ble_gatt_bluez import enable_mgmt_advertising

        calls: list[list[str]] = []

        def runner(cmd, **kwargs):
            calls.append(list(cmd))
            if cmd[-2:] == ["-n", "1"] or cmd[-1] == "1":
                return mock.Mock(returncode=0, stdout="Instance added: 1\n", stderr="")
            return mock.Mock(returncode=0, stdout="", stderr="")

        self.assertTrue(enable_mgmt_advertising("TEO", runner=runner))
        self.assertTrue(any(cmd[:3] == ["sudo", "-n", "btmgmt"] for cmd in calls))
        self.assertIn(["sudo", "-n", "btmgmt", "add-adv", "-c", "-g", "-n", "1"], calls)

    def test_enable_mgmt_advertising_falso_si_add_adv_sale_0x0d(self) -> None:
        from ble_gatt_bluez import enable_mgmt_advertising

        def runner(cmd, **kwargs):
            if "add-adv" in cmd:
                return mock.Mock(
                    returncode=1,
                    stdout="Add Advertising failed with status 0x0d (Invalid Parameters)\n",
                    stderr="",
                )
            return mock.Mock(returncode=0, stdout="", stderr="")

        self.assertFalse(enable_mgmt_advertising("TEO", runner=runner))

    def test_run_btmgmt_prueba_sin_sudo_si_sudo_n_falla(self) -> None:
        from ble_gatt_bluez import run_btmgmt

        def runner(cmd, **kwargs):
            if cmd[:3] == ["sudo", "-n", "btmgmt"]:
                return mock.Mock(returncode=1, stdout="sudo: a password is required\n", stderr="")
            return mock.Mock(returncode=0, stdout="Instance added: 1\n", stderr="")

        rc, out = run_btmgmt(["add-adv", "-c", "-g", "-n", "1"], runner=runner)
        self.assertEqual(rc, 0)
        self.assertIn("Instance added: 1", out)

    def test_slice_payload_devuelve_el_resto_del_blob(self) -> None:
        from ble_gatt_bluez import slice_payload

        payload = bytes(range(30))
        self.assertEqual(slice_payload(payload, 0), payload)
        self.assertEqual(slice_payload(payload, 22), payload[22:])
        self.assertEqual(slice_payload(payload, 30), b"")
        self.assertEqual(slice_payload(payload, 99), b"")

    def test_accumulate_write_rearma_el_payload_troceado(self) -> None:
        from ble_gatt_bluez import accumulate_write

        completo = b'{"ssid": "CasaTeo", "password": "clave-larga-de-verdad"}'
        buf = bytearray()
        buf = accumulate_write(buf, 0, completo[:20])
        self.assertEqual(bytes(buf), completo[:20])
        buf = accumulate_write(buf, 20, completo[20:40])
        buf = accumulate_write(buf, 40, completo[40:])
        self.assertEqual(bytes(buf), completo)

    def test_accumulate_write_offset_cero_reinicia_el_buffer(self) -> None:
        from ble_gatt_bluez import accumulate_write

        buf = accumulate_write(bytearray(b"basura previa"), 0, b"nuevo")
        self.assertEqual(bytes(buf), b"nuevo")

    def test_accumulate_write_rechaza_un_hueco(self) -> None:
        from ble_gatt_bluez import accumulate_write

        with self.assertRaises(ValueError):
            accumulate_write(bytearray(b"abc"), 10, b"xyz")

    def test_json_payload_complete(self) -> None:
        from ble_gatt_bluez import json_payload_complete

        self.assertFalse(json_payload_complete(b'{"ssid": "Casa", "pass'))
        self.assertFalse(json_payload_complete(b""))
        self.assertFalse(json_payload_complete(b"\xff\xfe"))
        self.assertTrue(json_payload_complete(b'{"ssid": "Casa"}'))
        self.assertFalse(json_payload_complete(b"[1, 2]"))


class TestPinRotationRepaint(unittest.TestCase):
    """C1: al rotar el PIN, la LCD tiene que mostrar el nuevo."""

    def test_session_informa_el_pin_rotado(self) -> None:
        pins = iter(["111111", "222222"])
        s = BleSetupSession(rng=lambda: next(pins), now=lambda: 0.0)
        s.start()
        for _ in range(4):
            self.assertEqual(s.verify_pin_detailed("000000"), ("failed:pin", ""))
        self.assertEqual(s.verify_pin_detailed("000000"), ("failed:pin", "222222"))

    def test_enter_setup_registra_handle_pin_en_el_transporte(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        self.assertEqual(fx.transport.on_pin, fx.coord.handle_pin)

    def test_handle_pin_correcto_notifica_pin_ok(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        self.assertIn("pin_ok", fx.transport.notified)
        self.assertTrue(fx.coord.session.allow_scan_or_connect())

    def test_handle_pin_incorrecto_notifica_failed_pin(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.coord.handle_pin(b"000000")
        self.assertEqual(fx.transport.notified[-1], "failed:pin")
        self.assertEqual(len(fx.eye.overrides), 1)

    def test_handle_pin_basura_no_revienta(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.coord.handle_pin(b"\xff\xfe")
        self.assertEqual(fx.transport.notified[-1], "failed:pin")

    def test_cinco_fallos_repintan_la_lcd_con_el_pin_nuevo(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        pin_viejo = fx.coord.session_pin
        for _ in range(5):
            fx.coord.handle_pin(b"000000")
        self.assertNotEqual(fx.coord.session_pin, pin_viejo)
        self.assertEqual(len(fx.eye.overrides), 2)
        self.assertIsNotNone(fx.eye.overrides[-1])
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        self.assertIn("pin_ok", fx.transport.notified)

    def test_el_pin_nunca_se_loguea(self) -> None:
        logs: list[str] = []
        fx = CoordinatorFixture()
        with mock.patch(
            "ble_setup.log_action", lambda tag, msg: logs.append(f"{tag} {msg}")
        ):
            fx.coord.enter_setup()
            pin_viejo = fx.coord.session_pin
            for _ in range(5):
                fx.coord.handle_pin(b"000000")
            pin_nuevo = fx.coord.session_pin
        texto = "\n".join(logs)
        self.assertNotIn(pin_viejo, texto)
        self.assertNotIn(pin_nuevo, texto)


class TestScanNoBloqueante(unittest.TestCase):
    """C3: el scan de nmcli no puede correr en el hilo de D-Bus."""

    def test_pin_ok_dispara_el_scan_en_un_worker(self) -> None:
        fx = CoordinatorFixture(defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        self.assertEqual(fx.provisioner.scan_calls, 0)
        self.assertEqual(fx.transport.notified[-1], "scanning")
        self.assertEqual(len(fx.jobs), 1)

    def test_read_devuelve_enseguida_mientras_el_worker_escanea(self) -> None:
        fx = CoordinatorFixture(defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        self.assertEqual(json.loads(fx.coord.handle_scan().decode("utf-8")), [])
        self.assertEqual(fx.provisioner.scan_calls, 0)
        fx.run_jobs()
        self.assertEqual(fx.provisioner.scan_calls, 1)
        self.assertEqual(fx.transport.notified[-1], "pin_ok")
        data = json.loads(fx.coord.handle_scan().decode("utf-8"))
        self.assertEqual(data, [{"ssid": "CasaTeo", "signal": 77, "secure": True}])

    def test_reads_concurrentes_no_encolan_varios_scans(self) -> None:
        fx = CoordinatorFixture(defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        fx.coord.handle_scan()
        fx.coord.handle_scan()
        self.assertEqual(len(fx.jobs), 1)

    def test_lecturas_repetidas_con_cache_fresco_no_reescanean(self) -> None:
        """El ping-pong: la app relee en cada `pin_ok`, así que un read de
        cache fresco no puede volver a emitir `scanning` ni lanzar nmcli."""
        fx = CoordinatorFixture(defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        fx.run_jobs()
        self.assertEqual(fx.transport.notified[-1], "pin_ok")
        notificados = list(fx.transport.notified)
        for _ in range(5):
            payload = fx.coord.handle_scan()
            self.assertEqual(
                json.loads(payload.decode("utf-8")),
                [{"ssid": "CasaTeo", "signal": 77, "secure": True}],
            )
        self.assertEqual(fx.jobs, [], "un cache fresco no debe lanzar workers")
        self.assertEqual(fx.provisioner.scan_calls, 1)
        self.assertEqual(fx.transport.notified, notificados, "no debe tocar el estado")
        self.assertEqual(fx.transport.notified.count("scanning"), 1)

    def test_cache_vencido_se_refresca(self) -> None:
        fx = CoordinatorFixture(defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        fx.run_jobs()
        fx.clock["n"] += 21.0
        fx.coord.handle_scan()
        self.assertEqual(len(fx.jobs), 1)
        self.assertEqual(fx.transport.notified[-1], "scanning")
        fx.run_jobs()
        self.assertEqual(fx.provisioner.scan_calls, 2)

    def test_lista_vacia_tambien_es_cache_fresco(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(networks=[]), defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        fx.run_jobs()
        self.assertEqual(fx.coord.handle_scan(), b"[]")
        self.assertEqual(fx.jobs, [])
        self.assertEqual(fx.provisioner.scan_calls, 1)

    def test_sesion_nueva_siempre_reescanea(self) -> None:
        fx = CoordinatorFixture(defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        fx.run_jobs()
        fx.coord.leave_setup()
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        self.assertEqual(len(fx.jobs), 1)
        fx.run_jobs()
        self.assertEqual(fx.provisioner.scan_calls, 2)

    def test_el_scan_tardio_no_pisa_el_estado_connecting(self) -> None:
        """Un scan que termina después del submit no puede volver a pin_ok."""
        fx = CoordinatorFixture(FakeProvisioner(connect_result="ok"), defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "secreta"}')
        self.assertEqual(fx.coord.session.status, "connecting")
        fx.run_jobs()  # corre primero el scan, después el connect
        self.assertNotIn("pin_ok", fx.transport.notified[-2:])

    def test_scan_fallido_emite_failed_session(self) -> None:
        class RotoProvisioner(FakeProvisioner):
            def scan(self):
                raise RuntimeError("nmcli murió")

        fx = CoordinatorFixture(RotoProvisioner(), defer=True)
        fx.coord.enter_setup()
        fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
        fx.run_jobs()
        self.assertEqual(fx.transport.notified[-1], "failed:session")

    def test_nuevo_setup_no_reusa_el_cache_viejo(self) -> None:
        fx = CoordinatorFixture()
        fx.enter_with_pin()
        self.assertNotEqual(fx.coord.handle_scan(), b"[]")
        fx.coord.leave_setup()
        fx.coord.enter_setup()
        fx.coord.handle_scan()  # sin PIN: no debe filtrar la lista anterior
        self.assertEqual(fx.coord._scan_cache, b"[]")


class TestStatusContract(unittest.TestCase):
    """I1/I2: sólo los nueve estados del plan, e incluyendo failed:timeout."""

    def test_los_nueve_estados_del_plan(self) -> None:
        self.assertEqual(
            STATUS_VALUES,
            frozenset(
                {
                    "idle",
                    "pin_ok",
                    "scanning",
                    "connecting",
                    "connected",
                    "failed:pin",
                    "failed:session",
                    "failed:auth",
                    "failed:timeout",
                }
            ),
        )

    def test_tick_expirado_emite_failed_timeout(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.clock["n"] = 601.0
        fx.coord.tick()
        self.assertEqual(fx.transport.notified[-1], "failed:timeout")
        self.assertEqual(fx.transport.stops, 1)

    def test_payload_invalido_emite_failed_session(self) -> None:
        fx = CoordinatorFixture()
        fx.enter_with_pin()
        fx.coord.handle_connect(b"{no json")
        self.assertEqual(fx.transport.notified[-1], "failed:session")

    def test_error_interno_del_provisioner_emite_failed_session(self) -> None:
        class RotoProvisioner(FakeProvisioner):
            def connect(self, ssid: str, password: str) -> str:
                raise RuntimeError("boom")

        fx = CoordinatorFixture(RotoProvisioner())
        fx.enter_with_pin()
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "x"}')
        self.assertEqual(fx.transport.notified[-1], "failed:session")

    def test_nmcli_timeout_emite_failed_timeout(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connect_result="timeout"))
        fx.enter_with_pin()
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "x"}')
        self.assertEqual(fx.transport.notified[-1], "failed:timeout")

    def test_resultado_desconocido_del_provisioner_cae_en_failed_session(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connect_result="error"))
        fx.enter_with_pin()
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "x"}')
        self.assertEqual(fx.transport.notified[-1], "failed:session")

    def test_ningun_estado_emitido_queda_fuera_del_contrato(self) -> None:
        flujos = [
            FakeProvisioner(connect_result="ok"),
            FakeProvisioner(connect_result="auth"),
            FakeProvisioner(connect_result="timeout"),
            FakeProvisioner(connect_result="error"),
        ]
        for provisioner in flujos:
            fx = CoordinatorFixture(provisioner)
            fx.coord.enter_setup()
            fx.coord.handle_pin(b"000000")
            fx.coord.handle_pin(fx.coord.session_pin.encode("ascii"))
            fx.coord.handle_scan()
            fx.coord.handle_connect(b"{no json")
            fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "x"}')
            fx.clock["n"] = 601.0
            fx.coord.tick()
            fuera = [s for s in fx.transport.notified if s not in STATUS_VALUES]
            self.assertEqual(fuera, [], f"estados fuera de contrato: {fuera}")


class TestPasswordNuncaEnLogs(unittest.TestCase):
    """C2: ni el provisioner ni el coordinator pueden loguear la clave."""

    def test_excepcion_del_provisioner_no_se_interpola(self) -> None:
        clave = "clave-super-secreta"
        logs: list[str] = []

        class RotoProvisioner(FakeProvisioner):
            def connect(self, ssid: str, password: str) -> str:
                raise RuntimeError(
                    f"Command '['nmcli', 'dev', 'wifi', 'connect', "
                    f"'{ssid}', 'password', '{password}']' timed out"
                )

        fx = CoordinatorFixture(RotoProvisioner())
        fx.enter_with_pin()
        with mock.patch(
            "ble_setup.log_action", lambda tag, msg: logs.append(f"{tag} {msg}")
        ):
            fx.coord.handle_connect(
                json.dumps({"ssid": "CasaTeo", "password": clave}).encode("utf-8")
            )
        texto = "\n".join(logs)
        self.assertNotIn(clave, texto)
        self.assertNotIn("password", texto)

    def test_credenciales_ok_no_loguean_la_clave(self) -> None:
        clave = "otra-clave-secreta"
        logs: list[str] = []
        fx = CoordinatorFixture(FakeProvisioner(connect_result="auth"))
        fx.enter_with_pin()
        with mock.patch(
            "ble_setup.log_action", lambda tag, msg: logs.append(f"{tag} {msg}")
        ):
            fx.coord.handle_connect(
                json.dumps({"ssid": "CasaTeo", "password": clave}).encode("utf-8")
            )
        self.assertNotIn(clave, "\n".join(logs))


class TestReentradaTrasTimeout(unittest.TestCase):
    """I9: tras el timeout se reintenta el setup mientras no haya WiFi."""

    def test_reentra_con_pin_nuevo_pasado_el_retry(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        pin_viejo = fx.coord.session_pin
        fx.clock["n"] = 601.0
        fx.coord.tick()
        self.assertFalse(fx.coord.session.active)
        fx.clock["n"] = 601.0 + 301.0
        fx.coord.tick()
        self.assertTrue(fx.coord.session.active)
        self.assertEqual(fx.transport.starts, 2)
        self.assertNotEqual(fx.coord.session_pin, pin_viejo)
        self.assertIsNotNone(fx.eye.overrides[-1])

    def test_no_reentra_antes_del_retry(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.clock["n"] = 601.0
        fx.coord.tick()
        fx.clock["n"] = 601.0 + 10.0
        fx.coord.tick()
        self.assertEqual(fx.transport.starts, 1)
        self.assertFalse(fx.coord.session.active)

    def test_reentrada_fallida_se_reintenta_mas_tarde(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.clock["n"] = 601.0
        fx.coord.tick()
        fx.transport.available = False
        fx.clock["n"] = 902.0
        fx.coord.tick()
        self.assertEqual(fx.transport.starts, 1)
        fx.transport.available = True
        fx.clock["n"] = 1203.0
        fx.coord.tick()
        self.assertEqual(fx.transport.starts, 2)

    def test_no_reentra_si_ya_hay_wifi(self) -> None:
        fx = CoordinatorFixture()
        fx.coord.enter_setup()
        fx.clock["n"] = 601.0
        fx.coord.tick()
        fx.provisioner.connected = True
        fx.clock["n"] = 601.0 + 301.0
        fx.coord.tick()
        self.assertEqual(fx.transport.starts, 1)
        self.assertFalse(fx.coord.session.active)
        self.assertIn(("wifi_connected", True), fx.states)

    def test_salida_exitosa_no_rearma_el_setup(self) -> None:
        fx = CoordinatorFixture(FakeProvisioner(connect_result="ok"))
        fx.enter_with_pin()
        fx.coord.handle_connect(b'{"ssid": "CasaTeo", "password": "secreta"}')
        fx.clock["n"] = 5000.0
        fx.coord.tick()
        self.assertEqual(fx.transport.starts, 1)
        self.assertFalse(fx.coord.session.active)
