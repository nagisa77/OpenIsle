"""Pydantic models shared across the MCP service."""

from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


class SearchResult(BaseModel):
    """Representation of a single search result entry."""

    model_config = ConfigDict(extra="ignore")

    type: Optional[str] = Field(default=None, description="Type of the result entry")
    id: Optional[int] = Field(default=None, description="Identifier of the result entry")
    text: Optional[str] = Field(default=None, description="Primary text of the result entry")
    subText: Optional[str] = Field(default=None, description="Secondary text associated with the result")
    extra: Optional[str] = Field(default=None, description="Additional information about the result")
    postId: Optional[int] = Field(default=None, description="Related post identifier, if applicable")
    highlightedText: Optional[str] = Field(default=None, description="Highlighted primary text segment")
    highlightedSubText: Optional[str] = Field(
        default=None,
        description="Highlighted secondary text segment",
    )
    highlightedExtra: Optional[str] = Field(
        default=None,
        description="Highlighted additional information",
    )


class SearchResponse(BaseModel):
    """Response payload returned by the search endpoint."""

    results: list[SearchResult] = Field(default_factory=list)


__all__ = ["SearchResult", "SearchResponse"]
