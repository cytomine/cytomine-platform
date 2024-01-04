FROM python:3.9-slim-bullseye

RUN apt-get update \
    && apt-get install -y --no-install-recommends git \
    && rm -rf /var/lib/apt/lists/*

COPY cbir /app/cbir
COPY pyproject.toml poetry.lock README.md /app

WORKDIR /app

RUN pip install --no-cache-dir --upgrade --extra-index-url https://download.pytorch.org/whl/cu118 .

CMD ["uvicorn", "cbir.app:app", "--host", "0.0.0.0", "--port", "6000"]
