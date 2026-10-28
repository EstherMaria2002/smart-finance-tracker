from pydantic import BaseModel, Field


class TrainingSample(BaseModel):
    description: str = Field(..., min_length=1, max_length=255)
    category: str = Field(..., min_length=1, max_length=80)


class RetrainRequest(BaseModel):
    """Muestras adicionales (p. ej. correcciones de usuarios) que se suman al dataset semilla."""

    extra_samples: list[TrainingSample] = Field(default_factory=list)


class RetrainResponse(BaseModel):
    status: str
    samples: int
    classes: list[str]
    cv_accuracy: float | None
