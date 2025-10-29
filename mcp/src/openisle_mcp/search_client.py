"""HTTP client helpers for talking to the OpenIsle backend endpoints."""

from __future__ import annotations

import json
import logging
from typing import Any

import httpx


logger = logging.getLogger(__name__)


class SearchClient:
    """Client for calling the OpenIsle HTTP APIs used by the MCP server."""

    def __init__(
        self,
        base_url: str,
        *,
        timeout: float = 10.0,
        access_token: str | None = None,
    ) -> None:
        self._base_url = base_url.rstrip("/")
        self._timeout = timeout
        self._client: httpx.AsyncClient | None = None
        self._access_token = self._sanitize_token(access_token)

    def _get_client(self) -> httpx.AsyncClient:
        if self._client is None:
            logger.debug(
                "Creating httpx.AsyncClient for base URL %s with timeout %.2fs",
                self._base_url,
                self._timeout,
            )
            self._client = httpx.AsyncClient(
                base_url=self._base_url,
                timeout=self._timeout,
            )
        return self._client

    @staticmethod
    def _sanitize_token(token: str | None) -> str | None:
        if token is None:
            return None
        stripped = token.strip()
        return stripped or None

    def update_access_token(self, token: str | None) -> None:
        """Update the default access token used for authenticated requests."""

        self._access_token = self._sanitize_token(token)
        if self._access_token:
            logger.debug("Configured default access token for SearchClient requests.")
        else:
            logger.debug("Cleared default access token for SearchClient requests.")

    def _resolve_token(self, token: str | None) -> str | None:
        candidate = self._sanitize_token(token)
        if candidate is not None:
            return candidate
        return self._access_token

    def _require_token(self, token: str | None) -> str:
        resolved = self._resolve_token(token)
        if resolved is None:
            raise ValueError(
                "Authenticated request requires an access token. Provide a Bearer token "
                "via the MCP Authorization header or configure a default token for the server."
            )
        return resolved

    def _build_headers(
        self,
        *,
        token: str,
        accept: str = "application/json",
        include_json: bool = False,
    ) -> dict[str, str]:
        headers: dict[str, str] = {"Accept": accept}
        resolved = self._resolve_token(token)
        if resolved:
            headers["Authorization"] = f"Bearer {resolved}"
        if include_json:
            headers["Content-Type"] = "application/json"
        return headers

    async def global_search(self, keyword: str) -> list[dict[str, Any]]:
        """Call the global search endpoint and return the parsed JSON payload."""

        client = self._get_client()
        logger.debug("Calling global search with keyword=%s", keyword)
        response = await client.get(
            "/api/search/global",
            params={"keyword": keyword},
            headers=self._build_headers(),
        )
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, list):
            formatted = json.dumps(payload, ensure_ascii=False)[:200]
            raise ValueError(f"Unexpected response format from search endpoint: {formatted}")
        logger.info(
            "Global search returned %d results for keyword '%s'",
            len(payload),
            keyword,
        )
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
        resolved_token = self._require_token(token)
        headers = self._build_headers(token=resolved_token, include_json=True)
        payload: dict[str, Any] = {"content": content}
        if captcha is not None:
            stripped_captcha = captcha.strip()
            if stripped_captcha:
                payload["captcha"] = stripped_captcha

        logger.debug(
            "Posting reply to comment_id=%s (captcha=%s)",
            comment_id,
            bool(captcha),
        )
        response = await client.post(
            f"/api/comments/{comment_id}/replies",
            json=payload,
            headers=headers,
        )
        response.raise_for_status()
        body = self._ensure_dict(response.json())
        logger.info("Reply to comment_id=%s succeeded with id=%s", comment_id, body.get("id"))
        return body

    async def reply_to_post(
        self,
        post_id: int,
        token: str,
        content: str,
        captcha: str | None = None,
    ) -> dict[str, Any]:
        """Create a comment on a post and return the backend payload."""

        client = self._get_client()
        resolved_token = self._require_token(token)
        headers = self._build_headers(token=resolved_token, include_json=True)
        payload: dict[str, Any] = {"content": content}
        if captcha is not None:
            stripped_captcha = captcha.strip()
            if stripped_captcha:
                payload["captcha"] = stripped_captcha

        logger.debug(
            "Posting comment to post_id=%s (captcha=%s)",
            post_id,
            bool(captcha),
        )
        response = await client.post(
            f"/api/posts/{post_id}/comments",
            json=payload,
            headers=headers,
        )
        response.raise_for_status()
        body = self._ensure_dict(response.json())
        logger.info("Reply to post_id=%s succeeded with id=%s", post_id, body.get("id"))
        return body

    async def create_post(
        self,
        payload: dict[str, Any],
        *,
        token: str,
    ) -> dict[str, Any]:
        """Create a new post and return the detailed backend payload."""

        client = self._get_client()
        resolved_token = self._require_token(token)
        headers = self._build_headers(token=resolved_token, include_json=True)

        logger.debug(
            "Creating post with category_id=%s and %d tag(s)",
            payload.get("categoryId"),
            len(payload.get("tagIds", []) if isinstance(payload.get("tagIds"), list) else []),
        )
        response = await client.post(
            "/api/posts",
            json=payload,
            headers=headers,
        )
        response.raise_for_status()
        body = self._ensure_dict(response.json())
        logger.info("Post creation succeeded with id=%s, token=%s", body.get("id"), token)
        return body

    async def recent_posts(self, minutes: int) -> list[dict[str, Any]]:
        """Return posts created within the given timeframe."""

        client = self._get_client()
        logger.debug(
            "Fetching recent posts within last %s minutes",
            minutes,
        )
        response = await client.get(
            "/api/posts/recent",
            params={"minutes": minutes},
            headers=self._build_headers(),
        )
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, list):
            formatted = json.dumps(payload, ensure_ascii=False)[:200]
            raise ValueError(
                f"Unexpected response format from recent posts endpoint: {formatted}"
            )
        logger.info(
            "Fetched %d recent posts for window=%s minutes",
            len(payload),
            minutes,
        )
        return [self._ensure_dict(entry) for entry in payload]

    async def get_post(self, post_id: int, token: str | None = None) -> dict[str, Any]:
        """Retrieve the detailed payload for a single post."""

        client = self._get_client()
        headers = self._build_headers(token=token)
        logger.debug("Fetching post details for post_id=%s", post_id)
        response = await client.get(f"/api/posts/{post_id}", headers=headers)
        response.raise_for_status()
        body = self._ensure_dict(response.json())
        logger.info(
            "Retrieved post_id=%s successfully with %d top-level comments",
            post_id,
            len(body.get("comments", []) if isinstance(body.get("comments"), list) else []),
        )
        return body

    async def list_unread_notifications(
        self,
        *,
        page: int = 0,
        size: int = 30,
        token: str,
    ) -> list[dict[str, Any]]:
        """Return unread notifications for the authenticated user."""

        client = self._get_client()
        resolved_token = self._require_token(token)
        logger.debug(
            "Fetching unread notifications with page=%s, size=%s",
            page,
            size,
        )
        response = await client.get(
            "/api/notifications/unread",
            params={"page": page, "size": size},
            headers=self._build_headers(token=resolved_token),
        )
        response.raise_for_status()
        payload = response.json()
        if not isinstance(payload, list):
            formatted = json.dumps(payload, ensure_ascii=False)[:200]
            raise ValueError(
                "Unexpected response format from unread notifications endpoint: "
                f"{formatted}"
            )
        logger.info(
            "Fetched %d unread notifications (page=%s, size=%s)",
            len(payload),
            page,
            size,
        )
        return [self._ensure_dict(entry) for entry in payload]

    async def mark_notifications_read(
        self,
        ids: list[int],
        *,
        token: str
    ) -> None:
        """Mark the provided notifications as read for the authenticated user."""

        if not ids:
            raise ValueError(
                "At least one notification identifier must be provided to mark as read."
            )

        sanitized_ids: list[int] = []
        for value in ids:
            if isinstance(value, bool):
                raise ValueError("Notification identifiers must be integers, not booleans.")
            try:
                converted = int(value)
            except (TypeError, ValueError) as exc:  # pragma: no cover - defensive
                raise ValueError(
                    "Notification identifiers must be integers."
                ) from exc
            if converted <= 0:
                raise ValueError(
                    "Notification identifiers must be positive integers."
                )
            sanitized_ids.append(converted)

        client = self._get_client()
        resolved_token = self._require_token(token)
        logger.debug(
            "Marking %d notifications as read: ids=%s",
            len(sanitized_ids),
            sanitized_ids,
        )
        response = await client.post(
            "/api/notifications/read",
            json={"ids": sanitized_ids},
            headers=self._build_headers(token=resolved_token, include_json=True),
        )
        response.raise_for_status()
        logger.info(
            "Successfully marked %d notifications as read.",
            len(sanitized_ids),
        )

    async def aclose(self) -> None:
        """Dispose of the underlying HTTP client."""

        if self._client is not None:
            await self._client.aclose()
            self._client = None
            logger.debug("Closed httpx.AsyncClient for SearchClient.")

    @staticmethod
    def _ensure_dict(entry: Any) -> dict[str, Any]:
        if not isinstance(entry, dict):
            raise ValueError(f"Expected JSON object, got: {type(entry)!r}")
        return entry
