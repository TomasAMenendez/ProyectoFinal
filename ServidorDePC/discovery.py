"""Beacon UDP del ServidorDePC. No conoce FastAPI."""
from __future__ import annotations

import json
import socket
import threading
import time
from collections.abc import Callable

DEVICE_ID = "teo_pc_server"
DEVICE_NAME = "TEO ServidorDePC"
BEACON_PORT = 5556
BEACON_INTERVAL_S = 1.5

SocketFactory = Callable[[], socket.socket]


def beacon_payload(api_port: int, token: str) -> dict[str, object]:
    return {
        "device_id": DEVICE_ID,
        "device_name": DEVICE_NAME,
        "api_port": int(api_port),
        "token": token,
    }


def beacon_bytes(api_port: int, token: str) -> bytes:
    return json.dumps(beacon_payload(api_port, token), ensure_ascii=False).encode("utf-8")


def beacon_log(port: int, token: str) -> str:
    shown = (token or "")[:8]
    return f"Beacon UDP :{port} token {shown}…"


def open_broadcast_socket() -> socket.socket:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_BROADCAST, 1)
    return sock


def transmit(sock: socket.socket, payload: bytes, opener: SocketFactory, port: int = BEACON_PORT) -> socket.socket:
    """Envía el beacon. Si falla, cierra el socket y devuelve uno nuevo. No propaga."""
    try:
        sock.sendto(payload, ("<broadcast>", port))
        return sock
    except Exception:
        try:
            sock.close()
        except Exception:
            pass
        return opener()


class DiscoveryBeacon:
    def __init__(
        self,
        api_port: int,
        token: str,
        port: int = BEACON_PORT,
        interval_s: float = BEACON_INTERVAL_S,
        opener: SocketFactory | None = None,
    ) -> None:
        self.api_port = int(api_port)
        self.token = token
        self.port = int(port)
        self.interval_s = float(interval_s)
        self._opener = opener if opener is not None else open_broadcast_socket
        self._running = False
        self._thread: threading.Thread | None = None

    def start(self) -> None:
        if self._running:
            return
        self._running = True
        self._thread = threading.Thread(target=self._loop, daemon=True, name="PcBeacon")
        self._thread.start()

    def stop(self) -> None:
        self._running = False
        thread = self._thread
        if thread is not None:
            thread.join(timeout=2)
            self._thread = None

    def _loop(self) -> None:
        try:
            sock = self._opener()
        except Exception:
            sock = None
        while self._running:
            payload = beacon_bytes(self.api_port, self.token)
            if sock is None:
                try:
                    sock = self._opener()
                except Exception:
                    sock = None
                    time.sleep(self.interval_s)
                    continue
            sock = transmit(sock, payload, self._opener, self.port)
            time.sleep(self.interval_s)
        if sock is not None:
            try:
                sock.close()
            except Exception:
                pass
