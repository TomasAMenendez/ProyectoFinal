"""Motor de juegos interactivos multi-turno.

Cuando hay un juego activo, el input del niño se redirige aquí en vez del
dispatcher de intenciones.
"""
from __future__ import annotations

import random
import re
from dataclasses import dataclass
from typing import Any

from debug_logger import log_action
from game_catalog import resolve_media
from stt_correct import correct_stt_text


def _wants_to_exit(text: str) -> bool:
    """Detecta si el niño quiere salir del juego usando match por palabras completas."""
    normalized = text.lower().strip()
    exit_phrases = {"no quiero jugar", "no quiero más", "me aburro", "me aburrí"}
    if any(phrase in normalized for phrase in exit_phrases):
        return True
    exit_exact_words = {"basta", "salir", "parar", "chau"}
    words_in_text = set(re.findall(r"\w+", normalized))
    return bool(words_in_text & exit_exact_words)


_PIEDRA_PAPEL_CHOICES = ["piedra", "papel", "tijera"]
_RPS_IMAGE_FILES = {
    "piedra": "Imagen_Piedra.png",
    "papel": "Imagen_Papel.png",
    "tijera": "Imagen_Tijera.png",
}

_CELEBRATION_PHRASES = [
    "¡Muy bien! ¡Sos {un} {genio}!",
    "¡Excelente! ¡Lo adivinaste!",
    "¡Bravo! ¡Qué crack que sos!",
    "¡Increíble! ¡Sabía que podías!",
]

_ENCOURAGEMENT_PHRASES = [
    "¡Casi casi! Intentá de nuevo, vos podés.",
    "¡Buen intento! Dale, otra vez.",
    "Mmm no era esa, pero estás muy cerca. ¡Probá otra vez!",
]

_WIN_PHRASES = [
    "¡Ganaste vos! ¡Felicitaciones, {campeon}!",
    "¡Me ganaste! Sos muy {bueno} en esto.",
]

_LOSE_PHRASES = [
    "¡Gané yo esta vez! Pero jugaste muy bien. ¿Otra ronda?",
    "¡Esta vez gané! ¿Revancha?",
]

_TIE_PHRASES = [
    "¡Empatamos! Pensamos igual. ¿Otra vez?",
    "¡Empate! ¡Qué coincidencia! Dale de nuevo.",
]

_RPS_SHOW_HAND_PHRASE = (
    "Mostrame la mano o decime piedra, papel o tijera. ¡Uno, dos, tres!"
)
_RPS_NOT_UNDERSTOOD_PHRASE = (
    "No entendí tu elección. Decí piedra, papel o tijera. ¡Uno, dos, tres!"
)

_INTENT_TO_GAME = {
    "play_veo_veo": "adivina",
    "play_adivina": "adivina",
    "play_contar": "contar",
    "play_sombras": "sombras",
    "play_piedra_papel": "piedra_papel_tijera",
}

_GUESS_KINDS = frozenset({"adivina", "contar", "sombras"})


@dataclass
class GameResponse:
    """Resultado de procesar un turno de juego."""
    text: str
    game_over: bool = False
    pilar: str = "cognitivo"
    lcd_image: str | None = None
    lcd_clear_after_speech: bool = False

    def __post_init__(self) -> None:
        from gender_speech import inflect_for_child

        self.text = inflect_for_child(self.text)


@dataclass
class PiedraPapelTijeraSession:
    """Juego Piedra-papel-tijera."""

    rounds_played: int = 0
    max_rounds: int = 3
    wins_child: int = 0
    wins_robot: int = 0
    state: str = "waiting_choice"
    empty_tries: int = 0
    round_committed: bool = False

    def process_input(self, text: str, *, saw_confident: bool = False) -> GameResponse:
        normalized = correct_stt_text(
            text,
            extra_words=_PIEDRA_PAPEL_CHOICES,
        ).lower().strip()

        if _wants_to_exit(normalized):
            return self._end_game()

        child_choice = None
        for choice in _PIEDRA_PAPEL_CHOICES:
            if choice in normalized:
                child_choice = choice
                break

        if child_choice is not None:
            if self.round_committed:
                return GameResponse(text="")
            return self.process_choice(child_choice)

        if saw_confident:
            return GameResponse(text="")

        return self._retry_empty()

    def process_choice(self, label: str) -> GameResponse:
        if self.round_committed:
            return GameResponse(text="")
        if label not in _PIEDRA_PAPEL_CHOICES:
            return self._retry_empty()

        robot_choice = random.choice(_PIEDRA_PAPEL_CHOICES)
        self.rounds_played += 1
        self.empty_tries = 0
        self.round_committed = True

        result = self._resolve(label, robot_choice)

        if result == "tie":
            phrase = random.choice(_TIE_PHRASES)
        elif result == "child_wins":
            self.wins_child += 1
            phrase = random.choice(_WIN_PHRASES)
        else:
            self.wins_robot += 1
            phrase = random.choice(_LOSE_PHRASES)

        response_text = f"Yo elegí {robot_choice}. {phrase}"
        lcd_image = resolve_media(_RPS_IMAGE_FILES[robot_choice])

        if self.rounds_played >= self.max_rounds:
            return self._end_game(prefix=response_text, lcd_image=lcd_image)

        response_text += " ¡Uno, dos, tres!"
        return GameResponse(
            text=response_text,
            lcd_image=lcd_image,
            lcd_clear_after_speech=True,
        )

    def _retry_empty(self) -> GameResponse:
        if self.empty_tries == 0:
            self.empty_tries = 1
            return GameResponse(text=_RPS_SHOW_HAND_PHRASE)
        return GameResponse(text=_RPS_NOT_UNDERSTOOD_PHRASE)

    def prepare_next_round(self) -> None:
        self.round_committed = False
        self.state = "waiting_choice"

    def _resolve(self, child: str, robot: str) -> str:
        if child == robot:
            return "tie"
        wins = {
            ("piedra", "tijera"),
            ("papel", "piedra"),
            ("tijera", "papel"),
        }
        return "child_wins" if (child, robot) in wins else "robot_wins"

    def _end_game(self, prefix: str = "", lcd_image: str | None = None) -> GameResponse:
        summary = (
            f"Jugamos {self.rounds_played} rondas. "
            f"Vos ganaste {self.wins_child} y yo gané {self.wins_robot}. "
        )
        if self.wins_child > self.wins_robot:
            summary += "¡Ganaste vos! ¡Felicitaciones, {campeon}!"
        elif self.wins_robot > self.wins_child:
            summary += "¡Gané yo esta vez! Pero jugaste increíble."
        else:
            summary += "¡Empatamos! Somos un gran equipo."

        full_text = f"{prefix} {summary}".strip() if prefix else summary
        return GameResponse(
            text=full_text,
            game_over=True,
            lcd_image=lcd_image,
            lcd_clear_after_speech=True,
        )


class GameEngine:
    """Gestiona la sesión de juego activa."""

    def __init__(self) -> None:
        self._session: Any = None

    @property
    def is_active(self) -> bool:
        return self._session is not None

    @property
    def game_type(self) -> str | None:
        from game_guess import GuessSession

        session = self._session
        if isinstance(session, GuessSession):
            return session.kind
        if isinstance(session, PiedraPapelTijeraSession):
            return "piedra_papel_tijera"
        return None

    @property
    def waiting_rps_choice(self) -> bool:
        session = self._session
        return (
            isinstance(session, PiedraPapelTijeraSession)
            and session.state == "waiting_choice"
        )

    def start_game(self, game_type: str) -> GameResponse:
        if game_type in _GUESS_KINDS:
            from game_guess import GuessSession, pick_item

            item = pick_item(game_type)
            if item is None:
                return GameResponse(text="Ahora no puedo iniciar ese juego.")
            session = GuessSession(kind=game_type, item=item)
            self._session = session
            log_action("GAME", f"inicio {game_type}")
            return session.opening()
        if game_type == "piedra_papel_tijera":
            self._session = PiedraPapelTijeraSession()
            log_action("GAME", "inicio piedra_papel_tijera")
            return GameResponse(
                text=(
                    "¡Dale, juguemos a Piedra, papel o tijera! "
                    "¿Listo? ¡Uno, dos, tres!"
                )
            )
        if game_type == "veo_veo":
            return self.start_game("adivina")
        return GameResponse(text="")

    def process_input(self, text: str, *, saw_confident: bool = False) -> GameResponse | None:
        if self._session is None:
            return None

        if isinstance(self._session, PiedraPapelTijeraSession):
            response = self._session.process_input(text, saw_confident=saw_confident)
        else:
            response = self._session.process_input(text)
        if response.game_over:
            log_action("GAME", f"fin {self.game_type or 'juego'}")
            self._session = None
        return response

    def commit_choice(self, label: str) -> GameResponse | None:
        session = self._session
        if not isinstance(session, PiedraPapelTijeraSession):
            return None
        if session.round_committed:
            return None
        response = session.process_choice(label)
        if response.game_over:
            log_action("GAME", f"fin {self.game_type or 'juego'}")
            self._session = None
        return response

    def prepare_next_rps_round(self) -> None:
        session = self._session
        if isinstance(session, PiedraPapelTijeraSession):
            session.prepare_next_round()

    def process_or_passthrough(
        self,
        text: str,
        dispatcher_result: dict[str, Any],
        *,
        saw_confident: bool = False,
    ) -> dict[str, Any]:
        if not self.is_active:
            intent_name = dispatcher_result.get("intent_name", "")
            game_type = _INTENT_TO_GAME.get(str(intent_name))
            if game_type:
                opened = self.start_game(game_type)
                payload = _payload_from_response(opened, str(intent_name))
                payload["game_started"] = True
                payload["confidence"] = dispatcher_result.get("confidence", 1.0)
                return payload
            return dispatcher_result

        current_game_type = self.game_type or "unknown"
        game_response = self.process_input(text, saw_confident=saw_confident)
        if game_response is None:
            return dispatcher_result
        return _payload_from_response(game_response, f"game_{current_game_type}")

    def cancel(self) -> None:
        self._session = None


_PLAY_GAME_ALIASES = {
    "sombras": "sombras",
    "sombra": "sombras",
    "silueta": "sombras",
    "siluetas": "sombras",
    "contar": "contar",
    "cuenta": "contar",
    "adivina": "adivina",
    "objeto": "adivina",
    "adivinar": "adivina",
    "piedra": "piedra_papel_tijera",
    "papel": "piedra_papel_tijera",
    "tijera": "piedra_papel_tijera",
    "ppt": "piedra_papel_tijera",
}


def child_asked_for_play_game(user_text: str, param: str) -> bool:
    """True solo si el nene pidió ese juego, no si el LLM lo nombró."""
    wanted = resolve_play_game_param(param)
    if wanted is None:
        return False
    from session_policy import is_clear_keyword_intent

    intent = is_clear_keyword_intent(user_text)
    if intent == "play_generic" or intent == "activity_offer":
        return False
    mapped = _INTENT_TO_GAME.get(str(intent or ""))
    return mapped == wanted


def resolve_play_game_param(param: str) -> str | None:
    from game_catalog import fold_text

    folded = fold_text(param)
    if not folded:
        return None
    if folded in _PLAY_GAME_ALIASES:
        return _PLAY_GAME_ALIASES[folded]
    for token in folded.split():
        kind = _PLAY_GAME_ALIASES.get(token)
        if kind:
            return kind
    return None


def apply_play_game_tag(
    engine: GameEngine,
    param: str,
    payload: dict[str, Any],
    user_text: str = "",
) -> bool:
    if payload.get("game_over"):
        return False
    if not child_asked_for_play_game(user_text, param):
        return False
    kind = resolve_play_game_param(param)
    if kind is None:
        return False
    opened = engine.start_game(kind)
    if not str(opened.text or "").strip() and not engine.is_active:
        return False
    payload.update(_payload_from_response(opened, f"play_{kind}"))
    payload["game_started"] = True
    return True


def _payload_from_response(response: GameResponse, intent_name: str) -> dict[str, Any]:
    from gender_speech import inflect_for_child

    payload: dict[str, Any] = {
        "intent_name": intent_name,
        "confidence": 1.0,
        "response": inflect_for_child(response.text),
        "pilar": response.pilar,
        "skip_tts_truncate": True,
        "lcd_image": response.lcd_image,
        "lcd_clear_after_speech": response.lcd_clear_after_speech,
        "game_over": response.game_over,
    }
    return payload
