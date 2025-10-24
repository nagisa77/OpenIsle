"""HTTP client for talking to the OpenIsle backend."""

from __future__ import annotations

import json
import logging
from typing import List

import httpx
from pydantic import ValidationError

from .models import BackendSearchResult

__all__ = ["BackendClientError", "OpenIsleBackendClient"]

logger = logging.getLogger(__name__)


class BackendClientError(RuntimeError):
    """Raised when the backend cannot fulfil a request."""


class OpenIsleBackendClient:
    """Tiny wrapper around the Spring Boot search endpoints."""

    def __init__(self, base_url: str, timeout: float = 10.0) -> None:
        if not base_url:
            raise ValueError("base_url must not be empty")
        self._base_url = base_url.rstrip("/")
        timeout = timeout if timeout > 0 else 10.0
        self._timeout = httpx.Timeout(timeout, connect=timeout, read=timeout)

    @property
    def base_url(self) -> str:
        return self._base_url

    async def search_global(self, keyword: str) -> List[BackendSearchResult]:
        """Call `/api/search/global` and normalise the payload."""

        url = f"{self._base_url}/api/search/global"
        params = {"keyword": keyword}
        headers = {"Accept": "application/json"}
        logger.debug("Calling OpenIsle backend", extra={"url": url, "params": params})

        try:
            async with httpx.AsyncClient(timeout=self._timeout, headers=headers, follow_redirects=True) as client:
                response = await client.get(url, params=params)
                response.raise_for_status()
        except httpx.HTTPStatusError as exc:  # pragma: no cover - network errors are rare in tests
            body_preview = _truncate_body(exc.response.text)
            raise BackendClientError(
                f"Backend returned HTTP {exc.response.status_code}: {body_preview}"
            ) from exc
        except httpx.RequestError as exc:  # pragma: no cover - network errors are rare in tests
            raise BackendClientError(f"Failed to reach backend: {exc}") from exc

        try:
            payload = response.json()
        except json.JSONDecodeError as exc:
            raise BackendClientError("Backend returned invalid JSON") from exc

        if not isinstance(payload, list):
            raise BackendClientError("Unexpected search payload type; expected a list")

        results: list[BackendSearchResult] = []
        for item in payload:
            try:
                results.append(BackendSearchResult.model_validate(item))
            except ValidationError as exc:
                raise BackendClientError(f"Invalid search result payload: {exc}") from exc

        return results


def _truncate_body(body: str, limit: int = 200) -> str:
    body = body.strip()
    if len(body) <= limit:
        return body
    return f"{body[:limit]}…"
