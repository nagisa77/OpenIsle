"""FastAPI application exposing the MCP server endpoints."""

from __future__ import annotations

import logging

from fastapi import Depends, FastAPI, HTTPException, Query, Request
import httpx

from .client import SearchClient
from .config import get_settings
from .models import SearchResponse, SearchResult

LOGGER = logging.getLogger(__name__)


async def _lifespan(app: FastAPI):
    settings = get_settings()
    client = SearchClient(settings)
    app.state.settings = settings
    app.state.search_client = client
    LOGGER.info(
        "Starting MCP server on %s:%s targeting backend %s",
        settings.host,
        settings.port,
        settings.normalized_backend_base_url,
    )
    try:
        yield
    finally:
        LOGGER.info("Shutting down MCP server")
        await client.close()


def create_app() -> FastAPI:
    """Create and configure the FastAPI application."""

    app = FastAPI(title="OpenIsle MCP Server", lifespan=_lifespan)

    @app.get("/healthz", tags=["health"])
    async def healthcheck() -> dict[str, str]:
        return {"status": "ok"}

    async def get_client(request: Request) -> SearchClient:
        return request.app.state.search_client

    @app.get("/search", response_model=SearchResponse, tags=["search"])
    async def search(
        keyword: str = Query(..., min_length=1, description="Keyword to search for"),
        client: SearchClient = Depends(get_client),
    ) -> SearchResponse:
        try:
            raw_results = await client.global_search(keyword)
        except httpx.HTTPStatusError as exc:
            LOGGER.warning("Backend responded with error %s", exc.response.status_code)
            raise HTTPException(status_code=exc.response.status_code, detail="Backend error") from exc
        except httpx.HTTPError as exc:
            LOGGER.error("Failed to reach backend: %s", exc)
            raise HTTPException(status_code=503, detail="Search service unavailable") from exc
        results = [SearchResult.model_validate(item) for item in raw_results]
        return SearchResponse(results=results)

    return app


__all__ = ["create_app"]
