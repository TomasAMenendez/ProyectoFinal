"""Provisionamiento WiFi vía nmcli (sin BLE ni radio en tests)."""
from __future__ import annotations

import subprocess
import time
from dataclasses import dataclass
from typing import Callable

Runner = Callable[[list[str], float], tuple[int, str, str]]

SCAN_TIMEOUT_S = 15.0
CONNECT_TIMEOUT_S = 30.0


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


def classify_nmcli_connect(
    returncode: int,
    stderr: str,
    elapsed_s: float,
    timeout_s: float = CONNECT_TIMEOUT_S,
) -> str:
    """Clasifica el resultado de `nmcli dev wifi connect`."""
    if elapsed_s >= timeout_s:
        return "timeout"
    if returncode == 0:
        return "ok"
    if "Secrets were required" in stderr or "802-11-wireless-security" in stderr:
        return "auth"
    return "auth"


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
        try:
            self._run(["nmcli", "dev", "wifi", "rescan"], SCAN_TIMEOUT_S)
        except WifiCommandError:
            pass  # el rescan es best-effort: igual listamos el cache de nmcli
        _, stdout, _ = self._run(
            ["nmcli", "-t", "-f", "SSID,SIGNAL,SECURITY", "dev", "wifi"],
            SCAN_TIMEOUT_S,
        )
        return parse_nmcli_wifi_list(stdout)

    def connect(self, ssid: str, password: str) -> str:
        """Devuelve `ok` | `auth` | `timeout` | `error`. Nunca levanta."""
        argv = ["nmcli", "dev", "wifi", "connect", ssid]
        if password:
            argv.extend(["password", password])
        start = time.monotonic()
        try:
            returncode, _, stderr = self._run(argv, CONNECT_TIMEOUT_S)
        except WifiCommandTimeout:
            return "timeout"
        except WifiCommandError:
            return "error"
        elapsed = time.monotonic() - start
        return classify_nmcli_connect(returncode, stderr, elapsed)
