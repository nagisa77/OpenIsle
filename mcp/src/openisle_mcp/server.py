"""Entry point for running the OpenIsle MCP server."""

from __future__ import annotations

from contextlib import asynccontextmanager
from typing import Annotated

import httpx
from mcp.server.fastmcp import Context, FastMCP
from pydantic import ValidationError
from pydantic import Field as PydanticField

from .config import get_settings
from .schemas import SearchResponse, SearchResultItem
from .search_client import SearchClient

settings = get_settings()
search_client = SearchClient(
    str(settings.backend_base_url), timeout=settings.request_timeout
)


@asynccontextmanager
async def lifespan(_: FastMCP):
    """Lifecycle hook that disposes shared resources when the server stops."""

    try:
        yield
    finally:
        await search_client.aclose()


app = FastMCP(
    name="openisle-mcp",
    instructions=(
        "Use this server to search OpenIsle posts, users, tags, categories, and comments "
        "via the global search endpoint."
    ),
    host=settings.host,
    port=settings.port,
    lifespan=lifespan,
)


@app.tool(
    name="search",
    description="Perform a global search across OpenIsle resources.",
    structured_output=True,
)
async def search(
    keyword: Annotated[str, PydanticField(description="Keyword to search for.")],
    ctx: Context | None = None,
) -> SearchResponse:
    """Call the OpenIsle global search endpoint and return structured results."""

    sanitized = keyword.strip()
    if not sanitized:
        raise ValueError("Keyword must not be empty.")

    try:
        raw_results = await search_client.global_search(sanitized)
    except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors
        message = (
            "OpenIsle backend returned HTTP "
            f"{exc.response.status_code} while searching for '{sanitized}'."
        )
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc
    except httpx.RequestError as exc:  # pragma: no cover - network errors
        message = f"Unable to reach OpenIsle backend search service: {exc}."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    try:
        results = [SearchResultItem.model_validate(entry) for entry in raw_results]
    except ValidationError as exc:
        message = "Received malformed data from the OpenIsle backend search endpoint."
        if ctx is not None:
            await ctx.error(message)
        raise ValueError(message) from exc

    if ctx is not None:
        await ctx.info(f"Search keyword '{sanitized}' returned {len(results)} results.")

    return SearchResponse(keyword=sanitized, total=len(results), results=results)


def main() -> None:
    """Run the MCP server using the configured transport."""

    app.run(transport=settings.transport)


if __name__ == "__main__":  # pragma: no cover - manual execution
    main()

