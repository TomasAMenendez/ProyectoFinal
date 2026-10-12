"""Whisper large-v3 float16 en la RTX. Si la placa rechaza el cálculo, CPU int8."""
from __future__ import annotations

import io
import wave
from typing import Any

import numpy as np

from config import WHISPER_COMPUTE, WHISPER_DEVICE, WHISPER_INITIAL_PROMPT, WHISPER_MODEL


def is_gpu_runtime_failure(exc: BaseException) -> bool:
    text = str(exc).lower()
    return any(needle in text for needle in ("out of memory", "cublas", "not_supported"))


class SttEngine:
    def __init__(self) -> None:
        self._model: Any = None
        self._cpu: Any = None
        self.ready = False

    def load(self) -> None:
        from faster_whisper import WhisperModel

        print(
            f"[STT] Cargando {WHISPER_MODEL} device={WHISPER_DEVICE} compute={WHISPER_COMPUTE}",
            flush=True,
        )
        try:
            self._model = WhisperModel(
                WHISPER_MODEL,
                device=WHISPER_DEVICE,
                compute_type=WHISPER_COMPUTE,
            )
        except RuntimeError as exc:
            if not is_gpu_runtime_failure(exc):
                raise
            print(f"[STT] la RTX no cargó el modelo ({exc}). Uso CPU.", flush=True)
            self._model = self._cpu_model()
        self.ready = True
        print("[STT] listo", flush=True)

    def _cpu_model(self) -> Any:
        if self._cpu is None:
            from faster_whisper import WhisperModel

            print(f"[STT] cargo {WHISPER_MODEL} en CPU int8", flush=True)
            self._cpu = WhisperModel(WHISPER_MODEL, device="cpu", compute_type="int8")
        return self._cpu

    def transcribe_wav(self, wav: bytes, language: str = "es") -> dict[str, Any]:
        empty: dict[str, Any] = {
            "text": "",
            "avg_logprob": None,
            "no_speech_prob": None,
        }
        if self._model is None:
            return empty
        audio = _wav_to_float32(wav)
        if audio.size == 0:
            return empty
        try:
            return self._collect(self._model, audio, language)
        except RuntimeError as exc:
            if self._model is self._cpu or not is_gpu_runtime_failure(exc):
                raise
            print(f"[STT] la RTX no transcribió ({exc}). Sigo en CPU.", flush=True)
            return self._collect(self._cpu_model(), audio, language)

    def _collect(self, model: Any, audio: np.ndarray, language: str) -> dict[str, Any]:
        segments, _info = model.transcribe(
            audio,
            language=language or "es",
            vad_filter=False,
            beam_size=1,
            initial_prompt=WHISPER_INITIAL_PROMPT,
            no_speech_threshold=0.6,
            compression_ratio_threshold=2.4,
            condition_on_previous_text=False,
            without_timestamps=True,
        )
        parts: list[str] = []
        logprobs: list[float] = []
        no_speech: list[float] = []
        for seg in segments:
            parts.append(str(getattr(seg, "text", "") or "").strip())
            avg_lp = getattr(seg, "avg_logprob", None)
            if avg_lp is not None:
                logprobs.append(float(avg_lp))
            nsp = getattr(seg, "no_speech_prob", None)
            if nsp is not None:
                no_speech.append(float(nsp))
        text = " ".join(p for p in parts if p).strip()
        return {
            "text": text,
            "avg_logprob": float(np.mean(logprobs)) if logprobs else None,
            "no_speech_prob": float(np.mean(no_speech)) if no_speech else None,
        }


def _wav_to_float32(wav: bytes) -> np.ndarray:
    try:
        with wave.open(io.BytesIO(wav), "rb") as wf:
            sr = wf.getframerate()
            nch = wf.getnchannels()
            sw = wf.getsampwidth()
            raw = wf.readframes(wf.getnframes())
    except Exception:
        return np.zeros(0, dtype=np.float32)
    if sw != 2 or not raw:
        return np.zeros(0, dtype=np.float32)
    pcm = np.frombuffer(raw, dtype="<i2").astype(np.float32) / 32768.0
    if nch > 1:
        pcm = pcm.reshape(-1, nch).mean(axis=1)
    if sr != 16000 and sr > 0:
        # Downsample entero simple (48k→16k = cada 3).
        step = max(1, int(round(sr / 16000.0)))
        pcm = pcm[::step]
    return pcm.astype(np.float32)
