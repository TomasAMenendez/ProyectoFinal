"""Género gramatical de las frases enlatadas según el perfil del chico."""
from __future__ import annotations

import re

# nena → femenino. nene, no_decir, vacío u otro → masculino.
_FORMS: dict[str, tuple[str, str]] = {
    "un": ("un", "una"),
    "genio": ("genio", "genia"),
    "campeon": ("campeón", "campeona"),
    "bueno": ("bueno", "buena"),
    "contento": ("contento", "contenta"),
    "solito": ("solito", "solita"),
    "ordenado": ("ordenado", "ordenada"),
    "mismo": ("mismo", "misma"),
    "cansado": ("cansado", "cansada"),
}

_TOKEN = re.compile(r"\{([a-z_]+)\}")


def speech_gender(raw: str | None) -> str:
    if (raw or "").strip().lower() == "nena":
        return "f"
    return "m"


def inflect(text: str, gender_raw: str | None) -> str:
    if not text or "{" not in text:
        return text
    idx = 0 if speech_gender(gender_raw) == "m" else 1

    def repl(match: re.Match[str]) -> str:
        pair = _FORMS.get(match.group(1))
        if pair is None:
            return match.group(0)
        return pair[idx]

    return _TOKEN.sub(repl, text)


def inflect_for_child(text: str) -> str:
    try:
        from child_profile import load_profile

        profile = load_profile()
        raw = profile.gender if profile is not None else ""
    except Exception:
        raw = ""
    return inflect(text, raw)
