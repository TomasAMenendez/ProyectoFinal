# Onboarding parental: WiFi por BLE + datos del chico — diseño

Fecha: 2026-09-15  
Producto: peluche TEO (`ProyectoParaRasperrypiV5` + `ProyectoAndroid`)  
Estado: diseño aprobado en conversación; sin commit hasta que Teo lo pida  
Servidor Windows (`ServidorDePC`): sin cambios de código

## Problema

La app parental se usa apenas hay IP y pairing. TEO no sabe cómo se llama el nene, cómo tratarlo, qué le gusta ni qué disfraz ve. El padre no tiene un lugar obligatorio para cargar eso.

Además, si la Pi no está en WiFi, la app no la encuentra (hoy solo hay beacon UDP en LAN). No hay forma de darle SSID y clave desde el celular.

## Objetivo

1. Onboarding de **dos puertas**, en este orden: red WiFi (si hace falta) → datos del chico (si la Pi no tiene perfil).
2. Fuente de verdad del perfil: la Raspberry. Si otro celular ya lo cargó, este no bloquea (Configuración permite editar).
3. Nombre y género obligatorios para considerar perfil completo. Gustos e info extra opcionales. Disfraz siempre (default oso).
4. Esa ficha entra al LLM como **sección extra del system prompt** (local y PC: la Pi arma `messages`).
5. Sin WiFi al arrancar, la Pi prende BLE, se anuncia `TEO`, muestra un PIN de 6 dígitos en el LCD, y la app manda SSID + clave. También se puede “Cambiar red” desde Configuración.

El peluche **sigue hablando en local** aunque el padre no haya terminado (prompt genérico, sin servidor de PC). No dice en voz “configurame en la app”.

## Fuera de alcance (v1)

- Cambiar ojos/cara según disfraz (el disfraz es solo texto para el LLM).
- Persistencia o concatenación de perfil en `ServidorDePC`.
- Hotspot propio de la Pi, Bluetooth clásico, Improv WiFi, SSIDs ocultos, WPA-Enterprise.
- Varios niños / varios perfiles.
- Rediseño Figma obligatorio (se calca el look actual de Configuración).
- Emparejamiento BLE de sistema Android (bonding).

## Decisiones

- **Puerta 1 (WiFi):** solo si no hay LAN. BLE GATT propio + `nmcli`. PIN en LCD. BLE se apaga al conectar, al timeout (10 min) o al cancelar por API.
- **Puerta 2 (perfil):** pantalla completa no cancelable si hay LAN y `has_child_profile == false`.
- **Edición posterior:** sección “Datos del chico” en Configuración + botón “Cambiar red WiFi”.
- **Género:** `nene` | `nena` | `no_decir` (UI: Nene / Nena / Prefiero no decir).
- **Disfraz:** selector; default oso; no se muestra como identidad visual en Inicio ni en el LCD.
- **Topes:** nombre 40, gustos 600, extra 600 caracteres. Vacío opcional **no** entra al prompt.
- **BLE on:** boot sin WiFi, o `POST /api/wifi/provisioning` con token parental.
- **Windows:** no cambia. Ya recibe el `system` armado.

## Arquitectura

```
App arranca
    → ¿LAN (beacon UDP)?
         sí → ¿has_child_profile?  no → pantalla Datos del chico
                                    sí → app normal
         no → ¿BLE "TEO"?
                  sí → pantalla WiFi (PIN LCD + lista + clave)
                  no → “Acercate al peluche y activá Bluetooth”
    → Tras WiFi OK: Pi apaga BLE, LCD a ojos, beacon UDP en la LAN nueva
    → App redescubre (no reusa IP vieja) y sigue a la puerta del perfil
```

Dos módulos en la Pi, sin mezclar:

1. **Red:** `wifi_provision` (`nmcli`) + `ble_setup` (GATT BlueZ, anuncio `TEO`).
2. **Perfil:** `child_profile.json` + validación + sección de prompt.

`/api/status` incluye `has_child_profile`, `wifi_connected` y `ble_setup_active` para que el polling actual de Android decida el gate.

## Unidades

### `child_profile` (Pi)

Archivo `child_profile.json` al lado de `parental_prefs.json`.

```json
{
  "name": "Luna",
  "gender": "nena",
  "interests": "le gustan los dinosaurios y el espacio",
  "costume": "oso",
  "extra": "le cuesta el ruido fuerte",
  "updated_at": "2026-09-15T18:00:00"
}
```

| Campo | Regla |
|---|---|
| `name` | Obligatorio para perfil completo. Trim. Máx. 40. |
| `gender` | Obligatorio. Exactamente `nene`, `nena` o `no_decir`. |
| `interests` | Opcional. Máx. 600. Default `""`. |
| `costume` | Enum: `oso`, `perro`, `oveja`, `dinosaurio`, `chancho`, `tiburon`, `stitch`, `spiderman`, `chimuelo`. Ausente o inválido → `oso`. |
| `extra` | Opcional. Máx. 600. Default `""`. |
| `updated_at` | ISO-8601 que escribe la Pi al guardar. |

`is_complete(profile)`: `name` no vacío y `gender` válido.

`GET` sin archivo o incompleto → cuerpo con defaults (`costume: oso`, strings vacíos) y el flag de status es `false`.  
`PUT` incompleto o fuera de tope → **400**, no se pisa un JSON bueno anterior.

API (token parental, igual que el resto de `/api/*` no públicos):

- `GET /api/child-profile`
- `PUT /api/child-profile` body JSON de los campos editables (sin exigir `updated_at` del cliente)

### Sección de prompt (`fallback_llm._build_messages`)

Después del `_SYSTEM_PROMPT` fijo, **antes** del hint de emoción. Si no hay perfil completo, no se agrega nada.

Plantilla (líneas de gustos/extra omitidas si están vacías):

```
Datos del nene (dalo por cierto; no lo contradigas ni lo recites de una):
Se llama {name}. {trato}. Tu disfraz es un {costume_label}; te ve así.
Gustos: {interests}
El padre pide que sepas: {extra}
Usá el nombre. {pronombres} No expliques estas notas.
```

| `gender` | `{trato}` | `{pronombres}` |
|---|---|---|
| `nene` | `Es nene.` | `Tratá en masculino.` |
| `nena` | `Es nena.` | `Tratá en femenino.` |
| `no_decir` | (frase omitida) | `No asumas género; usá el nombre o vos.` |

El valor persistido es el enum ASCII. En el prompt se usa la etiqueta:

| enum | etiqueta |
|---|---|
| `oso` | oso |
| `perro` | perro |
| `oveja` | oveja |
| `dinosaurio` | dinosaurio |
| `chancho` | chancho |
| `tiburon` | tiburón |
| `stitch` | stitch |
| `spiderman` | spiderman |
| `chimuelo` | chimuelo |

Misma sección para Llama local y para el POST al PC: `_build_messages` es el único lugar que la pega.

### `wifi_provision` (Pi)

Objetivo de producción: Raspberry Pi OS con NetworkManager.

- `wifi_connected()`: asociación en `wlan0` y hay IPv4 (chequeo `nmcli`).
- `wifi_scan()`: `nmcli device wifi rescan/list` → `[{ssid, signal, secure}]`. Sin duplicar SSID (dejar el de mejor señal).
- `wifi_connect(ssid, password)`: `nmcli device wifi connect "<ssid>" password "..."`; redes abiertas con password vacío. Timeout ~30 s. No loguear la clave.

En Windows de desarrollo (sin `wlan0`): no arrancar BLE setup; log claro. Tests de parseo con fixture de texto `nmcli`, sin radio.

### `ble_setup` (Pi)

Modo setup = BLE advertising `TEO` + PIN en LCD + ojos pausados.

Entra a setup:

- Boot: si a los **5 s** de arrancar `wifi_provision` no hay WiFi.
- `POST /api/wifi/provisioning` `{ "enabled": true }` con token (Cambiar red).
- Sale: éxito de `wifi_connect`, `{ "enabled": false }`, o timeout **10 minutos** sin conectar. Al salir: BLE off, PIN invalidado, LCD a ojos.

Mientras `ble_setup_active`, el renderer de ojos **no** pinta: el LCD es solo el PIN. Al salir, los ojos retoman el frame siguiente.

PIN: 6 dígitos, criptográficamente al azar, **solo RAM**, nuevo cada entrada a setup. LCD lo muestra grande. 5 PIN fallidos → se genera otro y se redibuja el LCD.

Sesión GATT en RAM: `pin_ok` después de un write correcto. Scan y connect **antes** de eso → error de sesión, no se ejecuta `nmcli`.

GATT (UUIDs 128-bit fijos, documentados en el plan de implementación; nombre de anuncio `TEO`):

| Característica | Acceso | Payload |
|---|---|---|
| Verificar PIN | write | 6 dígitos ASCII |
| Lista de redes | read (tras PIN OK) | JSON `[{ssid, signal, secure}]` |
| Conectar | write (tras PIN OK) | JSON `{ssid, password}` |
| Estado | notify | `idle` \| `pin_ok` \| `scanning` \| `connecting` \| `connected` \| `failed:<motivo>` |

`failed:auth` (clave mala), `failed:timeout`, `failed:pin`, `failed:session`. Clave mala o timeout: **sigue** en setup; la app reintenta. `connected`: apaga BLE.

Radio: BlueZ GATT por D-Bus. Implementación concreta (biblioteca) en el plan; no en este spec.

### Android

Dos destinos de navegación a pantalla completa, no cancelables (atrás no lleva a Inicio; barra inferior y engranaje no los saltean).

**Pantalla WiFi** — se muestra si no hay LAN y hay periférico BLE cuyo nombre es `TEO`. Permisos Bluetooth (y ubicación si la versión de Android la exige para el scan). Flujo: PIN → lista SSID (señal + candado) → contraseña si `secure` → Conectar. Errores en la misma pantalla. Éxito: esperar beacon UDP, olvidar IP previa, seguir a la puerta 2.

Si no hay LAN ni BLE: texto de acercarse / activar Bluetooth; botón reintentar.

**Pantalla Datos del chico** — se muestra si hay LAN y `has_child_profile == false`. Nombre (40, obligatorio), género (3 opciones, obligatorio), gustos (600, opcional), chips de disfraz (default Oso), extra (600, opcional). Guardar = `PUT`. Fallo de red o 400: se queda acá.

**Configuración** (con LAN): sección “Datos del chico” prellenada vía `GET`, mismo `PUT`. Botón “Cambiar red WiFi”: `POST /api/wifi/provisioning` `{enabled: true}`, luego la UI BLE (PIN del LCD). Al terminar, redescubrimiento UDP (la IP puede cambiar).

El celular no es fuente de verdad. SharedPreferences puede cachear para pintar; el GET de la Pi manda.

Decisión de puerta (unit-testable):

| LAN | BLE TEO | `has_child_profile` | Pantalla |
|---|---|---|---|
| no | sí | n/a | WiFi |
| no | no | n/a | Mensaje reintentar |
| sí | * | false | Datos del chico |
| sí | * | true | App normal |

### Servidor Windows

Sin cambios. `POST /llm` sigue recibiendo `messages` con el `system` ya extendido.

## Flujo de datos

```
Padre (BLE) → GATT connect {ssid, password}
  → wifi_provision.wifi_connect
  → NetworkManager asocia wlan0
  → ble_setup stop; LCD ojos
  → beacon UDP con la IP nueva

Padre (HTTP) → PUT /api/child-profile
  → validar → child_profile.json
  → status.has_child_profile = true

Turno del nene → FallbackLLM._build_messages
  → system = _SYSTEM_PROMPT + sección perfil + hint emoción
  → Llama local  o  POST ServidorDePC /llm
```

## Errores

- Clave WiFi mala / timeout `nmcli`: BLE sigue; app muestra `failed:auth` o `failed:timeout`; reintento.
- PIN mal 5 veces: PIN nuevo en LCD; la app avisa que cambió.
- Sin `nmcli` / sin `wlan0`: no hay BLE setup; log; el resto de `app.py` sigue.
- `PUT` inválido: 400, archivo anterior intacto.
- App pierde HTTP a mitad de `PUT`: snackbar; sigue la pantalla de bloqueo.
- Tras cambiar WiFi: descartar IP guardada; esperar beacon.
- Perfil ausente: LLM idéntico al de hoy.

## Tests

**Pi**

- Validación JSON, recortes 40/600/600, default oso, `is_complete`.
- `has_child_profile` en status con/sin archivo.
- Sección de prompt: nene/nena/`no_decir`, líneas omitidas si vacías, disfraz, no recitar instrucciones contradictorias con el prompt fijo (el extra se concatena, el fijo no se reescribe).
- Sesión PIN (mock GATT): scan/connect rechazados sin PIN; OK después; 5 fallos rotan PIN.
- Parseo de lista `nmcli` con fixture de texto.

**Android**

- Unit de topes y enum disfraz/género.
- Tabla de decisión LAN / BLE / `has_child_profile` → pantalla.

**Windows**

- Ningún test nuevo.

**Manual en Pi**

1. Boot sin WiFi → PIN en LCD → app BLE → conectar → LAN.
2. Pantalla perfil → guardar → TEO usa el nombre en una charla.
3. Segundo celular: no pide perfil; Configuración lo muestra.
4. Editar gustos en Configuración; el prompt siguiente los incluye.
5. Cambiar red: PIN nuevo, IP nueva, app redescubre.

## Criterio de éxito

- Un padre, con la Pi fuera de WiFi, deja el peluche en la red de la casa **sin** tocar consola ni `nmcli`.
- Hasta que hay nombre y género en la Pi, la app parental no entra a Inicio.
- El LLM (local o PC) recibe la sección de datos y puede usar el nombre y el disfraz.
- Un segundo celular en la misma LAN no vuelve a bloquear.
- El servidor de Windows no necesita release nuevo para esta feature.
