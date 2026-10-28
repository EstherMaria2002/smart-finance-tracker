"""Configuración del ML service (variables de entorno / fichero .env)."""
from pathlib import Path

from pydantic_settings import BaseSettings, SettingsConfigDict

BASE_DIR = Path(__file__).resolve().parent.parent


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    # Clave compartida con el backend Spring (cabecera X-Internal-Api-Key).
    # Si está vacía, el servicio rechaza TODAS las peticiones protegidas (fail-closed).
    internal_api_key: str = ""

    # Rutas del artefacto entrenado y del dataset semilla.
    category_model_path: Path = BASE_DIR / "models" / "category_model.joblib"
    seed_data_path: Path = BASE_DIR / "app" / "data" / "seed_transactions.csv"

    # Categoría neutra cuando no hay modelo o algo falla. Debe existir en la BD (V2__seed_categories.sql).
    default_category: str = "Otros"


settings = Settings()
