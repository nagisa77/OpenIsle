# OpenIsle MCP Service

This package hosts a lightweight Python service that exposes OpenIsle search
capabilities through a Model Context Protocol (MCP) compatible HTTP interface.
It currently forwards search requests to the main Spring Boot backend and
returns the aggregated results. The service is intentionally simple so we can
iterate quickly and extend it with additional tools (for example, post
creation) in future updates.

## Local development

```bash
pip install -e ./mcp
openisle-mcp
```

By default the server listens on port `9090` and expects the Spring Boot backend
at `http://localhost:8080`. Configure the behaviour with the following
environment variables:

- `MCP_PORT` – HTTP port the MCP service should listen on (default: `9090`).
- `MCP_HOST` – Bind host for the HTTP server (default: `0.0.0.0`).
- `MCP_BACKEND_BASE_URL` – Base URL of the Spring Boot backend that provides the
  search endpoints (default: `http://springboot:8080`).
- `MCP_CONNECT_TIMEOUT` – Connection timeout (seconds) when calling the backend
  (default: `5`).
- `MCP_READ_TIMEOUT` – Read timeout (seconds) when calling the backend (default:
  `10`).

## Docker

The repository contains a Dockerfile that builds a slim Python image running the
service with `uvicorn`. The compose configuration wires the container into the
existing OpenIsle stack so that deployments automatically start the MCP service.
