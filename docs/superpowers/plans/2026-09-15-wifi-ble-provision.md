# WiFi por BLE (provisioning) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. TDD. Sin `git commit` salvo pedido de Teo.

**Goal:** Si la Pi no tiene WiFi, se anuncia por BLE como `TEO`, muestra un PIN de 6 dígitos en el LCD y la app manda SSID+clave; al conectar, BLE se apaga y la app sigue el onboarding de perfil.

**Architecture:** Lógica pura (`wifi_provision` parsea `nmcli`, `BleSetupSession` maneja PIN/sesión) testeable en Windows. Radio real solo en Linux (BlueZ D-Bus, sin pip extra: `python3-dbus` del sistema). Android usa `OnboardingGate` del plan de perfil (`WIFI` / `RETRY`) y un cliente GATT.

**Tech Stack:** Python 3.11, `unittest`, `nmcli` (Pi), BlueZ D-Bus, Android BLE (`BluetoothLeScanner` + `BluetoothGatt`), Kotlin/XML.

**Spec:** `docs/superpowers/specs/2026-09-15-child-profile-wifi-onboarding-design.md`  
**Depende de:** `docs/superpowers/plans/2026-09-15-child-profile.md` (`OnboardingGate`, `has_child_profile`, pantalla de perfil). Ejecutar **después**.

## Global Constraints

- TDD: test rojo **antes** de producción. `python -m unittest …` desde `ProyectoParaRasperrypiV5`. **No pytest.**
- Sin commit salvo pedido de Teo.
- Servidor Windows (`ServidorDePC`): **cero** cambios.
- Anuncio BLE: nombre exacto `TEO`. PIN 6 dígitos, solo RAM, 5 fallos rotan PIN. Timeout setup 10 min. Boot check 5 s.
- Scan/connect GATT **rechazados** si `pin_ok` es false. No loguear la clave WiFi.
- En Windows de dev: no arrancar BLE; log claro. Tests de `nmcli` con fixture de texto.
- Mientras `ble_setup_active`, el LCD muestra el PIN (ojos no pisan).
- Tras cambios Pi: skill `uploading-to-raspberry-pi`. **NUNCA** arrancar `app.py` salvo que Teo lo pida.
- PowerShell: no `&&`.

### UUIDs GATT (fijos)

```
ADV_NAME = "TEO"
SERVICE_UUID = "74656f57-4946-4000-8000-0000a0000001"
CHAR_PIN     = "74656f57-4946-4000-8000-0000a0000002"  # write, 6 ASCII digits
CHAR_SCAN    = "74656f57-4946-4000-8000-0000a0000003"  # read JSON array
CHAR_CONNECT = "74656f57-4946-4000-8000-0000a0000004"  # write JSON {ssid,password}
CHAR_STATUS  = "74656f57-4946-4000-8000-0000a0000005"  # notify UTF-8
```

Estados notify: `idle` | `pin_ok` | `scanning` | `connecting` | `connected` | `failed:pin` | `failed:session` | `failed:auth` | `failed:timeout`.

---

## File structure

- Create: `ProyectoParaRasperrypiV5/wifi_provision.py`
- Create: `ProyectoParaRasperrypiV5/test_wifi_provision.py`
- Create: `ProyectoParaRasperrypiV5/ble_setup.py` — sesión + PIN image + coordinator
- Create: `ProyectoParaRasperrypiV5/test_ble_setup.py`
- Create: `ProyectoParaRasperrypiV5/ble_gatt_bluez.py` — transporte Linux (no se importa en tests Windows)
- Modify: `eye_display.py` — `set_lcd_override`
- Modify: `api_server.py` — `POST /api/wifi/provisioning`, flags status
- Modify: `app.py` — supervisor boot 5 s
- Create Android: `TeoBleClient.kt`, `WifiSetupFragment`, layout, permissions
- Modify: `MainActivity` (`bleTeo`), `ConfigFragment` (Cambiar red), `OnboardingGate` ya existe
- Create: `OnboardingRetryFragment` para `RETRY`

---

### Task 1: Parser `nmcli` + resultado de connect

**Files:**
- Create: `ProyectoParaRasperrypiV5/wifi_provision.py`
- Test: `ProyectoParaRasperrypiV5/test_wifi_provision.py`

**Interfaces:**
- Consumes: stdout de `nmcli` (texto)
- Produces:

```python
@dataclass(frozen=True)
class WifiNetwork:
    ssid: str
    signal: int
    secure: bool

def parse_nmcli_wifi_list(text: str) -> list[WifiNetwork]: ...
def parse_nmcli_device_wifi_connected(text: str) -> bool: ...
def classify_nmcli_connect(returncode: int, stderr: str, elapsed_s: float, timeout_s: float = 30.0) -> str:
    # "ok" | "auth" | "timeout"

class WifiProvisioner:
    def __init__(self, runner: Callable[[list[str], float], tuple[int, str, str]] | None = None) -> None: ...
    def is_connected(self) -> bool: ...
    def scan(self) -> list[WifiNetwork]: ...
    def connect(self, ssid: str, password: str) -> str: ...
```

`runner(argv, timeout) -> (returncode, stdout, stderr)`. Default: `subprocess.run` (no se usa en tests).

- [ ] **Step 1: Failing tests** — fixture:

```
IN-USE  BSSID              SSID              MODE   CHAN  RATE        SIGNAL  BARS  SECURITY
        AA:BB:CC:DD:EE:FF  Casa              Infra  6     130 Mbit/s  80      xxx   WPA2
        11:22:33:44:55:66  Casa              Infra  36    270 Mbit/s  40      xxx   WPA2
        AA:00:00:00:00:01  Directa           Infra  1     54 Mbit/s   70      xxx   --
        --                 --                Infra  1     54 Mbit/s   10      xxx   WPA2
```

(Usar `BARS` con caracteres ASCII en el test, no bloques unicode.)

Assert: dos redes, `Casa` signal 80 secure True (el de 40 se descarta), `Directa` secure False. Línea SSID `--` o vacía se ignora.

`parse_nmcli_device_wifi_connected`: texto `wlan0:wifi:connected` → True; `wlan0:wifi:disconnected` → False.

`classify_nmcli_connect(0, "", 2.0)` → `ok`; `returncode=1` stderr con `Secrets were required` o `802-11-wireless-security` → `auth`; `elapsed_s >= timeout_s` → `timeout`.

- [ ] **Step 2:** `python -m unittest test_wifi_provision.py -v` — FAIL (módulo ausente).

- [ ] **Step 3: Implement `wifi_provision.py`**

Parser: saltar header; `nmcli -t` es más fácil — **usar formato tabular `-t -f SSID,SIGNAL,SECURITY` en `scan()`** y parsear líneas `SSID:SIGNAL:SECURITY`. Entonces el test de parse usa:

```
Casa:80:WPA2
Casa:40:WPA2
Directa:70:
```

Deduplicar por SSID quedándose con max signal. `secure = bool(security.strip()) and security.strip() != "--"`.

`WifiProvisioner.scan`: `nmcli -t -f SSID,SIGNAL,SECURITY dev wifi` (timeout 15 s). Rescan previo `nmcli dev wifi rescan` (ignorar fallos).

`is_connected`: `nmcli -t -f DEVICE,TYPE,STATE device` y True si alguna línea `*:wifi:connected` (wlan).

`connect`: argv `nmcli dev wifi connect <ssid>` y si password no vacío agregar `password` + password. Timeout 30 s. `classify_nmcli_connect`. **Nunca** pasar password a `log_action`.

- [ ] **Step 4: unittest PASS.**
- [ ] **Step 5: No commit.**

---

### Task 2: `BleSetupSession` + imagen PIN

**Files:**
- Create: `ProyectoParaRasperrypiV5/ble_setup.py`
- Test: `ProyectoParaRasperrypiV5/test_ble_setup.py`

**Interfaces:**

```python
SETUP_TIMEOUT_S = 600.0
PIN_MAX_FAILURES = 5
ADV_NAME = "TEO"
SERVICE_UUID = "74656f57-4946-4000-8000-0000a0000001"
CHAR_PIN = "74656f57-4946-4000-8000-0000a0000002"
CHAR_SCAN = "74656f57-4946-4000-8000-0000a0000003"
CHAR_CONNECT = "74656f57-4946-4000-8000-0000a0000004"
CHAR_STATUS = "74656f57-4946-4000-8000-0000a0000005"

class BleSetupSession:
    def __init__(self, rng: Callable[[], str] | None = None, now: Callable[[], float] | None = None) -> None: ...
    def start(self) -> str:  # PIN
    def stop(self) -> None: ...
    active: bool
    pin_ok: bool
    status: str
    def verify_pin(self, pin: str) -> str:  # new status
    def allow_scan_or_connect(self) -> bool: ...
    def set_status(self, status: str) -> None: ...
    def expired(self) -> bool: ...

def render_pin_image(pin: str, width: int, height: int) -> "Image.Image": ...
```

`rng` default: `secrets.randbelow` → 6 dígitos zero-padded.

- [ ] **Step 1: Tests**

```python
class TestSession(unittest.TestCase):
    def test_start_sets_pin_and_idle(self) -> None:
        pins = iter(["111111", "222222"])
        s = BleSetupSession(rng=lambda: next(pins), now=lambda: 0.0)
        self.assertEqual(s.start(), "111111")
        self.assertTrue(s.active)
        self.assertFalse(s.pin_ok)
        self.assertFalse(s.allow_scan_or_connect())
        self.assertEqual(s.status, "idle")

    def test_wrong_pin_five_times_rotates(self) -> None:
        pins = iter(["111111", "222222"])
        s = BleSetupSession(rng=lambda: next(pins), now=lambda: 0.0)
        s.start()
        for _ in range(5):
            self.assertEqual(s.verify_pin("000000"), "failed:pin")
        self.assertEqual(s.status, "idle")
        self.assertEqual(s.verify_pin("222222"), "pin_ok")
        self.assertTrue(s.allow_scan_or_connect())

    def test_connect_before_pin_is_session(self) -> None:
        s = BleSetupSession(rng=lambda: "111111", now=lambda: 0.0)
        s.start()
        self.assertFalse(s.allow_scan_or_connect())

    def test_timeout(self) -> None:
        t = {"n": 0.0}
        s = BleSetupSession(rng=lambda: "111111", now=lambda: t["n"])
        s.start()
        t["n"] = 601.0
        self.assertTrue(s.expired())

class TestPinImage(unittest.TestCase):
    def test_size_and_digits(self) -> None:
        img = render_pin_image("123456", 320, 240)
        self.assertEqual(img.size, (320, 240))
```

- [ ] **Step 2:** `python -m unittest test_ble_setup.py -v` — FAIL.

- [ ] **Step 3: Implement** `ble_setup.py` (PIL `Image`, `ImageDraw`, `ImageFont.load_default()`). Fondo `#1a1a2e`, texto blanco centrado, PIN en font grande. No hardware.

- [ ] **Step 4: PASS.**
- [ ] **Step 5: No commit.**

---

### Task 3: Override LCD + flags API

**Files:**
- Modify: `ProyectoParaRasperrypiV5/eye_display.py` — `_lcd_override` + `set_lcd_override(image|None)`
- Modify: `ProyectoParaRasperrypiV5/test_eye_render.py` o `test_ble_setup.py` (mock lcd)
- Modify: `ProyectoParaRasperrypiV5/api_server.py`
- Modify: `ProyectoParaRasperrypiV5/test_child_profile.py` o nuevo `test_wifi_api.py`

**Interfaces:**
- Consumes: `BleSetupSession`, `WifiProvisioner`
- Produces: `robot_state.ble_setup_active`, `robot_state.wifi_connected`, `POST /api/wifi/provisioning`

- [ ] **Step 1: Test override** — `EyeDisplay` con lcd mock que registra `display()` calls. Con override, el frame no es el del animator (comparar `image.tobytes()` distinto o un flag `last_image`).

Test API:

- `to_dict()` incluye `ble_setup_active` (bool) y `wifi_connected` (bool).
- `POST {enabled:true}` llama callback `on_wifi_provisioning(True)`.
- `POST {enabled:false}` → `False`.
- JSON inválido → 400.

- [ ] **Step 2: FAIL, then implement.**

`EyeDisplay`:

```python
self._lcd_override = None  # Image | None
self._lcd_lock = threading.Lock()

def set_lcd_override(self, image) -> None:
    with self._lcd_lock:
        self._lcd_override = image

def _blit_lcd(self) -> None:
    lcd = self.lcd
    if lcd is None:
        return
    with self._lcd_lock:
        override = self._lcd_override
    try:
        frame = override if override is not None else self._anim.render(...)
        lcd.display(frame)
    except Exception:
        pass
```

`RobotState`:

```python
self.ble_setup_active: bool = False
self.wifi_connected: bool = False
self.on_wifi_provisioning: Callable[[bool], None] | None = None
```

`to_dict` agrega las dos keys (el `wifi_connected` lo actualiza el supervisor; default False hasta el primer poll).

`do_POST` `path == "/api/wifi/provisioning"`: body `enabled` bool; set flag; llamar callback; `_send_json({"status":"ok","enabled": ...})`.

GET no hace falta: status ya pollea.

- [ ] **Step 3: unittest PASS.**
- [ ] **Step 4: No commit.**

---

### Task 4: Coordinator + BlueZ (Linux) + boot 5 s

**Files:**
- Create: `ProyectoParaRasperrypiV5/ble_gatt_bluez.py`
- Modify: `ProyectoParaRasperrypiV5/ble_setup.py` — `WifiBleCoordinator`
- Modify: `ProyectoParaRasperrypiV5/app.py`
- Test: `test_ble_setup.py` con transporte mock (no D-Bus)

**Interfaces:**

```python
class GattTransport(Protocol):
    def start(self, session: BleSetupSession, on_scan, on_connect) -> None: ...
    def stop(self) -> None: ...
    def notify_status(self, status: str) -> None: ...

class WifiBleCoordinator:
    def __init__(self, provisioner: WifiProvisioner, transport: GattTransport,
                 eye_display, now=time.monotonic) -> None: ...
    def enter_setup(self) -> None: ...
    def leave_setup(self) -> None: ...
    def tick(self) -> None:  # timeout + wifi already connected
    def handle_scan(self) -> bytes:  # JSON; failed:session si no pin_ok
    def handle_connect(self, raw: bytes) -> None: ...
```

- [ ] **Step 1: Tests del coordinator con FakeTransport** (lista de notify, start/stop counts).

Casos:
- `enter_setup` → session.start, override LCD no None, transport.start, `ble_setup_active` True via callback opcional.
- `handle_scan` sin PIN → status `failed:session`, JSON `[]`.
- `handle_scan` con PIN → JSON de `provisioner.scan` (fake lista).
- `handle_connect` password mala → `failed:auth`, sigue active.
- `handle_connect` ok → `connected`, `leave_setup` (override None, transport.stop).
- `tick` a los 601 s → leave.
- `tick` si `provisioner.is_connected()` True durante setup de boot → leave (ya hay red).

- [ ] **Step 2: FAIL then implement coordinator** en `ble_setup.py`. FakeTransport en el test file.

- [ ] **Step 3: `ble_gatt_bluez.py`** — no importar este módulo en tests Windows. En Linux:

Objetos D-Bus (nombres fijos):

- App path `/com/teo/ble`
- Advertisement `/com/teo/ble/advertisement0` — `org.bluez.LEAdvertisement1`, `Type=peripheral`, `LocalName=TEO`, `ServiceUUIDs=[SERVICE_UUID]`
- Service `/com/teo/ble/service0` — `org.bluez.GattService1`, UUID `SERVICE_UUID`, Primary=true
- Characteristics `/com/teo/ble/service0/char{pin,scan,connect,status}` — `org.bluez.GattCharacteristic1` con los UUID del header
  - PIN: Flags `write`, `write-without-response`. `WriteValue(value, options)` → ASCII → `session.verify_pin` → `notify_status`
  - SCAN: Flags `read`. `ReadValue` → `on_scan()` (bytes JSON UTF-8) o `[]` + status `failed:session`
  - CONNECT: Flags `write`. `WriteValue` → `on_connect(bytes(value))`
  - STATUS: Flags `notify`. `StartNotify`/`StopNotify`; `notify_status` hace `PropertiesChanged` `Value`

Registro: `org.bluez.GattManager1.RegisterApplication` en el adapter (`/org/bluez/hci0`) y `org.bluez.LEAdvertisingManager1.RegisterAdvertisement`. `stop()` hace Unregister + `RemoveFromConnection`.

Si `import dbus` o el adapter fallan: clase no-op, `log_action("WIFI_BLE", "BLE setup omitido: dbus/BlueZ no disponible")`, `start()` no levanta radio. `app.py` no crashea.

No unittest de D-Bus en Windows.

- [ ] **Step 4: `app.py`**
  Tras `api_server.start()`, si Linux:

```python
self._wifi_ble = None
try:
    from ble_setup import WifiBleCoordinator
    from ble_gatt_bluez import BluezGattTransport
    from wifi_provision import WifiProvisioner
    from api_server import robot_state
    coord = WifiBleCoordinator(
        WifiProvisioner(),
        BluezGattTransport(),
        getattr(self, "eye_display", None),
    )
    robot_state.on_wifi_provisioning = lambda on: coord.enter_setup() if on else coord.leave_setup()
    self._wifi_ble = coord
    threading.Thread(target=self._wifi_ble_boot, name="WifiBleBoot", daemon=True).start()
except Exception as exc:
    log_action("WIFI_BLE", f"no arranqué: {exc}")

def _wifi_ble_boot(self) -> None:
    time.sleep(5)
    from wifi_provision import WifiProvisioner
    if WifiProvisioner().is_connected():
        from api_server import robot_state
        robot_state.wifi_connected = True
        return
    if self._wifi_ble is not None:
        self._wifi_ble.enter_setup()
```

Loop `tick` cada 2 s en el mismo hilo daemon mientras `app.py` viva (o el coordinator arranca su hilo en `enter_setup`).

Actualizar `robot_state.wifi_connected` / `ble_setup_active` desde el coordinator.

En Windows: no entrar a este bloque (o entrar y que `is_connected` sea False + transport no-op: **no** llamar `enter_setup` si el transport no puede anunciar). Condición: `sys.platform.startswith("linux")`.

- [ ] **Step 5: unittest coordinator PASS. No commit.**

---

### Task 5: Android BLE + pantallas WIFI/RETRY + Cambiar red

**Files:**
- Modify: `AndroidManifest.xml` — permisos BLE
- Create: `onboarding/TeoBleUuids.kt` (mismos UUID string)
- Create: `onboarding/TeoBleClient.kt`
- Create: `ui/onboarding/WifiSetupFragment.kt` + `fragment_wifi_setup.xml`
- Create: `ui/onboarding/OnboardingRetryFragment.kt` + layout
- Modify: `MainActivity.kt` — `bleTeo` LiveData, navegar WIFI/RETRY, olvidar IP al `connected`
- Modify: `ConfigFragment` + strings — botón Cambiar red
- Modify: `RobotApiClient` — `postWifiProvisioning(enabled: Boolean)`
- Test: `TeoBleUuids` / parse JSON scan (JUnit, sin radio)

**Interfaces:**
- Consumes: `OnboardingGate.decide`, GATT TEO
- Produces: LAN tras WiFi; luego el gate de perfil (plan anterior)

- [ ] **Step 1: JUnit** — parsear `[{"ssid":"Casa","signal":80,"secure":true}]` a data class `WifiNetworkUi`. UUID constants iguales a Python.

- [ ] **Step 2: Permisos**

```xml
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" android:usesPermissionFlags="neverForLocation" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" android:maxSdkVersion="30" />
<uses-feature android:name="android.hardware.bluetooth_le" android:required="false" />
```

Pedir runtime en `WifiSetupFragment` / MainActivity antes de escanear.

- [ ] **Step 3: `TeoBleClient`**
  - Scan 8 s, filtro nombre `TEO`.
  - `connect(device)` → discover services.
  - `writePin(pin)` CHAR_PIN.
  - `readScan(): List<WifiNetworkUi>` CHAR_SCAN.
  - `writeConnect(ssid, password)` CHAR_CONNECT.
  - Subscribe CHAR_STATUS.
  - Callbacks main thread.

- [ ] **Step 4: `WifiSetupFragment`**
  - Campo PIN 6 dígitos → Verificar.
  - Lista SSIDs (señal, candado). Tap → si `secure`, pedir password; si no, password `""`.
  - Estados: conectando / `failed:auth` / `failed:pin` (avisar “el PIN del LCD cambió” si 5 fallos — la app no cuenta: si `failed:pin` repetido y el LCD cambió, el padre relee). Texto: “Si falló 5 veces, mirá el PIN nuevo en TEO”.
  - `connected`: `RobotApiClient` clear IP (`setRobotAddress` vacío + `stopPolling`) y `RobotDiscovery.startScan`; `bleTeo` puede quedar true hasta haber LAN.
  - Atrás no sale. Bottom nav hidden (MainActivity).

- [ ] **Step 5: `OnboardingRetryFragment`**
  - Texto spec: acercate al peluche y activá Bluetooth.
  - Botón Reintentar: re-scan BLE 8 s + UDP discovery.

- [ ] **Step 6: MainActivity**
  - `TeoBleScanner` liviano (solo presencia de nombre TEO) cuando `!hasLan`, actualiza `_bleTeo`.
  - `decide(...)` ahora usa `bleTeo`.
  - Navegar a `nav_wifi_setup` / `nav_onboarding_retry` / `nav_child_profile` / NONE.
  - Si `WIFI` o `RETRY` o `CHILD_PROFILE`: bottom nav GONE, ignorar engranaje.

- [ ] **Step 7: Config “Cambiar red WiFi”**
  - Visible si `isConnected`.
  - Click: `postWifiProvisioning(true)` luego navegar a `nav_wifi_setup` (PIN nuevo en LCD). Al `connected`, `RobotDiscovery` de nuevo (IP puede cambiar: borrar IP guardada **antes** del scan).

```kotlin
fun forgetRobotAddress(context: Context) {
    // prefs robot_ip "" y baseUrl = ""
}
```

Añadir `RobotApiClient.clearAddress(context)` explícito.

- [ ] **Step 8: gradle unit tests onboarding + parse scan JSON — PASS. No commit.**

---

### Task 6: Verificación

- [ ] **Step 1:** `python -m unittest test_wifi_provision.py test_ble_setup.py test_child_profile.py test_llm_story_guard.py -v` — PASS.
- [ ] **Step 2:** gradle `OnboardingGateTest` sigue PASS (tabla WIFI/RETRY ya existía).
- [ ] **Step 3:** Subir Pi con uploading skill. Manual (cuando Teo pida `app.py`):
  1. Pi sin WiFi → PIN en LCD (ojos no se ven).
  2. App → pantalla WIFI → PIN → lista → clave → LAN → pantalla perfil si falta.
  3. Clave mala: sigue BLE, reintenta.
  4. Config Cambiar red → PIN nuevo.
  5. Timeout 10 min: ojos vuelven (no hace falta esperar 10 min en CI).
- [ ] **Step 4: No commit.**
