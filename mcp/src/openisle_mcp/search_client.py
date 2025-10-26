"""HTTP client helpers for talking to the OpenIsle backend search endpoints."""

from __future__ import annotations

import json
from typing import Any

import httpx


class SearchClient:
    """Client for calling the OpenIsle search API."""

    def __init__(self, base_url: str, *, timeout: float = 10.0) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._client: httpx.AsyncClient | None = None

    def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            self._client = httpx.AsyncClient(base_url=self._base_url, timeout=self._timeout)
        return self._client

    async def global_search(self, keyword: str) -> list[dict[str, Any]]:
        """Call the global search endpoint and return the parsed JSON payload."""

        client = self._get_client()
        response = await client.get(
            "/api/search/global",
            params={"keyword": keyword},
            headers={"Accept": "application/json"},
        )
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, list):
            formatted = json.dumps(payload, ensure_ascii=False)[:200]
            raise ValueError(f"Unexpected response format from search endpoint: {formatted}")
        return [self._validate_entry(entry) for entry in payload]

    async def aclose(self) -> None:
        """Dispose of the underlying HTTP client."""

        if self._client is not None:
            await self._client.aclose()
            self._client = None

    @staticmethod
    def _validate_entry(entry: Any) -> dict[str, Any]:
        if not isinstance(entry, dict):
            raise ValueError(f"Search entry must be an object, got: {type(entry)!r}")
        return entry
