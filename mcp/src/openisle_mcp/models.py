"""Data models for the OpenIsle MCP server."""

from __future__ import annotations

from enum import Enum
from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class SearchScope(str, Enum):
    """Supported search scopes exposed via the MCP tool."""

    GLOBAL = "global"
    USERS = "users"
    POSTS = "posts"
    POSTS_TITLE = "posts_title"
    POSTS_CONTENT = "posts_content"


class Highlight(BaseModel):
    """Highlighted fragments returned by the backend search API."""

    text: Optional[str] = Field(default=None, description="Highlighted main text snippet.")
    sub_text: Optional[str] = Field(default=None, description="Highlighted secondary text snippet.")
    extra: Optional[str] = Field(default=None, description="Additional highlighted data.")


class SearchItem(BaseModel):
    """Normalized representation of a single search result."""

    category: str = Field(description="Type/category of the search result, e.g. user or post.")
    title: Optional[str] = Field(default=None, description="Primary title or label for the result.")
    description: Optional[str] = Field(default=None, description="Supporting description or summary text.")
    url: Optional[str] = Field(default=None, description="Canonical URL that references the resource, if available.")
    metadata: Dict[str, Any] = Field(default_factory=dict, description="Additional structured metadata extracted from the API.")
    highlights: Optional[Highlight] = Field(default=None, description="Highlighted snippets returned by the backend search API.")


class SearchResponse(BaseModel):
    """Structured response returned by the MCP search tool."""

    scope: SearchScope = Field(description="Scope of the search that produced the results.")
    keyword: str = Field(description="Keyword submitted to the backend search endpoint.")
    results: list[SearchItem] = Field(default_factory=list, description="Normalized search results from the backend API.")
