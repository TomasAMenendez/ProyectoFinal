"""Rutas HTTP puras: se testean sin FastAPI ni GPU."""
from __future__ import annotations

import base64
import io
import json
import time
import wave
from typing import Any, Callable

LogFn = Callable[[str], None]


def _emit(log_fn: LogFn | None, msg: str) -> None:
    if log_fn is None:
        return
    log_fn(msg)


def _clip(text: str, limit: int | None = None) -> str:
    """Deja el mensaje entero. Solo aplana saltos de línea para que el log sea una línea."""
    del limit
    return (text or "").replace("\n", " ").strip()


def _opt_float(value: Any) -> float | None:
    if value is None:
        return None
    try:
        return float(value)
    except (TypeError, ValueError):
        return None


def _wav_meta(wav: bytes) -> str:
    n = len(wav or b"")
    extra = ""
    try:
        with wave.open(io.BytesIO(wav or b""), "rb") as wf:
            rate = wf.getframerate()
            frames = wf.getnframes()
        if rate > 0:
            extra = f", {frames / float(rate):.2f}s audio"
    except Exception:
        extra = ""
    return f"{n} bytes{extra}"


def _authorized(expected: str, header: str | None) -> bool:
    got = (header or "").strip()
    if not expected:
        return False
    prefix = "Bearer "
    if got.startswith(prefix):
        got = got[len(prefix):].strip()
    return got == expected


def handle_health(
    stt: Any,
    llm: Any,
    token: str,
    auth_header: str | None,
    tts: Any = None,
) -> tuple[int, dict[str, Any]]:
    if not _authorized(token, auth_header):
        return 401, {"error": "unauthorized"}
    stt_ok = bool(getattr(stt, "ready", False))
    llm_ok = bool(getattr(llm, "ready", False))
    tts_ok = bool(getattr(tts, "ready", False))
    body = {
        "status": "ok" if (stt_ok and llm_ok) else "loading",
        "stt": stt_ok,
        "llm": llm_ok,
        "tts": tts_ok,
    }
    if stt_ok and llm_ok:
        return 200, body
    return 503, body


def handle_transcribe(
    stt: Any,
    llm: Any,
    token: str,
    auth_header: str | None,
    wav: bytes,
    content_type: str | None,
    language: str,
    log_fn: LogFn | None = None,
    peer: str | None = None,
) -> tuple[int, dict[str, Any]]:
    del llm, content_type
    if not _authorized(token, auth_header):
        return 401, {"error": "unauthorized"}
    if not getattr(stt, "ready", False):
        return 503, {"error": "stt not ready"}
    who = peer or "?"
    _emit(log_fn, f"[STT] audio recibido de {who}: {_wav_meta(wav)}")
    t0 = time.perf_counter()
    try:
        raw_out = stt.transcribe_wav(wav, language=language or "es")
    except RuntimeError as exc:
        from stt_engine import is_gpu_runtime_failure

        if not is_gpu_runtime_failure(exc):
            raise
        ms = (time.perf_counter() - t0) * 1000.0
        _emit(log_fn, f"[STT] la GPU no transcribió {ms:.0f} ms")
        return 503, {"error": "stt gpu busy"}
    ms = (time.perf_counter() - t0) * 1000.0
    avg_logprob: float | None = None
    no_speech_prob: float | None = None
    if isinstance(raw_out, dict):
        shown = str(raw_out.get("text") or "")
        avg_logprob = _opt_float(raw_out.get("avg_logprob"))
        no_speech_prob = _opt_float(raw_out.get("no_speech_prob"))
    else:
        shown = str(raw_out or "")
    _emit(log_fn, f'[STT] transcripción {ms:.0f} ms: "{_clip(shown)}"')
    body: dict[str, Any] = {"text": shown}
    if avg_logprob is not None:
        body["avg_logprob"] = avg_logprob
    if no_speech_prob is not None:
        body["no_speech_prob"] = no_speech_prob
    return 200, body


def handle_chat(
    stt: Any,
    llm: Any,
    token: str,
    auth_header: str | None,
    raw_body: bytes,
    log_fn: LogFn | None = None,
    peer: str | None = None,
) -> tuple[int, dict[str, Any]]:
    del stt
    if not _authorized(token, auth_header):
        return 401, {"error": "unauthorized"}
    if not getattr(llm, "ready", False):
        return 503, {"error": "llm not ready"}
    try:
        payload = json.loads(raw_body.decode("utf-8") or "{}")
    except json.JSONDecodeError:
        return 400, {"error": "invalid json"}
    messages = payload.get("messages")
    if not isinstance(messages, list) or not messages:
        return 400, {"error": "messages required"}
    who = peer or "?"
    _emit(log_fn, f"[LLM] prompt recibido de {who}: {len(messages)} mensajes")
    t0 = time.perf_counter()
    content = llm.complete(messages)
    ms = (time.perf_counter() - t0) * 1000.0
    shown = str(content or "")
    _emit(log_fn, f'[LLM] respuesta {ms:.0f} ms: "{_clip(shown)}"')
    return 200, {
        "choices": [{"message": {"role": "assistant", "content": shown}}],
    }


def handle_speech(
    tts: Any,
    token: str,
    auth_header: str | None,
    raw_body: bytes,
    log_fn: LogFn | None = None,
    peer: str | None = None,
) -> tuple[int, bytes | dict[str, Any], str]:
    if not _authorized(token, auth_header):
        return 401, {"error": "unauthorized"}, "application/json"
    if not getattr(tts, "ready", False):
        return 503, {"error": "tts not ready"}, "application/json"
    try:
        from config import TTS_MAX_CHARS
    except Exception:
        TTS_MAX_CHARS = 500
    try:
        payload = json.loads((raw_body or b"").decode("utf-8") or "{}")
    except (json.JSONDecodeError, UnicodeDecodeError):
        return 400, {"error": "invalid json"}, "application/json"
    if not isinstance(payload, dict):
        return 400, {"error": "invalid json"}, "application/json"
    text = str(payload.get("input") or "").strip()
    if not text:
        return 400, {"error": "input required"}, "application/json"
    if len(text) > int(TTS_MAX_CHARS):
        return 400, {"error": "input too long"}, "application/json"
    voice = payload.get("voice")
    local_only = payload.get("local_only") is True
    who = peer or "?"
    _emit(
        log_fn,
        f'[TTS] texto de {who}: "{_clip(text)}" voice={voice or "topa"} local_only={local_only}',
    )
    t0 = time.perf_counter()
    try:
        wav = tts.synthesize(text, voice=voice, local_only=local_only)
    except TypeError:
        try:
            wav = tts.synthesize(text, voice=voice)
        except TypeError:
            wav = tts.synthesize(text)
    ms = (time.perf_counter() - t0) * 1000.0
    if not isinstance(wav, (bytes, bytearray)) or not wav:
        return 500, {"error": "tts failed"}, "application/json"
    _emit(log_fn, f"[TTS] wav {ms:.0f} ms: {_wav_meta(bytes(wav))}")
    return 200, bytes(wav), "audio/wav"


def _message_list(value: Any) -> list[dict[str, str]] | None:
    if not isinstance(value, list):
        return None
    out: list[dict[str, str]] = []
    for item in value:
        if not isinstance(item, dict):
            return None
        role = str(item.get("role") or "").strip()
        content = str(item.get("content") or "")
        if role not in {"system", "user", "assistant"}:
            return None
        out.append({"role": role, "content": content})
    return out


def handle_voice_turn(
    stt: Any,
    llm: Any,
    tts: Any,
    token: str,
    auth_header: str | None,
    raw_body: bytes,
    log_fn: LogFn | None = None,
    peer: str | None = None,
) -> tuple[int, dict[str, Any]]:
    """STT, Gemma y TTS en la PC. La Pi recibe el texto con corchetes y el wav."""
    if not _authorized(token, auth_header):
        return 401, {"error": "unauthorized"}
    try:
        payload = json.loads((raw_body or b"").decode("utf-8") or "{}")
    except (json.JSONDecodeError, UnicodeDecodeError):
        return 400, {"error": "invalid json"}
    if not isinstance(payload, dict):
        return 400, {"error": "invalid json"}
    messages = _message_list(payload.get("messages") or [])
    if messages is None:
        return 400, {"error": "messages required"}
    wav_b64 = str(payload.get("wav_b64") or "")
    voice = payload.get("voice")
    local_only = payload.get("local_only") is True
    language = str(payload.get("language") or "es")
    who = peer or "?"
    transcript = ""
    avg_logprob = None
    no_speech_prob = None
    if wav_b64:
        try:
            wav = base64.b64decode(wav_b64, validate=True)
        except Exception:
            return 400, {"error": "invalid wav"}
        status, body = handle_transcribe(
            stt,
            llm,
            token,
            auth_header,
            wav,
            "audio/wav",
            language,
            log_fn=log_fn,
            peer=peer,
        )
        if status != 200:
            return status, body
        transcript = str(body.get("text") or "")
        avg_logprob = body.get("avg_logprob")
        no_speech_prob = body.get("no_speech_prob")
        if not transcript.strip():
            return 200, {
                "text": "",
                "avg_logprob": avg_logprob,
                "no_speech_prob": no_speech_prob,
                "llm_text": "",
                "spoken": "",
                "wav_b64": None,
            }
        messages = [*messages, {"role": "user", "content": transcript}]
    elif not any(item["role"] == "user" and item["content"].strip() for item in messages):
        return 400, {"error": "messages required"}
    if not getattr(llm, "ready", False):
        return 200, {
            "text": transcript,
            "avg_logprob": avg_logprob,
            "no_speech_prob": no_speech_prob,
            "llm_text": "",
            "spoken": "",
            "wav_b64": None,
        }
    user_text = ""
    for item in reversed(messages):
        if item["role"] == "user":
            user_text = item["content"]
            break
    _emit(log_fn, f"[LLM] prompt recibido de {who}: {len(messages)} mensajes")
    t0 = time.perf_counter()
    try:
        content = llm.complete(messages)
    except Exception as exc:
        _emit(log_fn, f"[LLM] falló: {exc}")
        content = ""
    ms = (time.perf_counter() - t0) * 1000.0
    raw = str(content or "")
    _emit(log_fn, f'[LLM] respuesta {ms:.0f} ms: "{_clip(raw)}"')
    from reply_clean import keep_for_pi, spoken_for_tts

    kept = keep_for_pi(raw, user_text)
    spoken, tts_line = spoken_for_tts(raw, user_text)
    wav_out: str | None = None
    if tts_line and getattr(tts, "ready", False):
        _emit(
            log_fn,
            f'[TTS] texto de {who}: "{_clip(tts_line)}" voice={voice or "topa"} local_only={local_only}',
        )
        try:
            audio = tts.synthesize(tts_line, voice=voice, local_only=local_only)
        except TypeError:
            try:
                audio = tts.synthesize(tts_line, voice=voice)
            except TypeError:
                audio = tts.synthesize(tts_line)
        if isinstance(audio, (bytes, bytearray)) and audio:
            wav_out = base64.b64encode(bytes(audio)).decode("ascii")
    return 200, {
        "text": transcript,
        "avg_logprob": avg_logprob,
        "no_speech_prob": no_speech_prob,
        "llm_text": kept,
        "spoken": spoken,
        "wav_b64": wav_out,
    }
