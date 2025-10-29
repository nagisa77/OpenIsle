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
| `create_post` | Publish a new post using a JWT token. |
| `reply_to_post` | Create a new comment on a post using a JWT token. |
| `reply_to_comment` | Reply to an existing comment using a JWT token. |
| `recent_posts` | Retrieve posts created within the last *N* minutes. |

The tools return structured data mirroring the backend DTOs, including highlighted snippets for
search results, the full comment payload for post replies and comment replies, and detailed
metadata for recent posts.

