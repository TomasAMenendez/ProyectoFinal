"""Pictograma opcional de una rutina: contrato, coincidencias y pantalla."""
from __future__ import annotations

import tempfile
import unittest
from datetime import datetime
from pathlib import Path

from PIL import Image

from eye_render import EyeAnimator, compose_contained_image
from pictograms import resolve_pictogram
from routines import Routine, RoutineScheduler, speak_routine_cues


_BG = (26, 26, 46)


class ResolveTests(unittest.TestCase):
    def test_ids_del_catalogo_apuntan_a_un_png(self) -> None:
        for pictogram_id in ("banio", "cepillar_dientes", "comer", "jardin", "medico"):
            path = resolve_pictogram(pictogram_id)
            self.assertIsNotNone(path)
            assert path is not None
            self.assertTrue(path.is_file())
            self.assertEqual(path.suffix, ".png")

    def test_ausente_o_desconocido_no_resuelve(self) -> None:
        self.assertIsNone(resolve_pictogram(None))
        self.assertIsNone(resolve_pictogram(""))
        self.assertIsNone(resolve_pictogram("abrazo"))
        self.assertIsNone(resolve_pictogram("docs/ui-ux/referencias/pictogramas/comer"))


class SchedulerTests(unittest.TestCase):
    def test_rutina_antigua_no_exige_pictograma(self) -> None:
        routine = Routine({"id": "vieja", "name": "Juego", "time": "09:00", "enabled": True})
        self.assertIsNone(routine.pictogram_id)

    def test_varias_rutinas_conservan_su_id(self) -> None:
        now = datetime(2026, 10, 14, 12, 30, 10)
        scheduler = RoutineScheduler(Path(tempfile.gettempdir()) / "rutinas-inexistentes.json")
        scheduler.routines = [
            Routine({
                "id": "comer",
                "name": "Comer",
                "time": "12:30",
                "reminder_message": "A comer",
                "pictogram_id": "comer",
                "pre_reminder_minutes": 5,
                "enabled": True,
            }),
            Routine({
                "id": "jardin",
                "name": "Jardín",
                "time": "12:30",
                "reminder_message": "Al jardín",
                "pictogram_id": "jardin",
                "pre_reminder_minutes": 5,
                "enabled": True,
            }),
            Routine({
                "id": "sueno",
                "name": "Dormir",
                "time": "12:35",
                "reminder_message": "A dormir",
                "pictogram_id": "",
                "transition_from": "juego",
                "transition_to": "calma",
                "pre_reminder_minutes": 10,
                "enabled": True,
            }),
        ]
        cues = scheduler.check_pending(now)
        by_id = {cue.routine_id: cue for cue in cues}
        self.assertEqual(by_id["comer"].kind, "main")
        self.assertEqual(by_id["comer"].pictogram_id, "comer")
        self.assertEqual(by_id["comer"].message, "A comer")
        self.assertEqual(by_id["jardin"].pictogram_id, "jardin")
        self.assertEqual(by_id["sueno"].kind, "pre")
        self.assertIsNone(by_id["sueno"].pictogram_id)

    def test_el_preaviso_no_lleva_la_imagen_del_principal(self) -> None:
        now = datetime(2026, 10, 14, 8, 56, 0)
        routine = Routine({
            "id": "bano",
            "name": "Baño",
            "time": "09:00",
            "pictogram_id": "banio",
            "transition_from": "juego",
            "transition_to": "baño",
            "pre_reminder_minutes": 5,
            "enabled": True,
        })
        scheduler = RoutineScheduler(Path(tempfile.gettempdir()) / "rutinas-inexistentes-2.json")
        scheduler.routines = [routine]
        cues = scheduler.check_pending(now)
        self.assertEqual(len(cues), 1)
        self.assertEqual(cues[0].kind, "pre")
        self.assertIsNone(cues[0].pictogram_id)


class _Speech:
    def __init__(self, accept: bool = True) -> None:
        self.calls: list[dict] = []
        self.accept = accept

    def speak(self, text, on_playback_start=None, on_playback_cancel=None):
        self.calls.append({
            "text": text,
            "start": on_playback_start,
            "cancel": on_playback_cancel,
        })
        return self.accept


class _Eyes:
    def __init__(self) -> None:
        self.shown: list[str | None] = []
        self.dismissed: list[int] = []
        self.visible: str | None = None
        self.hug: str | None = None
        self._generation = 0

    def set_pictogram(self, name: str | None) -> None:
        self.hug = name

    def show_routine_pictogram(self, path: str | None, hold_seconds: float = 8.0) -> int:
        del hold_seconds
        self._generation += 1
        self.visible = path
        self.shown.append(path)
        return self._generation

    def dismiss_routine_pictogram(self, generation: int | None = None) -> None:
        self.dismissed.append(-1 if generation is None else generation)
        if generation is None or generation == self._generation:
            self.visible = None


class DispatchTests(unittest.TestCase):
    def test_la_imagen_espera_al_inicio_del_audio(self) -> None:
        from routines import RoutineCue

        speech = _Speech()
        eyes = _Eyes()
        speak_routine_cues(speech, eyes, [
            RoutineCue("comer", "main", "A comer", "comer"),
        ])
        self.assertEqual(eyes.shown, [])
        speech.calls[0]["start"]()
        self.assertTrue(str(eyes.shown[0]).endswith("comer.png"))

    def test_cancelar_el_primero_no_apaga_el_segundo(self) -> None:
        from routines import RoutineCue

        speech = _Speech()
        eyes = _Eyes()
        speak_routine_cues(speech, eyes, [
            RoutineCue("comer", "main", "A comer", "comer"),
            RoutineCue("jardin", "main", "Al jardín", "jardin"),
        ])
        speech.calls[0]["start"]()
        speech.calls[1]["start"]()
        speech.calls[0]["cancel"]()
        self.assertEqual(eyes.dismissed, [1])
        self.assertTrue(str(eyes.shown[1]).endswith("jardin.png"))

    def test_un_principal_sin_imagen_retira_la_anterior(self) -> None:
        from routines import RoutineCue
        from unittest.mock import patch

        speech = _Speech()
        eyes = _Eyes()
        eyes.set_pictogram("abrazo")
        speak_routine_cues(speech, eyes, [
            RoutineCue("comer", "main", "A comer", "comer"),
            RoutineCue("sueno", "main", "A dormir", None),
            RoutineCue("bano", "pre", "En cinco minutos", None),
            RoutineCue("otro", "main", "Algo", "desconocido"),
            RoutineCue("medico", "main", "Al médico", "medico"),
        ])
        speech.calls[0]["start"]()
        self.assertTrue(str(eyes.visible).endswith("comer.png"))
        speech.calls[1]["start"]()
        self.assertIsNone(eyes.visible)
        self.assertEqual(eyes.hug, "abrazo")
        self.assertIsNone(speech.calls[2]["start"])
        speech.calls[0]["start"]()
        self.assertTrue(str(eyes.visible).endswith("comer.png"))
        speech.calls[3]["start"]()
        self.assertIsNone(eyes.visible)
        self.assertEqual(eyes.hug, "abrazo")
        speech.calls[0]["start"]()
        with patch("pictograms.resolve_pictogram", return_value=None):
            speech.calls[4]["start"]()
        self.assertIsNone(eyes.visible)
        self.assertEqual(eyes.hug, "abrazo")


class RenderTests(unittest.TestCase):
    def test_contiene_sin_estirar_ni_recortar(self) -> None:
        source = Image.new("RGB", (100, 40), (220, 10, 10))
        frame = compose_contained_image(source, 80, 80, 1.0)
        self.assertEqual(frame.size, (80, 80))
        self.assertEqual(frame.getpixel((40, 40)), (220, 10, 10))
        self.assertEqual(frame.getpixel((4, 4)), _BG)
        self.assertEqual(frame.getpixel((40, 4)), _BG)

    def test_png_real_y_regreso_a_ojos(self) -> None:
        path = resolve_pictogram("comer")
        self.assertIsNotNone(path)
        assert path is not None
        source = Image.open(path).convert("RGB")
        animator = EyeAnimator(sprites_dir=Path(tempfile.mkdtemp()))
        animator.set_expression("neutral")
        animator.show_routine_image(source, hold_seconds=8)
        shown = animator.render(240, 240)
        self.assertEqual(shown.size, (240, 240))
        self.assertNotEqual(shown.getpixel((120, 120)), _BG)
        eyes = animator.render(240, 240)
        animator._routine_until = 0
        animator.tick()
        restored = animator.render(240, 240)
        self.assertNotEqual(list(restored.getdata()), list(shown.getdata()))
        out_dir = Path(__file__).resolve().parents[1] / "docs" / "ui-ux" / "validacion"
        out_dir.mkdir(parents=True, exist_ok=True)
        shown.save(out_dir / "peluche-pictograma-comer.png")
        restored.save(out_dir / "peluche-ojos-despues.png")
        self.assertNotEqual(eyes.size, (0, 0))

    def test_un_aviso_posterior_sobrevive_al_temporizador_anterior(self) -> None:
        animator = EyeAnimator(sprites_dir=Path(tempfile.mkdtemp()))
        first = Image.new("RGB", (20, 20), (255, 0, 0))
        second = Image.new("RGB", (20, 20), (0, 180, 0))
        generation = animator.show_routine_image(first, hold_seconds=8)
        animator.show_routine_image(second, hold_seconds=8)
        animator.dismiss_routine_image(generation)
        frame = animator.render(40, 40)
        self.assertEqual(frame.getpixel((20, 20)), (0, 180, 0))

    def test_dormir_vuelve_a_los_ojos(self) -> None:
        animator = EyeAnimator(sprites_dir=Path(tempfile.mkdtemp()))
        animator.show_routine_image(Image.new("RGB", (12, 12), (1, 2, 3)), hold_seconds=8)
        animator.set_expression("dormido")
        self.assertFalse(animator.routine_image_showing)

    def test_retirar_la_rutina_conserva_abrazo_y_expresion(self) -> None:
        animator = EyeAnimator(sprites_dir=Path(tempfile.mkdtemp()))
        animator.set_expression("neutral")
        animator.set_pictogram("abrazo")
        animator.show_routine_image(Image.new("RGB", (12, 12), (10, 10, 200)), 8)
        animator.dismiss_routine_image()
        self.assertFalse(animator.routine_image_showing)
        self.assertEqual(animator.pictogram, "abrazo")
        self.assertEqual(animator.expression, "neutral")


class SpeechWorkerPictogramTests(unittest.TestCase):
    def test_la_imagen_sigue_a_la_salida_y_un_fallo_no_pisa_el_siguiente(self) -> None:
        import time
        from unittest.mock import patch

        import numpy as np

        from routines import RoutineCue
        from workers import SpeechWorker

        speech = _Speech()
        eyes = _Eyes()
        speak_routine_cues(speech, eyes, [
            RoutineCue("comer", "main", "A comer", "comer"),
            RoutineCue("jardin", "main", "Al jardín", "jardin"),
        ])
        worker = SpeechWorker(output_device_index=0)
        order: list[str] = []
        attempts = {"opened": 0}

        class _Stream:
            def __init__(self, **kwargs):
                order.append("construct")
                if kwargs.get("dtype") == "float32":
                    raise RuntimeError("la salida no abre")
                self.callback = kwargs["callback"]
                self.channels = kwargs.get("channels", 1)
                self.dtype = kwargs.get("dtype", "float32")
                self.fail_after = False

            def __enter__(self):
                order.append("opened")
                attempts["opened"] += 1
                self.fail_after = attempts["opened"] == 1
                frames = 256
                dtype = np.int16 if self.dtype == "int16" else np.float32
                outdata = np.zeros((frames, self.channels), dtype=dtype)
                try:
                    while True:
                        self.callback(outdata, frames, None, None)
                except Exception:
                    pass
                return self

            def __exit__(self, exc_type, exc, tb):
                if self.fail_after:
                    raise RuntimeError("falla despues de empezar")
                return False

        audio = np.zeros(400, dtype=np.float32)

        def play(_text: str) -> None:
            worker._play_wav_via_output_stream(audio, 22050, announce_start=True)

        def wrap(callback):
            def inner() -> None:
                order.append("shown")
                callback()
            return inner

        with patch.object(worker, "_speak_queued_text_body", play), patch.object(
            worker, "_find_supported_output_config", return_value=(22050, "float32", 1)
        ), patch("workers.sd.OutputStream", side_effect=_Stream), patch.object(worker, "_log"):
            self.assertEqual(eyes.shown, [])
            started = time.monotonic()
            worker._speak_queued_text(
                "A comer",
                on_start=wrap(speech.calls[0]["start"]),
                on_cancel=speech.calls[0]["cancel"],
            )
            self.assertLess(order.index("opened"), order.index("shown"))
            self.assertEqual(order.count("shown"), 2)
            self.assertTrue(str(eyes.visible).endswith("comer.png"))
            self.assertLess(time.monotonic() - started, 8)
            worker.interrupt_playback()
            self.assertTrue(str(eyes.visible).endswith("comer.png"))
            worker._speak_queued_text(
                "Al jardín",
                on_start=wrap(speech.calls[1]["start"]),
                on_cancel=speech.calls[1]["cancel"],
            )
            speech.calls[0]["cancel"]()

        self.assertTrue(str(eyes.visible).endswith("jardin.png"))
        self.assertIn(1, eyes.dismissed)

    def test_si_la_salida_no_abre_no_hay_imagen(self) -> None:
        from unittest.mock import patch

        import numpy as np

        from routines import RoutineCue
        from workers import SpeechWorker

        speech = _Speech()
        eyes = _Eyes()
        speak_routine_cues(speech, eyes, [
            RoutineCue("comer", "main", "A comer", "comer"),
            RoutineCue("sueno", "main", "A dormir", None),
        ])
        speech.calls[0]["start"]()
        worker = SpeechWorker(output_device_index=0)

        def no_abre(_text: str) -> None:
            worker._play_wav_via_output_stream(
                np.zeros(200, dtype=np.float32), 22050, announce_start=True
            )

        with patch.object(worker, "_speak_queued_text_body", no_abre), patch(
            "workers.sd.OutputStream", side_effect=RuntimeError("sin dispositivo")
        ), patch.object(worker, "_log"):
            with self.assertRaises(RuntimeError):
                worker._speak_queued_text(
                    "A dormir",
                    on_start=speech.calls[1]["start"],
                    on_cancel=speech.calls[1]["cancel"],
                )

        self.assertTrue(str(eyes.visible).endswith("comer.png"))
        self.assertEqual(eyes.dismissed, [])

    def test_interrumpir_en_curso_no_quita_un_aviso_posterior(self) -> None:
        from routines import RoutineCue
        from workers import SpeechWorker

        speech = _Speech()
        eyes = _Eyes()
        speak_routine_cues(speech, eyes, [
            RoutineCue("comer", "main", "A comer", "comer"),
            RoutineCue("jardin", "main", "Al jardín", "jardin"),
        ])
        worker = SpeechWorker(output_device_index=0)
        worker._playback_start_hook = speech.calls[0]["start"]
        worker._playback_start_gen = worker._playback_generation
        worker._active_playback_cancel = speech.calls[0]["cancel"]
        worker._note_playback_started()
        self.assertTrue(str(eyes.visible).endswith("comer.png"))
        worker.interrupt_playback()
        self.assertIsNone(eyes.visible)
        worker._playback_start_hook = speech.calls[1]["start"]
        worker._playback_start_gen = worker._playback_generation
        worker._active_playback_cancel = speech.calls[1]["cancel"]
        worker._note_playback_started()
        speech.calls[0]["cancel"]()
        self.assertTrue(str(eyes.visible).endswith("jardin.png"))


if __name__ == "__main__":
    unittest.main()
