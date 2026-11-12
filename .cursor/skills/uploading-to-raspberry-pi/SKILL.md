---
name: uploading-to-raspberry-pi
description: >-
  Use when local files that should run on the Raspberry Pi were created, edited,
  or deleted; when deploying to the peluche, edge device, teo@192.168.0.136,
  ProyectoParaRasperrypiV5;   or when tempted to tell the user to scp/sftp instead
  of uploading. Use after any code change meant to run on the Pi.
  After a successful upload the script restarts app.py on the Pi.
---

# Uploading to Raspberry Pi

**Si el archivo tiene que ejecutarse en la Pi, subilo vos. No dejes el cambio solo en Windows.**

Violar la letra es violar el espíritu. Decirle a Teo que copie con `scp` no cuenta como subir.

## Cuándo

Después de crear, editar o borrar archivos que la Pi usa (`*.py`, tests, JSON, modelos de config, `requirements.txt`). El script sube y reinicia. No le digas a Teo que reinicie.

No aplica a README/docs que no corren en el dispositivo, ni a secretos (`.env`, claves).

## Cómo (cadena obligatoria)

1. Instalá `paramiko` si falta (`python -m pip install paramiko` o `uv pip install paramiko`).
2. **Nunca** pongas la contraseña en un archivo del repo, del skill, ni en `/tmp` versionable.
3. **Antes de subir, verificá el directorio que realmente corre.** El default del script no alcanza: en la Pi hay más de una copia de `ProyectoParaRasperrypiV5` y un `ok` en la copia equivocada no cambia el peluche.

   - Leé `~/.config/autostart/teo-peluche.desktop`. El `Exec` apunta al script de arranque (`start_peluche.sh`).
   - En ese script, la variable `DIR` es la carpeta que hay que modificar.
   - Si ya hay un `python -u app.py`, su cwd (`/proc/<pid>/cwd`) tiene que coincidir con ese `DIR`. Si no coincide, subí a la carpeta del proceso que arranca solo, no a la de una prueba.
   - Hoy ese directorio es `/home/teo/Desktop/ProyectoParaRasperrypiV5`. `ProyectoFinal/ProyectoParaRasperrypiV5` no existe en el escritorio, y `ProyectoFinal-main/ProyectoParaRasperrypiV5` es otra copia: no la uses salvo que el autostart haya cambiado y la verificación lo diga.
   - Pasá esa ruta en `PI_REMOTE_DIR` en la misma invocación. Después del `restart pid`, confirmá que el proceso nuevo tiene ese cwd.

4. En PowerShell, pasá la clave solo por env de esa invocación y ejecutá el script de **esta** skill:

```powershell
$env:PI_SSH_PASS = '<clave de teo, solo en memoria de la sesión>'
$env:PI_REMOTE_DIR = '/home/teo/Desktop/ProyectoParaRasperrypiV5'
python .cursor/skills/uploading-to-raspberry-pi/scripts/sftp_to_pi.py --put workers.py app.py
```

Solo el prompt de Gemma a `192.168.0.148` (sube `fallback_llm.py` y reinicia `app.py`):

```powershell
.\subir_prompt_pi.ps1
```

o `python .cursor/skills/uploading-to-raspberry-pi/scripts/upload_prompt_to_pi.py`.

Si el skill está en `~/.cursor/skills/uploading-to-raspberry-pi/`, usá esa ruta al `.py`.

Borrar en la Pi:

```powershell
python .cursor/skills/uploading-to-raspberry-pi/scripts/sftp_to_pi.py --delete mic_tcp.py
```

5. Comprobá la salida: cada `--put` imprime `ok <nombre> <bytes>`. Si falla SSH, reintentá; no caigas a "copialo vos".
6. Si el put o el delete terminó bien, el mismo script reinicia `app.py`. Tiene que aparecer `restart pid <número>`. Ese proceso es el de siempre: venv, `python -u app.py`, sin `--gui`, log en `autostart.log`. No uses `run_app_on_pi.py`: ese corta el proceso a los pocos segundos.
7. Si no aparece `restart pid`, el archivo subió y el peluche no llegó a quedar corriendo. Reintentá el script. No le pidas a Teo que lo arranque.

## Defaults (override por env)

| Env | Default |
|-----|---------|
| `PI_HOST` | `192.168.0.136` |
| `PI_USER` | `teo` |
| `PI_SSH_PASS` | obligatorio; no hay default en el skill |
| `PI_REMOTE_DIR` | `/home/teo/Desktop/ProyectoParaRasperrypiV5` (el `DIR` de `start_peluche.sh`; confirmalo cada vez) |

PowerShell: no uses `&&`. No metas Python con `python -c` y comillas anidadas: usá el script.

## Prohibido

- `scp` interactivo (pide clave y se cuelga).
- Dejar el upload "para que Teo lo haga".
- Escribir `PI_SSH_PASS` o `12345` en git, SKILL.md, o scripts commiteados.
- Asumir que hay clave SSH configurada (en esta Pi suele no haberla).
- Subir a la primera carpeta que se parezca al repo. Sin la verificación del autostart, el archivo no es el que corre.

## Exportar a otro repo o branch

Copiá **toda** la carpeta:

```
uploading-to-raspberry-pi/
  SKILL.md
  scripts/sftp_to_pi.py
```

- Otro branch/repo: `.cursor/skills/uploading-to-raspberry-pi/`
- Todos los proyectos de Cursor: `~/.cursor/skills/uploading-to-raspberry-pi/`

Ajustá `PI_REMOTE_DIR` / `PI_HOST` si el otro repo no vive en ese path.

## Excusas

| Excusa | Realidad |
|--------|----------|
| "Le digo que haga scp" | El agente sube. scp se traba pidiendo clave. |
| "No hay clave SSH" | Por eso paramiko + `PI_SSH_PASS`. |
| "El cambio es chico" | La Pi no ve el archivo hasta el SFTP. |
| "Windows no puede" | El script corre en Windows. |
| "Lo dejo para el final" | Subí en el mismo turno en que editaste. |
| "Le aviso que reinicie" | El script reinicia `app.py` al terminar. `restart pid` es la prueba. |
| "El default ya es la carpeta del proyecto" | Hay varias copias. Solo vale el `DIR` del autostart y el cwd de `python -u app.py`. |
