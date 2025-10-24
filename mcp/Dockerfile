FROM python:3.11-slim AS runtime

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

COPY mcp/pyproject.toml /app/pyproject.toml
COPY mcp/README.md /app/README.md
COPY mcp/src /app/src

RUN pip install --upgrade pip \
    && pip install .

EXPOSE 8765

CMD ["openisle-mcp"]
