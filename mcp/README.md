# OpenIsle MCP Server

This package provides a Python implementation of a Model Context Protocol (MCP) server for OpenIsle. The server focuses on the community search APIs so that AI assistants and other MCP-aware clients can discover OpenIsle users, posts, categories, comments, and tags. Additional capabilities such as content creation tools can be layered on later without changing the transport or deployment model.

## Features

- ✅ Implements the MCP tooling interface using [FastMCP](https://github.com/modelcontextprotocol/fastmcp).
- 🔍 Exposes a `search` tool that proxies requests to the existing OpenIsle REST endpoints and normalises the response payload.
- ⚙️ Configurable through environment variables for API base URL, timeout, result limits, and snippet size.
- 🐳 Packaged with a Docker image so it can be launched alongside the other OpenIsle services.

## Environment variables

| Variable | Default | Description |
| --- | --- | --- |
| `OPENISLE_API_BASE_URL` | `http://springboot:8080` | Base URL of the OpenIsle backend REST API. |
| `OPENISLE_API_TIMEOUT` | `10` | Timeout (in seconds) used when calling the backend search endpoints. |
| `OPENISLE_MCP_DEFAULT_LIMIT` | `20` | Default maximum number of search results to return when the caller does not provide a limit. Use `0` or a negative number to disable limiting. |
| `OPENISLE_MCP_SNIPPET_LENGTH` | `160` | Maximum length (in characters) of the normalised summary snippet returned by the MCP tool. |
| `OPENISLE_MCP_TRANSPORT` | `stdio` | Transport used when running the server directly. Set to `http` when running inside Docker. |
| `OPENISLE_MCP_HOST` | `127.0.0.1` | Bind host used when the transport is HTTP/SSE. |
| `OPENISLE_MCP_PORT` | `8974` | Bind port used when the transport is HTTP/SSE. |

## Local development

```bash
cd mcp
python -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -e .
OPENISLE_API_BASE_URL=http://localhost:8080 OPENISLE_MCP_TRANSPORT=http openisle-mcp
```

By default the server listens over stdio, which is useful when integrating with MCP-aware IDEs. When the `OPENISLE_MCP_TRANSPORT` variable is set to `http` the server will expose an HTTP transport on `OPENISLE_MCP_HOST:OPENISLE_MCP_PORT`.

## Docker image

The accompanying `Dockerfile` builds a minimal image that installs the package and starts the MCP server. The root Docker Compose manifest is configured to launch this service and connect it to the same internal network as the Spring Boot API so the MCP tools can reach the search endpoints.

## MCP tool contract

The `search` tool accepts the following arguments:

- `keyword` (string, required): Search phrase passed directly to the OpenIsle API.
- `scope` (string, optional): One of `global`, `posts`, `posts_content`, `posts_title`, or `users`. Defaults to `global`.
- `limit` (integer, optional): Overrides the default limit from `OPENISLE_MCP_DEFAULT_LIMIT`.

The tool returns a JSON object containing both the raw API response and a normalised representation with concise titles, subtitles, and snippets for each result.

Future tools (for example posting or moderation helpers) can be added within this package and exposed via additional decorators without changing the deployment setup.
