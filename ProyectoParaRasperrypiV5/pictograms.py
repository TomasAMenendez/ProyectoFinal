"""Catálogo fijo de pictogramas de rutina. No lee la carpeta de documentación."""
from __future__ import annotations

from pathlib import Path

PICTOGRAM_IDS = (
    "banio",
    "cepillar_dientes",
    "comer",
    "jardin",
    "medico",
)

_ASSETS = Path(__file__).resolve().parent / "assets" / "pictograms"


def resolve_pictogram(pictogram_id: str | None) -> Path | None:
    """Devuelve el PNG del id conocido si el archivo está en los assets."""
    if not isinstance(pictogram_id, str):
        return None
    key = pictogram_id.strip()
    if key not in PICTOGRAM_IDS:
        return None
    path = _ASSETS / f"{key}.png"
    if not path.is_file():
        return None
    return path
