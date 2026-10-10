#!/bin/sh
# Arranca app.py al abrir la sesion grafica. Lo llama el .desktop de autostart.
DIR=/home/teo/Desktop/ProyectoParaRasperrypiV5
LOG="$DIR/autostart.log"
cd "$DIR" || exit 1

exec 9>"$DIR/autostart.lock"
flock -n 9 || exit 0

if [ -f venv/bin/activate ]; then
  . venv/bin/activate
fi

export DISPLAY="${DISPLAY:-:0}"
export WAYLAND_DISPLAY="${WAYLAND_DISPLAY:-wayland-0}"
export XDG_RUNTIME_DIR="${XDG_RUNTIME_DIR:-/run/user/1000}"
if [ -z "$XAUTHORITY" ] && [ -f "$HOME/.Xauthority" ]; then
  export XAUTHORITY="$HOME/.Xauthority"
fi
export PYTHONUNBUFFERED=1

if [ -f "$LOG" ] && [ "$(wc -c < "$LOG")" -gt 1000000 ]; then
  : > "$LOG"
fi

echo "----- $(date -Is) start -----" >> "$LOG"
exec python -u app.py >> "$LOG" 2>&1
