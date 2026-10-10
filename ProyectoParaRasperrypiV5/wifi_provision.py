"""Provisionamiento WiFi vía nmcli (sin BLE ni radio en tests)."""
from __future__ import annotations

import re
import subprocess
import time
from dataclasses import dataclass
from typing import Callable

Runner = Callable[[list[str], float], tuple[int, str, str]]

SCAN_TIMEOUT_S = 15.0
CONNECT_TIMEOUT_S = 30.0
# nmcli, sin este tope, espera al agente de claves ~90s cuando el PSK es malo.
CONNECT_WAIT_S = 20.0


@dataclass(frozen=True)
class WifiNetwork:
    ssid: str
    signal: int
    secure: bool


def split_nmcli_fields(line: str) -> list[str]:
    """Separa campos de `nmcli -t` respetando el escape con backslash.

    En modo terse nmcli escapa los dos puntos literales del valor (`My\\:Net`)
    y los propios backslashes (`\\\\`), así que un split simple los rompe.
    """
    fields: list[str] = []
    current: list[str] = []
    index = 0
    length = len(line)
    while index < length:
        char = line[index]
        if char == "\\" and index + 1 < length:
            current.append(line[index + 1])
            index += 2
            continue
        if char == ":":
            fields.append("".join(current))
            current = []
            index += 1
            continue
        current.append(char)
        index += 1
    fields.append("".join(current))
    return fields


def parse_nmcli_wifi_list(text: str) -> list[WifiNetwork]:
    """Parsea salida `nmcli -t -f SSID,SIGNAL,SECURITY dev wifi`."""
    best: dict[str, WifiNetwork] = {}
    for line in text.splitlines():
        line = line.strip()
        if not line:
            continue
        parts = split_nmcli_fields(line)
        if len(parts) < 2:
            continue
        ssid = parts[0].strip()
        if not ssid or ssid == "--":
            continue
        try:
            signal = int(parts[1].strip())
        except ValueError:
            continue
        security = parts[2].strip() if len(parts) > 2 else ""
        secure = bool(security) and security != "--"
        prev = best.get(ssid)
        if prev is None or signal > prev.signal:
            best[ssid] = WifiNetwork(ssid=ssid, signal=signal, secure=secure)
    return sorted(best.values(), key=lambda n: (-n.signal, n.ssid))


def parse_nmcli_active_wifi_name(text: str) -> str | None:
    """Nombre de la conexión WiFi activa.

    Entrada: `nmcli -t -f NAME,TYPE connection show --active`.
    """
    for line in text.splitlines():
        parts = split_nmcli_fields(line.strip())
        if len(parts) < 2:
            continue
        name = parts[0].strip()
        kind = parts[1].strip()
        if name and kind == "802-11-wireless":
            return name
    return None


def parse_nmcli_device_wifi_connected(text: str) -> bool:
    """True si algún dispositivo wifi está en estado connected."""
    for line in text.splitlines():
        line = line.strip()
        if not line:
            continue
        parts = split_nmcli_fields(line)
        if len(parts) < 3:
            continue
        if parts[1] == "wifi" and parts[2] == "connected":
            return True
    return False


_ADDED_UUID = re.compile(
    r"\(([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\)"
)


def parse_nmcli_connection_rows(text: str) -> list[tuple[str, str, str]]:
    """Filas `nmcli -t -f NAME,UUID,TYPE connection show`."""
    rows: list[tuple[str, str, str]] = []
    for line in text.splitlines():
        parts = split_nmcli_fields(line.strip())
        if len(parts) < 3:
            continue
        rows.append((parts[0], parts[1], parts[2]))
    return rows


def parse_nmcli_added_uuid(text: str) -> str | None:
    """UUID que imprime `nmcli connection add`."""
    match = _ADDED_UUID.search(text)
    if match is None:
        return None
    return match.group(1)


def classify_nmcli_connect(
    returncode: int,
    stderr: str,
    elapsed_s: float,
    timeout_s: float = CONNECT_TIMEOUT_S,
) -> str:
    """Clasifica el resultado de conectar una red.

    `key-mgmt: property is missing` no es una clave mal puesta: nmcli armó
    mal el perfil. Una clave rechazada pide otro secreto y `nmcli -w` corta
    con Timeout; eso también es clave rechazada.
    """
    if elapsed_s >= timeout_s:
        return "timeout"
    if returncode == 0:
        return "ok"
    if "Secrets were required" in stderr or "Timeout" in stderr:
        return "auth"
    return "error"


class WifiCommandError(RuntimeError):
    """Fallo de un comando nmcli, ya sanitizado (nunca lleva el argv).

    El argv de `nmcli dev wifi connect` contiene `password <clave>`, y las
    excepciones de `subprocess` lo interpolan en su mensaje. Toda excepción del
    runner se traduce a esta clase con un texto fijo para que ni un log ni un
    traceback puedan terminar con la clave WiFi escrita en disco.
    """


class WifiCommandTimeout(WifiCommandError):
    """El comando nmcli superó su timeout."""


def _safe_command_name(argv: list[str]) -> str:
    """Nombre del comando sin argumentos: nunca puede contener la clave."""
    try:
        return str(argv[0])
    except Exception:
        return "nmcli"


def _default_runner(argv: list[str], timeout: float) -> tuple[int, str, str]:
    name = _safe_command_name(argv)
    try:
        result = subprocess.run(
            argv,
            capture_output=True,
            text=True,
            timeout=timeout,
            check=False,
        )
    except subprocess.TimeoutExpired:
        raise WifiCommandTimeout(f"{name}: timeout") from None
    except Exception as exc:
        raise WifiCommandError(f"{name}: {type(exc).__name__}") from None
    return result.returncode, result.stdout, result.stderr


class WifiProvisioner:
    """Ejecuta nmcli para escanear y conectar WiFi."""

    def __init__(self, runner: Runner | None = None) -> None:
        self._runner = runner or _default_runner

    def _run(self, argv: list[str], timeout: float) -> tuple[int, str, str]:
        """Corre el runner traduciendo TODA excepción a `WifiCommandError`.

        Un runner inyectado (o `subprocess`) puede levantar excepciones cuyo
        mensaje incluye el argv completo; acá se cortan en la fuente.
        """
        try:
            return self._runner(argv, timeout)
        except WifiCommandError:
            raise
        except subprocess.TimeoutExpired:
            raise WifiCommandTimeout(f"{_safe_command_name(argv)}: timeout") from None
        except Exception as exc:
            raise WifiCommandError(
                f"{_safe_command_name(argv)}: {type(exc).__name__}"
            ) from None

    def is_connected(self) -> bool:
        try:
            code, stdout, _ = self._run(
                ["nmcli", "-t", "-f", "DEVICE,TYPE,STATE", "device"],
                SCAN_TIMEOUT_S,
            )
        except WifiCommandError:
            return False
        if code != 0:
            return False
        return parse_nmcli_device_wifi_connected(stdout)

    def scan(self) -> list[WifiNetwork]:
        """Lista redes frescas. `--rescan yes` espera el barrido.

        Sin ese barrido, nmcli solo devuelve la red ya conectada. Si el
        barrido no está permitido, se usa el cache.
        """
        fresh = [
            "nmcli",
            "-t",
            "-f",
            "SSID,SIGNAL,SECURITY",
            "dev",
            "wifi",
            "list",
            "--rescan",
            "yes",
        ]
        try:
            code, stdout, _ = self._run(fresh, SCAN_TIMEOUT_S)
            if code == 0 and stdout.strip():
                return parse_nmcli_wifi_list(stdout)
        except WifiCommandError:
            pass
        _, stdout, _ = self._run(
            ["nmcli", "-t", "-f", "SSID,SIGNAL,SECURITY", "dev", "wifi"],
            SCAN_TIMEOUT_S,
        )
        return parse_nmcli_wifi_list(stdout)

    def connect(self, ssid: str, password: str) -> str:
        """Devuelve `ok` | `auth` | `timeout` | `error`. Nunca levanta.

        No usa `nmcli dev wifi connect`: si el perfil ya existe, ese comando
        actualiza la clave sin `wifi-sec.key-mgmt` y NetworkManager lo rechaza
        antes de probar la contraseña.
        """
        try:
            return self._connect(ssid, password)
        except WifiCommandTimeout:
            return "timeout"
        except WifiCommandError:
            return "error"

    def _wifi_connection_uuid(self, ssid: str) -> str | None:
        code, stdout, _ = self._run(
            ["nmcli", "-t", "-f", "NAME,UUID,TYPE", "connection", "show"],
            SCAN_TIMEOUT_S,
        )
        if code != 0:
            return None
        for _name, uuid, kind in parse_nmcli_connection_rows(stdout):
            if kind != "802-11-wireless" or not uuid:
                continue
            ssid_code, ssid_out, _ = self._run(
                ["nmcli", "-g", "802-11-wireless.ssid", "connection", "show", uuid],
                SCAN_TIMEOUT_S,
            )
            if ssid_code == 0 and ssid_out.strip() == ssid:
                return uuid
        return None

    def _connect(self, ssid: str, password: str) -> str:
        existing = self._wifi_connection_uuid(ssid)
        if existing:
            if password:
                code, _, stderr = self._run(
                    [
                        "nmcli",
                        "connection",
                        "modify",
                        existing,
                        "wifi-sec.key-mgmt",
                        "wpa-psk",
                        "wifi-sec.psk",
                        password,
                    ],
                    CONNECT_TIMEOUT_S,
                )
                if code != 0:
                    return classify_nmcli_connect(code, stderr, 0.0)
            up = ["nmcli", "-w", str(int(CONNECT_WAIT_S)), "connection", "up", "uuid", existing]
        else:
            argv = [
                "nmcli",
                "connection",
                "add",
                "type",
                "wifi",
                "con-name",
                ssid,
                "ssid",
                ssid,
            ]
            if password:
                argv.extend(
                    ["wifi-sec.key-mgmt", "wpa-psk", "wifi-sec.psk", password]
                )
            code, stdout, stderr = self._run(argv, CONNECT_TIMEOUT_S)
            if code != 0:
                return classify_nmcli_connect(code, stderr, 0.0)
            added = parse_nmcli_added_uuid(stdout)
            if added:
                up = ["nmcli", "-w", str(int(CONNECT_WAIT_S)), "connection", "up", "uuid", added]
            else:
                up = ["nmcli", "-w", str(int(CONNECT_WAIT_S)), "connection", "up", "id", ssid]
        start = time.monotonic()
        code, _, stderr = self._run(up, CONNECT_TIMEOUT_S)
        return classify_nmcli_connect(code, stderr, time.monotonic() - start)

    def active_wifi_uuid(self) -> str | None:
        """UUID de la WiFi en uso. None si no hay."""
        try:
            code, stdout, _ = self._run(
                ["nmcli", "-t", "-f", "NAME,UUID,TYPE", "connection", "show", "--active"],
                SCAN_TIMEOUT_S,
            )
        except WifiCommandError:
            return None
        if code != 0:
            return None
        for _name, uuid, kind in parse_nmcli_connection_rows(stdout):
            if kind == "802-11-wireless" and uuid:
                return uuid
        return None

    def _abandon(self, ssid: str) -> None:
        """Saca la red que falló para que no se quede reintentando."""
        uuid = self._wifi_connection_uuid(ssid)
        if not uuid:
            return
        for argv in (
            ["nmcli", "connection", "modify", uuid, "connection.autoconnect", "no"],
            ["nmcli", "connection", "down", "uuid", uuid],
        ):
            try:
                self._run(argv, SCAN_TIMEOUT_S)
            except WifiCommandError:
                pass

    def _restore(self, uuid: str) -> None:
        """Vuelve a la red que estaba antes. No levanta si nmcli falla."""
        try:
            self._run(
                ["nmcli", "-w", str(int(CONNECT_WAIT_S)), "connection", "up", "uuid", uuid],
                CONNECT_TIMEOUT_S,
            )
        except WifiCommandError:
            pass

    def active_connection_name(self) -> str | None:
        """Nombre NetworkManager de la WiFi en uso. None si no hay."""
        try:
            code, stdout, _ = self._run(
                ["nmcli", "-t", "-f", "NAME,TYPE", "connection", "show", "--active"],
                SCAN_TIMEOUT_S,
            )
        except WifiCommandError:
            return None
        if code != 0:
            return None
        return parse_nmcli_active_wifi_name(stdout)

    def forget(self, name: str) -> None:
        """Borra el perfil guardado para que no vuelva a autoconectarse."""
        code, _, _ = self._run(
            ["nmcli", "connection", "delete", "id", name],
            SCAN_TIMEOUT_S,
        )
        if code != 0:
            raise WifiCommandError("nmcli: delete")

    def switch_to(self, ssid: str, password: str) -> str:
        """Conecta a `ssid` y, si salió bien, olvida la red anterior.

        Si la clave falla, el perfil viejo sigue guardado.
        """
        old_uuid = self.active_wifi_uuid()
        old = self.active_connection_name()
        result = self.connect(ssid, password)
        if result != "ok":
            self._abandon(ssid)
            if old_uuid:
                self._restore(old_uuid)
            return result
        current = self.active_connection_name()
        if old and current and old != current:
            try:
                self.forget(old)
            except WifiCommandError:
                pass
        return "ok"
