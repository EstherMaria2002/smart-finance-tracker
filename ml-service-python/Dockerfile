FROM python:3.12-slim

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app
COPY models ./models

# Entrena el modelo inicial con el dataset semilla y lo deja en models/category_model.joblib.
# Si en el futuro hay un modelo mejor en models/, esta línea se puede quitar.
RUN python -m app.ml.train

RUN useradd --create-home sft && chown -R sft:sft /app
USER sft

EXPOSE 8000
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
