"""IP de LAN publicable en el beacon. Nunca 0.0.0.0 ni loopback."""
from __future__ import annotations

import socket
import subprocess
from collections.abc import Callable, Iterable
from typing import Any

UNUSABLE_IPS = frozenset({"", "0.0.0.0", "::", "::0"})


def usable_lan_ip(candidate: str | None) -> str | None:
    """None si la dirección no sirve para que el celular abra HTTP."""
    ip = (candidate or "").strip()
    if ip in UNUSABLE_IPS or ip.startswith("127."):
        return None
    return ip


def pick_lan_ip(routed: str | None, interface_ips: Iterable[str]) -> str | None:
    """Prefiere la IP de la ruta por defecto; si no hay, la primera de interfaz."""
    picked = usable_lan_ip(routed)
    if picked:
        return picked
    for ip in interface_ips:
        picked = usable_lan_ip(ip)
        if picked:
            return picked
    return None


def parse_ip_addr_show(text: str) -> list[str]:
    """IPv4 globales de `ip -4 -o addr show scope global`."""
    found: list[str] = []
    for line in text.splitlines():
        parts = line.split()
        try:
            inet_at = parts.index("inet")
            cidr = parts[inet_at + 1]
        except (ValueError, IndexError):
            continue
        ip = cidr.split("/", 1)[0]
        usable = usable_lan_ip(ip)
        if usable:
            found.append(usable)
    return found


def read_interface_ips(
    runner: Callable[..., subprocess.CompletedProcess[str]] = subprocess.run,
) -> list[str]:
    try:
        result = runner(
            ["ip", "-4", "-o", "addr", "show", "scope", "global"],
            capture_output=True,
            text=True,
            timeout=2,
        )
    except Exception:
        return []
    return parse_ip_addr_show(result.stdout or "")


def routed_source_ip() -> str | None:
    """IP que el kernel elige hacia 8.8.8.8. Falla sin ruta por defecto."""
    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.settimeout(0.5)
        sock.connect(("8.8.8.8", 80))
        ip = sock.getsockname()[0]
        sock.close()
    except Exception:
        return None
    return usable_lan_ip(ip)


def current_lan_ip() -> str | None:
    return pick_lan_ip(routed_source_ip(), read_interface_ips())


def beacon_fields(
    api_port: int,
    pairing_token: str,
    local_ip: str | None,
) -> dict[str, Any]:
    data: dict[str, Any] = {
        "device_id": "micompanero_robot",
        "device_name": "MiCompañero Peluche",
        "api_port": api_port,
        "pairing_token": pairing_token,
    }
    if local_ip:
        data["local_ip"] = local_ip
    return data
