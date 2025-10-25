"""HTTP client helpers for talking to the Spring Boot backend."""

from __future__ import annotations

import logging
from typing import Any

import httpx

from .config import Settings

LOGGER = logging.getLogger(__name__)


class SearchClient:
    """Wrapper around :class:`httpx.AsyncClient` for search operations."""

    def __init__(self, settings: Settings):
        timeout = httpx.Timeout(
            connect=settings.connect_timeout,
            read=settings.read_timeout,
            write=settings.read_timeout,
            pool=None,
        )
        self._client = httpx.AsyncClient(
            base_url=settings.normalized_backend_base_url,
            timeout=timeout,
        )

    async def close(self) -> None:
        await self._client.aclose()

    async def global_search(self, keyword: str) -> list[dict[str, Any]]:
        LOGGER.debug("Performing global search for keyword '%s'", keyword)
        response = await self._client.get("/api/search/global", params={"keyword": keyword})
        response.raise_for_status()
        payload = response.json()
        if isinstance(payload, list):
            return payload
        LOGGER.warning("Unexpected payload type from backend: %s", type(payload))
        return []


__all__ = ["SearchClient"]
