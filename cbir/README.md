# Cytomine CBIR

Content Based Image Retrieval Server

# Requirements

Python 3.9+

# Development

## Setup Redis database

```bash
docker compose up -d
```

## Run the server using an virtual environment

### Using Python venv

```bash
python -m venv .venv
source .venv/bin/activate
pip install . --extra-index-url=https://download.pytorch.org/whl/cu118
uvicorn cbir.app:app --reload
```

### Using Poetry

```bash
poetry shell
poetry install
uvicorn cbir.app:app --reload
```

# License

Apache 2.0

# Contributions

The code regarding the [retrieval](https://github.com/Cytomine-ULiege/Cytomine-cbir/tree/main/cbir/retrieval) and the [deep learning models](https://github.com/Cytomine-ULiege/Cytomine-cbir/tree/main/cbir/models) are based on the following repositories:
- <https://github.com/AxelleSchyns/cbir-tfe>
- <https://github.com/AxelleSchyns/WP1>
