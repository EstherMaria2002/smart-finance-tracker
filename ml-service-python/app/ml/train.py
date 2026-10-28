"""Entrenamiento del clasificador de categorías.

Uso:
    python -m app.ml.train                       # dataset semilla -> models/category_model.joblib
    python -m app.ml.train --data otro.csv       # otro CSV con columnas description,category
"""
import argparse
import logging
from datetime import datetime, timezone
from pathlib import Path

import pandas as pd
from sklearn.model_selection import StratifiedKFold, cross_val_score

from app.config import settings
from app.ml.model_store import save_model
from app.ml.pipeline import build_pipeline

logger = logging.getLogger(__name__)


def load_dataset(path: Path) -> pd.DataFrame:
    df = pd.read_csv(path, usecols=["description", "category"])
    return clean_dataset(df)


def clean_dataset(df: pd.DataFrame) -> pd.DataFrame:
    df = df.dropna(subset=["description", "category"]).copy()
    df["description"] = df["description"].astype(str).str.strip()
    df["category"] = df["category"].astype(str).str.strip()
    return df[(df["description"] != "") & (df["category"] != "")].reset_index(drop=True)


def cross_validated_accuracy(df: pd.DataFrame) -> float | None:
    """Precisión media con validación cruzada estratificada (None si hay muy pocos datos)."""
    min_per_class = int(df["category"].value_counts().min())
    n_splits = min(5, min_per_class)
    if n_splits < 2:
        return None
    cv = StratifiedKFold(n_splits=n_splits, shuffle=True, random_state=42)
    scores = cross_val_score(build_pipeline(), df["description"], df["category"], cv=cv, scoring="accuracy")
    return float(scores.mean())


def train_and_save(df: pd.DataFrame, output_path: Path) -> dict:
    """Entrena con TODO el dataset, guarda el modelo y devuelve métricas."""
    if df["category"].nunique() < 2:
        raise ValueError("Se necesitan al menos 2 categorías distintas para entrenar")

    accuracy = cross_validated_accuracy(df)
    pipeline = build_pipeline()
    pipeline.fit(df["description"], df["category"])

    metrics = {
        "samples": int(len(df)),
        "classes": [str(c) for c in pipeline.classes_],
        "cv_accuracy": accuracy,
    }
    save_model(
        {
            "pipeline": pipeline,
            "classes": metrics["classes"],
            "trained_at": datetime.now(timezone.utc).isoformat(),
            **metrics,
        },
        output_path,
    )
    return metrics


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")
    parser = argparse.ArgumentParser(description="Entrena el modelo de categorías")
    parser.add_argument("--data", type=Path, default=settings.seed_data_path)
    parser.add_argument("--output", type=Path, default=settings.category_model_path)
    args = parser.parse_args()

    df = load_dataset(args.data)
    metrics = train_and_save(df, args.output)
    acc = "n/d" if metrics["cv_accuracy"] is None else f"{metrics['cv_accuracy']:.1%}"
    logger.info("Modelo guardado en %s | muestras=%d | clases=%d | precisión CV=%s",
                args.output, metrics["samples"], len(metrics["classes"]), acc)


if __name__ == "__main__":
    main()
