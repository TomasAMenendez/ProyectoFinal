"""Transporte GATT sobre BlueZ (solo Linux). Degrada a no-op sin dbus.

Este módulo nunca debe levantar excepciones al importarse ni al usarse: si
`dbus`/BlueZ no están disponibles (Windows, Pi sin bluetooth) el transporte
queda inhabilitado y `app.py` sigue arrancando normalmente.
"""
from __future__ import annotations

import json
import re
import subprocess
import sys
import threading
from typing import Callable, Sequence

from ble_setup import (
    ADV_NAME,
    CHAR_CONNECT,
    CHAR_PIN,
    CHAR_SCAN,
    CHAR_STATUS,
    SERVICE_UUID,
    BleSetupSession,
)
from debug_logger import log_action

APP_PATH = "/com/teo/ble"
ADV_PATH = "/com/teo/ble/advertisement0"
SERVICE_PATH = "/com/teo/ble/service0"
CHAR_PIN_PATH = f"{SERVICE_PATH}/charpin"
CHAR_SCAN_PATH = f"{SERVICE_PATH}/charscan"
CHAR_CONNECT_PATH = f"{SERVICE_PATH}/charconnect"
CHAR_STATUS_PATH = f"{SERVICE_PATH}/charstatus"
DEFAULT_ADAPTER_PATH = "/org/bluez/hci0"

BLUEZ_SERVICE = "org.bluez"
DBUS_OM_IFACE = "org.freedesktop.DBus.ObjectManager"
DBUS_PROP_IFACE = "org.freedesktop.DBus.Properties"
GATT_MANAGER_IFACE = "org.bluez.GattManager1"
GATT_SERVICE_IFACE = "org.bluez.GattService1"
GATT_CHRC_IFACE = "org.bluez.GattCharacteristic1"
LE_ADV_MANAGER_IFACE = "org.bluez.LEAdvertisingManager1"
LE_ADVERTISEMENT_IFACE = "org.bluez.LEAdvertisement1"
ADAPTER_IFACE = "org.bluez.Adapter1"

_UNAVAILABLE_MSG = "BLE setup omitido: dbus/BlueZ no disponible"
# Tope del acumulador de escrituras largas: un JSON de credenciales real no
# pasa de ~200 bytes.
REGISTER_TIMEOUT_S = 20.0
MAX_WRITE_BYTES = 512
MGMT_ADV_INSTANCE = "1"
_ANSI_RE = re.compile(r"\x1b\[[0-9;]*m")
_MGMT_FAIL_MARKERS = (
    "permission denied",
    "invalid parameters",
    "failed with status",
)

def format_dbus_import_error(exc: BaseException, executable: str) -> str:
    """Texto de diagnóstico cuando el venv no tiene dbus/gi (no el python del sistema)."""
    return f"{exc} (python={executable})"


def strip_ansi(text: str) -> str:
    return _ANSI_RE.sub("", text or "")


def mgmt_output_failed(output: str) -> bool:
    """True si btmgmt pintó un fallo aunque el proceso haya devuelto 0."""
    text = strip_ansi(output).lower()
    return any(marker in text for marker in _MGMT_FAIL_MARKERS)


def mgmt_advertise_argvs(name: str) -> list[list[str]]:
    """Comandos btmgmt que esta Pi acepta. RegisterAdvertisement de BlueZ cae en 0x0d."""
    return [
        ["clr-adv"],
        ["name", name, name],
        ["add-adv", "-c", "-g", "-n", MGMT_ADV_INSTANCE],
    ]


def mgmt_stop_argvs() -> list[list[str]]:
    return [["clr-adv"], ["advertising", "off"]]


def run_btmgmt(
    args: Sequence[str],
    runner: Callable[..., subprocess.CompletedProcess] = subprocess.run,
) -> tuple[int, str]:
    """Corre btmgmt. Primero `sudo -n` (NOPASSWD), después sin sudo."""
    commands = (
        ["sudo", "-n", "btmgmt", *args],
        ["btmgmt", *args],
    )
    last_rc, last_out = 127, "btmgmt no está"
    for cmd in commands:
        try:
            result = runner(cmd, capture_output=True, text=True, timeout=8)
        except FileNotFoundError:
            continue
        except Exception as exc:
            last_rc, last_out = 1, str(exc)
            continue
        out = f"{result.stdout or ''}{result.stderr or ''}"
        last_rc, last_out = result.returncode, out
        if result.returncode == 0 and not mgmt_output_failed(out):
            return 0, out
    return last_rc, last_out


def enable_mgmt_advertising(
    name: str,
    runner: Callable[..., subprocess.CompletedProcess] = subprocess.run,
) -> bool:
    """Nombre TEO + anuncio connectable con el nombre en scan response."""
    run_btmgmt(["clr-adv"], runner=runner)
    run_btmgmt(["name", name, name], runner=runner)
    rc, out = run_btmgmt(
        ["add-adv", "-c", "-g", "-n", MGMT_ADV_INSTANCE], runner=runner
    )
    return rc == 0 and not mgmt_output_failed(out)


def disable_mgmt_advertising(
    runner: Callable[..., subprocess.CompletedProcess] = subprocess.run,
) -> None:
    for args in mgmt_stop_argvs():
        run_btmgmt(args, runner=runner)


try:  # pragma: no cover - depende del entorno Linux con BlueZ
    import dbus
    import dbus.mainloop.glib
    import dbus.service
    from gi.repository import GLib

    _IMPORT_ERROR = ""
except Exception as _exc:  # pragma: no cover - Windows / Pi sin dbus
    dbus = None  # type: ignore[assignment]
    GLib = None  # type: ignore[assignment]
    _IMPORT_ERROR = format_dbus_import_error(_exc, sys.executable)

_classes: dict[str, type] | None = None


# ----------------------------------------------------------------------------
# Helpers de offset (puros: se testean en Windows, sin dbus)
# ----------------------------------------------------------------------------

def read_offset(options) -> int:
    """Offset de una opción D-Bus `a{sv}`; 0 si falta o es inválido.

    BlueZ manda `offset` en las lecturas largas (blob) y en las escrituras
    preparadas. Ignorarlo devuelve el payload desde el byte 0 en cada trozo,
    y el cliente concatena basura.
    """
    if not options:
        return 0
    try:
        value = options.get("offset", 0)
    except Exception:
        return 0
    try:
        offset = int(value)
    except (TypeError, ValueError):
        return 0
    return offset if offset > 0 else 0


def slice_payload(payload: bytes, offset: int) -> bytes:
    """Trozo del payload que le toca a una lectura con `offset`."""
    data = bytes(payload or b"")
    if offset <= 0:
        return data
    if offset >= len(data):
        return b""
    return data[offset:]


def accumulate_write(buffer: bytearray, offset: int, chunk: bytes) -> bytearray:
    """Ensambla una escritura larga: `offset == 0` reinicia el buffer.

    Levanta `ValueError` si el offset deja un hueco (escritura fuera de orden),
    que es lo que corresponde rechazar con `org.bluez.Error.InvalidOffset`.
    """
    data = bytes(chunk or b"")
    if offset <= 0:
        return bytearray(data)
    if offset > len(buffer):
        raise ValueError("offset con hueco")
    result = bytearray(buffer)
    result[offset:offset + len(data)] = data
    return result


def json_payload_complete(payload: bytes) -> bool:
    """True si el acumulado ya es un objeto JSON completo y decodificable.

    BlueZ no avisa cuál es el último trozo de una escritura preparada, así que
    el criterio de fin es que el payload parsee como objeto JSON.
    """
    if not payload:
        return False
    try:
        data = json.loads(bytes(payload).decode("utf-8"))
    except Exception:
        return False
    return isinstance(data, dict)


def _build_classes() -> dict[str, type]:
    """Define las clases D-Bus una sola vez (requieren `dbus` importado)."""
    global _classes
    if _classes is not None:
        return _classes

    class Advertisement(dbus.service.Object):  # type: ignore[misc]
        """`org.bluez.LEAdvertisement1` con LocalName=TEO."""

        def __init__(self, bus) -> None:
            super().__init__(bus, ADV_PATH)

        def get_properties(self) -> dict:
            # BlueZ 5.82 en esta Pi rechazó IncludeTxPower + ServiceUUIDs
            # (Invalid Parameters 0x0d, MaxAdvLen 31). El scan Android filtra
            # por nombre TEO.
            return {
                LE_ADVERTISEMENT_IFACE: {
                    "Type": dbus.String("peripheral"),
                    "LocalName": dbus.String(ADV_NAME),
                    "Discoverable": dbus.Boolean(True),
                    "MinInterval": dbus.UInt32(100),
                    "MaxInterval": dbus.UInt32(200),
                }
            }

        @dbus.service.method(DBUS_PROP_IFACE, in_signature="s", out_signature="a{sv}")
        def GetAll(self, interface):  # noqa: N802 - nombre fijado por D-Bus
            if interface != LE_ADVERTISEMENT_IFACE:
                raise dbus.exceptions.DBusException("org.bluez.Error.InvalidArguments")
            return self.get_properties()[LE_ADVERTISEMENT_IFACE]

        @dbus.service.method(LE_ADVERTISEMENT_IFACE, in_signature="", out_signature="")
        def Release(self):  # noqa: N802 - nombre fijado por D-Bus
            log_action("WIFI_BLE", "advertisement liberado por BlueZ")

    class Characteristic(dbus.service.Object):  # type: ignore[misc]
        """`org.bluez.GattCharacteristic1` genérica con handlers inyectados."""

        def __init__(
            self,
            bus,
            path: str,
            uuid: str,
            flags: list[str],
            service_path: str,
            on_read: Callable[[], bytes] | None = None,
            on_write: Callable[[bytes], None] | None = None,
            chunked_write: bool = False,
        ) -> None:
            self._path = path
            self._uuid = uuid
            self._flags = flags
            self._service_path = service_path
            self._on_read = on_read
            self._on_write = on_write
            self._chunked_write = chunked_write
            self._read_blob: bytes = b""
            self._write_buffer = bytearray()
            self.notifying = False
            self.value: bytes = b""
            super().__init__(bus, path)

        @property
        def path(self) -> str:
            return self._path

        def get_properties(self) -> dict:
            return {
                GATT_CHRC_IFACE: {
                    "Service": dbus.ObjectPath(self._service_path),
                    "UUID": self._uuid,
                    "Flags": dbus.Array(self._flags, signature="s"),
                    "Notifying": dbus.Boolean(self.notifying),
                }
            }

        @dbus.service.method(DBUS_PROP_IFACE, in_signature="s", out_signature="a{sv}")
        def GetAll(self, interface):  # noqa: N802
            if interface != GATT_CHRC_IFACE:
                raise dbus.exceptions.DBusException("org.bluez.Error.InvalidArguments")
            return self.get_properties()[GATT_CHRC_IFACE]

        @dbus.service.signal(DBUS_PROP_IFACE, signature="sa{sv}as")
        def PropertiesChanged(self, interface, changed, invalidated):  # noqa: N802
            pass

        @dbus.service.method(GATT_CHRC_IFACE, in_signature="a{sv}", out_signature="ay")
        def ReadValue(self, options):  # noqa: N802
            if self._on_read is None:
                return dbus.Array([], signature="y")
            offset = read_offset(options)
            if offset == 0:
                # Primer trozo: se congela el payload para que las lecturas
                # siguientes del mismo blob devuelvan el mismo contenido.
                try:
                    self._read_blob = bytes(self._on_read() or b"")
                except Exception as exc:
                    log_action("WIFI_BLE", f"ReadValue falló en {self._uuid}: {exc}")
                    self._read_blob = b"[]"
            payload = slice_payload(self._read_blob, offset)
            return dbus.Array(bytearray(payload), signature="y")

        @dbus.service.method(GATT_CHRC_IFACE, in_signature="aya{sv}", out_signature="")
        def WriteValue(self, value, options):  # noqa: N802
            if self._on_write is None:
                return
            chunk = bytes(bytearray(value))
            offset = read_offset(options)
            if not self._chunked_write:
                if offset != 0:
                    raise dbus.exceptions.DBusException(
                        "org.bluez.Error.NotSupported"
                    )
                self._dispatch_write(chunk)
                return
            try:
                self._write_buffer = accumulate_write(
                    self._write_buffer, offset, chunk
                )
            except ValueError:
                self._write_buffer = bytearray()
                raise dbus.exceptions.DBusException("org.bluez.Error.InvalidOffset")
            payload = bytes(self._write_buffer)
            if not json_payload_complete(payload):
                if len(payload) < MAX_WRITE_BYTES:
                    # Falta cola: esperamos el resto de la escritura larga.
                    return
                # Nunca va a cerrar: entregamos igual para que el coordinator
                # conteste `failed:session` en lugar de quedarse mudo.
                log_action(
                    "WIFI_BLE",
                    f"escritura larga sin cierre en {self._uuid}, la descarto",
                )
            self._write_buffer = bytearray()
            self._dispatch_write(payload)

        def _dispatch_write(self, payload: bytes) -> None:
            try:
                self._on_write(payload)
            except Exception as exc:
                log_action("WIFI_BLE", f"WriteValue falló en {self._uuid}: {exc}")

        @dbus.service.method(GATT_CHRC_IFACE, in_signature="", out_signature="")
        def StartNotify(self):  # noqa: N802
            self.notifying = True

        @dbus.service.method(GATT_CHRC_IFACE, in_signature="", out_signature="")
        def StopNotify(self):  # noqa: N802
            self.notifying = False

        def push(self, payload: bytes) -> None:
            """Emite PropertiesChanged con el nuevo Value si hay suscriptor."""
            self.value = payload
            if not self.notifying:
                return
            try:
                self.PropertiesChanged(
                    GATT_CHRC_IFACE,
                    {"Value": dbus.Array(bytearray(payload), signature="y")},
                    [],
                )
            except Exception as exc:
                log_action("WIFI_BLE", f"no pude notificar por BLE: {exc}")

    class Service(dbus.service.Object):  # type: ignore[misc]
        """`org.bluez.GattService1` primaria de setup WiFi."""

        def __init__(self, bus) -> None:
            self.characteristics: list = []
            super().__init__(bus, SERVICE_PATH)

        def get_properties(self) -> dict:
            return {
                GATT_SERVICE_IFACE: {
                    "UUID": SERVICE_UUID,
                    "Primary": dbus.Boolean(True),
                    "Characteristics": dbus.Array(
                        [dbus.ObjectPath(c.path) for c in self.characteristics],
                        signature="o",
                    ),
                }
            }

        @dbus.service.method(DBUS_PROP_IFACE, in_signature="s", out_signature="a{sv}")
        def GetAll(self, interface):  # noqa: N802
            if interface != GATT_SERVICE_IFACE:
                raise dbus.exceptions.DBusException("org.bluez.Error.InvalidArguments")
            return self.get_properties()[GATT_SERVICE_IFACE]

    class Application(dbus.service.Object):  # type: ignore[misc]
        """Raíz `/com/teo/ble` que expone el árbol GATT a BlueZ."""

        def __init__(self, bus) -> None:
            self.service = None
            super().__init__(bus, APP_PATH)

        @dbus.service.method(DBUS_OM_IFACE, out_signature="a{oa{sa{sv}}}")
        def GetManagedObjects(self):  # noqa: N802
            objects: dict = {}
            if self.service is None:
                return objects
            objects[dbus.ObjectPath(SERVICE_PATH)] = self.service.get_properties()
            for chrc in self.service.characteristics:
                objects[dbus.ObjectPath(chrc.path)] = chrc.get_properties()
            return objects

    _classes = {
        "Advertisement": Advertisement,
        "Characteristic": Characteristic,
        "Service": Service,
        "Application": Application,
    }
    return _classes


class BluezGattTransport:
    """Anuncia el servicio de setup WiFi por BLE usando BlueZ vía D-Bus."""

    def __init__(self, adapter_path: str = DEFAULT_ADAPTER_PATH) -> None:
        self._adapter_path = adapter_path
        self._lock = threading.RLock()
        self._bus = None
        self._loop = None
        self._thread: threading.Thread | None = None
        self._app = None
        self._service = None
        self._adv = None
        self._chars: list = []
        self._status_char = None
        self._session: BleSetupSession | None = None
        self._running = False
        self._reason = _IMPORT_ERROR
        self._available = self._probe()
        self._pending_start: tuple | None = None
        self._start_ready = threading.Event()
        self._start_error: list[BaseException] = []

    # ------------------------------------------------------------------
    # Disponibilidad
    # ------------------------------------------------------------------
    @property
    def available(self) -> bool:
        """True solo si dbus, BlueZ y un adaptador están presentes."""
        return self._available

    @property
    def unavailable_reason(self) -> str:
        """Motivo por el que el transporte no puede anunciar."""
        return self._reason

    def _probe(self) -> bool:
        if dbus is None or GLib is None:
            self._reason = self._reason or "dbus/gi no importable"
            log_action("WIFI_BLE", _UNAVAILABLE_MSG)
            return False
        try:
            dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
            bus = dbus.SystemBus()
            manager = dbus.Interface(
                bus.get_object(BLUEZ_SERVICE, "/"), DBUS_OM_IFACE
            )
            objects = manager.GetManagedObjects()
        except Exception as exc:
            self._reason = f"no pude hablar con BlueZ: {exc}"
            log_action("WIFI_BLE", _UNAVAILABLE_MSG)
            return False
        adapters = [path for path, ifaces in objects.items() if ADAPTER_IFACE in ifaces]
        if not adapters:
            self._reason = "sin adaptador bluetooth"
            log_action("WIFI_BLE", _UNAVAILABLE_MSG)
            return False
        if self._adapter_path not in adapters:
            self._adapter_path = str(adapters[0])
        return True

    # ------------------------------------------------------------------
    # GattTransport
    # ------------------------------------------------------------------
    def start(
        self,
        session: BleSetupSession,
        on_scan: Callable[[], bytes],
        on_connect: Callable[[bytes], None],
        on_pin: Callable[[bytes], None] | None = None,
    ) -> None:
        """Registra el árbol GATT y arranca el anuncio. No-op sin BlueZ.

        BlueZ responde RegisterApplication/RegisterAdvertisement por el main
        loop de GLib. Hay que pedirlos async y *después* entrar a loop.run().
        """
        with self._lock:
            if not self._available or GLib is None or dbus is None:
                log_action("WIFI_BLE", _UNAVAILABLE_MSG)
                return
            if self._running:
                return
            self._session = session
            self._pending_start = (session, on_scan, on_connect, on_pin)
            self._start_ready = threading.Event()
            self._start_error = []
            self._running = True
            self._thread = threading.Thread(
                target=self._run_loop, name="BluezGattLoop", daemon=True
            )
            self._thread.start()

        if not self._start_ready.wait(REGISTER_TIMEOUT_S):
            log_action("WIFI_BLE", "timeout registrando GATT/anuncio BLE")
            self.stop()
            raise RuntimeError("timeout registrando GATT/anuncio BLE")
        if self._start_error:
            err = self._start_error[0]
            self.stop()
            raise err

    def stop(self) -> None:
        """Da de baja anuncio y aplicación, y corta el main loop."""
        with self._lock:
            if not self._running:
                return
            self._running = False
            # I7: el último `notify_status` (típicamente `connected`) está
            # encolado con idle_add; sin este flush el `quit()` lo descarta y
            # el padre nunca ve el éxito.
            self._flush_notifications()
            try:
                disable_mgmt_advertising()
            except Exception as exc:
                log_action("WIFI_BLE", f"error al bajar anuncio btmgmt: {exc}")
            try:
                adapter = self._adapter_object()
                dbus.Interface(adapter, GATT_MANAGER_IFACE).UnregisterApplication(
                    dbus.ObjectPath(APP_PATH)
                )
            except Exception as exc:
                log_action("WIFI_BLE", f"error al desregistrar BLE: {exc}")
            if self._loop is not None:
                try:
                    self._loop.quit()
                except Exception as exc:
                    log_action("WIFI_BLE", f"error al cerrar main loop: {exc}")
                self._loop = None
            self._thread = None
            self._teardown_objects()
            log_action("WIFI_BLE", "anuncio BLE detenido")

    def notify_status(self, status: str) -> None:
        """Publica el estado por la característica STATUS."""
        char = self._status_char
        if char is None or GLib is None:
            return
        payload = status.encode("utf-8")

        def _emit() -> bool:
            char.push(payload)
            return False

        try:
            GLib.idle_add(_emit)
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude encolar estado BLE: {exc}")

    # ------------------------------------------------------------------
    # Internos
    # ------------------------------------------------------------------
    def _flush_notifications(self, timeout_s: float = 1.5) -> None:
        """Espera a que corran los idle pendientes (FIFO) antes de cortar.

        Se encola un centinela con la misma prioridad que los push de estado:
        cuando se ejecuta, los anteriores ya salieron. Si el llamador es el
        propio hilo del loop no se espera (sería un deadlock).
        """
        loop = self._loop
        if loop is None or GLib is None:
            return
        if self._thread is not None and self._thread is threading.current_thread():
            return
        if not self._thread or not self._thread.is_alive():
            return
        done = threading.Event()

        def _mark() -> bool:
            done.set()
            return False

        try:
            GLib.idle_add(_mark)
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude encolar el flush BLE: {exc}")
            return
        if not done.wait(timeout_s):
            log_action("WIFI_BLE", "flush de notificaciones BLE vencido")

    def _adapter_object(self):
        if self._bus is None:
            self._bus = dbus.SystemBus()
        return self._bus.get_object(BLUEZ_SERVICE, self._adapter_path)

    def _setup_objects(
        self,
        session: BleSetupSession,
        on_scan: Callable[[], bytes],
        on_connect: Callable[[bytes], None],
        on_pin: Callable[[bytes], None] | None = None,
    ) -> None:
        classes = _build_classes()
        self._bus = dbus.SystemBus()
        bus = self._bus

        self._app = classes["Application"](bus)
        self._service = classes["Service"](bus)
        characteristic = classes["Characteristic"]

        def _on_pin_fallback(raw: bytes) -> None:
            """Sin coordinator: verificamos contra la sesión y notificamos."""
            try:
                pin = raw.decode("ascii").strip()
            except Exception:
                self.notify_status("failed:pin")
                return
            self.notify_status(session.verify_pin(pin))

        pin_char = characteristic(
            bus,
            CHAR_PIN_PATH,
            CHAR_PIN,
            ["write", "write-without-response"],
            SERVICE_PATH,
            on_write=on_pin or _on_pin_fallback,
        )
        scan_char = characteristic(
            bus,
            CHAR_SCAN_PATH,
            CHAR_SCAN,
            ["read"],
            SERVICE_PATH,
            on_read=on_scan,
        )
        connect_char = characteristic(
            bus,
            CHAR_CONNECT_PATH,
            CHAR_CONNECT,
            ["write"],
            SERVICE_PATH,
            on_write=on_connect,
            chunked_write=True,
        )
        status_char = characteristic(
            bus,
            CHAR_STATUS_PATH,
            CHAR_STATUS,
            ["notify"],
            SERVICE_PATH,
        )

        self._chars = [pin_char, scan_char, connect_char, status_char]
        self._status_char = status_char
        self._service.characteristics = list(self._chars)
        self._app.service = self._service
        self._adv = None

    def _set_adapter_alias(self) -> None:
        adapter = self._adapter_object()
        props = dbus.Interface(adapter, DBUS_PROP_IFACE)
        try:
            props.Set(ADAPTER_IFACE, "Alias", dbus.String(ADV_NAME))
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude poner Alias={ADV_NAME}: {exc}")
        try:
            props.Set(ADAPTER_IFACE, "Discoverable", dbus.Boolean(True))
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude poner Discoverable: {exc}")

    def _register_with_bluez(self) -> None:
        """GATT por D-Bus. El anuncio va por btmgmt: BlueZ Add Advertising falla 0x0d."""
        adapter = self._adapter_object()
        gatt = dbus.Interface(adapter, GATT_MANAGER_IFACE)

        def _fail(where: str, err: object) -> None:
            log_action("WIFI_BLE", f"no pude registrar {where}: {err}")
            self._start_error.append(RuntimeError(f"{where}: {err}"))
            self._start_ready.set()
            loop = self._loop
            if loop is not None:
                loop.quit()

        def _app_ok(*_args: object) -> None:
            self._set_adapter_alias()
            if not enable_mgmt_advertising(ADV_NAME):
                _fail("anuncio", "btmgmt add-adv no dejó el peluche visible como TEO")
                return
            log_action("WIFI_BLE", f"anunciando BLE como '{ADV_NAME}'")
            self._start_ready.set()

        gatt.RegisterApplication(
            dbus.ObjectPath(APP_PATH),
            {},
            reply_handler=_app_ok,
            error_handler=lambda e: _fail("GATT", e),
        )

    def _teardown_objects(self) -> None:
        for obj in list(self._chars) + [self._service, self._app, self._adv]:
            if obj is None:
                continue
            try:
                obj.remove_from_connection()
            except Exception:
                pass
        self._chars = []
        self._status_char = None
        self._service = None
        self._app = None
        self._adv = None
        self._session = None

    def _run_loop(self) -> None:
        try:
            dbus.mainloop.glib.DBusGMainLoop(set_as_default=True)
            self._loop = GLib.MainLoop()
            pending = self._pending_start
            if pending is None:
                self._start_ready.set()
                return
            self._setup_objects(*pending)
            self._register_with_bluez()
            self._loop.run()
        except Exception as exc:
            log_action("WIFI_BLE", f"main loop BLE terminó: {exc}")
            if not self._start_ready.is_set():
                self._start_error.append(exc)
                self._start_ready.set()
