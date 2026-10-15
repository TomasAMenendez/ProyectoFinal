# Integración Android y API Python

`teo_api_fixture.py` ejecuta el `ApiRequestHandler`, `RobotState`, validación de perfil,
persistencia, scheduler de rutinas y biblioteca de cuentos reales. Usa un directorio
temporal nuevo y un token exclusivo de prueba. No carga `.env`, datos existentes,
modelos, `app.py`, workers, Bluetooth, GPIO, audio ni cámara.

Los callbacks que representan hardware se simulan y se registran como tales. Esta
prueba acredita comunicación Android/Python y contratos de datos; no acredita los
efectos físicos del peluche. El Python utilizado necesita las dependencias existentes
del backend, incluido `numpy`. La extracción real de PDF requiere `pypdf`; ese caso
se marca omitido cuando falta la biblioteca.

Desde la raíz del proyecto, con los APK de aplicación y pruebas ya instalados:

```powershell
python tools/verification/teo_api_fixture.py --port 19876
```

En otra terminal:

```powershell
adb reverse tcp:19876 tcp:19876
adb shell am instrument -w -e class com.example.aplicacionparacelular.network.RealApiContractTest -e teoApiPort 19876 com.example.aplicacionparacelular.test/androidx.test.runner.AndroidJUnitRunner
```

La prueba instrumentada sólo se activa con `teoApiPort`. Usa `localhost` mediante
`adb reverse`, preferencias con nombres UUID y restaura `RobotApiClient` a las
preferencias reales en `@After`. No inicia `MainActivity` ni el servicio. Ejecutarla
separada de las pruebas de UI que cambien el mismo singleton.

La prueba `MainActivityStartupTest` se ejecuta por separado, con el teléfono
desbloqueado; concede los permisos de ejecución de la app necesarios para que
los diálogos del sistema no interrumpan su recreación. Si el teléfono está
bloqueado, se omite explícitamente esa comprobación.

Controles exclusivos del fixture:

| Método y ruta | Función |
| --- | --- |
| `GET /__fixture/health` | Confirma identidad, almacenamiento temporal, hardware simulado y soporte PDF. |
| `POST /__fixture/reset` | Crea un caso vacío; `{"seed_profile":true}` agrega un perfil ficticio para UI. |
| `POST /__fixture/reload-state` | Reconstruye el estado desde las preferencias temporales persistidas. |
| `POST /__fixture/shutdown` | Cierra el servidor local y elimina su almacenamiento temporal. |
| `GET /__fixture/events` | Lista callbacks físicos simulados. |
| `POST /__fixture/notifications` | Recibe `{"notifications":[{"type":"aviso","message":"Prueba"}]}` y usa la cola real. |

Salvo `health`, los controles requieren `X-Robot-Token` con el token público de
prueba `teo-integration-only-not-a-device-token`. Las rutas `/api/*` usan el handler
original. El servidor sólo escucha `127.0.0.1`; no anuncia beacons en la red.

Para una prueba de UI con perfil precargado, iniciar con `--seed-profile` o usar
`reset` con `seed_profile`. Cada caso instrumentado ejecuta su propio `reset`.

Al terminar, detener Python con Ctrl+C y quitar el puente:

```powershell
adb reverse --remove tcp:19876
```

Los datos temporales se eliminan al cerrar normalmente el proceso Python.
