from typing import Literal

from pydantic import BaseModel, Field


class PredictResponse(BaseModel):
    """Respuesta de POST /predict.

    source == "ML"   -> predicción real del modelo.
    source == "RULE" -> no hay modelo / hubo un fallo: respuesta neutra ("Otros", 0.0).
    """

    category: str
    confidence: float = Field(..., ge=0.0, le=1.0)
    source: Literal["ML", "RULE"]
