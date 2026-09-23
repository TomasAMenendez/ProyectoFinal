"""Inflección de frases enlatadas según género del perfil."""
from __future__ import annotations

import unittest

from gender_speech import inflect, speech_gender


class TestSpeechGender(unittest.TestCase):
    def test_nena_es_femenino(self) -> None:
        self.assertEqual(speech_gender("nena"), "f")

    def test_no_decir_y_vacio_son_masculino(self) -> None:
        self.assertEqual(speech_gender("no_decir"), "m")
        self.assertEqual(speech_gender(""), "m")
        self.assertEqual(speech_gender(None), "m")
        self.assertEqual(speech_gender("nene"), "m")

    def test_plantilla_campeon(self) -> None:
        raw = "Sos {un} {campeon}."
        self.assertEqual(inflect(raw, "nene"), "Sos un campeón.")
        self.assertEqual(inflect(raw, "nena"), "Sos una campeona.")
        self.assertEqual(inflect(raw, "no_decir"), "Sos un campeón.")

    def test_sin_llaves_no_cambia(self) -> None:
        self.assertEqual(inflect("¡Qué crack que sos!", "nena"), "¡Qué crack que sos!")


if __name__ == "__main__":
    unittest.main()
