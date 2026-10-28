from pydantic import BaseModel, Field


class PredictRequest(BaseModel):
    """Cuerpo de POST /predict."""

    description: str = Field(..., min_length=1, max_length=255, examples=["Cena en restaurante italiano"])
    # El modelo v1 solo usa el texto; el importe se acepta para poder incorporarlo en versiones futuras.
    amount: float | None = Field(default=None, examples=[42.50])
