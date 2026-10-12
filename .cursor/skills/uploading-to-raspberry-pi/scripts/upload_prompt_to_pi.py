#!/usr/bin/env python3
"""Sube fallback_llm.py (prompt de Gemma) a la Pi en 192.168.0.148 y reinicia app.py."""
from __future__ import annotations

import getpass
import os
import sys
from pathlib import Path

_SCRIPTS = Path(__file__).resolve().parent
if str(_SCRIPTS) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS))

os.environ.setdefault("PI_HOST", "192.168.0.148")
os.environ.setdefault("PI_USER", "teo")
os.environ.setdefault(
    "PI_REMOTE_DIR",
    "/home/teo/Desktop/ProyectoParaRasperrypiV5",
)


def _find_prompt() -> Path:
    named = "ProyectoParaRasperrypiV5/fallback_llm.py"
    here = Path(__file__).resolve()
    for parent in [here.parent, *here.parents]:
        candidate = parent / named
        if candidate.is_file():
            return candidate
    cwd = Path.cwd() / named
    if cwd.is_file():
        return cwd
    local = Path.cwd() / "fallback_llm.py"
    if local.is_file():
        return local
    sys.stderr.write("no encuentro fallback_llm.py\n")
    sys.exit(1)


def main() -> int:
    if not (os.environ.get("PI_SSH_PASS") or "").strip():
        os.environ["PI_SSH_PASS"] = getpass.getpass("PI_SSH_PASS: ")
    prompt = _find_prompt()
    sys.argv = [sys.argv[0], "--put", str(prompt)]
    from sftp_to_pi import main as sftp_main

    print(f"host {os.environ['PI_HOST']}  {prompt.name}", flush=True)
    return sftp_main()


if __name__ == "__main__":
    raise SystemExit(main())
