import logging

from fastapi import APIRouter, Depends, HTTPException

from app.api.deps import get_classifier, require_api_key
from app.config import settings
from app.ml.train import clean_dataset, load_dataset, train_and_save
from app.schemas.predict_request import PredictRequest
from app.schemas.predict_response import PredictResponse
from app.schemas.retrain import RetrainRequest, RetrainResponse
from app.services.classifier import CategoryClassifier
from app.services.fallback_rules import neutral_prediction

logger = logging.getLogger(__name__)

# Público: lo usa el healthcheck de Docker (no lleva API key).
public_router = APIRouter()

# Protegido: TODAS sus rutas exigen X-Internal-Api-Key (403 si falta o no coincide).
protected_router = APIRouter(dependencies=[Depends(require_api_key)])


@public_router.get("/health")
def health(clf: CategoryClassifier = Depends(get_classifier)) -> dict:
    # Siempre 200 mientras el proceso viva: sin modelo el servicio sigue siendo "healthy"
    # (responde la categoría neutra), solo lo indicamos en model_loaded.
    return {"status": "ok", "model_loaded": clf.is_loaded}


@protected_router.post("/predict", response_model=PredictResponse)
def predict(req: PredictRequest, clf: CategoryClassifier = Depends(get_classifier)) -> PredictResponse:
    """Predice la categoría. Contrato: SIEMPRE 200 con {category, confidence, source}."""
    try:
        return clf.predict(req.description, req.amount)
    except Exception:  # noqa: BLE001 - doble red de seguridad: nunca un 500 aquí
        logger.exception("Error inesperado en /predict")
        return neutral_prediction()


@protected_router.post("/retrain", response_model=RetrainResponse)
def retrain(
    body: RetrainRequest | None = None,
    clf: CategoryClassifier = Depends(get_classifier),
) -> RetrainResponse:
    """(Fase 3/6, opcional) Reentrena con el dataset semilla + muestras extra y recarga el modelo."""
    import pandas as pd  # import local: solo se necesita aquí

    try:
        df = load_dataset(settings.seed_data_path)
        if body and body.extra_samples:
            extra = pd.DataFrame([s.model_dump() for s in body.extra_samples])
            df = clean_dataset(pd.concat([df, extra], ignore_index=True))
        metrics = train_and_save(df, settings.category_model_path)
        clf.load()
    except Exception as exc:  # noqa: BLE001
        logger.exception("Fallo al reentrenar")
        # El modelo anterior sigue cargado en memoria: el servicio no se degrada.
        raise HTTPException(status_code=500, detail=f"No se pudo reentrenar: {exc}") from exc

    return RetrainResponse(status="ok", **metrics)
