"""API Python real con almacenamiento temporal y callbacks de hardware simulados.

No importa app.py ni workers, no carga .env y no usa datos del proyecto. Los
endpoints /api/* pertenecen al ApiRequestHandler del producto sin reemplazarlos.
Los endpoints /__fixture/* son controles exclusivos de esta prueba local.
"""
from __future__ import annotations

import argparse
import importlib.util
import json
import sys
import tempfile
import threading
from http.server import HTTPServer
from pathlib import Path
from types import SimpleNamespace
from urllib.parse import urlparse

FIXTURE_KIND = "teo-real-api-contract-v1"
TEST_TOKEN = "teo-integration-only-not-a-device-token"
BACKEND_DIR = Path(__file__).resolve().parents[2] / "ProyectoParaRasperrypiV5"


def load_real_api(temporary_root: Path):
    """Redirige incluso los inicializadores de importación antes de ejecutarlos."""
    sys.dont_write_bytecode = True
    sys.path.insert(0, str(BACKEND_DIR))
    import pairing

    # El primer RobotState creado al importar tampoco puede leer el token real.
    pairing.TOKEN_PATH = temporary_root / "pairing_token.txt"
    pairing.TOKEN_PATH.write_text(TEST_TOKEN + "\n", encoding="utf-8")
    source = BACKEND_DIR / "api_server.py"
    spec = importlib.util.spec_from_file_location("api_server", source)
    if spec is None or spec.loader is None:
        raise RuntimeError("No se encontró api_server.py")
    module = importlib.util.module_from_spec(spec)
    # SourceFileLoader lee el código original; __file__ define sus rutas de datos.
    module.__file__ = str(temporary_root / "api_server.py")
    sys.modules["api_server"] = module
    spec.loader.exec_module(module)
    if module.APP_DIR.resolve() != temporary_root.resolve():
        raise RuntimeError("La API no quedó aislada en el directorio temporal")
    return module


class RealApiFixture:
    def __init__(self, api, root: Path, seed_profile: bool):
        self.api = api
        self.root = root
        self.case_dir = root
        self.events: list[dict] = []
        self.events_lock = threading.Lock()
        self.reset(seed_profile)

    def reset(self, seed_profile: bool = False) -> None:
        self.case_dir = Path(tempfile.mkdtemp(prefix="case-", dir=self.root))
        self.api.APP_DIR = self.case_dir
        self.api.MUSIC_DIR = self.case_dir / "music"
        self.api.STORIES_DIR = self.case_dir / "stories"
        self.api.MUSIC_DIR.mkdir()
        self.api.STORIES_DIR.mkdir()
        (self.case_dir / "routines_config.json").write_text('{"routines":[]}', encoding="utf-8")
        self.reload_state()
        if seed_profile:
            self.api.save_profile(self.api.parse_payload({
                "name": "Perfil de prueba",
                "gender": "no_decir",
                "interests": "Cuentos y música",
                "costume": "oso",
                "extra": "Datos temporales de integración",
            }))

    def reload_state(self) -> None:
        from routines import RoutineScheduler
        from story_library import StoryLibrary

        state = self.api.RobotState(prefs_path=self.case_dir / "parental_prefs.json")
        state.pairing_token = TEST_TOKEN
        state.routine_scheduler = RoutineScheduler(self.case_dir / "routines_config.json")
        events: list[dict] = []
        events_lock = threading.Lock()
        playback = SimpleNamespace(
            _is_playing_music=False,
            _current_song_name=None,
            _story_guard=False,
            _pipeline_active=False,
        )
        state.speech_worker = playback
        music_dir = self.api.MUSIC_DIR
        stories_dir = self.api.STORIES_DIR

        def record(kind: str, value=None) -> None:
            with events_lock:
                events.append({"kind": kind, "value": value, "hardware": "simulated"})

        def power_changed(on: bool) -> None:
            record("power", on)
            if not on:
                playback._is_playing_music = False
                playback._current_song_name = None
                state.currently_reading = None

        def play_music(filename: str | None) -> bool:
            record("music_play", filename)
            selected = music_dir / Path(filename).name if filename else next(music_dir.glob("*.wav"), None)
            if selected is None or not selected.is_file():
                return False
            playback._is_playing_music = True
            playback._current_song_name = selected.name
            return True

        def stop_music() -> None:
            record("music_stop")
            playback._is_playing_music = False
            playback._current_song_name = None

        def play_story(story_id: str) -> bool:
            record("story_play", story_id)
            story = StoryLibrary(stories_dir).get(story_id)
            if story is None:
                return False
            state.currently_reading = {"id": story.id, "title": story.title}
            return True

        def stop_story() -> None:
            record("story_stop")
            state.currently_reading = None

        # Sólo estos efectos físicos se simulan; validación, estado y persistencia son reales.
        state.on_celebrate = lambda: record("celebrate")
        state.on_config_changed = lambda config: record("sensory", config)
        state.on_night_mode_changed = lambda enabled: record("night_mode", enabled)
        state.on_power_changed = power_changed
        state.on_not_ready_clip = lambda: record("not_ready_clip")
        state.on_play_music = play_music
        state.on_stop_music = stop_music
        state.on_play_story = play_story
        state.on_stop_story = stop_story
        state.notify_models_ready()
        state.set_power(True)
        self.events = events
        self.events_lock = events_lock
        self.api.robot_state = state

    def health(self) -> dict:
        return {
            "fixture": FIXTURE_KIND,
            "handler": "ApiRequestHandler",
            "state": "RobotState",
            "hardware": "simulated_callbacks",
            "storage": "temporary",
            "pdf_supported": importlib.util.find_spec("pypdf") is not None,
        }


def make_handler(fixture: RealApiFixture):
    api = fixture.api

    class FixtureHandler(api.ApiRequestHandler):
        def do_GET(self) -> None:
            path = urlparse(self.path).path.rstrip("/")
            if path == "/__fixture/health":
                self._send_json(fixture.health())
                return
            if path == "/__fixture/events":
                if not self._authorized(path, mutating=False):
                    return
                with fixture.events_lock:
                    self._send_json({"events": list(fixture.events)})
                return
            super().do_GET()

        def do_POST(self) -> None:
            path = urlparse(self.path).path.rstrip("/")
            if not path.startswith("/__fixture/"):
                super().do_POST()
                return
            if not self._authorized(path, mutating=True):
                return
            body = self._parse_json_body()
            if not isinstance(body, dict):
                self._send_error_json(400, "JSON inválido")
                return
            if path == "/__fixture/reset":
                fixture.reset(seed_profile=body.get("seed_profile") is True)
                self._send_json(fixture.health())
            elif path == "/__fixture/reload-state":
                fixture.reload_state()
                self._send_json({"status": "ok", "hardware": "simulated"})
            elif path == "/__fixture/shutdown":
                self._send_json({"status": "stopping"})
                threading.Thread(target=self.server.shutdown, daemon=True).start()
            elif path == "/__fixture/notifications":
                notices = body.get("notifications", [])
                if not isinstance(notices, list) or any(not isinstance(item, dict) for item in notices):
                    self._send_error_json(400, "Lista de avisos inválida")
                    return
                for item in notices:
                    api.robot_state.push_notification(str(item.get("type", "")), str(item.get("message", "")))
                with api.robot_state._notifications_lock:
                    count = len(api.robot_state._notifications)
                self._send_json({"count": count, "hardware": "simulated"})
            else:
                self._send_error_json(404, "Control de prueba no encontrado")

    return FixtureHandler


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=19876)
    parser.add_argument("--seed-profile", action="store_true", help="Crear un perfil ficticio para las pruebas de UI")
    args = parser.parse_args()
    if not 1 <= args.port <= 65535:
        parser.error("Puerto inválido")
    with tempfile.TemporaryDirectory(prefix="teo-real-api-") as directory:
        root = Path(directory)
        fixture = RealApiFixture(load_real_api(root), root, args.seed_profile)
        # Sin beacon UDP ni descubrimiento LAN: sólo escucha loopback para adb reverse.
        server = HTTPServer(("127.0.0.1", args.port), make_handler(fixture))
        print(json.dumps({**fixture.health(), "listen": f"127.0.0.1:{args.port}"}), flush=True)
        try:
            server.serve_forever(poll_interval=0.25)
        except KeyboardInterrupt:
            pass
        finally:
            server.server_close()


if __name__ == "__main__":
    main()
