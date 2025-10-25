"""Shared models for the OpenIsle MCP server."""

from __future__ import annotations

from enum import Enum
from typing import Any

from pydantic import BaseModel, Field


class SearchScope(str, Enum):
    """Supported search endpoints."""

    GLOBAL = "global"
    POSTS = "posts"
    POSTS_CONTENT = "posts_content"
    POSTS_TITLE = "posts_title"
    USERS = "users"

    def __str__(self) -> str:  # pragma: no cover - convenience for logging
        return self.value


class NormalizedSearchResult(BaseModel):
    """Compact structure returned by the MCP search tool."""

    type: str = Field(description="Entity type, e.g. user, post, comment.")
    id: int | None = Field(default=None, description="Primary identifier of the entity.")
    title: str | None = Field(default=None, description="Display title for the result.")
    subtitle: str | None = Field(default=None, description="Secondary line of context.")
    snippet: str | None = Field(default=None, description="Short summary of the result.")
    metadata: dict[str, Any] = Field(
        default_factory=dict,
        description="Additional attributes extracted from the API response.",
    )

    model_config = {
        "extra": "ignore",
    }


class SearchResponse(BaseModel):
    """Payload returned to MCP clients."""

    keyword: str
    scope: SearchScope
    endpoint: str
    limit: int | None = Field(
        default=None,
        description="Result limit applied to the request. None means unlimited.",
    )
    total_results: int = Field(
        default=0,
        description="Total number of items returned by the OpenIsle API before limiting.",
    )
    returned_results: int = Field(
        default=0,
        description="Number of items returned to the MCP client after limiting.",
    )
    normalized: list[NormalizedSearchResult] = Field(
        default_factory=list,
        description="Normalised representation of each search hit.",
    )
    raw: list[Any] = Field(
        default_factory=list,
        description="Raw response objects from the OpenIsle REST API.",
    )

    model_config = {
        "extra": "ignore",
    }
