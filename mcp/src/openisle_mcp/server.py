"""Entry point for the OpenIsle MCP server."""

from __future__ import annotations

import argparse
import logging
import os
from typing import Annotated, Optional

from mcp.server.fastmcp import Context, FastMCP
from mcp.server.fastmcp import exceptions as mcp_exceptions
from pydantic import Field

from .client import BackendClientError, OpenIsleBackendClient
from .models import BackendSearchResult, SearchResponse, SearchResult

logger = logging.getLogger(__name__)

APP_NAME = "openisle-mcp"
DEFAULT_BACKEND_URL = "http://springboot:8080"
DEFAULT_TRANSPORT = "stdio"
DEFAULT_TIMEOUT = 10.0
DEFAULT_LIMIT = 20
MAX_LIMIT = 50

server = FastMCP(
    APP_NAME,
    instructions=(
        "Use the `search` tool to query OpenIsle content. "
        "Results include posts, comments, users, categories, and tags."
    ),
)


def _env(name: str, default: Optional[str] = None) -> Optional[str]:
    value = os.getenv(name, default)
    if value is None:
        return None
    trimmed = value.strip()
    return trimmed or default


def _load_timeout() -> float:
    raw = _env("OPENISLE_BACKEND_TIMEOUT", str(DEFAULT_TIMEOUT))
    try:
        timeout = float(raw) if raw is not None else DEFAULT_TIMEOUT
    except ValueError:
        logger.warning("Invalid OPENISLE_BACKEND_TIMEOUT value '%s', falling back to %s", raw, DEFAULT_TIMEOUT)
        return DEFAULT_TIMEOUT
    if timeout <= 0:
        logger.warning("Non-positive OPENISLE_BACKEND_TIMEOUT %s, falling back to %s", timeout, DEFAULT_TIMEOUT)
        return DEFAULT_TIMEOUT
    return timeout


_BACKEND_CLIENT = OpenIsleBackendClient(
    base_url=_env("OPENISLE_BACKEND_URL", DEFAULT_BACKEND_URL) or DEFAULT_BACKEND_URL,
    timeout=_load_timeout(),
)
_PUBLIC_BASE_URL = _env("OPENISLE_PUBLIC_BASE_URL")


def _build_url(result: BackendSearchResult) -> Optional[str]:
    if not _PUBLIC_BASE_URL:
        return None
    base = _PUBLIC_BASE_URL.rstrip("/")
    if result.type in {"post", "post_title"} and result.id is not None:
        return f"{base}/posts/{result.id}"
    if result.type == "comment" and result.post_id is not None:
        anchor = f"#comment-{result.id}" if result.id is not None else ""
        return f"{base}/posts/{result.post_id}{anchor}"
    if result.type == "user" and result.id is not None:
        return f"{base}/users/{result.id}"
    if result.type == "category" and result.id is not None:
        return f"{base}/?categoryId={result.id}"
    if result.type == "tag" and result.id is not None:
        return f"{base}/?tagIds={result.id}"
    return None


def _to_search_result(result: BackendSearchResult) -> SearchResult:
    highlights = {
        "text": result.highlighted_text,
        "subText": result.highlighted_sub_text,
        "extra": result.highlighted_extra,
    }
    # Remove empty highlight entries to keep the payload clean
    highlights = {key: value for key, value in highlights.items() if value}
    return SearchResult(
        type=result.type,
        id=result.id,
        title=result.text,
        subtitle=result.sub_text,
        extra=result.extra,
        post_id=result.post_id,
        url=_build_url(result),
        highlights=highlights,
    )


KeywordParam = Annotated[str, Field(description="Keyword to search for", min_length=1)]
LimitParam = Annotated[
    int,
    Field(ge=1, le=MAX_LIMIT, description=f"Maximum number of results to return (<= {MAX_LIMIT})"),
]


@server.tool(name="search", description="Search OpenIsle content")
async def search(keyword: KeywordParam, limit: LimitParam = DEFAULT_LIMIT, ctx: Optional[Context] = None) -> SearchResponse:
    """Run a search query against the OpenIsle backend."""

    trimmed = keyword.strip()
    if not trimmed:
        raise mcp_exceptions.ToolError("Keyword must not be empty")

    if ctx is not None:
        await ctx.debug(f"Searching OpenIsle for '{trimmed}' (limit={limit})")

    try:
        raw_results = await _BACKEND_CLIENT.search_global(trimmed)
    except BackendClientError as exc:
        if ctx is not None:
            await ctx.error(f"Search request failed: {exc}")
        raise mcp_exceptions.ToolError(f"Search failed: {exc}") from exc

    results = [_to_search_result(result) for result in raw_results]
    limited = results[:limit]

    if ctx is not None:
        await ctx.info(
            "Search completed",
            keyword=trimmed,
            total_results=len(results),
            returned=len(limited),
        )

    return SearchResponse(keyword=trimmed, total_results=len(results), limit=limit, results=limited)


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the OpenIsle MCP server")
    parser.add_argument(
        "--transport",
        choices=["stdio", "sse", "streamable-http"],
        default=_env("OPENISLE_MCP_TRANSPORT", DEFAULT_TRANSPORT),
        help="Transport protocol to use",
    )
    parser.add_argument(
        "--mount-path",
        default=_env("OPENISLE_MCP_SSE_MOUNT_PATH", "/mcp"),
        help="Mount path when using the SSE transport",
    )
    args = parser.parse_args()

    logging.basicConfig(level=os.getenv("OPENISLE_MCP_LOG_LEVEL", "INFO"))
    logger.info(
        "Starting OpenIsle MCP server", extra={"transport": args.transport, "backend": _BACKEND_CLIENT.base_url}
    )

    server.run(transport=args.transport, mount_path=args.mount_path)


if __name__ == "__main__":
    main()
