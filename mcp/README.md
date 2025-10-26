# OpenIsle MCP Server

This package provides a [Model Context Protocol](https://modelcontextprotocol.io) (MCP) server
that exposes OpenIsle's search capabilities as MCP tools. The initial release focuses on the
global search endpoint so the agent ecosystem can retrieve relevant posts, users, tags, and
other resources.

## Configuration

The server is configured through environment variables (all prefixed with `OPENISLE_MCP_`):

| Variable | Default | Description |
| --- | --- | --- |
| `BACKEND_BASE_URL` | `http://springboot:8080` | Base URL of the OpenIsle backend. |
| `PORT` | `8085` | TCP port when running with the `streamable-http` transport. |
| `HOST` | `0.0.0.0` | Interface to bind when serving HTTP. |
| `TRANSPORT` | `streamable-http` | Transport to use (`stdio`, `sse`, or `streamable-http`). |
| `REQUEST_TIMEOUT` | `10.0` | Timeout (seconds) for backend HTTP requests. |

## Running locally

```bash
pip install .
OPENISLE_MCP_BACKEND_BASE_URL="http://localhost:8080" openisle-mcp
```

By default the server listens on port `8085` and serves MCP over Streamable HTTP.

## Available tools

| Tool | Description |
| --- | --- |
| `search` | Perform a global search against the OpenIsle backend. |

The tool returns structured data describing each search hit including highlighted snippets when
provided by the backend.

