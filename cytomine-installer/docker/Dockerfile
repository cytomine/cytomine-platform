FROM python:3.12-rc-slim-bullseye

RUN apt-get upgrade \
    && apt-get install openssl \
    && apt-get clean

RUN python -m pip install --upgrade pip

WORKDIR /app
COPY setup.cfg /app/setup.cfg
COPY setup.py /app/setup.py
COPY README.md /app/README.md
COPY requirements.txt /app/requirements.txt
COPY cytomine_installer /app/cytomine_installer
RUN pip install -e .

ENTRYPOINT ["ctmn"]
