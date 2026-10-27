"""Cartesia TTS. Un solo intento corto; si falla, el motor local sigue."""
from __future__ import annotations

import json
import os
import urllib.request
from pathlib import Path
from typing import Callable

API_URL = "https://api.cartesia.ai/tts/bytes"
API_VERSION = "2026-08-14"
TIMEOUT_S = 4.0
# Nombres reales de GET /voices: "Topa" (argentinian) y "Micaela " sin género
# femenino (argentinian). La otra Micaela publica lleva gender=feminine.
VOICE_IDS = {
    "topa": "be5dc977-8989-4292-a267-110fe23e7f93",
    "micaela": "2ab6bb3b-570f-4103-9bb3-5c6f4ab027be",
}

Opener = Callable[..., object]


def load_api_key() -> str:
    env = os.environ.get("API_KEY_CARTESIA", "").strip()
    if env:
        return env
    root = Path(__file__).resolve().parent
    for path in (root / ".env", root / "cartesia_api_key.txt"):
        if not path.is_file():
            continue
        for line in path.read_text(encoding="utf-8").splitlines():
            text = line.strip()
            if not text or text.startswith("#"):
                continue
            if text.startswith("API_KEY_CARTESIA="):
                return text.split("=", 1)[1].strip()
            if path.name == "cartesia_api_key.txt":
                return text
    return ""


def synthesize_wav(
    text: str,
    voice: str | None = None,
    *,
    timeout_s: float = TIMEOUT_S,
    opener: Opener | None = None,
    api_key: str | None = None,
) -> bytes:
    spoken = (text or "").strip()
    if not spoken:
        return b""
    key = (api_key if api_key is not None else load_api_key()).strip()
    if not key:
        return b""
    voice_id = VOICE_IDS.get(str(voice or "").strip().lower(), VOICE_IDS["topa"])
    payload = json.dumps(
        {
            "model_id": "sonic-3.6",
            "transcript": spoken,
            "voice": {"id": voice_id},
            "language": "es",
            "output_format": {
                "container": "wav",
                "encoding": "pcm_s16le",
                "sample_rate": 22050,
            },
        }
    ).encode("utf-8")
    request = urllib.request.Request(
        API_URL,
        data=payload,
        headers={
            "Authorization": f"Bearer {key}",
            "Cartesia-Version": API_VERSION,
            "Content-Type": "application/json",
        },
        method="POST",
    )
    fetch = opener or urllib.request.urlopen
    try:
        with fetch(request, timeout=timeout_s) as response:
            status = getattr(response, "status", 200)
            body = response.read()
    except Exception as exc:
        print(f"[TTS] Cartesia no respondió: {exc}", flush=True)
        return b""
    if status != 200 or not isinstance(body, (bytes, bytearray)):
        print(f"[TTS] Cartesia HTTP {status}", flush=True)
        return b""
    wav = bytes(body)
    if len(wav) < 44 or not wav.startswith(b"RIFF"):
        print("[TTS] Cartesia devolvió audio vacío", flush=True)
        return b""
    return wav
