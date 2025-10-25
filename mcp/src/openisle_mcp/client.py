"""HTTP client helpers for interacting with the OpenIsle backend APIs."""

from __future__ import annotations

from typing import Any

import httpx

from .config import Settings, get_settings
from .models import SearchScope


class OpenIsleAPI:
    """Thin wrapper around the OpenIsle REST API used by the MCP server."""

    def __init__(self, settings: Settings | None = None) -> None:
        self._settings = settings or get_settings()

    async def search(self, scope: SearchScope, keyword: str) -> list[Any]:
        """Execute a search request against the backend API."""

        url_path = self._settings.get_search_path(scope)
        async with httpx.AsyncClient(
            base_url=str(self._settings.backend_base_url),
            timeout=self._settings.request_timeout_seconds,
        ) as client:
            response = await client.get(url_path, params={"keyword": keyword})
            response.raise_for_status()
            data = response.json()

        if not isinstance(data, list):
            raise RuntimeError("Unexpected search response payload: expected a list")
        return data
