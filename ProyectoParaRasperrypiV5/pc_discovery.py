"""La Pi encuentra ServidorDePC en la LAN. El turno de voz no llama esto."""
from __future__ import annotations

import http.client
import json
import os
import socket
import threading
import time
from collections.abc import Callable, Mapping
from concurrent.futures import ThreadPoolExecutor
from typing import Any

DEVICE_ID = "teo_pc_server"
BEACON_PORT = 5556
LISTEN_S = 3.0
SCAN_WORKERS = 16
SCAN_TIMEOUT_S = 0.3
SCAN_PORT = 8090
RETRY_S = 15.0

ListenFn = Callable[[], tuple[str, str] | None]
ScanFn = Callable[[], tuple[str, str] | None]
ProbeFn = Callable[[Any], str]
SleepFn = Callable[[float], None]
LogFn = Callable[[str], None]

_REFUSED = {111, 10061}


def usable_source_ip(ip: str | None) -> str | None:
    text = (ip or "").strip()
    if not text or text in {"0.0.0.0", "255.255.255.255"}:
        return None
    if ":" in text or text.startswith("127."):
        return None
    return text


def parse_announce(payload: bytes | str) -> dict[str, Any] | None:
    try:
        data = json.loads(payload)
    except (TypeError, ValueError, json.JSONDecodeError):
        return None
    if not isinstance(data, dict) or data.get("device_id") != DEVICE_ID:
        return None
    token = str(data.get("token") or "").strip()
    if not token:
        return None
    try:
        port = int(data["api_port"])
    except (KeyError, TypeError, ValueError):
        return None
    if not 1 <= port <= 65535:
        return None
    return {"token": token, "api_port": port}


def announce_from_packet(payload: bytes, source_ip: str) -> tuple[str, str] | None:
    ip = usable_source_ip(source_ip)
    parsed = parse_announce(payload)
    if ip is None or parsed is None:
        return None
    return (f"http://{ip}:{parsed['api_port']}", parsed["token"])


def listen_for_pc(
    sock: socket.socket,
    window_s: float = LISTEN_S,
    now: Callable[[], float] = time.monotonic,
) -> tuple[str, str] | None:
    deadline = now() + window_s
    while True:
        remaining = deadline - now()
        if remaining <= 0:
            return None
        sock.settimeout(remaining)
        try:
            data, addr = sock.recvfrom(2048)
        except (TimeoutError, socket.timeout):
            return None
        source = addr[0] if isinstance(addr, tuple) and addr else ""
        found = announce_from_packet(data, source)
        if found is not None:
            return found


def open_listen_socket(port: int = BEACON_PORT) -> socket.socket:
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind(("", port))
        return sock
    except Exception:
        sock.close()
        raise


def default_listen(port: int = BEACON_PORT, window_s: float = LISTEN_S) -> tuple[str, str] | None:
    sock = open_listen_socket(port)
    try:
        return listen_for_pc(sock, window_s=window_s)
    finally:
        sock.close()


def scan_subnet(
    prefix: str,
    own_ip: str,
    fetch: Callable[[str], tuple[int, bytes]],
    workers: int = SCAN_WORKERS,
) -> tuple[str, str] | None:
    found: dict[str, str] = {}
    lock = threading.Lock()

    def work(host: int) -> None:
        ip = f"{prefix}.{host}"
        if ip == own_ip:
            return
        with lock:
            if found:
                return
        try:
            status, body = fetch(ip)
        except Exception:
            return
        if status != 200:
            return
        parsed = parse_announce(body)
        if parsed is None:
            return
        with lock:
            if not found:
                found["url"] = f"http://{ip}:{parsed['api_port']}"
                found["token"] = parsed["token"]

    with ThreadPoolExecutor(max_workers=workers) as pool:
        list(pool.map(work, range(1, 255)))
    if not found:
        return None
    return found["url"], found["token"]


def fetch_discover(ip: str, port: int = SCAN_PORT, timeout_s: float = SCAN_TIMEOUT_S) -> tuple[int, bytes]:
    conn = http.client.HTTPConnection(ip, port, timeout=timeout_s)
    try:
        conn.request("GET", "/discover")
        resp = conn.getresponse()
        return int(resp.status), resp.read()
    finally:
        conn.close()


def subnet_prefix(ip: str) -> str | None:
    parts = ip.split(".")
    if len(parts) != 4:
        return None
    return ".".join(parts[:3])


def default_scan(lan_ip_fn: Callable[[], str | None] | None = None) -> tuple[str, str] | None:
    if lan_ip_fn is None:
        from lan_ip import current_lan_ip

        lan_ip_fn = current_lan_ip
    own = usable_source_ip(lan_ip_fn())
    if own is None:
        return None
    prefix = subnet_prefix(own)
    if prefix is None:
        return None
    return scan_subnet(prefix, own, fetch_discover)


def classify_probe(status: int | None, exc: BaseException | None) -> str:
    if isinstance(exc, (TimeoutError, socket.timeout)):
        return "timeout"
    if isinstance(exc, ConnectionRefusedError):
        return "refused"
    if isinstance(exc, OSError):
        if getattr(exc, "errno", None) in _REFUSED:
            return "refused"
        return "unreachable"
    if status == 200:
        return "ok"
    if status == 503:
        return "loading"
    if status == 401:
        return "unauthorized"
    return "unreachable"


def probe_client(client: Any) -> str:
    try:
        status = client.raw_health()
    except Exception as exc:
        return classify_probe(None, exc)
    return classify_probe(int(status), None)


def should_start(env: Mapping[str, str]) -> bool:
    url = str(env.get("PC_SERVER_URL") or "").strip()
    token = str(env.get("PC_SERVER_TOKEN") or "").strip()
    return not url and not token


def _log_line(msg: str) -> None:
    print(f"[PC-DISCOVERY] {msg}", flush=True)


class PcDiscovery:
    def __init__(
        self,
        client: Any,
        listen_fn: ListenFn | None = None,
        scan_fn: ScanFn | None = None,
        probe_fn: ProbeFn | None = None,
        sleep_fn: SleepFn | None = None,
        log_fn: LogFn | None = None,
        retry_s: float = RETRY_S,
    ) -> None:
        self.client = client
        self.listen_fn = listen_fn if listen_fn is not None else default_listen
        self.scan_fn = scan_fn if scan_fn is not None else default_scan
        self.probe_fn = probe_fn if probe_fn is not None else probe_client
        self.sleep_fn = sleep_fn if sleep_fn is not None else time.sleep
        self.log_fn = log_fn if log_fn is not None else _log_line
        self.retry_s = retry_s
        self.timeouts = 0
        self.force_search = False

    def adopt(self, url: str, token: str) -> None:
        clean_url = url.strip().rstrip("/")
        clean_token = token.strip()
        with self.client._http_lock:
            self.client.base_url = clean_url
            self.client.token = clean_token
            self.client.fail_count = 0
            self.client.open_until = 0.0
            self.client.fail_count_tts = 0
            self.client.open_until_tts = 0.0
            self.timeouts = 0
            self._write_cache(clean_url, clean_token)
        shown = clean_token[:8]
        self.log_fn(f"{clean_url} token {shown}…")

    def _write_cache(self, url: str, token: str) -> None:
        cache = getattr(self.client, "cache_path", None)
        token_path = getattr(self.client, "token_path", None)
        if cache is not None:
            try:
                cache.write_text(url + "\n", encoding="utf-8")
            except OSError:
                pass
        if token_path is not None:
            try:
                token_path.write_text(token + "\n", encoding="utf-8")
            except OSError:
                pass

    def search_once(self) -> bool:
        found: tuple[str, str] | None = None
        try:
            found = self.listen_fn()
        except OSError as exc:
            self.log_fn(f"UDP no abre: {exc}")
            found = None
        if not found:
            try:
                found = self.scan_fn()
            except Exception as exc:
                self.log_fn(f"barrido falló: {exc}")
                found = None
        if not found:
            return False
        self.adopt(found[0], found[1])
        return True

    def tick(self) -> str:
        if self.client.base_url and self.client.token and not self.force_search:
            kind = self.probe_fn(self.client)
            if kind in ("ok", "loading"):
                self.timeouts = 0
                self.sleep_fn(self.retry_s)
                return "keep"
            if kind == "timeout":
                self.timeouts += 1
                if self.timeouts < 2:
                    self.sleep_fn(self.retry_s)
                    return "wait-timeout"
        hit = self.search_once()
        self.force_search = not hit
        self.sleep_fn(self.retry_s)
        return "search-hit" if hit else "search-miss"


def guarded_tick(discovery: PcDiscovery) -> None:
    try:
        discovery.tick()
    except Exception as exc:
        discovery.log_fn(f"error: {exc}")
        discovery.sleep_fn(discovery.retry_s)


def start_discovery(client: Any, env: Mapping[str, str] | None = None) -> threading.Thread | None:
    if client is None:
        return None
    source = os.environ if env is None else env
    if not should_start(source):
        return None
    discovery = PcDiscovery(client)

    def _loop() -> None:
        while True:
            guarded_tick(discovery)

    thread = threading.Thread(target=_loop, daemon=True, name="PcDiscovery")
    thread.start()
    return thread


def client_with_discovery() -> Any:
    from pc_server_client import PcServerClient

    client = PcServerClient.from_env()
    try:
        start_discovery(client)
    except Exception as exc:
        _log_line(f"no arranca: {exc}")
    return client
