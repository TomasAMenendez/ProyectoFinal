"""Sesión BLE de setup WiFi: PIN en RAM y renderizado para LCD."""
from __future__ import annotations

import json
import secrets
import threading
import time
from typing import Callable, Protocol

from PIL import Image, ImageDraw, ImageFont

from debug_logger import log_action

SETUP_TIMEOUT_S = 600.0
SETUP_RETRY_S = 300.0
BOOT_DELAY_S = 5.0
TICK_INTERVAL_S = 2.0
# Vida útil de la lista de redes cacheada. Corta para que el padre vea redes
# actuales, pero suficiente para que releer en cada `pin_ok` no dispare un
# `nmcli` nuevo (que a su vez emitiría `scanning` y provocaría otra relectura).
SCAN_CACHE_TTL_S = 20.0
PIN_MAX_FAILURES = 5

# Contrato de estados notify del plan: no se puede emitir nada fuera de acá.
# Cualquier error interno (payload roto, scan caído, excepción) es
# `failed:session`; la expiración de la sesión y el timeout de nmcli son
# `failed:timeout`.
STATUS_VALUES = frozenset(
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
)
# Resultados de `WifiProvisioner.connect` que tienen un estado propio.
_CONNECT_FAILURES = {"auth": "failed:auth", "timeout": "failed:timeout"}
ADV_NAME = "TEO"
SERVICE_UUID = "74656f57-4946-4000-8000-0000a0000001"
CHAR_PIN = "74656f57-4946-4000-8000-0000a0000002"
CHAR_SCAN = "74656f57-4946-4000-8000-0000a0000003"
CHAR_CONNECT = "74656f57-4946-4000-8000-0000a0000004"
CHAR_STATUS = "74656f57-4946-4000-8000-0000a0000005"

_BG = "#1a1a2e"
_TEXT = "#ffffff"
_TTF_CANDIDATES = (
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
    "DejaVuSans.ttf",
)


def _default_rng() -> str:
    """Genera un PIN de 6 dígitos zero-padded."""
    return f"{secrets.randbelow(1000000):06d}"


class BleSetupSession:
    """Máquina de estados de la sesión BLE de provisionamiento WiFi."""

    def __init__(
        self,
        rng: Callable[[], str] | None = None,
        now: Callable[[], float] | None = None,
    ) -> None:
        self._rng = rng or _default_rng
        self._now = now or time.time
        self._lock = threading.Lock()
        self._pin = ""
        self._active = False
        self._pin_ok = False
        self._status = "idle"
        self._failures = 0
        self._start_time = 0.0

    @property
    def active(self) -> bool:
        with self._lock:
            return self._active

    @property
    def pin_ok(self) -> bool:
        with self._lock:
            return self._pin_ok

    @property
    def status(self) -> str:
        with self._lock:
            return self._status

    def start(self) -> str:
        """Inicia la sesión y devuelve el PIN generado (solo en RAM)."""
        with self._lock:
            self._pin = self._rng()
            self._active = True
            self._pin_ok = False
            self._status = "idle"
            self._failures = 0
            self._start_time = self._now()
            return self._pin

    def stop(self) -> None:
        """Finaliza la sesión y descarta el estado mutable."""
        with self._lock:
            self._active = False
            self._pin = ""
            self._pin_ok = False
            self._status = "idle"
            self._failures = 0
            self._start_time = 0.0

    def verify_pin(self, pin: str) -> str:
        """Verifica el PIN; rota tras demasiados fallos."""
        return self.verify_pin_detailed(pin)[0]

    def verify_pin_detailed(self, pin: str) -> tuple[str, str]:
        """Como `verify_pin`, pero informa el PIN nuevo cuando rota.

        Devuelve `(estado, pin_rotado)`; `pin_rotado` es `""` si no hubo
        rotación. El llamador necesita ese dato para repintar la LCD: si no lo
        hace, el padre queda mirando un PIN que ya no sirve.
        """
        with self._lock:
            if pin == self._pin:
                self._pin_ok = True
                self._status = "pin_ok"
                self._failures = 0
                return "pin_ok", ""
            self._failures += 1
            if self._failures >= PIN_MAX_FAILURES:
                self._pin = self._rng()
                self._failures = 0
                self._pin_ok = False
                self._status = "idle"
                return "failed:pin", self._pin
            return "failed:pin", ""

    def allow_scan_or_connect(self) -> bool:
        """True solo cuando el PIN fue validado correctamente."""
        with self._lock:
            return self._pin_ok

    def set_status(self, status: str) -> None:
        """Actualiza el estado reportado por BLE."""
        with self._lock:
            self._status = status

    def expired(self) -> bool:
        """True si la sesión superó SETUP_TIMEOUT_S desde start()."""
        with self._lock:
            if not self._active:
                return False
            return (self._now() - self._start_time) >= SETUP_TIMEOUT_S


class GattTransport(Protocol):
    """Contrato mínimo del transporte GATT (BlueZ real o falso en tests)."""

    def start(
        self,
        session: BleSetupSession,
        on_scan: Callable[[], bytes],
        on_connect: Callable[[bytes], None],
        on_pin: Callable[[bytes], None] | None = None,
    ) -> None: ...

    def stop(self) -> None: ...

    def notify_status(self, status: str) -> None: ...


def _default_spawn(work: Callable[[], None]) -> None:
    """Corre `work` en un hilo daemon: nunca bloquea el loop BLE ni el cierre."""
    threading.Thread(target=work, name="WifiConnect", daemon=True).start()


class WifiBleCoordinator:
    """Orquesta sesión BLE, LCD del PIN y provisionamiento WiFi por nmcli."""

    def __init__(
        self,
        provisioner,
        transport: GattTransport,
        eye_display,
        now: Callable[[], float] = time.monotonic,
        on_state: Callable[[str, bool], None] | None = None,
        spawn: Callable[[Callable[[], None]], None] | None = None,
    ) -> None:
        self._provisioner = provisioner
        self._transport = transport
        self._eye_display = eye_display
        self._now = now
        self._on_state = on_state
        self._spawn = spawn or _default_spawn
        self._lock = threading.RLock()
        self._connecting = False
        self._scanning = False
        self._scan_cache = b"[]"
        self._scan_cache_at: float | None = None
        self._retry_at = 0.0
        self.session = BleSetupSession(now=now)
        self.session_pin = ""

    # ------------------------------------------------------------------
    # Ciclo de vida
    # ------------------------------------------------------------------
    def enter_setup(self) -> bool:
        """Arranca sesión + PIN en LCD + anuncio BLE. Idempotente.

        Devuelve True si al volver hay una sesión anunciándose (la API usa eso
        para no prometerle al padre una pantalla que nunca va a progresar).
        """
        with self._lock:
            if self.session.active:
                return True
            if not getattr(self._transport, "available", True):
                reason = getattr(self._transport, "unavailable_reason", "desconocido")
                log_action("WIFI_BLE", f"no puedo anunciar BLE: {reason}")
                return False
            self._retry_at = 0.0
            self._scan_cache = b"[]"
            self._scan_cache_at = None
            self._scanning = False
            self.session_pin = self.session.start()
            self._paint_pin(self.session_pin)
            try:
                self._transport.start(
                    self.session,
                    self.handle_scan,
                    self.handle_connect,
                    self.handle_pin,
                )
            except Exception as exc:
                log_action("WIFI_BLE", f"no pude anunciar BLE: {exc}")
                self.session.stop()
                self.session_pin = ""
                self._clear_lcd()
                return False
            self._emit("ble_setup_active", True)
            log_action("WIFI_BLE", "setup BLE activo, PIN en pantalla")
            return True

    def leave_setup(self) -> None:
        """Corta el anuncio BLE, borra el PIN de la LCD y cierra la sesión."""
        with self._lock:
            if not self.session.active:
                return
            self.session.stop()
            self.session_pin = ""
            self._scan_cache = b"[]"
            self._scan_cache_at = None
            self._clear_lcd()
            try:
                self._transport.stop()
            except Exception as exc:
                log_action("WIFI_BLE", f"error al detener BLE: {exc}")
            self._emit("ble_setup_active", False)
            log_action("WIFI_BLE", "setup BLE finalizado")

    def mark_wifi_connected(self) -> None:
        """Publica que ya hay WiFi (usado por el chequeo de arranque)."""
        self._emit("wifi_connected", True)

    def tick(self) -> None:
        """Vigila timeout de sesión, WiFi por otra vía y reentrada al setup."""
        with self._lock:
            active = self.session.active
            expired = active and self.session.expired()
        if expired:
            log_action("WIFI_BLE", "setup BLE expirado por timeout")
            self._set_status("failed:timeout")
            self.leave_setup()
            with self._lock:
                # I9: sin esto el peluche queda sin red y sin radio hasta
                # reiniciar. Reintentamos mientras siga sin WiFi.
                self._retry_at = self._now() + SETUP_RETRY_S
            return
        if not active:
            self._maybe_reenter()
            return
        try:
            connected = bool(self._provisioner.is_connected())
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude consultar estado WiFi: {type(exc).__name__}")
            return
        if connected:
            self._emit("wifi_connected", True)
            log_action("WIFI_BLE", "WiFi ya conectado, cierro setup BLE")
            self.leave_setup()

    def _maybe_reenter(self) -> None:
        """Reentra al setup (PIN nuevo) si venció el retry y no hay WiFi."""
        with self._lock:
            retry_at = self._retry_at
            if retry_at <= 0.0 or self._now() < retry_at:
                return
            self._retry_at = 0.0
        try:
            connected = bool(self._provisioner.is_connected())
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude consultar estado WiFi: {type(exc).__name__}")
            with self._lock:
                self._retry_at = self._now() + SETUP_RETRY_S
            return
        if connected:
            self._emit("wifi_connected", True)
            return
        log_action("WIFI_BLE", "sigo sin WiFi: reabro el setup BLE con PIN nuevo")
        if not self.enter_setup():
            with self._lock:
                # No se pudo anunciar ahora; reintentamos en el próximo ciclo.
                self._retry_at = self._now() + SETUP_RETRY_S

    # ------------------------------------------------------------------
    # Callbacks del transporte (corren en el hilo BLE)
    # ------------------------------------------------------------------
    def handle_pin(self, raw: bytes) -> str:
        """Verifica el PIN escrito por BLE y repinta la LCD si rotó.

        Corre en el hilo de D-Bus: nunca bloquea (el scan se delega a un
        worker) y nunca loguea el PIN.
        """
        try:
            pin = bytes(raw).decode("ascii").strip()
        except Exception:
            pin = ""
        status, rotated = self.session.verify_pin_detailed(pin)
        if rotated:
            with self._lock:
                self.session_pin = rotated
                self._paint_pin(rotated)
            log_action("WIFI_BLE", "PIN rotado tras 5 intentos fallidos, LCD repintada")
        self._set_status(status)
        if status == "pin_ok":
            self._start_scan()
        return status

    def handle_scan(self) -> bytes:
        """Devuelve el último JSON de redes conocido; refresca si está viejo.

        `ReadValue` corre en el loop de GLib, así que acá no se puede llamar a
        nmcli (dos comandos de hasta 15 s congelarían las notificaciones y
        harían expirar la respuesta de D-Bus).

        El refresco se dispara **sólo** si el cache está vacío o venció
        `SCAN_CACHE_TTL_S`. La app relee esta característica en cada `pin_ok`,
        así que tratar toda lectura como refresco armaba un ciclo infinito
        (`pin_ok` → read → `scanning` → `pin_ok` → read → ...) que dejaba a la
        Pi escaneando para siempre y al spinner del padre prendiéndose y
        apagándose. Con cache fresco esta función no toca el estado ni lanza
        workers, y el ciclo se corta solo.
        """
        if not self.session.allow_scan_or_connect():
            self._set_status("failed:session")
            return b"[]"
        with self._lock:
            if self._scan_is_fresh():
                return self._scan_cache
        self._start_scan()
        with self._lock:
            return self._scan_cache

    def _scan_is_fresh(self) -> bool:
        """True si hay una lista publicada y todavía no venció el TTL.

        Debe llamarse con `self._lock` tomado.
        """
        if self._scan_cache_at is None:
            return False
        return (self._now() - self._scan_cache_at) < SCAN_CACHE_TTL_S

    def _start_scan(self) -> None:
        """Lanza el refresco de redes si no hay uno en curso."""
        with self._lock:
            if self._scanning or not self.session.active:
                return
            self._scanning = True
        self._set_status("scanning")
        try:
            self._spawn(self._scan_worker)
        except Exception as exc:
            with self._lock:
                self._scanning = False
            log_action("WIFI_BLE", f"no pude lanzar el worker de scan: {type(exc).__name__}")
            self._set_status("failed:session")

    def _scan_worker(self) -> None:
        """Corre nmcli fuera del loop BLE y publica la lista en el cache."""
        try:
            networks = self._provisioner.scan()
            payload = json.dumps(
                [
                    {
                        "ssid": net.ssid,
                        "signal": int(net.signal),
                        "secure": bool(net.secure),
                    }
                    for net in networks
                ]
            ).encode("utf-8")
        except Exception as exc:
            with self._lock:
                self._scanning = False
            log_action("WIFI_BLE", f"scan WiFi falló: {type(exc).__name__}")
            self._set_status("failed:session")
            return
        with self._lock:
            self._scan_cache = payload
            self._scan_cache_at = self._now()
            self._scanning = False
            # Si mientras escaneábamos el padre ya mandó credenciales, el
            # estado vigente es `connecting`: no lo pisamos con `pin_ok`.
            anunciar = not self._connecting and self.session.status == "scanning"
        log_action("WIFI_BLE", f"scan WiFi listo ({len(networks)} redes)")
        if anunciar:
            self._set_status("pin_ok")

    def handle_connect(self, raw: bytes) -> None:
        """Valida credenciales y delega el nmcli a un worker. Nunca levanta.

        Corre en el loop D-Bus: debe volver enseguida para que el `connecting`
        salga por notificación y la respuesta de WriteValue no expire.
        """
        if not self.session.allow_scan_or_connect():
            self._set_status("failed:session")
            return
        ssid, password = self._parse_credentials(raw)
        if ssid is None:
            self._set_status("failed:session")
            return
        with self._lock:
            if self._connecting:
                log_action("WIFI_BLE", "conexión WiFi ya en curso, ignoro el pedido")
                return
            self._connecting = True
        self._set_status("connecting")
        try:
            self._spawn(lambda: self._connect_worker(ssid, password))
        except Exception as exc:
            with self._lock:
                self._connecting = False
            log_action(
                "WIFI_BLE",
                f"no pude lanzar el worker de conexión: {type(exc).__name__}",
            )
            self._set_status("failed:session")

    def _connect_worker(self, ssid: str, password: str) -> None:
        """Ejecuta el nmcli fuera del loop BLE y publica el estado final."""
        try:
            try:
                result = self._provisioner.connect(ssid, password)
            except Exception as exc:
                # Nunca interpolar `exc`: las excepciones de subprocess llevan
                # el argv completo, y ahí adentro va `password <clave>`.
                log_action(
                    "WIFI_BLE",
                    f"conexión WiFi falló para '{ssid}': {type(exc).__name__}",
                )
                self._set_status("failed:session")
                return
            if result == "ok":
                self._set_status("connected")
                self._emit("wifi_connected", True)
                log_action("WIFI_BLE", f"WiFi conectado a '{ssid}'")
                self.leave_setup()
                return
            self._set_status(_CONNECT_FAILURES.get(result, "failed:session"))
            log_action("WIFI_BLE", f"WiFi rechazado para '{ssid}': {result}")
        finally:
            with self._lock:
                self._connecting = False

    # ------------------------------------------------------------------
    # Helpers internos
    # ------------------------------------------------------------------
    @staticmethod
    def _parse_credentials(raw: bytes) -> tuple[str | None, str]:
        """Parsea `{ssid, password}` tolerando bytes o JSON basura."""
        try:
            text = bytes(raw).decode("utf-8")
        except (UnicodeDecodeError, TypeError, ValueError):
            log_action("WIFI_BLE", "credenciales BLE no son UTF-8")
            return None, ""
        try:
            data = json.loads(text)
        except (ValueError, TypeError):
            log_action("WIFI_BLE", "credenciales BLE no son JSON válido")
            return None, ""
        if not isinstance(data, dict):
            log_action("WIFI_BLE", "credenciales BLE no son un objeto JSON")
            return None, ""
        ssid = data.get("ssid")
        if not isinstance(ssid, str) or not ssid.strip():
            log_action("WIFI_BLE", "credenciales BLE sin ssid")
            return None, ""
        password = data.get("password")
        if not isinstance(password, str):
            password = ""
        return ssid.strip(), password

    def _paint_pin(self, pin: str) -> None:
        if self._eye_display is None:
            return
        try:
            width = int(getattr(self._eye_display, "width", 320) or 320)
            height = int(getattr(self._eye_display, "height", 240) or 240)
            self._eye_display.set_lcd_override(render_pin_image(pin, width, height))
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude pintar el PIN: {exc}")

    def _clear_lcd(self) -> None:
        if self._eye_display is None:
            return
        try:
            self._eye_display.set_lcd_override(None)
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude limpiar la LCD: {exc}")

    def _set_status(self, status: str) -> None:
        if status not in STATUS_VALUES:
            # Red de seguridad: la app sólo entiende los nueve estados del
            # contrato; cualquier otro se reporta como error genérico.
            log_action("WIFI_BLE", f"estado fuera de contrato '{status}', uso failed:session")
            status = "failed:session"
        self.session.set_status(status)
        try:
            self._transport.notify_status(status)
        except Exception as exc:
            log_action("WIFI_BLE", f"no pude notificar estado '{status}': {exc}")

    def _emit(self, key: str, value: bool) -> None:
        if self._on_state is None:
            return
        try:
            self._on_state(key, value)
        except Exception as exc:
            log_action("WIFI_BLE", f"callback de estado falló ({key}): {exc}")


def run_wifi_ble_loop(
    coord: "WifiBleCoordinator",
    provisioner,
    sleep: Callable[[float], None] = time.sleep,
    boot_delay_s: float = BOOT_DELAY_S,
    tick_interval_s: float = TICK_INTERVAL_S,
    iterations: int | None = None,
) -> None:
    """Chequeo de WiFi al arrancar y loop de `tick` para toda la vida de la app.

    El loop tiene que correr **siempre**, no sólo cuando el boot encontró la Pi
    sin WiFi: si se corta, el flujo "Cambiar red WiFi" (que sólo es alcanzable
    con WiFi andando) queda sin nadie que vigile el timeout de 10 minutos y el
    peluche se anuncia por BLE con el PIN en pantalla indefinidamente.

    `iterations` existe para los tests; en producción queda en None (infinito).
    """
    try:
        sleep(boot_delay_s)
        _boot_wifi_check(coord, provisioner)
        count = 0
        while iterations is None or count < iterations:
            count += 1
            sleep(tick_interval_s)
            try:
                coord.tick()
            except Exception as exc:
                log_action("WIFI_BLE", f"tick de setup WiFi falló: {type(exc).__name__}")
    except Exception as exc:
        log_action("WIFI_BLE", f"loop de setup WiFi BLE terminado: {exc}")


def _boot_wifi_check(coord: "WifiBleCoordinator", provisioner) -> None:
    """Entra al setup sólo si tras el boot no hay WiFi."""
    try:
        connected = bool(provisioner.is_connected())
    except Exception as exc:
        log_action("WIFI_BLE", f"no pude consultar WiFi al arrancar: {type(exc).__name__}")
        connected = False
    if connected:
        coord.mark_wifi_connected()
        log_action("WIFI_BLE", "WiFi ya conectado al arrancar; no anuncio BLE")
        return
    joined = _try_known_wifi(provisioner)
    if joined:
        coord.mark_wifi_connected()
        log_action("WIFI_BLE", f"WiFi conocido '{joined}'; no anuncio BLE")
        return
    log_action("WIFI_BLE", "ninguna red conocida conectó; anuncio BLE")
    coord.enter_setup()


def _try_known_wifi(provisioner) -> str | None:
    """Prueba known_wifi.json. None si ninguna conecta. No loguea claves."""
    try:
        return provisioner.try_known_networks()
    except Exception as exc:
        log_action("WIFI_BLE", f"redes conocidas fallaron: {type(exc).__name__}")
        return None


def _load_pin_font(size: int):
    """Fuente del PIN tolerante a Pillow < 10 (sin `load_default(size=...)`)."""
    try:
        return ImageFont.load_default(size=size)
    except TypeError:
        pass
    for path in _TTF_CANDIDATES:
        try:
            return ImageFont.truetype(path, size)
        except Exception:
            continue
    return ImageFont.load_default()


def render_pin_image(pin: str, width: int, height: int) -> Image.Image:
    """Renderiza el PIN centrado sobre fondo oscuro para la LCD."""
    image = Image.new("RGB", (width, height), _BG)
    draw = ImageDraw.Draw(image)
    font = _load_pin_font(max(height // 4, 24))
    bbox = draw.textbbox((0, 0), pin, font=font)
    text_w = bbox[2] - bbox[0]
    text_h = bbox[3] - bbox[1]
    x = (width - text_w) // 2
    y = (height - text_h) // 2
    draw.text((x, y), pin, fill=_TEXT, font=font)
    return image
