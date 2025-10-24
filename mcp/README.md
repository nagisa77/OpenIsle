# OpenIsle MCP Server

This package provides a [Model Context Protocol](https://github.com/modelcontextprotocol) (MCP) server that exposes the OpenIsle
search capabilities to AI assistants. The server wraps the existing Spring Boot backend and currently provides a single `search`
tool. Future iterations can extend the server with additional functionality such as publishing new posts or moderating content.

## Features

- 🔍 **Global search** — delegates to the existing `/api/search/global` endpoint exposed by the OpenIsle backend.
- 🧠 **Structured results** — responses include highlights and deep links so AI clients can present the results cleanly.
- ⚙️ **Configurable** — point the server at any reachable OpenIsle backend by setting environment variables.

## Local development

```bash
cd mcp
python -m venv .venv
source .venv/bin/activate
pip install -e .
openisle-mcp --transport stdio  # or "sse"/"streamable-http"
```

Environment variables:

| Variable | Description | Default |
| --- | --- | --- |
| `OPENISLE_BACKEND_URL` | Base URL of the Spring Boot backend | `http://springboot:8080` |
| `OPENISLE_BACKEND_TIMEOUT` | Timeout (seconds) for backend HTTP calls | `10` |
| `OPENISLE_PUBLIC_BASE_URL` | Optional base URL used to build deep links in search results | *(unset)* |
| `OPENISLE_MCP_TRANSPORT` | MCP transport (`stdio`, `sse`, `streamable-http`) | `stdio` |
| `OPENISLE_MCP_SSE_MOUNT_PATH` | Mount path when using SSE transport | `/mcp` |
| `FASTMCP_HOST` | Host for SSE / HTTP transports | `127.0.0.1` |
| `FASTMCP_PORT` | Port for SSE / HTTP transports | `8000` |

## Docker

A dedicated Docker image is provided and wired into `docker-compose.yaml`. The container listens on
`${MCP_PORT:-8765}` and connects to the backend service running in the same compose stack.

