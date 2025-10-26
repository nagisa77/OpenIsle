"""Pydantic models describing tool inputs and outputs."""

from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, Field, ConfigDict


class SearchResultItem(BaseModel):
    """A single search result entry."""

    type: str = Field(description="Entity type for the result (post, user, tag, etc.).")
    id: Optional[int] = Field(default=None, description="Identifier of the matched entity.")
    text: Optional[str] = Field(default=None, description="Primary text associated with the result.")
    sub_text: Optional[str] = Field(
        default=None,
        alias="subText",
        description="Secondary text, e.g. a username or excerpt.",
    )
    extra: Optional[str] = Field(default=None, description="Additional contextual information.")
    post_id: Optional[int] = Field(
        default=None,
        alias="postId",
        description="Associated post identifier when relevant.",
    )
    highlighted_text: Optional[str] = Field(
        default=None,
        alias="highlightedText",
        description="Highlighted snippet of the primary text if available.",
    )
    highlighted_sub_text: Optional[str] = Field(
        default=None,
        alias="highlightedSubText",
        description="Highlighted snippet of the secondary text if available.",
    )
    highlighted_extra: Optional[str] = Field(
        default=None,
        alias="highlightedExtra",
        description="Highlighted snippet of extra information if available.",
    )

    model_config = ConfigDict(populate_by_name=True)


class SearchResponse(BaseModel):
    """Structured response returned by the search tool."""

    keyword: str = Field(description="The keyword that was searched.")
    total: int = Field(description="Total number of matches returned by the backend.")
    results: list[SearchResultItem] = Field(
        default_factory=list,
        description="Ordered collection of search results.",
    )

