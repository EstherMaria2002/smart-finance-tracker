"""Tests del ML service. Ejecutar:  pip install -r requirements-dev.txt && pytest"""
import pytest
from fastapi.testclient import TestClient

from app.api.deps import get_classifier
from app.config import settings
from app.main import app
from app.ml.train import load_dataset, train_and_save
from app.services.classifier import CategoryClassifier

API_KEY = "test-key"
HEADERS = {"X-Internal-Api-Key": API_KEY}


@pytest.fixture(autouse=True)
def configure_key(monkeypatch):
    monkeypatch.setattr(settings, "internal_api_key", API_KEY)


@pytest.fixture(scope="module")
def trained_classifier(tmp_path_factory):
    path = tmp_path_factory.mktemp("models") / "category_model.joblib"
    train_and_save(load_dataset(settings.seed_data_path), path)
    clf = CategoryClassifier(path)
    assert clf.load()
    return clf


@pytest.fixture
def client_with_model(trained_classifier):
    app.dependency_overrides[get_classifier] = lambda: trained_classifier
    yield TestClient(app)
    app.dependency_overrides.clear()


@pytest.fixture
def client_without_model(tmp_path):
    empty = CategoryClassifier(tmp_path / "no-existe.joblib")
    empty.load()
    app.dependency_overrides[get_classifier] = lambda: empty
    yield TestClient(app)
    app.dependency_overrides.clear()


def test_health_needs_no_api_key(client_without_model):
    res = client_without_model.get("/health")
    assert res.status_code == 200
    assert res.json() == {"status": "ok", "model_loaded": False}


def test_predict_without_api_key_is_403(client_with_model):
    res = client_with_model.post("/predict", json={"description": "Cena", "amount": 10})
    assert res.status_code == 403


def test_predict_with_wrong_api_key_is_403(client_with_model):
    res = client_with_model.post("/predict", json={"description": "Cena"}, headers={"X-Internal-Api-Key": "mala"})
    assert res.status_code == 403


def test_retrain_without_api_key_is_403(client_with_model):
    assert client_with_model.post("/retrain").status_code == 403


def test_predict_returns_real_prediction(client_with_model):
    res = client_with_model.post(
        "/predict", json={"description": "Cena en restaurante italiano", "amount": 42.5}, headers=HEADERS
    )
    body = res.json()
    assert res.status_code == 200
    assert body["category"] == "Restaurantes"
    assert body["source"] == "ML"
    assert 0.0 < body["confidence"] <= 1.0


def test_predict_without_model_returns_neutral_200(client_without_model):
    res = client_without_model.post("/predict", json={"description": "Cena", "amount": 10}, headers=HEADERS)
    assert res.status_code == 200
    assert res.json() == {"category": "Otros", "confidence": 0.0, "source": "RULE"}


def test_predict_never_returns_500_when_model_breaks(tmp_path):
    class BrokenPipeline:
        def predict_proba(self, _):
            raise RuntimeError("boom")

    clf = CategoryClassifier(tmp_path / "x.joblib")
    clf._bundle = {"pipeline": BrokenPipeline()}
    app.dependency_overrides[get_classifier] = lambda: clf
    try:
        res = TestClient(app).post("/predict", json={"description": "Cena"}, headers=HEADERS)
    finally:
        app.dependency_overrides.clear()
    assert res.status_code == 200
    assert res.json() == {"category": "Otros", "confidence": 0.0, "source": "RULE"}


def test_service_without_configured_key_rejects_everything(client_with_model, monkeypatch):
    monkeypatch.setattr(settings, "internal_api_key", "")
    res = client_with_model.post("/predict", json={"description": "Cena"}, headers=HEADERS)
    assert res.status_code == 403
