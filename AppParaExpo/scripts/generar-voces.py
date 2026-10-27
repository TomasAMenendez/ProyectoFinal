"""Genera una vez las voces de Micaela y no vuelve a pedir las que ya existen."""
from __future__ import annotations

import hashlib
import json
import os
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor, as_completed
from pathlib import Path

APP = Path(__file__).resolve().parents[1]
REPO = APP.parent
sys.path.insert(0, str(REPO / "ServidorDePC"))

from tts_cartesia import synthesize_wav  # noqa: E402

VOCES = APP / "src" / "recursos" / "voces"
INDICE = VOCES / "indice.json"
FFMPEG = os.environ.get("FFMPEG", "").strip()


def recolectar(valor, salida: set[str]) -> None:
    if isinstance(valor, str):
        texto = valor.strip()
        if texto:
            salida.add(texto)
    elif isinstance(valor, dict):
        for item in valor.values():
            recolectar(item, salida)
    elif isinstance(valor, list):
        for item in valor:
            recolectar(item, salida)


def textos() -> list[str]:
    salida: set[str] = set()
    sombras = json.loads((APP / "src" / "datos" / "sombras.json").read_text(encoding="utf-8"))
    adivina = json.loads((APP / "src" / "datos" / "adivina.json").read_text(encoding="utf-8"))
    contar = json.loads((APP / "src" / "datos" / "contar.json").read_text(encoding="utf-8"))
    fijos = json.loads((APP / "src" / "audio" / "textos-fijos.json").read_text(encoding="utf-8"))
    for ficha in sombras["juego_siluetas"]:
        salida.add(ficha["respuestas"][0].strip())
        recolectar(ficha["pistas"], salida)
    for ficha in adivina:
        salida.add(ficha["respuestas"][0].strip())
        recolectar(ficha["pistas"], salida)
    for ficha in contar:
        recolectar(ficha["pregunta"], salida)
    recolectar(fijos, salida)
    return sorted(salida)


def archivo_de(texto: str) -> str:
    return hashlib.sha256(texto.encode("utf-8")).hexdigest()[:20] + ".mp3"


def convertir(wav: Path, mp3: Path) -> None:
    subprocess.run(
        [FFMPEG, "-y", "-i", str(wav), "-codec:a", "libmp3lame", "-b:a", "32k", "-ac", "1", str(mp3)],
        check=True,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
    )


def generar(texto: str) -> tuple[str, str | None]:
    destino = VOCES / archivo_de(texto)
    if destino.is_file() and destino.stat().st_size > 0:
        return texto, None
    wav = synthesize_wav(texto, "micaela", timeout_s=45)
    if not wav:
        return texto, "sin audio"
    temporal = destino.with_suffix(".wav")
    temporal.write_bytes(wav)
    try:
        convertir(temporal, destino)
    finally:
        temporal.unlink(missing_ok=True)
    if not destino.is_file():
        return texto, "no se comprimió"
    return texto, None


def main() -> int:
    if not FFMPEG or not Path(FFMPEG).is_file():
        print("ffmpeg no está disponible. La generación de voces se detiene.", file=sys.stderr)
        return 1
    VOCES.mkdir(parents=True, exist_ok=True)
    indice = json.loads(INDICE.read_text(encoding="utf-8")) if INDICE.is_file() else {}
    pendientes = textos()
    fallos: list[str] = []
    with ThreadPoolExecutor(max_workers=3) as grupo:
        futuros = [grupo.submit(generar, texto) for texto in pendientes]
        for hecho, futuro in enumerate(as_completed(futuros), start=1):
            texto, error = futuro.result()
            indice[texto] = archivo_de(texto)
            if error:
                fallos.append(texto)
            if hecho % 20 == 0 or hecho == len(pendientes):
                print(f"Voces {hecho}/{len(pendientes)}", flush=True)
    INDICE.write_text(json.dumps(indice, ensure_ascii=False, indent=2), encoding="utf-8")
    if fallos:
        print(f"No se pudieron generar {len(fallos)} voces.", file=sys.stderr)
        return 1
    print(f"Voces listas: {len(pendientes)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
