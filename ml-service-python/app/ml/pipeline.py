"""Definición del pipeline de scikit-learn: TF-IDF + regresión logística."""
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import FeatureUnion, Pipeline


def build_pipeline() -> Pipeline:
    """TF-IDF de palabras + TF-IDF de n-gramas de caracteres -> regresión logística.

    Los n-gramas de caracteres cubren plurales, conjugaciones y erratas ("cenamos"/"cena",
    "restaurantes"/"restaurante"), algo clave con pocos datos de entrenamiento: en validación cruzada
    sobre el dataset semilla mejoran ~7 puntos frente a usar solo palabras.
    """
    features = FeatureUnion(
        [
            (
                "words",
                TfidfVectorizer(
                    lowercase=True,
                    strip_accents="unicode",   # "Nómina" == "nomina"
                    ngram_range=(1, 2),        # palabras sueltas y pares ("menu del", "del dia")
                    sublinear_tf=True,
                ),
            ),
            (
                "chars",
                TfidfVectorizer(
                    lowercase=True,
                    strip_accents="unicode",
                    analyzer="char_wb",        # n-gramas dentro de los límites de cada palabra
                    ngram_range=(3, 5),
                    sublinear_tf=True,
                ),
            ),
        ]
    )
    return Pipeline(
        steps=[
            ("features", features),
            (
                "clf",
                LogisticRegression(
                    max_iter=2000,
                    C=50.0,
                    class_weight="balanced",   # clases con pocos ejemplos no quedan ahogadas
                ),
            ),
        ]
    )
