# OpenIsle MCP Server

This package exposes a [Model Context Protocol](https://github.com/modelcontextprotocol) (MCP) server for OpenIsle.
The initial release focuses on surfacing the platform's search capabilities so that AI assistants can discover
users and posts directly through the existing REST API. Future iterations can expand this service with post
creation and other productivity tools.

## Features

- 🔍 Keyword search across users and posts using the OpenIsle backend APIs
- ✅ Structured MCP tool response for downstream reasoning
- 🩺 Lightweight health check endpoint (`/health`) for container orchestration
- ⚙️ Configurable via environment variables with sensible defaults for Docker Compose

## Running locally

```bash
cd mcp
pip install .
openisle-mcp  # starts the MCP server on http://127.0.0.1:8000 by default
```

By default the server targets `http://localhost:8080` for backend requests. Override the target by setting
`OPENISLE_API_BASE_URL` before starting the service.

## Environment variables

| Variable | Default | Description |
| -------- | ------- | ----------- |
| `OPENISLE_API_BASE_URL` | `http://localhost:8080` | Base URL of the OpenIsle backend API |
| `OPENISLE_MCP_HOST` | `127.0.0.1` | Hostname/interface for the MCP HTTP server |
| `OPENISLE_MCP_PORT` | `8000` | Port for the MCP HTTP server |
| `OPENISLE_MCP_TRANSPORT` | `streamable-http` | Transport mode (`stdio`, `sse`, or `streamable-http`) |
| `OPENISLE_MCP_TIMEOUT_SECONDS` | `10` | HTTP timeout when calling the backend |

## Docker

The repository's Docker Compose stack now includes the MCP server. To start it alongside other services:

```bash
cd docker
docker compose --profile dev up mcp-server
```

The service exposes port `8000` by default. Update `OPENISLE_MCP_PORT` to customize the mapped port.
