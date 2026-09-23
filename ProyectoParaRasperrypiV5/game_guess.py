"""Sesión de adivinar con 3 intentos, pistas e imagen de LCD."""
from __future__ import annotations

import random
from collections.abc import Callable, Sequence
from dataclasses import dataclass

from game_catalog import (
    GuessItem,
    answer_accepted,
    first_spoken_answer,
    load_catalog,
    resolve_media,
)
from game_engine import GameResponse, _CELEBRATION_PHRASES, _ENCOURAGEMENT_PHRASES, _wants_to_exit

_KIND_OPENING = {
    "contar": "¡Vamos a contar! Mirá la pantalla.",
    "sombras": "¡Adiviná la sombra!",
    "adivina": "¡Adiviná el objeto!",
}

_KIND_BYE = {
    "contar": "¡Fue muy divertido contar juntos!",
    "sombras": "¡Fue muy divertido adivinar sombras!",
    "adivina": "¡Fue muy divertido adivinar el objeto!",
}

HINT_FRAMES = (
    "No era esa. Te doy una pista: {hint}",
    "Estás cerca. Te doy una pista: {hint}",
    "Casi casi. Te doy una pista: {hint}",
    "Probá de nuevo. Te doy una pista: {hint}",
)

_HINTED_KINDS = frozenset({"sombras", "adivina"})


def frame_follow_hint(
    hint: str,
    pick_frame: Callable[[Sequence[str]], str] | None = None,
) -> str:
    picker = pick_frame or random.choice
    template = picker(HINT_FRAMES)
    return template.format(hint=hint.strip())


def pick_item(kind: str, rng=random.choice) -> GuessItem | None:
    catalog = load_catalog(kind)
    if not catalog:
        return None
    return rng(catalog)


@dataclass
class GuessSession:
    kind: str
    item: GuessItem
    attempts: int = 0
    max_attempts: int = 3
    state: str = "waiting_answer"
    pick_frame: Callable[[Sequence[str]], str] | None = None

    def opening(self) -> GameResponse:
        text = f"{_KIND_OPENING.get(self.kind, '¡Juguemos!')} {self.item.spoken_open}"
        return GameResponse(
            text=text,
            lcd_image=resolve_media(self.item.persist_image),
            lcd_clear_after_speech=False,
        )

    def process_input(self, text: str) -> GameResponse:
        normalized = (text or "").strip()
        if _wants_to_exit(normalized.lower()):
            return GameResponse(
                text=f"{_KIND_BYE.get(self.kind, '¡Gracias por jugar!')} Cuando quieras jugamos de nuevo.",
                game_over=True,
                lcd_clear_after_speech=True,
            )
        if len(normalized) < 1:
            return GameResponse(
                text="No escuché bien. ¿Qué dijiste? Probá de nuevo.",
                lcd_image=resolve_media(self.item.persist_image),
            )

        if answer_accepted(normalized, self.item.answers):
            celebration = random.choice(_CELEBRATION_PHRASES)
            shown = first_spoken_answer(self.item.answers)
            return GameResponse(
                text=f"{celebration} ¡Era {shown}!",
                game_over=True,
                lcd_image=resolve_media(self.item.reveal_image),
                lcd_clear_after_speech=True,
            )

        self.attempts += 1
        if self.attempts >= self.max_attempts:
            shown = first_spoken_answer(self.item.answers)
            return GameResponse(
                text=(
                    f"¡Era {shown}! No te preocupes, la próxima la adivinás. "
                    "¡Seguí intentando, sos {un} crack!"
                ),
                game_over=True,
                lcd_image=resolve_media(self.item.reveal_image),
                lcd_clear_after_speech=True,
            )

        hint_index = self.attempts - 1
        if hint_index < len(self.item.follow_hints):
            line = self.item.follow_hints[hint_index]
            if self.kind in _HINTED_KINDS:
                line = frame_follow_hint(line, self.pick_frame)
        else:
            line = random.choice(_ENCOURAGEMENT_PHRASES)
        persist = resolve_media(self.item.persist_image)
        return GameResponse(
            text=line,
            lcd_image=persist,
            lcd_clear_after_speech=False,
        )
