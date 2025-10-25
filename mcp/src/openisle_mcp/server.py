"""Entry point for the OpenIsle MCP server."""

from __future__ import annotations

import logging
import os
from typing import Annotated

from mcp.server.fastmcp import Context, FastMCP
from mcp.server.fastmcp.logging import configure_logging
from pydantic import Field
from starlette.requests import Request
from starlette.responses import JSONResponse, Response

from .client import OpenIsleAPI
from .config import Settings, get_settings
from .models import SearchResponse, SearchScope
from .search import normalise_results

_logger = logging.getLogger(__name__)


def _create_server(settings: Settings) -> FastMCP:
    """Instantiate the FastMCP server with configured metadata."""

    server = FastMCP(
        name="OpenIsle MCP",
        instructions=(
            "Access OpenIsle search functionality. Provide a keyword and optionally a scope to "
            "discover users and posts from the community."
        ),
        host=settings.host,
        port=settings.port,
        transport_security=None,
    )

    @server.custom_route("/health", methods=["GET"])
    async def health(_: Request) -> Response:  # pragma: no cover - exercised via runtime checks
        return JSONResponse({"status": "ok"})

    return server


async def _execute_search(
    *,
    api: OpenIsleAPI,
    scope: SearchScope,
    keyword: str,
    context: Context | None,
) -> SearchResponse:
    message = f"Searching OpenIsle scope={scope.value} keyword={keyword!r}"
    if context is not None:
        context.info(message)
    else:
        _logger.info(message)

    payload = await api.search(scope, keyword)
    items = normalise_results(scope, payload)
    return SearchResponse(scope=scope, keyword=keyword, results=items)


def build_server(settings: Settings | None = None) -> FastMCP:
    """Configure and return the FastMCP server instance."""

    resolved_settings = settings or get_settings()
    server = _create_server(resolved_settings)
    api_client = OpenIsleAPI(resolved_settings)

    @server.tool(
        name="openisle_search",
        description="Search OpenIsle for users and posts.",
    )
    async def openisle_search(
        keyword: Annotated[str, Field(description="Keyword used to query OpenIsle search.")],
        scope: Annotated[
            SearchScope,
            Field(
                description=(
                    "Scope of the search. Use 'global' to search across users and posts, or specify "
                    "'users', 'posts', 'posts_title', or 'posts_content' to narrow the results."
                )
            ),
        ] = SearchScope.GLOBAL,
        context: Context | None = None,
    ) -> SearchResponse:
        try:
            return await _execute_search(api=api_client, scope=scope, keyword=keyword, context=context)
        except Exception as exc:  # pragma: no cover - surfaced to the MCP runtime
            error_message = f"Search failed: {exc}"
            if context is not None:
                context.error(error_message)
            _logger.exception("Search tool failed")
            raise

    return server


def main() -> None:
    """CLI entry point used by the console script."""

    settings = get_settings()
    configure_logging("INFO")
    server = build_server(settings)

    transport = os.getenv("OPENISLE_MCP_TRANSPORT", settings.transport)
    if transport not in {"stdio", "sse", "streamable-http"}:
        raise RuntimeError(f"Unsupported transport mode: {transport}")

    _logger.info("Starting OpenIsle MCP server on %s:%s via %s", settings.host, settings.port, transport)

    if transport == "stdio":
        server.run("stdio")
    elif transport == "sse":
        mount_path = os.getenv("OPENISLE_MCP_SSE_PATH")
        server.run("sse", mount_path=mount_path)
    else:
        server.run("streamable-http")


if __name__ == "__main__":  # pragma: no cover - manual execution path
    main()
