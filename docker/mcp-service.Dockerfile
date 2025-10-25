FROM python:3.11-slim AS base

ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1

WORKDIR /app

COPY mcp/pyproject.toml mcp/README.md ./
COPY mcp/src ./src
RUN pip install --upgrade pip \
    && pip install .

EXPOSE 8000

ENV OPENISLE_API_BASE_URL=http://springboot:8080 \
    OPENISLE_MCP_HOST=0.0.0.0 \
    OPENISLE_MCP_PORT=8000 \
    OPENISLE_MCP_TRANSPORT=streamable-http

CMD ["openisle-mcp"]
