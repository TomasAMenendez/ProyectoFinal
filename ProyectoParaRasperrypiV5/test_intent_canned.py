"""Tests livianos de rotación de respuestas y utterances cortos."""
from __future__ import annotations

import json
import sys
import unittest
from pathlib import Path
from unittest.mock import MagicMock

sys.modules.setdefault("cv2", MagicMock())
sys.modules.setdefault("mediapipe", MagicMock())
sys.modules.setdefault("sounddevice", MagicMock())

from workers import IntentDispatcher


class TestCannedRotation(unittest.TestCase):
    def setUp(self) -> None:
        self.dispatcher = IntentDispatcher.__new__(IntentDispatcher)
        self.dispatcher._last_canned = {}

    def test_no_repite_si_hay_alternativa(self) -> None:
        seen = set()
        for _ in range(8):
            seen.add(self.dispatcher._pick_response("greeting", ["A", "B"]))
        self.assertEqual(seen, {"A", "B"})

    def test_si_corto_no_es_enojo(self) -> None:
        self.assertTrue(self.dispatcher._utterance_too_thin("Sí.", "emotion_angry"))
        self.assertFalse(self.dispatcher._utterance_too_thin("estoy enojado", "emotion_angry"))


class TestActivityOfferNoListaJuegos(unittest.TestCase):
    _GAME_TOKENS = frozenset(
        {"adivinar", "adivina", "sombras", "sombra", "contar", "piedra", "papel", "tijera"}
    )

    def _responses(self, intent_name: str) -> list[str]:
        rules = json.loads(
            (Path(__file__).resolve().parent / "intent_rules.json").read_text(
                encoding="utf-8"
            )
        )
        return list(rules[intent_name]["response"])

    def test_activity_offer_no_nombra_los_cuatro_juegos(self) -> None:
        from game_catalog import fold_text

        for line in self._responses("activity_offer"):
            tokens = set(fold_text(line).split())
            hit = tokens & self._GAME_TOKENS
            self.assertFalse(
                hit,
                f"activity_offer lista juegos {hit}: {line!r}",
            )

    def test_play_generic_tampoco_lista_el_menu(self) -> None:
        from game_catalog import fold_text

        for line in self._responses("play_generic"):
            tokens = set(fold_text(line).split())
            hit = tokens & self._GAME_TOKENS
            self.assertFalse(
                hit,
                f"play_generic lista juegos {hit}: {line!r}",
            )


if __name__ == "__main__":
    unittest.main()
