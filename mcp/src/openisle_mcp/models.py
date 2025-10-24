"""Pydantic models used by the OpenIsle MCP server."""

from __future__ import annotations

from typing import Dict, Optional

from pydantic import BaseModel, ConfigDict, Field

__all__ = [
    "BackendSearchResult",
    "SearchResult",
    "SearchResponse",
]


class BackendSearchResult(BaseModel):
    """Shape of the payload returned by the OpenIsle backend."""

    type: str
    id: Optional[int] = None
    text: Optional[str] = None
    sub_text: Optional[str] = Field(default=None, alias="subText")
    extra: Optional[str] = None
    post_id: Optional[int] = Field(default=None, alias="postId")
    highlighted_text: Optional[str] = Field(default=None, alias="highlightedText")
    highlighted_sub_text: Optional[str] = Field(default=None, alias="highlightedSubText")
    highlighted_extra: Optional[str] = Field(default=None, alias="highlightedExtra")

    model_config = ConfigDict(populate_by_name=True, extra="ignore")


class SearchResult(BaseModel):
    """Structured search result returned to MCP clients."""

    type: str = Field(description="Entity type, e.g. post, comment, user")
    id: Optional[int] = Field(default=None, description="Primary identifier for the entity")
    title: Optional[str] = Field(default=None, description="Primary text to display")
    subtitle: Optional[str] = Field(default=None, description="Secondary text (e.g. author or category)")
    extra: Optional[str] = Field(default=None, description="Additional descriptive snippet")
    post_id: Optional[int] = Field(default=None, description="Associated post id for comment results")
    url: Optional[str] = Field(default=None, description="Deep link to the resource inside OpenIsle")
    highlights: Dict[str, Optional[str]] = Field(
        default_factory=dict,
        description="Highlighted HTML fragments keyed by field name",
    )

    model_config = ConfigDict(populate_by_name=True)


class SearchResponse(BaseModel):
    """Response envelope returned from the MCP search tool."""

    keyword: str = Field(description="Sanitised keyword that was searched for")
    total_results: int = Field(description="Total number of results returned by the backend")
    limit: int = Field(description="Maximum number of results included in the response")
    results: list[SearchResult] = Field(default_factory=list, description="Search results up to the requested limit")

    model_config = ConfigDict(populate_by_name=True)
