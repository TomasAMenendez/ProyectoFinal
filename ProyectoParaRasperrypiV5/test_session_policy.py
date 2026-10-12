"""Tests TDD de políticas de sesión (volumen, noche, playtime, privacidad, barge-in)."""
from __future__ import annotations

import unittest
from datetime import date, datetime, timedelta
from pathlib import Path
import tempfile

import numpy as np

from session_policy import (
    PlaytimeGuard,
    apply_gain,
    effective_volume_limit,
    gain_from_limit,
    is_barge_in_text,
    is_clear_keyword_intent,
    keep_audio_queue_after_tts,
    mic_open_for_listen,
    night_should_engage,
    sanitize_notification_extra,
    telemetry_range_summary,
    tts_blocks_mic,
    tts_remaining_seconds,
)


class TestVolumeGain(unittest.TestCase):
    def test_cien_no_atenuar(self) -> None:
        self.assertAlmostEqual(gain_from_limit(100), 1.0)

    def test_cincuenta_mitad(self) -> None:
        self.assertAlmostEqual(gain_from_limit(50), 0.5)

    def test_cero_silencio(self) -> None:
        self.assertAlmostEqual(gain_from_limit(0), 0.0)

    def test_clamp(self) -> None:
        self.assertAlmostEqual(gain_from_limit(200), 1.0)
        self.assertAlmostEqual(gain_from_limit(-10), 0.0)

    def test_apply_gain_escala_pico(self) -> None:
        audio = np.array([0.5, -0.5], dtype=np.float32)
        out = apply_gain(audio, 0.5)
        self.assertAlmostEqual(float(out.max()), 0.25, places=5)

    def test_scale_int16_respeta_el_tope_parental(self) -> None:
        from session_policy import scale_playback_chunk

        chunk = np.array([[32767], [-32767]], dtype=np.int16)
        out = scale_playback_chunk(chunk, 0.5)
        self.assertEqual(out.dtype, np.int16)
        self.assertAlmostEqual(int(out[0, 0]), 16383, delta=2)
        self.assertAlmostEqual(int(out[1, 0]), -16383, delta=2)

    def test_scale_float_respeta_el_tope_parental(self) -> None:
        from session_policy import scale_playback_chunk

        chunk = np.array([[1.0], [-0.4]], dtype=np.float32)
        out = scale_playback_chunk(chunk, 0.25)
        self.assertAlmostEqual(float(out[0, 0]), 0.25, places=5)
        self.assertAlmostEqual(float(out[1, 0]), -0.10, places=5)

    def test_noche_baja_el_tope(self) -> None:
        self.assertEqual(effective_volume_limit(100, night_mode=True), 40)
        self.assertEqual(effective_volume_limit(20, night_mode=True), 20)
        self.assertEqual(effective_volume_limit(100, night_mode=False), 100)


class TestNightMode(unittest.TestCase):
    def test_juegos_bloqueados(self) -> None:
        self.assertFalse(night_should_engage("play_veo_veo"))
        self.assertFalse(night_should_engage("play_piedra_papel"))
        self.assertFalse(night_should_engage("story_request"))
        self.assertFalse(night_should_engage("song_request"))
        self.assertFalse(night_should_engage("yoga_request"))

    def test_crisis_y_padres_permitidos(self) -> None:
        self.assertTrue(night_should_engage("call_parent"))
        self.assertTrue(night_should_engage("crisis_cry"))
        self.assertTrue(night_should_engage("tired_sleepy"))
        self.assertTrue(night_should_engage("hug_request"))

    def test_sin_noche_todo_pasa(self) -> None:
        self.assertTrue(night_should_engage("play_veo_veo", night_mode=False))


class TestPlaytimeGuard(unittest.TestCase):
    def test_cero_es_sin_limite(self) -> None:
        guard = PlaytimeGuard(limit_minutes=0)
        guard.add_seconds(10_000)
        self.assertFalse(guard.is_over_limit())

    def test_corta_al_exceder(self) -> None:
        guard = PlaytimeGuard(limit_minutes=1, today=date(2026, 8, 26))
        guard.add_seconds(59)
        self.assertFalse(guard.is_over_limit())
        guard.add_seconds(2)
        self.assertTrue(guard.is_over_limit())

    def test_crisis_sigue_permitida(self) -> None:
        guard = PlaytimeGuard(limit_minutes=1)
        guard.add_seconds(120)
        self.assertTrue(guard.allows_intent("call_parent"))
        self.assertFalse(guard.allows_intent("play_veo_veo"))

    def test_reset_today_vuelve_a_cero(self) -> None:
        guard = PlaytimeGuard(limit_minutes=1)
        guard.add_seconds(120)
        self.assertTrue(guard.is_over_limit())
        guard.reset_today()
        self.assertFalse(guard.is_over_limit())

    def test_is_over_limit_resetea_al_cambiar_el_dia_sin_hablar(self) -> None:
        guard = PlaytimeGuard(limit_minutes=1, today=date.today() - timedelta(days=1))
        guard._seconds = 9999
        self.assertFalse(guard.is_over_limit())

    def test_restante_none_sin_limite(self) -> None:
        guard = PlaytimeGuard(limit_minutes=0)
        guard.add_seconds(30)
        self.assertIsNone(guard.remaining_seconds())
        self.assertIsNone(guard.remaining_fraction())

    def test_restante_decrece_con_el_uso(self) -> None:
        guard = PlaytimeGuard(limit_minutes=2)
        self.assertEqual(guard.remaining_seconds(), 120)
        self.assertAlmostEqual(guard.remaining_fraction() or 0.0, 1.0)
        guard.add_seconds(30)
        self.assertEqual(guard.remaining_seconds(), 90)
        self.assertAlmostEqual(guard.remaining_fraction() or 0.0, 0.75)

    def test_restante_no_baja_de_cero(self) -> None:
        guard = PlaytimeGuard(limit_minutes=1)
        guard.add_seconds(120)
        self.assertEqual(guard.remaining_seconds(), 0)
        self.assertEqual(guard.remaining_fraction(), 0.0)

    def test_reloj_despierto_agota_el_tope_sin_hablar(self) -> None:
        now = datetime(2026, 9, 23, 12, 0, 0)

        def clock() -> datetime:
            return now

        guard = PlaytimeGuard(limit_minutes=1, today=date(2026, 9, 23), now_fn=clock)
        guard.set_running(True)
        now = datetime(2026, 9, 23, 12, 0, 30)
        self.assertEqual(guard.remaining_seconds(), 30)
        self.assertFalse(guard.is_over_limit())
        now = datetime(2026, 9, 23, 12, 1, 0)
        self.assertEqual(guard.remaining_seconds(), 0)
        self.assertTrue(guard.is_over_limit())
        self.assertTrue(guard.claim_end())
        self.assertFalse(guard.claim_end())


class TestPrivacyExtra(unittest.TestCase):
    def test_saca_texto_del_nene(self) -> None:
        extra = sanitize_notification_extra({"intent": "call_parent", "text": "quiero a mama"})
        self.assertIsNotNone(extra)
        self.assertNotIn("text", extra)
        self.assertEqual(extra["intent"], "call_parent")

    def test_saca_utterance_y_respuesta(self) -> None:
        extra = sanitize_notification_extra(
            {"intent": "crisis_cry", "utterance": "aaa", "response_given": "hola"}
        )
        self.assertIsNotNone(extra)
        self.assertNotIn("utterance", extra)
        self.assertNotIn("response_given", extra)

    def test_none_sigue_none(self) -> None:
        self.assertIsNone(sanitize_notification_extra(None))


class TestBargeIn(unittest.TestCase):
    def test_detecta_corte(self) -> None:
        self.assertTrue(is_barge_in_text("basta"))
        self.assertTrue(is_barge_in_text("para la musica"))
        self.assertTrue(is_barge_in_text("quiero a mama"))
        self.assertFalse(is_barge_in_text("quiero jugar"))


class TestMicHalfDuplex(unittest.TestCase):
    def test_cerrado_si_esta_hablando(self) -> None:
        self.assertFalse(mic_open_for_listen(True, now=10.0, echo_mute_until=0.0))

    def test_abierto_si_idle(self) -> None:
        self.assertTrue(mic_open_for_listen(False, now=10.0, echo_mute_until=0.0))

    def test_cerrado_en_cola_de_eco(self) -> None:
        self.assertFalse(mic_open_for_listen(False, now=10.0, echo_mute_until=10.3))

    def test_abre_despues_de_la_cola(self) -> None:
        self.assertTrue(mic_open_for_listen(False, now=10.5, echo_mute_until=10.3))

    def test_keywords_claros(self) -> None:
        self.assertEqual(is_clear_keyword_intent("dame un abrazo"), "hug_request")
        self.assertEqual(is_clear_keyword_intent("hagamos yoga"), "yoga_request")
        self.assertEqual(is_clear_keyword_intent("llama a mama"), "call_parent")
        self.assertEqual(is_clear_keyword_intent("como te llamas"), "identity_name")
        self.assertNotEqual(is_clear_keyword_intent("como te llamas"), "call_parent")
        self.assertIsNone(is_clear_keyword_intent("quiero a mama"))
        self.assertEqual(is_clear_keyword_intent("piedra papel o tijera"), "play_piedra_papel")
        self.assertEqual(is_clear_keyword_intent("adivina el objeto"), "play_veo_veo")
        self.assertEqual(is_clear_keyword_intent("juguemos a contar"), "play_contar")
        self.assertEqual(is_clear_keyword_intent("vamos a contar"), "play_contar")
        self.assertEqual(is_clear_keyword_intent("adiviná la sombra"), "play_sombras")
        self.assertEqual(is_clear_keyword_intent("contame un cuento"), "story_request")
        self.assertEqual(
            is_clear_keyword_intent("vamos a adivinar objetos"),
            "play_veo_veo",
        )
        self.assertEqual(
            is_clear_keyword_intent("Quiero jugar a adivinar la silueta."),
            "play_sombras",
        )
        self.assertEqual(
            is_clear_keyword_intent("¿Qué podemos jugar?"),
            "activity_offer",
        )
        self.assertEqual(is_clear_keyword_intent("quiero jugar"), "play_generic")
        self.assertEqual(is_clear_keyword_intent("Hola. Sombras."), "play_sombras")
        self.assertEqual(
            is_clear_keyword_intent("Quiero volver a jugar a contar, amigo."),
            "play_contar",
        )
        self.assertEqual(
            is_clear_keyword_intent("Tengo un poco más alegría. Quiero comer un zanguche."),
            "needs_basic",
        )
        self.assertIsNone(is_clear_keyword_intent("no quiero comer"))
        self.assertIsNone(is_clear_keyword_intent("no tengo hambre"))


class TestTtsEarlyMic(unittest.TestCase):
    def test_sintetizando_bloquea(self) -> None:
        self.assertIsNone(tts_remaining_seconds(10.0, None, speaking=True))
        self.assertTrue(tts_blocks_mic(None, is_music=False, is_story=False))

    def test_idle_remaining_cero(self) -> None:
        self.assertEqual(tts_remaining_seconds(10.0, 12.0, speaking=False), 0.0)
        self.assertFalse(tts_blocks_mic(0.0, is_music=False, is_story=False))

    def test_abre_en_punto_dos(self) -> None:
        self.assertTrue(tts_blocks_mic(0.21, is_music=False, is_story=False))
        self.assertFalse(tts_blocks_mic(0.20, is_music=False, is_story=False))
        self.assertFalse(tts_blocks_mic(0.10, is_music=False, is_story=False))

    def test_musica_y_cuento_bloquean_el_tail(self) -> None:
        self.assertTrue(tts_blocks_mic(0.10, is_music=True, is_story=False))
        self.assertTrue(tts_blocks_mic(0.10, is_music=False, is_story=True))

    def test_keep_queue_solo_conversacional(self) -> None:
        self.assertTrue(keep_audio_queue_after_tts(is_music=False, is_story=False))
        self.assertFalse(keep_audio_queue_after_tts(is_music=True, is_story=False))
        self.assertFalse(keep_audio_queue_after_tts(is_music=False, is_story=True))

    def test_tag_solo_usa_fallback_unknown(self) -> None:
        from session_policy import spoken_text_or_unknown_fallback

        self.assertEqual(
            spoken_text_or_unknown_fallback(
                "unknown", "", ["INTENTS_OFF"], "¿Me lo decís de otra forma?"
            ),
            "¿Me lo decís de otra forma?",
        )
        self.assertEqual(
            spoken_text_or_unknown_fallback(
                "unknown", "¿En qué pensás?", ["INTENTS_OFF"], "rescate"
            ),
            "¿En qué pensás?",
        )
        self.assertEqual(
            spoken_text_or_unknown_fallback("greeting", "", ["INTENTS_OFF"], "rescate"),
            "",
        )
        self.assertEqual(
            spoken_text_or_unknown_fallback(
                "unknown", "", ["PLAY_GAME"], "rescate"
            ),
            "",
        )


class TestSpokenAfterPlayGame(unittest.TestCase):
    def test_dice_la_frase_del_modelo_y_despues_abre_el_juego(self) -> None:
        from session_policy import spoken_after_play_game

        self.assertEqual(
            spoken_after_play_game(
                "¡Uf! ¡Qué feo! ¿Querés que juguemos a buscar sombras?",
                {"response": "¡Adiviná la sombra! Mirá.", "game_started": True},
                [{"action": "PLAY_GAME", "param": "sombras"}],
            ),
            "¡Uf! ¡Qué feo! ¿Querés que juguemos a buscar sombras? ¡Adiviná la sombra! Mirá.",
        )
        self.assertEqual(
            spoken_after_play_game(
                "",
                {"response": "¡Adiviná la sombra! Mirá.", "game_started": True},
                [{"action": "PLAY_GAME", "param": "sombras"}],
            ),
            "¡Adiviná la sombra! Mirá.",
        )
        self.assertEqual(
            spoken_after_play_game("hola", {"response": ""}, []),
            "hola",
        )
        self.assertEqual(
            spoken_after_play_game(
                "¿Jugamos a contar después?",
                {"response": "¿Jugamos a contar después? [PLAY_GAME:contar]"},
                [{"action": "PLAY_GAME", "param": "contar"}],
            ),
            "¿Jugamos a contar después?",
        )


class TestWeakSpeechSttGate(unittest.TestCase):
    def test_menos_de_150ms_salta_stt(self) -> None:
        from session_policy import skip_stt_for_weak_speech

        self.assertTrue(skip_stt_for_weak_speech(0.14))
        self.assertFalse(skip_stt_for_weak_speech(0.15))
        self.assertFalse(skip_stt_for_weak_speech(0.40))

    def test_no_speech_alto_es_baja_confianza(self) -> None:
        from session_policy import stt_is_low_confidence

        self.assertTrue(
            stt_is_low_confidence(
                "Qué pasa.", avg_logprob=-0.2, no_speech_prob=0.91
            )
        )
        self.assertFalse(
            stt_is_low_confidence(
                "Qué pasa.", avg_logprob=-0.2, no_speech_prob=0.90
            )
        )
        self.assertFalse(
            stt_is_low_confidence(
                "Qué pasa.", avg_logprob=-0.2, no_speech_prob=0.76
            )
        )

    def test_logprob_bajo_es_baja_confianza(self) -> None:
        from session_policy import stt_is_low_confidence

        self.assertTrue(
            stt_is_low_confidence("Hola.", avg_logprob=-1.21, no_speech_prob=0.1)
        )
        self.assertFalse(
            stt_is_low_confidence("Hola.", avg_logprob=-1.20, no_speech_prob=0.1)
        )
        self.assertFalse(
            stt_is_low_confidence("Hola.", avg_logprob=-0.91, no_speech_prob=0.1)
        )

    def test_texto_vacio_no_marca_confianza(self) -> None:
        from session_policy import stt_is_low_confidence

        self.assertFalse(
            stt_is_low_confidence("", avg_logprob=-2.0, no_speech_prob=0.99)
        )


class TestGreetingFarewellExact(unittest.TestCase):
    def test_saludos_cortos(self) -> None:
        for phrase in (
            "hola",
            "Hola.",
            "hola teo",
            "holi",
            "holis",
            "hey",
            "que tal",
            "buenos dias teo",
            "buenas tardes",
        ):
            self.assertEqual(is_clear_keyword_intent(phrase), "greeting", phrase)

    def test_despedidas_cortas(self) -> None:
        for phrase in ("chau", "Chau!", "adios teo", "nos vemos", "me voy", "hasta luego"):
            self.assertEqual(is_clear_keyword_intent(phrase), "farewell", phrase)

    def test_hola_con_nombre_no_es_saludo(self) -> None:
        self.assertIsNone(is_clear_keyword_intent("hola me llamo tomas"))

    def test_hola_como_va_no_es_saludo(self) -> None:
        self.assertIsNone(is_clear_keyword_intent("hola como va"))

    def test_hey_hola_no_es_saludo_exacto(self) -> None:
        self.assertIsNone(is_clear_keyword_intent("hey hola"))


class TestSingleWordTranscripts(unittest.TestCase):
    def test_session_policy_ya_no_filtra_palabras_cortas(self) -> None:
        import session_policy

        self.assertFalse(hasattr(session_policy, "utterance_too_thin"))

    def test_workers_deja_pasar_una_palabra(self) -> None:
        src = Path(__file__).resolve().parent.joinpath("workers.py").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("Muy corto para una respuesta", src)
        self.assertNotIn("utterance_too_thin(raw_text)", src)

    def test_oracion_larga_no_es_basura(self) -> None:
        from session_policy import stt_looks_like_garbage

        koala = (
            "Ojalá en mi colegio hubiese koalas porque a mí me gustan mucho."
        )
        salad = (
            "no no tengo ganas halaga treinta segundos west caemos "
            "escuchando del miraflores"
        )
        self.assertFalse(stt_looks_like_garbage(koala))
        self.assertFalse(stt_looks_like_garbage(salad))
        self.assertFalse(stt_looks_like_garbage("quiero que hagamos un pozo"))
        workers = Path(__file__).resolve().parent / "workers.py"
        src = workers.read_text(encoding="utf-8")
        self.assertNotIn("stt_looks_like_garbage(", src)


class TestShouldAllowLlm(unittest.TestCase):
    def test_unknown_va_al_llm(self) -> None:
        from session_policy import should_allow_llm

        self.assertTrue(should_allow_llm("unknown"))

    def test_needs_basic_no_va_al_llm(self) -> None:
        from session_policy import should_allow_llm

        self.assertFalse(should_allow_llm("needs_basic"))

    def test_keyword_matcheado_no_pregunta_al_llm(self) -> None:
        from session_policy import should_allow_llm

        for name in (
            "greeting",
            "farewell",
            "play_veo_veo",
            "identity_name",
            "call_parent",
        ):
            self.assertFalse(should_allow_llm(name), name)

    def test_cuento_y_musica_van_al_llm(self) -> None:
        from session_policy import should_allow_llm

        self.assertTrue(should_allow_llm("story_request"))
        self.assertTrue(should_allow_llm("song_request"))

    def test_unknown_aunque_el_stt_sea_largo_va_al_llm(self) -> None:
        from session_policy import should_allow_llm

        self.assertTrue(should_allow_llm("unknown"))
        self.assertTrue(should_allow_llm("unknown", garbage_stt=True))

    def test_cuento_reflect_si(self) -> None:
        from session_policy import should_allow_llm

        self.assertTrue(should_allow_llm("story_reflect_answer", story_reflect=True))


class TestTelemetryRange(unittest.TestCase):
    def test_suma_varios_dias(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            d1 = date(2026, 8, 1)
            d2 = date(2026, 8, 2)
            for day, n in ((d1, 3), (d2, 5)):
                payload = {
                    "date": day.isoformat(),
                    "summary": {
                        "total_interactions": n,
                        "total_duration_s": n * 10.0,
                        "pillar_counts": {"cognitivo": n},
                        "crisis_count": 0,
                        "games_played": 1,
                        "routines_completed": 0,
                        "new_words_today": 1,
                    },
                    "events": [],
                }
                (root / f"telemetry_{day.isoformat()}.json").write_text(
                    __import__("json").dumps(payload), encoding="utf-8"
                )
            summary = telemetry_range_summary(root, d1, d2)
            self.assertEqual(summary["total_interactions"], 8)
            self.assertEqual(summary["pillar_counts"]["cognitivo"], 8)
            self.assertEqual(summary["days"], 2)
            self.assertEqual(len(summary["daily"]), 2)
            self.assertEqual(summary["daily"][0]["interactions"], 3)
            self.assertEqual(summary["daily"][1]["interactions"], 5)


class TestAgentsMdRuteo(unittest.TestCase):
    def test_regla_unknown_siempre_llm_y_match_es_intent(self) -> None:
        md = (Path(__file__).resolve().parent / "Agents.md").read_text(encoding="utf-8")
        folded = md.lower()
        self.assertIn("sin match", folded)
        self.assertIn("siempre", folded)
        self.assertIn("preguntar al llm", folded)
        self.assertIn("solo si hay match", folded)
        self.assertIn("respuesta enlatada", folded)


if __name__ == "__main__":
    unittest.main()
