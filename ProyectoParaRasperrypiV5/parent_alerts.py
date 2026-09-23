"""Mensajes de alerta para la app parental.

La bandeja del padre muestra avisos, logros, llamados a mamá o papá
y crisis. El vocabulario nuevo queda en telemetría y métricas.
"""
from __future__ import annotations

PARENT_INBOX_TYPES = frozenset({"aviso", "logro", "pedido", "crisis"})


def belongs_in_parent_inbox(notif_type: str) -> bool:
    return (notif_type or "").strip().lower() in PARENT_INBOX_TYPES


def describe_child_achievement(
    param: str,
    user_text: str,
    intent_name: str = "",
) -> str:
    """Arma el texto de la notificación de logro para el padre."""
    detail = (param or "").strip()
    if detail:
        return f"El nene logró: {detail}."
    spoken = " ".join((user_text or "").split())
    if spoken:
        if len(spoken) > 90:
            spoken = spoken[:87] + "…"
        return f"El nene logró algo: dijo «{spoken}»."
    intent = (intent_name or "").strip()
    if intent and intent not in {"unknown", ""}:
        return f"El nene logró un avance en la actividad ({intent})."
    return "El nene logró un momento especial jugando con TEO."
