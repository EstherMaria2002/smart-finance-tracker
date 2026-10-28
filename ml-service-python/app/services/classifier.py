"""Carga el modelo y predice categorías. Nunca lanza excepciones hacia fuera."""
import logging
import threading
from pathlib import Path

from app.config import settings
from app.ml.model_store import load_model
from app.schemas.predict_response import PredictResponse
from app.services.fallback_rules import neutral_prediction

logger = logging.getLogger(__name__)


class CategoryClassifier:
    def __init__(self, model_path: Path):
        self._model_path = Path(model_path)
        self._bundle: dict | None = None
        self._lock = threading.Lock()

    @property
    def is_loaded(self) -> bool:
        return self._bundle is not None

    def load(self) -> bool:
        """(Re)carga el modelo desde disco. Devuelve True si hay modelo disponible."""
        bundle = load_model(self._model_path)
        with self._lock:
            self._bundle = bundle
        return bundle is not None

    def predict(self, description: str, amount: float | None = None) -> PredictResponse:
        """Predice la categoría. Si no hay modelo o algo falla -> respuesta neutra (nunca 500)."""
        bundle = self._bundle  # lectura atómica de la referencia; load() la sustituye entera
        if bundle is None or not description or not description.strip():
            return neutral_prediction()
        try:
            pipeline = bundle["pipeline"]
            probabilities = pipeline.predict_proba([description.strip()])[0]
            best = int(probabilities.argmax())
            return PredictResponse(
                category=str(pipeline.classes_[best]),
                confidence=round(float(probabilities[best]), 3),
                source="ML",
            )
        except Exception:  # noqa: BLE001
            logger.exception("Fallo al predecir; se devuelve la respuesta neutra")
            return neutral_prediction()


# Instancia única de la aplicación (se carga en el arranque, ver main.py).
classifier = CategoryClassifier(settings.category_model_path)
