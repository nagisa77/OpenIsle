"""Entrypoint for the OpenIsle MCP server."""

from __future__ import annotations

import os
from contextlib import asynccontextmanager
from typing import Any

import httpx
from fastmcp import Context, FastMCP

from .client import SearchClient
from .models import SearchResponse, SearchScope
from .settings import Settings

__all__ = ["main"]


def _create_lifespan(settings: Settings):
    @asynccontextmanager
    async def lifespan(app: FastMCP):
        client = SearchClient(settings)
        setattr(app, "_search_client", client)
        try:
            yield {"client": client}
        finally:
            await client.aclose()
            if hasattr(app, "_search_client"):
                delattr(app, "_search_client")

    return lifespan


_settings = Settings.from_env()

mcp = FastMCP(
    name="OpenIsle Search",
    version="0.1.0",
    instructions=(
        "Provides access to OpenIsle search endpoints for retrieving users, posts, "
        "comments, tags, and categories."
    ),
    lifespan=_create_lifespan(_settings),
)


@mcp.tool("search")
async def search(
    keyword: str,
    scope: SearchScope = SearchScope.GLOBAL,
    limit: int | None = None,
    ctx: Context | None = None,
) -> dict[str, Any]:
    """Perform a search against the OpenIsle backend."""

    client = _resolve_client(ctx)
    try:
        response: SearchResponse = await client.search(keyword=keyword, scope=scope, limit=limit)
    except httpx.HTTPError as exc:
        message = f"OpenIsle search request failed: {exc}".rstrip()
        raise RuntimeError(message) from exc

    payload = response.model_dump()
    payload["transport"] = {
        "scope": scope.value,
        "endpoint": client.endpoint_url(scope),
    }
    return payload


def _resolve_client(ctx: Context | None) -> SearchClient:
    app = ctx.fastmcp if ctx is not None else mcp
    client = getattr(app, "_search_client", None)
    if client is None:
        raise RuntimeError("Search client is not initialised; lifespan hook not executed")
    return client


def main() -> None:
    """CLI entrypoint."""

    transport = os.getenv("OPENISLE_MCP_TRANSPORT", "stdio").strip().lower()
    show_banner = os.getenv("OPENISLE_MCP_SHOW_BANNER", "true").lower() in {"1", "true", "yes"}
    run_kwargs: dict[str, Any] = {"show_banner": show_banner}

    if transport in {"http", "sse", "streamable-http"}:
        host = os.getenv("OPENISLE_MCP_HOST", "127.0.0.1")
        port = int(os.getenv("OPENISLE_MCP_PORT", "8974"))
        run_kwargs.update({"host": host, "port": port})

    mcp.run(transport=transport, **run_kwargs)


if __name__ == "__main__":  # pragma: no cover - manual execution guard
    main()
