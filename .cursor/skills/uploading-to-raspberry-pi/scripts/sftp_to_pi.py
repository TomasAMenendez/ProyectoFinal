#!/usr/bin/env python3
"""SFTP files to the Raspberry Pi. Password only via PI_SSH_PASS (never commit it)."""
from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

try:
    import paramiko
except ImportError:
    sys.stderr.write("paramiko no está instalado. Corré: python -m pip install paramiko\n")
    sys.exit(1)


def env(name: str, default: str | None = None) -> str:
    value = (os.environ.get(name) or "").strip()
    if value:
        return value
    if default is not None:
        return default
    sys.stderr.write(f"Falta {name}. Definila en el entorno; no la escribas en el repo.\n")
    sys.exit(2)


def connect() -> tuple[paramiko.SSHClient, paramiko.SFTPClient]:
    host = env("PI_HOST", "192.168.0.136")
    user = env("PI_USER", "teo")
    password = env("PI_SSH_PASS")
    client = paramiko.SSHClient()
    client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    client.connect(host, username=user, password=password, timeout=15)
    return client, client.open_sftp()


def remote_path(remote_dir: str, local: Path) -> str:
    return f"{remote_dir.rstrip('/')}/{local.name}"


# Mata python app.py de este usuario y lo vuelve a abrir como el autostart
# (venv, DISPLAY, sin --gui, sin el runner que corta a los N segundos).
_RESTART_SH = """\
set -u
cd "$DIR" || exit 1
me=$(id -un)
uid=$(id -u)
kill_matches() {
  sig=$1
  ps -u "$me" -o pid=,args= | while read -r pid args; do
    case "$args" in
      *python*app.py*)
        kill "$sig" "$pid" 2>/dev/null || true
        ;;
    esac
  done
}
kill_matches -TERM
sleep 1
kill_matches -KILL
sleep 1
alive=$(ps -u "$me" -o args= | grep -E 'python.*app[.]py' | grep -v grep || true)
if [ -n "$alive" ]; then
  echo "app.py sigue vivo" >&2
  exit 1
fi
if [ -x venv/bin/python ]; then
  PY="$DIR/venv/bin/python"
else
  PY=python
fi
export DISPLAY="${DISPLAY:-:0}"
export WAYLAND_DISPLAY="${WAYLAND_DISPLAY:-wayland-0}"
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/$uid}"
if [ -z "${XAUTHORITY:-}" ] && [ -f "$HOME/.Xauthority" ]; then
  export XAUTHORITY="$HOME/.Xauthority"
fi
export PYTHONUNBUFFERED=1
echo "----- $(date -Is) restart -----" >> "$DIR/autostart.log"
setsid "$PY" -u app.py >> "$DIR/autostart.log" 2>&1 </dev/null &
sleep 2
found=0
ps -u "$me" -o pid=,args= | while read -r pid args; do
  case "$args" in
    *python*app.py*)
      echo "restart pid $pid"
      found=1
      ;;
  esac
done
exit 0
"""


def restart_app(client: paramiko.SSHClient, remote_dir: str) -> int:
    import shlex

    quoted = shlex.quote(remote_dir)
    stdin, stdout, stderr = client.exec_command(f"DIR={quoted} sh -s", timeout=40)
    stdin.write(_RESTART_SH)
    stdin.channel.shutdown_write()
    out = stdout.read().decode("utf-8", "replace")
    err = stderr.read().decode("utf-8", "replace").strip()
    code = stdout.channel.recv_exit_status()
    sys.stdout.write(out)
    if err:
        sys.stderr.write(err + "\n")
    if code != 0 or "restart pid " not in out:
        sys.stderr.write("no quedó app.py corriendo después del reinicio\n")
        return code or 1
    return 0


def main() -> int:
    parser = argparse.ArgumentParser(description="Subir o borrar archivos en la Raspberry Pi")
    parser.add_argument("--put", nargs="*", default=[], help="Archivos locales a subir (basename en la Pi)")
    parser.add_argument("--delete", nargs="*", default=[], help="Nombres a borrar en PI_REMOTE_DIR")
    args = parser.parse_args()
    if not args.put and not args.delete:
        parser.error("pasá --put y/o --delete")

    remote_dir = env(
        "PI_REMOTE_DIR",
        "/home/teo/Desktop/ProyectoFinal/ProyectoParaRasperrypiV5",
    )
    client, sftp = connect()
    uploaded = False
    try:
        for raw in args.put:
            local = Path(raw).resolve()
            if not local.is_file():
                sys.stderr.write(f"no existe: {local}\n")
                return 1
            dest = remote_path(remote_dir, local)
            sftp.put(str(local), dest)
            print("ok", local.name, local.stat().st_size)
        for name in args.delete:
            dest = f"{remote_dir.rstrip('/')}/{Path(name).name}"
            try:
                sftp.remove(dest)
                print("rm", Path(name).name)
            except FileNotFoundError:
                print("missing", Path(name).name)
        uploaded = True
    finally:
        sftp.close()
        if not uploaded:
            client.close()
    try:
        return restart_app(client, remote_dir)
    finally:
        client.close()


if __name__ == "__main__":
    raise SystemExit(main())
