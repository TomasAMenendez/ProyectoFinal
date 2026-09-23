"""Tests de child_profile (sin radio, sin LLM)."""
from __future__ import annotations

import json
import tempfile
import threading
import unittest
from contextlib import contextmanager
from http.server import HTTPServer
from pathlib import Path
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from child_profile import (
    ChildProfile,
    ChildProfileError,
    DEFAULT_COSTUME,
    NAME_MAX,
    TEXT_MAX,
    build_prompt_section,
    default_profile,
    is_complete,
    load_profile,
    parse_payload,
    save_profile,
    set_profile_path,
)
from fallback_llm import FallbackLLM, _SYSTEM_PROMPT

# _SYSTEM_PROMPT stays <= 1800; profile section (~200+200 clipped) plus template
# must stay under ~2500 chars so Llama n_ctx=1024 has headroom.
_SYSTEM_PLUS_PROFILE_BUDGET = 1800 + 610


@contextmanager
def _api_profile(profile_path: Path):
    import api_server
    import child_profile as child_profile_mod

    prev_token = api_server.robot_state.pairing_token
    prev_override = child_profile_mod._override_path
    api_server.robot_state.pairing_token = ""
    set_profile_path(profile_path)
    httpd = HTTPServer(("127.0.0.1", 0), api_server.ApiRequestHandler)
    thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    thread.start()
    try:
        yield f"http://127.0.0.1:{httpd.server_address[1]}"
    finally:
        httpd.shutdown()
        httpd.server_close()
        thread.join(timeout=2)
        api_server.robot_state.pairing_token = prev_token
        set_profile_path(prev_override)


def _http_json(method, url, body=None, *, raw_data: bytes | None = None):
    if raw_data is not None:
        data = raw_data
    elif body is not None:
        data = json.dumps(body).encode("utf-8")
    else:
        data = None
    hdrs = {"Content-Type": "application/json"} if data is not None else {}
    req = Request(url, data=data, headers=hdrs, method=method)
    try:
        with urlopen(req, timeout=5) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except HTTPError as exc:
        return exc.code, json.loads(exc.read().decode("utf-8") or "{}")


class TestChildProfileApi(unittest.TestCase):
    def test_get_missing_defaults_and_status_false(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "child_profile.json"
            with _api_profile(path) as base:
                import api_server
                st = api_server.robot_state.to_dict()
                self.assertFalse(st["has_child_profile"])
                code, body = _http_json("GET", f"{base}/api/child-profile")
                self.assertEqual(code, 200)
                self.assertEqual(body["name"], "")
                self.assertEqual(body["costume"], "oso")
                self.assertFalse(body["complete"])

    def test_put_ok_then_complete(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "child_profile.json"
            with _api_profile(path) as base:
                code, body = _http_json("PUT", f"{base}/api/child-profile", {
                    "name": "Luna", "gender": "nena", "interests": "",
                    "costume": "oso", "extra": "",
                })
                self.assertEqual(code, 200, str(body))
                self.assertTrue(body["complete"])
                import api_server
                self.assertTrue(api_server.robot_state.to_dict()["has_child_profile"])

    def test_put_incomplete_400_does_not_clobber(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "child_profile.json"
            save_profile(parse_payload({"name": "Luna", "gender": "nena"}), path)
            with _api_profile(path) as base:
                code, _ = _http_json("PUT", f"{base}/api/child-profile", {
                    "name": "", "gender": "nena",
                })
                self.assertEqual(code, 400)
                loaded = load_profile(path)
                self.assertEqual(loaded.name, "Luna")

    def test_put_json_array_returns_400(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "child_profile.json"
            with _api_profile(path) as base:
                code, body = _http_json("PUT", f"{base}/api/child-profile", raw_data=b"[]")
                self.assertEqual(code, 400)
                self.assertEqual(body.get("error"), "JSON inválido")

    def test_put_uses_set_profile_path_not_robot_state_attribute(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            api_path = Path(tmp) / "child_profile.json"
            decoy = Path(tmp) / "decoy.json"
            with _api_profile(api_path) as base:
                import api_server
                api_server.robot_state._child_profile_path = decoy
                code, body = _http_json("PUT", f"{base}/api/child-profile", {
                    "name": "Luna", "gender": "nena", "interests": "",
                    "costume": "oso", "extra": "",
                })
                self.assertEqual(code, 200, str(body))
                self.assertTrue(api_path.is_file())
                self.assertFalse(decoy.is_file())
                loaded = load_profile(api_path)
                assert loaded is not None
                self.assertEqual(loaded.name, "Luna")


class TestParse(unittest.TestCase):
    def test_complete_ok(self) -> None:
        p = parse_payload({
            "name": "  Luna  ",
            "gender": "nena",
            "interests": "dinos",
            "costume": "perro",
            "extra": "ruido",
        })
        self.assertEqual(p.name, "Luna")
        self.assertEqual(p.gender, "nena")
        self.assertEqual(p.costume, "perro")
        self.assertTrue(is_complete(p))

    def test_default_costume_when_missing(self) -> None:
        p = parse_payload({"name": "Luna", "gender": "nene"})
        self.assertEqual(p.costume, DEFAULT_COSTUME)

    def test_invalid_costume_becomes_oso(self) -> None:
        p = parse_payload({"name": "Luna", "gender": "nene", "costume": "goku"})
        self.assertEqual(p.costume, "oso")

    def test_empty_name_raises(self) -> None:
        with self.assertRaises(ChildProfileError):
            parse_payload({"name": "  ", "gender": "nene"})

    def test_bad_gender_raises(self) -> None:
        with self.assertRaises(ChildProfileError):
            parse_payload({"name": "Luna", "gender": "otro"})

    def test_name_too_long_raises(self) -> None:
        with self.assertRaises(ChildProfileError):
            parse_payload({"name": "x" * (NAME_MAX + 1), "gender": "nene"})

    def test_interests_too_long_raises(self) -> None:
        with self.assertRaises(ChildProfileError):
            parse_payload({
                "name": "Luna", "gender": "nena",
                "interests": "y" * (TEXT_MAX + 1),
            })

    def test_incomplete_without_gender(self) -> None:
        self.assertFalse(is_complete(default_profile()))
        self.assertFalse(is_complete(None))

    def test_null_name_raises(self) -> None:
        with self.assertRaises(ChildProfileError):
            parse_payload({"name": None, "gender": "nene"})

    def test_null_gender_raises(self) -> None:
        with self.assertRaises(ChildProfileError):
            parse_payload({"name": "Luna", "gender": None})

    def test_null_interests_and_extra_become_empty(self) -> None:
        p = parse_payload({
            "name": "Luna",
            "gender": "nena",
            "interests": None,
            "extra": None,
        })
        self.assertEqual(p.interests, "")
        self.assertEqual(p.extra, "")
        text = build_prompt_section(p)
        self.assertNotIn("Gustos:", text)
        self.assertNotIn("El padre pide que sepas:", text)

    def test_non_string_name_raises(self) -> None:
        with self.assertRaises(ChildProfileError):
            parse_payload({"name": 123, "gender": "nene"})


class TestPrompt(unittest.TestCase):
    def test_none_is_empty(self) -> None:
        self.assertEqual(build_prompt_section(None), "")

    def test_incomplete_is_empty(self) -> None:
        self.assertEqual(build_prompt_section(default_profile()), "")

    def test_nena_includes_trato_and_omits_empty_optional(self) -> None:
        p = ChildProfile("Luna", "nena", "", "oso", "")
        text = build_prompt_section(p)
        self.assertIn("Se llama Luna.", text)
        self.assertIn("Es nena.", text)
        self.assertIn("Tu disfraz es un oso", text)
        self.assertIn("Tratá en femenino.", text)
        self.assertNotIn("Gustos:", text)
        self.assertNotIn("El padre pide que sepas:", text)

    def test_no_decir_omits_gender_line(self) -> None:
        p = ChildProfile("Luna", "no_decir", "dinos", "tiburon", "ruido")
        text = build_prompt_section(p)
        self.assertIn("Tratá en masculino.", text)
        self.assertNotIn("Es nene.", text)
        self.assertNotIn("Es nena.", text)
        self.assertNotIn("No asumas género", text)
        self.assertIn("tiburón", text)
        self.assertIn("Gustos: dinos", text)
        self.assertIn("El padre pide que sepas: ruido", text)
        self.assertIn("no lo contradigas ni lo recites de una", text.lower())

    def test_prompt_truncates_long_interests_and_extra(self) -> None:
        p = ChildProfile("Luna", "nena", "i" * TEXT_MAX, "oso", "e" * TEXT_MAX)
        text = build_prompt_section(p)
        self.assertNotIn("i" * 201, text)
        self.assertNotIn("e" * 201, text)
        self.assertIn("Gustos:", text)
        self.assertIn("El padre pide que sepas:", text)

    def test_max_size_profile_fits_n_ctx_budget(self) -> None:
        p = ChildProfile(
            "n" * NAME_MAX,
            "no_decir",
            "i" * TEXT_MAX,
            "spiderman",
            "e" * TEXT_MAX,
        )
        section = build_prompt_section(p)
        self.assertLessEqual(
            len(_SYSTEM_PROMPT) + len(section),
            _SYSTEM_PLUS_PROFILE_BUDGET,
        )
        self.assertLessEqual(len(_SYSTEM_PROMPT) + len(section), 2500)


class TestBuildMessages(unittest.TestCase):
    def test_section_before_emotion(self) -> None:
        section = build_prompt_section(ChildProfile("Luna", "nena", "", "oso", ""))
        msgs = FallbackLLM._build_messages(
            "hola",
            {"label": "triste", "score": 0.9},
            [],
            profile_section=section,
        )
        sysmsg = msgs[0]["content"]
        self.assertIn("Se llama Luna.", sysmsg)
        self.assertIn("El nene parece estar triste", sysmsg)
        self.assertLess(sysmsg.find("Se llama Luna."), sysmsg.find("El nene parece estar triste"))

    def test_empty_section_keeps_today_prompt(self) -> None:
        msgs = FallbackLLM._build_messages("hola", None, [])
        self.assertTrue(msgs[0]["content"].startswith("Tu nombre es TEO."))
        self.assertNotIn("Datos del nene", msgs[0]["content"])


class TestStore(unittest.TestCase):
    def test_roundtrip(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "child_profile.json"
            p = parse_payload({"name": "Luna", "gender": "nena", "costume": "stitch"})
            saved = save_profile(p, path)
            self.assertTrue(saved.updated_at)
            loaded = load_profile(path)
            assert loaded is not None
            self.assertEqual(loaded.name, "Luna")
            self.assertEqual(loaded.costume, "stitch")
            self.assertEqual(list(Path(tmp).glob("*.tmp")), [])

    def test_json_still_stores_600_char_interests(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "child_profile.json"
            interests = "i" * TEXT_MAX
            extra = "e" * TEXT_MAX
            p = parse_payload({
                "name": "Luna",
                "gender": "nena",
                "interests": interests,
                "extra": extra,
            })
            save_profile(p, path)
            loaded = load_profile(path)
            assert loaded is not None
            self.assertEqual(len(loaded.interests), 600)
            self.assertEqual(len(loaded.extra), 600)
            raw = json.loads(path.read_text(encoding="utf-8"))
            self.assertEqual(len(raw["interests"]), 600)
            self.assertEqual(len(raw["extra"]), 600)

    def test_load_missing_is_none(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            self.assertIsNone(load_profile(Path(tmp) / "nope.json"))

    def test_save_does_not_write_if_we_only_tested_parse(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "child_profile.json"
            self.assertFalse(path.exists())


if __name__ == "__main__":
    unittest.main()
