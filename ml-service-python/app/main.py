import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI

from app.api.routes import protected_router, public_router
from app.config import settings
from app.services.classifier import classifier

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
logger = logging.getLogger("sft.ml")


@asynccontextmanager
async def lifespan(_: FastAPI):
    if not settings.internal_api_key:
        logger.error("INTERNAL_API_KEY vacía: /predict y /retrain responderán 403 hasta que se configure")
    if classifier.load():
        logger.info("Modelo cargado desde %s", settings.category_model_path)
    else:
        logger.warning("Arrancando SIN modelo: /predict devolverá 'Otros' con source=RULE")
    yield


# Servicio SOLO interno: no tiene CORS a propósito (el navegador nunca debe hablar con él).
app = FastAPI(title="SFT ML Service", version="0.1.0", lifespan=lifespan)
app.include_router(public_router)
app.include_router(protected_router)
