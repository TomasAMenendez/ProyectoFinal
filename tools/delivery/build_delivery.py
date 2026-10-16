"""Empaqueta el proyecto Android y una actualización de código del peluche."""
from __future__ import annotations

import argparse
import hashlib
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
EXCLUDED_PARTS = {".git", ".gradle", ".kotlin", ".idea", "build", "__pycache__", "venv"}


def tree_files(directory: Path):
    for file in sorted(directory.rglob("*")):
        if file.is_file() and not EXCLUDED_PARTS.intersection(file.relative_to(directory).parts):
            yield file


def digest(file: Path) -> str:
    with file.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def write_package(target: Path, files: list[tuple[Path, str]]) -> dict:
    inventory = []
    names = set()
    with zipfile.ZipFile(target, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
        for source, name in files:
            if name in names or name.startswith("/") or ".." in Path(name).parts:
                raise ValueError(f"Ruta de paquete inválida: {name}")
            names.add(name)
            archive.write(source, name)
            inventory.append({"path": name, "bytes": source.stat().st_size, "sha256": digest(source)})
    with zipfile.ZipFile(target) as archive:
        invalid = archive.testzip()
        if invalid is not None:
            raise RuntimeError(f"ZIP dañado: {invalid}")
    return {"file": target.name, "bytes": target.stat().st_size, "sha256": digest(target), "entries": inventory}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--date", default="2026-10-15")
    args = parser.parse_args()
    from datetime import date
    stamp = date.fromisoformat(args.date).isoformat()
    output = (ROOT / "entrega" / stamp).resolve()
    if not output.is_relative_to(ROOT / "entrega"):
        raise ValueError("Destino fuera de la carpeta de entrega")
    output.mkdir(parents=True, exist_ok=True)

    android = ROOT / "ProyectoAndroid"
    files = [(file, f"ProyectoAndroid/{file.relative_to(android).as_posix()}") for file in tree_files(android / "app" / "src")]
    for relative in ("settings.gradle.kts", "build.gradle.kts", "gradle.properties", "gradlew", "gradlew.bat", "app/build.gradle.kts", "app/proguard-rules.pro"):
        file = android / relative
        if file.is_file():
            files.append((file, f"ProyectoAndroid/{relative}"))
    files.extend((file, f"ProyectoAndroid/{file.relative_to(android).as_posix()}") for file in tree_files(android / "gradle"))
    guide = ROOT / "docs" / "entrega" / "LEEME.md"
    validation = ROOT / "docs" / "entrega" / "VALIDACION.md"
    files.extend([(guide, "LEEME.md"), (validation, "VALIDACION.md")])
    packages = [write_package(output / f"TEO-Android-{stamp}.zip", files)]

    backend = ROOT / "ProyectoParaRasperrypiV5"
    files = [(file, f"ProyectoParaRasperrypiV5/{file.name}") for file in sorted(backend.glob("*.py")) if not file.name.startswith("_")]
    for relative in ("requirements.txt", "intent_rules.json"):
        files.append((backend / relative, f"ProyectoParaRasperrypiV5/{relative}"))
    for relative in ("assets", "eyes", "juegos"):
        files.extend((file, f"ProyectoParaRasperrypiV5/{file.relative_to(backend).as_posix()}") for file in tree_files(backend / relative) if file.name != ".gitkeep")
    files.extend([(guide, "LEEME.md"), (validation, "VALIDACION.md")])
    packages.append(write_package(output / f"TEO-peluche-actualizacion-{stamp}.zip", files))

    apk = android / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    target_apk = output / "TEO-real-debug.apk"
    shutil.copy2(apk, target_apk)
    for source in (guide, validation):
        shutil.copy2(source, output / source.name)
    manifest = {
        "date": stamp,
        "application_id": "com.example.aplicacionparacelular",
        "variant": "debug",
        "peluche_hardware_verified": False,
        "apk": {"file": target_apk.name, "bytes": target_apk.stat().st_size, "sha256": digest(target_apk)},
        "packages": packages,
    }
    (output / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"output": str(output), "apk": manifest["apk"], "packages": [{key: package[key] for key in ("file", "bytes", "sha256")} for package in packages]}, ensure_ascii=False))


if __name__ == "__main__":
    main()
