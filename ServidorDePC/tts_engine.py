"""Piper + Elena → WAV 16-bit. ready = Piper cargó."""
from __future__ import annotations

import time
from collections.abc import Callable
from typing import Any

from config import prefer_elena
from tts_elena import synthesize_wav as elena_synthesize_wav
from tts_piper import PiperEngine


class TtsEngine:
    def __init__(
        self,
        piper: Any = None,
        elena_fn: Callable[[str], bytes] | None = None,
        prefer_fn: Callable[[], bool] | None = None,
        cartesia_fn: Callable[..., bytes] | None = None,
    ) -> None:
        self._piper = piper if piper is not None else PiperEngine()
        self._elena_fn = elena_fn if elena_fn is not None else elena_synthesize_wav
        self._prefer_fn = prefer_fn if prefer_fn is not None else prefer_elena
        self._cartesia_fn = cartesia_fn if cartesia_fn is not None else (lambda *_a, **_k: b"")
        self.ready = False

    def load(self) -> None:
        self._piper.load()
        self.ready = bool(getattr(self._piper, "ready", False))

    def synthesize(
        self,
        text: str,
        voice: str | None = None,
        local_only: bool = False,
    ) -> bytes:
        if not (text or "").strip():
            return b""
        if not getattr(self._piper, "ready", False):
            return b""
        chosen = str(voice or "topa")
        if not local_only:
            remote = self._try_cartesia(text, voice)
            if remote:
                return remote
        started = time.monotonic()
        try:
            wav = self._piper.synthesize(text, voice=voice) or b""
        except TypeError:
            wav = self._piper.synthesize(text) or b""
        except Exception as exc:
            print(f"[TTS] Piper synthesize falló: {exc}", flush=True)
            return b""
        ms = (time.monotonic() - started) * 1000.0
        print(f"[TTS] motor=piper voice={chosen} ms={ms:.0f}", flush=True)
        return wav

    def _try_cartesia(self, text: str, voice: str | None) -> bytes:
        started = time.monotonic()
        try:
            wav = self._cartesia_fn(text, voice) or b""
        except TypeError:
            try:
                wav = self._cartesia_fn(text) or b""
            except Exception as exc:
                print(f"[TTS] Cartesia falló: {exc}", flush=True)
                return b""
        except Exception as exc:
            print(f"[TTS] Cartesia falló: {exc}", flush=True)
            return b""
        if not isinstance(wav, (bytes, bytearray)) or not bytes(wav).startswith(b"RIFF"):
            return b""
        ms = (time.monotonic() - started) * 1000.0
        chosen = str(voice or "topa")
        print(f"[TTS] motor=cartesia voice={chosen} ms={ms:.0f}", flush=True)
        return bytes(wav)
