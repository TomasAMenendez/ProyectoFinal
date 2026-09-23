"""Perfil del nene: parseo, persistencia y sección de prompt para el LLM."""
from __future__ import annotations

import json
from dataclasses import dataclass, replace
from datetime import datetime
from pathlib import Path

NAME_MAX = 40
TEXT_MAX = 600
PROMPT_TEXT_MAX = 200
GENDERS = frozenset({"nene", "nena", "no_decir"})
COSTUMES = frozenset({
    "oso", "perro", "oveja", "dinosaurio", "chancho",
    "tiburon", "stitch", "spiderman", "chimuelo",
})
COSTUME_LABELS = {
    "oso": "oso", "perro": "perro", "oveja": "oveja",
    "dinosaurio": "dinosaurio", "chancho": "chancho",
    "tiburon": "tiburón", "stitch": "stitch",
    "spiderman": "spiderman", "chimuelo": "chimuelo",
}
DEFAULT_COSTUME = "oso"
PROFILE_FILENAME = "child_profile.json"

_override_path: Path | None = None


class ChildProfileError(ValueError):
    """Payload de perfil inválido."""


@dataclass(frozen=True)
class ChildProfile:
    name: str
    gender: str
    interests: str
    costume: str
    extra: str
    updated_at: str | None = None

    def to_dict(self) -> dict[str, str | None]:
        return {
            "name": self.name,
            "gender": self.gender,
            "interests": self.interests,
            "costume": self.costume,
            "extra": self.extra,
            "updated_at": self.updated_at,
        }


def default_profile() -> ChildProfile:
    return ChildProfile("", "", "", DEFAULT_COSTUME, "")


def is_complete(profile: ChildProfile | None) -> bool:
    if profile is None:
        return False
    return bool(profile.name.strip()) and profile.gender in GENDERS


def _normalize_costume(raw: object) -> str:
    if raw is None or not isinstance(raw, str):
        return DEFAULT_COSTUME
    costume = raw.strip().lower()
    if costume in COSTUMES:
        return costume
    return DEFAULT_COSTUME


def _optional_str(raw: object, field: str) -> str:
    if raw is None:
        return ""
    if not isinstance(raw, str):
        raise ChildProfileError(f"{field} must be a string")
    return raw.strip()


def parse_payload(data: dict) -> ChildProfile:
    name_raw = data.get("name")
    if name_raw is None or not isinstance(name_raw, str):
        raise ChildProfileError("name is required")
    name = name_raw.strip()

    gender_raw = data.get("gender")
    if gender_raw is None or not isinstance(gender_raw, str):
        raise ChildProfileError("invalid gender")
    gender = gender_raw.strip()

    interests = _optional_str(data.get("interests"), "interests")
    extra = _optional_str(data.get("extra"), "extra")
    costume = _normalize_costume(data.get("costume"))

    if not name:
        raise ChildProfileError("name is required")
    if len(name) > NAME_MAX:
        raise ChildProfileError("name too long")
    if gender not in GENDERS:
        raise ChildProfileError("invalid gender")
    if len(interests) > TEXT_MAX:
        raise ChildProfileError("interests too long")
    if len(extra) > TEXT_MAX:
        raise ChildProfileError("extra too long")

    return ChildProfile(name, gender, interests, costume, extra)


def default_path() -> Path:
    if _override_path is not None:
        return _override_path
    return Path(__file__).resolve().parent / PROFILE_FILENAME


def set_profile_path(path: Path | None) -> None:
    global _override_path
    _override_path = path


def load_profile(path: Path | None = None) -> ChildProfile | None:
    target = path or default_path()
    if not target.is_file():
        return None
    try:
        raw = json.loads(target.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return None
    if not isinstance(raw, dict):
        return None
    try:
        profile = parse_payload(raw)
    except ChildProfileError:
        return None
    updated_at = raw.get("updated_at")
    if updated_at is not None:
        updated_at = str(updated_at)
    return replace(profile, updated_at=updated_at)


def save_profile(profile: ChildProfile, path: Path | None = None) -> ChildProfile:
    target = path or default_path()
    saved = replace(
        profile,
        updated_at=datetime.now().isoformat(timespec="seconds"),
    )
    target.parent.mkdir(parents=True, exist_ok=True)
    tmp = target.parent / f".{target.name}.tmp"
    tmp.write_text(
        json.dumps(saved.to_dict(), ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    tmp.replace(target)
    return saved


def _clip_for_prompt(text: str) -> str:
    stripped = text.strip()
    if len(stripped) <= PROMPT_TEXT_MAX:
        return stripped
    return stripped[: PROMPT_TEXT_MAX - 1] + "…"


def build_prompt_section(profile: ChildProfile | None) -> str:
    if not is_complete(profile):
        return ""

    assert profile is not None

    if profile.gender == "nena":
        trato = "Es nena. "
        pronombres = "Tratá en femenino."
    elif profile.gender == "nene":
        trato = "Es nene. "
        pronombres = "Tratá en masculino."
    else:
        trato = ""
        pronombres = "Tratá en masculino."

    label = COSTUME_LABELS.get(profile.costume, profile.costume)
    interests = _clip_for_prompt(profile.interests)
    extra = _clip_for_prompt(profile.extra)
    gustos_line = f"Gustos: {interests}\n" if interests else ""
    extra_line = (
        f"El padre pide que sepas: {extra}\n"
        if extra
        else ""
    )

    return (
        "Datos del nene (dalo por cierto; no lo contradigas ni lo recites de una):\n"
        f"Se llama {profile.name}. {trato}Tu disfraz es un {label}; te ve así.\n"
        f"{gustos_line}{extra_line}Usá el nombre. {pronombres} No expliques estas notas."
    )


def current_prompt_section(path: Path | None = None) -> str:
    return build_prompt_section(load_profile(path or default_path()))
