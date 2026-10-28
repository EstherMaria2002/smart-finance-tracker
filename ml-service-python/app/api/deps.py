"""Dependencias compartidas de la API."""
import logging
import secrets

from fastapi import Header, HTTPException, status

from app.config import settings
from app.services.classifier import CategoryClassifier, classifier

logger = logging.getLogger(__name__)


def require_api_key(
    x_internal_api_key: str | None = Header(default=None, alias="X-Internal-Api-Key"),
) -> None:
    """Exige la cabecera X-Internal-Api-Key (la misma que envía el backend Spring).

    - Falta o es incorrecta -> 403.
    - Si el servicio no tiene clave configurada, rechaza todo (fail-closed) en vez de quedar abierto.
    - La comparación es en tiempo constante para no filtrar información por timing.
    """
    expected = settings.internal_api_key
    if not expected:
        logger.error("INTERNAL_API_KEY no configurada: se rechazan las peticiones protegidas")
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")
    if not x_internal_api_key or not secrets.compare_digest(
        x_internal_api_key.encode("utf-8"), expected.encode("utf-8")
    ):
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Forbidden")


def get_classifier() -> CategoryClassifier:
    return classifier
