FROM python:3.11-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY mcp/pyproject.toml mcp/README.md ./
COPY mcp/src ./src

RUN pip install --no-cache-dir --upgrade pip \
    && pip install --no-cache-dir .

ENV OPENISLE_MCP_HOST=0.0.0.0 \
    OPENISLE_MCP_PORT=8085 \
    OPENISLE_MCP_TRANSPORT=streamable-http

EXPOSE 8085

CMD ["openisle-mcp"]

