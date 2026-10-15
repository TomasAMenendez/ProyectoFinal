"""Regresiones de contratos parentales, sin modelos, dispositivos ni secretos."""
from __future__ import annotations

import ast
import json
import secrets
import tempfile
import threading
import time
import unittest
from datetime import date, datetime, timedelta
from http.server import BaseHTTPRequestHandler
from pathlib import Path
from types import SimpleNamespace
from typing import Any, Callable
from urllib.parse import parse_qs, quote, urlparse
from unittest.mock import patch

from routines import RoutineScheduler
from session_policy import PlaytimeGuard
from sleep_state import SleepState
from story_engine import StoryEngine
from story_library import StoryLibrary


ROOT = Path(__file__).resolve().parent


def isolated_classes(filename: str, classes: dict[str, set[str] | None], namespace: dict):
    """Ejecuta métodos reales sin ejecutar imports/arranque de hardware del módulo."""
    tree = ast.parse((ROOT / filename).read_text(encoding="utf-8-sig"), filename)
    selected: list[ast.stmt] = [ast.ImportFrom("__future__", [ast.alias("annotations")], 0)]
    for node in tree.body:
        if not isinstance(node, ast.ClassDef) or node.name not in classes:
            continue
        methods = classes[node.name]
        if methods is not None:
            node.body = [n for n in node.body if isinstance(n, ast.FunctionDef) and n.name in methods]
            node.bases = []
        selected.append(node)
    module = ast.fix_missing_locations(ast.Module(selected, type_ignores=[]))
    exec(compile(module, filename, "exec"), namespace)
    return namespace


def isolated_api(root: Path) -> dict:
    return isolated_classes("api_server.py", {"RobotState": None, "ApiRequestHandler": None}, {
        "json": json, "threading": threading, "time": time, "secrets": secrets,
        "date": date, "datetime": datetime, "Path": Path, "Any": Any, "Callable": Callable,
        "BaseHTTPRequestHandler": BaseHTTPRequestHandler, "urlparse": urlparse, "parse_qs": parse_qs,
        "APP_DIR": root, "MUSIC_DIR": root / "music", "STORIES_DIR": root / "stories",
        "SleepState": SleepState, "PlaytimeGuard": PlaytimeGuard,
        "load_or_create_token": lambda: "test-token",
        "set_profile_path": lambda _path: None,
        "load_profile": lambda: None, "is_complete": lambda _profile: False,
        "log_action": lambda *_args, **_kwargs: None,
    })


class DailyRoutineTests(unittest.TestCase):
    def test_completed_routine_is_reminded_once_on_the_next_day(self) -> None:
        with tempfile.TemporaryDirectory(dir=ROOT) as tmp:
            path = Path(tmp) / "routines.json"
            path.write_text(json.dumps({"routines": [{
                "id": "comer", "name": "Comer", "time": "12:00", "pictogram_id": "comer",
            }]}), encoding="utf-8")
            scheduler = RoutineScheduler(path)
            yesterday = datetime.now().replace(hour=12, minute=1) - timedelta(days=1)
            routine = scheduler.routines[0]
            routine._last_reminded_date = yesterday.strftime("%Y-%m-%d")
            routine._reminded_today = True
            routine._completed_today = True
            today = yesterday + timedelta(days=1)
            cues = scheduler.check_pending(today)
            self.assertEqual([cue.routine_id for cue in cues], ["comer"])
            self.assertEqual(cues[0].pictogram_id, "comer")
            self.assertEqual(scheduler.check_pending(today), [])


class StorySessionTests(unittest.TestCase):
    def test_reading_session_is_visible_before_the_first_audio_block_finishes(self) -> None:
        with tempfile.TemporaryDirectory(dir=ROOT) as tmp:
            library = StoryLibrary(Path(tmp))
            record = library.save_story("El bosque", "Había una vez un conejo. Fue al bosque y encontró amigos.")
            state = SimpleNamespace(currently_reading=None)
            methods = {
                "_invalidate_story_timer", "_publish_story_status", "_sync_story_guard",
                "stop_story_from_api", "play_story_from_api",
            }
            cls = isolated_classes("workers.py", {"AudioWorker": methods}, {"threading": threading})["AudioWorker"]
            worker = cls()
            worker._story_generation = 0
            worker._story_pipeline_active = False
            worker._story_lock = threading.Lock()
            worker._robot_state = state
            worker.story_engine = StoryEngine(library)
            worker.speech_worker = SimpleNamespace(
                stop_music=lambda: None, interrupt_playback=lambda: None, set_story_guard=lambda _active: None,
            )
            started = threading.Event()
            release = threading.Event()
            finished = threading.Event()

            def blocked_audio(_payload, generation):
                started.set()
                release.wait(2)
                finished.set()

            worker._speak_story_payload = blocked_audio
            try:
                self.assertTrue(worker.play_story_from_api(record.id))
                self.assertTrue(started.wait(1))
                self.assertEqual(state.currently_reading, {"id": record.id, "title": "El bosque"})
                worker.stop_story_from_api()
                self.assertIsNone(state.currently_reading)
                self.assertFalse(worker.story_engine.is_active)
            finally:
                release.set()
                finished.wait(2)


class SensoryPersistenceTests(unittest.TestCase):
    def test_failed_preferences_write_is_not_acknowledged_or_applied(self) -> None:
        with tempfile.TemporaryDirectory(dir=ROOT) as tmp:
            namespace = isolated_api(Path(tmp))
            path = Path(tmp) / "preferences.json"
            state = namespace["RobotState"](prefs_path=path)
            namespace["robot_state"] = state
            state.update_config({"volume_limit": 20, "brightness": 0.25})
            handler = namespace["ApiRequestHandler"].__new__(namespace["ApiRequestHandler"])
            handler._parse_json_body = lambda: {"volume_limit": 80, "brightness": 0.8}
            responses: list[tuple[int, dict]] = []
            handler._send_json = lambda body, status=200: responses.append((status, body))
            with patch.object(Path, "write_text", side_effect=OSError("sin espacio")):
                handler._handle_post_config()
            self.assertEqual(responses[0][0], 500)
            self.assertEqual(state.volume_limit, 20)
            self.assertEqual(state.brightness, 0.25)
            self.assertEqual(json.loads(path.read_text(encoding="utf-8"))["volume_limit"], 20)

    def test_volume_and_brightness_survive_restart_and_other_preference_writes(self) -> None:
        with tempfile.TemporaryDirectory(dir=ROOT) as tmp:
            namespace = isolated_api(Path(tmp))
            cls = namespace["RobotState"]
            path = Path(tmp) / "preferences.json"
            first = cls(prefs_path=path)
            first.update_config({"volume_limit": 20, "brightness": 0.25})
            first.update_config({"belly_wake_enabled": False})
            restarted = cls(prefs_path=path)
            self.assertEqual(restarted.to_dict()["volume_limit"], 20)
            self.assertEqual(restarted.to_dict()["brightness"], 0.25)
            self.assertFalse(restarted.sleep.belly_wake_enabled)

    def test_api_bootstrap_preserves_loaded_sensory_settings_and_applies_them(self) -> None:
        for class_name in ("EyeModeApp", "EdgeAiDesktopApp"):
            with self.subTest(mode=class_name):
                state = SimpleNamespace(volume_limit=20, brightness=0.25, playtime_limit_minutes=0)
                volumes: list[int] = []
                brightness: list[float] = []
                variable = SimpleNamespace(get=lambda: 100, set=lambda value: None)
                app = SimpleNamespace(
                    telemetry=None, routine_scheduler=None, audio_worker=None, api_server=object(),
                    speech_worker=SimpleNamespace(set_volume_limit=volumes.append),
                    _eye_display=SimpleNamespace(set_brightness=brightness.append),
                    _volume_limit=100, _brightness=1.0, _volume_var=variable,
                    after=lambda _ms, callback: callback(), _append_log=lambda _line: None,
                )
                namespace = isolated_classes("app.py", {class_name: {"_start_api_server"}}, {
                    "robot_state": state,
                    "ApiServer": lambda: SimpleNamespace(start=lambda: None),
                    "_bind_playback_callbacks": lambda _app: None,
                    "_start_wifi_ble": lambda _app: None,
                    "log_action": lambda *_args, **_kwargs: None,
                })
                namespace[class_name]._start_api_server(app)
                self.assertEqual(state.volume_limit, 20)
                self.assertEqual(state.brightness, 0.25)
                self.assertEqual(volumes, [20])
                self.assertEqual(brightness, [0.25])


class MusicFilenameTests(unittest.TestCase):
    def test_delete_decodes_a_single_filename_segment_and_preserves_other_files(self) -> None:
        with tempfile.TemporaryDirectory(dir=ROOT) as tmp:
            namespace = isolated_api(Path(tmp))
            music = Path(tmp) / "music"
            music.mkdir()
            state = namespace["RobotState"](prefs_path=Path(tmp) / "preferences.json")
            namespace["robot_state"] = state
            for filename in ("cancion#1.mp3", "canción + 50%.mp3"):
                with self.subTest(filename=filename):
                    (music / filename).write_bytes(b"audio")
                    (music / "cancion").write_bytes(b"preservar")
                    handler = namespace["ApiRequestHandler"].__new__(namespace["ApiRequestHandler"])
                    handler.path = "/api/music/" + quote(filename, safe="")
                    handler.headers = {"X-Robot-Token": "test-token"}
                    responses: list[tuple[int, dict]] = []
                    handler._send_json = lambda body, status=200: responses.append((status, body))
                    handler.do_DELETE()
                    self.assertEqual(responses[0][0], 200, responses)
                    self.assertFalse((music / filename).exists())
                    self.assertEqual((music / "cancion").read_bytes(), b"preservar")


if __name__ == "__main__":
    unittest.main()
