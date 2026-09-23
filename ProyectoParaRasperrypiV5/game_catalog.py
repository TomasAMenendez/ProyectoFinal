"""Catálogos de juegos de adivinar. JSON en el borde, matching puro adentro."""
from __future__ import annotations

import json
import re
import unicodedata
from collections.abc import Sequence
from dataclasses import dataclass
from pathlib import Path

APP_DIR = Path(__file__).resolve().parent
JUEGOS_DIR = APP_DIR / "juegos"
MEDIA_DIR = JUEGOS_DIR / "media"
REPO_JUEGOS = APP_DIR.parent / "Juegos"

_NUM_WORDS = {
    "cero": "0",
    "uno": "1",
    "una": "1",
    "un": "1",
    "dos": "2",
    "tres": "3",
    "cuatro": "4",
    "cinco": "5",
    "seis": "6",
    "siete": "7",
    "ocho": "8",
    "nueve": "9",
    "diez": "10",
}


@dataclass(frozen=True)
class GuessItem:
    kind: str
    item_id: str
    answers: tuple[str, ...]
    spoken_open: str
    follow_hints: tuple[str, ...]
    persist_image: str | None
    reveal_image: str | None


def fold_text(text: str) -> str:
    normalized = unicodedata.normalize("NFKD", text or "")
    stripped = "".join(ch for ch in normalized if not unicodedata.combining(ch))
    cleaned = re.sub(r"[^\w\s]", " ", stripped.lower())
    return re.sub(r"\s+", " ", cleaned).strip()


def _tokens(text: str) -> set[str]:
    folded = fold_text(text)
    tokens = set(re.findall(r"\w+", folded))
    extra: set[str] = set()
    for word, digit in _NUM_WORDS.items():
        if word in tokens:
            extra.add(digit)
    extra.update(re.findall(r"\d+", folded))
    return tokens | extra


def answer_accepted(guess: str, answers: Sequence[str]) -> bool:
    """True si el nene dijo cualquiera de las respuestas del JSON."""
    folded_guess = fold_text(guess)
    if len(folded_guess) < 1:
        return False
    guess_tokens = _tokens(guess)
    digits = {str(n) for n in range(0, 21)}
    guess_digits = guess_tokens & digits
    answer_digits_all: set[str] = set()
    for raw in answers:
        answer_digits_all |= _tokens(raw) & digits
    if answer_digits_all:
        return bool(guess_digits & answer_digits_all)
    for raw in answers:
        folded = fold_text(raw)
        if not folded:
            continue
        if folded_guess == folded:
            return True
        answer_tokens = _tokens(raw)
        if folded in guess_tokens:
            return True
        meaningful_guess = {w for w in guess_tokens if len(w) >= 3}
        meaningful_answer = {w for w in answer_tokens if len(w) >= 3}
        if meaningful_guess and meaningful_answer and meaningful_guess & meaningful_answer:
            return True
    return False


def first_spoken_answer(answers: Sequence[str]) -> str:
    for raw in answers:
        text = str(raw or "").strip()
        if text:
            return text
    return "eso"


def resolve_media(filename: str | None) -> str | None:
    name = (filename or "").strip()
    if not name:
        return None
    roots = (
        MEDIA_DIR,
        REPO_JUEGOS / "Juego contar",
        REPO_JUEGOS / "Juego sombras",
        REPO_JUEGOS / "Juego piedra, papel o tijera",
    )
    for root in roots:
        path = root / name
        if path.is_file():
            return str(path)
    wanted = fold_text(name)
    for root in roots:
        if not root.is_dir():
            continue
        for candidate in root.iterdir():
            if candidate.is_file() and fold_text(candidate.name) == wanted:
                return str(candidate)
    return None


def _load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def _json_path(name: str, fallback: Path) -> Path:
    local = JUEGOS_DIR / name
    if local.is_file():
        return local
    return fallback


def parse_contar(raw) -> list[GuessItem]:
    items: list[GuessItem] = []
    rows = raw if isinstance(raw, list) else []
    for index, row in enumerate(rows):
        if not isinstance(row, dict):
            continue
        answers = tuple(str(a) for a in (row.get("respuestas") or []) if str(a).strip())
        image = str(row.get("imagen_archivo") or "").strip() or None
        question = str(row.get("pregunta") or "").strip()
        if not answers or not question:
            continue
        items.append(
            GuessItem(
                kind="contar",
                item_id=f"contar-{index}",
                answers=answers,
                spoken_open=question,
                follow_hints=(),
                persist_image=image,
                reveal_image=image,
            )
        )
    return items


def parse_sombras(raw) -> list[GuessItem]:
    items: list[GuessItem] = []
    rows = raw.get("juego_siluetas") if isinstance(raw, dict) else raw
    if not isinstance(rows, list):
        return items
    for row in rows:
        if not isinstance(row, dict):
            continue
        answers = tuple(str(a) for a in (row.get("respuestas") or []) if str(a).strip())
        hints = tuple(str(h) for h in (row.get("pistas") or []) if str(h).strip())
        item_id = str(row.get("id") or "").strip() or "sombra"
        if not answers:
            continue
        items.append(
            GuessItem(
                kind="sombras",
                item_id=item_id,
                answers=answers,
                spoken_open="Mirá la sombra en la pantalla. ¿Qué es?",
                follow_hints=hints,
                persist_image=str(row.get("silueta_archivo") or "").strip() or None,
                reveal_image=str(row.get("imagen_archivo") or "").strip() or None,
            )
        )
    return items


def parse_adivina(raw) -> list[GuessItem]:
    items: list[GuessItem] = []
    rows = raw if isinstance(raw, list) else []
    for row in rows:
        if not isinstance(row, dict):
            continue
        answers = tuple(str(a) for a in (row.get("respuestas") or []) if str(a).strip())
        hints = tuple(str(h) for h in (row.get("pistas") or []) if str(h).strip())
        if not answers or not hints:
            continue
        spoken, follow = hints[0], hints[1:]
        items.append(
            GuessItem(
                kind="adivina",
                item_id=str(row.get("id") or spoken),
                answers=answers,
                spoken_open=spoken,
                follow_hints=follow,
                persist_image=None,
                reveal_image=str(row.get("imagen_archivo") or "").strip() or None,
            )
        )
    return items


def load_catalog(kind: str) -> list[GuessItem]:
    if kind == "contar":
        path = _json_path("contar.json", REPO_JUEGOS / "Juego contar" / "juego_contar.json")
        return parse_contar(_load_json(path))
    if kind == "sombras":
        path = _json_path("sombras.json", REPO_JUEGOS / "Juego sombras" / "juego_sombras.json")
        return parse_sombras(_load_json(path))
    if kind == "adivina":
        path = _json_path("adivina.json", REPO_JUEGOS / "juego_adivinaElObjeto.json")
        return parse_adivina(_load_json(path))
    return []
