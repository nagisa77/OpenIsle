"""HTTP client wrappers for interacting with the OpenIsle backend."""

from __future__ import annotations

import html
import re
from typing import Any, Iterable

import httpx

from .models import NormalizedSearchResult, SearchResponse, SearchScope
from .settings import Settings

_TAG_RE = re.compile(r"<[^>]+>")
_WHITESPACE_RE = re.compile(r"\s+")


class SearchClient:
    """High level client around the OpenIsle search API."""

    _ENDPOINTS: dict[SearchScope, str] = {
        SearchScope.GLOBAL: "/api/search/global",
        SearchScope.POSTS: "/api/search/posts",
        SearchScope.POSTS_CONTENT: "/api/search/posts/content",
        SearchScope.POSTS_TITLE: "/api/search/posts/title",
        SearchScope.USERS: "/api/search/users",
    }

    def __init__(self, settings: Settings) -> None:
        self._base_url = settings.sanitized_base_url()
        self._timeout = settings.request_timeout
        self._default_limit = settings.default_limit
        self._snippet_length = settings.snippet_length
        self._client = httpx.AsyncClient(
            base_url=self._base_url,
            timeout=self._timeout,
            headers={"Accept": "application/json"},
        )

    async def aclose(self) -> None:
        await self._client.aclose()

    def endpoint_path(self, scope: SearchScope) -> str:
        return self._ENDPOINTS[scope]

    def endpoint_url(self, scope: SearchScope) -> str:
        return f"{self._base_url}{self.endpoint_path(scope)}"

    async def search(
        self,
        keyword: str,
        scope: SearchScope,
        *,
        limit: int | None = None,
    ) -> SearchResponse:
        """Execute a search request and normalise the results."""

        keyword = keyword.strip()
        effective_limit = self._resolve_limit(limit)

        if not keyword:
            return SearchResponse(
                keyword=keyword,
                scope=scope,
                endpoint=self.endpoint_url(scope),
                limit=effective_limit,
                total_results=0,
                returned_results=0,
                normalized=[],
                raw=[],
            )

        response = await self._client.get(
            self.endpoint_path(scope),
            params={"keyword": keyword},
        )
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, list):  # pragma: no cover - defensive programming
            raise ValueError("Search API did not return a JSON array")

        total_results = len(payload)
        items = payload if effective_limit is None else payload[:effective_limit]
        normalized = [self._normalise_item(scope, item) for item in items]

        return SearchResponse(
            keyword=keyword,
            scope=scope,
            endpoint=self.endpoint_url(scope),
            limit=effective_limit,
            total_results=total_results,
            returned_results=len(items),
            normalized=normalized,
            raw=items,
        )

    def _resolve_limit(self, requested: int | None) -> int | None:
        value = requested if requested is not None else self._default_limit
        if value is None:
            return None
        if value <= 0:
            return None
        return value

    def _normalise_item(
        self,
        scope: SearchScope,
        item: Any,
    ) -> NormalizedSearchResult:
        """Normalise raw API objects into a consistent structure."""

        if not isinstance(item, dict):  # pragma: no cover - defensive programming
            return NormalizedSearchResult(type=scope.value, metadata={"raw": item})

        if scope == SearchScope.GLOBAL:
            return self._normalise_global(item)
        if scope in {SearchScope.POSTS, SearchScope.POSTS_CONTENT, SearchScope.POSTS_TITLE}:
            return self._normalise_post(item)
        if scope == SearchScope.USERS:
            return self._normalise_user(item)
        return NormalizedSearchResult(type=scope.value, metadata=item)

    def _normalise_global(self, item: dict[str, Any]) -> NormalizedSearchResult:
        highlights = {
            "title": item.get("highlightedText"),
            "subtitle": item.get("highlightedSubText"),
            "snippet": item.get("highlightedExtra"),
        }
        snippet_source = highlights.get("snippet") or item.get("extra")
        metadata = {
            "postId": item.get("postId"),
            "highlights": {k: v for k, v in highlights.items() if v},
        }
        return NormalizedSearchResult(
            type=str(item.get("type", "result")),
            id=_safe_int(item.get("id")),
            title=highlights.get("title") or _safe_str(item.get("text")),
            subtitle=highlights.get("subtitle") or _safe_str(item.get("subText")),
            snippet=self._snippet(snippet_source),
            metadata={k: v for k, v in metadata.items() if v not in (None, {}, [])},
        )

    def _normalise_post(self, item: dict[str, Any]) -> NormalizedSearchResult:
        author = _safe_dict(item.get("author"))
        category = _safe_dict(item.get("category"))
        tags = [tag.get("name") for tag in _safe_iter(item.get("tags")) if isinstance(tag, dict)]
        metadata = {
            "author": author.get("username"),
            "category": category.get("name"),
            "tags": tags,
            "views": item.get("views"),
            "commentCount": item.get("commentCount"),
            "status": item.get("status"),
            "apiUrl": f"{self._base_url}/api/posts/{item.get('id')}" if item.get("id") else None,
        }
        return NormalizedSearchResult(
            type="post",
            id=_safe_int(item.get("id")),
            title=_safe_str(item.get("title")),
            subtitle=_safe_str(category.get("name")),
            snippet=self._snippet(item.get("content")),
            metadata={k: v for k, v in metadata.items() if v not in (None, [], {})},
        )

    def _normalise_user(self, item: dict[str, Any]) -> NormalizedSearchResult:
        metadata = {
            "followers": item.get("followers"),
            "following": item.get("following"),
            "totalViews": item.get("totalViews"),
            "role": item.get("role"),
            "subscribed": item.get("subscribed"),
            "apiUrl": f"{self._base_url}/api/users/{item.get('id')}" if item.get("id") else None,
        }
        return NormalizedSearchResult(
            type="user",
            id=_safe_int(item.get("id")),
            title=_safe_str(item.get("username")),
            subtitle=_safe_str(item.get("email") or item.get("role")),
            snippet=self._snippet(item.get("introduction")),
            metadata={k: v for k, v in metadata.items() if v not in (None, [], {})},
        )

    def _snippet(self, value: Any) -> str | None:
        text = _safe_str(value)
        if not text:
            return None
        text = html.unescape(text)
        text = _TAG_RE.sub(" ", text)
        text = _WHITESPACE_RE.sub(" ", text).strip()
        if not text:
            return None
        if len(text) <= self._snippet_length:
            return text
        return text[: self._snippet_length - 1].rstrip() + "…"


def _safe_int(value: Any) -> int | None:
    try:
        return int(value)
    except (TypeError, ValueError):  # pragma: no cover - defensive
        return None


def _safe_str(value: Any) -> str | None:
    if value is None:
        return None
    text = str(value).strip()
    return text or None


def _safe_dict(value: Any) -> dict[str, Any]:
    return value if isinstance(value, dict) else {}


def _safe_iter(value: Any) -> Iterable[Any]:
    if isinstance(value, list | tuple | set):
        return value
    return []
