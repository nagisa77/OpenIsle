# syntax=docker/dockerfile:1

FROM python:3.11-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

COPY pyproject.toml README.md ./
COPY src ./src

RUN pip install --upgrade pip \
    && pip install --no-cache-dir . \
    && pip cache purge

ENV OPENISLE_MCP_TRANSPORT=http \
    OPENISLE_MCP_HOST=0.0.0.0 \
    OPENISLE_MCP_PORT=8974

EXPOSE 8974

ENTRYPOINT ["openisle-mcp"]
