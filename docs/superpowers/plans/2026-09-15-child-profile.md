# Datos del chico (perfil + prompt + gate Android) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. TDD. Sin `git commit` salvo pedido de Teo.

**Goal:** El padre carga nombre y género (y opcionalmente gustos, disfraz, extra) en la Pi; la app bloquea Inicio hasta que hay perfil; el LLM recibe esa ficha en el system prompt.

**Architecture:** Módulo puro `child_profile.py` (validar, persistir, armar sección de prompt). API `GET/PUT /api/child-profile` y `has_child_profile` en `/api/status`. `_build_messages` concatena la sección. Android: `OnboardingGate` puro + pantalla completa + sección en Configuración.

**Tech Stack:** Python 3.11, `unittest`, Android Kotlin/XML/ViewBinding, JUnit.

**Spec:** `docs/superpowers/specs/2026-09-15-child-profile-wifi-onboarding-design.md`  
**Siguiente plan:** `docs/superpowers/plans/2026-09-15-wifi-ble-provision.md` (WiFi por BLE). Este plan ya deja `OnboardingGate` con la tabla completa (BLE como boolean); el radio BLE se cablea en el otro.

## Global Constraints

- TDD: test rojo **antes** de producción. `python -m unittest …` desde `ProyectoParaRasperrypiV5`. **No pytest.**
- Android unit tests: `.\gradlew.bat :app:testDebugUnitTest` desde `ProyectoAndroid` (PowerShell: no `&&`).
- Sin commit salvo pedido de Teo (omitir todos los `git commit`).
- Servidor Windows: **cero** cambios.
- Topes: nombre 40, gustos 600, extra 600. `PUT` incompleto o fuera de tope → 400, no pisar JSON bueno.
- Costume inválido → `oso`. Gender inválido o nombre vacío → no completo / 400.
- Vacío opcional **no** entra al prompt.
- Tras cambios de producción que corren en la Pi, subir con `uploading-to-raspberry-pi`. `PI_REMOTE_DIR=/home/teo/Desktop/ProyectoParaRasperrypiV5`. **NUNCA** arrancar `app.py` salvo que Teo lo pida.
- PowerShell: no `&&`.

---

## File structure

- Create: `ProyectoParaRasperrypiV5/child_profile.py` — modelo, validación, load/save, sección de prompt.
- Create: `ProyectoParaRasperrypiV5/test_child_profile.py` — unittest del módulo + HTTP API.
- Modify: `ProyectoParaRasperrypiV5/fallback_llm.py` — `_build_messages(..., profile_section="")`.
- Modify: `ProyectoParaRasperrypiV5/workers.py` — pasar sección al armar `messages` (ruta PC).
- Modify: `ProyectoParaRasperrypiV5/cloud_services.py` — mismo `system` extendido si Groq está activo.
- Modify: `ProyectoParaRasperrypiV5/api_server.py` — status flag, GET/PUT.
- Modify: `ProyectoParaRasperrypiV5/test_full.py` — `_build_messages` con sección / sin sección.
- Modify: `ProyectoParaRasperrypiV5/test_llm_story_guard.py` — el tope de 1800 sigue siendo solo `_SYSTEM_PROMPT` fijo.
- Create: Android `onboarding/OnboardingGate.kt` + test.
- Create: Android `onboarding/ChildProfile.kt` + test.
- Create: `ChildProfileFragment` + layout + nav destination.
- Modify: `MainActivity`, `RobotApiClient`, `RobotConnectionManager`, `ConfigFragment`, `fragment_config.xml`, `strings.xml`, `mobile_navigation.xml`.

---

### Task 1: Módulo `child_profile` puro

**Files:**
- Create: `ProyectoParaRasperrypiV5/child_profile.py`
- Test: `ProyectoParaRasperrypiV5/test_child_profile.py`

**Interfaces:**
- Consumes: `pathlib.Path`, `json`, `datetime`
- Produces:

```python
NAME_MAX = 40
TEXT_MAX = 600
GENDERS = frozenset({"nene", "nena", "no_decir"})
COSTUMES = frozenset({
    "oso", "perro", "oveja", "dinosaurio", "chancho",
    "tiburon", "stitch", "spiderman", "chimuelo",
})
COSTUME_LABELS = {
    "oso": "oso", "perro": "perro", "oveja": "oveja",
    "dinosaurio": "dinosaurio", "chancho": "chancho",
    "tiburon": "tiburón", "stitch": "stitch",
    "spiderman": "spiderman", "chimuelo": "chimuelo",
}
DEFAULT_COSTUME = "oso"
PROFILE_FILENAME = "child_profile.json"

class ChildProfileError(ValueError): ...

@dataclass(frozen=True)
class ChildProfile:
    name: str
    gender: str
    interests: str
    costume: str
    extra: str
    updated_at: str | None = None

    def to_dict(self) -> dict[str, str | None]: ...

def default_profile() -> ChildProfile: ...
def is_complete(profile: ChildProfile | None) -> bool: ...
def parse_payload(data: dict) -> ChildProfile:  # raises ChildProfileError
def load_profile(path: Path | None = None) -> ChildProfile | None: ...
def save_profile(profile: ChildProfile, path: Path | None = None) -> ChildProfile: ...
def default_path() -> Path: ...  # directorio del módulo / child_profile.json
def set_profile_path(path: Path | None) -> None: ...  # tests/API; None restaura default
def current_prompt_section(path: Path | None = None) -> str: ...
def build_prompt_section(profile: ChildProfile | None) -> str: ...
```

`set_profile_path` es el único override. `RobotState` llama `set_profile_path(self._child_profile_path)` al iniciar (y los tests HTTP también). Así `complete_sync` / workers leen el mismo archivo que el PUT.

- [ ] **Step 1: Write the failing tests** in `test_child_profile.py`:

```python
"""Tests de child_profile (sin radio, sin LLM)."""
from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

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
)


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
        self.assertNotIn("Es nene.", text)
        self.assertNotIn("Es nena.", text)
        self.assertIn("No asumas género", text)
        self.assertIn("tiburón", text)
        self.assertIn("Gustos: dinos", text)
        self.assertIn("El padre pide que sepas: ruido", text)
        self.assertIn("no lo recites de una", text.lower())


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

    def test_load_missing_is_none(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            self.assertIsNone(load_profile(Path(tmp) / "nope.json"))

    def test_save_does_not_write_if_we_only_tested_parse(self) -> None:
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "child_profile.json"
            self.assertFalse(path.exists())


if __name__ == "__main__":
    unittest.main()
```

- [ ] **Step 2: Run** `python -m unittest test_child_profile.py -v`  
  Expected: FAIL (`ModuleNotFoundError: child_profile`).

- [ ] **Step 3: Implement `child_profile.py`** mínimo para verde.

`parse_payload`: trim name/interests/extra; name vacío o `len(name) > 40` o `len(interests|extra) > 600` → `ChildProfileError`; gender no en `GENDERS` → error; costume missing/invalid → `oso`.

`build_prompt_section`: si no `is_complete`, `""`. Si no:

```
Datos del nene (dalo por cierto; no lo contradigas ni lo recites de una):
Se llama {name}. {trato}Tu disfraz es un {label}; te ve así.
{gustos_line}{extra_line}Usá el nombre. {pronombres} No expliques estas notas.
```

- `nene` → trato `Es nene. ` (con espacio), pronombres `Tratá en masculino.`
- `nena` → `Es nena. ` / `Tratá en femenino.`
- `no_decir` → trato `""`, pronombres `No asumas género; usá el nombre o vos.`
- gustos_line solo si `interests.strip()`: `Gustos: {interests}\n`
- extra_line solo si `extra.strip()`: `El padre pide que sepas: {extra}\n`

`save_profile`: copia con `updated_at` ISO-8601 local (`datetime.now().isoformat(timespec="seconds")`), write JSON `ensure_ascii=False`, indent 2. `load_profile`: None si falta o JSON roto.

`_override_path: Path | None = None` a nivel módulo. `default_path()`: override o `Path(__file__).resolve().parent / PROFILE_FILENAME`. `set_profile_path(path)` setea override.

`current_prompt_section(path=None)`: `build_prompt_section(load_profile(path or default_path()))`.

- [ ] **Step 4: Re-run unittest** — PASS.
- [ ] **Step 5: No commit.**

---

### Task 2: Pegar la sección en `_build_messages`

**Files:**
- Modify: `ProyectoParaRasperrypiV5/fallback_llm.py` (`_build_messages`, `complete_sync`)
- Modify: `ProyectoParaRasperrypiV5/workers.py` (ruta PC, ~2500)
- Modify: `ProyectoParaRasperrypiV5/cloud_services.py` (system content)
- Modify: `ProyectoParaRasperrypiV5/test_child_profile.py` (tests de `_build_messages`)
- Modify: `ProyectoParaRasperrypiV5/test_full.py` (llamadas existentes siguen compilando)

**Interfaces:**
- Consumes: `child_profile.current_prompt_section`, `build_prompt_section`
- Produces: `FallbackLLM._build_messages(text, emotion, history, profile_section: str = "") -> list[dict[str, str]]`

- [ ] **Step 1: Add tests** to `test_child_profile.py`:

```python
from fallback_llm import FallbackLLM

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
```

- [ ] **Step 2: Run** `python -m unittest test_child_profile.TestBuildMessages -v`  
  Expected: FAIL (`TypeError: unexpected keyword argument profile_section` o no aparece Luna).

- [ ] **Step 3: Implement**

En `fallback_llm.py`:

```python
@staticmethod
def _build_messages(
    text: str,
    emotion: dict[str, Any] | None,
    history: list[dict[str, str]],
    profile_section: str = "",
) -> list[dict[str, str]]:
    system_content = _SYSTEM_PROMPT
    if profile_section.strip():
        system_content += "\n" + profile_section.strip() + "\n"
    # ... emotion hints iguales que hoy ...
```

En `complete_sync`, antes de `_build_messages`:

```python
from child_profile import current_prompt_section
section = current_prompt_section()
messages = self._build_messages(
    text, emotion, history if history is not None else self._history,
    profile_section=section,
)
```

En `workers.py` donde arma `messages` para el PC:

```python
from child_profile import current_prompt_section
messages = FallbackLLM._build_messages(
    user_text, emotion, history,
    profile_section=current_prompt_section(),
)
```

En `cloud_services.py`, el `system` content:

```python
from child_profile import current_prompt_section
section = current_prompt_section()
system = self._SYSTEM_PROMPT
if section.strip():
    system = system + "\n" + section.strip() + "\n"
messages = [{"role": "system", "content": system}, *hist, {"role": "user", "content": user_content}]
```

`test_full.py` no cambia firmas: el default `""` mantiene 4.7 y 4.8.

- [ ] **Step 4: Run** `python -m unittest test_child_profile.TestBuildMessages test_llm_story_guard.py -v`  
  Expected: PASS. `_SYSTEM_PROMPT` sigue `<= 1800`.
- [ ] **Step 5: No commit.**

---

### Task 3: API `GET/PUT /api/child-profile` y flag en status

**Files:**
- Modify: `ProyectoParaRasperrypiV5/api_server.py`
- Modify: `ProyectoParaRasperrypiV5/test_child_profile.py`

**Interfaces:**
- Consumes: `parse_payload`, `save_profile`, `load_profile`, `is_complete`, `default_path`
- Produces: `GET /api/child-profile`, `PUT /api/child-profile`, `to_dict()["has_child_profile"]`

- [ ] **Step 1: Write HTTP tests** (mismo patrón que `test_stories._api_stories`):

```python
import json
import threading
from contextlib import contextmanager
from http.server import HTTPServer
from urllib.error import HTTPError
from urllib.request import Request, urlopen

@contextmanager
def _api_profile(profile_path: Path):
    import api_server
    prev_token = api_server.robot_state.pairing_token
    prev_path = getattr(api_server.robot_state, "_child_profile_path", None)
    from child_profile import set_profile_path
    api_server.robot_state.pairing_token = ""
    api_server.robot_state._child_profile_path = profile_path
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
        set_profile_path(prev_path)
        if prev_path is not None:
            api_server.robot_state._child_profile_path = prev_path

def _http_json(method, url, body=None):
    data = json.dumps(body).encode("utf-8") if body is not None else None
    hdrs = {"Content-Type": "application/json"} if body is not None else {}
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
```

- [ ] **Step 2: Run** `python -m unittest test_child_profile.TestChildProfileApi -v`  
  Expected: FAIL (404 / no `has_child_profile`).

- [ ] **Step 3: Implement in `api_server.py`**

En `RobotState.__init__`:

```python
self._child_profile_path = APP_DIR / "child_profile.json"
```

En `to_dict()` (dentro del lock, keys nuevas):

```python
from child_profile import is_complete, load_profile
profile = load_profile(self._child_profile_path)
"has_child_profile": is_complete(profile),
```

GET handler `path == "/api/child-profile"`:

```python
from child_profile import default_profile, is_complete, load_profile
p = load_profile(robot_state._child_profile_path) or default_profile()
self._send_json({**p.to_dict(), "complete": is_complete(p)})
```

`to_dict` del dataclass: `name, gender, interests, costume, extra, updated_at` (updated_at puede ser `None` → JSON `null`).

PUT en `do_PUT` `path == "/api/child-profile"`:

```python
from child_profile import ChildProfileError, parse_payload, save_profile, is_complete
body = self._parse_json_body()
if body is None:
    self._send_error_json(400, "JSON inválido")
    return
try:
    profile = parse_payload(body)
except ChildProfileError as exc:
    self._send_error_json(400, str(exc))
    return
saved = save_profile(profile, robot_state._child_profile_path)
self._send_json({**saved.to_dict(), "complete": is_complete(saved)})
```

Endpoint **no** es público: requiere token si hay `pairing_token`. Tests ponen token `""` como stories.

- [ ] **Step 4: Re-run** `python -m unittest test_child_profile.py -v` — PASS.
- [ ] **Step 5: No commit.**

---

### Task 4: Android — modelo + `OnboardingGate`

**Files:**
- Create: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/onboarding/ChildProfile.kt`
- Create: `ProyectoAndroid/app/src/main/java/com/example/aplicacionparacelular/onboarding/OnboardingGate.kt`
- Test: `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/onboarding/ChildProfileTest.kt`
- Test: `ProyectoAndroid/app/src/test/java/com/example/aplicacionparacelular/onboarding/OnboardingGateTest.kt`

**Interfaces:**
- Consumes: `org.json.JSONObject` (mismo patrón que `StoryJson`)
- Produces: `ChildProfile`, `ChildProfile.parse`, `ChildProfile.canSubmit`, `OnboardingGate.decide`

- [ ] **Step 1: Write failing JUnit tests**

`OnboardingGateTest.kt`:

```kotlin
package com.example.aplicacionparacelular.onboarding

import org.junit.Assert.assertEquals
import org.junit.Test

class OnboardingGateTest {
    @Test fun wifiWhenNoLanAndBle() {
        assertEquals(
            OnboardingScreen.WIFI,
            OnboardingGate.decide(hasLan = false, bleTeo = true, hasChildProfile = false),
        )
    }
    @Test fun retryWhenNoLanNoBle() {
        assertEquals(
            OnboardingScreen.RETRY,
            OnboardingGate.decide(hasLan = false, bleTeo = false, hasChildProfile = false),
        )
    }
    @Test fun childProfileWhenLanIncomplete() {
        assertEquals(
            OnboardingScreen.CHILD_PROFILE,
            OnboardingGate.decide(hasLan = true, bleTeo = false, hasChildProfile = false),
        )
    }
    @Test fun noneWhenLanComplete() {
        assertEquals(
            OnboardingScreen.NONE,
            OnboardingGate.decide(hasLan = true, bleTeo = true, hasChildProfile = true),
        )
    }
    @Test fun lanWinsOverBle() {
        assertEquals(
            OnboardingScreen.CHILD_PROFILE,
            OnboardingGate.decide(hasLan = true, bleTeo = true, hasChildProfile = false),
        )
    }
}
```

`ChildProfileTest.kt`: parse JSON, `canSubmit` true solo con name no blank y gender en el set; `clamp` recorta 40/600/600; costume default oso; gender enum `NENE/NENA/NO_DECIR`.

- [ ] **Step 2: Run** desde `ProyectoAndroid`:

```
.\gradlew.bat :app:testDebugUnitTest --tests com.example.aplicacionparacelular.onboarding.OnboardingGateTest --tests com.example.aplicacionparacelular.onboarding.ChildProfileTest
```

Expected: FAIL (clases ausentes).

- [ ] **Step 3: Implement**

```kotlin
enum class OnboardingScreen { WIFI, RETRY, CHILD_PROFILE, NONE }

object OnboardingGate {
    fun decide(hasLan: Boolean, bleTeo: Boolean, hasChildProfile: Boolean): OnboardingScreen {
        if (!hasLan) return if (bleTeo) OnboardingScreen.WIFI else OnboardingScreen.RETRY
        return if (!hasChildProfile) OnboardingScreen.CHILD_PROFILE else OnboardingScreen.NONE
    }
}
```

`ChildProfile` data class: `name`, `gender` (`nene`/`nena`/`no_decir`/empty), `interests`, `costume`, `extra`, `complete`.  
`canSubmit`: `name.trim().isNotEmpty() && gender in setOf("nene","nena","no_decir")`.  
`COSTUMES` lista en el orden de la spec. `sanitize()` recorta y default oso.

En **este** plan, `OnboardingScreen.WIFI` y `RETRY` existen pero MainActivity **no** navega a ellos todavía (`bleTeo = false` siempre). El plan BLE los cablea.

- [ ] **Step 4: Re-run gradle tests** — PASS.
- [ ] **Step 5: No commit.**

---

### Task 5: Android — API client + pantalla de bloqueo + Configuración

**Files:**
- Modify: `RobotApiClient.kt` — `getChildProfile()`, `putChildProfile(profile)`
- Modify: `RobotConnectionManager.kt` — wrappers async + LiveData `hasChildProfile` desde poll de status
- Create: `ui/onboarding/ChildProfileFragment.kt`
- Create: `res/layout/fragment_child_profile.xml`
- Modify: `res/navigation/mobile_navigation.xml`
- Modify: `MainActivity.kt` — observar gate, ocultar bottom nav, bloquear atrás
- Modify: `ui/config/ConfigFragment.kt` + `fragment_config.xml` + `ConfigViewModel.kt`
- Modify: `res/values/strings.xml`

**Interfaces:**
- Consumes: `GET/PUT /api/child-profile`, `status.has_child_profile`
- Produces: pantalla `nav_child_profile`; sección Config “Datos del chico”

- [ ] **Step 1: No hay test de Fragment (JUnit no infla UI).** El gate ya está testeado. Implementar UI contra el contrato JSON del Task 3.

- [ ] **Step 2: Client**

```kotlin
fun getChildProfile(): ApiResult<JSONObject> = doGet("/api/child-profile")
fun putChildProfile(body: JSONObject): ApiResult<JSONObject> = doPut("/api/child-profile", body)
```

En `pollStatus` Success: `_hasChildProfile.postValue(result.data.optBoolean("has_child_profile", true))`  
Ojo default: si la Pi vieja no manda el campo, `true` evita bloquear para siempre a quien aún no actualizó firmware. Documentar en comentario. Tras subir este código a la Pi, el campo existe.

LiveData `hasChildProfile: LiveData<Boolean>`.

- [ ] **Step 3: Layout de bloqueo** — `ScrollView` + card (mismos estilos `Widget.App.CardView` / `TextAppearance.App.SectionTitle` que Config):
  - Título `onboarding_child_title` = “Datos del chico”
  - Subtítulo: no se puede usar la app hasta nombre y género
  - `TextInputLayout` nombre (`maxLength=40`, obligatorio)
  - `MaterialButtonToggleGroup` singleSelection: Nene / Nena / Prefiero no decir (`nene`/`nena`/`no_decir`)
  - Gustos `EditText` multi-line `maxLength=600`
  - ChipGroup disfraces (9 chips, `oso` checked por default)
  - Extra `EditText` `maxLength=600`
  - Botón Guardar (`btn_save`). Deshabilitado si `!canSubmit`.

Strings nuevas en `strings.xml` (castellano rioplatense, vos).

- [ ] **Step 4: `ChildProfileFragment`**
  - `OnBackPressedCallback` enabled = true que no hace pop.
  - Guardar → `putChildProfile`. Success + `complete==true` → `findNavController().popBackStack(R.id.nav_dashboard, false)` y dejar que MainActivity reevalúe (flag ya true).
  - Error: Snackbar, se queda.

- [ ] **Step 5: Nav** — destination `nav_child_profile` (no topLevel).

- [ ] **Step 6: `MainActivity`**
  - Observar `isConnected` + `hasChildProfile`.
  - `bleTeo = false` (plan BLE).
  - `hasLan = isConnected == true`.
  - Si `decide == CHILD_PROFILE` y destino actual no es `nav_child_profile` → `navigate(R.id.nav_child_profile)`.
  - Si `decide == NONE` y estamos en `nav_child_profile` → pop a dashboard.
  - Bottom nav: `visibility = GONE` si screen != NONE; `VISIBLE` si NONE. Toolbar engranaje: en `onOptionsItemSelected` si screen != NONE, ignorar `nav_config`.
  - En tablet `nav_view` drawer: mismo criterio si existe.

- [ ] **Step 7: Configuración** — card “Datos del chico” **arriba** de Sobreestimulación (después de Conexión): mismos campos, prefill `GET`, botón Guardar llama `PUT`. Visible solo si `isConnected`. No es blocking.

- [ ] **Step 8: Run** `python -m unittest test_child_profile.py -v` y gradle `OnboardingGateTest` + `ChildProfileTest` — PASS.
- [ ] **Step 9: No commit.**

Si se tocó código de la Pi: skill `uploading-to-raspberry-pi` (SFTP). No arrancar `app.py`.

---

### Task 6: Verificación local (Windows)

**Files:** ninguno nuevo.

- [ ] **Step 1:** `python -m unittest test_child_profile.py test_llm_story_guard.py -v` desde `ProyectoParaRasperrypiV5` — PASS.
- [ ] **Step 2:** `python test_full.py` — la sección 4.7/4.8 sigue OK (default sin perfil).
- [ ] **Step 3:** gradle unit tests onboarding — PASS.
- [ ] **Step 4:** Manual cuando Teo pida: Pi con perfil ausente → app en LAN muestra pantalla bloqueo → guardar Luna/nena → Inicio; segundo freeze de la app no pide de nuevo; Config edita gustos; una frase al LLM menciona a Luna (log del system o charla).
- [ ] **Step 5: No commit.**
