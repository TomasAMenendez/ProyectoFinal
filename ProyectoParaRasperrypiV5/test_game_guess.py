"""Juegos de adivinar: contar, sombras y adivina el objeto."""
from __future__ import annotations

import unittest
from dataclasses import replace

from game_catalog import (
    answer_accepted,
    first_spoken_answer,
    load_catalog,
    parse_adivina,
    parse_contar,
    parse_sombras,
    resolve_media,
)
from game_engine import (
    GameEngine,
    PiedraPapelTijeraSession,
    _ENCOURAGEMENT_PHRASES,
    apply_play_game_tag,
    resolve_play_game_param,
)
from game_guess import GuessSession


class TestAnswerAccepted(unittest.TestCase):
    def test_numero_y_palabra(self) -> None:
        answers = ("2", "2 bananas", "dos bananas", "dos")
        self.assertTrue(answer_accepted("dos", answers))
        self.assertTrue(answer_accepted("hay 2", answers))
        self.assertTrue(answer_accepted("2 bananas", answers))
        self.assertFalse(answer_accepted("tres", answers))

    def test_sinonimo_sombra(self) -> None:
        answers = ("perro", "perrito", "cachorro")
        self.assertTrue(answer_accepted("un perrito", answers))
        self.assertTrue(answer_accepted("perro", answers))
        self.assertFalse(answer_accepted("es", answers))

    def test_contar_exige_el_numero(self) -> None:
        answers = ("2", "2 bananas", "dos bananas", "dos")
        self.assertFalse(answer_accepted("bananas", answers))
        self.assertFalse(answer_accepted("hay 3 bananas", answers))
        self.assertTrue(answer_accepted("dos", answers))

    def test_primera_respuesta_para_decir(self) -> None:
        self.assertEqual(first_spoken_answer(("corazón", "corazon")), "corazón")


class TestCatalogParse(unittest.TestCase):
    def test_contar_pregunta(self) -> None:
        items = parse_contar(
            [{"imagen_archivo": "Contar_2-Bananas.png", "pregunta": "¿Cuántas bananas hay?", "respuestas": ["2", "dos"]}]
        )
        self.assertEqual(items[0].spoken_open, "¿Cuántas bananas hay?")
        self.assertEqual(items[0].follow_hints, ())

    def test_sombras_abre_sin_pista(self) -> None:
        items = parse_sombras(
            {
                "juego_siluetas": [
                    {
                        "id": "perro",
                        "silueta_archivo": "Silueta_Perro.png",
                        "imagen_archivo": "Imagen_Perro.png",
                        "respuestas": ["perro"],
                        "pistas": ["mueve la cola", "hace guau"],
                    }
                ]
            }
        )
        self.assertIn("sombra", items[0].spoken_open.lower())
        self.assertEqual(items[0].follow_hints, ("mueve la cola", "hace guau"))

    def test_adivina_primera_pista_es_apertura(self) -> None:
        items = parse_adivina(
            [
                {
                    "id": "manzana",
                    "imagen_archivo": "Imagen_Manzana.png",
                    "respuestas": ["manzana"],
                    "pistas": ["pista uno", "pista dos", "pista tres"],
                }
            ]
        )
        self.assertEqual(items[0].spoken_open, "pista uno")
        self.assertEqual(items[0].follow_hints, ("pista dos", "pista tres"))
        self.assertIsNone(items[0].persist_image)

    def test_catalogos_reales_cargan(self) -> None:
        self.assertGreaterEqual(len(load_catalog("contar")), 10)
        self.assertGreaterEqual(len(load_catalog("sombras")), 10)
        self.assertGreaterEqual(len(load_catalog("adivina")), 10)

    def test_imagenes_del_catalogo_existen(self) -> None:
        missing: list[str] = []
        for kind in ("contar", "sombras", "adivina"):
            for item in load_catalog(kind):
                for name in (item.persist_image, item.reveal_image):
                    if not name:
                        continue
                    if resolve_media(name) is None:
                        missing.append(f"{kind}:{name}")
        self.assertEqual(missing, [])


class TestGuessSession(unittest.TestCase):
    def _item(self, **overrides):
        from game_catalog import GuessItem

        base = GuessItem(
            kind="adivina",
            item_id="manzana",
            answers=("manzana",),
            spoken_open="Es la fruta de Blancanieves.",
            follow_hints=("Es roja y empieza con M.", "Crece en los árboles."),
            persist_image=None,
            reveal_image="Imagen_Manzana.png",
        )
        return replace(base, **overrides) if overrides else base

    def test_tres_fallos_dice_la_primera_respuesta(self) -> None:
        session = GuessSession(kind="adivina", item=self._item())
        session.process_input("auto")
        session.process_input("casa")
        last = session.process_input("sol")
        self.assertTrue(last.game_over)
        self.assertIn("¡Era manzana!", last.text)
        self.assertIn("próxima", last.text.lower())
        self.assertTrue(last.lcd_clear_after_speech)

    def test_acierta_con_sinonimo_en_intento_dos(self) -> None:
        item = self._item(answers=("corazón", "corazon", "corazoncito"))
        session = GuessSession(kind="adivina", item=item)
        session.process_input("auto")
        win = session.process_input("corazoncito")
        self.assertTrue(win.game_over)
        self.assertIn("Era corazón", win.text)

    def test_pistas_en_orden(self) -> None:
        session = GuessSession(kind="sombras", item=self._item(
            kind="sombras",
            spoken_open="Mirá la sombra.",
            follow_hints=("pista A", "pista B"),
            persist_image="Silueta_Perro.png",
            reveal_image="Imagen_Perro.png",
            answers=("perro",),
        ))
        first = session.process_input("gato")
        self.assertIn("pista A", first.text)
        self.assertFalse(first.game_over)
        second = session.process_input("pez")
        self.assertIn("pista B", second.text)
        third = session.process_input("oso")
        self.assertTrue(third.game_over)
        self.assertIn("¡Era perro!", third.text)

    def test_fallo_enmarca_la_pista(self) -> None:
        session = GuessSession(
            kind="sombras",
            item=self._item(
                kind="sombras",
                spoken_open="Mirá la sombra.",
                follow_hints=("pista A", "pista B"),
                persist_image="Silueta_Perro.png",
                reveal_image="Imagen_Perro.png",
                answers=("perro",),
            ),
            pick_frame=lambda frames: frames[0],
        )
        first = session.process_input("gato")
        self.assertEqual(first.text, "No era esa. Te doy una pista: pista A")
        second = GuessSession(
            kind="adivina",
            item=self._item(follow_hints=("Es roja y empieza con M.", "Crece en los árboles.")),
            pick_frame=lambda frames: frames[3],
        ).process_input("auto")
        self.assertEqual(
            second.text,
            "Probá de nuevo. Te doy una pista: Es roja y empieza con M.",
        )

    def test_contar_sin_pistas_anima_hasta_el_limite(self) -> None:
        session = GuessSession(
            kind="contar",
            item=self._item(
                kind="contar",
                spoken_open="¿Cuántas bananas hay?",
                follow_hints=(),
                answers=("2", "dos"),
                persist_image="Contar_2-Bananas.png",
                reveal_image="Contar_2-Bananas.png",
            ),
        )
        miss = session.process_input("nueve")
        self.assertFalse(miss.game_over)
        self.assertIn(miss.text, _ENCOURAGEMENT_PHRASES)
        joined = " ".join(_ENCOURAGEMENT_PHRASES)
        self.assertNotEqual(miss.text, joined)

    def test_exit_basta(self) -> None:
        session = GuessSession(kind="adivina", item=self._item())
        out = session.process_input("basta")
        self.assertTrue(out.game_over)

    def test_noche_no_sale(self) -> None:
        session = GuessSession(kind="adivina", item=self._item(answers=("noche",), spoken_open="x", follow_hints=("a", "b")))
        out = session.process_input("sol")
        self.assertFalse(out.game_over)


class TestEngineStart(unittest.TestCase):
    def test_veo_veo_abre_adivina(self) -> None:
        engine = GameEngine()
        payload = engine.process_or_passthrough(
            "juguemos veo veo",
            {"intent_name": "play_veo_veo", "confidence": 0.9, "response": ""},
        )
        self.assertTrue(payload.get("game_started"))
        self.assertTrue(engine.is_active)
        self.assertEqual(engine.game_type, "adivina")
        self.assertIn("objeto", (payload.get("response") or "").lower())

    def test_contar_intent(self) -> None:
        engine = GameEngine()
        payload = engine.process_or_passthrough(
            "juguemos a contar",
            {"intent_name": "play_contar", "confidence": 1, "response": ""},
        )
        self.assertTrue(engine.is_active)
        self.assertEqual(engine.game_type, "contar")
        self.assertIn("cuánt", (payload.get("response") or "").lower())
        self.assertTrue(str(payload.get("lcd_image") or "").endswith(".png"))
        self.assertFalse(payload.get("lcd_clear_after_speech"))

    def test_sombras_intent(self) -> None:
        engine = GameEngine()
        payload = engine.process_or_passthrough(
            "adiviná la sombra",
            {"intent_name": "play_sombras", "confidence": 1, "response": ""},
        )
        self.assertTrue(engine.is_active)
        self.assertEqual(engine.game_type, "sombras")
        self.assertTrue(str(payload.get("lcd_image") or "").endswith(".png"))
        self.assertFalse(payload.get("lcd_clear_after_speech"))

    def test_play_game_tag_arranca_sombras(self) -> None:
        self.assertEqual(resolve_play_game_param("sombras"), "sombras")
        self.assertEqual(resolve_play_game_param("silueta"), "sombras")
        self.assertEqual(resolve_play_game_param("contar"), "contar")
        self.assertEqual(resolve_play_game_param("adivina"), "adivina")
        self.assertEqual(resolve_play_game_param("piedra"), "piedra_papel_tijera")
        engine = GameEngine()
        payload: dict = {"intent_name": "unknown", "response": "invento reglas"}
        self.assertTrue(
            apply_play_game_tag(
                engine, "sombras", payload, user_text="adiviná la sombra"
            )
        )
        self.assertTrue(engine.is_active)
        self.assertEqual(engine.game_type, "sombras")
        self.assertTrue(str(payload.get("lcd_image") or "").endswith(".png"))
        self.assertNotIn("invento", (payload.get("response") or "").lower())

    def test_play_game_sin_pedido_del_nene_no_arranca(self) -> None:
        from game_engine import child_asked_for_play_game

        self.assertFalse(child_asked_for_play_game("Hola, ¿cómo estás?", "contar"))
        self.assertFalse(child_asked_for_play_game("quiero jugar", "sombras"))
        self.assertFalse(
            child_asked_for_play_game("juguemos a contar", "sombras")
        )
        self.assertTrue(
            child_asked_for_play_game("juguemos a contar", "contar")
        )
        engine = GameEngine()
        payload: dict = {"intent_name": "llm_fallback", "response": "charla"}
        self.assertFalse(
            apply_play_game_tag(
                engine, "contar", payload, user_text="Hola Teo, ¿qué hiciste hoy?"
            )
        )
        self.assertFalse(engine.is_active)

    def test_ppt_trae_lcd_al_elegir(self) -> None:
        session = PiedraPapelTijeraSession()
        out = session.process_choice("piedra")
        self.assertIn("Yo elegí", out.text)
        self.assertTrue(out.lcd_clear_after_speech)
        self.assertTrue(out.lcd_image)
        self.assertTrue(out.lcd_image.endswith(".png"))


class TestNoInyectaPlayGame(unittest.TestCase):
    def test_no_existe_ensure_play_game_tag(self) -> None:
        import game_engine

        self.assertFalse(hasattr(game_engine, "ensure_play_game_tag"))
        self.assertFalse(hasattr(game_engine, "mentioned_game_kinds"))

    def test_workers_no_inyecta(self) -> None:
        from pathlib import Path

        src = (Path(__file__).resolve().parent / "workers.py").read_text(
            encoding="utf-8"
        )
        self.assertNotIn("ensure_play_game_tag", src)

    def test_basta_no_reabre_contar(self) -> None:
        engine = GameEngine()
        engine.process_or_passthrough(
            "juguemos a contar",
            {"intent_name": "play_contar", "confidence": 1, "response": ""},
        )
        payload = engine.process_or_passthrough(
            "Basta, no quiero más.",
            {"intent_name": "unknown", "confidence": 0, "response": ""},
        )
        self.assertTrue(payload.get("game_over"))
        self.assertFalse(engine.is_active)
        self.assertFalse(apply_play_game_tag(engine, "contar", dict(payload)))
        self.assertFalse(engine.is_active)


class TestMediaResolve(unittest.TestCase):
    def test_encuentra_png_de_contar(self) -> None:
        path = resolve_media("Contar_2-Bananas.png")
        self.assertIsNotNone(path)
        self.assertTrue(path.endswith("Contar_2-Bananas.png"))


if __name__ == "__main__":
    unittest.main()
