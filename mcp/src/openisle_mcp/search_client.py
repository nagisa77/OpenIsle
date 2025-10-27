"""HTTP client helpers for talking to the OpenIsle backend endpoints."""

from __future__ import annotations

import json
from typing import Any

import httpx


class SearchClient:
    """Client for calling the OpenIsle HTTP APIs used by the MCP server."""

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
        return [self._ensure_dict(entry) for entry in payload]

    async def reply_to_comment(
        self,
        comment_id: int,
        token: str,
        content: str,
        captcha: str | None = None,
    ) -> dict[str, Any]:
        """Reply to an existing comment and return the created reply."""

        client = self._get_client()
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "Authorization": f"Bearer {token}",
        }
        payload: dict[str, Any] = {"content": content}
        if captcha is not None:
            stripped_captcha = captcha.strip()
            if stripped_captcha:
                payload["captcha"] = stripped_captcha

        response = await client.post(
            f"/api/comments/{comment_id}/replies",
            json=payload,
            headers=headers,
        )
        response.raise_for_status()
        return self._ensure_dict(response.json())

    async def recent_posts(self, minutes: int) -> list[dict[str, Any]]:
        """Return posts created within the given timeframe."""

        client = self._get_client()
        response = await client.get(
            "/api/posts/recent",
            params={"minutes": minutes},
            headers={"Accept": "application/json"},
        )
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, list):
            formatted = json.dumps(payload, ensure_ascii=False)[:200]
            raise ValueError(
                f"Unexpected response format from recent posts endpoint: {formatted}"
            )
        return [self._ensure_dict(entry) for entry in payload]

    async def get_post(self, post_id: int, token: str | None = None) -> dict[str, Any]:
        """Retrieve the detailed payload for a single post."""

        client = self._get_client()
        headers = {"Accept": "application/json"}
        if token:
            headers["Authorization"] = f"Bearer {token}"

        response = await client.get(f"/api/posts/{post_id}", headers=headers)
        response.raise_for_status()
        return self._ensure_dict(response.json())

    async def aclose(self) -> None:
        """Dispose of the underlying HTTP client."""

        if self._client is not None:
            await self._client.aclose()
            self._client = None

    @staticmethod
    def _ensure_dict(entry: Any) -> dict[str, Any]:
        if not isinstance(entry, dict):
            raise ValueError(f"Expected JSON object, got: {type(entry)!r}")
        return entry
