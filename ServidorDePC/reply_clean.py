"""Limpieza de la respuesta de Gemma antes del TTS. No toca los corchetes que ve la Pi."""
from __future__ import annotations

import re
import unicodedata

_FOLLOW_AFTER_LEAK = "Contame más, ¿qué pasó?"
_NAME_QUESTION = re.compile(
    r"c[oó]mo te llam|qui[eé]n sos|\btu nombre\b",
    re.IGNORECASE,
)
_LEAK_REPLIES = frozenset({
    "veo veo",
    "veoveo",
    "me llamo teo",
    "neutral",
    "feliz",
    "triste",
    "enojado",
    "sorprendido",
    "intents_on",
    "intents_off",
    "intents on",
    "intents off",
    "play_music",
    "play_story",
    "list_stories",
    "list_music",
    "calm_mode",
    "go_to_sleep",
})
_VEO_VEO_SPEECH = re.compile(r"\bveo[\s\-]*veo\b", re.IGNORECASE)
_ACTION_TAG_RE = re.compile(
    r"\["
    r"(PLAY_MUSIC|STOP_MUSIC|NOTIFY_PARENT|EXPRESSION|CELEBRATE|HUG|CALM_MODE|INTENTS_OFF|INTENTS_ON|GO_TO_SLEEP|LIST_STORIES|LIST_MUSIC|PLAY_STORY|PLAY_GAME)"
    r"(?::([^\]]*))?"
    r"\]",
    re.IGNORECASE,
)
_ANY_BRACKET_RE = re.compile(r"\[[^\]]*\]")
_BARE_NOTIFY_RE = re.compile(r"NOTIFY_PARENT\s*:?\s*.*$", re.IGNORECASE)
_TAGS_PROSE_RE = re.compile(r"(?i)(?:^|\s)tags?\s*:.*$")
_INCOMPLETE_BRACKET_RE = re.compile(r"\[[^\]]*$")


def _fold_es(text: str) -> str:
    folded = unicodedata.normalize("NFD", text or "")
    folded = "".join(c for c in folded if unicodedata.category(c) != "Mn")
    return re.sub(r"\s+", " ", folded.lower()).strip()


def clean_format(text: str) -> str:
    """Saca emojis, asteriscos y paréntesis. Conserva los corchetes."""
    if not text:
        return ""
    cleaned = re.sub(r"\*[^*]*\*", "", text)
    cleaned = re.sub(r"\([^\)]*\)", "", cleaned)
    cleaned = re.sub(
        r"[\U00010000-\U0010ffff\u2600-\u27ff\u2300-\u23ff\u2b50]",
        "",
        cleaned,
    )
    cleaned = cleaned.replace('"', "").replace("“", "").replace("”", "")
    cleaned = " ".join(cleaned.split()).strip()
    if not cleaned:
        cleaned = text.replace('"', "").strip()
    return cleaned


def drop_prompt_leak(user_text: str, reply: str) -> str:
    raw = (reply or "").strip()
    if not raw:
        return reply
    folded = _fold_es(raw).rstrip(".!…").strip()
    compact = folded.replace(" ", "")
    if folded not in _LEAK_REPLIES and compact not in _LEAK_REPLIES:
        return reply
    if folded == "me llamo teo" and _NAME_QUESTION.search(user_text or ""):
        return reply
    return _FOLLOW_AFTER_LEAK


def scrub_veo_veo_speech(reply: str) -> str:
    if not (reply or "").strip():
        return reply
    return _VEO_VEO_SPEECH.sub("adivinar el objeto", reply)


def strip_unspeakable(text: str) -> str:
    cleaned = _ACTION_TAG_RE.sub("", text or "")
    cleaned = _ANY_BRACKET_RE.sub("", cleaned)
    cleaned = _BARE_NOTIFY_RE.sub("", cleaned)
    cleaned = _INCOMPLETE_BRACKET_RE.sub("", cleaned)
    cleaned = _TAGS_PROSE_RE.sub("", cleaned)
    cleaned = re.sub(r"\s{2,}", " ", cleaned).strip()
    if not any(ch.isalpha() for ch in cleaned):
        return ""
    return cleaned


def truncate_words(text: str, max_words: int = 50) -> str:
    words = (text or "").split()
    if len(words) <= max_words:
        return text or ""
    truncated = " ".join(words[:max_words])
    for sep in (".", "?", "!", "。"):
        last_idx = truncated.rfind(sep)
        if last_idx > 0:
            return truncated[: last_idx + 1]
    return truncated.rstrip(",;: ") + "."


def soften_caps(text: str) -> str:
    parts: list[str] = []
    for token in text.split(" "):
        letters = [c for c in token if c.isalpha()]
        if len(letters) >= 2 and all(c.isupper() for c in letters):
            parts.append("".join(c.lower() if c.isalpha() else c for c in token))
        else:
            parts.append(token)
    return " ".join(parts)


def prepare_tts_line(text: str) -> str:
    cleaned = re.sub(r"\s{2,}", " ", text or "").strip()
    if not cleaned:
        return ""
    cleaned = soften_caps(cleaned)
    if cleaned[-1] not in ".!?…":
        cleaned += "."
    return cleaned


def keep_for_pi(raw: str, user_text: str) -> str:
    """Texto con corchetes para que la Pi ejecute acciones. No recorta ofertas de cuento."""
    text = clean_format(raw)
    text = drop_prompt_leak(user_text, text)
    return scrub_veo_veo_speech(text)


def spoken_for_tts(raw: str, user_text: str) -> tuple[str, str]:
    """Devuelve (texto que la Pi compara, línea que entra al TTS)."""
    kept = keep_for_pi(raw, user_text)
    spoken = truncate_words(strip_unspeakable(kept))
    return spoken, prepare_tts_line(spoken)
