"""Utilities for normalising OpenIsle search results."""

from __future__ import annotations

import re
from typing import Any, Iterable

from .models import Highlight, SearchItem, SearchScope


def _truncate(text: str | None, *, limit: int = 240) -> str | None:
    """Compress whitespace and truncate overly long text fragments."""

    if not text:
        return None
    compact = re.sub(r"\s+", " ", text).strip()
    if len(compact) <= limit:
        return compact
    return f"{compact[:limit - 1]}…"


def _extract_highlight(data: dict[str, Any]) -> Highlight | None:
    highlighted = {
        "text": data.get("highlightedText"),
        "sub_text": data.get("highlightedSubText"),
        "extra": data.get("highlightedExtra"),
    }
    if any(highlighted.values()):
        return Highlight(**highlighted)
    return None


def normalise_results(scope: SearchScope, payload: Iterable[dict[str, Any]]) -> list[SearchItem]:
    """Convert backend payloads into :class:`SearchItem` entries."""

    normalised: list[SearchItem] = []

    for item in payload:
        if not isinstance(item, dict):
            continue

        if scope is SearchScope.GLOBAL:
            normalised.append(
                SearchItem(
                    category=item.get("type", scope.value),
                    title=_truncate(item.get("text")),
                    description=_truncate(item.get("subText")),
                    metadata={
                        "id": item.get("id"),
                        "postId": item.get("postId"),
                        "extra": item.get("extra"),
                    },
                    highlights=_extract_highlight(item),
                )
            )
            continue

        if scope in {SearchScope.POSTS, SearchScope.POSTS_CONTENT, SearchScope.POSTS_TITLE}:
            author = item.get("author") or {}
            category = item.get("category") or {}
            metadata = {
                "id": item.get("id"),
                "author": author.get("username"),
                "category": category.get("name"),
                "views": item.get("views"),
                "commentCount": item.get("commentCount"),
                "tags": [tag.get("name") for tag in item.get("tags", []) if isinstance(tag, dict)],
            }
            normalised.append(
                SearchItem(
                    category="post",
                    title=_truncate(item.get("title")),
                    description=_truncate(item.get("content")),
                    metadata={k: v for k, v in metadata.items() if v is not None},
                )
            )
            continue

        if scope is SearchScope.USERS:
            metadata = {
                "id": item.get("id"),
                "email": item.get("email"),
                "followers": item.get("followers"),
                "following": item.get("following"),
                "role": item.get("role"),
            }
            normalised.append(
                SearchItem(
                    category="user",
                    title=_truncate(item.get("username")),
                    description=_truncate(item.get("introduction")),
                    metadata={k: v for k, v in metadata.items() if v is not None},
                )
            )
            continue

        # Fallback: include raw entry to aid debugging of unsupported scopes
        normalised.append(SearchItem(category=scope.value, metadata=item))

    return normalised
