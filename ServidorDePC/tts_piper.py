"""Piper local → WAV. Solo ServidorDePC; la Pi sigue con Daniela."""
from __future__ import annotations

import json
from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path
from types import SimpleNamespace
from typing import Any

import numpy as np

from tts_wav import float_to_wav_bytes

VOICES_DIR = Path(__file__).resolve().parent / "VocesParaPiper"


def _scales_from_config(path: Path) -> tuple[float, float, float]:
    """Escalas de inference del JSON de la voz."""
    data = json.loads(path.read_text(encoding="utf-8"))
    inference = data["inference"]
    return (
        float(inference["length_scale"]),
        float(inference["noise_scale"]),
        float(inference["noise_w"]),
    )


@dataclass(frozen=True)
class PiperVoiceProfile:
    name: str
    model_file: Path
    config_file: Path
    length_scale: float
    noise_scale: float
    noise_w_scale: float


def _micaela_profile() -> PiperVoiceProfile:
    config = VOICES_DIR / "Micaela.json"
    length_scale, noise_scale, noise_w_scale = _scales_from_config(config)
    return PiperVoiceProfile(
        name="Micaela",
        model_file=VOICES_DIR / "Micaela.onnx",
        config_file=config,
        length_scale=length_scale,
        noise_scale=noise_scale,
        noise_w_scale=noise_w_scale,
    )


# Micaela queda con las escalas de su JSON (1.0 / 0.667 / 0.8).
# Para volver a usarla: ACTIVE_VOICE = MICAELA
MICAELA = _micaela_profile()
TOPA = PiperVoiceProfile(
    name="Topa-e1500",
    model_file=VOICES_DIR / "Topa-e1500.onnx",
    config_file=VOICES_DIR / "Topa-e1500.onnx.json",
    length_scale=1.45,
    noise_scale=1.00,
    noise_w_scale=0.50,
)
ACTIVE_VOICE = TOPA
VOICE_BY_ID = {"topa": TOPA, "micaela": MICAELA}


def profile_for(voice: str | None) -> PiperVoiceProfile:
    """topa y micaela son válidas. Cualquier otro pedido usa Topa."""
    key = str(voice or "").strip().lower()
    return VOICE_BY_ID.get(key, TOPA)


class PiperEngine:
    MODEL_NAME = ACTIVE_VOICE.name
    MODEL_FILE = ACTIVE_VOICE.model_file
    CONFIG_FILE = ACTIVE_VOICE.config_file
    LENGTH_SCALE = ACTIVE_VOICE.length_scale
    NOISE_SCALE = ACTIVE_VOICE.noise_scale
    NOISE_W_SCALE = ACTIVE_VOICE.noise_w_scale

    def __init__(
        self,
        voice: Any = None,
        voices: dict[str, Any] | None = None,
        voice_loader: Callable[[str], Any] | None = None,
        ensure_model: Callable[[], Path] | None = None,
    ) -> None:
        self._voices: dict[str, Any] = dict(voices or {})
        if voice is not None and not self._voices:
            self._voices = {"topa": voice, "micaela": voice}
        self._voice_loader = voice_loader
        self._ensure_model_fn = ensure_model
        self.ready = False

    def load(self) -> None:
        try:
            if not self._voices:
                self._load_profiles()
            self.ready = bool(self._voices)
            if self.ready:
                loaded = ", ".join(sorted(self._voices))
                print(f"[TTS] Piper listo ({loaded})", flush=True)
        except Exception as exc:
            print(f"[TTS] Piper load falló: {exc}", flush=True)
            self._voices = {}
            self.ready = False

    def synthesize(self, text: str, voice: str | None = None) -> bytes:
        profile = profile_for(voice)
        key = "micaela" if profile is MICAELA else "topa"
        speaker = self._voices.get(key)
        if speaker is None or not (text or "").strip():
            return b""
        chunks: list[np.ndarray] = []
        sample_rate = 22050
        syn_config = _make_syn_config(
            profile.length_scale, profile.noise_scale, profile.noise_w_scale
        )
        try:
            for chunk in speaker.synthesize(text, syn_config=syn_config):
                chunks.append(np.asarray(chunk.audio_float_array, dtype=np.float32))
                sample_rate = int(chunk.sample_rate)
        except Exception as exc:
            print(f"[TTS] Piper synthesize falló: {exc}", flush=True)
            return b""
        if not chunks:
            return b""
        audio = np.concatenate(chunks).astype(np.float32)
        return float_to_wav_bytes(audio, sample_rate)

    def _load_profiles(self) -> None:
        loader = self._voice_loader
        for key, profile in VOICE_BY_ID.items():
            try:
                if self._ensure_model_fn is not None:
                    path = self._ensure_model_fn()
                else:
                    path = _require_profile(profile)
                if loader is not None:
                    self._voices[key] = loader(str(path))
                else:
                    self._voices[key] = _load_piper_voice(str(path), profile.config_file)
            except Exception as exc:
                print(f"[TTS] Piper no cargó {key}: {exc}", flush=True)

    def _ensure_model(self) -> Path:
        return _require_profile(TOPA)


def _require_profile(profile: PiperVoiceProfile) -> Path:
    if profile.model_file.is_file() and profile.config_file.is_file():
        return profile.model_file
    raise FileNotFoundError(
        f"Falta la voz {profile.name} en {VOICES_DIR} "
        f"(esperado {profile.model_file.name} y {profile.config_file.name})"
    )


def _load_piper_voice(model_path: str, config_path: Path | None = None) -> Any:
    try:
        from piper.voice import PiperVoice
    except ImportError:
        from piper import PiperVoice  # type: ignore[attr-defined]
    if config_path is None:
        return PiperVoice.load(model_path)
    try:
        return PiperVoice.load(model_path, config_path=str(config_path))
    except TypeError:
        return PiperVoice.load(model_path, str(config_path))


def _make_syn_config(length_scale: float, noise_scale: float, noise_w_scale: float) -> Any:
    try:
        from piper.config import SynthesisConfig

        return SynthesisConfig(
            length_scale=length_scale,
            noise_scale=noise_scale,
            noise_w_scale=noise_w_scale,
        )
    except Exception:
        return SimpleNamespace(
            length_scale=length_scale,
            noise_scale=noise_scale,
            noise_w_scale=noise_w_scale,
        )
