"""Historial de conversación. La PC recibe 10 turnos; el 3B de la Pi, 2."""
from __future__ import annotations

import os
from typing import Any

PC_MAX_TURNS = 10
LOCAL_MAX_TURNS = 2


def default_max_turns() -> int:
    """Turnos que se guardan y se mandan al servidor de PC. Default 10."""
    try:
        return max(1, int(os.environ.get("CONVO_MAX_TURNS") or str(PC_MAX_TURNS)))
    except ValueError:
        return PC_MAX_TURNS


def assistant_memory_text(raw_with_tags: str, spoken: str) -> str:
    """El historial guarda corchetes para que el LLM copie el formato."""
    raw = (raw_with_tags or "").strip()
    if raw:
        return raw
    return (spoken or "").strip()


def local_history(
    history: list[dict[str, str]],
    turns: int = LOCAL_MAX_TURNS,
) -> list[dict[str, str]]:
    """Los últimos `turns` para el LLM de la Pi. Un turno son 2 mensajes."""
    cap = max(1, turns) * 2
    return list(history)[-cap:]


class ConversationMemory:
    """Últimos N turnos (par nene + TEO). N default = lo que usa la PC."""

    MAX_TURNS = PC_MAX_TURNS

    def __init__(self, max_turns: int | None = None) -> None:
        self.max_turns = default_max_turns() if max_turns is None else max_turns
        self._messages: list[dict[str, str]] = []

    def messages(self) -> list[dict[str, str]]:
        return list(self._messages)

    def add_turn(self, user_text: str, assistant_text: str) -> None:
        user = (user_text or "").strip()
        assistant = (assistant_text or "").strip()
        if not user and not assistant:
            return
        if user:
            self._messages.append({"role": "user", "content": user})
        if assistant:
            self._messages.append({"role": "assistant", "content": assistant})
        self._trim()

    def clear(self) -> None:
        self._messages.clear()

    def _trim(self) -> None:
        max_msgs = self.max_turns * 2
        if len(self._messages) > max_msgs:
            self._messages = self._messages[-max_msgs:]

    def as_debug(self) -> dict[str, Any]:
        return {"turns": len(self._messages) // 2, "messages": self.messages()}
