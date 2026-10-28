"""Respuesta neutra del ML service.

IMPORTANTE (decisión de diseño): las reglas por palabra clave NO viven aquí sino en Spring
(MlFallbackService), porque es Spring quien decide el fallback cuando el ML falla. Este módulo solo
fija el contrato: sin modelo, o ante cualquier error interno, /predict responde SIEMPRE con HTTP 200 y

    {"category": "Otros", "confidence": 0.0, "source": "RULE"}

Spring interpreta source="RULE" como "no es una predicción real" y aplica sus propias reglas.
"""
from app.config import settings
from app.schemas.predict_response import PredictResponse


def neutral_prediction() -> PredictResponse:
    return PredictResponse(category=settings.default_category, confidence=0.0, source="RULE")
