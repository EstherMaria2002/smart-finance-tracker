"""Carga y guarda el modelo entrenado (joblib) de forma segura."""
import logging
import os
import tempfile
from pathlib import Path
from typing import Any

import joblib

logger = logging.getLogger(__name__)


def save_model(bundle: dict[str, Any], path: Path) -> None:
    """Guarda de forma atómica: escribe a un temporal y renombra.

    Así, si el servicio lee el fichero mientras se reentrena, nunca ve un joblib a medio escribir.
    """
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_name = tempfile.mkstemp(dir=path.parent, suffix=".tmp")
    os.close(fd)
    try:
        joblib.dump(bundle, tmp_name)
        os.replace(tmp_name, path)
    finally:
        if os.path.exists(tmp_name):
            os.remove(tmp_name)


def load_model(path: Path) -> dict[str, Any] | None:
    """Devuelve el bundle {"pipeline", "classes", ...} o None si no existe / está corrupto."""
    path = Path(path)
    if not path.is_file():
        logger.warning("No hay modelo en %s: el servicio responderá siempre 'Otros' (source=RULE)", path)
        return None
    try:
        bundle = joblib.load(path)
        if not isinstance(bundle, dict) or "pipeline" not in bundle:
            raise ValueError("formato de modelo no reconocido")
        return bundle
    except Exception:  # noqa: BLE001 - un modelo corrupto no debe tumbar el servicio
        logger.exception("No se pudo cargar el modelo %s", path)
        return None
